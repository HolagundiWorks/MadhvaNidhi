package com.paperdesk.launcher.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Favourite song ids for this device. There is no per-person profile in Madhva Nidhi.
 *
 * Earlier versions kept one set per profile (`profile_<id>_songs`). On first use those sets are merged into the
 * device set, so no favourite is lost. Copies prevent StringSet aliasing.
 */
class SongFavoritesStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    init {
        migrateLegacyProfiles()
    }

    fun ids(): Set<String> = prefs.getStringSet(KEY, emptySet()).orEmpty().toSet()
    fun contains(id: String): Boolean = id in ids()
    fun set(id: String, favorite: Boolean) {
        require(id.isNotBlank())
        val next = ids().toMutableSet()
        if (favorite) next.add(id) else next.remove(id)
        prefs.edit().putStringSet(KEY, next).apply()
    }

    private fun migrateLegacyProfiles() {
        if (prefs.getBoolean(MIGRATED, false)) return
        val merged = ids().toMutableSet()
        for ((name, value) in prefs.all) {
            if (name.startsWith("profile_") && name.endsWith("_songs") && value is Set<*>) {
                value.forEach { if (it is String && it.isNotBlank()) merged.add(it) }
            }
        }
        prefs.edit().putStringSet(KEY, merged).putBoolean(MIGRATED, true).apply()
    }

    companion object {
        const val PREFS = "madhvanidhi_song_favorites"
        private const val KEY = "songs"
        private const val MIGRATED = "migrated_from_profiles"

        /** The favourites as a JSON object with one array, for the encrypted backup. */
        fun exportAll(context: Context): JSONObject = JSONObject().also { out ->
            out.put("songs", JSONArray(SongFavoritesStore(context).ids().sorted()))
        }

        /** Restores favourites from a backup. Older backups keyed arrays by profile id; all of them are merged. */
        fun importAll(context: Context, value: JSONObject) {
            val store = SongFavoritesStore(context)
            val merged = store.ids().toMutableSet()
            val keys = value.keys()
            while (keys.hasNext()) {
                val arr = value.optJSONArray(keys.next()) ?: continue
                for (i in 0 until arr.length()) {
                    val id = arr.optString(i)
                    if (id.isNotBlank()) merged.add(id)
                }
            }
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putStringSet(KEY, merged).apply()
        }
    }
}
