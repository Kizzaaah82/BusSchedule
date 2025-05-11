package com.example.busschedule.model

data class GtfsStop(
    val stopId: String,
    val stopCode: String,
    val name: String,
    val lat: Double,
    val lon: Double
)