package com.paperdesk.launcher.ui

import android.content.Context
import android.provider.Settings
import android.view.View
import android.view.ViewPropertyAnimator
import com.paperdesk.launcher.R

/** Carbon productive motion helper for Views. */
object CarbonMotion {
    fun reduced(context: Context): Boolean =
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f

    fun ms(context: Context, resId: Int): Long {
        if (reduced(context)) return 0L
        return context.resources.getInteger(resId).toLong()
    }

    fun fadeIn(view: View, durationRes: Int = R.integer.carbon_duration_fast_02): ViewPropertyAnimator {
        val d = ms(view.context, durationRes)
        view.alpha = 0f
        return view.animate().alpha(1f).setDuration(d)
    }

    fun pressFlash(view: View) {
        val d = ms(view.context, R.integer.carbon_duration_fast_01)
        if (d == 0L) return
        view.animate().cancel()
        view.animate().alpha(0.72f).setDuration(d).withEndAction {
            view.animate().alpha(1f).setDuration(d).start()
        }.start()
    }
}
