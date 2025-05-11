package com.example.busschedule.model

import org.threeten.bp.DayOfWeek

enum class AppDayOfWeek(val displayName: String) {
    MONDAY("Monday"),
    TUESDAY("Tuesday"),
    WEDNESDAY("Wednesday"),
    THURSDAY("Thursday"),
    FRIDAY("Friday"),
    SATURDAY("Saturday"),
    SUNDAY("Sunday");
}

fun DayOfWeek.toAppDayOfWeek(): AppDayOfWeek {
    return when (this) {
        DayOfWeek.MONDAY -> AppDayOfWeek.MONDAY
        DayOfWeek.TUESDAY -> AppDayOfWeek.TUESDAY
        DayOfWeek.WEDNESDAY -> AppDayOfWeek.WEDNESDAY
        DayOfWeek.THURSDAY -> AppDayOfWeek.THURSDAY
        DayOfWeek.FRIDAY -> AppDayOfWeek.FRIDAY
        DayOfWeek.SATURDAY -> AppDayOfWeek.SATURDAY
        DayOfWeek.SUNDAY -> AppDayOfWeek.SUNDAY
    }
}