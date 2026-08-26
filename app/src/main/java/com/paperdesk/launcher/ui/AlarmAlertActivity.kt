package com.paperdesk.launcher.ui

import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.paperdesk.launcher.DeskWindow
import com.paperdesk.launcher.R
import com.paperdesk.launcher.alarm.AlarmScheduler

class AlarmAlertActivity : AppCompatActivity() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                or WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_alarm_alert)
        DeskWindow.immersive(this)
        DeskWindow.padCutout(findViewById(android.R.id.content))

        val id = intent.getStringExtra("id") ?: ""
        findViewById<TextView>(R.id.alertTime).text = intent.getStringExtra("time") ?: ""
        val label = intent.getStringExtra("label").orEmpty()
        findViewById<TextView>(R.id.alertLabel).text = if (label.isBlank()) "Alarm" else label

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        try {
            vibrator?.vibrate(longArrayOf(0, 500, 300, 500), 0)
        } catch (_: Exception) {
        }
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(this, uri)
            ringtone?.streamType = AudioManager.STREAM_ALARM
            ringtone?.play()
        } catch (_: Exception) {
        }

        findViewById<TextView>(R.id.alertDismiss).setOnClickListener {
            silence()
            finish()
        }
        findViewById<TextView>(R.id.alertSnooze).setOnClickListener {
            if (id.isNotEmpty()) AlarmScheduler.snooze(this, id, 10)
            silence()
            finish()
        }
    }

    private fun silence() {
        try { ringtone?.stop() } catch (_: Exception) {}
        try { vibrator?.cancel() } catch (_: Exception) {}
    }

    override fun onDestroy() {
        silence()
        super.onDestroy()
    }
}
