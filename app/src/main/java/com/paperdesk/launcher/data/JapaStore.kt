package com.paperdesk.launcher.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Per-mantra japa numbers: daily count, cumulative, and resolved Sankalpa.
 * At-rest AES-256 via EncryptedSharedPreferences.
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
            val flagsOnly = plain.all.keys.all {
                it == "profiles_migrated" || it == "lifetime_removed" || it == "encrypted_migrated"
            }
            if (flagsOnly || encrypted.all.isNotEmpty() || copied == 0) {
                plain.edit().clear().apply()
            }
        }
    }

    /** Snapshot for one mantra — numbers only. */
    data class MantraCounts(
        val mantraId: String,
        val daily: Long,
        val dailyTarget: Long,
        val cumulative: Long,
        /** User-configured Sankalpa; 0 = auto from cumulative. */
        val sankalpaConfigured: Long,
        /** Effective Sankalpa (configured or next ladder rung). */
        val sankalpa: Long
    )

    /** Legacy single-session view used by older call sites. */
    data class State(
        val mantra: String,
        val target: Long,
        val targetEnabled: Boolean,
        val current: Long,
        val sessions: Long
    )

    fun activeMantraId(): String {
        val stored = prefs.getString(key("mantra_id"), null)
        if (!stored.isNullOrBlank()) return stored
        val mantra = prefs.getString(key("mantra"), null)
        val fromCatalog = JapaCatalog.entries.firstOrNull { it.mantra == mantra }?.id
        return fromCatalog ?: "gayatri"
    }

    fun setActiveMantraId(id: String) {
        val entry = JapaCatalog.entries.firstOrNull { it.id == id } ?: return
        prefs.edit()
            .putString(key("mantra_id"), entry.id)
            .putString(key("mantra"), entry.mantra)
            .apply()
    }

    /** Sum of cumulative counts across catalogue mantras (for japa badges). */
    fun totalCumulative(): Long =
        JapaCatalog.entries.sumOf { counts(it.id).cumulative }

    fun counts(mantraId: String = activeMantraId()): MantraCounts {
        ensureDay(mantraId)
        val profile = ProfileStore(app).active()
        val targets = profile.japaTargets[mantraId]
        val dailyTarget = (targets?.first ?: JapaGoals.DEFAULT_DAILY).coerceAtLeast(1)
        val configured = (targets?.second ?: JapaGoals.SANKALPA_AUTO).coerceAtLeast(0)
        val cumulative = prefs.getLong(mKey(mantraId, "cumulative"), 0L).coerceAtLeast(0)
        val daily = prefs.getLong(mKey(mantraId, "daily"), 0L).coerceAtLeast(0)
        // Migrate legacy single current into active mantra once.
        migrateLegacyCurrent(mantraId, daily)
        val freshDaily = prefs.getLong(mKey(mantraId, "daily"), 0L).coerceAtLeast(0)
        val freshCum = prefs.getLong(mKey(mantraId, "cumulative"), cumulative).coerceAtLeast(0)
        return MantraCounts(
            mantraId = mantraId,
            daily = freshDaily,
            dailyTarget = dailyTarget,
            cumulative = freshCum,
            sankalpaConfigured = configured,
            sankalpa = JapaGoals.resolveSankalpa(configured, freshCum)
        )
    }

    fun state(): State {
        val c = counts()
        val entry = JapaCatalog.entries.firstOrNull { it.id == c.mantraId }
        return State(
            mantra = entry?.mantra ?: prefs.getString(key("mantra"), entry?.mantra ?: "") ?: "",
            target = c.dailyTarget,
            targetEnabled = true,
            current = c.daily,
            sessions = prefs.getLong(key("sessions"), 0L).coerceAtLeast(0)
        )
    }

    fun configure(mantra: String, target: Long, targetEnabled: Boolean) {
        val entry = JapaCatalog.entries.firstOrNull { it.mantra == mantra }
        val edit = prefs.edit()
            .putString(key("mantra"), mantra.trim())
            .putLong(key("target"), target.coerceAtLeast(1))
            .putBoolean(key("target_enabled"), targetEnabled)
        if (entry != null) edit.putString(key("mantra_id"), entry.id)
        edit.apply()
        // Persist daily target into profile when this mantra already has targets.
        if (entry != null) {
            val store = ProfileStore(app)
            val p = store.active()
            if (p.japaTargets.containsKey(entry.id) || entry.id in p.dikshaMantras) {
                val sankalpa = p.japaTargets[entry.id]?.second ?: JapaGoals.SANKALPA_AUTO
                val next = p.japaTargets.toMutableMap()
                next[entry.id] = Pair(target.coerceAtLeast(1), sankalpa)
                store.save(p.copy(japaTargets = next))
            }
        }
    }

    /** Demo / import helper — sets absolute daily + cumulative for a mantra. */
    fun seedCounts(mantraId: String, daily: Long, cumulative: Long) {
        ensureDay(mantraId)
        prefs.edit()
            .putLong(mKey(mantraId, "daily"), daily.coerceAtLeast(0))
            .putLong(mKey(mantraId, "cumulative"), cumulative.coerceAtLeast(0))
            .apply()
    }

    fun add(delta: Long, mantraId: String = activeMantraId()): MantraCounts {
        ensureDay(mantraId)
        val d = delta.coerceAtLeast(0)
        if (d == 0L) return counts(mantraId)
        val daily = safeAdd(prefs.getLong(mKey(mantraId, "daily"), 0L), d)
        val cumulative = safeAdd(prefs.getLong(mKey(mantraId, "cumulative"), 0L), d)
        prefs.edit()
            .putLong(mKey(mantraId, "daily"), daily)
            .putLong(mKey(mantraId, "cumulative"), cumulative)
            .apply()
        // Auto-advance Sankalpa when unset and cumulative crossed the rung.
        maybeAdvanceAutoSankalpa(mantraId, cumulative)
        return counts(mantraId)
    }

    fun undo(mantraId: String = activeMantraId()): MantraCounts {
        ensureDay(mantraId)
        val daily = prefs.getLong(mKey(mantraId, "daily"), 0L)
        if (daily <= 0L) return counts(mantraId)
        val cumulative = prefs.getLong(mKey(mantraId, "cumulative"), 0L).coerceAtLeast(1) - 1
        prefs.edit()
            .putLong(mKey(mantraId, "daily"), daily - 1)
            .putLong(mKey(mantraId, "cumulative"), cumulative)
            .apply()
        return counts(mantraId)
    }

    fun complete(mantraId: String = activeMantraId()): MantraCounts {
        val c = counts(mantraId)
        if (c.daily == 0L) return c
        val entry = JapaCatalog.entries.firstOrNull { it.id == mantraId }
        val history = history()
        history.put(
            JSONObject()
                .put("at", System.currentTimeMillis())
                .put("mantra", entry?.mantra ?: mantraId)
                .put("mantraId", mantraId)
                .put("count", c.daily)
        )
        while (history.length() > 100) history.remove(0)
        prefs.edit()
            .putString(key("history"), history.toString())
            .putLong(key("sessions"), prefs.getLong(key("sessions"), 0L) + 1)
            .putLong(mKey(mantraId, "daily"), 0L)
            .apply()
        return counts(mantraId)
    }

    fun resetCurrent(mantraId: String = activeMantraId()): MantraCounts {
        prefs.edit().putLong(mKey(mantraId, "daily"), 0L).apply()
        return counts(mantraId)
    }

    fun exportJson(): JSONObject {
        val s = state()
        val mantras = JSONObject()
        JapaCatalog.entries.forEach { entry ->
            val c = counts(entry.id)
            if (c.daily > 0 || c.cumulative > 0) {
                mantras.put(
                    entry.id,
                    JSONObject()
                        .put("daily", c.daily)
                        .put("cumulative", c.cumulative)
                )
            }
        }
        return JSONObject()
            .put("mantra", s.mantra)
            .put("mantraId", activeMantraId())
            .put("target", s.target)
            .put("targetEnabled", s.targetEnabled)
            .put("current", s.current)
            .put("sessions", s.sessions)
            .put("mantras", mantras)
            .put("history", history())
    }

    fun importJson(obj: JSONObject) {
        val edit = prefs.edit()
            .putString(key("mantra"), obj.optString("mantra", "ಓಂ ನಮೋ ನಾರಾಯಣಾಯ"))
            .putLong(key("target"), obj.optLong("target", 108).coerceAtLeast(1))
            .putBoolean(key("target_enabled"), obj.optBoolean("targetEnabled", false))
            .putLong(key("sessions"), obj.optLong("sessions", 0).coerceAtLeast(0))
            .putString(key("history"), obj.optJSONArray("history")?.toString() ?: "[]")
        val mantraId = obj.optString("mantraId").ifBlank {
            JapaCatalog.entries.firstOrNull { it.mantra == obj.optString("mantra") }?.id.orEmpty()
        }
        if (mantraId.isNotBlank()) edit.putString(key("mantra_id"), mantraId)
        val mantras = obj.optJSONObject("mantras")
        if (mantras != null) {
            mantras.keys().forEach { id ->
                val row = mantras.optJSONObject(id) ?: return@forEach
                edit.putLong(mKey(id, "daily"), row.optLong("daily", 0).coerceAtLeast(0))
                edit.putLong(mKey(id, "cumulative"), row.optLong("cumulative", 0).coerceAtLeast(0))
                edit.putString(mKey(id, "day"), PanchangaStore.dateKey())
            }
        } else if (mantraId.isNotBlank()) {
            edit.putLong(mKey(mantraId, "daily"), obj.optLong("current", 0).coerceAtLeast(0))
        }
        edit.apply()
    }

    private fun maybeAdvanceAutoSankalpa(mantraId: String, cumulative: Long) {
        val store = ProfileStore(app)
        val p = store.active()
        val configured = p.japaTargets[mantraId]?.second ?: return
        if (configured > 0) return
        // Keep profile daily; Sankalpa stays 0 (auto) — resolveSankalpa picks next rung.
        // No write needed; next counts() call recomputes.
    }

    private fun ensureDay(mantraId: String) {
        val today = PanchangaStore.dateKey()
        val key = mKey(mantraId, "day")
        if (prefs.getString(key, "") == today) return
        prefs.edit()
            .putString(key, today)
            .putLong(mKey(mantraId, "daily"), 0L)
            .apply()
    }

    private fun migrateLegacyCurrent(mantraId: String, already: Long) {
        if (already > 0) return
        if (!prefs.contains(key("current"))) return
        val legacy = prefs.getLong(key("current"), 0L)
        if (legacy <= 0) {
            prefs.edit().remove(key("current")).apply()
            return
        }
        prefs.edit()
            .putLong(mKey(mantraId, "daily"), legacy)
            .putLong(mKey(mantraId, "cumulative"), safeAdd(prefs.getLong(mKey(mantraId, "cumulative"), 0L), legacy))
            .putString(mKey(mantraId, "day"), PanchangaStore.dateKey())
            .remove(key("current"))
            .apply()
    }

    private fun key(name: String) = prefix + name
    private fun mKey(mantraId: String, name: String) = prefix + "m_" + mantraId + "_" + name
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
