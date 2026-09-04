package com.naze.vault.security

import android.content.Context
import android.content.SharedPreferences

/**
 * Gate that stands between "app process is alive" and "user may see vault
 * contents". The PIN itself is never stored in plaintext — it's run through
 * CryptoManager (Keystore-backed AES-GCM) just like any other secret.
 */
object VaultLockManager {

    private const val PREFS_NAME = "naze_vault_lock_prefs"
    private const val KEY_PIN_CIPHER = "pin_cipher"
    private const val KEY_PIN_IV = "pin_iv"
    private const val KEY_AUTO_LOCK_MINUTES = "auto_lock_minutes"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"

    private lateinit var prefs: SharedPreferences

    /** In-memory only — resets to locked whenever the process restarts. */
    var isUnlockedThisSession: Boolean = false
    var lastBackgroundedAtMillis: Long = 0L

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun hasPinConfigured(): Boolean = prefs.contains(KEY_PIN_CIPHER)

    fun setPin(pin: String) {
        val encrypted = CryptoManager.encrypt(pin)
        prefs.edit()
            .putString(KEY_PIN_CIPHER, encrypted.cipherTextB64)
            .putString(KEY_PIN_IV, encrypted.ivB64)
            .apply()
    }

    fun verifyPin(candidate: String): Boolean {
        val cipherText = prefs.getString(KEY_PIN_CIPHER, null) ?: return false
        val iv = prefs.getString(KEY_PIN_IV, null) ?: return false
        return runCatching { CryptoManager.decrypt(cipherText, iv) == candidate }.getOrDefault(false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)

    /** 0 = never auto-lock; otherwise minutes of background time before re-locking. */
    fun setAutoLockMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_AUTO_LOCK_MINUTES, minutes).apply()
    }

    fun getAutoLockMinutes(): Int = prefs.getInt(KEY_AUTO_LOCK_MINUTES, 1)

    fun lockNow() {
        isUnlockedThisSession = false
    }

    fun unlock() {
        isUnlockedThisSession = true
    }

    fun onAppBackgrounded() {
        lastBackgroundedAtMillis = System.currentTimeMillis()
    }

    /** Call when the app returns to foreground; applies the auto-lock policy. */
    fun onAppForegrounded() {
        val minutes = getAutoLockMinutes()
        if (minutes <= 0 || lastBackgroundedAtMillis == 0L) return
        val elapsedMinutes = (System.currentTimeMillis() - lastBackgroundedAtMillis) / 60000
        if (elapsedMinutes >= minutes) {
            lockNow()
        }
    }
}
