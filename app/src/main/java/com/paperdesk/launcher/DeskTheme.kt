package com.paperdesk.launcher

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object DeskTheme {
    const val BLACK = "black"
    const val PAPER = "paper"
    fun current(context: Context): String = context.getSharedPreferences("madhvanidhi_theme", 0).getString("theme", BLACK) ?: BLACK
    fun apply(context: Context, name: String = current(context)) {
        AppCompatDelegate.setDefaultNightMode(if (name == PAPER) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES)
    }
    fun set(context: Context, name: String) {
        context.getSharedPreferences("madhvanidhi_theme", 0).edit().putString("theme", name).apply()
        apply(context, name)
    }
}
