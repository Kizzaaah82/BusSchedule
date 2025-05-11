package com.example.busschedule

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen

class BusScheduleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
    }
}