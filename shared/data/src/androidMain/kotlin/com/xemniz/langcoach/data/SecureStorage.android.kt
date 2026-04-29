package com.xemniz.langcoach.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

actual class SecureStorage(private val context: Context) {
    private val prefs by lazy {
        val masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "langcoach_secure_prefs",
            masterKey,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    actual suspend fun save(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    actual suspend fun read(key: String): String? {
        return prefs.getString(key, null)
    }

    actual suspend fun delete(key: String) {
        prefs.edit().remove(key).apply()
    }
}
