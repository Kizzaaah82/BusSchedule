package com.example.busschedule.util

import android.content.Context
import com.google.transit.realtime.GtfsRealtime
import java.io.InputStream

object BusDelayChecker {

    // Call this with context + stopId you want to check
    fun getDelaysForStops(context: Context, stopIds: List<String>): Map<String, Int> {
        val delayMap = mutableMapOf<String, Int>()

        try {
            // Load the .bin file from assets or local storage
            val inputStream: InputStream = context.assets.open("gtfrealtime_TripUpdates.bin")
            val feed = GtfsRealtime.FeedMessage.parseFrom(inputStream)

            for (entity in feed.entityList) {
                if (!entity.hasTripUpdate()) continue
                val tripUpdate = entity.tripUpdate

                for (stopTimeUpdate in tripUpdate.stopTimeUpdateList) {
                    val stopId = stopTimeUpdate.stopId

                    if (stopIds.contains(stopId)) {
                        val delaySeconds = stopTimeUpdate.arrival?.delay ?: 0
                        val delayMinutes = delaySeconds / 60
                        delayMap[stopId] = delayMinutes
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return delayMap
    }
}