package com.paperdesk.launcher

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import com.paperdesk.launcher.data.CatalogueStore
import com.paperdesk.launcher.data.FestivalStore
import com.paperdesk.launcher.data.SettingsStore
import java.util.concurrent.Executors

class PaperDeskApp : Application() {

    override fun onCreate() {
        super.onCreate()
        DeskTheme.apply(this)
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
        // Warm heavy asset/JSON stores off the main thread before first reader/desk open.
        val app = applicationContext
        Executors.newSingleThreadExecutor { r -> Thread(r, "madhva-warmup") }.execute {
            runCatching { FestivalStore.load(app) }
            runCatching { CatalogueStore.load(app) }
            runCatching { SettingsStore(app).pages() }
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
