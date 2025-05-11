package com.example.busschedule.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.busschedule.model.WeekSchedule
import com.example.busschedule.ui.theme.FontColor
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import com.example.busschedule.model.AppDayOfWeek
import com.example.busschedule.model.FavoriteStop

// DataStore setup
private const val PREF_NAME = "theme_prefs"
val Context.dataStore by preferencesDataStore(name = PREF_NAME)

// Preference keys
private val FONT_COLOR_KEY = stringPreferencesKey("font_color")
private val HOME_LOCATION_KEY = stringPreferencesKey("home_location")
private val WORK_LOCATION_KEY = stringPreferencesKey("work_location")
private val SHOW_WEATHER_KEY = booleanPreferencesKey("show_weather")
private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
private val WEEKLY_SCHEDULE_KEY = stringPreferencesKey("weekly_schedule")
private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
private val FAVE_STOPS_KEY = stringPreferencesKey("favorite_stops")

class ThemePreferences(private val context: Context) {

    private val gson = Gson()

    val fontColorFlow: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[FONT_COLOR_KEY] ?: FontColor.PASTEL_BLUE.name }

    suspend fun saveFontColorSetting(name: String) {
        context.dataStore.edit { prefs -> prefs[FONT_COLOR_KEY] = name }
    }

    val darkModeFlow: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[DARK_MODE_KEY] ?: false }

    suspend fun saveDarkModeSetting(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[DARK_MODE_KEY] = enabled }
    }

    val showWeatherFlow: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[SHOW_WEATHER_KEY] ?: true }

    suspend fun saveShowWeatherSetting(show: Boolean) {
        context.dataStore.edit { prefs -> prefs[SHOW_WEATHER_KEY] = show }
    }

    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[NOTIFICATIONS_ENABLED_KEY] ?: true }

    suspend fun saveNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[NOTIFICATIONS_ENABLED_KEY] = enabled }
    }

    val homeLocationFlow: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[HOME_LOCATION_KEY] ?: "" }

    suspend fun saveHomeLocation(value: String) {
        context.dataStore.edit { prefs -> prefs[HOME_LOCATION_KEY] = value }
    }

    val workLocationFlow: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[WORK_LOCATION_KEY] ?: "" }

    suspend fun saveWorkLocation(value: String) {
        context.dataStore.edit { prefs -> prefs[WORK_LOCATION_KEY] = value }
    }

    // ✅ WEEKLY SCHEDULE SUPPORT
    val weeklyScheduleFlow: Flow<WeekSchedule> = context.dataStore.data
        .map { prefs ->
            val json = prefs[WEEKLY_SCHEDULE_KEY]
            if (json.isNullOrEmpty()) WeekSchedule()
            else gson.fromJson(json, object : TypeToken<WeekSchedule>() {}.type)
        }

    suspend fun saveWeeklySchedule(schedule: WeekSchedule) {
        val json = gson.toJson(schedule)
        context.dataStore.edit { prefs -> prefs[WEEKLY_SCHEDULE_KEY] = json }
    }

    val favoriteStopsFlow: Flow<List<FavoriteStop>> = context.dataStore.data.map { prefs ->
        val json = prefs[FAVE_STOPS_KEY] ?: return@map emptyList()
        gson.fromJson(json, object : TypeToken<List<FavoriteStop>>() {}.type)
    }

    suspend fun saveFavoriteStops(stops: List<FavoriteStop>) {
        val json = gson.toJson(stops)
        context.dataStore.edit { prefs -> prefs[FAVE_STOPS_KEY] = json }
    }
}