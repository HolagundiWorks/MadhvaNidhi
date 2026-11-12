package com.paperdesk.launcher.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.paperdesk.launcher.DeskTheme
import com.paperdesk.launcher.DeskWindow
import com.paperdesk.launcher.R

class AppSettingsActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(AppTextSize.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        DeskTheme.apply(this)
        super.onCreate(savedInstanceState)
        setContentView(ScreenPages.wrap(layoutInflater.inflate(R.layout.activity_app_settings, null)))
        DeskWindow.padCutout(findViewById(android.R.id.content))
        DeskWindow.immersive(this)
        SettingsScreenBinder.bind(findViewById(android.R.id.content), this, showBack = true) { finish() }
    }

    override fun onResume() {
        super.onResume()
        SettingsScreenBinder.refreshProfileSummary(findViewById(android.R.id.content), this)
    }
}
