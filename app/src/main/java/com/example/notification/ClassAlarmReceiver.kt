package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ClassAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!NotificationHelper.isNotificationEnabled(context)) return

        val groupName = intent.getStringExtra("GROUP_NAME") ?: "المجموعة"
        val subject = intent.getStringExtra("SUBJECT") ?: "المادة"
        val groupId = intent.getIntExtra("GROUP_ID", 0)
        val dayName = intent.getStringExtra("DAY_NAME").orEmpty()
        val timeText = intent.getStringExtra("TIME_TEXT") ?: ""

        NotificationHelper.showClassNotification(context, groupName, subject, timeText)
        if (groupId > 0 && dayName.isNotBlank() && timeText.isNotBlank()) {
            AlarmScheduler.scheduleNextClassOccurrence(
                context = context,
                groupId = groupId,
                groupName = groupName,
                subject = subject,
                dayName = dayName,
                timeText = timeText
            )
        }
    }
}
