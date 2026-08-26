package com.paperdesk.launcher

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import com.paperdesk.launcher.data.SettingsStore

class PaperDeskApp : Application() {

    private val wake = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            LockGuard.disableKeyguard(context)
            if (DeskLock.isLocked(context)) LockGuard.showDesk(context)
        }
    }

    override fun onCreate() {
        super.onCreate()
        LockGuard.disableKeyguard(this)
        com.paperdesk.launcher.DeskTheme.apply(this)
        runCatching { com.paperdesk.launcher.bridge.OsDataService.ensureLayout(this) }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(wake, filter)
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {
                DeskWindow.keepAwake(activity, SettingsStore(activity).alwaysOn())
            }
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
