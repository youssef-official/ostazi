package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StopAlarmSoundReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        NotificationHelper.stopSound()
    }
}
