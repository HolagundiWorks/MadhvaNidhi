package com.paperdesk.launcher.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun json(): JSONObject {
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
        if (obj.optInt("pagesRev", 0) < 2) {
            if (insertPage(obj, "sandhya", after = "schedule")) dirty = true
            obj.put("pagesRev", 2)
            dirty = true
        }
        if (obj.optInt("pagesRev", 0) < 3) {
            obj.put("pages", JSONArray(PAGE_IDS))
            obj.put("gitaEnabled", false)
            obj.put("pagesRev", 3)
            dirty = true
        }
        if (obj.optInt("pagesRev", 0) < 4) {
            obj.put("pages", JSONArray(PAGE_IDS))
            obj.put("gitaEnabled", false)
            obj.put("pagesRev", 4)
            dirty = true
        }
        if (obj.optInt("pagesRev", 0) < 5) {
            obj.put("pages", JSONArray(PAGE_IDS))
            obj.put("gitaEnabled", false)
            obj.put("pagesRev", 5)
            dirty = true
        }
        if (obj.optInt("pagesRev", 0) < 6) {
            obj.put("pages", JSONArray(PAGE_IDS))
            obj.put("scheduleEnabled", false)
            obj.put("gitaEnabled", false)
            obj.put("pagesRev", 6)
            dirty = true
        }
        if (obj.optInt("pagesRev", 0) < 7) {
            obj.put("pages", JSONArray(PAGE_IDS))
            obj.put("pagesRev", 7)
            dirty = true
        }
        if (obj.optInt("pagesRev", 0) < 8) {
            obj.put("pages", JSONArray(PAGE_IDS))
            obj.put("pagesRev", 8)
            dirty = true
        }
        if (obj.optInt("pagesRev", 0) < 9) {
            obj.put("pages", JSONArray(PAGE_IDS))
            obj.put("pagesRev", 9)
            dirty = true
        }
        if (obj.optInt("pagesRev", 0) < 10) {
            if (insertPage(obj, "nitya", after = "sandhya")) dirty = true
            obj.put("pagesRev", 10)
            dirty = true
        }
        if (dirty) prefs.edit().putString("json", obj.toString()).apply()
        return obj
    }

    fun save(obj: JSONObject) {
        val merged = defaults()
        obj.keys().forEach { merged.put(it, obj.get(it)) }
        prefs.edit().putString("json", merged.toString()).commit()
    }

    fun pages(): List<String> {
        val arr = json().optJSONArray("pages") ?: defaults().getJSONArray("pages")
        return (0 until arr.length()).map { arr.getString(it) }
            .filter { it in PAGE_IDS }
    }

    fun landscape(): Boolean = json().optBoolean("landscape", true)
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

    companion object {
        const val PREFS = "paperdesk_os"
        const val VEDA_RIGVEDA = "rigveda_ashvalayana"
        const val VEDA_KRISHNA_YAJURVEDA = "krishna_yajurveda_taittiriya"
        val VEDA_IDS = listOf(VEDA_RIGVEDA, VEDA_KRISHNA_YAJURVEDA)
        val PAGE_IDS = listOf("desk", "sandhya", "nitya", "mantras", "puja", "hadugalu", "japa", "aayadi")
        val PAGE_LABELS = mapOf(
            "desk" to "PANCHANGA",
            "sandhya" to "SANDHYAVANDANA",
            "nitya" to "NITYA PARAYANA",
            "mantras" to "MANTRAS",
            "puja" to "POOJA",
            "hadugalu" to "HADUGALU"
            ,"japa" to "JAPA"
            ,"aayadi" to "AAYADI"
        )

        private fun insertPage(obj: JSONObject, id: String, after: String): Boolean {
            val arr = obj.optJSONArray("pages") ?: return false
            val list = (0 until arr.length()).map { arr.getString(it) }.toMutableList()
            if (id in list) return false
            val i = list.indexOf(after)
            if (i >= 0) list.add(i + 1, id) else list.add(id)
            obj.put("pages", JSONArray(list))
            return true
        }

        fun defaults(): JSONObject = JSONObject()
            .put("flavor", "madhvanidhi")
            .put("pages", JSONArray(PAGE_IDS))
            .put("pagesRev", 10)
            .put("veda", VEDA_RIGVEDA)
            .put("panchangaTradition", PanchangaTraditions.SRS)
            .put("landscape", true)
            .put("theme", "black")
            .put("accent", "#FF6A00")
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
