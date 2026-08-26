package com.paperdesk.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.paperdesk.launcher.alarm.AlarmScheduler
import com.paperdesk.launcher.bridge.OsRadios
import com.paperdesk.launcher.data.SettingsStore

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        AlarmScheduler.rescheduleAll(context)
        DeskLock.lock(context)
        runCatching { OsRadios.apply(context, SettingsStore(context).json()) }
        LockGuard.showDesk(context)
    }
}
