package com.example.busschedule.model

data class BusSchedule(
    val homeToSchoolTime: String = "",
    val schoolToHomeTime: String = ""
)

data class WeekSchedule(
    val dailySchedule: Map<AppDayOfWeek, BusSchedule> = defaultWeekSchedule()
)

fun defaultWeekSchedule(): Map<AppDayOfWeek, BusSchedule> {
    return AppDayOfWeek.entries.associateWith { BusSchedule() }
}