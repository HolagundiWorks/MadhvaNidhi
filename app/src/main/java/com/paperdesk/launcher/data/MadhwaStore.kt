package com.paperdesk.launcher.data

import android.content.Context
import org.json.JSONObject

data class MadhwaItem(
    val id: String,
    val category: String,
    val title: String,
    val author: String,
    val note: String,
    val file: String
)

/** Local Madhva catalog: Kannada / English / Sanskrit texts on device. No WebView. */
object MadhwaStore {
    val CATEGORIES = listOf(
        "sampradaya", "stotra", "mantra", "dasara", "ugabhoga", "suladhi",
        "kesava", "haridasa", "yathi", "festival", "puja", "books"
    )

    const val STARS = "madhwa"

    @Volatile private var loaded = false
    private var items: List<MadhwaItem> = emptyList()
    private var byId: Map<String, MadhwaItem> = emptyMap()

    @Synchronized
    fun load(context: Context): List<MadhwaItem> {
        if (loaded) return items
        val raw = runCatching {
            context.assets.open("madhwa/catalog.json").bufferedReader().use { it.readText() }
        }.getOrDefault("")
        val arr = runCatching { JSONObject(raw).optJSONArray("items") }.getOrNull()
        val out = ArrayList<MadhwaItem>()
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val file = o.optString("file").ifBlank { o.optString("src") }
                if (file.isBlank()) continue
                out += MadhwaItem(
                    id = o.optString("id"),
                    category = o.optString("category"),
                    title = o.optString("title"),
                    author = o.optString("author"),
                    note = o.optString("note"),
                    file = file
                )
            }
        }
        items = out
        byId = out.associateBy { it.id }
        loaded = true
        return items
    }

    fun item(id: String): MadhwaItem? = byId[id]

    fun inCategory(category: String): List<MadhwaItem> =
        if (category.isBlank()) items else items.filter { it.category == category }

    fun search(query: String, category: String = ""): List<MadhwaItem> {
        val base = inCategory(category)
        val q = query.trim()
        if (q.isBlank()) return base
        return base.filter { haystack(it).contains(q, true) }
    }

    fun label(item: MadhwaItem, lang: IndicScript.Lang): String =
        IndicScript.convert(item.title, lang)

    fun author(item: MadhwaItem, lang: IndicScript.Lang): String =
        if (item.author.isBlank()) "" else IndicScript.convert(item.author, lang)

    fun note(item: MadhwaItem, lang: IndicScript.Lang): String =
        IndicScript.convert(item.note, lang)

    fun body(context: Context, item: MadhwaItem, lang: IndicScript.Lang): String {
        val raw = read(context, item.file)
        return IndicScript.convert(RitualText.tidy(raw), lang)
    }

    private fun read(context: Context, file: String): String {
        if (file.startsWith("lib:")) {
            val rest = file.removePrefix("lib:")
            val slash = rest.indexOf('/')
            if (slash <= 0) return ""
            return LibraryStore.text(
                context,
                rest.substring(0, slash),
                rest.substring(slash + 1),
                IndicScript.Lang.SA
            )
        }
        val path = if (file.startsWith("madhwa/")) file else "madhwa/texts/$file"
        return runCatching {
            context.assets.open(path).bufferedReader().use { it.readText() }
        }.getOrDefault("")
    }

    private fun haystack(item: MadhwaItem): String =
        item.id + " " + item.title + " " + item.author + " " + item.note + " " + item.category + " " +
            IndicScript.convert(item.title, IndicScript.Lang.KN) + " " +
            IndicScript.convert(item.title, IndicScript.Lang.EN)
}
