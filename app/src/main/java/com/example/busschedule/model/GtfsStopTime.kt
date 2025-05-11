package com.example.busschedule.model

data class GtfsStopTime(
    val tripId: String,
    val stopId: String,
    val arrivalTime: String,
    val stopSequence: String
)