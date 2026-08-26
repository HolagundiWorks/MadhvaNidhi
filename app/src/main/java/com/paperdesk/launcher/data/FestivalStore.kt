package com.paperdesk.launcher.data

import android.content.Context
import org.json.JSONObject

data class FestivalDay(
    val date: String,
    val sa: String,
    val kn: String,
    val en: String
)

/** Parabhava-year date-wise festival list for calendar and panchanga. */
object FestivalStore {
    @Volatile private var loaded = false
    private var byDate: Map<String, FestivalDay> = emptyMap()

    @Synchronized
    fun load(context: Context) {
        if (loaded) return
        val raw = runCatching {
            context.assets.open("madhwa/festivals.json").bufferedReader().use { it.readText() }
        }.getOrDefault("")
        val days = runCatching { JSONObject(raw).optJSONObject("days") }.getOrNull()
        val map = HashMap<String, FestivalDay>()
        if (days != null) {
            days.keys().forEach { key ->
                val o = days.optJSONObject(key) ?: return@forEach
                map[key] = FestivalDay(
                    date = key,
                    sa = o.optString("sa"),
                    kn = o.optString("kn"),
                    en = o.optString("en")
                )
            }
        }
        byDate = map
        loaded = true
    }

    fun get(date: String): FestivalDay? = byDate[date]

    fun has(date: String): Boolean = byDate.containsKey(date)

    fun label(date: String, lang: IndicScript.Lang): String {
        val d = byDate[date] ?: return ""
        return when (lang) {
            IndicScript.Lang.KN -> d.kn.ifBlank { d.sa }
            IndicScript.Lang.SA -> d.sa.ifBlank { d.kn }
            IndicScript.Lang.EN -> d.en.ifBlank { d.sa }
        }
    }
}
