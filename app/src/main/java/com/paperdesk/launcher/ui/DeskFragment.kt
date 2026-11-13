package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.paperdesk.launcher.DeskLock
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.FestivalStore
import com.paperdesk.launcher.data.IndicScript
import com.paperdesk.launcher.data.OsLang
import com.paperdesk.launcher.data.PanchangaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeskFragment : Fragment() {
    private var panchangaJob: kotlinx.coroutines.Job? = null
    private var lastPanchangaDate = ""
    private var selectedDate = PanchangaStore.dateKey()
    private val lockListener: () -> Unit = { applyLockState() }
    private val langTick: () -> Unit = { bindDate(); bindLang(); loadPanchanga() }
    private var detailsDay: com.paperdesk.launcher.data.PanchangaDay? = null
    private var detailsEvent = ""
    private var detailsSummary = ""
    private var showDetailsAfterLoad = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedDate = savedInstanceState?.getString("selectedDate") ?: selectedDate
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("selectedDate", selectedDate)
        super.onSaveInstanceState(outState)
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.frozen_panchanga, container, false)

    override fun onViewCreated(view: View, state: Bundle?) {
        PagePalette.apply(view, "desk")
        MaterialLayouts.bind(view)
        fun fitContext() {
            if (view.width <= 0 || view.height <= 0) return
            val dm = resources.displayMetrics
            val px = dm.density
            // Mock density: clamp(0.55, min(vw/960, vh/540), 1.75) — CSS px ≈ dp
            val scale = minOf(view.width / px / 960f, view.height / px / 540f)
                .coerceIn(0.55f, 1.75f)
            val fitKey = "${view.width}x${view.height}:${"%.4f".format(scale)}"
            if (view.getTag(R.id.panchangaFacts) == fitKey) return
            fun spx(baseDp: Float, floorDp: Float = 0f): Int =
                (maxOf(floorDp, baseDp * scale) * px).toInt()

            val pageInset = spx(24f, 8f)
            val pagePadBlock = spx(16f, 4f)
            val paneGap = spx(24f, 8f)
            val ctxGap = spx(16f, 4f)
            val calendarGap = spx(12f, 4f)
            val control = spx(48f, 32f)
            val switcherH = spx(40f, 28f)
            val moon = spx(16f, 10f)
            val factLabel = spx(88f, 52f)
            val factPad = spx(8f, 2f)
            val factGap = spx(16f, 8f)
            val typeLabel = maxOf(10f, 12f * scale) * dm.scaledDensity
            val typeBody = maxOf(11f, 14f * scale) * dm.scaledDensity
            val typeTitle = maxOf(14f, 16f * scale) * dm.scaledDensity
            val eventTitle = maxOf(12f, 14f * scale) * dm.scaledDensity

            fun setPad(target: View, l: Int, t: Int, r: Int, b: Int) {
                if (target.paddingLeft != l || target.paddingTop != t ||
                    target.paddingRight != r || target.paddingBottom != b
                ) {
                    target.setPadding(l, t, r, b)
                }
            }
            fun setMarginTop(target: View, top: Int) {
                val lp = target.layoutParams as? android.view.ViewGroup.MarginLayoutParams ?: return
                if (lp.topMargin != top) {
                    lp.topMargin = top
                    target.layoutParams = lp
                }
            }
            fun setSize(target: View, w: Int, h: Int) {
                val lp = target.layoutParams ?: return
                if (lp.width != w || lp.height != h) {
                    lp.width = w
                    lp.height = h
                    target.layoutParams = lp
                }
            }

            setPad(view, pageInset, pagePadBlock, pageInset, pagePadBlock)
            setPad(view.findViewById(R.id.panchangaContext), 0, 0, paneGap, 0)
            setPad(view.findViewById(R.id.panchangaCalendar), paneGap, 0, 0, 0)

            val divider = resources.getDimensionPixelSize(R.dimen.carbon_border_width)
            val usable = (view.width - view.paddingLeft - view.paddingRight - divider).coerceAtLeast(0)
            val minContext = (208 * px * scale.coerceAtLeast(0.7f)).toInt().coerceAtMost(usable / 2)
            val contextWidth = when {
                usable <= 0 -> minContext
                else -> maxOf(minContext.coerceAtMost(usable / 2), (usable * .3f).toInt())
                    .coerceAtMost((usable * .5f).toInt())
                    .coerceAtMost(usable)
            }
            val guide = view.findViewById<androidx.constraintlayout.widget.Guideline>(R.id.contextGuide)
            if (guide.layoutParams.let { (it as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams).guideBegin } != contextWidth) {
                guide.setGuidelineBegin(contextWidth)
            }

            listOf(R.id.shakaYear, R.id.samvatsaraText, R.id.masaValue).forEach { id ->
                view.findViewById<TextView>(id).setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, typeTitle)
            }
            listOf(R.id.rashiValue, R.id.nakshatraValue, R.id.bhavaValue, R.id.karanaValue).forEach { id ->
                view.findViewById<TextView>(id).setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, typeBody)
            }
            view.findViewById<TextView>(R.id.todayEventsTitle)
                .setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, typeLabel)
            view.findViewById<TextView>(R.id.dayEvent)
                .setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, eventTitle)

            view.findViewById<View>(R.id.panchangaFacts).let { facts ->
                setMarginTop(facts, ctxGap)
                for (i in 0 until (facts as android.view.ViewGroup).childCount) {
                    val row = facts.getChildAt(i)
                    if (row is android.view.ViewGroup && row.childCount >= 2) {
                        setPad(row, 0, factPad, 0, factPad)
                        val label = row.getChildAt(0)
                        val lp = label.layoutParams
                        val needsWidth = lp.width != factLabel
                        val needsEnd = (lp as? android.view.ViewGroup.MarginLayoutParams)?.marginEnd != factGap
                        if (needsWidth || needsEnd) {
                            if (lp is android.view.ViewGroup.MarginLayoutParams) lp.marginEnd = factGap
                            lp.width = factLabel
                            label.layoutParams = lp
                        }
                    }
                }
            }
            setMarginTop(view.findViewById(R.id.contextRule), ctxGap)
            setMarginTop(view.findViewById(R.id.pakshaScroll), calendarGap)
            setPad(
                view.findViewById(R.id.pakshaScroll),
                spx(16f, 4f), spx(16f, 4f), spx(16f, 4f), spx(16f, 4f)
            )

            setSize(view.findViewById(R.id.chooseDate), control, control)
            setSize(view.findViewById(R.id.eventDetails), control, control)
            listOf(R.id.calendarPrevious, R.id.calendarNext).forEach { id ->
                val btn = view.findViewById<com.google.android.material.button.MaterialButton>(id)
                if (btn.minHeight != switcherH) btn.minHeight = switcherH
                setPad(btn, factGap, 0, factGap, 0)
                btn.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, typeBody)
                if (btn.iconSize != moon) btn.iconSize = moon
            }
            listOf(R.id.phaseMoonStart, R.id.phaseMoonMid, R.id.phaseMoonEnd, R.id.masaWeather).forEach { id ->
                setSize(view.findViewById(id), moon, moon)
            }
            view.setTag(R.id.panchangaScreen, scale)
            view.setTag(R.id.panchangaFacts, fitKey)
        }
        view.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
                fitContext()
            }
        }
        view.post { fitContext() }
        view.findViewById<View>(R.id.dayEvent).setOnClickListener { showDetails() }
        view.findViewById<View>(R.id.dayEventsCard).setOnClickListener { showDetails() }
        view.findViewById<View>(R.id.eventDetails).setOnClickListener { showDetails() }
        view.findViewById<View>(R.id.chooseDate).setOnClickListener { showDatePicker() }
        parentFragmentManager.setFragmentResultListener("panchangaDate", viewLifecycleOwner) { _, result ->
            selectedDate = result.getString("date") ?: selectedDate
            bindDate()
            loadPanchanga()
        }
        view.findViewById<View>(R.id.todayDate).setOnClickListener {
            selectedDate = PanchangaStore.dateKey()
            bindDate()
            loadPanchanga()
        }
        bindDate()
        bindLang()
        applyLockState()
    }
    override fun onResume() {
        super.onResume()
        DeskLock.addListener(lockListener)
        OsLang.addListener(langTick)
        bindLang()
        // bindDate() reloads only when the selected date changed — avoid double calendar rebuild.
        bindDate()
        if (detailsDay == null || lastPanchangaDate.isEmpty()) loadPanchanga()
    }
    override fun onPause() {
        DeskLock.removeListener(lockListener)
        OsLang.removeListener(langTick)
        super.onPause()
    }
    fun applyLockState() = Unit
    fun showDatePicker() {
        if (DeskLock.isLocked(requireContext()) || parentFragmentManager.findFragmentByTag("panchanga-picker") != null) return
        val utc = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val picker = com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
            .setTitleText("ದಿನಾಂಕ ಆಯ್ಕೆಮಾಡಿ")
            .setSelection(utc.parse(selectedDate)?.time ?: com.google.android.material.datepicker.MaterialDatePicker.todayInUtcMilliseconds())
            .setInputMode(com.google.android.material.datepicker.MaterialDatePicker.INPUT_MODE_CALENDAR)
            .build()
        picker.addOnPositiveButtonClickListener {
            selectedDate = utc.format(java.util.Date(it))
            loadPanchanga()
        }
        picker.show(parentFragmentManager, "panchanga-picker")
    }

    private fun showDetails() {
        val day = detailsDay ?: run {
            loadPanchanga()
            return
        }
        if (!isAdded) return
        val lang = OsLang.get(requireContext())
        val cal = PanchangaStore.parse(day.date)
        val content = layoutInflater.inflate(R.layout.dialog_panchanga_date, null, false)
        val ekadashi = day.tithi.contains("EKADASHI", true)
        val phase = if (day.paksha.contains("KRISHNA", true)) "ಕೃಷ್ಣಪಕ್ಷ" else "ಶುಕ್ಲಪಕ್ಷ"
        val tithi = OsLang.script(
            day.tithi.uppercase()
                .replace("DWITEEYA", "DWITIYA")
                .replace("TRUTEEYA", "TRITIYA")
                .replace("THRITEEYA", "TRITIYA"),
            lang
        )
        val masa = OsLang.script(day.masa.uppercase().replace("KARTEEKA", "KARTIKA"), lang) + "ಮಾಸ"
        val nakshatra = OsLang.script(
            day.nakshatra.replace(Regex("^(PURVA|UTTARA)(PHALGUNI|ASHADHA|BHADRAPADA)$"), "$1 $2"),
            lang
        )
        fun bind(id: Int, value: String, pending: Boolean = false, accent: Boolean = false) {
            content.findViewById<TextView>(id).apply {
                text = value
                DeskType.apply(this, if (pending) DeskType.REGULAR else DeskType.SEMIBOLD)
                setTextColor(
                    requireContext().getColor(
                        when {
                            accent -> R.color.carbon_ekadashi
                            pending -> R.color.carbon_text_helper
                            else -> R.color.carbon_text
                        }
                    )
                )
            }
        }
        content.findViewById<TextView>(R.id.dateDialogTitle).apply {
            text = view?.findViewById<TextView>(R.id.selectedDateText)?.text
            DeskType.apply(this, DeskType.REGULAR)
        }
        bind(R.id.dateDialogWeekday, OsLang.weekday(cal, lang))
        bind(R.id.dateDialogPhase, phase)
        bind(R.id.dateDialogTithi, tithi, accent = ekadashi)
        bind(R.id.dateDialogMasa, masa)
        val rashi = OsLang.script(PanchangaStore.chandraRashi(day.date), lang)
        val pada = OsLang.digits(PanchangaStore.nakshatraPada(day.date), lang)
        bind(R.id.dateDialogRashi, rashi)
        bind(R.id.dateDialogNakshatra, nakshatra.ifBlank { "ಜೋಡಣೆ ಬಾಕಿ" }, pending = nakshatra.isBlank())
        bind(R.id.dateDialogBhava, "ಪಾದ $pada")
        bind(R.id.dateDialogKarana, OsLang.script(day.karana, lang).ifBlank { "ಜೋಡಣೆ ಬಾಕಿ" }, pending = day.karana.isBlank())
        bind(R.id.dateDialogYoga, OsLang.script(day.yoga, lang).ifBlank { "ಜೋಡಣೆ ಬಾಕಿ" }, pending = day.yoga.isBlank())
        bind(R.id.dateDialogEvent, detailsEvent.ifBlank { "ವಿಶೇಷ ಕಾರ್ಯಕ್ರಮ ದಾಖಲಾಗಿಲ್ಲ" }, accent = ekadashi)
        content.findViewById<TextView>(R.id.dateDialogSummary).apply {
            if (detailsSummary.isBlank()) visibility = View.GONE
            else {
                visibility = View.VISIBLE
                text = detailsSummary
                DeskType.apply(this, DeskType.REGULAR)
            }
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setView(content)
            .setPositiveButton("ಸರಿ", null)
            .show()
    }

    fun bindLang() {
        view?.findViewById<TextView>(R.id.todayEventsTitle)?.text = when (OsLang.get(requireContext())) {
            IndicScript.Lang.KN -> "ದಿನದ ವಿಶೇಷ"
            IndicScript.Lang.SA -> "अद्यतन विशेषाः"
            IndicScript.Lang.EN -> "Day’s events"
        }
        view?.findViewById<TextView>(R.id.selectedDateLabel)?.text = when (OsLang.get(requireContext())) {
            IndicScript.Lang.KN -> "ಆಯ್ದ ದಿನಾಂಕ"
            IndicScript.Lang.SA -> "चयनित तिथिः"
            IndicScript.Lang.EN -> "Selected date"
        }
    }
    private fun bindDate() {
        if (view == null) return
        if (selectedDate != lastPanchangaDate) loadPanchanga()
    }

    private fun loadPanchanga() {
        val context = requireContext().applicationContext
        val requestedDate = selectedDate
        panchangaJob?.cancel()
        panchangaJob = viewLifecycleOwner.lifecycleScope.launch {
            val cycle = withContext(Dispatchers.IO) {
                FestivalStore.load(context)
                com.paperdesk.launcher.data.PanchangaCalendar.cycle(requestedDate)
            }
            if (requestedDate != selectedDate) return@launch
            // Paint local estimate first so the UI is responsive, then refresh if network/local merge differs.
            val local = PanchangaStore.local(requestedDate).copy(source = "ಸ್ಥಳೀಯ ಗಣನೆ")
            renderPanchanga(local, cycle)
            val day = withContext(Dispatchers.IO) { PanchangaStore.forDate(context, requestedDate) }
            if (requestedDate != selectedDate) return@launch
            if (day.tithi != local.tithi || day.nakshatra != local.nakshatra ||
                day.paksha != local.paksha || day.masa != local.masa || day.source != local.source
            ) {
                renderPanchanga(day, cycle)
            } else {
                detailsDay = day
                lastPanchangaDate = day.date
            }
        }
    }

    private fun renderPanchanga(
        day: com.paperdesk.launcher.data.PanchangaDay,
        cycle: com.paperdesk.launcher.data.PanchangaCalendar.Cycle
    ) {
        val v = view ?: return
        lastPanchangaDate = day.date
        detailsDay = day
        val lang = OsLang.get(requireContext())
        val cal = PanchangaStore.parse(day.date)
        val dateLabel = "${cal.get(java.util.Calendar.DAY_OF_MONTH)} ${OsLang.month(cal, lang)} ${cal.get(java.util.Calendar.YEAR)}"
        v.findViewById<TextView>(R.id.selectedDateText).apply {
            text = dateLabel
            DeskType.apply(this, DeskType.SEMIBOLD)
        }
        val phase = if (day.paksha.contains("KRISHNA", true)) 1 else 0
        val phaseLabel = if (phase == 0) "ಶುಕ್ಲಪಕ್ಷ" else "ಕೃಷ್ಣಪಕ್ಷ"
        val tithiLabel = OsLang.script(
            day.tithi.uppercase()
                .replace("DWITEEYA", "DWITIYA")
                .replace("TRUTEEYA", "TRITIYA")
                .replace("THRITEEYA", "TRITIYA"),
            lang
        )
        val weekday = OsLang.weekday(cal, lang)
        val ekadashi = day.tithi.contains("EKADASHI", true)
        v.findViewById<TextView>(R.id.selectedTithi).apply {
            text = tithiLabel
            DeskType.apply(this, DeskType.SEMIBOLD)
            setTextColor(requireContext().getColor(if (ekadashi) R.color.carbon_ekadashi else R.color.carbon_text))
        }
        v.findViewById<TextView>(R.id.selectedWeekday).apply {
            text = weekday
            DeskType.apply(this, DeskType.SEMIBOLD)
            setTextColor(requireContext().getColor(R.color.carbon_text))
        }
        v.findViewById<TextView>(R.id.selectedMeta).text = "$tithiLabel · $weekday"
        val year = PanchangaStore.shakaYear(day).toString()
        val samvatsara = OsLang.script(PanchangaStore.samvatsaraFor(day), lang)
        val masa = OsLang.script(day.masa.uppercase().replace("KARTEEKA", "KARTIKA"), lang) + "ಮಾಸ"
        v.findViewById<TextView>(R.id.shakaYear).apply {
            text = "ಶ್ರೀ ಶಾಲಿವಾಹನ ಗತಶಕೆ-$year"
            DeskType.apply(this, DeskType.SEMIBOLD)
            setTextColor(requireContext().getColor(R.color.carbon_text))
        }
        v.findViewById<TextView>(R.id.samvatsaraText).apply {
            text = "ಶ್ರೀ $samvatsara ನಾಮ ಸಂವತ್ಸರ"
            DeskType.apply(this, DeskType.SEMIBOLD)
            setTextColor(requireContext().getColor(R.color.carbon_text_secondary))
        }
        v.findViewById<TextView>(R.id.masaValue).apply {
            text = masa
            DeskType.apply(this, DeskType.SEMIBOLD)
        }
        v.findViewById<TextView>(R.id.pakshaValue).text = phaseLabel
        // Keep combined header for instrumentation that still asserts on this id.
        v.findViewById<TextView>(R.id.panchangaHeader).text =
            "$dateLabel\nಶ್ರೀ ಶಾಲಿವಾಹನ ಗತಶಕೆ-$year\nಶ್ರೀ $samvatsara ನಾಮ ಸಂವತ್ಸರ\n$masa · $phaseLabel"
        fun fact(id: Int, value: String) {
            val pending = value.isBlank()
            val actual = value.ifBlank { "ಜೋಡಣೆ ಬಾಕಿ" }
            v.findViewById<TextView>(id).apply {
                text = actual
                DeskType.apply(this, if (pending) DeskType.REGULAR else DeskType.SEMIBOLD)
                setTextColor(
                    requireContext().getColor(
                        if (pending) R.color.carbon_text_helper else R.color.carbon_text
                    )
                )
            }
        }
        fact(R.id.rashiValue, OsLang.script(PanchangaStore.chandraRashi(day.date), lang))
        fact(
            R.id.nakshatraValue,
            OsLang.script(
                day.nakshatra.replace(Regex("^(PURVA|UTTARA)(PHALGUNI|ASHADHA|BHADRAPADA)$"), "$1 $2"),
                lang
            )
        )
        fact(R.id.bhavaValue, "ಪಾದ ${OsLang.digits(PanchangaStore.nakshatraPada(day.date), lang)}")
        fact(R.id.karanaValue, OsLang.script(day.karana, lang))
        FrozenPanchangaBinder.bind(v, cycle, selectedDate) { date ->
            if (!DeskLock.isLocked(requireContext())) {
                selectedDate = date
                showDetailsAfterLoad = true
                loadPanchanga()
            }
        }
        val fest = FestivalStore.label(day.date, lang)
            .ifBlank { com.paperdesk.launcher.data.MadhwaDays.name(day.masa, day.paksha, day.tithi) }
        detailsEvent = if (fest.isBlank()) "ವಿಶೇಷ ಕಾರ್ಯಕ್ರಮ ದಾಖಲಾಗಿಲ್ಲ" else OsLang.script(fest, lang)
        detailsSummary = ""
        v.findViewById<TextView>(R.id.dayEvent).apply {
            text = detailsEvent
            DeskType.apply(this, DeskType.SEMIBOLD)
            setTextColor(
                requireContext().getColor(if (ekadashi) R.color.carbon_ekadashi else R.color.carbon_text)
            )
        }
        v.findViewById<TextView>(R.id.dayEventSummary).visibility = View.GONE
        if (showDetailsAfterLoad) {
            showDetailsAfterLoad = false
            showDetails()
        }
    }
}
