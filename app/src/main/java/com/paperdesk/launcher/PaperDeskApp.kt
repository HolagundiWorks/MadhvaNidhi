package com.paperdesk.launcher

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import com.paperdesk.launcher.data.CatalogueStore
import com.paperdesk.launcher.data.FestivalStore
import com.paperdesk.launcher.data.SecurePrefs
import com.paperdesk.launcher.data.SettingsStore
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class PaperDeskApp : Application() {
    private val alwaysOnCached = AtomicBoolean(false)
    private val alwaysOnReady = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()
        // Night mode once here — avoid a second apply in activity that can force recreate.
        DeskTheme.apply(this)
        val app = applicationContext
        Executors.newSingleThreadExecutor { r -> Thread(r, "madhva-warmup") }.execute {
            runCatching {
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
            }
            runCatching { SecurePrefs.warm(app) }
            runCatching {
                val on = SettingsStore(app).alwaysOn()
                alwaysOnCached.set(on)
                alwaysOnReady.set(true)
            }
            runCatching { FestivalStore.load(app) }
            runCatching { CatalogueStore.load(app) }
        }
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {
                val on = if (alwaysOnReady.get()) alwaysOnCached.get()
                else SettingsStore(activity).alwaysOn().also {
                    alwaysOnCached.set(it)
                    alwaysOnReady.set(true)
                }
                DeskWindow.keepAwake(activity, on)
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
