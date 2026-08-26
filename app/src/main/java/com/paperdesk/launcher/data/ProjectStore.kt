package com.paperdesk.launcher.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val number: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

data class ProjectSummary(
    val measures: Int,
    val notes: Int,
    val minutes: Int,
    val schedules: Int,
    val upcoming: Int,
    val todos: Int,
    val pendingTodos: Int
)

class ProjectStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("paperdesk_projects", Context.MODE_PRIVATE)

    fun load(): MutableList<Project> {
        val raw = prefs.getString("items", "[]") ?: "[]"
        val arr = JSONArray(raw)
        val out = mutableListOf<Project>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += Project(
                id = o.optString("id", UUID.randomUUID().toString()),
                name = o.getString("name"),
                number = o.optString("number", ""),
                updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
            )
        }
        return out.sortedByDescending { it.updatedAt }.toMutableList()
    }

    fun get(id: String): Project? = load().firstOrNull { it.id == id }

    fun nameOf(id: String): String {
        if (id.isBlank()) return ""
        return get(id)?.name.orEmpty()
    }

    fun upsert(item: Project) {
        val next = load().filter { it.id != item.id }.toMutableList()
        next.add(0, item)
        save(next)
    }

    fun touch(id: String) {
        if (id.isBlank()) return
        val p = get(id) ?: return
        upsert(p.copy(updatedAt = System.currentTimeMillis()))
    }

    fun delete(id: String) {
        save(load().filter { it.id != id })
        MeasureStore(context).deleteProject(id)
        NoteStore(context).deleteProject(id)
        MeetingStore(context).deleteProject(id)
        EventStore(context).deleteProject(id)
        TaskStore(context).deleteProject(id)
    }

    fun save(items: List<Project>) {
        val arr = JSONArray()
        items.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("name", it.name)
                    .put("number", it.number)
                    .put("updatedAt", it.updatedAt)
            )
        }
        prefs.edit().putString("items", arr.toString()).apply()
    }

    fun summary(id: String): ProjectSummary {
        val today = EventStore.startOfDay()
        val measures = MeasureStore(context).forProject(id)
        val notes = NoteStore(context).forProject(id)
        val minutes = MeetingStore(context).forProject(id)
        val events = EventStore(context).forProject(id)
        val todos = TaskStore(context).forProject(id)
        return ProjectSummary(
            measures = measures.size,
            notes = notes.size,
            minutes = minutes.size,
            schedules = events.size,
            upcoming = events.count { !it.done && it.dayStart >= today },
            todos = todos.size,
            pendingTodos = todos.count { !it.done }
        )
    }
}
