package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.GroupEntity
import com.example.data.normalizeArabicDay
import java.util.Calendar

object AlarmScheduler {

    fun scheduleAlarmForGroup(context: Context, group: GroupEntity) {
        val scheduleList = listOfNotNull(
            validSchedule(group.day1, group.timeSlot),
            validSchedule(group.day2, group.timeSlot2 ?: group.timeSlot),
            validSchedule(group.day3, group.timeSlot3 ?: group.timeSlot),
            validSchedule(group.day4, group.timeSlot4 ?: group.timeSlot),
            validSchedule(group.day5, group.timeSlot5 ?: group.timeSlot),
            validSchedule(group.day6, group.timeSlot6 ?: group.timeSlot),
            validSchedule(group.day7, group.timeSlot7 ?: group.timeSlot)
        ).distinct()

        for ((day, time) in scheduleList) {
            scheduleNextClassOccurrence(context, group.id, group.name, group.subject, day, time)
        }
    }

    fun scheduleNextClassOccurrence(
        context: Context,
        groupId: Int,
        groupName: String,
        subject: String,
        dayName: String,
        timeText: String
    ) {
        val triggerTime = calculateNextTriggerMillis(dayName, timeText) ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
            putExtra("GROUP_ID", groupId)
            putExtra("GROUP_NAME", groupName)
            putExtra("SUBJECT", subject)
            putExtra("DAY_NAME", dayName)
            putExtra("TIME_TEXT", timeText)
        }
        val requestCode = groupId * 100 + dayToCalendarDay(dayName)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (_: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    private fun validSchedule(day: String?, time: String?): Pair<String, String>? {
        if (day.isNullOrBlank() || time.isNullOrBlank() || normalizeArabicDay(day).isBlank()) return null
        return day to time
    }

    fun triggerTestNotification(context: Context, group: GroupEntity) {
        NotificationHelper.showClassNotification(
            context = context,
            groupName = group.name,
            subject = group.subject,
            timeText = group.timeSlot
        )
    }

    fun scheduleDailyBackupReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyBackupAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            9001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21) // 9:00 PM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If 9 PM today has already passed, schedule for tomorrow 9 PM
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val triggerTime = calendar.timeInMillis

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    private fun calculateNextTriggerMillis(dayName: String, timeSlot: String): Long? {
        val targetDay = dayToCalendarDay(dayName)
        val (hour24, minute) = parseTimeSlot(timeSlot) ?: return null

        val now = Calendar.getInstance()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour24)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // 10 minutes before class
            add(Calendar.MINUTE, -10)
        }

        // Adjust day of week
        while (calendar.get(Calendar.DAY_OF_WEEK) != targetDay || calendar.before(now)) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return calendar.timeInMillis
    }

    private fun dayToCalendarDay(dayName: String): Int {
        val norm = normalizeArabicDay(dayName)
        return when {
            norm.contains("احد") -> Calendar.SUNDAY
            norm.contains("ثنين") || norm.contains("اثنان") -> Calendar.MONDAY
            norm.contains("ثلاثاء") -> Calendar.TUESDAY
            norm.contains("ربعاء") -> Calendar.WEDNESDAY
            norm.contains("خميس") -> Calendar.THURSDAY
            norm.contains("جمعة") -> Calendar.FRIDAY
            else -> Calendar.SATURDAY // "السبت"
        }
    }

    private fun parseTimeSlot(timeSlot: String): Pair<Int, Int>? {
        // Examples: "04:00 م", "08:30 ص", "16:00"
        return try {
            val isPm = timeSlot.contains("م")
            val clean = timeSlot.replace("م", "").replace("ص", "").trim()
            val parts = clean.split(":")
            var hour = parts[0].trim().toInt()
            val min = parts[1].trim().toInt()

            if (isPm && hour < 12) hour += 12
            if (!isPm && timeSlot.contains("ص") && hour == 12) hour = 0

            Pair(hour, min)
        } catch (e: Exception) {
            Pair(16, 0)
        }
    }
}
