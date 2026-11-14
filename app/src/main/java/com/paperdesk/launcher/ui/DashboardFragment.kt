package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.JapaCatalog
import com.paperdesk.launcher.data.JapaStore
import com.paperdesk.launcher.data.OsLang
import com.paperdesk.launcher.data.PanchangaStore
import com.paperdesk.launcher.data.PracticeDayStore
import com.paperdesk.launcher.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class DashboardFragment : Fragment() {
    private var japa: JapaStore? = null
    private lateinit var practice: PracticeDayStore
    private val countFormat = NumberFormat.getIntegerInstance(Locale("en", "IN"))
    private val slotIds = listOf(
        R.id.dashboardJapaSlot0, R.id.dashboardJapaSlot1, R.id.dashboardJapaSlot2,
        R.id.dashboardJapaSlot3, R.id.dashboardJapaSlot4
    )
    private val knDays = listOf("ಭಾ", "ಸೋ", "ಮಂ", "ಬು", "ಗು", "ಶು", "ಶ")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.page_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        practice = PracticeDayStore(requireContext())
        view.findViewById<MaterialButton>(R.id.dashProgress).setOnClickListener {
            CarbonMotion.pressFlash(it)
            showProgressDialog()
        }
        CarbonMotion.fadeIn(view)
        refreshAll(view)
        viewLifecycleOwner.lifecycleScope.launch {
            val app = requireContext().applicationContext
            val store = withContext(Dispatchers.IO) { JapaStore(app) }
            if (!isAdded) return@launch
            japa = store
            bindJapaRow(view)
            bindProgressSummary(view)
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let { refreshAll(it) }
    }

    private fun refreshAll(view: View) {
        practice.refreshBestStreak()
        bindWeek(view)
        bindStreak(view)
        bindShortcuts(view)
        if (japa != null) bindJapaRow(view)
        bindProgressSummary(view)
    }

    private fun bindProgressSummary(view: View) {
        val btn = view.findViewById<MaterialButton>(R.id.dashProgress)
        val sandhya = practice.sandhya()
        val done = sandhya.toList().count { it }
        val japaLine = japa?.let { store ->
            val slots = SettingsStore(requireContext()).dashboardJapaSlots()
            val first = slots.firstOrNull() ?: "gayatri"
            val c = store.counts(first)
            "${countFormat.format(c.daily)}/${countFormat.format(c.dailyTarget)}"
        } ?: "—"
        val remain = 3 - done
        btn.text = when {
            done == 3 -> "ಇಂದು ಪೂರ್ಣ · 3/3 ಸಂಧ್ಯಾ · ಜಪ $japaLine"
            done == 0 -> "ಉಳಿದಿದೆ · 3 ಸಂಧ್ಯಾ · ಜಪ $japaLine"
            else -> "ಮುಗಿದಿದೆ $done · ಉಳಿದಿದೆ $remain · ಜಪ $japaLine"
        }
        btn.contentDescription = "Daily progress. Sandhya $done of 3. Japa $japaLine."
    }

    private fun bindShortcuts(view: View) {
        val settings = SettingsStore(requireContext())
        val slots = settings.dashboardShortcuts()
        val lang = OsLang.get(requireContext())
        val cells = listOf(
            R.id.dashboardShortcut0 to R.id.dashboardShortcutLabel0,
            R.id.dashboardShortcut1 to R.id.dashboardShortcutLabel1,
            R.id.dashboardShortcut2 to R.id.dashboardShortcutLabel2
        )
        cells.forEachIndexed { index, (cellId, labelId) ->
            val destId = slots.getOrElse(index) { SettingsStore.DASHBOARD_SHORTCUT_DEFAULTS[index] }
            val cell = view.findViewById<LinearLayout>(cellId)
            val label = view.findViewById<TextView>(labelId)
            label.text = OsLang.ui(destId, lang)
            cell.contentDescription = getString(R.string.dashboard_shortcut_hint)
            cell.setOnClickListener {
                (activity as? LauncherActivity)?.navigateTo(destId)
            }

        }
    }


    private fun bindWeek(view: View) {
        val week = view.findViewById<LinearLayout>(R.id.dashboardWeek)
        week.removeAllViews()
        val density = resources.displayMetrics.density
        val gap = resources.getDimensionPixelSize(R.dimen.carbon_spacing_02)
        val todayKey = PanchangaStore.dateKey()
        val day = Calendar.getInstance().apply { add(Calendar.DATE, -6) }
        repeat(7) { index ->
            val key = PanchangaStore.dateKey(day)
            val flags = practice.sandhya(key)
            val marks = listOf(flags.first, flags.second, flags.third)
            val done = marks.count { it }
            val future = key > todayKey
            val pendingToday = key == todayKey && done < 3
            val status = CarbonStatus.sandhyaDay(done, future, pendingToday)
            val markText = when (status) {
                CarbonStatus.FUTURE -> "– – –"
                CarbonStatus.PENDING -> marks.joinToString(" ") { if (it) "✓" else "–" }
                CarbonStatus.COMPLETE -> "✓ ✓ ✓"
                CarbonStatus.SKIPPED -> marks.joinToString(" ") { if (it) "✓" else "×" }
            }
            val dayName = knDays[day.get(Calendar.DAY_OF_WEEK) - 1]

            val wrap = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_sandhya_day)
                isClickable = !future
                isFocusable = !future
                contentDescription = when (status) {
                    CarbonStatus.FUTURE -> "$dayName · upcoming"
                    CarbonStatus.COMPLETE -> "$dayName · complete"
                    CarbonStatus.SKIPPED -> "$dayName · incomplete"
                    CarbonStatus.PENDING -> "$dayName · $done of 3 · tap to record"
                }
                minimumWidth = resources.getDimensionPixelSize(R.dimen.carbon_layout_size_height_lg)
            }
            wrap.addView(View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (3 * density).toInt()
                )
                setBackgroundColor(ContextCompat.getColor(requireContext(), status.accent))
            })
            val body = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(0, (6 * density).toInt(), 0, (6 * density).toInt())
            }
            body.addView(TextView(requireContext()).apply {
                text = dayName
                gravity = Gravity.CENTER
                setTextAppearance(R.style.TextAppearance_Carbon_BodyCompact01)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.carbon_text))
            })
            body.addView(TextView(requireContext()).apply {
                text = markText
                gravity = Gravity.CENTER
                setTextAppearance(R.style.TextAppearance_Carbon_Label01)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.carbon_text_secondary))
            })
            wrap.addView(body, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            ))
            if (!future) {
                val dateForCell = key
                wrap.setOnClickListener {
                    CarbonMotion.pressFlash(it)
                    showProgressDialog(dateForCell)
                }
            }
            week.addView(
                wrap,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                    if (index > 0) marginStart = gap
                }
            )
            day.add(Calendar.DATE, 1)
        }
    }

    private fun bindStreak(view: View) {
        val current = practice.currentStreak()
        val best = practice.bestStreak()
        view.findViewById<TextView>(R.id.dashStreakCurrent).text = countFormat.format(current)
        view.findViewById<TextView>(R.id.dashStreakBest).text = "best ${countFormat.format(best)}"
        view.findViewById<View>(R.id.dashStreak).contentDescription =
            "Current streak ${countFormat.format(current)} days. Best ${countFormat.format(best)}."
    }

    private fun bindJapaRow(view: View) {
        val store = japa ?: return
        val slots = SettingsStore(requireContext()).dashboardJapaSlots()
        slotIds.forEachIndexed { index, viewId ->
            val cell = view.findViewById<LinearLayout>(viewId)
            val mantraId = slots.getOrElse(index) { if (index == 0) "gayatri" else "narayana" }
            val entry = JapaCatalog.entries.firstOrNull { it.id == mantraId }
                ?: JapaCatalog.entries.first()
            val counts = store.counts(entry.id)
            val target = counts.dailyTarget.coerceAtLeast(1)
            val pct = ((counts.daily * 100) / target).toInt().coerceIn(0, 100)
            val label = if (index == 0) "ಗಾಯತ್ರೀ" else shortLabel(entry.label)
            cell.removeAllViews()
            cell.gravity = Gravity.BOTTOM
            // Daily, Sankalpa and cumulative match the approved mockup.
            cell.addView(TextView(requireContext()).apply {
                text = "Daily "+countFormat.format(counts.daily)+"/"+countFormat.format(target)
                setTextAppearance(R.style.TextAppearance_Carbon_HeadingCompact01)
                setTextColor(requireContext().getColor(R.color.carbon_text))
            })
            cell.addView(TextView(requireContext()).apply {
                text = "Sankalpa "+countFormat.format(counts.sankalpa)+"\nCumulative "+countFormat.format(counts.cumulative)
                setTextAppearance(R.style.TextAppearance_Carbon_Label01)
                setTextColor(requireContext().getColor(R.color.carbon_text_secondary))
            })
            cell.addView(TextView(requireContext()).apply {
                text = label
                setTextAppearance(R.style.TextAppearance_Carbon_BodyCompact01)
                setTextColor(requireContext().getColor(R.color.carbon_text))
                setPadding(0, resources.getDimensionPixelSize(R.dimen.carbon_spacing_02), 0, 0)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                includeFontPadding = false
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            })
            cell.contentDescription =
                "$label $pct percent. Next target ${countFormat.format(target)}. Tap to open japa."
            cell.setOnClickListener {
                CarbonMotion.pressFlash(it)
                store.setActiveMantraId(entry.id)
                (activity as? LauncherActivity)?.navigateTo("japa")
            }

        }
    }

    private fun shortLabel(label: String): String =
        label.substringBefore("—").substringBefore("·").trim()

    private fun showProgressDialog(date: String = PanchangaStore.dateKey()) {
        val content = layoutInflater.inflate(R.layout.dialog_dashboard_progress, null, false)
        val store = japa
        val today = date == PanchangaStore.dateKey()
        fun paintJapa() {
            if (store == null) return
            val slots = SettingsStore(requireContext()).dashboardJapaSlots()
            val lines = slots.map { id ->
                val entry = JapaCatalog.entries.firstOrNull { it.id == id }
                val c = store.counts(id)
                val name = if (id == "gayatri") "ಗಾಯತ್ರೀ" else shortLabel(entry?.label ?: id)
                "$name ${countFormat.format(c.daily)}/${countFormat.format(c.dailyTarget)} · Σ ${countFormat.format(c.cumulative)}"
            }
            val active = store.counts()
            content.findViewById<TextView>(R.id.dialogJapaCount).text = countFormat.format(active.daily)
            content.findViewById<TextView>(R.id.dialogJapaMeta).text = lines.joinToString("\n")
        }
        fun refreshDash() {
            view?.let { refreshAll(it) }
        }
        val (pratah, madhya, sayam) = practice.sandhya(date)
        content.findViewById<MaterialCheckBox>(R.id.dialogSandhyaPratah).apply {
            isChecked = pratah
            isEnabled = true
            setOnCheckedChangeListener { _, checked ->
                practice.setSandhya(PracticeDayStore.SESSION_PRATAH, checked, date)
                refreshDash()
            }
        }
        content.findViewById<MaterialCheckBox>(R.id.dialogSandhyaMadhyahna).apply {
            isChecked = madhya
            setOnCheckedChangeListener { _, checked ->
                practice.setSandhya(PracticeDayStore.SESSION_MADHYAHNA, checked, date)
                refreshDash()
            }
        }
        content.findViewById<MaterialCheckBox>(R.id.dialogSandhyaSayam).apply {
            isChecked = sayam
            setOnCheckedChangeListener { _, checked ->
                practice.setSandhya(PracticeDayStore.SESSION_SAYAM, checked, date)
                refreshDash()
            }
        }
        content.findViewById<MaterialCheckBox>(R.id.dialogStotra1).apply {
            isChecked = practice.stotra("hari_vayu", date)
            setOnCheckedChangeListener { _, checked ->
                practice.setStotra("hari_vayu", checked, date)
            }
        }
        content.findViewById<MaterialCheckBox>(R.id.dialogStotra2).apply {
            isChecked = practice.stotra("rama_dwadasha", date)
            setOnCheckedChangeListener { _, checked ->
                practice.setStotra("rama_dwadasha", checked, date)
            }
        }
        paintJapa()
        content.findViewById<MaterialButton>(R.id.dialogJapaInc).apply {
            isEnabled = today
            setOnClickListener {
                store?.add(1)
                paintJapa()
                refreshDash()
            }
        }
        content.findViewById<MaterialButton>(R.id.dialogJapaUndo).apply {
            isEnabled = today
            setOnClickListener {
                store?.undo()
                paintJapa()
                refreshDash()
            }
        }
        content.findViewById<MaterialButton>(R.id.dialogOpenJapa).setOnClickListener {
            (activity as? LauncherActivity)?.navigateTo("japa")
        }
        content.findViewById<MaterialButton>(R.id.dialogOpenGrantha).setOnClickListener {
            (activity as? LauncherActivity)?.navigateTo("grantha")
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(date)
            .setView(content)
            .setPositiveButton("ಸರಿ", null)
            .show()
    }
}
