package com.paperdesk.launcher.data

import android.content.Context

/** Starred puja paddhati, stotra, and namavali texts. */
object FavStore {
    private const val PREFS = "paperdesk_fav"

    fun canStar(collection: String): Boolean =
        collection == LibraryStore.PUJA ||
            collection == LibraryStore.STOTRA ||
            collection == LibraryStore.NAMAVALI ||
            collection == MadhwaStore.STARS

    fun starred(context: Context, collection: String, id: String): Boolean =
        ids(context, collection).contains(id)

    fun toggle(context: Context, collection: String, id: String): Boolean {
        val next = ids(context, collection).toMutableSet()
        if (!next.add(id)) next.remove(id)
        context.getSharedPreferences(PREFS, 0)
            .edit()
            .putStringSet(collection, next)
            .apply()
        return id in next
    }

    fun ids(context: Context, collection: String): Set<String> {
        return context.getSharedPreferences(PREFS, 0)
            .getStringSet(collection, emptySet())
            ?.toSet()
            ?: emptySet()
    }

    fun sort(items: List<LibraryItem>, starred: Set<String>): List<LibraryItem> {
        if (starred.isEmpty()) return items
        val fav = items.filter { it.id in starred }
        val rest = items.filter { it.id !in starred }
        return fav + rest
    }

    data class StarItem(val collection: String, val item: LibraryItem)

    /** Starred puja/stotra/namavali first, then remaining stotra and namavali. */
    fun catalog(context: Context): List<StarItem> {
        val out = ArrayList<StarItem>()
        val seen = HashSet<String>()
        fun add(col: String, item: LibraryItem) {
            val k = "$col/${item.id}"
            if (seen.add(k)) out += StarItem(col, item)
        }
        for (col in listOf(LibraryStore.PUJA, LibraryStore.STOTRA, LibraryStore.NAMAVALI)) {
            val starred = ids(context, col)
            LibraryStore.load(context, col).filter { it.id in starred }.forEach { add(col, it) }
        }
        for (col in listOf(LibraryStore.STOTRA, LibraryStore.NAMAVALI)) {
            LibraryStore.load(context, col).forEach { add(col, it) }
        }
        return out
    }
}
