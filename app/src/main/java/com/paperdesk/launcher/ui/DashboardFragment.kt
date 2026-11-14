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
    private val knDays = listOf("ಭಾನು", "ಸೋಮ", "ಮಂಗಳ", "ಬುಧ", "ಗುರು", "ಶುಕ್ರ", "ಶನಿ")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.page_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        practice = PracticeDayStore(requireContext())
        view.findViewById<View>(R.id.dashboardMenu).setOnClickListener { (activity as? LauncherActivity)?.openNavigation() }
        view.findViewById<View>(R.id.dashboardProfile).setOnClickListener { (activity as? LauncherActivity)?.navigateTo("user") }
        view.findViewById<View>(R.id.dashboardSettings).setOnClickListener { (activity as? LauncherActivity)?.navigateTo("settings") }
        view.findViewById<View>(R.id.dashboardResumeJapa).setOnClickListener { (activity as? LauncherActivity)?.navigateTo("japa") }
        viewLifecycleOwner.lifecycleScope.launch {
            val app = requireContext().applicationContext
            val profile = withContext(Dispatchers.IO) { com.paperdesk.launcher.data.ProfileStore(app).active() }
            view.findViewById<TextView>(R.id.dashboardGreeting).text = profile.name.ifBlank { "MadhvaNidhi" }
        }

        MdMotion.fadeIn(view)
        viewLifecycleOwner.lifecycleScope.launch {
            val app = requireContext().applicationContext
            val store = withContext(Dispatchers.IO) {
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

    private fun shortcutIcon(destId: String): Int = FrozenShell.iconFor(
        when (destId) {
            "panchanga", "schedule" -> "desk"
            else -> destId
        }
    )

    private fun bindWeek(view: View) {
        val week = view.findViewById<LinearLayout>(R.id.dashboardWeek)
        week.removeAllViews()
        val todayFlags = practice.sandhya()
        val todayCount = listOf(todayFlags.first, todayFlags.second, todayFlags.third).count { it }
        view.findViewById<TextView>(R.id.dashboardTodayCount).text = "$todayCount / 3"
        view.findViewById<LinearProgressIndicator>(R.id.dashboardTodayProgress).progress = todayCount
        val density = resources.displayMetrics.density
        val gap = resources.getDimensionPixelSize(R.dimen.md_spacing_02)
        val padH = resources.getDimensionPixelSize(R.dimen.md_spacing_03)
        val padV = resources.getDimensionPixelSize(R.dimen.md_spacing_02)
        val todayKey = PanchangaStore.dateKey()
        val completeDays = practice.weeklySandhyaCompleteCount()
        val weeklyBadge = DashboardBadges.evaluate(DashboardBadges.WEEKLY_TIERS, completeDays.toLong())

        view.findViewById<TextView>(R.id.dashWeekScore).text = "$completeDays/7"
        view.findViewById<LinearProgressIndicator>(R.id.dashWeekProgress).progress = completeDays
        view.findViewById<View>(R.id.dashWeek).contentDescription =
            "Sandhya week $completeDays of 7" +
                (weeklyBadge?.let { ". ${it.tier.hint}" } ?: "")

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
            val status = DayStatus.sandhyaDay(done, future, pendingToday)
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
                    DayStatus.FUTURE -> "$dayName · upcoming"
                    DayStatus.COMPLETE -> "$dayName · complete · $done of 3"
                    DayStatus.SKIPPED -> "$dayName · incomplete · $done of 3"
                    DayStatus.PENDING -> "$dayName · $done of 3 · tap to view"
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

            if (!future) {
                val dateForCell = key
                row.setOnClickListener {
                    MdMotion.pressFlash(it)
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

        applyAwardIcon(view.findViewById(R.id.dashStreakIcon), earned?.tier ?: DashboardBadges.STREAK_TIERS.first())
        view.findViewById<View>(R.id.dashStreakIcon).alpha = if (earned == null) .38f else 1f
        view.findViewById<TextView>(R.id.dashStreakCurrent).text = countFormat.format(current)
        view.findViewById<TextView>(R.id.dashStreakBest).text = "Best ${countFormat.format(best)}"
        view.findViewById<LinearProgressIndicator>(R.id.dashStreakProgress).progress = progress

        val nextLabel = view.findViewById<TextView>(R.id.dashStreakNext)
        nextLabel.text = if (next != null) {
            "+${next.threshold - current}"
        } else {
            "Top tier"
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

        // At most peak + next; sit beside streak in one quiet row.
        val inflater = layoutInflater
        val peak = earned.maxByOrNull { it.tier.threshold }
        if (peak != null) {
            val chip = inflater.inflate(R.layout.item_dash_badge, tray, false)
            applyAwardIcon(chip.findViewById(R.id.badgeIcon), peak.tier)
            chip.findViewById<TextView>(R.id.badgeTitle).text = compactBadgeValue(peak)
            chip.contentDescription = "${DashboardBadges.finishFor(peak.tier).name.lowercase()} award. ${peak.tier.hint}"
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
                applyAwardIcon(chip.findViewById(R.id.badgeIcon), next)
                chip.findViewById<TextView>(R.id.badgeTitle).text = compactThreshold(next.threshold)
                chip.contentDescription = "Next ${DashboardBadges.finishFor(next).name.lowercase()} award. ${next.hint}"
                tray.addView(chip)
            }
        } else if (earned.size > 1) {
            val second = earned[1]
            val chip = inflater.inflate(R.layout.item_dash_badge, tray, false)
            applyAwardIcon(chip.findViewById(R.id.badgeIcon), second.tier)
            chip.findViewById<TextView>(R.id.badgeTitle).text = compactBadgeValue(second)
            chip.contentDescription = "${DashboardBadges.finishFor(second.tier).name.lowercase()} award. ${second.tier.hint}"
            tray.addView(chip)
        }
    }

    private fun applyAwardIcon(icon: ImageView, tier: DashboardBadges.Tier) {
        icon.setImageResource(R.drawable.ic_award_star)
        val color = when (DashboardBadges.finishFor(tier)) {
            DashboardBadges.Finish.BROWN -> R.color.award_brown
            DashboardBadges.Finish.SILVER -> R.color.award_silver
            DashboardBadges.Finish.GOLD -> R.color.award_gold
        }
        icon.imageTintList = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), color)
        )
    }

    private fun compactBadgeValue(item: DashboardBadges.Earned): String = when (item.tier.kind) {
        DashboardBadges.Kind.STREAK -> countFormat.format(item.value)
        DashboardBadges.Kind.WEEKLY -> "${item.value}/7"
        DashboardBadges.Kind.JAPA -> compactThreshold(item.value)
    }

    private fun compactThreshold(value: Long): String = countFormat.format(value)

    private fun bindJapaRow(view: View) {
        val store = japa ?: return
        val density = resources.displayMetrics.density
        val slots = SettingsStore(requireContext()).dashboardJapaSlots()
        val summary = store.counts(slots.firstOrNull() ?: "gayatri")
        val complete = store.procedurePhase(summary.mantraId) == 3
        val status = when {
            complete -> "ಪೂರ್ಣ · Completed"
            practice.japaSkipped() -> "ಬಿಟ್ಟಿದೆ · Skipped"
            else -> "ಬಾಕಿ · Not marked complete"
        }
        val foreground = ContextCompat.getColor(requireContext(),
            R.color.md_theme_onSurface)
        val secondary = ContextCompat.getColor(requireContext(),
            R.color.md_theme_onSurfaceVariant)
        val overall = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.dashboardOverallCard)
        styleJapaCard(overall, complete)
        view.findViewById<TextView>(R.id.dashboardOverallTitle).apply {
            text = "Your Japa practice"
            setTextColor(foreground)
        }
        view.findViewById<TextView>(R.id.dashboardOverallStatus).apply { text = status; setTextColor(secondary) }
        view.findViewById<TextView>(R.id.dashboardOverallJapa).setTextColor(foreground)
        view.findViewById<ImageView>(R.id.dashboardOverallIcon).imageTintList =
            android.content.res.ColorStateList.valueOf(if (complete) foreground else requireContext().getColor(R.color.md_theme_primary))
        view.findViewById<MaterialButton>(R.id.dashboardResumeJapa).apply {
            text = if (complete) "Open Japa →" else "Continue Japa →"
            setTextColor(if (complete) foreground else requireContext().getColor(R.color.md_theme_onSecondaryContainer))
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (complete) androidx.core.graphics.ColorUtils.setAlphaComponent(foreground, 24)
                else requireContext().getColor(R.color.md_theme_secondaryContainer))
        }

        view.findViewById<TextView>(R.id.dashboardOverallJapa).text =
            countFormat.format(summary.daily) + " / " + countFormat.format(summary.dailyTarget) + " today"
        view.findViewById<com.google.android.material.progressindicator.CircularProgressIndicator>(R.id.dashboardOverallProgress).progress =
            ((summary.daily.toDouble() / summary.dailyTarget.coerceAtLeast(1)) * 100).toInt().coerceIn(0, 100)
        view.findViewById<com.google.android.material.progressindicator.CircularProgressIndicator>(R.id.dashboardOverallProgress).apply {
            setIndicatorColor(if (complete) foreground else requireContext().getColor(R.color.md_theme_primary))
            trackColor = if (complete) androidx.core.graphics.ColorUtils.setAlphaComponent(foreground, 32)
                else requireContext().getColor(R.color.md_theme_surfaceContainerHighest)
        }
        slotIds.forEachIndexed { index, viewId ->
            val cell = view.findViewById<LinearLayout>(viewId)
            val mantraId = slots.getOrElse(index) { if (index == 0) "gayatri" else "narayana" }
            val entry = JapaCatalog.entries.firstOrNull { it.id == mantraId }
                ?: JapaCatalog.entries.first()
            val counts = store.counts(entry.id)
            val complete = store.procedurePhase(entry.id) == 3
            val status = if (complete) "ಪೂರ್ಣ · Completed" else "ಬಾಕಿ · Not marked complete"
            val target = counts.dailyTarget.coerceAtLeast(1)
            val pct = ((counts.daily * 100) / target).toInt().coerceIn(0, 100)
            val label = entry.label
            styleJapaCard(cell.parent as com.google.android.material.card.MaterialCardView, complete)
            cell.removeAllViews()
            cell.gravity = Gravity.CENTER_HORIZONTAL

            val head = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            head.addView(TextView(requireContext()).apply {
                text = label
                gravity = Gravity.CENTER
                minLines = 2
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleSmall)
                setTextColor(foreground)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            cell.addView(head)

            cell.addView(TextView(requireContext()).apply {
                text = "${countFormat.format(counts.daily)}/${countFormat.format(target)}"
                gravity = Gravity.CENTER
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
                setTextColor(foreground)
                setPadding(0, resources.getDimensionPixelSize(R.dimen.md_spacing_01), 0, 0)
            })

            cell.addView(LinearProgressIndicator(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = resources.getDimensionPixelSize(R.dimen.md_spacing_02)
                }
                max = 100
                progress = pct
                trackCornerRadius = (4 * density).toInt()
                trackThickness = (4 * density).toInt()
                setIndicatorColor(if (complete) foreground else requireContext().getColor(R.color.md_theme_primary))
                trackColor = if (complete) androidx.core.graphics.ColorUtils.setAlphaComponent(foreground, 32)
                    else requireContext().getColor(R.color.md_theme_surfaceContainerHighest)
            })

            cell.addView(TextView(requireContext()).apply {
                text = "Σ ${countFormat.format(counts.cumulative)}"
                gravity = Gravity.CENTER
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelSmall)
                setTextColor(secondary)
                setPadding(0, resources.getDimensionPixelSize(R.dimen.md_spacing_01), 0, 0)
            })

            cell.addView(TextView(requireContext()).apply {
                text = status
                gravity = Gravity.CENTER
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelMedium)
                setTextColor(secondary)
                setPadding(0, resources.getDimensionPixelSize(R.dimen.md_spacing_02), 0, 0)
            })
            androidx.core.view.ViewCompat.setStateDescription(cell, status)
            cell.contentDescription =
                "$label. $status. ${countFormat.format(counts.daily)} of ${countFormat.format(target)}. " +
                    "Cumulative ${countFormat.format(counts.cumulative)}. Tap to open japa."
            cell.setOnClickListener {
                MdMotion.pressFlash(it)
                store.setActiveMantraId(entry.id)
                (activity as? LauncherActivity)?.navigateTo("japa")
            }
        }
    }

    private fun styleJapaCard(card: com.google.android.material.card.MaterialCardView, complete: Boolean) {
        card.setCardBackgroundColor(requireContext().getColor(
            R.color.md_theme_surfaceContainer))
        card.strokeWidth = if (complete) resources.getDimensionPixelSize(R.dimen.md_border_width) else 0
        card.strokeColor = requireContext().getColor(R.color.md_theme_primary)
    }

    private fun showProgressDialog(date: String = PanchangaStore.dateKey()) {
        val content = layoutInflater.inflate(R.layout.dialog_dashboard_progress, null, false)
        val (pratah, madhya, sayam) = practice.sandhya(date)
        listOf(
            R.id.dialogSandhyaPratah to pratah,
            R.id.dialogSandhyaMadhyahna to madhya,
            R.id.dialogSandhyaSayam to sayam,
            R.id.dialogStotra1 to practice.stotra("hari_vayu", date),
            R.id.dialogStotra2 to practice.stotra("rama_dwadasha", date)
        ).forEach { (id, done) ->
            content.findViewById<MaterialCheckBox>(id).apply {
                isChecked = done
                isEnabled = false
            }
        }
        val completed = practice.japaComplete(date)
        content.findViewById<TextView>(R.id.dialogJapaCount).text =
            if (completed) "Marked complete" else "Not marked complete"
        val lines = if (date == PanchangaStore.dateKey()) {
            SettingsStore(requireContext()).dashboardJapaSlots().map { id ->
                val label = JapaCatalog.entries.firstOrNull { it.id == id }?.label ?: id
                val c = japa?.counts(id)
                if (c == null) label else
                    "$label ${countFormat.format(c.daily)}/${countFormat.format(c.dailyTarget)} · Σ ${countFormat.format(c.cumulative)}"
            }
        } else emptyList()
        content.findViewById<TextView>(R.id.dialogJapaMeta).text = lines.joinToString("\n")
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(date)
            .setView(content)
            .setPositiveButton("ಸರಿ", null)
            .create()
        fun open(id: Int, page: String) {
            content.findViewById<MaterialButton>(id).setOnClickListener {
                dialog.dismiss()
                (activity as? LauncherActivity)?.navigateTo(page)
            }
        }
        open(R.id.dialogOpenSandhya, "sandhya")
        open(R.id.dialogOpenJapa, "japa")
        open(R.id.dialogOpenGrantha, "grantha")
        dialog.show()
    }
}
