package com.paperdesk.launcher.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.paperdesk.launcher.DeskTheme
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.IndicScript
import com.paperdesk.launcher.data.JapaCatalog
import com.paperdesk.launcher.data.OsLang
import com.paperdesk.launcher.data.PanchangaTraditions
import com.paperdesk.launcher.data.ProfileStore
import com.paperdesk.launcher.data.SettingsStore

object SettingsScreenBinder {
    fun bind(root: View, context: Context, showBack: Boolean, onBack: () -> Unit = {}) {
        MaterialLayouts.bind(root)
        root.findViewById<View>(R.id.settingsPageTitle).visibility = if (showBack) View.VISIBLE else View.GONE
        val settings = SettingsStore(context)
        refreshProfileSummary(root, context)
        bindJapaTables(root, context, settings)
        root.findViewById<View>(R.id.settingsOpenProfile).setOnClickListener {
            (context as? LauncherActivity)?.navigateTo("user")
                ?: context.startActivity(ProfileRegisterActivity.intent(context))
        }
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
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        root.findViewById<com.google.android.material.slider.Slider>(R.id.settingsVolume).apply {
            valueFrom = 0f
            valueTo = audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).coerceAtLeast(1).toFloat()
            stepSize = 1f
            value = audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC).toFloat().coerceIn(valueFrom, valueTo)
            addOnChangeListener { _, selected, fromUser ->
                if (fromUser) audio.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, selected.toInt(), 0)
            }
        }
        val textSlider = root.findViewById<com.google.android.material.slider.Slider>(R.id.settingsTextSize)
        val textLabel = root.findViewById<TextView>(R.id.settingsTextSizeLabel)
        fun showTextSize(percent: Float) {
            textLabel.text = "ಅಕ್ಷರ ಗಾತ್ರ · ${percent.toInt()}%"
        }
        fun applyTextSize(percent: Float) {
            if (kotlin.math.abs(AppTextSize.scale(context) * 100f - percent) < 1f) return
            AppTextSize.save(context, percent / 100f)
            (context as? android.app.Activity)?.recreate()
        }
        textSlider.apply {
            valueFrom = 100f
            valueTo = 200f
            stepSize = 25f
            value = AppTextSize.scale(context) * 100f
            setLabelFormatter { "${it.toInt()}%" }
            addOnChangeListener { _, percent, _ -> showTextSize(percent) }
            addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) = Unit
                override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) { applyTextSize(slider.value) }
            })
        }
        root.findViewById<View>(R.id.settingsTextApply).setOnClickListener { applyTextSize(textSlider.value) }
        showTextSize(textSlider.value)
        val themeIds = DeskTheme.CARBON_OPTIONS.map { it.first }
        val themeLabels = DeskTheme.CARBON_OPTIONS.map { it.second }
        val themeIndex = themeIds.indexOf(DeskTheme.carbonId(context)).coerceAtLeast(0)
        root.findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.settingsTheme)?.let { themeView ->
            MaterialChoices.bind(themeView, themeLabels, themeIndex) { index ->
                val next = themeIds[index]
                if (DeskTheme.carbonId(context) != next) {
                    DeskTheme.set(context, next)
                    (context as? android.app.Activity)?.recreate()
                }
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
        root.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.settingsDarkTheme)?.apply {
            // Legacy toggle kept hidden; Carbon theme dropdown is the authority.
            visibility = View.GONE
        }
        val vedaChoices = listOf("ಋಗ್ವೇದ — ಆಶ್ವಲಾಯನ", "ಕೃಷ್ಣ ಯಜುರ್ವೇದ — ತೈತ್ತಿರೀಯ")
        MaterialChoices.bind(root.findViewById(R.id.settingsVeda), vedaChoices,
            SettingsStore.VEDA_IDS.indexOf(settings.veda()).coerceAtLeast(0)) { settings.setVeda(SettingsStore.VEDA_IDS[it]) }
        val hint = root.findViewById<TextView>(R.id.settingsPanchangaHint)
        fun showHint(index: Int) {
            hint.setText(if (PanchangaTraditions.all[index].liveDailyData) R.string.settings_panchanga_live else R.string.settings_panchanga_reference)
        }
        val selected = PanchangaTraditions.all.indexOfFirst { it.id == settings.panchangaTradition() }.coerceAtLeast(0)
        MaterialChoices.bind(root.findViewById(R.id.settingsPanchanga), PanchangaTraditions.all.map { it.kn }, selected) {
            settings.setPanchangaTradition(PanchangaTraditions.all[it].id)
            showHint(it)
        }
        showHint(selected)
        root.findViewById<TextView>(R.id.settingsVersion).text = context.getString(
            R.string.settings_version,
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        )
    }

    fun refreshProfileSummary(root: View, context: Context) {
        val summary = root.findViewById<TextView>(R.id.settingsProfileSummary) ?: return
        val p = ProfileStore(context).active()
        val parts = buildList {
            add(p.name)
            if (p.gotra.isNotBlank() && p.gotra != "ತಿಳಿಸಿಲ್ಲ") add("${p.gotra} ಗೋತ್ರ")
            if (p.rashi.isNotBlank() && p.rashi != "ತಿಳಿಸಿಲ್ಲ") add("${p.rashi} ರಾಶಿ")
            if (p.nakshatra.isNotBlank() && p.nakshatra != "ತಿಳಿಸಿಲ್ಲ") add("${p.nakshatra} ನಕ್ಷತ್ರ")
            if (p.yearOfBirth > 0) add(p.yearOfBirth.toString())
            if (p.placeOfBirth.isNotBlank()) add(p.placeOfBirth)
        }
        summary.text =
            if (parts.size <= 1 && (p.name == "ಸಾಧಕ" || p.name.isBlank())) {
                context.getString(R.string.profile_summary_empty)
            } else {
                parts.joinToString(" · ")
            }
        root.findViewById<LinearLayout>(R.id.settingsJapaTargetsTable)?.let {
            JapaTargetsTable.paintReadonly(it, context)
        }
    }

    private fun bindJapaTables(root: View, context: Context, settings: SettingsStore) {
        root.findViewById<LinearLayout>(R.id.settingsJapaTargetsTable)?.let {
            JapaTargetsTable.paintReadonly(it, context)
        }
        val host = root.findViewById<LinearLayout>(R.id.settingsDashboardJapaSlots) ?: return
        host.removeAllViews()
        val slots = settings.dashboardJapaSlots().toMutableList()
        val labels = JapaCatalog.entries.map { it.label }
        val ids = JapaCatalog.entries.map { it.id }
        slots.forEachIndexed { index, mantraId ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, 0, 0, context.resources.getDimensionPixelSize(R.dimen.carbon_spacing_03))
            }
            row.addView(TextView(context).apply {
                text = if (index == 0) "1 · Gayatri" else "${index + 1}"
                setTextAppearance(R.style.TextAppearance_Carbon_Label01)
                setTextColor(context.getColor(R.color.carbon_text_secondary))
                layoutParams = LinearLayout.LayoutParams(
                    context.resources.getDimensionPixelSize(R.dimen.carbon_spacing_09),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            })
            if (index == 0) {
                row.addView(TextView(context).apply {
                    text = JapaCatalog.entries.firstOrNull { it.id == "gayatri" }?.label ?: "ಗಾಯತ್ರೀ"
                    setTextAppearance(R.style.TextAppearance_Carbon_BodyCompact01)
                    setTextColor(context.getColor(R.color.carbon_text))
                })
            } else {
                val picker = MaterialAutoCompleteTextView(context)
                val selected = ids.indexOf(mantraId).coerceAtLeast(0)
                MaterialChoices.bind(picker, labels, selected) { choice ->
                    slots[index] = ids[choice]
                    slots[0] = "gayatri"
                    settings.setDashboardJapaSlots(slots)
                }
                row.addView(picker, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            }
            host.addView(row)
        }
        bindShortcutPickers(root, context, settings)
    }

    private fun bindShortcutPickers(root: View, context: Context, settings: SettingsStore) {
        val host = root.findViewById<LinearLayout>(R.id.settingsDashboardShortcuts) ?: return
        host.removeAllViews()
        val slots = settings.dashboardShortcuts().toMutableList()
        val ids = SettingsStore.DASHBOARD_SHORTCUT_IDS
        val lang = com.paperdesk.launcher.data.OsLang.get(context)
        val labels = ids.map { com.paperdesk.launcher.data.OsLang.ui(it, lang) }
        slots.forEachIndexed { index, destId ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, 0, 0, context.resources.getDimensionPixelSize(R.dimen.carbon_spacing_03))
            }
            row.addView(TextView(context).apply {
                text = "${index + 1}"
                setTextAppearance(R.style.TextAppearance_Carbon_Label01)
                setTextColor(context.getColor(R.color.carbon_text_secondary))
                layoutParams = LinearLayout.LayoutParams(
                    context.resources.getDimensionPixelSize(R.dimen.carbon_spacing_07),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            })
            val picker = MaterialAutoCompleteTextView(context)
            val selected = ids.indexOf(destId).coerceAtLeast(0)
            MaterialChoices.bind(picker, labels, selected) { choice ->
                slots[index] = ids[choice]
                settings.setDashboardShortcuts(slots)
            }
            row.addView(picker, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            host.addView(row)
        }
    }
}
