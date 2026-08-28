package com.paperdesk.launcher.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object AlarmScheduler {
    fun rescheduleAll(context: Context) {
        val store = AlarmStore(context)
        store.load().forEach { schedule(context, it) }
    }

    fun schedule(context: Context, alarm: AlarmItem) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pending(context, alarm.id)
        if (!alarm.enabled) {
            am.cancel(pi)
            return
        }
        val whenMs = alarm.nextTriggerMs()
        am.setWindow(AlarmManager.RTC_WAKEUP, whenMs, 10 * 60_000L, pi)
    }

    fun cancel(context: Context, id: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(context, id))
    }

    fun snooze(context: Context, id: String, minutes: Int = 10) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val whenMs = System.currentTimeMillis() + minutes * 60_000L
        am.setWindow(AlarmManager.RTC_WAKEUP, whenMs, 60_000L, pending(context, id))
    }

    private fun pending(context: Context, id: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).putExtra("id", id)
        return PendingIntent.getBroadcast(context, id.hashCode(), intent, flags())
    }

    private fun flags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
}
