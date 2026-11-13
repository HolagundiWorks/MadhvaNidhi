package com.paperdesk.launcher.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Japa counts / mantra / history for the active profile.
 * At-rest AES-256 via EncryptedSharedPreferences (same pattern as [ProfileStore]).
 */
class JapaStore(private val context: Context, private val profileId: String = ProfileStore(context).active().id) {
    private val app = context.applicationContext
    private val prefs: SharedPreferences by lazy { encryptedPrefs() }
    private val prefix = "profile_${profileId}_"

    private fun encryptedPrefs(): SharedPreferences {
        migratePlaintextIfNeeded()
        return createEncrypted()
    }

    private fun createEncrypted(): SharedPreferences = SecurePrefs.get(app, PREFS_ENCRYPTED)

    private fun migratePlaintextIfNeeded() {
        val plain = app.getSharedPreferences(PREFS_PLAIN, Context.MODE_PRIVATE)
        if (plain.all.isEmpty()) return
        // Ensure profile_ prefix migration runs on plain first.
        migrateLegacyKeys(plain)
        val encrypted = createEncrypted()
        val edit = encrypted.edit()
        var copied = 0
        for ((key, value) in plain.all) {
            if (encrypted.contains(key)) continue
            when (value) {
                is String -> edit.putString(key, value)
                is Long -> edit.putLong(key, value)
                is Int -> edit.putLong(key, value.toLong())
                is Boolean -> edit.putBoolean(key, value)
                is Float -> edit.putFloat(key, value)
                else -> continue
            }
            copied++
        }
        val ok = if (copied > 0) edit.commit() else true
        if (ok) {
            // Verify at least one migrated key or plain only held flags.
            val flagsOnly = plain.all.keys.all {
                it == "profiles_migrated" || it == "lifetime_removed" || it == "encrypted_migrated"
            }
            if (flagsOnly || encrypted.all.isNotEmpty() || copied == 0) {
                plain.edit().clear().apply()
            }
        }
    }

    data class State(
        val mantra: String,
        val target: Long,
        val targetEnabled: Boolean,
        val current: Long,
        val sessions: Long
    )

    fun state(): State = State(
        prefs.getString(key("mantra"), "ಓಂ ನಮೋ ನಾರಾಯಣಾಯ") ?: "",
        prefs.getLong(key("target"), 108L).coerceAtLeast(1),
        prefs.getBoolean(key("target_enabled"), false),
        prefs.getLong(key("current"), 0L).coerceAtLeast(0),
        prefs.getLong(key("sessions"), 0L).coerceAtLeast(0)
    )

    fun configure(mantra: String, target: Long, targetEnabled: Boolean) {
        prefs.edit()
            .putString(key("mantra"), mantra.trim())
            .putLong(key("target"), target.coerceAtLeast(1))
            .putBoolean(key("target_enabled"), targetEnabled)
            .apply()
    }

    fun add(delta: Long): State {
        val s = state()
        val next = safeAdd(s.current, delta.coerceAtLeast(0))
        prefs.edit().putLong(key("current"), next).apply()
        return s.copy(current = next)
    }

    fun undo(): State {
        val s = state()
        if (s.current == 0L) return s
        val next = s.current - 1
        prefs.edit().putLong(key("current"), next).apply()
        return s.copy(current = next)
    }

    fun complete(): State {
        val s = state()
        if (s.current == 0L) return s
        val history = history()
        history.put(
            JSONObject()
                .put("at", System.currentTimeMillis())
                .put("mantra", s.mantra)
                .put("count", s.current)
        )
        while (history.length() > 100) history.remove(0)
        prefs.edit()
            .putString(key("history"), history.toString())
            .putLong(key("sessions"), s.sessions + 1)
            .putLong(key("current"), 0L)
            .apply()
        return s.copy(current = 0L, sessions = s.sessions + 1)
    }

    fun resetCurrent(): State {
        prefs.edit().putLong(key("current"), 0L).apply()
        return state().copy(current = 0L)
    }

    fun exportJson(): JSONObject {
        val s = state()
        return JSONObject()
            .put("mantra", s.mantra)
            .put("target", s.target)
            .put("targetEnabled", s.targetEnabled)
            .put("current", s.current)
            .put("sessions", s.sessions)
            .put("history", history())
    }

    fun importJson(obj: JSONObject) {
        prefs.edit()
            .putString(key("mantra"), obj.optString("mantra", "ಓಂ ನಮೋ ನಾರಾಯಣಾಯ"))
            .putLong(key("target"), obj.optLong("target", 108).coerceAtLeast(1))
            .putBoolean(key("target_enabled"), obj.optBoolean("targetEnabled", false))
            .putLong(key("current"), obj.optLong("current", 0).coerceAtLeast(0))
            .putLong(key("sessions"), obj.optLong("sessions", 0).coerceAtLeast(0))
            .putString(key("history"), obj.optJSONArray("history")?.toString() ?: "[]")
            .apply()
    }

    private fun key(name: String) = prefix + name
    private fun history(): JSONArray =
        runCatching { JSONArray(prefs.getString(key("history"), "[]")) }.getOrElse { JSONArray() }

    private fun safeAdd(a: Long, b: Long): Long =
        if (Long.MAX_VALUE - a < b) Long.MAX_VALUE else a + b

    private fun migrateLegacyKeys(prefs: SharedPreferences) {
        if (prefs.getBoolean("profiles_migrated", false)) return
        val edit = prefs.edit()
        val activeId = ProfileStore(context).active().id
        listOf("mantra", "history").forEach { name ->
            if (prefs.contains(name)) edit.putString("profile_${activeId}_$name", prefs.getString(name, null))
        }
        listOf("target", "current", "sessions").forEach { name ->
            if (prefs.contains(name)) edit.putLong("profile_${activeId}_$name", prefs.getLong(name, 0L))
        }
        prefs.all.keys.filter { it == "lifetime" || it.endsWith("_lifetime") }.forEach { edit.remove(it) }
        edit.putBoolean("profiles_migrated", true)
        edit.putBoolean("lifetime_removed", true)
        edit.commit()
    }

    companion object {
        const val PREFS_PLAIN = "paperdesk_japa"
        const val PREFS_ENCRYPTED = "paperdesk_japa_enc"
        /** @deprecated Use PREFS_PLAIN / PREFS_ENCRYPTED */
        const val PREFS = PREFS_PLAIN

        fun exportAll(context: Context): JSONObject = JSONObject().also { out ->
            ProfileStore(context).profiles().forEach { out.put(it.id, JapaStore(context, it.id).exportJson()) }
        }

        fun importAll(context: Context, obj: JSONObject) {
            obj.keys().forEach { id -> obj.optJSONObject(id)?.let { JapaStore(context, id).importJson(it) } }
        }
    }
}
