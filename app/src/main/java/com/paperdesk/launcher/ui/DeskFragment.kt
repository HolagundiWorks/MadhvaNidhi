package com.paperdesk.launcher.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.paperdesk.launcher.weather.WeatherClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DeskFragment : Fragment() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastPanchangaDate = ""
    private var selectedDate = com.paperdesk.launcher.data.PanchangaStore.dateKey()
    private val lockListener: () -> Unit = { applyLockState() }
    private val langTick: () -> Unit = {
        bindDate()
        bindLang()
        loadPanchanga()
        view?.findViewById<MonthCalendarView>(R.id.calendar)?.refresh()
    }

    private val clockTick = object : Runnable {
        override fun run() {
            bindDate()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.page_desk, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        PagePalette.apply(view, "desk")
        bindClockLock(view.findViewById(R.id.clockLockArea))
        view.findViewById<MonthCalendarView>(R.id.calendar).onDaySelected = { y, m, d ->
            val c = java.util.Calendar.getInstance()
            c.set(y, m, d)
            selectedDate = com.paperdesk.launcher.data.PanchangaStore.dateKey(c)
            bindDate()
            loadPanchanga()
        }
        view.findViewById<MonthCalendarView>(R.id.calendar).onDayOpened = { y, m, d ->
            if (!DeskLock.isLocked(requireContext())) {
                MonthCalendarView.showDay(requireContext(), y, m, d)
            }
        }
        bindDate()
        bindLang()
        loadPanchanga()
        applyLockState()
    }

    override fun onResume() {
        super.onResume()
        DeskLock.addListener(lockListener)
        OsLang.addListener(langTick)
        handler.removeCallbacks(clockTick)
        handler.post(clockTick)
        applyLockState()
        loadPanchanga()
    }

    override fun onPause() {
        DeskLock.removeListener(lockListener)
        OsLang.removeListener(langTick)
        handler.removeCallbacks(clockTick)
        super.onPause()
    }

    fun applyLockState() {
        val v = view ?: return
        val locked = DeskLock.isLocked(requireContext())
        v.findViewById<View>(R.id.deskOpen).visibility = View.VISIBLE
        v.findViewById<MonthCalendarView>(R.id.calendar).locked = locked
        bindDate()
        bindLang()
    }

    fun bindLang() {
        val v = view ?: return
        v.findViewById<TextView>(R.id.todayEventsTitle)?.text = when (OsLang.get(requireContext())) {
            IndicScript.Lang.KN -> "ಇಂದಿನ ವಿಶೇಷಗಳು"
            IndicScript.Lang.SA -> "अद्यतन विशेषाः"
            IndicScript.Lang.EN -> "TODAY'S EVENTS"
        }
    }

    private fun bindClockLock(target: ClockLockLayout?) {
        target ?: return
        target.onDoubleTap = { toggleLock() }
    }

    private fun toggleLock() {
        DeskLock.toggle(requireContext())
        (activity as? LauncherActivity)?.applyLockChrome()
        applyLockState()
    }

    private fun bindDate() {
        val v = view ?: return
        val cal = java.util.Calendar.getInstance()
        val parts = selectedDate.split("-")
        if (parts.size == 3) {
            cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        }
        val lang = OsLang.get(requireContext())
        val day = OsLang.digits(cal.get(java.util.Calendar.DAY_OF_MONTH), lang)
        val year = OsLang.digits(cal.get(java.util.Calendar.YEAR), lang)
        v.findViewById<TextView>(R.id.todayWeek)?.text = OsLang.weekday(cal, lang)
        v.findViewById<TextView>(R.id.clockDate)?.text = "$day ${OsLang.month(cal, lang)} $year"
        val clock = v.findViewById<AnalogClockView>(R.id.analogClock)
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val min = java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE)
        val locked = DeskLock.isLocked(requireContext())
        val time = "${OsLang.ui("time", lang)} ${OsLang.digits(hour, lang)}:${OsLang.digits("%02d".format(min), lang)}"
        clock?.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        clock?.moonPhase = WeatherClient.moonPhase(cal.timeInMillis)
        v.findViewById<View>(R.id.clockLockArea).contentDescription =
            "$time. ${OsLang.ui(if (locked) "unlock" else "lock", lang)}"
        if (selectedDate != lastPanchangaDate) {
            loadPanchanga()
        }
    }

    private fun loadPanchanga() {
        viewLifecycleOwner.lifecycleScope.launch {
            val day = withContext(Dispatchers.IO) {
                com.paperdesk.launcher.data.PanchangaStore.forDate(requireContext(), selectedDate)
            }
            val v = view ?: return@launch
            lastPanchangaDate = day.date
            val lang = OsLang.get(requireContext())
            FestivalStore.load(requireContext())
            val fest = FestivalStore.label(day.date, lang).ifBlank {
                com.paperdesk.launcher.data.MadhwaDays.name(day.masa, day.paksha, day.tithi)
            }
            val event = v.findViewById<TextView>(R.id.dayEvent)
            if (fest.isBlank()) {
                event.visibility = View.VISIBLE
                event.text = "—"
            } else {
                event.visibility = View.VISIBLE
                event.text = OsLang.script(fest, lang)
                DeskType.apply(event, DeskType.SEMIBOLD)
            }
            line(v, R.id.clockSam, OsLang.script(day.samvatsara, lang))
            line(
                v,
                R.id.clockMasaPaksha,
                OsLang.script(
                    listOf(day.masa, day.paksha).filter { it.isNotBlank() }.joinToString("  ·  "),
                    lang
                )
            )
            line(v, R.id.panchangaVara, OsLang.script(day.vara, lang))
            line(v, R.id.panchangaTithi, OsLang.script(day.tithi, lang))
            line(
                v,
                R.id.panchangaNak,
                if (day.nakshatra.isBlank()) "" else OsLang.script("${day.nakshatra} NAKSHATRA", lang)
            )
            line(v, R.id.panchangaKarana, OsLang.script(day.karana, lang))
            line(v, R.id.panchangaAyana, OsLang.script(day.ayana, lang))
            line(v, R.id.panchangaRitu, OsLang.script(day.ritu, lang))
            DeskType.apply(v.findViewById(R.id.clockSam), DeskType.SEMIBOLD)
            DeskType.apply(v.findViewById(R.id.panchangaTithi), DeskType.SEMIBOLD)
        }
    }

    private fun line(v: View, id: Int, text: String) {
        val tv = v.findViewById<TextView>(id)
        tv.text = text
        tv.visibility = if (text.isBlank()) View.GONE else View.VISIBLE
    }
}
