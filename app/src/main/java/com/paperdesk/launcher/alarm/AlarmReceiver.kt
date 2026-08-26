package com.paperdesk.launcher.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
        context.startActivity(alert)
        if (alarm != null && alarm.enabled) {
            AlarmScheduler.schedule(context, alarm)
        }
    }
}
