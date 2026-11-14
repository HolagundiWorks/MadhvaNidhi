package com.paperdesk.launcher.ui

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.os.Bundle
import com.paperdesk.launcher.data.SongFavoritesStore
import com.paperdesk.launcher.data.CatalogueStore
import java.util.UUID

/** Isolated test profiles: never edits the active user's favorites. */
class SongFavoritesInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val id = "song-test-${UUID.randomUUID()}"
        val result = Bundle()
        var code = Activity.RESULT_OK
        try {
            val store = SongFavoritesStore(targetContext, id)
            store.set("song-one", true)
            store.set("song-one", true)
            check(store.ids() == setOf("song-one"))
            check(SongFavoritesStore(targetContext, id).contains("song-one"))
            check(SongFavoritesStore(targetContext, "$id-other").ids().isEmpty())
            val copy = store.ids().toMutableSet()
            copy.clear()
            check(store.contains("song-one"))
            store.set("song-one", false)
            check(store.ids().isEmpty())
            CatalogueStore.load(targetContext)
            check(CatalogueStore.groupsForSubpage("namavali", "namavali").isNotEmpty())
            check(CatalogueStore.groupsFor("hadugalu").size >= 3)
            result.putString("stream", "PASS: favorite add/remove, idempotency, persistence, profile isolation and catalogue grouping")
        } catch (error: Throwable) {
            code = Activity.RESULT_CANCELED
            result.putString("stream", android.util.Log.getStackTraceString(error))
        } finally {
            targetContext.getSharedPreferences(SongFavoritesStore.PREFS, Context.MODE_PRIVATE)
                .edit().remove("profile_${id}_songs").remove("profile_${id}-other_songs").commit()
        }
        finish(code, result)
    }
}
