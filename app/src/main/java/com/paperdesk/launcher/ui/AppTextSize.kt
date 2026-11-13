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
        val scale = scale(context)
        // Avoid createConfigurationContext on the hot path when user has not changed text size.
        if (scale == 1f) return context
        val configuration = Configuration().apply {
            fontScale = context.resources.configuration.fontScale * scale
        }
        return context.createConfigurationContext(configuration)
    }
}
