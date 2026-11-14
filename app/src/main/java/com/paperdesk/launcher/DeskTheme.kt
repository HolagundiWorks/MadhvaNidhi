package com.paperdesk.launcher

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/** Light/dark Material themes; legacy preference values remain readable. */
object DeskTheme {
    const val BLACK = "black"
    const val PAPER = "paper"
    const val G100 = "g100"
    const val G90 = "g90"
    const val G10 = "g10"
    const val WHITE = "white"

    private const val PREFS = "madhvanidhi_theme"

    val OPTIONS = listOf(G10 to "Light", G100 to "Dark")

    fun current(context: Context): String =
        context.getSharedPreferences(PREFS, 0).getString("theme", G100) ?: G100

    fun themeId(context: Context): String {
        val raw = current(context)
        return if (isDark(raw)) G100 else G10
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
        val id = if (isDark(name)) G100 else G10
        context.getSharedPreferences(PREFS, 0).edit().putString("theme", id).apply()
        apply(context, id)
    }
}
