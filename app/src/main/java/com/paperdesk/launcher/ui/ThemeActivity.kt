package com.paperdesk.launcher.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.paperdesk.launcher.DeskLock
import com.paperdesk.launcher.DeskTheme
import com.paperdesk.launcher.DeskWindow
import com.paperdesk.launcher.R

class ThemeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        DeskTheme.apply(this)
        super.onCreate(savedInstanceState)
        if (DeskLock.bounce(this)) return
        setContentView(R.layout.activity_theme)
        DeskWindow.padCutout(findViewById(android.R.id.content))
        DeskWindow.immersive(this)
        findViewById<TextView>(R.id.themeBack).setOnClickListener { finish() }
        findViewById<android.view.View>(R.id.themeBlack).setOnClickListener { pick(DeskTheme.BLACK) }
        findViewById<android.view.View>(R.id.themePaper).setOnClickListener { pick(DeskTheme.PAPER) }
        findViewById<android.view.View>(R.id.themeAlwaysOn).setOnClickListener {
            val s = com.paperdesk.launcher.data.SettingsStore(this)
            s.setAlwaysOn(!s.alwaysOn())
            DeskWindow.keepAwake(this, s.alwaysOn())
            runCatching {
                com.paperdesk.launcher.bridge.OsRadios.apply(this, s.json())
            }
            paint()
        }
        paint()
    }

    override fun onResume() {
        super.onResume()
        if (DeskLock.bounce(this)) return
        val on = com.paperdesk.launcher.data.SettingsStore(this).alwaysOn()
        DeskWindow.keepAwake(this, on)
        paint()
    }

    private fun pick(name: String) {
        if (DeskTheme.current(this) == name) return
        DeskTheme.set(this, name)
        recreate()
    }

    private fun paint() {
        val on = DeskTheme.current(this)
        val black = findViewById<TextView>(R.id.themeBlackName)
        val paper = findViewById<TextView>(R.id.themePaperName)
        if (on == DeskTheme.PAPER) {
            DeskType.supporting(black)
            DeskType.primary(paper)
            paper.setTextColor(getColor(R.color.accent))
        } else {
            DeskType.primary(black)
            black.setTextColor(getColor(R.color.accent))
            DeskType.supporting(paper)
        }
        val always = findViewById<TextView>(R.id.themeAlwaysName)
        val alwaysOn = com.paperdesk.launcher.data.SettingsStore(this).alwaysOn()
        if (alwaysOn) {
            DeskType.primary(always)
            always.setTextColor(getColor(R.color.accent))
            findViewById<TextView>(R.id.themeAlwaysHint).text = getString(R.string.on)
        } else {
            DeskType.supporting(always)
            findViewById<TextView>(R.id.themeAlwaysHint).text = getString(R.string.always_on_hint)
        }
    }
}
