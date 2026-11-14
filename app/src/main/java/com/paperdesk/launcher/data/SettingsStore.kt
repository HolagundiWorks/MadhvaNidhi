package com.paperdesk.launcher.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun json(): JSONObject {
        snapshot?.let { return JSONObject(it.toString()) }
        val raw = prefs.getString("json", "") ?: ""
        val obj = if (raw.isBlank()) defaults() else runCatching { JSONObject(raw) }.getOrElse { defaults() }
        var dirty = false
        if (obj.optString("flavor") != "madhvanidhi") {
            obj.put("flavor", "madhvanidhi")
            obj.put("theme", "black")
            obj.put("pages", JSONArray(PAGE_IDS))
            obj.put("projectEnabled", false)
            obj.put("todoEnabled", false)
            obj.put("notesEnabled", false)
            obj.put("calcEnabled", false)
            obj.put("gitaEnabled", false)
            obj.put("scheduleEnabled", false)
            dirty = true
        }
        // Historical page migrations collapsed into a single rev bump when needed.
        if (obj.optInt("pagesRev", 0) < 14) {
            obj.put("pages", JSONArray(PAGE_IDS))
            obj.put("projectEnabled", false)
            obj.put("todoEnabled", false)
            obj.put("notesEnabled", false)
            obj.put("calcEnabled", false)
            obj.put("gitaEnabled", false)
            obj.put("scheduleEnabled", false)
            obj.put("pagesRev", 14)
            dirty = true
        }
        // Collapse legacy sandhya/japa/puja pager slots into one nityakarma page (RAM).
        if (obj.optInt("pagesRev", 0) < 15) {
            obj.put("pages", JSONArray(PAGE_IDS))
            obj.put("pagesRev", 15)
            dirty = true
        }
        // Puja is a top-level destination (not a Nitya Karma tab).
        if (obj.optInt("pagesRev", 0) < 16) {
            obj.put("pages", JSONArray(PAGE_IDS))
            obj.put("pagesRev", 16)
            dirty = true
        }
        // Nitya Parayana moves under Nitya Karma (tab alias, not a Grantha page).
        if (obj.optInt("pagesRev", 0) < 17) {
            obj.put("pages", JSONArray(PAGE_IDS))
            obj.put("pagesRev", 17)
            dirty = true
        }
        if (dirty) {
            prefs.edit().putString("json", obj.toString()).apply()
        }
        snapshot = obj
        return JSONObject(obj.toString())
    }

    fun save(obj: JSONObject) {
        val merged = defaults()
        obj.keys().forEach { merged.put(it, obj.get(it)) }
        snapshot = merged
        prefs.edit().putString("json", merged.toString()).apply()
    }

    fun pages(): List<String> {
        val arr = json().optJSONArray("pages") ?: defaults().getJSONArray("pages")
        return (0 until arr.length()).map { arr.getString(it) }
            .map { canonicalizePageId(it) }
            .filter { it in PAGE_IDS }
            .distinct()
    }

    fun landscape(): Boolean = json().optBoolean("landscape", false)
    fun alwaysOn(): Boolean = json().optBoolean("alwaysOn", false)
    fun statusBar(): Boolean = json().optBoolean("statusBar", false)
    fun navBar(): Boolean = json().optBoolean("navBar", false)
    fun clockAnalog(): Boolean = json().optBoolean("clockAnalog", true)
    fun projectEnabled(): Boolean = json().optBoolean("projectEnabled", false)
    fun todoEnabled(): Boolean = json().optBoolean("todoEnabled", false)
    fun scheduleEnabled(): Boolean = json().optBoolean("scheduleEnabled", false)
    fun calcEnabled(): Boolean = json().optBoolean("calcEnabled", false)
    fun notesEnabled(): Boolean = json().optBoolean("notesEnabled", false)
    fun gitaEnabled(): Boolean = json().optBoolean("gitaEnabled", false)
    fun measureEnabled(): Boolean = json().optBoolean("measureEnabled", false)

    fun setAlwaysOn(on: Boolean) {
        val obj = json()
        obj.put("alwaysOn", on)
        save(obj)
    }

    fun veda(): String = json().optString("veda", VEDA_RIGVEDA)

    fun setVeda(id: String) {
        val value = if (id in VEDA_IDS) id else VEDA_RIGVEDA
        save(json().put("veda", value))
    }

    fun panchangaTradition(): String =
        json().optString("panchangaTradition", PanchangaTraditions.SRS)

    fun setPanchangaTradition(id: String) {
        save(json().put("panchangaTradition", PanchangaTraditions.find(id).id))
    }

    /**
     * Dashboard row-2 japa slots (5). Slot 0 is always Gayatri; slots 1–4 are user-chosen mantra ids.
     */
    fun dashboardJapaSlots(): List<String> {
        val arr = json().optJSONArray("dashboardJapaSlots")
        val defaults = listOf("gayatri", "narayana", "rama", "hanuman", "raghavendra")
        if (arr == null || arr.length() == 0) return defaults
        val slots = (0 until arr.length()).map { arr.optString(it).ifBlank { defaults.getOrElse(it) { "narayana" } } }
            .toMutableList()
        while (slots.size < 5) slots += defaults[slots.size]
        slots[0] = "gayatri"
        return slots.take(5)
    }

    fun setDashboardJapaSlots(slots: List<String>) {
        val next = slots.take(5).toMutableList()
        while (next.size < 5) next += "narayana"
        next[0] = "gayatri"
        save(json().put("dashboardJapaSlots", JSONArray(next)))
    }

    /** Dashboard row-3 shortcuts (3). User-chosen destination ids. */
    fun dashboardShortcuts(): List<String> {
        val arr = json().optJSONArray("dashboardShortcuts")
        val defaults = DASHBOARD_SHORTCUT_DEFAULTS
        if (arr == null || arr.length() == 0) return defaults
        val slots = (0 until arr.length()).map { i ->
            val id = arr.optString(i).ifBlank { defaults.getOrElse(i) { "desk" } }
            if (id in DASHBOARD_SHORTCUT_IDS) id else defaults.getOrElse(i) { "desk" }
        }.toMutableList()
        while (slots.size < 3) slots += defaults[slots.size]
        return slots.take(3)
    }

    fun setDashboardShortcuts(slots: List<String>) {
        val next = slots.take(3).map { id ->
            if (id in DASHBOARD_SHORTCUT_IDS) id else "desk"
        }.toMutableList()
        while (next.size < 3) next += DASHBOARD_SHORTCUT_DEFAULTS[next.size]
        save(json().put("dashboardShortcuts", JSONArray(next)))
    }

    companion object {
        const val PREFS = "paperdesk_os"
        const val VEDA_RIGVEDA = "rigveda_ashvalayana"
        const val VEDA_KRISHNA_YAJURVEDA = "krishna_yajurveda_taittiriya"
        val VEDA_IDS = listOf(VEDA_RIGVEDA, VEDA_KRISHNA_YAJURVEDA)

        /** Shipped destinations — Nitya Karma (Sandhya/Japa/Parayana) + standalone Puja. */
        val PAGE_IDS = listOf(
            "dashboard", "desk",
            "nityakarma", "puja",
            "grantha", "stotra", "suktas", "mantras", "namavali",
            "sangeeta", "hadugalu", "favorites",
            "samskruti", "aayadi", "rangavalli", "recipes",
            "user",
            "settings"
        )

        /** Navigation aliases that open Nitya tabs (not separate pages). */
        val NITYA_TAB_ALIASES = setOf("sandhya", "japa", "nitya", "nityakarma")

        val DASHBOARD_SHORTCUT_DEFAULTS = listOf("desk", "japa", "grantha")

        /**
         * Dashboard shortcut picker — hubs + high-use practice tabs only (Hick).
         * Leaf readers open from their hub tabs; User is under Settings.
         */
        val DASHBOARD_SHORTCUT_IDS = listOf(
            "desk", "sandhya", "japa", "nitya", "puja",
            "grantha", "sangeeta", "samskruti", "settings"
        )

        val PAGE_LABELS = mapOf(
            "dashboard" to "DASHBOARD",
            "desk" to "PANCHANGA",
            "nityakarma" to "NITYA KARMA",
            "sandhya" to "SANDHYAVANDANA",
            "nitya" to "NITYA PARAYANA",
            "stotra" to "STOTRAS",
            "suktas" to "SUKTAS",
            "mantras" to "MANTRAS",
            "namavali" to "NAMAVALIS",
            "puja" to "POOJA",
            "grantha" to "GRANTHA",
            "hadugalu" to "HADUGALU",
            "playlists" to "FAVORITES",
            "favorites" to "FAVORITES",
            "sangeeta" to "BHAKTI SANGEETA",
            "japa" to "JAPA",
            "samskruti" to "GRUHA SAMSKRUTI",
            "aayadi" to "AAYADI",
            "rangavalli" to "RANGAVALLI",
            "recipes" to "RECIPES",
            "user" to "USER",
            "settings" to "SETTINGS"
        )

        @Volatile
        private var snapshot: JSONObject? = null

        fun invalidateCache() {
            snapshot = null
        }

        fun canonicalizePageId(id: String): String = when (id) {
            "sandhya", "japa", "nitya" -> "nityakarma"
            "panchanga" -> "desk"
            "playlists" -> "favorites"
            else -> id
        }

        fun isKnownPage(id: String): Boolean =
            id == "panchanga" || id in PAGE_IDS || id in NITYA_TAB_ALIASES || id in PAGE_LABELS

        fun defaults(): JSONObject = JSONObject()
            .put("flavor", "madhvanidhi")
            .put("pages", JSONArray(PAGE_IDS))
            .put("pagesRev", 17)
            .put("veda", VEDA_RIGVEDA)
            .put("panchangaTradition", PanchangaTraditions.SRS)
            .put("dashboardJapaSlots", JSONArray(listOf("gayatri", "narayana", "rama", "hanuman", "raghavendra")))
            .put("dashboardShortcuts", JSONArray(DASHBOARD_SHORTCUT_DEFAULTS))
            .put("landscape", false)
            .put("theme", "black")
            .put("accent", "#BA4E00")
            .put("fontScale", 1.0)
            .put("clockAnalog", true)
            .put("calcScientific", false)
            .put("statusBar", false)
            .put("navBar", false)
            .put("alwaysOn", false)
            .put("mobileData", false)
            .put("nfc", false)
            .put("gps", false)
            .put("sensors", true)
            .put("autoRotate", false)
            .put("usbDebug", true)
            .put("developerOptions", true)
            .put("screenTimeoutMs", 0)
            .put("lockScreen", false)
            .put("notifications", true)
            .put("sound", false)
            .put("vibration", false)
            .put("projectEnabled", false)
            .put("measureEnabled", false)
            .put("calcEnabled", false)
            .put("todoEnabled", false)
            .put("scheduleEnabled", false)
            .put("notesEnabled", false)
            .put("gitaEnabled", false)
            .put("profiles", JSONArray())
    }
}
