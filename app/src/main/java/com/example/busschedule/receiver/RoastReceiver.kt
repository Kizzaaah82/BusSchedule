package com.example.busschedule.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.busschedule.util.NotificationUtils

class RoastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val roastLines = listOf(
            "It’s cold out. But not as cold as your social life.",
            "Reminder: You still suck at being on time.",
            "Running late? So is your motivation.",
            "Your bus is delayed. Probably ran away from your personality.",
            "This bus is the only thing that’ll pick you up today.",
            "Reminder: The bus has a schedule. Unlike your life.",
            "It’s cold out. Perfect match for your emotional availability.",
            "Delay alert: Your ride’s stuck in traffic. Just like your potential.",
            "The weather's shit. But not as shit as your time management.",
            "You might wanna leave early. Not that it ever helped your GPA.",
            "Today’s forecast: 90% chance of disappointment, 100% chance you deserve it.",
            "Your stop is coming. Prepare to disappoint the world… again.",
            "If this notification surprised you, imagine how surprised your bus is to see you actually trying."
        )

        val randomRoast = roastLines.random()

        NotificationUtils.sendNotification(
            context = context,
            title = "Transit Sass",
            message = randomRoast
        )
    }
}