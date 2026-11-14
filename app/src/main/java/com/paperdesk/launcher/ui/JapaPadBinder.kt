package com.paperdesk.launcher.ui

import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.IndicScript
import com.paperdesk.launcher.data.JapaCatalog
import com.paperdesk.launcher.data.JapaGoals
import com.paperdesk.launcher.data.JapaStore
import com.paperdesk.launcher.data.OsLang
import com.paperdesk.launcher.data.PracticeDayStore
import com.paperdesk.launcher.data.ProfileStore
import com.paperdesk.launcher.data.SandhyaStore
import com.paperdesk.launcher.data.Sankalpa
import java.text.NumberFormat
import java.util.Locale

/** Frozen Japa pad UI binder (toolbar + anga tabs + count/goal tiles). */
object JapaPadBinder {
    private val countFormat = NumberFormat.getIntegerInstance(Locale("en", "IN"))

    fun bind(host: Fragment, root: View) {
        val context = host.requireContext()
        val store = JapaStore(context)
        val practice = PracticeDayStore(context)
        val profile = ProfileStore(context).active()
        val lang = OsLang.get(context)
        val mantraIds = profile.dikshaMantras.ifEmpty { listOf("gayatri") }
            .filter { id -> JapaCatalog.entries.any { it.id == id } }
            .ifEmpty { listOf(JapaCatalog.entries.first().id) }
        val entries = mantraIds.mapNotNull { id -> JapaCatalog.entries.firstOrNull { it.id == id } }
        var activeId = store.activeMantraId().takeIf { it in mantraIds } ?: mantraIds.first()

        fun dailyTargetFor(id: String): Long =
            (profile.japaTargets[id]?.first ?: store.counts(id).dailyTarget)
                .coerceAtLeast(1)

        var dailyGoal = dailyTargetFor(activeId)
        val profileDaily = mantraIds.associateWith { dailyTargetFor(it) }

        fun poorvanga(): String {
            val purpose = IndicScript.convert("श्रीहरिप्रीत्यर्थं जपं करिष्ये।", lang)
            return Sankalpa.block(context, SandhyaStore.nowId(), lang) + "\n\n" + purpose
        }
        fun uttaranga(): String = IndicScript.convert(
            "जपसमाप्तौ\n" +
                "यदक्षरपदभ्रष्टं मात्राहीनं च यद्भवेत्।\n" +
                "तत्सर्वं क्षम्यतां देव नारायण नमोऽस्तु ते॥\n\n" +
                "कायेन वाचा मनसेन्द्रियैर्वा बुद्ध्यात्मना वा प्रकृतेः स्वभावात्।\n" +
                "करोमि यद्यत् सकलं परस्मै नारायणायेति समर्पयामि॥",
            lang
        )

        val mantra = root.findViewById<MaterialAutoCompleteTextView>(R.id.japaMantra)
        val labels = entries.map { it.label }
        val startIndex = entries.indexOfFirst { it.id == activeId }.coerceAtLeast(0)
        MaterialChoices.bind(mantra, labels, startIndex) {
            val entry = entries[it]
            activeId = entry.id
            store.setActiveMantraId(entry.id)
            dailyGoal = profileDaily[entry.id] ?: dailyTargetFor(entry.id)
            store.configure(entry.mantra, dailyGoal, true)
            paint(root, store.counts(activeId), dailyGoal, practice)
        }

        val angaTabs = root.findViewById<TabLayout>(R.id.japaAngaTabs)
        angaTabs.removeAllTabs()
        angaTabs.addTab(angaTabs.newTab().setText("ಪೂರ್ವಾಂಗ"))
        angaTabs.addTab(angaTabs.newTab().setText("ಉತ್ತರಾಂಗ"))
        CarbonTabs.equalizeLeft(angaTabs)
        val angaBody = root.findViewById<TextView>(R.id.japaAngaBody)
        angaBody.text = poorvanga()
        angaTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                angaBody.text = if (tab.position == 0) poorvanga() else uttaranga()
            }
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        root.findViewById<View>(R.id.japaOpenAnga)?.setOnClickListener {
            val activeLabel = entries.firstOrNull { it.id == activeId }?.label ?: OsLang.ui("japa", lang)
            host.startActivity(
                ContentReaderActivity.forJapaAnga(
                    context,
                    activeLabel,
                    angaTabs.selectedTabPosition.coerceAtLeast(0)
                )
            )
        }

        fun addGoal(delta: Long) {
            dailyGoal = (dailyGoal + delta).coerceAtMost(JapaGoals.SANKALPA.last())
            val entry = JapaCatalog.entries.first { it.id == activeId }
            store.configure(entry.mantra, dailyGoal, true)
            paint(root, store.counts(activeId), dailyGoal, practice)
            root.findViewById<TextView>(R.id.japaStatus).text =
                "ದೈನಂದಿನ ಗುರಿ +${countFormat.format(delta)} → ${countFormat.format(dailyGoal)}"
        }

        root.findViewById<View>(R.id.japaGoal11).setOnClickListener { addGoal(11) }
        root.findViewById<View>(R.id.japaGoal101).setOnClickListener { addGoal(101) }
        root.findViewById<View>(R.id.japaGoal1001).setOnClickListener { addGoal(1001) }
        root.findViewById<View>(R.id.japaReset).setOnClickListener {
            dailyGoal = profileDaily[activeId] ?: dailyTargetFor(activeId)
            val entry = JapaCatalog.entries.first { it.id == activeId }
            store.configure(entry.mantra, dailyGoal, true)
            paint(root, store.counts(activeId), dailyGoal, practice)
            root.findViewById<TextView>(R.id.japaStatus).text =
                "ದೈನಂದಿನ ಗುರಿ ಮರುಸ್ಥಾಪನೆ · ${countFormat.format(dailyGoal)}"
        }
        root.findViewById<View>(R.id.japaTap).setOnClickListener { button ->
            button.performHapticFeedback(
                HapticFeedbackConstants.KEYBOARD_TAP,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            paint(root, store.add(1, activeId), dailyGoal, practice)
        }

        val skip = root.findViewById<MaterialButton>(R.id.japaSkip)
        val complete = root.findViewById<MaterialButton>(R.id.japaComplete)
        fun paintDayActions() {
            skip.isSelected = practice.japaSkipped()
            complete.isSelected = practice.japaComplete()
        }
        complete.setOnClickListener {
            val on = !practice.japaComplete()
            if (on) {
                val counts = store.counts(activeId)
                val need = (dailyGoal - counts.daily).coerceAtLeast(0)
                val after = if (need > 0) store.add(need, activeId) else counts
                practice.setJapaDay(complete = true, skipped = false)
                paint(root, after, dailyGoal, practice)
                root.findViewById<TextView>(R.id.japaStatus).text = "ಇಂದು ಪೂರ್ಣ · Completed"
            } else {
                practice.setJapaDay(complete = false, skipped = false)
                paint(root, store.counts(activeId), dailyGoal, practice)
            }
            paintDayActions()
        }
        skip.setOnClickListener {
            val on = !practice.japaSkipped()
            practice.setJapaDay(complete = false, skipped = on)
            paint(root, store.counts(activeId), dailyGoal, practice)
            root.findViewById<TextView>(R.id.japaStatus).text =
                if (on) "ಇಂದು ಬಿಟ್ಟಿದೆ · Skipped" else cumulativeStatus(store.counts(activeId))
            paintDayActions()
        }

        store.setActiveMantraId(activeId)
        val entry = JapaCatalog.entries.first { it.id == activeId }
        store.configure(entry.mantra, dailyGoal, true)
        paint(root, store.counts(activeId), dailyGoal, practice)
        paintDayActions()
    }

    private fun paint(
        root: View,
        counts: JapaStore.MantraCounts,
        dailyGoal: Long,
        practice: PracticeDayStore
    ) {
        val done = counts.daily
        val target = dailyGoal.coerceAtLeast(1)
        root.findViewById<TextView>(R.id.japaCountDone).text = formatCount(done)
        root.findViewById<TextView>(R.id.japaCountTarget).text = countFormat.format(target)
        val status = root.findViewById<TextView>(R.id.japaStatus)
        status.text = when {
            practice.japaComplete() -> "ಇಂದು ಪೂರ್ಣ · Completed"
            practice.japaSkipped() -> "ಇಂದು ಬಿಟ್ಟಿದೆ · Skipped"
            else -> cumulativeStatus(counts)
        }
    }

    private fun cumulativeStatus(counts: JapaStore.MantraCounts): String {
        val sankalpaLabel = if (counts.sankalpaConfigured > 0) {
            countFormat.format(counts.sankalpa)
        } else {
            "ಸ್ವಯಂ ${countFormat.format(counts.sankalpa)}"
        }
        return "ಒಟ್ಟು ${countFormat.format(counts.cumulative)} · ಸಂಕಲ್ಪ $sankalpaLabel"
    }

    private fun formatCount(value: Long): String =
        if (value < 100) String.format(Locale.US, "%02d", value) else countFormat.format(value)
}
