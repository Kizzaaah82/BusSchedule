package com.example.busschedule.util

import android.util.Log
import com.example.busschedule.model.WeatherApiResponse
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WeatherUtils {
    private val client = OkHttpClient()

    suspend fun getWeather(city: String, apiKey: String): WeatherApiResponse? = withContext(Dispatchers.IO) {
        val url = "https://api.weatherapi.com/v1/current.json?q=$city&key=$apiKey"

        Log.d("WEATHER_API", "Fetching weather for $city with API key: $apiKey")

        val request = Request.Builder().url(url).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("WEATHER_API", "API Error: ${response.code} ${response.message}")
                    return@withContext null
                }

                val body = response.body?.string()
                Log.d("WEATHER_API", "API Response: $body")
                return@withContext Gson().fromJson(body, WeatherApiResponse::class.java)
            }
        } catch (e: Exception) {
            Log.e("WEATHER_API", "Exception: ${e.message}", e)
            return@withContext null
        }
    }
}