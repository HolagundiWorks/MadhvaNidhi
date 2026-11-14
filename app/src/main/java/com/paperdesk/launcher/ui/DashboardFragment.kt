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
    private val knDays = listOf("ಭಾನು", "ಸೋಮ", "ಮಂಗಳ", "ಬುಧ", "ಗುರು", "ಶುಕ್ರ", "ಶನಿ")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.page_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        practice = PracticeDayStore(requireContext())
        view.findViewById<View>(R.id.dashboardMenu).setOnClickListener { (activity as? LauncherActivity)?.openNavigation() }
        view.findViewById<View>(R.id.dashboardProfile).setOnClickListener { (activity as? LauncherActivity)?.navigateTo("user") }
        view.findViewById<View>(R.id.dashboardSettings).setOnClickListener { (activity as? LauncherActivity)?.navigateTo("settings") }
        viewLifecycleOwner.lifecycleScope.launch {
            val app = requireContext().applicationContext
            val profile = withContext(Dispatchers.IO) { com.paperdesk.launcher.data.ProfileStore(app).active() }
            view.findViewById<TextView>(R.id.dashboardGreeting).text = profile.name.ifBlank { "MadhvaNidhi" }
            view.findViewById<TextView>(R.id.dashboardProfileMeta).text =
                listOf(profile.gotra, profile.nakshatra)
                    .filter { it.isNotBlank() }
                    .joinToString("  ·  ")
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
        bindPanchanga(view)
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

    /** Today's Panchanga card and Dina Visesha card, filled from the same day data as the Panchanga screen. */
    private fun bindPanchanga(view: View) {
        val ctx = requireContext().applicationContext
        val lang = OsLang.get(ctx)
        viewLifecycleOwner.lifecycleScope.launch {
            val day = withContext(Dispatchers.IO) {
                com.paperdesk.launcher.data.FestivalStore.load(ctx)
                PanchangaStore.forDate(ctx, PanchangaStore.dateKey())
            }
            val cal = PanchangaStore.parse(day.date)
            view.findViewById<TextView>(R.id.dashPanchangaDate).text =
                "${cal.get(java.util.Calendar.DAY_OF_MONTH)} ${OsLang.month(cal, lang)} ${cal.get(java.util.Calendar.YEAR)} · ${OsLang.weekday(cal, lang)}"
            val ekadashi = day.tithi.contains("EKADASHI", true)
            view.findViewById<TextView>(R.id.dashPanchangaTithi).apply {
                text = OsLang.script(day.tithi.uppercase(), lang)
                setTextColor(requireContext().getColor(if (ekadashi) R.color.md_ekadashi else R.color.md_text))
            }
            view.findViewById<TextView>(R.id.dashPanchangaPaksha).text =
                OsLang.script(if (day.paksha.contains("KRISHNA", true)) "ಕೃಷ್ಣಪಕ್ಷ" else "ಶುಕ್ಲಪಕ್ಷ", lang)
            view.findViewById<TextView>(R.id.dashPanchangaNakshatra).text = OsLang.nakshatra(day.nakshatra, lang)
            view.findViewById<TextView>(R.id.dashPanchangaKarana).text = OsLang.script(day.karana, lang)
            view.findViewById<TextView>(R.id.dashPanchangaYoga).text = OsLang.script(day.yoga, lang)
            val fest = com.paperdesk.launcher.data.FestivalStore.label(day.date, lang)
                .ifBlank { com.paperdesk.launcher.data.MadhwaDays.name(day.masa, day.paksha, day.tithi) }
            view.findViewById<TextView>(R.id.dashDayEvent).text =
                if (fest.isBlank()) "ವಿಶೇಷ ಕಾರ್ಯಕ್ರಮ ದಾಖಲಾಗಿಲ್ಲ" else OsLang.script(fest, lang)
        }
    }

    private fun bindWeek(view: View) {
        val week = view.findViewById<LinearLayout>(R.id.dashboardWeek)
        week.removeAllViews()
        val todayFlags = practice.sandhya()
        val todayCount = listOf(todayFlags.first, todayFlags.second, todayFlags.third).count { it }
        view.findViewById<TextView>(R.id.dashboardTodayCount).text = "$todayCount / 3"
        view.findViewById<LinearProgressIndicator>(R.id.dashboardTodayProgress).progress = todayCount
        // Morning, noon and evening light up when that session is done today.
        val sessions = listOf(
            R.id.dashSessionMorning to todayFlags.first,
            R.id.dashSessionNoon to todayFlags.second,
            R.id.dashSessionEvening to todayFlags.third
        )
        for ((id, done) in sessions) {
            view.findViewById<android.widget.ImageView>(id).imageTintList = android.content.res.ColorStateList.valueOf(
                requireContext().getColor(if (done) R.color.md_theme_primary else R.color.ds_text_disabled))
        }
        val density = resources.displayMetrics.density
        val gap = resources.getDimensionPixelSize(R.dimen.ds_space_1)
        val padH = resources.getDimensionPixelSize(R.dimen.ds_space_2)
        val padV = resources.getDimensionPixelSize(R.dimen.ds_space_1)
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
                setTextAppearance(R.style.TextAppearance_DS_TitleSmall)
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

    private var badgesExpanded = false

    private fun bindBadges(view: View, japaCumulative: Long? = null) {
        view.findViewById<TextView>(R.id.dashCurrentStreak).text = practice.currentStreak().toString()
        view.findViewById<TextView>(R.id.dashBestStreak).text = practice.bestStreak().toString()
        val grid = view.findViewById<LinearLayout>(R.id.dashBadgeGrid)
        val toggle = view.findViewById<TextView>(R.id.dashBadgeToggle)
        grid.removeAllViews()
        val (morningOnly, allThree) = practice.sandhyaPatternCounts()
        val japaDays = practice.japaCompleteDays()
        val cum = japaCumulative ?: japa?.totalCumulative() ?: 0L
        val pictoDark = 0xFF2B2B2B.toInt()

        // kind: 0 = silver sun (morning only), 1 = gold sun (all three), 2 = japa lotus.
        data class Medal(val kind: Int, val gold: Boolean, val milestone: Long, val reached: Boolean, val desc: String)
        val medals = mutableListOf<Medal>()
        DashboardBadges.DAY_MILESTONES.forEach { tier ->
            medals += Medal(0, false, tier.threshold, morningOnly >= tier.threshold,
                "Silver sun, ${tier.threshold} morning-only days")
        }
        DashboardBadges.DAY_MILESTONES.forEach { tier ->
            medals += Medal(1, true, tier.threshold, allThree >= tier.threshold,
                "Gold sun, ${tier.threshold} days with all three sessions")
        }
        DashboardBadges.DAY_MILESTONES.forEach { tier ->
            medals += Medal(2, cum >= 1_00_000L, tier.threshold, japaDays >= tier.threshold,
                "Japa hexagon, ${tier.threshold} japa days")
        }

        // Earned only, highest milestone first; same milestone: gold sun, silver sun, japa.
        val earned = medals.filter { it.reached }.sortedWith(
            compareByDescending<Medal> { it.milestone }.thenBy {
                when {
                    it.kind == 2 -> 2
                    it.gold -> 0
                    else -> 1
                }
            }
        )
        val perRow = 5
        val rows = earned.chunked(perRow)
        rows.forEachIndexed { rowIndex, rowMedals ->
            val row = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
            rowMedals.forEach { m ->
                val c = layoutInflater.inflate(R.layout.item_dash_badge, row, false)
                c.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { marginEnd = resources.getDimensionPixelSize(R.dimen.ds_space_half) }
                c.findViewById<ImageView>(R.id.badgeMedal).setImageResource(
                    when (m.kind) {
                        0 -> R.drawable.medal_sun_silver
                        1 -> R.drawable.medal_sun_gold
                        else -> if (m.gold) R.drawable.medal_hex_gold else R.drawable.medal_hex_silver
                    }
                )
                c.findViewById<ImageView>(R.id.badgePicto).apply {
                    setImageResource(if (m.kind == 2) R.drawable.ic_japa_eda else R.drawable.ic_badge_sun)
                    imageTintList = android.content.res.ColorStateList.valueOf(pictoDark)
                }
                c.findViewById<TextView>(R.id.badgeTitle).text = countFormat.format(m.milestone)
                c.contentDescription = m.desc
                row.addView(c)
            }
            repeat(perRow - rowMedals.size) {
                row.addView(View(requireContext()), LinearLayout.LayoutParams(0, 0, 1f))
            }
            row.visibility = if (rowIndex == 0 || badgesExpanded) View.VISIBLE else View.GONE
            grid.addView(row, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        }

        toggle.visibility = if (rows.size > 1) View.VISIBLE else View.GONE
        toggle.text = if (badgesExpanded) "Show less" else "Show all"
        toggle.setOnClickListener {
            badgesExpanded = !badgesExpanded
            for (i in 1 until grid.childCount) {
                grid.getChildAt(i).visibility = if (badgesExpanded) View.VISIBLE else View.GONE
            }
            toggle.text = if (badgesExpanded) "Show less" else "Show all"
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
        val foreground = ContextCompat.getColor(requireContext(),
            R.color.md_theme_onSurface)
        val secondary = ContextCompat.getColor(requireContext(),
            R.color.md_theme_onSurfaceVariant)
        val table = view.findViewById<LinearLayout>(R.id.dashboardJapaTable)
        table.removeAllViews()
        val rowPad = resources.getDimensionPixelSize(R.dimen.ds_space_3)
        val gapH = resources.getDimensionPixelSize(R.dimen.ds_space_3)
        val neutralTrack = requireContext().getColor(R.color.md_theme_surfaceContainerHighest)
        val primary = requireContext().getColor(R.color.md_theme_primary)
        slots.take(5).forEachIndexed { index, mantraId ->
            val entry = JapaCatalog.entries.firstOrNull { it.id == mantraId } ?: JapaCatalog.entries.first()
            val counts = store.counts(entry.id)
            val complete = store.procedurePhase(entry.id) == 3
            val target = counts.dailyTarget.coerceAtLeast(1)
            val pct = ((counts.daily * 100) / target).toInt().coerceIn(0, 100)
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, rowPad, 0, rowPad)
                isClickable = true
                isFocusable = true
                contentDescription = "${entry.label}. ${countFormat.format(counts.daily)} of " +
                    "${countFormat.format(target)}. Tap to open japa."
                setOnClickListener {
                    MdMotion.pressFlash(it)
                    store.setActiveMantraId(entry.id)
                    (activity as? LauncherActivity)?.navigateTo("japa")
                }
            }
            // Column 1: japa name
            row.addView(TextView(requireContext()).apply {
                text = entry.label
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTextAppearance(R.style.TextAppearance_DS_TitleSmall)
                setTextColor(foreground)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3f)
            })
            // Column 2: progress bar
            row.addView(LinearProgressIndicator(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 4f).apply {
                    marginStart = gapH
                    marginEnd = gapH
                }
                max = 100
                progress = pct
                trackCornerRadius = (4 * density).toInt()
                trackThickness = (4 * density).toInt()
                setIndicatorColor(if (complete) foreground else primary)
                trackColor = if (complete) androidx.core.graphics.ColorUtils.setAlphaComponent(foreground, 32)
                    else neutralTrack
            })
            // Column 3: target number
            row.addView(TextView(requireContext()).apply {
                text = countFormat.format(target)
                gravity = Gravity.END
                setTextAppearance(R.style.TextAppearance_DS_Subtitle)
                setTextColor(foreground)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f)
            })
            table.addView(row)
            if (index < slots.take(5).size - 1) {
                table.addView(View(requireContext()).apply {
                    setBackgroundColor(requireContext().getColor(R.color.md_theme_outlineVariant))
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (1 * density).toInt()))
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
