package com.paperdesk.launcher.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class JapaStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    data class State(
        val mantra: String,
        val target: Long,
        val current: Long,
        val lifetime: Long,
        val sessions: Long
    )

    fun state(): State = State(
        prefs.getString("mantra", "ಓಂ ನಮೋ ನಾರಾಯಣಾಯ") ?: "",
        prefs.getLong("target", 108L).coerceAtLeast(1),
        prefs.getLong("current", 0L).coerceAtLeast(0),
        prefs.getLong("lifetime", 0L).coerceAtLeast(0),
        prefs.getLong("sessions", 0L).coerceAtLeast(0)
    )

    fun configure(mantra: String, target: Long) {
        prefs.edit().putString("mantra", mantra.trim()).putLong("target", target.coerceAtLeast(1)).commit()
    }

    fun add(delta: Long): State {
        val s = state()
        val safe = delta.coerceAtLeast(0)
        prefs.edit()
            .putLong("current", safeAdd(s.current, safe))
            .putLong("lifetime", safeAdd(s.lifetime, safe))
            .commit()
        return state()
    }

    fun undo(): State {
        val s = state()
        if (s.current == 0L) return s
        prefs.edit().putLong("current", s.current - 1).putLong("lifetime", (s.lifetime - 1).coerceAtLeast(0)).commit()
        return state()
    }

    fun complete(): State {
        val s = state()
        if (s.current == 0L) return s
        val history = history()
        history.put(JSONObject().put("at", System.currentTimeMillis()).put("mantra", s.mantra).put("count", s.current))
        while (history.length() > 100) history.remove(0)
        prefs.edit().putString("history", history.toString()).putLong("sessions", s.sessions + 1).putLong("current", 0L).commit()
        return state()
    }

    fun resetCurrent(): State {
        prefs.edit().putLong("current", 0L).commit()
        return state()
    }

    fun exportJson(): JSONObject {
        val s = state()
        return JSONObject().put("mantra", s.mantra).put("target", s.target).put("current", s.current)
            .put("lifetime", s.lifetime).put("sessions", s.sessions).put("history", history())
    }

    fun importJson(obj: JSONObject) {
        prefs.edit().putString("mantra", obj.optString("mantra", "ಓಂ ನಮೋ ನಾರಾಯಣಾಯ"))
            .putLong("target", obj.optLong("target", 108).coerceAtLeast(1))
            .putLong("current", obj.optLong("current", 0).coerceAtLeast(0))
            .putLong("lifetime", obj.optLong("lifetime", 0).coerceAtLeast(0))
            .putLong("sessions", obj.optLong("sessions", 0).coerceAtLeast(0))
            .putString("history", obj.optJSONArray("history")?.toString() ?: "[]").commit()
    }

    private fun history(): JSONArray = runCatching { JSONArray(prefs.getString("history", "[]")) }.getOrElse { JSONArray() }
    private fun safeAdd(a: Long, b: Long): Long = if (Long.MAX_VALUE - a < b) Long.MAX_VALUE else a + b

    companion object { const val PREFS = "paperdesk_japa" }
}
