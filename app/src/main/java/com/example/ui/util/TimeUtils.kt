package com.example.ui.util

import java.util.Locale

object TimeUtils {
    fun parseTime(time: String): Int {
        // Expected format "hh:mm ص/م" or "hh:mm ص/م" (e.g., "07:00 م")
        try {
            val parts = time.split(" ")
            if (parts.size < 2) return 0
            val hhmm = parts[0].split(":")
            var hour = hhmm[0].toInt()
            val minute = hhmm[1].toInt()
            val amPm = parts[1] // "ص" or "م"

            if (amPm == "م" && hour != 12) hour += 12
            if (amPm == "ص" && hour == 12) hour = 0
            return hour * 60 + minute
        } catch (e: Exception) {
            return 0
        }
    }
}
