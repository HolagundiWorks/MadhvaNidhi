package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.paperdesk.launcher.DeskTheme
import com.paperdesk.launcher.DeskWindow
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.PanchangaTraditions
import com.paperdesk.launcher.data.SettingsStore

class AppSettingsActivity : AppCompatActivity() {
    private lateinit var settings: SettingsStore
    private var binding = true

    override fun onCreate(savedInstanceState: Bundle?) {
        DeskTheme.apply(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_settings)
        DeskWindow.padCutout(findViewById(android.R.id.content))
        DeskWindow.immersive(this)
        settings = SettingsStore(this)
        findViewById<View>(R.id.settingsBack).setOnClickListener { finish() }
        bindVeda()
        bindPanchanga()
        findViewById<TextView>(R.id.settingsVersion).text = getString(
            R.string.settings_version,
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0"
        )
        binding = false
    }

    private fun bindVeda() {
        val choices = listOf(
            "ಋಗ್ವೇದ — ಆಶ್ವಲಾಯನ",
            "ಕೃಷ್ಣ ಯಜುರ್ವೇದ — ತೈತ್ತಿರೀಯ"
        )
        findViewById<Spinner>(R.id.settingsVeda).apply {
            adapter = ArrayAdapter(this@AppSettingsActivity, R.layout.item_dropdown_selected, choices).also {
                it.setDropDownViewResource(R.layout.item_dropdown)
            }
            setSelection(SettingsStore.VEDA_IDS.indexOf(settings.veda()).coerceAtLeast(0), false)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (!binding) settings.setVeda(SettingsStore.VEDA_IDS[position])
                }
            }
        }
    }

    private fun bindPanchanga() {
        val selected = settings.panchangaTradition()
        findViewById<Spinner>(R.id.settingsPanchanga).apply {
            adapter = ArrayAdapter(
                this@AppSettingsActivity,
                R.layout.item_dropdown_selected,
                PanchangaTraditions.all.map { it.kn }
            ).also { it.setDropDownViewResource(R.layout.item_dropdown) }
            setSelection(PanchangaTraditions.all.indexOfFirst { it.id == selected }.coerceAtLeast(0), false)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (!binding) settings.setPanchangaTradition(PanchangaTraditions.all[position].id)
                    bindSourceNote(PanchangaTraditions.all[position].liveDailyData)
                }
            }
        }
        bindSourceNote(PanchangaTraditions.find(selected).liveDailyData)
    }

    private fun bindSourceNote(live: Boolean) {
        findViewById<TextView>(R.id.settingsPanchangaHint).setText(
            if (live) R.string.settings_panchanga_live else R.string.settings_panchanga_reference
        )
    }
}
