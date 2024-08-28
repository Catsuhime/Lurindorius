package com.example.mycalendarapp
//AlarmReceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title")
        val description = intent.getStringExtra("description")
        Toast.makeText(context, "Alarm: $title - $description", Toast.LENGTH_LONG).show()
    }
}


