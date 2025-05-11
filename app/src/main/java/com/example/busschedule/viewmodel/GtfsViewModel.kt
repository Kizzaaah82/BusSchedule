package com.example.busschedule.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.busschedule.data.GtfsRepository
import com.example.busschedule.model.GtfsStop
import com.example.busschedule.ui.formatArrivalTime
import com.example.busschedule.ui.sortTimetableTimes
import com.example.busschedule.util.GtfsDownloader
import com.google.transit.realtime.GtfsRealtime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.*
import kotlin.math.*
import androidx.core.graphics.toColorInt
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.ChronoUnit



data class ArrivalDisplay(
    val minutesAway: Int,
    val status: String?, // "early", "on time", "late", or null for scheduled
    val isRealtime: Boolean,
    val timeFormatted: String? = null, // e.g., "03:45 PM"
    val wasInterpolated: Boolean = false // New flag for interpolation
)

data class UiRoute(
    val routeId: String, // ADD THIS
    val shortName: String,
    val longName: String,
    val color: Color,
    val textColor: Color
)

data class BusPosition(
    val vehicleId: String,
    val routeId: String?,
    val tripId: String?,
    val lat: Double,
    val lon: Double,
    val bearing: Float?,
    val label: String?,
    val occupancyStatus: String? = null
)

class GtfsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GtfsRepository(application)
    val agencyTimezone = ZoneId.of(repository.getAgencyTimezone())


    var availableRoutes = mutableStateOf<List<UiRoute>>(emptyList())
        private set

    var isRoutesLoaded = mutableStateOf(false)
        private set

    private var detailedDataLoaded = false

    private var lastFetchTimeMillis: Long = 0

    private var tripToRouteMap = emptyMap<String, String>()
    private val cachedArrivalsByStop = mutableMapOf<String, Pair<Long, List<ArrivalDisplay>>>()
    private val arrivalCacheDurationMillis = 30_000L // 30 seconds
    private val stopToRouteColorMap = mutableMapOf<String, Color>()
    private var lastFetchedTripFeed: GtfsRealtime.FeedMessage? = null

    init {
        viewModelScope.launch {
            tripToRouteMap = repository.loadTripIdToRouteIdMap()
            initializeGtfsData()
        }
    }

    private fun initializeGtfsData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (GtfsDownloader.shouldDownloadNewGtfsFiles(getApplication())) {
                    val success = GtfsDownloader.downloadGtfsFiles(getApplication())
                    if (success) {
                        Log.d("GtfsViewModel", "GTFS files downloaded successfully.")
                    } else {
                        Log.w("GtfsViewModel", "GTFS file download failed. Using existing local or bundled data.")
                    }
                } else {
                    Log.d("GtfsViewModel", "GTFS files are still fresh. Skipping download.")
                }
            } catch (e: Exception) {
                Log.e("GtfsViewModel", "Error downloading GTFS files", e)
            } finally {
                loadRoutesOnly()
            }
        }
    }

    private fun loadRoutesOnly() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.loadRoutesOnly()
                val routes = repository.getAllRoutes()
                val uiRoutes = routes.map { route ->
                    UiRoute(
                        routeId = route.routeId, // NEW!
                        shortName = route.routeShortName,
                        longName = route.routeLongName,
                        color = Color("#${route.routeColor}".toColorInt()),
                        textColor = Color("#${route.routeColor}".toColorInt())
                    )
                }
                withContext(Dispatchers.Main) {
                    availableRoutes.value = uiRoutes
                    isRoutesLoaded.value = true
                }
            } catch (e: Exception) {
                Log.e("GtfsViewModel", "Error loading routes", e)
            }
        }
    }

    fun loadRouteData(
        routeName: String,
        // Update the callback signature to expect a List of Lists
        onLoaded: (stops: List<GtfsStop>, shapePointLists: List<List<Pair<Double, Double>>>) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!detailedDataLoaded) {
                    // Consider loading detailed data only if needed or if it hasn't been loaded recently
                    repository.loadDetailedData()
                    detailedDataLoaded = true
                }
                val stops = repository.getStopsForRoute(routeName)
                // Call the updated repository function which returns List<List<Pair<Double, Double>>>
                val shapePointLists = repository.getShapePointsForRoute(routeName)
                withContext(Dispatchers.Main) {
                    // Pass the list of lists to the callback
                    onLoaded(stops, shapePointLists)
                }
            } catch (e: Exception) {
                Log.e("GtfsViewModel", "Error loading route data for $routeName", e)
                withContext(Dispatchers.Main) {
                    // Ensure empty lists are passed on error
                    onLoaded(emptyList(), emptyList())
                }
            }
        }
    }

    fun getUpcomingArrivalsForStop(stop: GtfsStop, onResult: (List<ArrivalDisplay>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()

            cachedArrivalsByStop[stop.stopId]?.let { (time, list) ->
                if (now - time < arrivalCacheDurationMillis) {
                    withContext(Dispatchers.Main) { onResult(list) }
                    return@launch
                }
            }

            val arrivals = mutableListOf<ArrivalDisplay>()
            val seenTimes = mutableSetOf<Long>()

            val realtime = fetchTripUpdateArrival(stop.stopId, now)
            realtime?.let {
                arrivals.add(it)
                seenTimes.add(it.minutesAway * 60000L + now)
            }

            if (realtime == null) {
                val interpolated = interpolateArrivalForStop(stop, now)
                interpolated?.let { arrivals.add(it) }
            }

            val staticSchedule = repository.getStaticTimetableForStop(stop.stopId)
            val staticTimes = staticSchedule.mapNotNull { timeStr ->
                parseTimeStringToFutureMillis(timeStr, now)?.let { timeStr to it }
            }

            val extras = staticTimes
                .filterNot { (_, millis) -> seenTimes.any { abs(millis - it) < 2 * 60 * 1000 } }
                .take(3 - arrivals.size)
                .map { (timeStr, millis) ->
                    ArrivalDisplay(
                        minutesAway = ((millis - now) / 60000).toInt(),
                        status = null,
                        isRealtime = false,
                        timeFormatted = formatArrivalTime(timeStr)
                    )
                }

            arrivals.addAll(extras)
            val sorted = arrivals.sortedBy { it.minutesAway }
            cachedArrivalsByStop[stop.stopId] = now to sorted

            withContext(Dispatchers.Main) {
                onResult(sorted)
            }
        }
    }

    private fun interpolateArrivalForStop(stop: GtfsStop, now: Long): ArrivalDisplay? {
        try {
            val allTripUpdates = lastFetchedTripFeed?.entityList?.filter { it.hasTripUpdate() } ?: return null
            val candidateTrips = repository.getValidTripIdsForStop(stop.stopId)

            for (entity in allTripUpdates) {
                val tripId = entity.tripUpdate.trip.tripId
                if (tripId !in candidateTrips) continue

                val stopTimes = repository.getStopTimesForTrip(tripId)
                val stopSequenceMap = stopTimes.associateBy { it.stopId }
                val currentStopTime = stopSequenceMap[stop.stopId] ?: continue
                val targetSeq = currentStopTime.stopSequence.toIntOrNull() ?: continue

                val realtimeBySeq = entity.tripUpdate.stopTimeUpdateList
                    .filter { it.hasArrival() && it.hasStopId() && stopSequenceMap.containsKey(it.stopId) }
                    .associateBy {
                        stopSequenceMap[it.stopId]?.stopSequence?.toIntOrNull() ?: -1
                    }.filterKeys { it >= 0 }

                val lower = realtimeBySeq.keys.filter { it < targetSeq }.maxOrNull() ?: continue
                val upper = realtimeBySeq.keys.filter { it > targetSeq }.minOrNull() ?: continue

                val lowerTime = realtimeBySeq[lower]?.arrival?.time?.times(1000) ?: continue
                val upperTime = realtimeBySeq[upper]?.arrival?.time?.times(1000) ?: continue

                val ratio = (targetSeq - lower).toDouble() / (upper - lower).toDouble()
                val interpolatedMillis = lowerTime + ((upperTime - lowerTime) * ratio).toLong()

                val scheduledEpoch = repository.getScheduledTimeForStop(tripId, stop.stopId) ?: continue
                val delayMin = ChronoUnit.MINUTES.between(
                    Instant.ofEpochSecond(scheduledEpoch).atZone(agencyTimezone),
                    Instant.ofEpochMilli(interpolatedMillis).atZone(agencyTimezone)
                ).toInt()

                val status = when {
                    delayMin < -2 -> "early"
                    delayMin <= 2 -> "on time"
                    else -> "late"
                }

                val minutesAway = ((interpolatedMillis - now) / 60000).toInt()
                return ArrivalDisplay(
                    minutesAway = minutesAway,
                    status = status,
                    isRealtime = true,
                    timeFormatted = Instant.ofEpochMilli(interpolatedMillis).atZone(agencyTimezone)
                        .format(DateTimeFormatter.ofPattern("h:mm a")),
                    wasInterpolated = true
                )
            }
        } catch (e: Exception) {
            Log.e("GtfsViewModel", "Interpolation failed", e)
        }

        return null
    }

    private fun fetchTripUpdateArrival(stopId: String, now: Long): ArrivalDisplay? {
        return try {
            if (lastFetchedTripFeed == null || now - lastFetchTimeMillis > 15000) {
                val url = URL("https://windsor.mapstrat.com/current/gtfrealtime_TripUpdates.bin")
                lastFetchedTripFeed = GtfsRealtime.FeedMessage.parseFrom(url.openStream())
                lastFetchTimeMillis = now
                Log.d("TripUpdateDebug", "🛰️ Refreshed TripUpdates feed at $now")
            }

            // Get valid tripIds for today’s service that actually visit this stop
            val validTripIds = repository.getValidTripIdsForStop(stopId)
            if (validTripIds.isEmpty()) {
                Log.w("TripUpdateDebug", "⚠️ No valid trips visit stopId=$stopId today.")
                return null
            }

            var bestArrival: ArrivalDisplay? = null
            var soonestTime = Long.MAX_VALUE

            lastFetchedTripFeed?.entityList
                ?.filter { it.hasTripUpdate() && validTripIds.contains(it.tripUpdate.trip.tripId) }
                ?.forEach { entity ->
                    val tripId = entity.tripUpdate.trip.tripId
                    entity.tripUpdate.stopTimeUpdateList.forEach { stu ->
                        if (stu.hasStopId() && stu.stopId == stopId && stu.hasArrival()) {
                            val arrivalEpochMillis = stu.arrival.time * 1000
                            if (arrivalEpochMillis > now && arrivalEpochMillis < soonestTime) {
                                val delayMin = stu.arrival.delay / 60
                                val status = when {
                                    delayMin < -2 -> "early"
                                    delayMin <= 2 -> "on time"
                                    else -> "late"
                                }

                                Log.d("TripUpdateDebug", "🟢 Match! tripId=$tripId stopId=$stopId ETA=${(arrivalEpochMillis - now) / 60000}min delay=$delayMin")

                                val arrivalTime = Instant.ofEpochSecond(stu.arrival.time).atZone(agencyTimezone)
                                val nowZoned = ZonedDateTime.now(agencyTimezone)

                                val minutesAway = ChronoUnit.MINUTES.between(nowZoned, arrivalTime).toInt()

                                bestArrival = ArrivalDisplay(
                                    minutesAway = minutesAway,
                                    status = status,
                                    isRealtime = true,
                                    timeFormatted = arrivalTime.format(DateTimeFormatter.ofPattern("h:mm a"))
                                )

                                soonestTime = arrivalEpochMillis
                            }
                        }
                    }
                }

            if (bestArrival == null) {
                Log.w("TripUpdateDebug", "⚠️ No valid TripUpdate arrival for stopId=$stopId")
            }

            bestArrival
        } catch (e: Exception) {
            Log.e("GtfsViewModel", "❌ Error in fetchTripUpdateArrival", e)
            null
        }
    }


    private fun fetchFallbackEta(stop: GtfsStop, now: Long): ArrivalDisplay? {
        return try {
            val url = URL("https://windsor.mapstrat.com/current/gtfrealtime_VehiclePositions.bin")
            val feed = GtfsRealtime.FeedMessage.parseFrom(url.openStream())

            feed.entityList.mapNotNull { entity ->
                val vehicle = entity.vehicle
                val tripId = vehicle.trip.tripId
                if (!tripToRouteMap.containsKey(tripId)) return@mapNotNull null

                val pos = vehicle.position
                val dist = haversineDistanceMeters(stop.lat, stop.lon, pos.latitude.toDouble(), pos.longitude.toDouble())
                if (dist > 1500) return@mapNotNull null

                val eta = now + (dist / 8.3 * 1000).toLong()
                ArrivalDisplay(((eta - now) / 60000).toInt(), "nearby", true)
            }.minByOrNull { it.minutesAway }

        } catch (e: Exception) {
            Log.e("GtfsViewModel", "Error in fallback ETA", e)
            null
        }
    }

    private fun parseTimeStringToFutureMillis(timeStr: String, now: Long): Long? {
        return try {
            val parts = timeStr.split(":")
            if (parts.size == 3) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                    set(Calendar.MINUTE, parts[1].toInt())
                    set(Calendar.SECOND, parts[2].toInt())
                }
                val result = cal.timeInMillis
                if (result > now) result else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun registerStopsForRoute(route: UiRoute, stops: List<GtfsStop>) {
        stops.forEach { stop ->
            if (!stopToRouteColorMap.containsKey(stop.stopId)) {
                stopToRouteColorMap[stop.stopId] = route.color
            }
        }
    }

    fun getRouteColorForStop(stopId: String): Color {
        return stopToRouteColorMap[stopId] ?: Color.Gray
    }

    fun getStaticTimetableForStop(stopId: String, onResult: (List<String>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rawTimes = repository.getStaticTimetableForStop(stopId)
                val formattedTimes = rawTimes.filter { it.isNotBlank() }.map { formatArrivalTime(it) }
                val sortedTimes = sortTimetableTimes(formattedTimes)
                withContext(Dispatchers.Main) {
                    onResult(sortedTimes)
                }
            } catch (e: Exception) {
                Log.e("GtfsViewModel", "Error loading static timetable", e)
                withContext(Dispatchers.Main) {
                    onResult(emptyList())
                }
            }
        }
    }

    fun forceRefreshGtfs(onFinished: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = GtfsDownloader.downloadGtfsFiles(getApplication())
                if (success) {
                    detailedDataLoaded = false // Reset so that detailed reload happens
                    loadRoutesOnly()
                }
                withContext(Dispatchers.Main) {
                    onFinished(success)
                }
            } catch (e: Exception) {
                Log.e("GtfsViewModel", "Force refresh failed", e)
                withContext(Dispatchers.Main) {
                    onFinished(false)
                }
            }
        }
    }

    fun isBusNearStop(stopLat: Double, stopLon: Double, radiusMeters: Double = 200.0, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://windsor.mapstrat.com/current/gtfrealtime_VehiclePositions.bin")
                val inputStream = url.openStream()
                val feed = GtfsRealtime.FeedMessage.parseFrom(inputStream)

                val isNearby = feed.entityList.any { entity ->
                    if (entity.hasVehicle()) {
                        val pos = entity.vehicle.position
                        val dist = haversineDistanceMeters(
                            stopLat,
                            stopLon,
                            pos.latitude.toDouble(),
                            pos.longitude.toDouble()
                        )
                        dist <= radiusMeters
                    } else false
                }

                withContext(Dispatchers.Main) {
                    onResult(isNearby)
                }
            } catch (e: Exception) {
                Log.e("GtfsViewModel", "Error fetching VehiclePositions", e)
                withContext(Dispatchers.Main) {
                    onResult(false)
                }
            }
        }
    }

    fun getLiveBusPositions(onResult: (List<BusPosition>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://windsor.mapstrat.com/current/gtfrealtime_VehiclePositions.bin")
                val inputStream = url.openStream()
                val feed = GtfsRealtime.FeedMessage.parseFrom(inputStream)

                val buses = feed.entityList.mapNotNull { entity ->
                    if (!entity.hasVehicle()) return@mapNotNull null

                    val vehicle = entity.vehicle
                    val tripId = vehicle.trip.tripId
                    val label = vehicle.vehicle.label
                    val occupancyStatus = vehicle.occupancyStatus.name.lowercase()

                    val resolvedTripId = when {
                        !tripId.isNullOrBlank() -> tripId
                        !label.isNullOrBlank() -> "Tri$label"
                        else -> null
                    }

                    if (resolvedTripId != null) {
                        val routeId = tripToRouteMap[resolvedTripId]
                        if (routeId != null) {
                            return@mapNotNull BusPosition(
                                vehicleId = vehicle.vehicle.id,
                                routeId = routeId,
                                tripId = resolvedTripId,
                                lat = vehicle.position.latitude.toDouble(),
                                lon = vehicle.position.longitude.toDouble(),
                                bearing = if (vehicle.position.hasBearing()) vehicle.position.bearing else null,
                                label = label,
                                occupancyStatus = occupancyStatus
                            )
                        } else {
                            Log.w("LiveMap", "❌ tripId=$resolvedTripId isn't in tripToRouteMap. Might be due to expired GTFS?")
                            return@mapNotNull null
                        }
                    } else {
                        Log.w("LiveMap", "⚠️ Vehicle ${vehicle.vehicle.id} missing tripId and fallback label")
                        return@mapNotNull null
                    }
                }

                withContext(Dispatchers.Main) {
                    Log.d("LiveMap", "Found ${buses.size} buses total")
                    onResult(buses)
                }
            } catch (e: Exception) {
                Log.e("LiveMap", "🚨 Error fetching vehicle positions", e)
                withContext(Dispatchers.Main) {
                    onResult(emptyList())
                }
            }
        }
    }

    private fun haversineDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * R * atan2(sqrt(a), sqrt(1 - a))
    }
}