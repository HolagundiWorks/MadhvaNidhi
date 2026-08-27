package com.paperdesk.launcher.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.FestivalStore
import com.paperdesk.launcher.data.IndicScript
import com.paperdesk.launcher.data.MadhwaDays
import com.paperdesk.launcher.data.OsLang
import com.paperdesk.launcher.data.PanchangaStore
import java.util.Calendar
import java.util.Locale

class MonthCalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val accent = ContextCompat.getColor(context, R.color.accent)
    private val black = ContextCompat.getColor(context, R.color.black)
    private val white = ContextCompat.getColor(context, R.color.white)
    private val muted = ContextCompat.getColor(context, R.color.white_55)
    private val locale = Locale("en", "IN")

    var year: Int
    var month: Int
    var selectedDay: Int
        private set

    var onDaySelected: ((year: Int, month: Int, day: Int) -> Unit)? = null
    var onDayOpened: ((year: Int, month: Int, day: Int) -> Unit)? = null
    var locked: Boolean = false
        set(value) {
            field = value
            bind(jumpToMonth = false)
        }

    private val titleTv: TextView
    private val headRow: LinearLayout
    private val scroller: NestedScrollView
    private val host: LinearLayout
    private val monthRoots = ArrayList<View>(12)
    private var fittedH = 0
    private var headerMonth = -1
    private var headerYear = -1
    private var binding = false

    init {
        orientation = VERTICAL
        clipChildren = true
        clipToPadding = true
        FestivalStore.load(context)
        val now = Calendar.getInstance()
        year = now.get(Calendar.YEAR)
        month = now.get(Calendar.MONTH)
        selectedDay = now.get(Calendar.DAY_OF_MONTH)

        titleTv = TextView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTextColor(white)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            includeFontPadding = false
            setPadding(0, dp(2), 0, dp(4))
        }
        DeskType.apply(titleTv, DeskType.SEMIBOLD)
        addView(titleTv)

        headRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        addView(headRow)

        host = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        scroller = object : NestedScrollView(context) {
            override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
                if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
                    parent.requestDisallowInterceptTouchEvent(true)
                }
                return super.onInterceptTouchEvent(ev)
            }

            override fun onTouchEvent(ev: MotionEvent): Boolean {
                if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
                    parent.requestDisallowInterceptTouchEvent(true)
                }
                return super.onTouchEvent(ev)
            }
        }.apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
            isFillViewport = true
            overScrollMode = OVER_SCROLL_NEVER
            isVerticalScrollBarEnabled = false
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
            addView(host)
            setOnScrollChangeListener { _, _, scrollY, _, _ ->
                if (!binding) applyHeader(monthAt(scrollY))
            }
        }
        addView(scroller)
        bindHeaderLabels()
        applyHeader(month)
    }

    fun show(y: Int, m: Int, day: Int) {
        year = y
        month = m
        selectedDay = day
        bind(jumpToMonth = true)
    }

    fun refresh() {
        bindHeaderLabels()
        bind(jumpToMonth = false)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (binding) return
        val gridH = (height - titleTv.measuredHeight - headRow.measuredHeight).coerceAtLeast(0)
        if (gridH <= 0 || (gridH == fittedH && monthRoots.isNotEmpty())) return
        fittedH = gridH
        post { bind(jumpToMonth = true) }
    }

    private fun bind(jumpToMonth: Boolean = true) {
        if (fittedH <= 0) return
        val keepY = scroller.scrollY
        val lang = OsLang.get(context)
        binding = true
        host.removeAllViews()
        monthRoots.clear()
        FestivalStore.load(context)
        val pageH = fittedH
        repeat(12) { m ->
            val block = monthBlock(year, m, lang, pageH)
            monthRoots += block
            host.addView(block)
        }
        post {
            if (jumpToMonth) scrollToMonth(month)
            else scroller.scrollTo(0, keepY)
            applyHeader(monthAt(scroller.scrollY))
            binding = false
        }
    }

    private fun bindHeaderLabels() {
        val lang = OsLang.get(context)
        headRow.removeAllViews()
        HEAD_KEYS.forEach { key ->
            headRow.addView(TextView(context).apply {
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
                gravity = Gravity.CENTER
                setTextColor(muted)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                includeFontPadding = false
                setPadding(dp(1), dp(1), dp(1), dp(2))
                text = OsLang.ui(key, lang)
            })
        }
    }

    private fun scrollToMonth(m: Int) {
        val child = monthRoots.getOrNull(m) ?: return
        scroller.scrollTo(0, child.top)
    }

    private fun monthAt(scrollY: Int): Int {
        if (monthRoots.isEmpty() || fittedH <= 0) return month
        val idx = ((scrollY + fittedH / 2) / fittedH).coerceIn(0, monthRoots.lastIndex)
        return idx
    }

    private fun applyHeader(m: Int) {
        if (m == headerMonth && year == headerYear) return
        headerMonth = m
        headerYear = year
        val cal = Calendar.getInstance()
        cal.set(year, m, 1)
        val lang = OsLang.get(context)
        val label = "${OsLang.month(cal, lang)} ${OsLang.digits(year, IndicScript.Lang.KN)}"
        titleTv.text = label
        titleTv.contentDescription = label
        scroller.contentDescription = label
    }

    private fun monthBlock(y: Int, m: Int, lang: IndicScript.Lang, pageH: Int): View {
        val wrap = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, pageH)
        }
        val cal = Calendar.getInstance()
        cal.set(y, m, 1)
        val firstDow = cal.get(Calendar.DAY_OF_WEEK)
        val offset = (firstDow + 5) % 7
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val weekCount = 6
        val rowH = (pageH / weekCount).coerceAtLeast(dp(16))
        val extra = (pageH - rowH * weekCount).coerceAtLeast(0)
        val today = Calendar.getInstance()
        var day = 1
        repeat(weekCount) { r ->
            val h = if (r == weekCount - 1) rowH + extra else rowH
            val row = LinearLayout(context).apply {
                orientation = HORIZONTAL
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, h)
            }
            repeat(7) { col ->
                val index = r * 7 + col
                val box = LinearLayout(context).apply {
                    orientation = VERTICAL
                    gravity = Gravity.CENTER
                    layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
                    setPadding(dp(1), dp(1), dp(1), dp(1))
                }
                val dateTv = TextView(context).apply {
                    gravity = Gravity.CENTER
                    textAlignment = TEXT_ALIGNMENT_CENTER
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    includeFontPadding = true
                    setPadding(0, 0, 0, 0)
                    val mark = dp(28)
                    minWidth = mark
                    minHeight = mark
                    layoutParams = LayoutParams(mark, mark).apply { gravity = Gravity.CENTER }
                }
                box.addView(dateTv)
                if (index < offset || day > daysInMonth) {
                    dateTv.text = ""
                    box.tag = 0
                    box.isClickable = false
                    box.alpha = 0.28f
                    box.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                    box.setBackgroundResource(R.drawable.bg_cell)
                } else {
                    val d = day
                    val key = String.format(locale, "%04d-%02d-%02d", y, m + 1, d)
                    val pan = PanchangaStore.local(key)
                    val purnima = PanchangaStore.isPurnima(pan.tithi)
                    val amavasya = PanchangaStore.isAmavasya(pan.tithi)
                    val ekadasi = PanchangaStore.isEkadasi(pan.tithi)
                    val marked = FestivalStore.has(key) ||
                        PanchangaStore.isMarked(pan.tithi) ||
                        MadhwaDays.isMarked(pan.masa, pan.paksha, pan.tithi)
                    dateTv.text = OsLang.digits(d, IndicScript.Lang.KN)
                    box.tag = d
                    box.isClickable = !locked
                    val isToday = today.get(Calendar.YEAR) == y &&
                        today.get(Calendar.MONTH) == m &&
                        today.get(Calendar.DAY_OF_MONTH) == d
                    val isSel = y == year && m == month && d == selectedDay
                    box.alpha = 1f
                    when {
                        purnima -> {
                            box.setBackgroundResource(R.drawable.bg_cell)
                            dateTv.setBackgroundResource(R.drawable.bg_date_fill)
                        }
                        amavasya -> {
                            box.setBackgroundResource(R.drawable.bg_cell)
                            dateTv.setBackgroundResource(R.drawable.bg_date_outline)
                        }
                        ekadasi -> {
                            box.setBackgroundResource(R.drawable.bg_cell)
                            dateTv.setBackgroundResource(R.drawable.bg_date_circle)
                        }
                        isSel && !isToday -> {
                            box.setBackgroundResource(R.drawable.bg_cell_on)
                            dateTv.setBackgroundResource(R.drawable.bg_cell)
                        }
                        else -> {
                            box.setBackgroundResource(R.drawable.bg_cell)
                            dateTv.setBackgroundResource(R.drawable.bg_cell)
                        }
                    }
                    dateTv.setTextColor(
                        when {
                            purnima || (isSel && !isToday) -> black
                            isToday -> accent
                            ekadasi -> white
                            amavasya || marked -> accent
                            isSel -> black
                            else -> white
                        }
                    )
                    DeskType.apply(dateTv, if (isSel || isToday || marked) DeskType.SEMIBOLD else DeskType.REGULAR)
                    dateTv.paintFlags = if (isToday) {
                        dateTv.paintFlags or Paint.UNDERLINE_TEXT_FLAG
                    } else {
                        dateTv.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
                    }
                    box.contentDescription = "${OsLang.digits(d, lang)} ${OsLang.month(cal, lang)}"
                    if (isSel) box.contentDescription = "${box.contentDescription}, ${OsLang.ui("date", lang)}"
                    box.setOnClickListener {
                        if (locked) return@setOnClickListener
                        year = y
                        month = m
                        selectedDay = d
                        onDaySelected?.invoke(y, m, d)
                        bind(jumpToMonth = false)
                    }
                    box.setOnLongClickListener {
                        if (locked) return@setOnLongClickListener false
                        year = y
                        month = m
                        selectedDay = d
                        onDaySelected?.invoke(y, m, d)
                        onDayOpened?.invoke(y, m, d)
                        bind(jumpToMonth = false)
                        true
                    }
                    day++
                }
                row.addView(box)
            }
            wrap.addView(row)
        }
        return wrap
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    companion object {
        private val HEAD_KEYS = arrayOf("mon_s", "tue_s", "wed_s", "thu_s", "fri_s", "sat_s", "sun_s")

        fun showDay(context: Context, y: Int, m: Int, d: Int) {
            val cal = Calendar.getInstance()
            cal.set(y, m, d)
            val lang = OsLang.get(context)
            val pan = PanchangaStore.forDate(context, PanchangaStore.dateKey(cal))
            FestivalStore.load(context)
            val key = PanchangaStore.dateKey(cal)
            val fest = FestivalStore.label(key, lang).ifBlank {
                MadhwaDays.name(pan.masa, pan.paksha, pan.tithi)
            }
            val dlg = Dialog(context, R.style.Theme_PaperDesk_Dialog)
            dlg.setContentView(R.layout.popup_day)
            dlg.findViewById<TextView>(R.id.dayDate).text = OsLang.dateLine(cal, lang)
            val festTv = dlg.findViewById<TextView>(R.id.dayFest)
            if (fest.isBlank()) {
                festTv.visibility = View.GONE
            } else {
                festTv.visibility = View.VISIBLE
                festTv.text = OsLang.script(fest, lang)
            }
            dlg.findViewById<TextView>(R.id.dayTithi).text = OsLang.script(
                listOf(pan.tithi, pan.paksha).filter { it.isNotBlank() }.joinToString("  ·  "),
                lang
            )
            dlg.findViewById<TextView>(R.id.dayMasa).text = OsLang.script(
                listOf(pan.masa, pan.samvatsara, pan.ayana).filter { it.isNotBlank() }.joinToString("  ·  "),
                lang
            )
            dlg.findViewById<TextView>(R.id.dayNak).text =
                if (pan.nakshatra.isBlank()) "" else OsLang.script("${pan.nakshatra} NAKSHATRA", lang)
            dlg.findViewById<TextView>(R.id.dayYoga).text = OsLang.script(
                listOf(pan.yoga, pan.karana).filter { it.isNotBlank() }.joinToString("  ·  "),
                lang
            )
            val srcTv = dlg.findViewById<TextView>(R.id.daySrc)
            if (pan.source.contains("SRS", ignoreCase = true)) {
                srcTv.visibility = View.VISIBLE
                srcTv.text = OsLang.script("SRS MANTRALAYA", lang)
            } else {
                srcTv.visibility = View.GONE
            }
            DeskType.apply(dlg.findViewById(R.id.dayDate), DeskType.SEMIBOLD)
            DeskType.apply(dlg.findViewById(R.id.dayFest), DeskType.SEMIBOLD)
            DeskDialogs.show(dlg, 0.52f)
        }
    }
}
