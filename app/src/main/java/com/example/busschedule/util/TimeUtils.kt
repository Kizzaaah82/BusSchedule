package com.example.busschedule.util

import java.util.Locale

fun parseTimeForPicker(raw: String): Pair<Int, Int> {
    return try {
        val parts = raw.replace("AM", "").replace("PM", "").trim().split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 7
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        Pair(hour, minute)
    } catch (e: Exception) {
        Pair(7, 0)
    }
}

fun formatAs12Hour(hour: Int, minute: Int): String {
    val isPM = hour >= 12
    val hour12 = if (hour % 12 == 0) 12 else hour % 12
    return String.format(Locale.US, "%02d:%02d %s", hour12, minute, if (isPM) "PM" else "AM")
}

fun formatTimeNicely(raw: String): String {
    return try {
        if (raw.contains("AM") || raw.contains("PM")) raw else {
            val parts = raw.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            val isPM = hour >= 12
            val hour12 = if (hour % 12 == 0) 12 else hour % 12
            val paddedMinute = minute.toString().padStart(2, '0')
            "$hour12:$paddedMinute ${if (isPM) "PM" else "AM"}"
        }
    } catch (e: Exception) {
        "Not Set"
    }
}