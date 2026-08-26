package com.paperdesk.launcher.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

data class CalendarEvent(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String = "",
    val title: String,
    val dayStart: Long,
    val hour: Int = 9,
    val minute: Int = 0,
    val notes: String = "",
    val done: Boolean = false,
    val timeLabel: String = ""
) {
    fun clock(): String = String.format("%02d:%02d", hour, minute)
}

class EventStore(context: Context) {
    private val prefs = context.getSharedPreferences("paperdesk_events", Context.MODE_PRIVATE)

    fun load(): MutableList<CalendarEvent> {
        val raw = prefs.getString("items", "[]") ?: "[]"
        val arr = JSONArray(raw)
        val out = mutableListOf<CalendarEvent>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val label = o.optString("timeLabel", "")
            var hour = o.optInt("hour", -1)
            var minute = o.optInt("minute", 0)
            if (hour < 0) {
                val parts = label.split(":")
                hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
                minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            }
            out += CalendarEvent(
                id = o.optString("id", UUID.randomUUID().toString()),
                projectId = o.optString("projectId", ""),
                title = o.getString("title"),
                dayStart = o.getLong("dayStart"),
                hour = hour.coerceIn(0, 23),
                minute = minute.coerceIn(0, 59),
                notes = o.optString("notes", ""),
                done = o.optBoolean("done", false),
                timeLabel = label
            )
        }
        return out
    }

    fun forDay(dayStart: Long, projectId: String? = null): List<CalendarEvent> =
        load().filter { it.dayStart == dayStart && (projectId.isNullOrBlank() || it.projectId == projectId) }
            .sortedWith(compareBy({ it.hour }, { it.minute }))

    fun forProject(projectId: String): List<CalendarEvent> =
        load().filter { it.projectId == projectId }

    fun deleteProject(projectId: String) {
        save(load().filter { it.projectId != projectId })
    }

    fun get(id: String): CalendarEvent? = load().firstOrNull { it.id == id }

    fun upsert(item: CalendarEvent) {
        val next = load().filter { it.id != item.id }.toMutableList()
        next += item
        lastTouchedDay = item.dayStart
        save(next)
    }

    fun save(items: List<CalendarEvent>) {
        val arr = JSONArray()
        items.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("projectId", it.projectId)
                    .put("title", it.title)
                    .put("dayStart", it.dayStart)
                    .put("hour", it.hour)
                    .put("minute", it.minute)
                    .put("timeLabel", it.clock())
                    .put("notes", it.notes)
                    .put("done", it.done)
            )
        }
        prefs.edit().putString("items", arr.toString()).apply()
    }

    fun daysWithEvents(): Set<Long> = load().map { it.dayStart }.toSet()

    companion object {
        @Volatile
        var lastTouchedDay: Long? = null
        fun startOfDay(year: Int, month: Int, day: Int): Long {
            val c = Calendar.getInstance()
            c.set(year, month, day, 0, 0, 0)
            c.set(Calendar.MILLISECOND, 0)
            return c.timeInMillis
        }

        fun startOfDay(ms: Long = System.currentTimeMillis()): Long {
            val c = Calendar.getInstance()
            c.timeInMillis = ms
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            return c.timeInMillis
        }
    }
}
