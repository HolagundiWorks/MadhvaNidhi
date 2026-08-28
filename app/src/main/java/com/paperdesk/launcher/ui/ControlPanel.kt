package com.paperdesk.launcher.ui

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.OsLang
import com.paperdesk.launcher.data.SettingsStore

class ControlPanel(private val host: Activity) {

    private lateinit var shade: View
    private lateinit var panel: View
    private lateinit var scrim: View
    private lateinit var bar: SeekBar
    private lateinit var value: TextView
    private lateinit var audio: AudioManager
    private var shown = false
    private var dragging = false
    private var openedAt = 0L

    private val volumeRx = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!dragging) bindBar()
        }
    }

    fun attach() {
        shade = host.findViewById(R.id.controlShade)
        panel = host.findViewById(R.id.controlPanel)
        scrim = host.findViewById(R.id.controlScrim)
        bar = host.findViewById(R.id.controlSound)
        value = host.findViewById(R.id.controlSoundVal)
        audio = host.getSystemService(AudioManager::class.java)
        DeskType.apply(host.findViewById(R.id.controlSoundLabel), DeskType.SEMIBOLD)
        DeskType.apply(host.findViewById(R.id.controlAlwaysLabel), DeskType.SEMIBOLD)
        DeskType.apply(host.findViewById(R.id.controlSettingsLabel), DeskType.SEMIBOLD)
        host.findViewById<View>(R.id.shadeGuard).setOnClickListener { toggle() }
        host.findViewById<View>(R.id.controlAlwaysOn).setOnClickListener { toggleAlwaysOn() }
        host.findViewById<View>(R.id.controlSettings).setOnClickListener {
            close()
            host.startActivity(Intent(host, AppSettingsActivity::class.java))
        }
        bindAlways()
        bindLang()
        scrim.setOnClickListener {
            if (android.os.SystemClock.uptimeMillis() - openedAt < 400L) return@setOnClickListener
            close()
        }
        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                value.text = progress.toString()
                if (fromUser) {
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                dragging = true
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                dragging = false
                bindBar()
            }
        })
    }

    fun isOpen(): Boolean = shown

    fun toggle() {
        if (shown) close() else open()
    }

    fun open() {
        if (shown) return
        shown = true
        openedAt = android.os.SystemClock.uptimeMillis()
        bindBar()
        bindAlways()
        bindLang()
        shade.visibility = View.VISIBLE
        shade.alpha = 1f
        panel.post {
            panel.translationY = -panel.height.toFloat()
            scrim.alpha = 0f
            panel.animate().translationY(0f).setDuration(160).start()
            scrim.animate().alpha(1f).setDuration(160).start()
        }
        runCatching {
            host.registerReceiver(volumeRx, IntentFilter("android.media.VOLUME_CHANGED_ACTION"))
        }
    }

    fun close() {
        if (!shown) return
        shown = false
        runCatching { host.unregisterReceiver(volumeRx) }
        val hide = Runnable { if (!shown) shade.visibility = View.GONE }
        panel.animate().translationY(-panel.height.toFloat()).setDuration(140).start()
        scrim.animate().alpha(0f).setDuration(140).withEndAction(hide).start()
    }

    private fun bindBar() {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val cur = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        bar.max = max
        bar.progress = cur
        value.text = cur.toString()
    }

    private fun toggleAlwaysOn() {
        val s = SettingsStore(host)
        s.setAlwaysOn(!s.alwaysOn())
        bindAlways()
        (host as? LauncherActivity)?.applyAlwaysOn()
    }

    private fun bindAlways() {
        val on = SettingsStore(host).alwaysOn()
        val btn = host.findViewById<TextView>(R.id.controlAlwaysVal)
        btn.text = OsLang.ui(if (on) "on" else "off", OsLang.get(host))
        if (on) {
            btn.setBackgroundResource(R.drawable.bg_circle_on)
            btn.setTextColor(host.getColor(R.color.accent))
        } else {
            btn.setBackgroundResource(R.drawable.bg_circle)
            btn.setTextColor(host.getColor(R.color.white))
        }
        DeskType.apply(btn, DeskType.SEMIBOLD)
    }

    fun bindLang() {
        if (!::shade.isInitialized) return
        val lang = OsLang.get(host)
        host.findViewById<TextView>(R.id.controlSoundLabel).text = OsLang.ui("sound", lang)
        host.findViewById<TextView>(R.id.controlAlwaysLabel).text = OsLang.ui("always_on", lang)
        host.findViewById<TextView>(R.id.controlSettingsLabel).text = host.getString(R.string.settings_title)
        bindAlways()
    }
}
