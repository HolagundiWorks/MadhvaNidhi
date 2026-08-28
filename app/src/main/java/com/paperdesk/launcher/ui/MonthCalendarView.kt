package com.paperdesk.launcher.ui

import android.app.Dialog
import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.FestivalStore
import com.paperdesk.launcher.data.IndicScript
import com.paperdesk.launcher.data.MadhwaDays
import com.paperdesk.launcher.data.OsLang
import com.paperdesk.launcher.data.PanchangaStore
import java.util.Calendar

/** A lunar calendar: one 15-tithi row for each paksha, anchored around the selected date. */
class MonthCalendarView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {

    private val accent = ContextCompat.getColor(context, R.color.accent)
    private val white = ContextCompat.getColor(context, R.color.white)
    private val muted = ContextCompat.getColor(context, R.color.white_55)
    private val previousMonth = ContextCompat.getColor(context, R.color.calendar_month_previous)
    private val currentMonth = ContextCompat.getColor(context, R.color.calendar_month_current)
    private val nextMonth = ContextCompat.getColor(context, R.color.calendar_month_next)
    private val titleTv = label(13.6f, white, Gravity.CENTER)
    private val monthLegend = LinearLayout(context)
    private val tithiRow = LinearLayout(context)
    private val shuklaLabel = label(10.4f, accent, Gravity.START)
    private val krishnaLabel = label(10.4f, accent, Gravity.START)
    private val shuklaRow = LinearLayout(context)
    private val krishnaRow = LinearLayout(context)

    var year: Int
    var month: Int
    var selectedDay: Int
        private set
    var onDaySelected: ((year: Int, month: Int, day: Int) -> Unit)? = null
    var onDayOpened: ((year: Int, month: Int, day: Int) -> Unit)? = null
    var locked: Boolean = false
        set(value) { field = value; bind() }

    init {
        val now = Calendar.getInstance()
        year = now.get(Calendar.YEAR)
        month = now.get(Calendar.MONTH)
        selectedDay = now.get(Calendar.DAY_OF_MONTH)
        orientation = VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(8), dp(6), dp(8), dp(6))
        FestivalStore.load(context)
        titleTv.setPadding(0, 0, 0, dp(3))
        DeskType.apply(titleTv, DeskType.SEMIBOLD)
        addView(titleTv, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        configureRow(monthLegend)
        monthLegend.gravity = Gravity.CENTER
        addView(monthLegend, LayoutParams(LayoutParams.MATCH_PARENT, dp(21)))
        configureRow(tithiRow)
        addView(tithiRow, LayoutParams(LayoutParams.MATCH_PARENT, dp(25)))
        addView(shuklaLabel, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        configureRow(shuklaRow)
        addView(shuklaRow, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        krishnaLabel.setPadding(0, dp(3), 0, 0)
        addView(krishnaLabel, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        configureRow(krishnaRow)
        addView(krishnaRow, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        bind()
    }

    fun show(y: Int, m: Int, day: Int) { year = y; month = m; selectedDay = day; bind() }
    fun refresh() = bind()

    private fun configureRow(row: LinearLayout) {
        row.orientation = HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
    }

    private fun bind() {
        val lang = OsLang.get(context)
        val selected = Calendar.getInstance().apply { set(year, month, selectedDay) }
        val selectedKey = PanchangaStore.dateKey(selected)
        val dates = lunarCycle(selectedKey)
        val masa = PanchangaStore.local(selectedKey).masa
        titleTv.text = OsLang.script("$masa · TITHI", lang)
        bindMonthLegend(lang)
        shuklaLabel.text = "${OsLang.ui("shukla", lang)}   ${OsLang.script("PRATIPADA → PURNIMA", lang)}"
        krishnaLabel.text = "${OsLang.ui("krishna", lang)}   ${OsLang.script("PRATIPADA → AMAVASYA", lang)}"
        bindTithiHeader(lang)
        bindRow(shuklaRow, dates.first, lang, selectedKey)
        bindRow(krishnaRow, dates.second, lang, selectedKey)
    }

    private fun bindMonthLegend(lang: IndicScript.Lang) {
        monthLegend.removeAllViews()
        val anchor = Calendar.getInstance().apply { set(year, month, 1) }
        listOf(-1, 0, 1).forEach { offset ->
            val cal = (anchor.clone() as Calendar).apply { add(Calendar.MONTH, offset) }
            val relation = when (offset) {
                -1 -> when (lang) {
                    IndicScript.Lang.KN -> "ಹಿಂದಿನ ತಿಂಗಳು"
                    IndicScript.Lang.SA -> "पूर्वमासः"
                    IndicScript.Lang.EN -> "PREVIOUS"
                }
                0 -> when (lang) {
                    IndicScript.Lang.KN -> "ಈ ತಿಂಗಳು"
                    IndicScript.Lang.SA -> "अयं मासः"
                    IndicScript.Lang.EN -> "THIS MONTH"
                }
                else -> when (lang) {
                    IndicScript.Lang.KN -> "ಮುಂದಿನ ತಿಂಗಳು"
                    IndicScript.Lang.SA -> "अग्रिममासः"
                    IndicScript.Lang.EN -> "NEXT"
                }
            }
            val colour = monthColour(offset)
            val monthName = OsLang.month(cal, lang)
            val legend = label(9.8f, colour, Gravity.CENTER).apply {
                text = "$monthName · $relation"
                contentDescription = "$monthName, $relation"
                DeskType.apply(this, if (offset == 0) DeskType.SEMIBOLD else DeskType.REGULAR)
            }
            monthLegend.addView(legend, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        }
    }

    private fun lunarCycle(anchor: String): Pair<Array<String?>, Array<String?>> {
        var probe = if (PanchangaStore.isAmavasya(PanchangaStore.local(anchor).tithi)) PanchangaStore.shiftKey(anchor, -1) else anchor
        var attempts = 0
        while (!PanchangaStore.isAmavasya(PanchangaStore.local(probe).tithi) && attempts++ < 40) {
            probe = PanchangaStore.shiftKey(probe, -1)
        }
        val shukla = arrayOfNulls<String>(15)
        val krishna = arrayOfNulls<String>(15)
        var key = PanchangaStore.shiftKey(probe, 1)
        repeat(36) {
            val pan = PanchangaStore.local(key)
            val idx = tithiIndex(pan.tithi)
            val target = if (pan.paksha.contains("SHUKLA", true) || PanchangaStore.isPurnima(pan.tithi)) shukla else krishna
            if (idx >= 0 && target[idx] == null) target[idx] = key
            key = PanchangaStore.shiftKey(key, 1)
        }
        return shukla to krishna
    }

    private fun bindTithiHeader(lang: IndicScript.Lang) {
        tithiRow.removeAllViews()
        repeat(15) { index ->
            val heading = label(9.6f, if (index == 14) accent else muted, Gravity.CENTER).apply {
                text = tithiShort(index, lang)
                setBackgroundResource(R.drawable.bg_tithi_grid)
                if (index == 14) DeskType.apply(this, DeskType.SEMIBOLD)
            }
            tithiRow.addView(heading, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        }
    }

    private fun bindRow(row: LinearLayout, dates: Array<String?>, lang: IndicScript.Lang, selectedKey: String) {
        row.removeAllViews()
        dates.forEachIndexed { index, key ->
            val cell = LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(1), dp(1), dp(1), dp(1))
                setBackgroundResource(R.drawable.bg_tithi_grid)
            }
            val date = label(13.2f, white, Gravity.CENTER).apply {
                text = key?.let { OsLang.digits(PanchangaStore.parse(it).get(Calendar.DAY_OF_MONTH), IndicScript.Lang.KN) } ?: "—"
            }
            cell.addView(date, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            if (key == null) {
                cell.alpha = 0.35f
                cell.isEnabled = false
            } else {
                val pan = PanchangaStore.local(key)
                val selected = key == selectedKey
                val today = key == PanchangaStore.dateKey()
                val marked = FestivalStore.has(key) || MadhwaDays.isMarked(pan.masa, pan.paksha, pan.tithi)
                val special = PanchangaStore.isPurnima(pan.tithi) ||
                    PanchangaStore.isAmavasya(pan.tithi) || PanchangaStore.isEkadasi(pan.tithi)
                cell.setBackgroundResource(if (today) R.drawable.bg_tithi_today else R.drawable.bg_tithi_grid)
                val cal = PanchangaStore.parse(key)
                date.setTextColor(monthColour(monthDistance(cal)))
                DeskType.apply(date, if (selected || today || marked || special) DeskType.SEMIBOLD else DeskType.REGULAR)
                cell.contentDescription = "${OsLang.script(pan.tithi, lang)}, ${OsLang.dateLine(cal, lang)}"
                cell.isClickable = !locked
                cell.setOnClickListener { select(key, false) }
                cell.setOnLongClickListener { select(key, true); true }
            }
            row.addView(cell, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        }
    }

    private fun monthDistance(cal: Calendar): Int {
        val anchorIndex = year * 12 + month
        val dateIndex = cal.get(Calendar.YEAR) * 12 + cal.get(Calendar.MONTH)
        return dateIndex - anchorIndex
    }

    private fun monthColour(distance: Int): Int = when {
        distance < 0 -> previousMonth
        distance > 0 -> nextMonth
        else -> currentMonth
    }

    private fun select(key: String, open: Boolean) {
        if (locked) return
        val cal = PanchangaStore.parse(key)
        year = cal.get(Calendar.YEAR); month = cal.get(Calendar.MONTH); selectedDay = cal.get(Calendar.DAY_OF_MONTH)
        onDaySelected?.invoke(year, month, selectedDay)
        if (open) onDayOpened?.invoke(year, month, selectedDay)
        bind()
    }

    private fun tithiIndex(value: String): Int {
        val t = value.uppercase()
        return when {
            PanchangaStore.isPurnima(t) || PanchangaStore.isAmavasya(t) -> 14
            "CHATURDASH" in t -> 13
            "TRAYODASH" in t -> 12
            "DWADASH" in t || "DVADASH" in t -> 11
            "EKADASH" in t || "EKADAS" in t -> 10
            "DASHAM" in t || "DASAM" in t -> 9
            "NAVAM" in t -> 8
            "ASHTAM" in t -> 7
            "SAPTAM" in t -> 6
            "SHASHTH" in t || "SASHTH" in t -> 5
            "PANCHAM" in t -> 4
            "CHATURTH" in t -> 3
            "TRITI" in t -> 2
            "DWITI" in t || "DVITI" in t -> 1
            "PRATIP" in t -> 0
            else -> -1
        }
    }

    private fun tithiShort(index: Int, lang: IndicScript.Lang): String {
        return when (lang) {
            IndicScript.Lang.KN -> KN_SHORT[index]
            IndicScript.Lang.SA -> SA_SHORT[index]
            IndicScript.Lang.EN -> EN_SHORT[index]
        }
    }

    private fun label(size: Float, color: Int, gravityValue: Int) = TextView(context).apply {
        gravity = gravityValue
        textAlignment = if (gravityValue == Gravity.CENTER) View.TEXT_ALIGNMENT_CENTER else View.TEXT_ALIGNMENT_VIEW_START
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        includeFontPadding = false
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        private val TITHIS = arrayOf("PRATIPADA", "DWITIYA", "TRITIYA", "CHATURTHI", "PANCHAMI", "SHASHTHI", "SAPTAMI", "ASHTAMI", "NAVAMI", "DASHAMI", "EKADASHI", "DWADASHI", "TRAYODASHI", "CHATURDASHI", "PURNIMA")
        private val KN_SHORT = arrayOf("ಪ್ರ", "ದ್ವಿ", "ತೃ", "ಚ", "ಪಂ", "ಷ", "ಸ", "ಅ", "ನ", "ದ", "ಏ", "ದ್ವಾ", "ತ್ರ", "ಚತು", "ಪೂ / ಅಮಾ")
        private val SA_SHORT = arrayOf("प्र", "द्वि", "तृ", "च", "पं", "ष", "स", "अ", "न", "द", "ए", "द्वा", "त्र", "चतु", "पू / अमा")
        private val EN_SHORT = arrayOf("PR", "D2", "TR", "C4", "P5", "S6", "S7", "A8", "N9", "D10", "E11", "D12", "T13", "C14", "FULL / NEW")

        fun showDay(context: Context, y: Int, m: Int, d: Int) {
            val cal = Calendar.getInstance().apply { set(y, m, d) }
            val lang = OsLang.get(context)
            val pan = PanchangaStore.forDate(context, PanchangaStore.dateKey(cal))
            FestivalStore.load(context)
            val key = PanchangaStore.dateKey(cal)
            val fest = FestivalStore.label(key, lang).ifBlank { MadhwaDays.name(pan.masa, pan.paksha, pan.tithi) }
            val dlg = Dialog(context, R.style.Theme_PaperDesk_Dialog)
            dlg.setContentView(R.layout.popup_day)
            dlg.findViewById<TextView>(R.id.dayDate).text = OsLang.dateLine(cal, lang)
            dlg.findViewById<TextView>(R.id.dayFest).apply { visibility = if (fest.isBlank()) View.GONE else View.VISIBLE; text = OsLang.script(fest, lang) }
            dlg.findViewById<TextView>(R.id.dayTithi).text = OsLang.script(listOf(pan.tithi, pan.paksha).filter { it.isNotBlank() }.joinToString("  ·  "), lang)
            dlg.findViewById<TextView>(R.id.dayMasa).text = OsLang.script(listOf(pan.masa, pan.samvatsara, pan.ayana).filter { it.isNotBlank() }.joinToString("  ·  "), lang)
            dlg.findViewById<TextView>(R.id.dayNak).text = if (pan.nakshatra.isBlank()) "" else OsLang.script("${pan.nakshatra} NAKSHATRA", lang)
            dlg.findViewById<TextView>(R.id.dayYoga).text = OsLang.script(listOf(pan.yoga, pan.karana).filter { it.isNotBlank() }.joinToString("  ·  "), lang)
            dlg.findViewById<TextView>(R.id.daySrc).apply { visibility = if (pan.source.isBlank()) View.GONE else View.VISIBLE; text = pan.source }
            DeskType.apply(dlg.findViewById(R.id.dayDate), DeskType.SEMIBOLD)
            DeskType.apply(dlg.findViewById(R.id.dayFest), DeskType.SEMIBOLD)
            DeskDialogs.show(dlg, 0.52f)
        }
    }
}
