package com.paperdesk.launcher.ui

import android.content.Context
import kotlin.math.roundToInt

/** Persisted, clamped text size multiplier strictly for the landscape ContentReaderActivity. */
object ReaderTextScale {
    private const val PREFS = "madhvanidhi_reader_font"
    private const val KEY_SCALE = "reader_scale"

    const val MIN_SCALE = 0.85f
    const val MAX_SCALE = 2.20f
    const val STEP = 0.15f
    const val DEFAULT_SCALE = 1.0f
    const val BASE_BODY_SP = 17f
    const val BASE_HEAD_SP = 21f
    const val BASE_LINE_HEIGHT_SP = 26f

    fun clamp(scale: Float): Float =
        ((scale * 100f).roundToInt() / 100f).coerceIn(MIN_SCALE, MAX_SCALE)

    fun get(context: Context): Float =
        clamp(context.getSharedPreferences(PREFS, 0).getFloat(KEY_SCALE, DEFAULT_SCALE))

    fun set(context: Context, scale: Float): Float {
        val clamped = clamp(scale)
        context.getSharedPreferences(PREFS, 0).edit().putFloat(KEY_SCALE, clamped).apply()
        return clamped
    }

    fun stepUp(current: Float): Float = clamp(current + STEP)

    fun stepDown(current: Float): Float = clamp(current - STEP)

    fun bodySp(scale: Float): Float = BASE_BODY_SP * clamp(scale)

    fun headSp(scale: Float): Float = BASE_HEAD_SP * clamp(scale)

    fun lineHeightSp(scale: Float): Float = BASE_LINE_HEIGHT_SP * clamp(scale)

    fun percentLabel(scale: Float): String = "${(clamp(scale) * 100f).roundToInt()}%"
}
