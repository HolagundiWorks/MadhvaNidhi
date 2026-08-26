package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.CalendarEvent
import com.paperdesk.launcher.data.EventStore
import com.paperdesk.launcher.data.OsLang
import com.paperdesk.launcher.data.PanchangaStore
import com.paperdesk.launcher.data.ProjectStore
import com.paperdesk.launcher.data.IndicScript
import java.util.Calendar
import java.util.Locale

class ScheduleFragment : Fragment() {

    private lateinit var store: EventStore
    private lateinit var projects: ProjectStore
    private var projectId: String = ""
    private var dayStart = EventStore.startOfDay()
    private var gridStart = mondayOf(EventStore.startOfDay())
    private lateinit var adapter: EventAdapter
    private val langTick: () -> Unit = { if (view != null) { bindGrid(); bindDay() } }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.page_schedule, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        store = EventStore(requireContext())
        projects = ProjectStore(requireContext())
        projectId = arguments?.getString(ARG_PROJECT).orEmpty()
        adapter = EventAdapter(
            rows = { store.forDay(dayStart, projectId.ifBlank { null }) },
            nameOf = { projects.nameOf(it) },
            showProject = projectId.isBlank(),
            onEdit = { openEntry(it.id) },
            onOpen = { ev ->
                if (ev.projectId.isNotBlank()) {
                    startActivity(EntryUi.openProject(requireContext(), ev.projectId, ProjectActivity.SCHEDULE))
                }
            },
            onToggle = { item ->
                store.upsert(item.copy(done = !item.done))
                if (item.projectId.isNotBlank()) projects.touch(item.projectId)
                bindDay()
            },
            onDelete = { item ->
                store.save(store.load().filter { it.id != item.id })
                if (item.projectId.isNotBlank()) projects.touch(item.projectId)
                bindDay()
            }
        )
        val list = view.findViewById<RecyclerView>(R.id.schedList)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
        view.findViewById<View>(R.id.fabSched).setOnClickListener { openEntry(null) }
        DeskType.apply(view.findViewById(R.id.schedTitle), DeskType.SEMIBOLD)
        gridStart = mondayOf(dayStart)
        bindGrid()
        bindDay()
    }

    override fun onResume() {
        super.onResume()
        OsLang.addListener(langTick)
        if (!::store.isInitialized) return
        EventStore.lastTouchedDay?.let {
            dayStart = it
            EventStore.lastTouchedDay = null
            if (dayStart < gridStart || dayStart >= endOfGrid()) {
                gridStart = mondayOf(dayStart)
            }
        }
        bindGrid()
        bindDay()
    }

    override fun onPause() {
        OsLang.removeListener(langTick)
        super.onPause()
    }

    private fun openEntry(id: String?) {
        startActivity(
            EntryUi.schedule(
                requireContext(),
                id = id,
                dayStart = dayStart,
                projectId = projectId.ifBlank { null }
            )
        )
    }

    private fun endOfGrid(): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = gridStart
        c.add(Calendar.DAY_OF_MONTH, 14)
        return EventStore.startOfDay(c.timeInMillis)
    }

    private fun bindGrid() {
        val grid = view?.findViewById<LinearLayout>(R.id.weekGrid) ?: return
        grid.removeAllViews()
        val lang = OsLang.get(requireContext())
        val today = EventStore.startOfDay()
        val h = (40 * resources.displayMetrics.density).toInt()
        val gap = resources.getDimensionPixelSize(R.dimen.space_4)
        for (row in 0 until 2) {
            val line = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    h
                ).apply { if (row > 0) topMargin = gap }
            }
            for (col in 0 until 7) {
                val c = Calendar.getInstance().apply {
                    timeInMillis = gridStart
                    add(Calendar.DAY_OF_MONTH, row * 7 + col)
                }
                val start = EventStore.startOfDay(c.timeInMillis)
                val key = PanchangaStore.dateKey(c)
                val pan = PanchangaStore.local(key)
                val holy = PanchangaStore.isMarked(pan.tithi)
                val cell = TextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                    text = OsLang.digits(c.get(Calendar.DAY_OF_MONTH), IndicScript.Lang.KN)
                    isClickable = true
                    isFocusable = true
                    DeskUi.dayCell(
                        this,
                        start == dayStart,
                        start == today,
                        ekadasi = holy,
                        compact = false
                    )
                    setOnClickListener {
                        dayStart = start
                        bindGrid()
                        bindDay()
                    }
                }
                line.addView(cell)
            }
            grid.addView(line)
        }
    }

    private fun bindDay() {
        val v = view ?: return
        val lang = OsLang.get(requireContext())
        val cal = Calendar.getInstance()
        cal.timeInMillis = dayStart
        v.findViewById<TextView>(R.id.schedTitle).text = OsLang.ui("schedule", lang)
        v.findViewById<TextView>(R.id.schedDayLabel).text = OsLang.dateLine(cal, lang)
        val pan = PanchangaStore.local(PanchangaStore.dateKey(cal))
        val pakshaKey = if ("KRISHNA" in pan.paksha.uppercase(Locale.US)) "krishna" else "shukla"
        v.findViewById<TextView>(R.id.schedPanchanga).text = listOf(
            OsLang.ui(pakshaKey, lang),
            OsLang.script(pan.tithi, lang),
            OsLang.script(pan.masa, lang),
            OsLang.script(pan.nakshatra, lang)
        ).filter { it.isNotBlank() }.joinToString("  ·  ")
        v.findViewById<TextView>(R.id.schedEmpty).text = OsLang.ui("no_schedules", lang)
        val rows = store.forDay(dayStart, projectId.ifBlank { null })
        val empty = rows.isEmpty()
        v.findViewById<View>(R.id.schedEmptyBox).visibility = if (empty) View.VISIBLE else View.GONE
        v.findViewById<View>(R.id.schedList).visibility = if (empty) View.GONE else View.VISIBLE
        adapter.lang = lang
        adapter.refresh()
    }

    companion object {
        const val ARG_PROJECT = "projectId"
        fun newInstance(projectId: String) = ScheduleFragment().apply {
            arguments = Bundle().apply { putString(ARG_PROJECT, projectId) }
        }

        fun mondayOf(ms: Long): Long {
            val c = Calendar.getInstance()
            c.timeInMillis = EventStore.startOfDay(ms)
            val dow = c.get(Calendar.DAY_OF_WEEK)
            val delta = (dow + 5) % 7
            c.add(Calendar.DAY_OF_MONTH, -delta)
            return c.timeInMillis
        }
    }

    private class EventAdapter(
        private val rows: () -> List<CalendarEvent>,
        private val nameOf: (String) -> String,
        private val showProject: Boolean,
        private val onEdit: (CalendarEvent) -> Unit,
        private val onOpen: (CalendarEvent) -> Unit,
        private val onToggle: (CalendarEvent) -> Unit,
        private val onDelete: (CalendarEvent) -> Unit
    ) : RecyclerView.Adapter<EventAdapter.VH>() {
        private var data = rows()
        var lang: IndicScript.Lang = IndicScript.Lang.KN

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val time: TextView = view.findViewById(R.id.eventTime)
            val title: TextView = view.findViewById(R.id.eventTitle)
            val project: TextView = view.findViewById(R.id.eventProject)
            val notes: TextView = view.findViewById(R.id.eventNotes)
            val open: TextView = view.findViewById(R.id.eventOpen)
            val mark: TextView = view.findViewById(R.id.eventMark)
            val del: TextView = view.findViewById(R.id.eventDelete)
        }

        fun refresh() {
            data = rows()
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false))
        }

        override fun getItemCount(): Int = data.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = data[position]
            holder.time.text = item.clock()
            DeskType.secondary(holder.time)
            holder.title.text = item.title
            if (item.done) {
                DeskType.supporting(holder.title)
                holder.itemView.alpha = 0.72f
            } else {
                DeskType.primary(holder.title)
                holder.itemView.alpha = 1f
            }
            val pname = nameOf(item.projectId)
            val tagged = showProject && pname.isNotBlank()
            holder.project.text = "[$pname]"
            holder.project.visibility = if (tagged) View.VISIBLE else View.GONE
            holder.open.visibility = if (tagged) View.VISIBLE else View.GONE
            holder.notes.text = item.notes
            holder.notes.visibility = if (item.notes.isBlank()) View.GONE else View.VISIBLE
            holder.mark.text = OsLang.ui(if (item.done) "done" else "todo", lang)
            holder.mark.setTextColor(
                holder.itemView.context.getColor(if (item.done) R.color.accent else R.color.white)
            )
            holder.mark.setOnClickListener { onToggle(item) }
            holder.open.setOnClickListener { onOpen(item) }
            holder.itemView.setOnClickListener { onEdit(item) }
            holder.del.setOnClickListener { onDelete(item) }
        }
    }
}
