package com.paperdesk.launcher.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Shared MasterKey + EncryptedSharedPreferences cache.
 * Creating MasterKey on every store open was costing ~1–2s on mid/low devices.
 */
object SecurePrefs {
    private val lock = Any()
    @Volatile private var masterKey: MasterKey? = null
    private val files = HashMap<String, SharedPreferences>(4)

    fun warm(context: Context) {
        runCatching { master(context.applicationContext) }
    }

    fun get(context: Context, fileName: String): SharedPreferences {
        val app = context.applicationContext
        synchronized(lock) {
            files[fileName]?.let { return it }
            val prefs = EncryptedSharedPreferences.create(
                app,
                fileName,
                master(app),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            files[fileName] = prefs
            return prefs
        }
    }

    private fun master(app: Context): MasterKey {
        masterKey?.let { return it }
        synchronized(lock) {
            masterKey?.let { return it }
            return MasterKey.Builder(app)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
                .also { masterKey = it }
        }
    }
}
