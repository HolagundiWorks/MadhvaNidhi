package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.DashboardBadges
import com.paperdesk.launcher.data.DemoUser
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
        viewLifecycleOwner.lifecycleScope.launch {
            val app = requireContext().applicationContext
            val store = withContext(Dispatchers.IO) {
                DemoUser.ensure(app)
                JapaStore(app)
            }
            if (!isAdded) return@launch
            japa = store
            practice = PracticeDayStore(requireContext())
            refreshAll(view)
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
        if (japa != null) {
            bindJapaRow(view)
            bindBadges(view)
        } else {
            bindBadges(view, japaCumulative = 0L)
        }
        bindProgressSummary(view)
    }

    private fun bindProgressSummary(view: View) {
        val btn = view.findViewById<MaterialButton>(R.id.dashProgress)
        val sandhya = practice.sandhya()
        val done = sandhya.toList().count { it }
        val week = practice.weeklySandhyaCompleteCount()
        val japaLine = japa?.let { store ->
            val slots = SettingsStore(requireContext()).dashboardJapaSlots()
            val first = slots.firstOrNull() ?: "gayatri"
            val c = store.counts(first)
            "${countFormat.format(c.daily)}/${countFormat.format(c.dailyTarget)}"
        } ?: "—"
        btn.text = "$done/3 · $week/7 · $japaLine"
        btn.contentDescription =
            "Daily progress. Sandhya $done of 3. Weekly $week of 7. Japa $japaLine."
    }

    private fun bindShortcuts(view: View) {
        val settings = SettingsStore(requireContext())
        val slots = settings.dashboardShortcuts()
        val lang = OsLang.get(requireContext())
        val cells = listOf(
            Triple(R.id.dashboardShortcut0, R.id.dashboardShortcutLabel0, R.id.dashboardShortcutIcon0),
            Triple(R.id.dashboardShortcut1, R.id.dashboardShortcutLabel1, R.id.dashboardShortcutIcon1),
            Triple(R.id.dashboardShortcut2, R.id.dashboardShortcutLabel2, R.id.dashboardShortcutIcon2)
        )
        cells.forEachIndexed { index, (cellId, labelId, iconId) ->
            val destId = slots.getOrElse(index) { SettingsStore.DASHBOARD_SHORTCUT_DEFAULTS[index] }
            val cell = view.findViewById<LinearLayout>(cellId)
            val label = view.findViewById<TextView>(labelId)
            val icon = view.findViewById<ImageView>(iconId)
            label.text = OsLang.ui(destId, lang)
            icon.setImageResource(shortcutIcon(destId))
            cell.contentDescription = OsLang.ui(destId, lang)
            cell.setOnClickListener {
                (activity as? LauncherActivity)?.navigateTo(destId)
            }
        }
    }

    private fun shortcutIcon(destId: String): Int = when (destId) {
        "desk", "panchanga" -> R.drawable.ic_calendar
        "japa" -> R.drawable.ic_badge_japa
        "grantha" -> R.drawable.ic_book
        "sandhya", "nitya", "nityakarma" -> R.drawable.ic_sun
        "puja" -> R.drawable.ic_sunny
        "sangeeta" -> R.drawable.ic_music
        "schedule" -> R.drawable.ic_today
        "user" -> R.drawable.ic_user
        else -> R.drawable.ic_launch
    }

    private fun bindWeek(view: View) {
        val week = view.findViewById<LinearLayout>(R.id.dashboardWeek)
        week.removeAllViews()
        val density = resources.displayMetrics.density
        val gap = resources.getDimensionPixelSize(R.dimen.carbon_spacing_02)
        val padH = resources.getDimensionPixelSize(R.dimen.carbon_spacing_03)
        val padV = resources.getDimensionPixelSize(R.dimen.carbon_spacing_02)
        val todayKey = PanchangaStore.dateKey()
        val completeDays = practice.weeklySandhyaCompleteCount()
        val weeklyBadge = DashboardBadges.evaluate(DashboardBadges.WEEKLY_TIERS, completeDays.toLong())

        view.findViewById<TextView>(R.id.dashWeekScore).text = "$completeDays/7"
        view.findViewById<LinearProgressIndicator>(R.id.dashWeekProgress).progress = completeDays
        view.findViewById<View>(R.id.dashWeek).contentDescription =
            "Sandhya week $completeDays of 7" +
                (weeklyBadge?.let { ". ${it.tier.hint}" } ?: "")

        view.findViewById<ImageView>(R.id.dashWeekBadgeIcon).visibility =
            if (weeklyBadge != null) View.VISIBLE else View.GONE

        val sessionIcons = listOf(R.drawable.ic_morning, R.drawable.ic_noon, R.drawable.ic_evening)
        val day = Calendar.getInstance().apply { add(Calendar.DATE, -6) }
        repeat(7) { index ->
            val key = PanchangaStore.dateKey(day)
            val flags = practice.sandhya(key)
            val marks = listOf(flags.first, flags.second, flags.third)
            val done = marks.count { it }
            val future = key > todayKey
            val isToday = key == todayKey
            val pendingToday = isToday && done < 3
            val dayComplete = done == 3
            val status = CarbonStatus.sandhyaDay(done, future, pendingToday)
            val dayName = knDays[day.get(Calendar.DAY_OF_WEEK) - 1]

            // Fitts ≥48dp rows; Von Restorff: today outlined; completed quiet (not full orange flood).
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                minimumHeight = (48 * density).toInt()
                setPadding(padH, padV, padH, padV)
                background = ContextCompat.getDrawable(
                    requireContext(),
                    when {
                        isToday -> R.drawable.bg_sandhya_day_today
                        dayComplete -> R.drawable.bg_sandhya_day
                        else -> R.drawable.bg_sandhya_day
                    }
                )
                alpha = when {
                    future -> 0.4f
                    dayComplete && !isToday -> 0.72f
                    else -> 1f
                }
                isClickable = !future
                isFocusable = !future
                contentDescription = when (status) {
                    CarbonStatus.FUTURE -> "$dayName · upcoming"
                    CarbonStatus.COMPLETE -> "$dayName · complete · $done of 3"
                    CarbonStatus.SKIPPED -> "$dayName · incomplete · $done of 3"
                    CarbonStatus.PENDING -> "$dayName · $done of 3 · tap to record"
                }
            }

            row.addView(TextView(requireContext()).apply {
                text = dayName
                minWidth = (36 * density).toInt()
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleSmall)
                setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        if (isToday) R.color.md_theme_primary else R.color.md_theme_onSurface
                    )
                )
            })

            val sessions = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL or Gravity.END
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            marks.forEachIndexed { i, on ->
                sessions.addView(ImageView(requireContext()).apply {
                    setImageResource(sessionIcons[i])
                    layoutParams = LinearLayout.LayoutParams(
                        (18 * density).toInt(),
                        (18 * density).toInt()
                    ).apply { if (i > 0) marginStart = (8 * density).toInt() }
                    imageTintList = android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            when {
                                future -> R.color.md_theme_outlineVariant
                                on -> R.color.md_theme_primary
                                else -> R.color.md_theme_outline
                            }
                        )
                    )
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                })
            }
            row.addView(sessions)

            if (dayComplete) {
                row.addView(ImageView(requireContext()).apply {
                    setImageResource(R.drawable.ic_check)
                    layoutParams = LinearLayout.LayoutParams(
                        (16 * density).toInt(),
                        (16 * density).toInt()
                    ).apply { marginStart = (6 * density).toInt() }
                    imageTintList = android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.md_theme_primary)
                    )
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                })
            }

            if (!future) {
                val dateForCell = key
                row.setOnClickListener {
                    CarbonMotion.pressFlash(it)
                    showProgressDialog(dateForCell)
                }
            }

            week.addView(
                row,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { if (index > 0) topMargin = gap }
            )
            day.add(Calendar.DATE, 1)
        }
    }

    private fun bindStreak(view: View) {
        val current = practice.currentStreak()
        val best = practice.bestStreak()
        val earned = DashboardBadges.evaluate(DashboardBadges.STREAK_TIERS, current.toLong())
        val next = DashboardBadges.nextTier(DashboardBadges.STREAK_TIERS, current.toLong())
        val progress = (DashboardBadges.progressToNext(DashboardBadges.STREAK_TIERS, current.toLong()) * 100).toInt()

        view.findViewById<TextView>(R.id.dashStreakCurrent).text = countFormat.format(current)
        view.findViewById<TextView>(R.id.dashStreakBest).text = "★ ${countFormat.format(best)}"
        view.findViewById<LinearProgressIndicator>(R.id.dashStreakProgress).progress = progress

        val nextLabel = view.findViewById<TextView>(R.id.dashStreakNext)
        nextLabel.text = if (next != null) {
            "+${next.threshold - current}"
        } else {
            "★"
        }

        view.findViewById<View>(R.id.dashStreak).contentDescription =
            "Streak ${countFormat.format(current)} days. Best ${countFormat.format(best)}. " +
                (earned?.let { it.tier.hint } ?: next?.hint.orEmpty())
    }

    private fun bindBadges(view: View, japaCumulative: Long? = null) {
        val tray = view.findViewById<LinearLayout>(R.id.dashBadgeTray)
        tray.removeAllViews()
        val streak = practice.currentStreak()
        val week = practice.weeklySandhyaCompleteCount()
        val cum = japaCumulative ?: japa?.totalCumulative() ?: 0L
        val live = DashboardBadges.allEarned(streak, week, cum)
        val demo = live.isEmpty()
        val earned = if (demo) DashboardBadges.demoEarned() else live

        // Hick: at most one earned peak + one next unlock.
        val inflater = layoutInflater
        val peak = earned.maxByOrNull { it.tier.threshold }
        if (peak != null) {
            val chip = inflater.inflate(R.layout.item_dash_badge, tray, false)
            chip.findViewById<ImageView>(R.id.badgeIcon).setImageResource(badgeIcon(peak.tier.kind))
            chip.findViewById<TextView>(R.id.badgeTitle).text = compactBadgeValue(peak)
            chip.contentDescription = peak.tier.hint
            tray.addView(chip)
        }

        if (!demo) {
            val next = listOfNotNull(
                DashboardBadges.nextTier(DashboardBadges.WEEKLY_TIERS, week.toLong()),
                DashboardBadges.nextTier(DashboardBadges.STREAK_TIERS, streak.toLong()),
                DashboardBadges.nextTier(DashboardBadges.JAPA_TIERS, cum)
            ).minByOrNull { it.threshold }
            if (next != null) {
                val chip = inflater.inflate(R.layout.item_dash_badge, tray, false)
                chip.alpha = 0.5f
                chip.findViewById<ImageView>(R.id.badgeIcon).setImageResource(badgeIcon(next.kind))
                chip.findViewById<TextView>(R.id.badgeTitle).text = compactThreshold(next.threshold)
                chip.contentDescription = "Next. ${next.hint}"
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                lp.marginStart = resources.getDimensionPixelSize(R.dimen.carbon_spacing_01)
                tray.addView(chip, lp)
            }
        } else if (earned.size > 1) {
            val second = earned[1]
            val chip = inflater.inflate(R.layout.item_dash_badge, tray, false)
            chip.findViewById<ImageView>(R.id.badgeIcon).setImageResource(badgeIcon(second.tier.kind))
            chip.findViewById<TextView>(R.id.badgeTitle).text = compactBadgeValue(second)
            chip.contentDescription = second.tier.hint
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            lp.marginStart = resources.getDimensionPixelSize(R.dimen.carbon_spacing_01)
            tray.addView(chip, lp)
        }
    }

    private fun badgeIcon(kind: DashboardBadges.Kind): Int = when (kind) {
        DashboardBadges.Kind.STREAK -> R.drawable.ic_badge_streak
        DashboardBadges.Kind.WEEKLY -> R.drawable.ic_badge_week
        DashboardBadges.Kind.JAPA -> R.drawable.ic_badge_japa
    }

    private fun compactBadgeValue(item: DashboardBadges.Earned): String = when (item.tier.kind) {
        DashboardBadges.Kind.STREAK -> countFormat.format(item.value)
        DashboardBadges.Kind.WEEKLY -> "${item.value}/7"
        DashboardBadges.Kind.JAPA -> compactThreshold(item.value)
    }

    private fun compactThreshold(value: Long): String = when {
        value >= 1_00_000 -> "${value / 1_00_000}ಲ"
        value >= 1_000 -> "${value / 1_000}ಸ"
        else -> countFormat.format(value)
    }

    private fun bindJapaRow(view: View) {
        val store = japa ?: return
        val slots = SettingsStore(requireContext()).dashboardJapaSlots()
        val density = resources.displayMetrics.density
        slotIds.forEachIndexed { index, viewId ->
            val cell = view.findViewById<LinearLayout>(viewId)
            val mantraId = slots.getOrElse(index) { if (index == 0) "gayatri" else "narayana" }
            val entry = JapaCatalog.entries.firstOrNull { it.id == mantraId }
                ?: JapaCatalog.entries.first()
            val counts = store.counts(entry.id)
            val target = counts.dailyTarget.coerceAtLeast(1)
            val pct = ((counts.daily * 100) / target).toInt().coerceIn(0, 100)
            val label = if (index == 0) "ಗಾಯತ್ರೀ" else shortLabel(entry.label)
            val glyph = label.firstOrNull()?.toString() ?: "ॐ"
            cell.removeAllViews()
            cell.gravity = Gravity.CENTER_HORIZONTAL

            val head = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            head.addView(TextView(requireContext()).apply {
                text = glyph
                gravity = Gravity.CENTER
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleLarge)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_primary))
                minimumWidth = (28 * density).toInt()
            })
            if (counts.daily >= target) {
                head.addView(ImageView(requireContext()).apply {
                    setImageResource(R.drawable.ic_check)
                    layoutParams = LinearLayout.LayoutParams(
                        (18 * density).toInt(),
                        (18 * density).toInt()
                    ).apply { marginStart = (4 * density).toInt() }
                    imageTintList = android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.md_theme_primary)
                    )
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                })
            }
            cell.addView(head)

            cell.addView(TextView(requireContext()).apply {
                text = "${countFormat.format(counts.daily)}/${countFormat.format(target)}"
                gravity = Gravity.CENTER
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurface))
            })

            cell.addView(LinearProgressIndicator(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = resources.getDimensionPixelSize(R.dimen.carbon_spacing_02)
                }
                max = 100
                progress = pct
                trackCornerRadius = (4 * density).toInt()
                trackThickness = (4 * density).toInt()
                setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.md_theme_primary))
                trackColor = ContextCompat.getColor(requireContext(), R.color.md_theme_surfaceContainerHighest)
            })

            cell.addView(TextView(requireContext()).apply {
                text = "Σ ${compactThreshold(counts.cumulative)}"
                gravity = Gravity.CENTER
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelSmall)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_onSurfaceVariant))
                setPadding(0, resources.getDimensionPixelSize(R.dimen.carbon_spacing_01), 0, 0)
            })

            cell.contentDescription =
                "$label ${countFormat.format(counts.daily)} of ${countFormat.format(target)}. " +
                    "Cumulative ${countFormat.format(counts.cumulative)}. Tap to open japa."
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
