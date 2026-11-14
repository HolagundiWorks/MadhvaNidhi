package com.paperdesk.launcher.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Stable song IDs, scoped to the active Sadhak. Copies prevent StringSet aliasing. */
class SongFavoritesStore(context: Context, profileId: String = ProfileStore(context).active().id) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val key = "profile_${profileId}_songs"
    fun ids(): Set<String> = prefs.getStringSet(key, emptySet()).orEmpty().toSet()
    fun contains(id: String): Boolean = id in ids()
    fun set(id: String, favorite: Boolean) {
        require(id.isNotBlank())
        val next = ids().toMutableSet()
        if (favorite) next.add(id) else next.remove(id)
        prefs.edit().putStringSet(key,next).apply()
    }
    companion object {
        const val PREFS = "madhvanidhi_song_favorites"
        fun exportAll(context: Context): JSONObject = JSONObject().also { out ->
            ProfileStore(context).profiles().forEach { p -> out.put(p.id, JSONArray(SongFavoritesStore(context,p.id).ids().sorted())) }
        }
        fun importAll(context: Context, value: JSONObject) {
            ProfileStore(context).profiles().forEach { p ->
                val arr = value.optJSONArray(p.id) ?: return@forEach
                val ids = (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }.toSet()
                context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putStringSet("profile_${p.id}_songs",ids).apply()
            }
        }
    }
}
