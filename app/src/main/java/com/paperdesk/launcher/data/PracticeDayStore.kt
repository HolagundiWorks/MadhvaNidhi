package com.paperdesk.launcher.data

import android.content.Context

/** Local daily practice flags for the Carbon dashboard. Not a streak engine yet. */
class PracticeDayStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun sandhya(date: String = PanchangaStore.dateKey()): Triple<Boolean, Boolean, Boolean> =
        Triple(get(date, "pratah"), get(date, "madhyahna"), get(date, "sayam"))

    fun setSandhya(session: String, done: Boolean, date: String = PanchangaStore.dateKey()) {
        prefs.edit().putBoolean(key(date, session), done).apply()
    }

    fun stotra(id: String, date: String = PanchangaStore.dateKey()): Boolean = get(date, "stotra_$id")

    fun setStotra(id: String, done: Boolean, date: String = PanchangaStore.dateKey()) {
        prefs.edit().putBoolean(key(date, "stotra_$id"), done).apply()
    }

    private fun get(date: String, name: String) = prefs.getBoolean(key(date, name), false)
    private fun key(date: String, name: String) = "${date}_$name"

    companion object {
        private const val PREFS = "madhvanidhi_practice_day"
    }
}
