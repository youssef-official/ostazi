package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DailyBackupAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.showDailyBackupReminderNotification(context)
        // Reschedule for next day 9:00 PM
        AlarmScheduler.scheduleDailyBackupReminder(context)
    }
}
