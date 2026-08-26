package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.ProjectStore
import com.paperdesk.launcher.data.TaskItem
import com.paperdesk.launcher.data.TaskStore
import com.paperdesk.launcher.util.PomodoroClock

class TasksFragment : Fragment() {

    private lateinit var store: TaskStore
    private lateinit var projects: ProjectStore
    private val all = mutableListOf<TaskItem>()
    private lateinit var adapter: TaskAdapter
    private var projectId: String = ""
    private var filter = FILTER_ALL
    private var filterProjectId = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.page_tasks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        store = TaskStore(requireContext())
        projects = ProjectStore(requireContext())
        projectId = arguments?.getString(ARG_PROJECT).orEmpty()
        adapter = TaskAdapter(
            all,
            onToggle = { item ->
                store.upsert(item)
                if (item.projectId.isNotBlank()) projects.touch(item.projectId)
                reload()
            },
            onDelete = { item ->
                store.save(store.load().filter { it.id != item.id })
                if (item.projectId.isNotBlank()) projects.touch(item.projectId)
                reload()
            },
            onEdit = { startActivity(EntryUi.task(requireContext(), it.id, it.projectId.ifBlank { projectId.ifBlank { null } })) },
            nameOf = { projects.nameOf(it) },
            showProject = projectId.isBlank(),
            onOpen = { item ->
                if (item.projectId.isNotBlank()) {
                    startActivity(EntryUi.openProject(requireContext(), item.projectId, ProjectActivity.TODO))
                }
            }
        )
        val list = view.findViewById<RecyclerView>(R.id.taskList)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, a: RecyclerView.ViewHolder, b: RecyclerView.ViewHolder) = false
            override fun onSwiped(holder: RecyclerView.ViewHolder, dir: Int) {
                val item = adapter.itemAt(holder.bindingAdapterPosition) ?: return
                if (dir == ItemTouchHelper.LEFT) {
                    store.save(store.load().filter { it.id != item.id })
                    if (item.projectId.isNotBlank()) projects.touch(item.projectId)
                    reload()
                    return
                }
                val next = store.load()
                val i = next.indexOfFirst { it.id == item.id }
                if (i < 0) return
                next[i] = next[i].copy(done = !next[i].done)
                store.save(next)
                if (next[i].projectId.isNotBlank()) projects.touch(next[i].projectId)
                reload()
            }
        }).attachToRecyclerView(list)
        view.findViewById<View>(R.id.fabTask).setOnClickListener {
            startActivity(EntryUi.task(requireContext(), projectId = projectId.ifBlank { null }))
        }
        val pomo = view.findViewById<PomodoroStripView>(R.id.pomoStrip)
        pomo.onLongPress = { pomodoroSettings() }
        if (projectId.isNotBlank()) {
            pomo.visibility = View.GONE
            view.findViewById<View>(R.id.taskFilters).visibility = View.GONE
        }
        reload()
    }

    override fun onResume() {
        super.onResume()
        if (::store.isInitialized) reload()
    }

    private fun reload() {
        val src = if (projectId.isNotBlank()) store.forProject(projectId) else store.load()
        val visible = when {
            projectId.isNotBlank() -> src
            filter == FILTER_TODAY -> src.filter { it.isToday() }
            filter == FILTER_UPCOMING -> src.filter { it.isUpcoming() }
            filter == FILTER_OVERDUE -> src.filter { it.isOverdue() }
            filter == FILTER_DONE -> src.filter { it.done }
            filter == FILTER_PROJECT -> src.filter { it.projectId == filterProjectId }
            else -> src
        }
        all.clear()
        all.addAll(visible)
        if (::adapter.isInitialized) adapter.refresh()
        paintFilters()
    }

    private fun paintFilters() {
        val row = view?.findViewById<LinearLayout>(R.id.taskFilters) ?: return
        if (projectId.isNotBlank()) return
        row.removeAllViews()
        val chips = mutableListOf(
            FILTER_ALL to "ALL",
            FILTER_TODAY to "TODAY",
            FILTER_UPCOMING to "UPCOMING",
            FILTER_OVERDUE to "OVERDUE",
            FILTER_DONE to "DONE"
        )
        projects.load().forEach { chips += (FILTER_PROJECT + ":" + it.id) to it.name.uppercase() }
        chips.forEach { (key, label) ->
            val selected = if (key.startsWith("$FILTER_PROJECT:")) {
                filter == FILTER_PROJECT && filterProjectId == key.substringAfter(":")
            } else {
                filter == key
            }
            row.addView(TextView(requireContext()).apply {
                text = label
                DeskUi.chip(this, selected)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.rightMargin = dp(8)
                layoutParams = lp
                setOnClickListener {
                    if (key.startsWith("$FILTER_PROJECT:")) {
                        filter = FILTER_PROJECT
                        filterProjectId = key.substringAfter(":")
                    } else {
                        filter = key
                        filterProjectId = ""
                    }
                    reload()
                }
            })
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun pomodoroSettings() {
        val dlg = android.app.Dialog(requireContext(), R.style.Theme_PaperDesk_Dialog)
        dlg.setContentView(R.layout.dialog_pomo)
        val accent = requireContext().getColor(R.color.accent)
        val white = requireContext().getColor(R.color.white)
        val auto = dlg.findViewById<TextView>(R.id.pomoAuto)
        auto.setTextColor(if (PomodoroClock.autoBreak) accent else white)
        dlg.findViewById<View>(R.id.pomoFocus25).setOnClickListener {
            PomodoroClock.setFocusMinutes(25); dlg.dismiss()
        }
        dlg.findViewById<View>(R.id.pomoFocus50).setOnClickListener {
            PomodoroClock.setFocusMinutes(50); dlg.dismiss()
        }
        dlg.findViewById<View>(R.id.pomoBreak5).setOnClickListener {
            PomodoroClock.shortBreakMs = 5 * 60_000L; dlg.dismiss()
        }
        dlg.findViewById<View>(R.id.pomoBreak15).setOnClickListener {
            PomodoroClock.longBreakMs = 15 * 60_000L; dlg.dismiss()
        }
        auto.setOnClickListener {
            PomodoroClock.autoBreak = !PomodoroClock.autoBreak
            auto.setTextColor(if (PomodoroClock.autoBreak) accent else white)
        }
        dlg.findViewById<View>(R.id.pomoReset).setOnClickListener {
            PomodoroClock.reset(); dlg.dismiss()
        }
        dlg.findViewById<View>(R.id.pomoDone).setOnClickListener { dlg.dismiss() }
        DeskDialogs.show(dlg, 0.56f)
    }

    companion object {
        const val ARG_PROJECT = "projectId"
        private const val FILTER_ALL = "all"
        private const val FILTER_TODAY = "today"
        private const val FILTER_UPCOMING = "upcoming"
        private const val FILTER_OVERDUE = "overdue"
        private const val FILTER_DONE = "done"
        private const val FILTER_PROJECT = "project"

        fun newInstance(projectId: String) = TasksFragment().apply {
            arguments = Bundle().apply { putString(ARG_PROJECT, projectId) }
        }
    }
}
