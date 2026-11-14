package com.paperdesk.launcher.data

import android.content.Context
import org.json.JSONObject

/** Offline line studies with separately identified bibliographic references. */
object RangavalliStore {
    data class Design(val id: String, val titleKn: String, val titleEn: String, val category: String,
                      val asset: String, val steps: List<String>, val sourceUrl: String, val sourceTitle: String)
    fun load(context: Context): List<Design> {
        fun json(path: String) = context.assets.open(path).bufferedReader().use { JSONObject(it.readText()) }
        val sources = json("rangavalli/sources.json").getJSONArray("sources")
        val sourceMap = (0 until sources.length()).associate { i ->
            val source = sources.getJSONObject(i)
            source.getString("id") to (source.getString("url") to source.optString("title"))
        }
        val items = json("rangavalli/index.json").getJSONArray("items")
        return (0 until items.length()).map { index ->
            val item = items.getJSONObject(index)
            val steps = item.getJSONArray("steps_kn")
            val source = sourceMap[item.getString("source_id")]
            val asset = item.getString("asset")
            require(asset.matches(Regex("rangavalli/[a-z_]+\\.png")))
            Design(item.getString("id"), item.getString("title_kn"), item.getString("title_en"),
                item.getString("category"), asset, (0 until steps.length()).map { steps.getString(it) },
                source?.first.orEmpty(), source?.second.orEmpty())
        }
    }
}
