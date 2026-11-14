package com.paperdesk.launcher

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/** Carbon theme tokens mapped to Android night/day host. */
object DeskTheme {
    const val BLACK = "black"
    const val PAPER = "paper"
    const val G100 = "g100"
    const val G90 = "g90"
    const val G10 = "g10"
    const val WHITE = "white"

    private const val PREFS = "madhvanidhi_theme"

    val CARBON_OPTIONS = listOf(
        G100 to "Dark",
        G90 to "Dark (soft)",
        G10 to "Light",
        WHITE to "Light (bright)"
    )

    fun current(context: Context): String =
        context.getSharedPreferences(PREFS, 0).getString("theme", G100) ?: G100

    fun carbonId(context: Context): String {
        val raw = current(context)
        return when (raw) {
            BLACK, "dark" -> G100
            PAPER, "light" -> G10
            G100, G90, G10, WHITE -> raw
            else -> G100
        }
    }

    fun isDark(name: String): Boolean = when (name) {
        PAPER, G10, WHITE, "light" -> false
        else -> true
    }

    fun apply(context: Context, name: String = current(context)) {
        val id = when (name) {
            BLACK -> G100
            PAPER -> G10
            else -> name
        }
        AppCompatDelegate.setDefaultNightMode(
            if (isDark(id)) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    fun set(context: Context, name: String) {
        val id = when (name) {
            BLACK -> G100
            PAPER -> G10
            else -> name
        }
        context.getSharedPreferences(PREFS, 0).edit().putString("theme", id).apply()
        apply(context, id)
    }
}
