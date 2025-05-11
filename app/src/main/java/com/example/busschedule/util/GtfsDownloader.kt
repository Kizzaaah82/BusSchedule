package com.example.busschedule.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import androidx.core.content.edit

object GtfsDownloader {

    private val FILES_TO_DOWNLOAD = listOf(
        "calendar.txt",
        "calendar_dates.txt",
        "routes.txt",
        "shapes.txt",
        "stop_times.txt",
        "stops.txt",
        "trips.txt"
    )

    private const val BASE_URL = "https://raw.githubusercontent.com/Kizzaaah82/gtfs/main/"
    private const val PREF_NAME = "gtfs_prefs"
    private const val KEY_LAST_DOWNLOAD = "last_download_timestamp"

    suspend fun downloadGtfsFiles(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val gtfsFolder = File(context.filesDir, "gtfs")
                if (!gtfsFolder.exists()) {
                    if (!gtfsFolder.mkdirs()) {
                        Log.e("GtfsDownloader", "Failed to create GTFS folder!")
                        return@withContext false
                    }
                }

                for (fileName in FILES_TO_DOWNLOAD) {
                    val success = downloadSingleFile(context, fileName, gtfsFolder)
                    if (!success) {
                        Log.e("GtfsDownloader", "Failed to download $fileName")
                        return@withContext false
                    }
                }

                saveLastDownloadTime(context)
                Log.d("GtfsDownloader", "✅ All GTFS files downloaded successfully!")
                true
            } catch (e: Exception) {
                Log.e("GtfsDownloader", "Error downloading GTFS files", e)
                false
            }
        }
    }

    private fun downloadSingleFile(context: Context, fileName: String, folder: File): Boolean {
        return try {
            val url = URL(BASE_URL + fileName)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.e("GtfsDownloader", "Server error for $fileName: ${connection.responseCode}")
                return false
            }

            val localFile = File(folder, fileName)
            connection.inputStream.use { input ->
                localFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            Log.d("GtfsDownloader", "✅ Downloaded $fileName")
            true
        } catch (e: Exception) {
            Log.e("GtfsDownloader", "Exception downloading $fileName", e)
            false
        }
    }

    fun getLocalGtfsFile(context: Context, filename: String): File? {
        val file = File(context.filesDir, "gtfs/$filename")
        return if (file.exists()) file else null
    }

    fun shouldDownloadNewGtfsFiles(context: Context): Boolean {
        val lastDownload = getLastDownloadTime(context)
        if (lastDownload == 0L) return true

        val now = System.currentTimeMillis()
        val sevenDaysMillis = 7 * 24 * 60 * 60 * 1000L
        return now - lastDownload >= sevenDaysMillis
    }

    private fun saveLastDownloadTime(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit { putLong(KEY_LAST_DOWNLOAD, System.currentTimeMillis()) }
    }

    private fun getLastDownloadTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_DOWNLOAD, 0L)
    }

    fun getDaysSinceLastDownload(context: Context): Int {
        val lastTime = getLastDownloadTime(context)
        if (lastTime == 0L) return -1
        val diffMillis = System.currentTimeMillis() - lastTime
        return TimeUnit.MILLISECONDS.toDays(diffMillis).toInt()
    }
}