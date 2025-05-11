package com.example.busschedule.util

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.busschedule.data.ThemePreferences
import com.example.busschedule.model.toAppDayOfWeek
import com.example.busschedule.util.NotificationUtils.sendNotification
import kotlinx.coroutines.flow.first
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter

class RoastWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val prefs = ThemePreferences(appContext)
    private val context = appContext

    override suspend fun doWork(): Result {
        val weeklySchedule = prefs.weeklyScheduleFlow.first()
        val enabled = prefs.notificationsEnabledFlow.first()

        if (!enabled) {
            Log.d("RoastWorker", "Notifications disabled. Skipping.")
            return Result.success()
        }

        val today = org.threeten.bp.LocalDate.now().dayOfWeek.toAppDayOfWeek()
        val schedule = weeklySchedule.dailySchedule[today] ?: return Result.success()
        val now = LocalTime.now()

        val formatter = DateTimeFormatter.ofPattern("hh:mm a")

        val homeTime = try {
            LocalTime.parse(schedule.homeToSchoolTime, formatter)
        } catch (e: Exception) {
            null
        }

        val workTime = try {
            LocalTime.parse(schedule.schoolToHomeTime, formatter)
        } catch (e: Exception) {
            null
        }

        val bufferMinutes = 30

        val isNearHomeTime = homeTime?.let {
            now.isAfter(it.minusMinutes(bufferMinutes.toLong())) &&
                    now.isBefore(it.plusMinutes(10))
        } ?: false

        val isNearWorkTime = workTime?.let {
            now.isAfter(it.minusMinutes(bufferMinutes.toLong())) &&
                    now.isBefore(it.plusMinutes(10))
        } ?: false

        if (isNearHomeTime || isNearWorkTime) {
            val stopIds = listOf(
                prefs.homeLocationFlow.first(),
                prefs.workLocationFlow.first()
            ).filter { it.isNotBlank() }

            val delayMap = BusDelayChecker.getDelaysForStops(context, stopIds)

            val anyDelayed = delayMap.values.any { delay -> delay >= 5 }

            if (anyDelayed) {
                sendNotification(
                    context = context,
                    title = "Bus Sass",
                    message = getRandomRoast()
                )
                Log.d("RoastWorker", "Sent a roast (bus was late).")
            } else {
                Log.d("RoastWorker", "No delays detected. No roast today.")
            }

        } else {
            Log.d("RoastWorker", "Outside commute window. No roast today.")
        }

        return Result.success()
    }

    private fun getRandomRoast(): String {
        val roasts = listOf(
            "Reminder: You still suck at being on time.",
            "Your bus is delayed. Probably ran away from your personality.",
            "Running late? So is your ambition.",
            "Transit’s here. Are you though?",
            "Still waiting on that bus? Same energy as your life plans."
        )
        return roasts.random()
    }
}