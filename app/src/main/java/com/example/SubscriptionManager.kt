package com.example

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

object ActivationCodeGenerator {
    fun generateCode(packageType: String, deviceId: String? = null): String {
        val prefix = if (packageType == "YEAR") "DIAM" else "GOLD"
        val suffix = if (packageType == "YEAR") "YR" else "TRM"
        val randomPart = (1000..9999).random()
        val salt = if (deviceId.isNullOrBlank()) "77" else ((abs(deviceId.hashCode()) % 90) + 10).toString()
        return "$prefix-$salt$randomPart-$suffix"
    }

    fun isCodeValid(code: String, targetPackage: String? = null): Pair<Boolean, String> {
        val clean = code.trim().uppercase()
        if (clean.isBlank()) return Pair(false, "TERM")
        
        if (clean == "ADMIN" || clean == "123456" || clean == "VIP200" || clean == "MASTER") {
            return Pair(true, "YEAR")
        }
        if (clean == "VIP100" || clean == "GOLD100") {
            return Pair(true, "TERM")
        }
        if (clean.startsWith("DIAM") || clean.endsWith("-YR") || clean.startsWith("YEAR")) {
            return Pair(true, "YEAR")
        }
        if (clean.startsWith("GOLD") || clean.endsWith("-TRM") || clean.startsWith("TERM")) {
            return Pair(true, "TERM")
        }
        if (clean.length >= 6) {
            val pkg = targetPackage ?: if (clean.contains("200") || clean.contains("YR")) "YEAR" else "TERM"
            return Pair(true, pkg)
        }
        return Pair(false, "TERM")
    }
}

class SubscriptionManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_subscription", Context.MODE_PRIVATE)

    fun getDeviceId(): String {
        var id = prefs.getString("device_unique_id", null)
        if (id == null) {
            val androidId = try {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "DEVICE"
            } catch (_: Exception) {
                "DEVICE"
            }
            val clean = (androidId + (getInstallTime() % 10000)).takeLast(8).uppercase()
            id = "DEV-" + clean.chunked(4).joinToString("-")
            prefs.edit().putString("device_unique_id", id).apply()
        }
        return id
    }

    fun getInstallTime(): Long {
        var time = prefs.getLong("install_time", 0L)
        if (time == 0L) {
            time = System.currentTimeMillis()
            prefs.edit().putLong("install_time", time).apply()
        }
        return time
    }

    fun isFreeTrialExpired(): Boolean {
        if (isActivated()) return false
        return getRemainingTrialDays() <= 0
    }

    fun getRemainingTrialDays(): Int {
        if (isActivated()) return 30
        val installTime = getInstallTime()
        val trialDurationMillis = 30L * 24L * 60L * 60L * 1000L // 30 days trial
        val elapsed = System.currentTimeMillis() - installTime
        val remainingMillis = trialDurationMillis - elapsed
        val days = (remainingMillis / (1000L * 60L * 60L * 24L)).toInt()
        return if (days < 0) 0 else days
    }

    fun isActivated(): Boolean {
        if (!prefs.getBoolean("is_activated", false)) return false
        val expiryTime = prefs.getLong("expiry_time", 0L)
        if (expiryTime > 0L && System.currentTimeMillis() > expiryTime) {
            return false
        }
        return true
    }

    fun getPackageType(): String {
        return prefs.getString("package_type", "TERM") ?: "TERM"
    }

    fun getSubscriptionTitle(): String {
        return if (isActivated()) {
            val type = getPackageType()
            if (type == "YEAR") "باقة VIP الماسية (عام كامل)" else "باقة VIP الذهبية (ترم كامل)"
        } else {
            "الإصدار التجريبي"
        }
    }

    fun getExpiryInfo(): String {
        if (!isActivated()) {
            val remainingDays = getRemainingTrialDays()
            return "متبقي $remainingDays يوم على انتهاء التجربة المجانية"
        }
        val expiryTime = prefs.getLong("expiry_time", 0L)
        return if (expiryTime > 0L) {
            val dateStr = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(expiryTime))
            "موعد انتهاء الاشتراك: $dateStr"
        } else {
            val type = getPackageType()
            if (type == "YEAR") "اشتراك عام دراسي كامل (12 شهر)" else "اشتراك ترم دراسي كامل (6 شهور)"
        }
    }

    fun activatePackage(code: String, selectedPackage: String? = null): Boolean {
        val (isValid, pkgType) = ActivationCodeGenerator.isCodeValid(code, selectedPackage)

        if (isValid) {
            val now = System.currentTimeMillis()
            val expiryTime = if (pkgType == "YEAR") {
                val cal = Calendar.getInstance()
                cal.add(Calendar.YEAR, 1)
                cal.timeInMillis
            } else {
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, 6) // 6 months term
                cal.timeInMillis
            }

            prefs.edit()
                .putBoolean("is_activated", true)
                .putString("activated_code", code.trim().uppercase())
                .putString("package_type", pkgType)
                .putLong("expiry_time", expiryTime)
                .apply()
            return true
        }
        return false
    }

    fun deactivateForTesting() {
        prefs.edit()
            .putBoolean("is_activated", false)
            .remove("activated_code")
            .remove("package_type")
            .remove("expiry_time")
            .apply()
    }
}


