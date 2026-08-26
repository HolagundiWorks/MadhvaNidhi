package com.paperdesk.launcher.data

import android.content.Context
import org.json.JSONObject

data class LibraryItem(
    val id: String,
    val title: String,
    val section: String,
    val preview: String
)

object LibraryStore {
    const val MANTRAS = "mantras"
    const val PUJA = "puja"
    const val STOTRA = "stotra"
    const val NAMAVALI = "namavali"

    private val cache = HashMap<String, List<LibraryItem>>()

    fun load(context: Context, collection: String): List<LibraryItem> {
        cache[collection]?.let { return it }
        val raw = runCatching {
            context.assets.open("library/$collection/index.json").bufferedReader().use { it.readText() }
        }.getOrNull() ?: return emptyList()
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return emptyList()
        val arr = root.optJSONArray("items") ?: return emptyList()
        val items = ArrayList<LibraryItem>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            items += LibraryItem(
                id = o.getString("id"),
                title = o.optString("title"),
                section = o.optString("section"),
                preview = o.optString("preview")
            )
        }
        cache[collection] = items
        return items
    }

    fun text(context: Context, collection: String, id: String, lang: IndicScript.Lang = IndicScript.Lang.SA): String {
        val raw = runCatching {
            context.assets.open("library/$collection/$id.txt").bufferedReader().use { it.readText() }
        }.getOrDefault("")
        return IndicScript.convert(RitualText.tidy(raw), lang)
    }

    fun label(item: LibraryItem, lang: IndicScript.Lang): String =
        IndicScript.convert(item.title, lang)

    fun haystack(item: LibraryItem): String =
        item.title + " " +
            IndicScript.convert(item.title, IndicScript.Lang.KN) + " " +
            IndicScript.convert(item.title, IndicScript.Lang.EN) + " " +
            item.preview + " " +
            IndicScript.convert(item.preview, IndicScript.Lang.EN)

    fun search(items: List<LibraryItem>, query: String): List<LibraryItem> {
        val q = query.trim()
        if (q.isBlank()) return items
        return items.filter { haystack(it).contains(q, true) || it.section.contains(q, true) }
    }
}
