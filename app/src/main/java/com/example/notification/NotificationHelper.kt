package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

object NotificationHelper {

    private const val CHANNEL_ID_PREFIX = "teacher_class_reminders_v5"
    private const val CHANNEL_NAME = "تنبيهات الدروس والحصص (صوت عالي)"
    private const val PREFS_NAME = "app_notification_prefs"
    private const val KEY_SOUND_TYPE = "selected_sound_type"
    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private const val KEY_SOUND_MUTED = "sound_muted"

    fun isNotificationEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setNotificationEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun isSoundMuted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SOUND_MUTED, false)
    }

    fun setSoundMuted(context: Context, muted: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SOUND_MUTED, muted).apply()
        createNotificationChannel(context)
    }

    // Sound URI string
    fun getSelectedSoundUriString(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("custom_sound_uri", null)
    }

    fun setSelectedSoundUriString(context: Context, uriString: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("custom_sound_uri", uriString).apply()
        // Unmute sound if user picks a custom sound
        setSoundMuted(context, false)
        createNotificationChannel(context)
    }

    private fun getSoundUri(context: Context): Uri? {
        if (isSoundMuted(context)) {
            return null
        }
        val customUriStr = getSelectedSoundUriString(context)
        if (customUriStr != null) {
            return Uri.parse(customUriStr)
        }
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }

    private fun activeChannelId(context: Context): String {
        val soundKey = getSoundUri(context)?.toString()?.hashCode() ?: 0
        return "${CHANNEL_ID_PREFIX}_${soundKey}_${if (isSoundMuted(context)) "muted" else "sound"}"
    }

    private var activeRingtone: android.media.Ringtone? = null

    fun playTestSound(context: Context) {
        if (isSoundMuted(context)) return
        try {
            stopSound()
            val uri = getSoundUri(context) ?: return
            activeRingtone = RingtoneManager.getRingtone(context, uri)
            activeRingtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopSound() {
        try {
            activeRingtone?.stop()
            activeRingtone = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = getSoundUri(context)
            val audioAttributes = if (soundUri != null) {
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
            } else null

            val channel = NotificationChannel(
                activeChannelId(context),
                CHANNEL_NAME,
                if (soundUri != null) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "تنبيهات تلقائية قبل بدء الحصص بـ 10 دقائق"
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 300, 800)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showClassNotification(context: Context, groupName: String, subject: String, timeText: String) {
        if (!isNotificationEnabled(context)) return

        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Stop sound PendingIntent for action button and dismissal
        val stopSoundIntent = Intent(context, StopAlarmSoundReceiver::class.java).apply {
            action = "ACTION_STOP_ALARM_SOUND"
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            stopSoundIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = getSoundUri(context)

        val notificationBuilder = NotificationCompat.Builder(context, activeChannelId(context))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("تذكير بموعد الحصة — متبقي 10 دقائق")
            .setContentText("$groupName - مادة $subject ($timeText)")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("استعد لبداية حصة $groupName في مادة $subject الساعة $timeText. يمكنك إيقاف النغمة من الزر بالأسفل.")
            )
            .setPriority(if (soundUri != null) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 800, 300, 800))
            .setContentIntent(pendingIntent)
            .setDeleteIntent(stopPendingIntent)
            .addAction(
                android.R.drawable.ic_lock_silent_mode,
                "إغلاق النغمة",
                stopPendingIntent
            )
            .setAutoCancel(true)

        if (soundUri != null) {
            notificationBuilder.setSound(soundUri)
        } else {
            notificationBuilder.setSound(null)
        }

        val notification = notificationBuilder.build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify((System.currentTimeMillis() % 10000).toInt(), notification)

        // Also play ringtone directly if sound not muted
        if (soundUri != null) {
            playTestSound(context)
        }
    }

    fun showDailyBackupReminderNotification(context: Context) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_BACKUP_DIALOG", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = getSoundUri(context)

        val notificationBuilder = NotificationCompat.Builder(context, activeChannelId(context))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("☁️ تذكير النسخ الاحتياطي اليومي (9:00 م)")
            .setContentText("حان موعد أخذ نسخة احتياطية لبيانات التطبيق لضمان أمان سجلات الطلاب والحصص.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("مساء الخير أستاذنا العزيز! حان موعد الساعة 9:00 مساءً. يرجى فتح التطبيق وأخذ نسخة احتياطية (سحابية أو ملف إكسيل) لضمان حفظ بياناتك ومستحقاتك بأمان تام. ✨")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (soundUri != null) {
            notificationBuilder.setSound(soundUri)
        }

        val notification = notificationBuilder.build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(9001, notification)
    }
}
