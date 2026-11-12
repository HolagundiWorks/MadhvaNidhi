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
            if (view.width <= 0) return
            val density = resources.displayMetrics.density
            val divider = resources.getDimensionPixelSize(R.dimen.carbon_spacing_05)
            val usable = (view.width - view.paddingLeft - view.paddingRight - divider).coerceAtLeast(0)
            val minContext = resources.getDimensionPixelSize(R.dimen.shell_context_min_width)
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
            val compact = view.height / density < 360
            val size = minOf(
                (if (compact) 12f else 14f) * resources.displayMetrics.scaledDensity,
                (if (compact) 12f else 18f) * density
            )
            listOf(
                R.id.shakaLabel, R.id.shakaYear, R.id.samvatsaraText, R.id.masaValue, R.id.pakshaValue,
                R.id.rashiValue, R.id.nakshatraValue, R.id.bhavaValue, R.id.karanaValue,
                R.id.selectedTithi, R.id.selectedWeekday
            ).forEach { id ->
                view.findViewById<TextView>(id).setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, size)
            }
        }
        view.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> fitContext() }
        view.post { fitContext() }
        val selected = view.findViewById<TextView>(R.id.selectedDateText)
        selected.maxLines = 1
        androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
            selected, 12, 22, 1, android.util.TypedValue.COMPLEX_UNIT_SP
        )
        view.findViewById<View>(R.id.selectedDateRow).setOnClickListener { showDetails() }
        view.findViewById<View>(R.id.selectedDateText).setOnClickListener { showDetails() }
        view.findViewById<View>(R.id.dayEvent).setOnClickListener { showDetails() }
        view.findViewById<View>(R.id.dayEventsCard).setOnClickListener { showDetails() }
        view.findViewById<View>(R.id.eventDetails).setOnClickListener { showDetails() }
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
        bindDate()
        loadPanchanga()
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
        bind(R.id.dateDialogRashi, "ಜೋಡಣೆ ಬಾಕಿ", pending = true)
        bind(R.id.dateDialogNakshatra, nakshatra.ifBlank { "ಜೋಡಣೆ ಬಾಕಿ" }, pending = nakshatra.isBlank())
        bind(R.id.dateDialogBhava, "ಜೋಡಣೆ ಬಾಕಿ", pending = true)
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
            val cycle = withContext(Dispatchers.IO) { com.paperdesk.launcher.data.PanchangaCalendar.cycle(requestedDate) }
            if (requestedDate != selectedDate) return@launch
            renderPanchanga(PanchangaStore.local(requestedDate).copy(source = "ಸ್ಥಳೀಯ ಗಣನೆ"), cycle)
            val day = withContext(Dispatchers.IO) { PanchangaStore.forDate(context, requestedDate) }
            if (requestedDate == selectedDate) renderPanchanga(day, cycle)
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
        v.findViewById<TextView>(R.id.shakaLabel).apply {
            DeskType.apply(this, DeskType.REGULAR)
            setTextColor(requireContext().getColor(R.color.carbon_text_secondary))
        }
        v.findViewById<TextView>(R.id.shakaYear).apply {
            text = year
            DeskType.apply(this, DeskType.SEMIBOLD)
        }
        v.findViewById<TextView>(R.id.samvatsaraText).apply {
            val line = "ಶ್ರೀ $samvatsara ನಾಮ ಸಂವತ್ಸರ"
            text = android.text.SpannableString(line).apply {
                val start = line.indexOf(samvatsara)
                if (start >= 0) {
                    setSpan(
                        android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                        start,
                        start + samvatsara.length,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    setSpan(
                        android.text.style.ForegroundColorSpan(requireContext().getColor(R.color.carbon_text)),
                        start,
                        start + samvatsara.length,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            DeskType.apply(this, DeskType.REGULAR)
            setTextColor(requireContext().getColor(R.color.carbon_text_secondary))
        }
        v.findViewById<TextView>(R.id.masaValue).apply {
            text = masa
            DeskType.apply(this, DeskType.SEMIBOLD)
        }
        v.findViewById<TextView>(R.id.pakshaValue).apply {
            text = phaseLabel
            DeskType.apply(this, DeskType.SEMIBOLD)
            setTextColor(requireContext().getColor(R.color.carbon_text_secondary))
        }
        // Keep combined header for instrumentation that still asserts on this id.
        v.findViewById<TextView>(R.id.panchangaHeader).text =
            "$dateLabel\nಶ್ರೀ ಶಾಲಿವಾಹನ ಗತಶಕೆ $year\nಶ್ರೀ $samvatsara ನಾಮ ಸಂವತ್ಸರ\n$masa · $phaseLabel"
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
        fact(R.id.rashiValue, "")
        fact(
            R.id.nakshatraValue,
            OsLang.script(
                day.nakshatra.replace(Regex("^(PURVA|UTTARA)(PHALGUNI|ASHADHA|BHADRAPADA)$"), "$1 $2"),
                lang
            )
        )
        fact(R.id.bhavaValue, "")
        fact(R.id.karanaValue, OsLang.script(day.karana, lang))
        FrozenPanchangaBinder.bind(v, cycle, selectedDate) { date ->
            if (!DeskLock.isLocked(requireContext())) {
                selectedDate = date
                showDetailsAfterLoad = true
                loadPanchanga()
            }
        }
        FestivalStore.load(requireContext())
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
