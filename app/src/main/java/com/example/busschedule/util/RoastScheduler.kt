package com.example.busschedule.util

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log // Optional: for logging
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.example.busschedule.receiver.RoastReceiver

object RoastScheduler {

    private const val ROAST_REQUEST_CODE = 0 // Use a constant for the request code

    fun scheduleSingleRoast(context: Context, delayMillis: Long = 60_000L) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.w("RoastScheduler", "Exact alarms not supported on this API level.")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 💥 Android 12+ check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w("RoastScheduler", "Exact alarm permission not granted. Roast not scheduled.")
                return
            }
        }

        val intent = Intent(context, com.example.busschedule.receiver.RoastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ROAST_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val triggerTime = SystemClock.elapsedRealtime() + delayMillis

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerTime,
                pendingIntent
            )
            Log.d("RoastScheduler", "Scheduling single roast in ${delayMillis / 1000} seconds.")
        } catch (e: SecurityException) {
            Log.e("RoastScheduler", "Failed to schedule roast: ${e.message}")
        }
    }

    // --- NEW FUNCTION ---
    fun cancelRoast(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Create an Intent and PendingIntent that *exactly match* the one used for scheduling
        val intent = Intent(context, RoastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ROAST_REQUEST_CODE, // Use the SAME request code
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT // Use the SAME flags
            // Note: FLAG_NO_CREATE could also be used here if you only want to cancel if it exists,
            // but FLAG_UPDATE_CURRENT is fine and consistent with scheduling.
        )

        Log.d("RoastScheduler", "Cancelling roast alarm.") // Optional logging
        alarmManager.cancel(pendingIntent)
    }
    // --- END NEW FUNCTION ---
}