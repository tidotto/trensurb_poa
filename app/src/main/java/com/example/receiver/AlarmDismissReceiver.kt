package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.util.AlertHelper

class AlarmDismissReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DISMISS_ALARM = "com.example.receiver.ACTION_DISMISS_ALARM"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_DISMISS_ALARM) {
            Log.d("AlarmDismissReceiver", "Received dismiss alarm request from notification")
            AlertHelper.stopActiveAlarm(context)
        }
    }
}
