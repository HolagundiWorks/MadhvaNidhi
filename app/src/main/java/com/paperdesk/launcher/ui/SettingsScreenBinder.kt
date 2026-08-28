package com.paperdesk.launcher.ui

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.PanchangaTraditions
import com.paperdesk.launcher.data.SettingsStore

object SettingsScreenBinder {
    fun bind(root: View, context: Context, showBack: Boolean, onBack: () -> Unit = {}) {
        val settings = SettingsStore(context)
        var binding = true
        root.findViewById<View>(R.id.settingsBack).apply {
            visibility = if (showBack) View.VISIBLE else View.GONE
            setOnClickListener { onBack() }
        }

        val vedaChoices = listOf("ಋಗ್ವೇದ — ಆಶ್ವಲಾಯನ", "ಕೃಷ್ಣ ಯಜುರ್ವೇದ — ತೈತ್ತಿರೀಯ")
        root.findViewById<Spinner>(R.id.settingsVeda).apply {
            adapter = ArrayAdapter(context, R.layout.item_dropdown_selected, vedaChoices).also {
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

        val selected = settings.panchangaTradition()
        val hint = root.findViewById<TextView>(R.id.settingsPanchangaHint)
        root.findViewById<Spinner>(R.id.settingsPanchanga).apply {
            adapter = ArrayAdapter(context, R.layout.item_dropdown_selected, PanchangaTraditions.all.map { it.kn }).also {
                it.setDropDownViewResource(R.layout.item_dropdown)
            }
            setSelection(PanchangaTraditions.all.indexOfFirst { it.id == selected }.coerceAtLeast(0), false)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val tradition = PanchangaTraditions.all[position]
                    if (!binding) settings.setPanchangaTradition(tradition.id)
                    hint.setText(if (tradition.liveDailyData) R.string.settings_panchanga_live else R.string.settings_panchanga_reference)
                }
            }
        }
        hint.setText(if (PanchangaTraditions.find(selected).liveDailyData) R.string.settings_panchanga_live else R.string.settings_panchanga_reference)
        root.findViewById<TextView>(R.id.settingsVersion).text = context.getString(
            R.string.settings_version,
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        )
        binding = false
    }
}
