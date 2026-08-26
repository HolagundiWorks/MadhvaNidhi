package com.paperdesk.launcher.data

import android.content.Context
import org.json.JSONObject

/** Verse text from GitaVani (nikhilsi/gitavani), LGPL-3.0. */
object GitaStore {
    data class Chapter(val n: Int, val count: Int, val name: String, val meaning: String)
    data class Verse(
        val c: Int,
        val v: Int,
        val slok: String,
        val tr: String,
        val en: String,
        val hi: String
    )

    @Volatile private var loaded = false
    var chapters: List<Chapter> = emptyList()
        private set
    var verses: List<Verse> = emptyList()
        private set
    private var byChapter: Map<Int, List<Verse>> = emptyMap()

    @Synchronized
    fun load(context: Context) {
        if (loaded) return
        val raw = context.assets.open("gita.json").bufferedReader().use { it.readText() }
        val root = JSONObject(raw)
        val chArr = root.getJSONArray("chapters")
        val ch = ArrayList<Chapter>(chArr.length())
        for (i in 0 until chArr.length()) {
            val o = chArr.getJSONObject(i)
            ch += Chapter(
                n = o.getInt("n"),
                count = o.getInt("count"),
                name = o.optString("name"),
                meaning = o.optString("meaning")
            )
        }
        val vArr = root.getJSONArray("verses")
        val vs = ArrayList<Verse>(vArr.length())
        for (i in 0 until vArr.length()) {
            val o = vArr.getJSONObject(i)
            vs += Verse(
                c = o.getInt("c"),
                v = o.getInt("v"),
                slok = o.optString("slok"),
                tr = o.optString("tr"),
                en = o.optString("en"),
                hi = o.optString("hi")
            )
        }
        chapters = ch
        verses = vs
        byChapter = vs.groupBy { it.c }
        loaded = true
    }

    fun versesIn(chapter: Int): List<Verse> = byChapter[chapter] ?: emptyList()

    fun chapter(n: Int): Chapter? = chapters.firstOrNull { it.n == n }

    fun next(current: Verse): Verse? {
        val i = verses.indexOfFirst { it.c == current.c && it.v == current.v }
        if (i < 0 || i + 1 >= verses.size) return verses.firstOrNull()
        return verses[i + 1]
    }

    fun prev(current: Verse): Verse? {
        val i = verses.indexOfFirst { it.c == current.c && it.v == current.v }
        if (i <= 0) return verses.lastOrNull()
        return verses[i - 1]
    }

    fun isLast(current: Verse): Boolean {
        val last = verses.lastOrNull() ?: return true
        return current.c == last.c && current.v == last.v
    }
}
