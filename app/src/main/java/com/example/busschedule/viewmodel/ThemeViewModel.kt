package com.example.busschedule.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.busschedule.data.ThemePreferences
import com.example.busschedule.model.AppDayOfWeek
import com.example.busschedule.model.BusSchedule
import com.example.busschedule.model.FavoriteStop
import com.example.busschedule.model.GtfsStop
import com.example.busschedule.model.WeekSchedule
import com.example.busschedule.ui.theme.FontColor
import com.example.busschedule.util.RoastWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.util.concurrent.TimeUnit

class ThemeViewModel(application: Application) : AndroidViewModel(application) {

    private val themePreferences = ThemePreferences(application)
    private val appContext = application.applicationContext

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    private val _isThemeInitialized = MutableStateFlow(false)
    val isThemeInitialized: StateFlow<Boolean> = _isThemeInitialized

    private val _fontColor = MutableStateFlow(FontColor.PASTEL_BLUE)
    val fontColor: StateFlow<FontColor> = _fontColor

    private val _homeLocation = MutableStateFlow("")
    val homeLocation: StateFlow<String> = _homeLocation

    private val _workLocation = MutableStateFlow("")
    val workLocation: StateFlow<String> = _workLocation

    private val _showWeather = MutableStateFlow(true)
    val showWeather: StateFlow<Boolean> = _showWeather

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled

    private val _weeklySchedule = MutableStateFlow(WeekSchedule())
    val weeklySchedule: StateFlow<WeekSchedule> = _weeklySchedule

    val favoriteStops: StateFlow<List<FavoriteStop>> = themePreferences.favoriteStopsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            _isDarkMode.value = themePreferences.darkModeFlow.first()
            _notificationsEnabled.value = themePreferences.notificationsEnabledFlow.first()
            _fontColor.value = FontColor.valueOf(themePreferences.fontColorFlow.first())
            _homeLocation.value = themePreferences.homeLocationFlow.first()
            _workLocation.value = themePreferences.workLocationFlow.first()
            _showWeather.value = themePreferences.showWeatherFlow.first()
            _weeklySchedule.value = themePreferences.weeklyScheduleFlow.first()

            _isThemeInitialized.value = true

            if (_notificationsEnabled.value) {
                scheduleRoastWorker()
            }
        }
    }

    fun updateFontColor(color: FontColor) {
        _fontColor.value = color
        viewModelScope.launch {
            themePreferences.saveFontColorSetting(color.name)
        }
    }

    fun toggleTheme() {
        val newValue = !_isDarkMode.value
        _isDarkMode.value = newValue
        viewModelScope.launch {
            themePreferences.saveDarkModeSetting(newValue)
        }
    }

    fun updateShowWeather(value: Boolean) {
        _showWeather.value = value
        viewModelScope.launch {
            themePreferences.saveShowWeatherSetting(value)
        }
    }

    fun updateHomeLocation(value: String) {
        _homeLocation.value = value
        viewModelScope.launch {
            themePreferences.saveHomeLocation(value)
        }
    }

    fun updateWorkLocation(value: String) {
        _workLocation.value = value
        viewModelScope.launch {
            themePreferences.saveWorkLocation(value)
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        if (_notificationsEnabled.value == enabled) return

        _notificationsEnabled.value = enabled
        viewModelScope.launch {
            themePreferences.saveNotificationsEnabled(enabled)

            if (enabled) {
                scheduleRoastWorker()
            } else {
                cancelRoastWorker()
            }
        }
    }

    fun updateScheduleForDay(day: AppDayOfWeek, schedule: BusSchedule) {
        val updatedMap = _weeklySchedule.value.dailySchedule.toMutableMap()
        updatedMap[day] = schedule
        val newWeek = WeekSchedule(updatedMap)
        _weeklySchedule.value = newWeek

        viewModelScope.launch {
            themePreferences.saveWeeklySchedule(newWeek)
        }
    }

    fun toggleFavoriteStop(stop: GtfsStop) {
        viewModelScope.launch {
            val currentFaves = favoriteStops.value.toMutableList()
            val alreadyFaved = currentFaves.any { it.stopId == stop.stopId }

            if (alreadyFaved) {
                currentFaves.removeAll { it.stopId == stop.stopId }
            } else {
                currentFaves.add(
                    FavoriteStop(
                        stopId = stop.stopId,
                        name = stop.name,
                        lat = stop.lat,
                        lon = stop.lon
                    )
                )
            }

            themePreferences.saveFavoriteStops(currentFaves)
        }
    }

    fun resetSettings() {
        viewModelScope.launch {
            cancelRoastWorker()

            val defaultDarkMode = true
            val defaultFontColor = FontColor.PASTEL_BLUE
            val defaultNotificationsEnabled = true
            val defaultSchedule = WeekSchedule()

            themePreferences.saveDarkModeSetting(defaultDarkMode)
            themePreferences.saveFontColorSetting(defaultFontColor.name)
            themePreferences.saveHomeLocation("")
            themePreferences.saveWorkLocation("")
            themePreferences.saveShowWeatherSetting(true)
            themePreferences.saveNotificationsEnabled(defaultNotificationsEnabled)
            themePreferences.saveWeeklySchedule(defaultSchedule)

            _isDarkMode.value = defaultDarkMode
            _fontColor.value = defaultFontColor
            _homeLocation.value = ""
            _workLocation.value = ""
            _showWeather.value = true
            _notificationsEnabled.value = defaultNotificationsEnabled
            _weeklySchedule.value = defaultSchedule

            if (defaultNotificationsEnabled) {
                scheduleRoastWorker()
            }
        }
    }

    private fun scheduleRoastWorker() {
        val workManager = WorkManager.getInstance(appContext)

        val request = PeriodicWorkRequestBuilder<RoastWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag("roast_worker")
            .build()

        workManager.enqueueUniquePeriodicWork(
            "roast_worker_unique",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun cancelRoastWorker() {
        WorkManager.getInstance(appContext).cancelAllWorkByTag("roast_worker")
    }
}