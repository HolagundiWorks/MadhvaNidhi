package com.paperdesk.launcher.alarm

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.paperdesk.launcher.PaperDeskApp
import com.paperdesk.launcher.R
import com.paperdesk.launcher.ui.AlarmAlertActivity

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val id = intent?.getStringExtra("id") ?: return
        val alarm = AlarmStore(context).byId(id)
        val alert = Intent(context, AlarmAlertActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("id", id)
            putExtra("label", alarm?.label ?: "")
            putExtra("time", alarm?.timeLabel() ?: "")
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            id.hashCode(),
            alert,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val label = alarm?.label?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.alarm_notification_title)
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, PaperDeskApp.ALARM_CHANNEL)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }.setSmallIcon(R.drawable.ic_madhvanidhi)
            .setContentTitle(label)
            .setContentText(alarm?.timeLabel() ?: "")
            .setCategory(Notification.CATEGORY_ALARM)
            .setPriority(Notification.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
            .build()

        val allowed = Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (allowed) {
            context.getSystemService(NotificationManager::class.java).notify(id.hashCode(), notification)
        }
        if (alarm != null && alarm.enabled) {
            AlarmScheduler.schedule(context, alarm)
        }
    }
}
