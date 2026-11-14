package com.paperdesk.launcher

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import com.paperdesk.launcher.data.DemoUser
import com.paperdesk.launcher.data.SettingsStore
import kotlin.concurrent.thread

class PaperDeskApp : Application() {

    override fun onCreate() {
        super.onCreate()
        com.paperdesk.launcher.DeskTheme.apply(this)
        // Seed demo sadhaka (profile + streaks + japa) before UI binds Sankalpa/Puja.
        thread(name = "demo-user-seed") { DemoUser.ensure(this) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALARM_CHANNEL,
                getString(R.string.alarm_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.alarm_channel_description)
                enableVibration(true)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
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

    companion object {
        const val ALARM_CHANNEL = "madhvanidhi_alarms"
    }
}
