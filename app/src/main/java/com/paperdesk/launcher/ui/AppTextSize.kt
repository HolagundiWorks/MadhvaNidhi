package com.paperdesk.launcher.ui

import android.content.Context
import android.content.res.Configuration

/** App text scaling uses standard Android font configuration, not custom widgets. */
object AppTextSize {
    private const val PREFS = "madhvanidhi_accessibility"
    fun scale(context: Context): Float = context.getSharedPreferences(PREFS, 0).getFloat("text_scale", 1f).coerceIn(1f, 2f)
    fun save(context: Context, scale: Float) {
        context.getSharedPreferences(PREFS, 0).edit().putFloat("text_scale", scale.coerceIn(1f, 2f)).apply()
    }
    fun wrap(context: Context): Context {
        // Override only text scaling; keep window size, density and orientation live.
        val configuration = Configuration().apply {
            fontScale = context.resources.configuration.fontScale * scale(context)
        }
        return context.createConfigurationContext(configuration)
    }
}
