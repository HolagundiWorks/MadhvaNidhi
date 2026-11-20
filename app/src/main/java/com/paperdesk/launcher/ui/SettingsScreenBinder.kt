package com.paperdesk.launcher.ui

import android.content.Context
import android.view.View
import android.widget.TextView
import com.paperdesk.launcher.DeskTheme
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.IndicScript
import com.paperdesk.launcher.data.OsLang
import com.paperdesk.launcher.data.SettingsStore

object SettingsScreenBinder {
    fun bind(root: View, context: Context, showBack: Boolean, onBack: () -> Unit = {}) {
        MaterialLayouts.bind(root)
        root.findViewById<View>(R.id.settingsPageTitle).visibility = if (showBack) View.VISIBLE else View.GONE
        val settings = SettingsStore(context)
        root.findViewById<View>(R.id.settingsBack).apply {
            visibility = if (showBack) View.VISIBLE else View.GONE
            setOnClickListener { onBack() }
        }
        root.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.settingsAlwaysOn).apply {
            isChecked = settings.alwaysOn()
            setOnCheckedChangeListener { _, checked ->
                settings.setAlwaysOn(checked)
                (context as? android.app.Activity)?.let { com.paperdesk.launcher.DeskWindow.keepAwake(it, checked) }
            }
        }
        root.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.settingsThemeSwitch).apply {
            isChecked = DeskTheme.isDark(DeskTheme.current(context))
            setOnCheckedChangeListener { _, dark ->
                DeskTheme.set(context, if (dark) DeskTheme.G100 else DeskTheme.G10)
                (context as? android.app.Activity)?.recreate()
            }
        }
        val langChoices = listOf(
            IndicScript.Lang.KN to "ಕನ್ನಡ",
            IndicScript.Lang.SA to "संस्कृतम्",
            IndicScript.Lang.EN to "English"
        )
        val langIndex = langChoices.indexOfFirst { it.first == OsLang.get(context) }.coerceAtLeast(0)
        root.findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.settingsLang)?.let { langView ->
            MaterialChoices.bind(langView, langChoices.map { it.second }, langIndex) { index ->
                val next = langChoices[index].first
                if (OsLang.get(context) != next) {
                    OsLang.set(context, next)
                    (context as? android.app.Activity)?.recreate()
                }
            }
        }
        root.findViewById<TextView>(R.id.settingsVersion).text = context.getString(
            R.string.settings_version,
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        )
    }
}
