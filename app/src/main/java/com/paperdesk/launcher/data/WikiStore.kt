package com.paperdesk.launcher.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Offline Madhva wiki loaded from assets/knowledge/wiki.json. Content is structured data:
 * each section carries its own status, and sources carry URL and access date.
 * Bookmarks are viewer-local (SharedPreferences), never sent anywhere.
 */
object WikiStore {
    private const val PREFS = "wiki_prefs"
    private const val KEY_BOOKMARKS = "bookmarks"

    data class Category(val id: String, val titleEn: String, val titleKn: String)
    data class Source(val title: String, val url: String, val accessed: String, val note: String)
    data class Section(val heading: String, val body: String, val status: String)
    data class Article(
        val id: String,
        val category: String,
        val titleEn: String,
        val titleKn: String,
        val status: String,
        val summary: String,
        val sections: List<Section>,
        val related: List<String>,
        val sources: List<Source>
    )
    data class Book(val categories: List<Category>, val articles: List<Article>) {
        fun article(id: String): Article? = articles.firstOrNull { it.id == id }
    }

    fun load(context: Context): Book {
        val root = JSONObject(context.assets.open("knowledge/wiki.json").bufferedReader(Charsets.UTF_8).use { it.readText() })
        return Book(
            categories = root.getJSONArray("categories").objects().map {
                Category(it.getString("id"), it.getString("title_en"), it.getString("title_kn"))
            },
            articles = root.getJSONArray("articles").objects().map { a ->
                Article(
                    id = a.getString("id"),
                    category = a.getString("category"),
                    titleEn = a.getString("title_en"),
                    titleKn = a.optString("title_kn"),
                    status = a.getString("status"),
                    summary = a.optString("summary"),
                    sections = a.getJSONArray("sections").objects().map {
                        Section(it.getString("heading"), it.optString("body"), it.optString("status", "pending"))
                    },
                    related = a.optJSONArray("related")?.strings().orEmpty(),
                    sources = a.getJSONArray("sources").objects().map {
                        Source(it.getString("title"), it.getString("url"), it.optString("accessed"), it.optString("note"))
                    }
                )
            }
        )
    }

    fun bookmarks(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet(KEY_BOOKMARKS, emptySet()).orEmpty().toSet()

    fun setBookmarked(context: Context, articleId: String, on: Boolean) {
        val next = bookmarks(context).toMutableSet()
        if (on) next.add(articleId) else next.remove(articleId)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putStringSet(KEY_BOOKMARKS, next).apply()
    }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
    private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }
}
