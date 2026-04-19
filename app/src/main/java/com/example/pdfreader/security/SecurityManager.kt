package com.example.pdfreader.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Manages app lock security — PIN code stored in encrypted preferences,
 * with biometric as the primary unlock method.
 */
class SecurityManager(context: Context) {

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                "narrately_secure_prefs",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (_: Exception) {
            // Fallback to regular prefs if encryption fails (old devices)
            context.getSharedPreferences("narrately_secure_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    // ─── App Lock ───
    var isAppLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_LOCK_ENABLED, value).apply()

    var pinCode: String?
        get() = prefs.getString(KEY_PIN, null)
        set(value) = prefs.edit().putString(KEY_PIN, value).apply()

    var useBiometric: Boolean
        get() = prefs.getBoolean(KEY_USE_BIOMETRIC, true)
        set(value) = prefs.edit().putBoolean(KEY_USE_BIOMETRIC, value).apply()

    var autoLockTimeoutSeconds: Int
        get() = prefs.getInt(KEY_TIMEOUT, 30)
        set(value) = prefs.edit().putInt(KEY_TIMEOUT, value).apply()

    // ─── User Preferences (non-sensitive) ───
    var defaultSpeed: Float
        get() = prefs.getFloat(KEY_DEFAULT_SPEED, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_DEFAULT_SPEED, value).apply()

    var defaultPitch: Float
        get() = prefs.getFloat(KEY_DEFAULT_PITCH, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_DEFAULT_PITCH, value).apply()

    var fontSize: Float
        get() = prefs.getFloat(KEY_FONT_SIZE, 15f)
        set(value) = prefs.edit().putFloat(KEY_FONT_SIZE, value).apply()

    // ─── Background timestamps (for auto-lock) ───
    var lastBackgroundTimestamp: Long
        get() = prefs.getLong(KEY_LAST_BG, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BG, value).apply()

    fun shouldLockOnResume(): Boolean {
        if (!isAppLockEnabled) return false
        val elapsed = System.currentTimeMillis() - lastBackgroundTimestamp
        return elapsed > autoLockTimeoutSeconds * 1000L
    }

    fun verifyPin(entered: String): Boolean {
        return pinCode == entered
    }

    fun setupLock(pin: String, biometric: Boolean) {
        pinCode = pin
        useBiometric = biometric
        isAppLockEnabled = true
    }

    fun removeLock() {
        isAppLockEnabled = false
        pinCode = null
    }

    companion object {
        private const val KEY_LOCK_ENABLED = "lock_enabled"
        private const val KEY_PIN = "pin_code"
        private const val KEY_USE_BIOMETRIC = "use_biometric"
        private const val KEY_TIMEOUT = "auto_lock_timeout"
        private const val KEY_DEFAULT_SPEED = "default_speed"
        private const val KEY_DEFAULT_PITCH = "default_pitch"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_LAST_BG = "last_background_ts"
    }
}
