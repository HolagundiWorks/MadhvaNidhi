package com.paperdesk.launcher.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class JapaStore(private val context: Context, private val profileId: String = ProfileStore(context).active().id) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val prefix = "profile_${profileId}_"

    init { migrateLegacy() }

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
        prefs.edit().putString(key("mantra"), mantra.trim()).putLong(key("target"), target.coerceAtLeast(1))
            .putBoolean(key("target_enabled"), targetEnabled).commit()
    }

    fun add(delta: Long): State {
        val s = state()
        val safe = delta.coerceAtLeast(0)
        prefs.edit()
            .putLong(key("current"), safeAdd(s.current, safe))
            .commit()
        return state()
    }

    fun undo(): State {
        val s = state()
        if (s.current == 0L) return s
        prefs.edit().putLong(key("current"), s.current - 1).commit()
        return state()
    }

    fun complete(): State {
        val s = state()
        if (s.current == 0L) return s
        val history = history()
        history.put(JSONObject().put("at", System.currentTimeMillis()).put("mantra", s.mantra).put("count", s.current))
        while (history.length() > 100) history.remove(0)
        prefs.edit().putString(key("history"), history.toString()).putLong(key("sessions"), s.sessions + 1).putLong(key("current"), 0L).commit()
        return state()
    }

    fun resetCurrent(): State {
        prefs.edit().putLong(key("current"), 0L).commit()
        return state()
    }

    fun exportJson(): JSONObject {
        val s = state()
        return JSONObject().put("mantra", s.mantra).put("target", s.target)
            .put("targetEnabled", s.targetEnabled).put("current", s.current)
            .put("sessions", s.sessions).put("history", history())
    }

    fun importJson(obj: JSONObject) {
        prefs.edit().putString(key("mantra"), obj.optString("mantra", "ಓಂ ನಮೋ ನಾರಾಯಣಾಯ"))
            .putLong(key("target"), obj.optLong("target", 108).coerceAtLeast(1))
            .putBoolean(key("target_enabled"), obj.optBoolean("targetEnabled", false))
            .putLong(key("current"), obj.optLong("current", 0).coerceAtLeast(0))
            .putLong(key("sessions"), obj.optLong("sessions", 0).coerceAtLeast(0))
            .putString(key("history"), obj.optJSONArray("history")?.toString() ?: "[]").commit()
    }

    private fun key(name: String) = prefix + name
    private fun history(): JSONArray = runCatching { JSONArray(prefs.getString(key("history"), "[]")) }.getOrElse { JSONArray() }
    private fun safeAdd(a: Long, b: Long): Long = if (Long.MAX_VALUE - a < b) Long.MAX_VALUE else a + b

    private fun migrateLegacy() {
        val edit = prefs.edit()
        if (!prefs.getBoolean("profiles_migrated", false)) {
            val activeId = ProfileStore(context).active().id
            listOf("mantra", "history").forEach { name ->
                if (prefs.contains(name)) edit.putString("profile_${activeId}_$name", prefs.getString(name, null))
            }
            listOf("target", "current", "sessions").forEach { name ->
                if (prefs.contains(name)) edit.putLong("profile_${activeId}_$name", prefs.getLong(name, 0L))
            }
            edit.putBoolean("profiles_migrated", true)
        }
        if (!prefs.getBoolean("lifetime_removed", false)) {
            prefs.all.keys.filter { it == "lifetime" || it.endsWith("_lifetime") }.forEach { edit.remove(it) }
            edit.putBoolean("lifetime_removed", true)
        }
        edit.commit()
    }

    companion object {
        const val PREFS = "paperdesk_japa"

        fun exportAll(context: Context): JSONObject = JSONObject().also { out ->
            ProfileStore(context).profiles().forEach { out.put(it.id, JapaStore(context, it.id).exportJson()) }
        }

        fun importAll(context: Context, obj: JSONObject) {
            obj.keys().forEach { id -> obj.optJSONObject(id)?.let { JapaStore(context, id).importJson(it) } }
        }
    }
}
