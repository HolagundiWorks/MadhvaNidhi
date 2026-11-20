package com.paperdesk.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.paperdesk.launcher.alarm.AlarmScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        AlarmScheduler.rescheduleAll(context)
    }
}
