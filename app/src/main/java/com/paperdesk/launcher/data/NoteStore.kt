package com.paperdesk.launcher.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class NoteItem(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val title: String = "",
    val body: String = "",
    val photo: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

class NoteStore(context: Context) {
    private val prefs = context.getSharedPreferences("paperdesk_notes", Context.MODE_PRIVATE)

    fun load(): MutableList<NoteItem> {
        val raw = prefs.getString("items", "[]") ?: "[]"
        val arr = JSONArray(raw)
        val out = mutableListOf<NoteItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += NoteItem(
                id = o.optString("id", UUID.randomUUID().toString()),
                projectId = o.optString("projectId", ""),
                title = o.optString("title", ""),
                body = o.optString("body", ""),
                photo = o.optString("photo", ""),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
            )
        }
        return out
    }

    fun forProject(projectId: String): List<NoteItem> =
        load().filter { it.projectId == projectId }.sortedByDescending { it.updatedAt }

    fun get(id: String): NoteItem? = load().firstOrNull { it.id == id }

    fun upsert(item: NoteItem) {
        val next = load().filter { it.id != item.id }.toMutableList()
        next.add(0, item)
        save(next)
    }

    fun delete(id: String) {
        val item = get(id)
        save(load().filter { it.id != id })
        DeskPhoto.delete(item?.photo.orEmpty())
    }

    fun deleteProject(projectId: String) {
        load().filter { it.projectId == projectId }.forEach { DeskPhoto.delete(it.photo) }
        save(load().filter { it.projectId != projectId })
    }

    fun save(items: List<NoteItem>) {
        val arr = JSONArray()
        items.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("projectId", it.projectId)
                    .put("title", it.title)
                    .put("body", it.body)
                    .put("photo", it.photo)
                    .put("createdAt", it.createdAt)
                    .put("updatedAt", it.updatedAt)
            )
        }
        prefs.edit().putString("items", arr.toString()).apply()
    }
}
