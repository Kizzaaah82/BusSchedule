package com.example.busschedule.model

data class GtfsTrip(
    val routeId: String,
    val serviceId: String, // Keep if needed, otherwise remove
    val tripId: String,
    val shapeId: String? // Add shapeId (nullable if not always present)
)