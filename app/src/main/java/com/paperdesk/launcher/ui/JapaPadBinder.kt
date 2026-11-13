package com.paperdesk.launcher.ui

import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.JapaCatalog
import com.paperdesk.launcher.data.JapaStore
import java.text.NumberFormat
import java.util.Locale

/** Frozen Japa pad UI binder (toolbar + anga tabs + count/goal tiles). */
object JapaPadBinder {
    private val countFormat = NumberFormat.getIntegerInstance(Locale("en", "IN"))
    private const val DEFAULT_DAILY = 101L

    private val poorvanga =
        "Sankalpa and preparatory angas before the main mantra count. Sample text — catalogue binding later."
    private val uttaranga =
        "Closing angas after the main count. Mark complete when the day’s japa is finished."

    fun bind(host: Fragment, root: View) {
        val store = JapaStore(host.requireContext())
        val prefs = host.requireContext().getSharedPreferences("paperdesk_japa_ui", 0)
        var dailyGoal = prefs.getLong("daily_goal", DEFAULT_DAILY).coerceAtLeast(1)
        val baseGoal = prefs.getLong("base_daily_goal", dailyGoal).coerceAtLeast(1)

        val mantra = root.findViewById<MaterialAutoCompleteTextView>(R.id.japaMantra)
        MaterialChoices.bind(mantra, JapaCatalog.entries.map { it.label }, JapaCatalog.indexOf(store.state().mantra)) {
            val entry = JapaCatalog.entries[it]
            store.configure(entry.mantra, dailyGoal, true)
            paint(root, store.state(), dailyGoal)
        }

        val angaTabs = root.findViewById<TabLayout>(R.id.japaAngaTabs)
        angaTabs.removeAllTabs()
        angaTabs.addTab(angaTabs.newTab().setText("ಪೂರ್ವಾಂಗ"))
        angaTabs.addTab(angaTabs.newTab().setText("ಉತ್ತರಾಂಗ"))
        val angaBody = root.findViewById<TextView>(R.id.japaAngaBody)
        angaBody.text = poorvanga
        angaTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                angaBody.text = if (tab.position == 0) poorvanga else uttaranga
            }
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })

        fun addGoal(delta: Long) {
            dailyGoal = (dailyGoal + delta).coerceAtMost(10_00_001L)
            store.configure(store.state().mantra, dailyGoal, true)
            prefs.edit().putLong("daily_goal", dailyGoal).apply()
            paint(root, store.state(), dailyGoal)
            root.findViewById<TextView>(R.id.japaStatus).text = "Daily goal ${countFormat.format(dailyGoal)}"
        }

        root.findViewById<View>(R.id.japaGoal11).setOnClickListener { addGoal(11) }
        root.findViewById<View>(R.id.japaGoal101).setOnClickListener { addGoal(101) }
        root.findViewById<View>(R.id.japaGoal1001).setOnClickListener { addGoal(1001) }
        root.findViewById<View>(R.id.japaReset).setOnClickListener {
            dailyGoal = baseGoal
            store.configure(store.state().mantra, dailyGoal, true)
            prefs.edit().putLong("daily_goal", dailyGoal).apply()
            paint(root, store.state(), dailyGoal)
            root.findViewById<TextView>(R.id.japaStatus).text = "Daily goal restored"
        }
        root.findViewById<View>(R.id.japaTap).setOnClickListener { button ->
            button.performHapticFeedback(
                HapticFeedbackConstants.KEYBOARD_TAP,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            paint(root, store.add(1), dailyGoal)
        }
        root.findViewById<View>(R.id.japaComplete).setOnClickListener {
            paint(root, store.complete(), dailyGoal)
            root.findViewById<TextView>(R.id.japaStatus).text = "Completed for today"
        }
        root.findViewById<View>(R.id.japaSkip).setOnClickListener {
            root.findViewById<TextView>(R.id.japaStatus).text = "Skipped today"
        }

        // Ensure target from pad goal
        store.configure(store.state().mantra, dailyGoal, true)
        paint(root, store.state(), dailyGoal)
    }

    private fun paint(root: View, state: JapaStore.State, dailyGoal: Long) {
        val done = state.current
        val target = dailyGoal.coerceAtLeast(1)
        root.findViewById<TextView>(R.id.japaCountDone).text = formatCount(done)
        root.findViewById<TextView>(R.id.japaCountTarget).text = countFormat.format(target)
    }

    private fun formatCount(value: Long): String =
        if (value < 100) String.format(Locale.US, "%02d", value) else countFormat.format(value)
}
