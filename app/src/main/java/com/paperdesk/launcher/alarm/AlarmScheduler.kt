package com.paperdesk.launcher.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.paperdesk.launcher.ui.AlarmAlertActivity

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
        val show = alertPending(context, alarm)
        if (Build.VERSION.SDK_INT >= 21) {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(whenMs, show), pi)
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, whenMs, pi)
        }
    }

    fun cancel(context: Context, id: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(context, id))
    }

    fun snooze(context: Context, id: String, minutes: Int = 10) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val whenMs = System.currentTimeMillis() + minutes * 60_000L
        val show = PendingIntent.getActivity(
            context,
            id.hashCode() xor 0x5a11,
            alertIntent(context, id, "", ""),
            flags()
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(whenMs, show), pending(context, id))
    }

    private fun pending(context: Context, id: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).putExtra("id", id)
        return PendingIntent.getBroadcast(context, id.hashCode(), intent, flags())
    }

    private fun alertPending(context: Context, alarm: AlarmItem): PendingIntent {
        return PendingIntent.getActivity(
            context,
            alarm.id.hashCode() xor 0x5a11,
            alertIntent(context, alarm.id, alarm.label, alarm.timeLabel()),
            flags()
        )
    }

    private fun alertIntent(context: Context, id: String, label: String, time: String): Intent {
        return Intent(context, AlarmAlertActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("id", id)
            putExtra("label", label)
            putExtra("time", time)
        }
    }

    private fun flags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
}
