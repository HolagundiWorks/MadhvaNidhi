package com.paperdesk.launcher.data

import android.content.Context
import org.json.JSONObject

/** Offline cookbook: ingredient facts, original preparation summaries and attribution. */
object NaivedyaStore {
    data class Ingredient(val name: String, val quantity: String)
    data class Recipe(val id: String, val title: String, val englishTitle: String, val batch: String,
                      val time: String, val ingredients: List<Ingredient>, val steps: List<String>,
                      val note: String, val author: String, val sourceUrl: String)
    data class Cookbook(val offeringNote: String, val recipes: List<Recipe>)
    fun load(context: Context): Cookbook {
        fun json(path: String) = context.assets.open(path).bufferedReader().use { JSONObject(it.readText()) }
        val sources = json("naivedya/sources.json").getJSONArray("sources")
        val byId = (0 until sources.length()).associate { sources.getJSONObject(it).let { s -> s.getString("id") to s } }
        val data = json("naivedya/index.json")
        val items = data.getJSONArray("items")
        return Cookbook(data.getString("offering_note_kn"), (0 until items.length()).map { i ->
            val r = items.getJSONObject(i)
            val ingredients = r.getJSONArray("ingredients")
            val steps = r.getJSONArray("steps_kn")
            val source = requireNotNull(byId[r.getString("source_id")])
            Recipe(r.getString("id"), r.getString("title_kn"), r.getString("title_en"),
                r.getString("batch_kn"), r.getString("time_kn"),
                (0 until ingredients.length()).map { ingredients.getJSONObject(it).let { item ->
                    Ingredient(item.getString("name_kn"), item.getString("quantity_kn")) } },
                (0 until steps.length()).map { steps.getString(it) }, r.optString("note_kn"),
                source.getString("author"), source.getString("url"))
        })
    }
}
