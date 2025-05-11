package com.example.busschedule.model

data class GtfsShape(
    val shapeId: String,
    val lat: Double,
    val lon: Double,
    val sequence: Int
)