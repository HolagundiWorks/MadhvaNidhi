package com.paperdesk.launcher

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.WindowManager
import com.paperdesk.launcher.ui.LauncherActivity

object LockGuard {

    @Suppress("DEPRECATION")
    fun apply(activity: Activity) {
        DeskWindow.cutout(activity)
        if (Build.VERSION.SDK_INT >= 27) {
            activity.setShowWhenLocked(true)
            activity.setTurnScreenOn(true)
        }
        activity.window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        disableKeyguard(activity)
    }

    @Suppress("DEPRECATION")
    fun disableKeyguard(context: Context) {
        try {
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            km.newKeyguardLock("vishwakarma").disableKeyguard()
        } catch (_: Exception) {
        }
    }

    fun showDesk(context: Context) {
        disableKeyguard(context)
        val launch = Intent(context, LauncherActivity::class.java)
        launch.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
                or Intent.FLAG_ACTIVITY_SINGLE_TOP
                or Intent.FLAG_ACTIVITY_CLEAR_TOP
                or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        )
        context.startActivity(launch)
    }
}
