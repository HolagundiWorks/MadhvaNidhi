package com.paperdesk.launcher.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

data class TaskItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val done: Boolean = false,
    val priority: Int = PRIORITY_MED,
    val dueAt: Long = startOfDay(System.currentTimeMillis()),
    val projectId: String = "",
    val project: String = "",
    val timeLabel: String = "",
    val notes: String = ""
) {
    companion object {
        const val PRIORITY_LOW = 0
        const val PRIORITY_MED = 1
        const val PRIORITY_HIGH = 2

        fun startOfDay(ms: Long): Long {
            val c = Calendar.getInstance()
            c.timeInMillis = ms
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            return c.timeInMillis
        }
    }

    fun isOverdue(now: Long = System.currentTimeMillis()): Boolean {
        if (done) return false
        return dueAt < startOfDay(now)
    }

    fun isToday(now: Long = System.currentTimeMillis()): Boolean {
        val start = startOfDay(now)
        val end = start + 24L * 60 * 60 * 1000
        return dueAt in start until end
    }

    fun isUpcoming(now: Long = System.currentTimeMillis()): Boolean {
        return !done && dueAt >= startOfDay(now) + 24L * 60 * 60 * 1000
    }
}

class TaskStore(context: Context) {
    private val prefs = context.getSharedPreferences("paperdesk_tasks", Context.MODE_PRIVATE)

    fun load(): MutableList<TaskItem> {
        val raw = prefs.getString("items", "[]") ?: "[]"
        val arr = JSONArray(raw)
        val out = mutableListOf<TaskItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += TaskItem(
                id = o.optString("id", UUID.randomUUID().toString()),
                text = o.getString("text"),
                done = o.optBoolean("done", false),
                priority = o.optInt("priority", TaskItem.PRIORITY_MED),
                dueAt = o.optLong("dueAt", TaskItem.startOfDay(System.currentTimeMillis())),
                projectId = o.optString("projectId", ""),
                project = o.optString("project", ""),
                timeLabel = o.optString("timeLabel", ""),
                notes = o.optString("notes", "")
            )
        }
        return out
    }

    fun forProject(projectId: String): List<TaskItem> =
        load().filter { it.projectId == projectId }

    fun get(id: String): TaskItem? = load().firstOrNull { it.id == id }

    fun deleteProject(projectId: String) {
        save(load().filter { it.projectId != projectId })
    }

    fun upsert(item: TaskItem) {
        val next = load().filter { it.id != item.id }.toMutableList()
        next.add(0, item)
        save(next)
    }

    fun save(items: List<TaskItem>) {
        val arr = JSONArray()
        items.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("text", it.text)
                    .put("done", it.done)
                    .put("priority", it.priority)
                    .put("dueAt", it.dueAt)
                    .put("projectId", it.projectId)
                    .put("project", it.project)
                    .put("timeLabel", it.timeLabel)
                    .put("notes", it.notes)
            )
        }
        prefs.edit().putString("items", arr.toString()).apply()
    }
}
