package com.paperdesk.launcher.alarm

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

data class AlarmItem(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val enabled: Boolean = true,
    val days: Int = 0x7F
) {
    fun nextTriggerMs(from: Long = System.currentTimeMillis()): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = from
        c.set(Calendar.HOUR_OF_DAY, hour)
        c.set(Calendar.MINUTE, minute)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        repeat(8) {
            val dow = c.get(Calendar.DAY_OF_WEEK) // 1 Sun
            val bit = 1 shl ((dow + 5) % 7) // Mon=1
            if (c.timeInMillis > from && (days and bit) != 0) return c.timeInMillis
            c.add(Calendar.DAY_OF_YEAR, 1)
        }
        return c.timeInMillis
    }

    fun timeLabel(): String = String.format("%02d:%02d", hour, minute)

    fun daysLabel(): String {
        if (days == 0x7F) return "EVERY DAY"
        if (days == 0x1F) return "WEEKDAYS"
        val names = listOf("M", "T", "W", "T", "F", "S", "S")
        return names.mapIndexed { i, n -> if (days and (1 shl i) != 0) n else "" }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
    }
}

class AlarmStore(context: Context) {
    private val prefs = context.getSharedPreferences("paperdesk_alarms", Context.MODE_PRIVATE)

    fun load(): MutableList<AlarmItem> {
        val raw = prefs.getString("items", "[]") ?: "[]"
        val arr = JSONArray(raw)
        val out = mutableListOf<AlarmItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += AlarmItem(
                id = o.optString("id", UUID.randomUUID().toString()),
                hour = o.getInt("hour"),
                minute = o.getInt("minute"),
                label = o.optString("label", ""),
                enabled = o.optBoolean("enabled", true),
                days = o.optInt("days", 0x7F)
            )
        }
        return out.sortedBy { it.hour * 60 + it.minute }.toMutableList()
    }

    fun save(items: List<AlarmItem>) {
        val arr = JSONArray()
        items.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("hour", it.hour)
                    .put("minute", it.minute)
                    .put("label", it.label)
                    .put("enabled", it.enabled)
                    .put("days", it.days)
            )
        }
        prefs.edit().putString("items", arr.toString()).apply()
    }

    fun byId(id: String?): AlarmItem? = load().firstOrNull { it.id == id }

    fun nextEnabled(): AlarmItem? {
        val now = System.currentTimeMillis()
        return load().filter { it.enabled }.minByOrNull { it.nextTriggerMs(now) }
    }
}
