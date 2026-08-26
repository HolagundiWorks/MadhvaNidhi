package com.paperdesk.launcher

import android.content.Context
import android.os.SystemClock
import android.view.KeyEvent

object ShadeLock {
    private const val PREFS = "paperdesk_shade"
    private const val WINDOW_MS = 2500L
    private const val TAPS = 5

    private val taps = LongArray(TAPS)
    private var tapAt = 0

    fun isLocked(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("locked", true)

    fun setLocked(context: Context, locked: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("locked", locked)
            .commit()
        listeners.toList().forEach { it.invoke() }
    }

    fun toggle(context: Context) = setLocked(context, !isLocked(context))

    fun isVolumeKey(code: Int): Boolean =
        code == KeyEvent.KEYCODE_VOLUME_UP || code == KeyEvent.KEYCODE_VOLUME_DOWN

    fun onVolume(): Boolean {
        val now = SystemClock.uptimeMillis()
        taps[tapAt % TAPS] = now
        tapAt++
        if (tapAt < TAPS) return false
        val oldest = (0 until TAPS).minOf { taps[it] }
        if (now - oldest in 1L..WINDOW_MS) {
            tapAt = 0
            taps.fill(0L)
            return true
        }
        return false
    }

    private val listeners = mutableListOf<() -> Unit>()

    fun addListener(listener: () -> Unit) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }
}
