package com.example.busschedule.model

data class GtfsCalendarDate(
    val serviceId: String,
    val date: String,
    val exceptionType: Int // 1 = added service, 2 = removed service
)