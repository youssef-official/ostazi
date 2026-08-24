package com.example.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecurityManager {
    private const val PREF_NAME = "security_prefs"
    private const val KEY_SECURITY_OPTION = "security_option"
    private const val KEY_PIN = "pin"
    private const val KEY_PATTERN = "pattern"

    // Options: NONE, BIOMETRIC, PIN, PATTERN
    enum class SecurityOption { NONE, BIOMETRIC, PIN, PATTERN }

    private fun getPrefs(context: Context) = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getSecurityOption(context: Context): SecurityOption {
        val option = getPrefs(context).getString(KEY_SECURITY_OPTION, SecurityOption.NONE.name)
        return SecurityOption.valueOf(option ?: SecurityOption.NONE.name)
    }

    fun setSecurityOption(context: Context, option: SecurityOption) {
        getPrefs(context).edit().putString(KEY_SECURITY_OPTION, option.name).apply()
    }

    fun setPin(context: Context, pin: String) {
        // In real app, hash this!
        getPrefs(context).edit().putString(KEY_PIN, pin).apply()
    }

    fun getPin(context: Context): String? = getPrefs(context).getString(KEY_PIN, null)

    fun setPattern(context: Context, pattern: String) {
        // In real app, hash this!
        getPrefs(context).edit().putString(KEY_PATTERN, pattern).apply()
    }

    fun getPattern(context: Context): String? = getPrefs(context).getString(KEY_PATTERN, null)
}
