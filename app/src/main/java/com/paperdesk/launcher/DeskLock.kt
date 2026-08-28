package com.paperdesk.launcher

import android.app.Activity
import android.content.Context
import android.view.KeyEvent
import com.paperdesk.launcher.ui.AlarmAlertActivity
import com.paperdesk.launcher.ui.LauncherActivity

object DeskLock {
    private const val PREFS = "paperdesk_lock"
    const val UNLOCK_KEY = KeyEvent.KEYCODE_VOLUME_DOWN

    fun isLocked(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("locked", false)

    fun setLocked(context: Context, locked: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("locked", locked)
            .commit()
        listeners.toList().forEach { it.invoke() }
    }

    fun lock(context: Context) = setLocked(context, true)
    fun unlock(context: Context) = setLocked(context, false)
    fun toggle(context: Context) = setLocked(context, !isLocked(context))

    fun bounce(activity: Activity): Boolean {
        if (activity is LauncherActivity || activity is AlarmAlertActivity) return false
        if (!isLocked(activity)) return false
        LockGuard.showDesk(activity)
        activity.finish()
        return true
    }

    private val listeners = mutableListOf<() -> Unit>()

    fun addListener(listener: () -> Unit) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }
}
