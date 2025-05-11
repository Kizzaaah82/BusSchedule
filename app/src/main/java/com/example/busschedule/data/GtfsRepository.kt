package com.example.busschedule.data

import android.content.Context
import android.util.Log
import com.example.busschedule.model.*
import com.example.busschedule.util.GtfsDownloader
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

class GtfsRepository(private val context: Context) {

    private val routes = mutableListOf<GtfsRoute>()
    private val trips = mutableListOf<GtfsTrip>()
    private val stopTimes = mutableListOf<GtfsStopTime>()
    private val stops = mutableListOf<GtfsStop>()
    private val shapes = mutableListOf<GtfsShape>()
    private val calendar = mutableListOf<GtfsCalendar>()
    private val calendarDates = mutableListOf<GtfsCalendarDate>()

    suspend fun loadRoutesOnly() {
        loadRoutes()
    }

    suspend fun loadDetailedData() {
        loadTrips()
        loadStopTimes()
        loadStops()
        loadShapes()
        loadCalendar()
        loadCalendarDates()
    }

    fun getAgencyTimezone(): String {
        openGtfsFile("gtfs/agency.txt").use { reader ->
            val header = reader.readLine()?.split(",") ?: return "America/New_York"
            val timezoneIndex = header.indexOf("agency_timezone")
            val line = reader.readLine()?.split(",") ?: return "America/New_York"
            return if (timezoneIndex != -1 && line.size > timezoneIndex) {
                line[timezoneIndex].trim()
            } else {
                "America/New_York" // fallback default
            }
        }
    }

    // Inside GtfsRepository.kt

    suspend fun loadTripIdToRouteIdMap(): Map<String, String> {
        val tripIdToRouteId = mutableMapOf<String, String>()
        Log.d("GtfsRepository", "Attempting to load tripId to routeId map...")

        try {
            // Use the existing function to open the file (handles local vs asset)
            val tripReader = openGtfsFile("gtfs/trips.txt")

            tripReader.useLines { linesSeq ->
                val lines = linesSeq.toList() // Read all lines to easily get header and iterate

                if (lines.isEmpty()) {
                    Log.e("GtfsRepository", "Error loading map: trips.txt appears to be empty!")
                    return@useLines // Exit the useLines block, will return empty map
                }

                val header = lines.first()
                val headerParts = header.split(",").map { it.trim() } // Trim header parts

                // --- CORE FIX: Use "trip_id" instead of "trip_short_name" ---
                val tripIdIndex = headerParts.indexOf("trip_id")
                val routeIdIndex = headerParts.indexOf("route_id")

                // --- Robustness Checks ---
                if (tripIdIndex == -1) {
                    Log.e("GtfsRepository", "Error loading map: Missing 'trip_id' column in trips.txt header. Header: $header")
                    return@useLines // Exit useLines block
                }
                if (routeIdIndex == -1) {
                    Log.e("GtfsRepository", "Error loading map: Missing 'route_id' column in trips.txt header. Header: $header")
                    return@useLines // Exit useLines block
                }
                // --- End Checks ---

                Log.d("GtfsRepository", "Found 'trip_id' at index $tripIdIndex and 'route_id' at index $routeIdIndex in trips.txt")

                // Process data lines (skip header)
                lines.drop(1).forEachIndexed { lineIndex, line ->
                    val parts = line.split(",")
                    // Check if the line has enough parts to access both required indices
                    if (parts.size > maxOf(tripIdIndex, routeIdIndex)) {
                        val tripId = parts[tripIdIndex].trim() // Extract trip_id and trim
                        val routeId = parts[routeIdIndex].trim() // Extract route_id and trim

                        // Ensure trip_id is not blank before adding to the map
                        if (tripId.isNotBlank()) {
                            tripIdToRouteId[tripId] = routeId
                        } else {
                            Log.w("GtfsRepository", "Found blank trip_id in trips.txt at line ${lineIndex + 2}: $line") // +2 because drop(1) and 0-based index
                        }
                    } else {
                        Log.w("GtfsRepository", "Skipping malformed line in trips.txt (not enough columns: ${parts.size}, need max($tripIdIndex, $routeIdIndex)+1) at line ${lineIndex + 2}: $line")
                    }
                }
            } // End useLines (Reader is automatically closed here)

        } catch (e: Exception) {
            // Catch potential exceptions during file opening or reading
            Log.e("GtfsRepository", "Exception while loading tripId to routeId map", e)
            return emptyMap() // Return empty map on failure
        }

        Log.d("GtfsRepository", "Successfully loaded ${tripIdToRouteId.size} entries into tripIdToRouteId map.")
        return tripIdToRouteId
    }

    fun getValidTripIdsForStop(stopId: String): Set<String> {
        val validServiceIds = getValidServiceIdsForToday()
        return stopTimes.filter { it.stopId == stopId }
            .filter { stopTime ->
                trips.any { it.tripId == stopTime.tripId && it.serviceId in validServiceIds }
            }
            .map { it.tripId }
            .toSet()
    }

    private fun getFile(fileName: String): File {
        val dir = File(context.filesDir, "gtfs")
        return File(dir, fileName)
    }

    private fun openGtfsFile(filename: String): BufferedReader {
        val cleanFilename = filename.removePrefix("gtfs/")
        val localFile = GtfsDownloader.getLocalGtfsFile(context, cleanFilename)
        return if (localFile != null && localFile.exists()) {
            Log.d("GtfsRepository", "Using local file: ${'$'}{localFile.path}")
            BufferedReader(InputStreamReader(localFile.inputStream()))
        } else {
            Log.d("GtfsRepository", "Local file not found for $cleanFilename, using asset: $filename")
            try {
                BufferedReader(InputStreamReader(context.assets.open(filename)))
            } catch (e: java.io.FileNotFoundException) {
                Log.e("GtfsRepository", "Asset file not found either: $filename", e)
                throw e
            }
        }
    }

    private fun loadRoutes() {
        openGtfsFile("gtfs/routes.txt").use { reader ->
            reader.readLine()
            reader.forEachLine { line ->
                val parts = line.split(",")
                if (parts.size >= 9) {
                    routes.add(
                        GtfsRoute(
                            routeId = parts[0],
                            routeShortName = parts[2],
                            routeLongName = parts[3],
                            routeColor = parts[7].ifBlank { "000000" },
                            routeTextColor = parts[8].ifBlank { "FFFFFF" }
                        )
                    )
                }
            }
        }
    }

    private fun loadTrips() {
        if (trips.isNotEmpty()) return
        openGtfsFile("gtfs/trips.txt").use { reader ->
            val header = reader.readLine()?.split(",") ?: return
            val routeIdIndex = header.indexOf("route_id")
            val serviceIdIndex = header.indexOf("service_id")
            val tripIdIndex = header.indexOf("trip_id")
            val shapeIdIndex = header.indexOf("shape_id")

            val requiredIndices = listOf(routeIdIndex, tripIdIndex) + if (shapeIdIndex != -1) listOf(shapeIdIndex) else emptyList()

            reader.forEachLine { line ->
                val parts = line.split(",")
                if (parts.size > requiredIndices.max()) {
                    trips.add(
                        GtfsTrip(
                            routeId = parts[routeIdIndex],
                            serviceId = parts.getOrElse(serviceIdIndex) { "" },
                            tripId = parts[tripIdIndex],
                            shapeId = if (shapeIdIndex != -1 && parts[shapeIdIndex].isNotBlank()) parts[shapeIdIndex] else null
                        )
                    )
                }
            }
        }
    }

    private fun loadStopTimes() {
        if (stopTimes.isNotEmpty()) return
        openGtfsFile("gtfs/stop_times.txt").use { reader ->
            reader.readLine()
            reader.forEachLine { line ->
                val parts = line.split(",")
                if (parts.size > 3) {
                    stopTimes.add(
                        GtfsStopTime(
                            tripId = parts[0].trim(),
                            arrivalTime = parts[1].trim(),
                            stopId = parts[3].trim(),
                            stopSequence = parts[4].trim() // 👈 Adjust the index if needed
                        )
                    )
                }
            }
        }
    }

    private fun loadStops() {
        if (stops.isNotEmpty()) return
        openGtfsFile("gtfs/stops.txt").use { reader ->
            reader.readLine()
            reader.forEachLine { line ->
                val parts = line.split(",")
                if (parts.size > 5) {
                    stops.add(
                        GtfsStop(
                            stopId = parts[0],
                            stopCode = parts[1],
                            name = parts[2],
                            lat = parts[4].toDouble(),
                            lon = parts[5].toDouble()
                        )
                    )
                }
            }
        }
    }

    private fun loadShapes() {
        if (shapes.isNotEmpty()) return
        openGtfsFile("gtfs/shapes.txt").use { reader ->
            reader.readLine()
            reader.forEachLine { line ->
                val parts = line.split(",")
                if (parts.size >= 4 && parts[0].isNotBlank()) {
                    try {
                        shapes.add(
                            GtfsShape(
                                shapeId = parts[0].trim(),
                                lat = parts[1].trim().toDouble(),
                                lon = parts[2].trim().toDouble(),
                                sequence = parts[3].trim().toInt()
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("GtfsRepository", "Invalid shape point: $line", e)
                    }
                }
            }
        }
    }

    private fun loadCalendar() {
        openGtfsFile("gtfs/calendar.txt").use { reader ->
            reader.readLine()
            reader.forEachLine { line ->
                val parts = line.split(",")
                if (parts.size >= 10) {
                    calendar.add(
                        GtfsCalendar(
                            serviceId = parts[0],
                            monday = parts[1] == "1",
                            tuesday = parts[2] == "1",
                            wednesday = parts[3] == "1",
                            thursday = parts[4] == "1",
                            friday = parts[5] == "1",
                            saturday = parts[6] == "1",
                            sunday = parts[7] == "1",
                            startDate = parts[8],
                            endDate = parts[9]
                        )
                    )
                }
            }
        }
    }

    private fun loadCalendarDates() {
        openGtfsFile("gtfs/calendar_dates.txt").use { reader ->
            reader.readLine()
            reader.forEachLine { line ->
                val parts = line.split(",")
                if (parts.size >= 3) {
                    calendarDates.add(
                        GtfsCalendarDate(
                            serviceId = parts[0],
                            date = parts[1],
                            exceptionType = parts[2].toInt()
                        )
                    )
                }
            }
        }
    }

    private fun getValidServiceIdsForToday(): Set<String> {
        val now = Calendar.getInstance()
        val formatter = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

        val isEarlyMorning = now.get(Calendar.HOUR_OF_DAY) < 3
        val dateStringsToCheck = mutableSetOf(formatter.format(now.time))

        if (isEarlyMorning) {
            val yesterday = now.clone() as Calendar
            yesterday.add(Calendar.DATE, -1)
            dateStringsToCheck.add(formatter.format(yesterday.time))
        }

        // Pull calendar_dates first
        val exceptions = calendarDates.filter { it.date in dateStringsToCheck }
        if (exceptions.isNotEmpty()) {
            return exceptions.filter { it.exceptionType == 1 }.map { it.serviceId }.toSet()
        }

        val validServiceIds = mutableSetOf<String>()
        dateStringsToCheck.forEach { dateStr ->
            val cal = formatter.parse(dateStr)
            val calInstance = Calendar.getInstance().apply { time = cal!! }
            val dayOfWeek = calInstance.get(Calendar.DAY_OF_WEEK)

            validServiceIds += calendar.filter { entry ->
                when (dayOfWeek) {
                    Calendar.MONDAY -> entry.monday
                    Calendar.TUESDAY -> entry.tuesday
                    Calendar.WEDNESDAY -> entry.wednesday
                    Calendar.THURSDAY -> entry.thursday
                    Calendar.FRIDAY -> entry.friday
                    Calendar.SATURDAY -> entry.saturday
                    Calendar.SUNDAY -> entry.sunday
                    else -> false
                }
            }.map { it.serviceId }
        }

        return validServiceIds
    }

    fun getAllRoutes(): List<GtfsRoute> {
        return routes.sortedBy { it.routeShortName }
    }

    fun getStopsForRoute(routeName: String): List<GtfsStop> {
        val routeId = routes.find { it.routeShortName.equals(routeName, ignoreCase = true) }?.routeId
            ?: return emptyList()
        val tripIds = trips.filter { it.routeId == routeId }.map { it.tripId }.toSet()
        if (tripIds.isEmpty()) return emptyList()

        val stopIds = stopTimes.filter { tripIds.contains(it.tripId) }.map { it.stopId }.toSet()
        return stops.filter { stopIds.contains(it.stopId) }
    }

    fun getShapePointsForRoute(routeName: String): List<List<Pair<Double, Double>>> {
        val routeId = routes.find { it.routeShortName.equals(routeName, ignoreCase = true) }?.routeId
            ?: return emptyList()

        val relevantTrips = trips.filter { it.routeId == routeId && it.shapeId != null }

        val distinctShapeIds = relevantTrips.mapNotNull { it.shapeId }.distinct()

        if (distinctShapeIds.isEmpty()) {
            Log.w("GtfsRepository", "No distinct shape IDs found for routeName: $routeName (routeId: $routeId)")
            return emptyList()
        }

        val allShapePoints = distinctShapeIds.map { shapeId ->
            shapes.filter { it.shapeId == shapeId }
                .sortedBy { it.sequence }
                .map { it.lat to it.lon }
        }

        Log.d("GtfsRepository", "Found ${'$'}{allShapePoints.size} distinct shapes for routeName: $routeName")
        return allShapePoints
    }

    fun getStaticTimetableForStop(stopId: String): List<String> {
        val validServiceIds = getValidServiceIdsForToday()
        val todayTripIds = trips.filter { it.serviceId in validServiceIds }.map { it.tripId }.toSet()

        return stopTimes.filter { it.stopId == stopId && it.tripId in todayTripIds }
            .map { it.arrivalTime }
            .toSet()
            .sorted()
    }

    fun getStopTimesForTrip(tripId: String): List<GtfsStopTime> {
        return stopTimes.filter { it.tripId == tripId }
            .sortedBy { it.stopSequence.toIntOrNull() ?: Int.MAX_VALUE }
    }

    fun getScheduledTimeForStop(tripId: String, stopId: String): Long? {
        val stopTime = stopTimes.find { it.tripId == tripId && it.stopId == stopId } ?: return null
        val parts = stopTime.arrivalTime.split(":")
        if (parts.size != 3) return null

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, parts[0].toInt())
            set(Calendar.MINUTE, parts[1].toInt())
            set(Calendar.SECOND, parts[2].toInt())
        }

        val now = System.currentTimeMillis()
        return calendar.timeInMillis.takeIf { it > now }
    }
}