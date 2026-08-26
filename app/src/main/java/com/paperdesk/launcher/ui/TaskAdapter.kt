package com.paperdesk.launcher.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.TaskItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskAdapter(
    private val all: MutableList<TaskItem>,
    private val onToggle: (TaskItem) -> Unit,
    private val onDelete: (TaskItem) -> Unit,
    private val onEdit: (TaskItem) -> Unit,
    private val nameOf: (String) -> String = { "" },
    private val showProject: Boolean = true,
    private val onOpen: (TaskItem) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val rows = mutableListOf<Row>()
    private val locale = Locale("en", "IN")

    private sealed class Row {
        data class Head(val title: String) : Row()
        data class Task(val item: TaskItem) : Row()
        data class Empty(val title: String) : Row()
    }

    class HeadVH(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view as TextView
    }

    class TaskVH(view: View) : RecyclerView.ViewHolder(view) {
        val check: CheckBox = view.findViewById(R.id.taskCheck)
        val text: TextView = view.findViewById(R.id.taskText)
        val project: TextView = view.findViewById(R.id.taskProject)
        val meta: TextView = view.findViewById(R.id.taskMeta)
        val open: TextView = view.findViewById(R.id.taskOpen)
        val delete: TextView = view.findViewById(R.id.taskDelete)
    }

    fun refresh() {
        rows.clear()
        val pending = all.filter { !it.done && !it.isUpcoming() }.sortedBy { it.dueAt }
        val upcoming = all.filter { it.isUpcoming() }.sortedBy { it.dueAt }
        val done = all.filter { it.done }
        if (pending.isEmpty() && upcoming.isEmpty() && done.isEmpty()) {
            rows += Row.Empty("NO TODOS")
        } else {
            rows += Row.Head("PENDING")
            if (pending.isEmpty()) {
                rows += Row.Empty("NO PENDING")
            } else {
                pending.forEach { rows += Row.Task(it) }
            }
            rows += Row.Head("UPCOMING")
            if (upcoming.isEmpty()) {
                rows += Row.Empty("NO UPCOMING")
            } else {
                upcoming.forEach { rows += Row.Task(it) }
            }
            if (done.isNotEmpty()) {
                rows += Row.Head("COMPLETED")
                done.forEach { rows += Row.Task(it) }
            }
        }
        notifyDataSetChanged()
    }

    fun itemAt(position: Int): TaskItem? = (rows.getOrNull(position) as? Row.Task)?.item

    override fun getItemViewType(position: Int): Int = when (rows[position]) {
        is Row.Head, is Row.Empty -> 0
        is Row.Task -> 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) {
            val tv = TextView(parent.context).apply {
                setTextColor(parent.context.getColor(R.color.white_55))
                textSize = 11f
                DeskType.apply(this, DeskType.MEDIUM)
                setPadding(0, (12 * resources.displayMetrics.density).toInt(), 0, (6 * resources.displayMetrics.density).toInt())
            }
            HeadVH(tv)
        } else {
            TaskVH(LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false))
        }
    }

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Head -> (holder as HeadVH).text.text = row.title
            is Row.Empty -> (holder as HeadVH).text.text = row.title
            is Row.Task -> {
                val h = holder as TaskVH
                val item = row.item
                h.text.text = item.text
                val pname = nameOf(item.projectId).ifBlank { item.project }
                val tagged = showProject && pname.isNotBlank()
                val due = dueLabel(item)
                h.project.text = if (tagged) "[$pname]  ·  $due" else due
                h.project.visibility = View.VISIBLE
                h.open.visibility = if (tagged) View.VISIBLE else View.GONE
                h.meta.text = item.notes
                h.meta.visibility = if (item.notes.isBlank()) View.GONE else View.VISIBLE
                h.check.setOnCheckedChangeListener(null)
                h.check.isChecked = item.done
                if (item.done) {
                    h.text.paintFlags = h.text.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    DeskType.apply(h.text, DeskType.REGULAR)
                    h.text.setTextColor(h.itemView.context.getColor(R.color.white_70))
                    h.itemView.alpha = 0.72f
                } else {
                    h.text.paintFlags = h.text.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    DeskType.primary(h.text)
                    h.itemView.alpha = 1f
                }
                h.check.setOnCheckedChangeListener { _, checked ->
                    onToggle(item.copy(done = checked))
                }
                h.open.setOnClickListener { onOpen(item) }
                h.delete.setOnClickListener { onDelete(item) }
                h.itemView.setOnClickListener { onEdit(item) }
            }
        }
    }

    private fun dueLabel(item: TaskItem): String {
        val pri = when (item.priority) {
            TaskItem.PRIORITY_HIGH -> "HIGH  "
            TaskItem.PRIORITY_LOW -> "LOW  "
            else -> ""
        }
        val whenStr = when {
            item.isToday() -> "Today"
            item.isOverdue() -> "Overdue  " + SimpleDateFormat("d MMM", locale).format(Date(item.dueAt))
            else -> SimpleDateFormat("d MMM  HH:mm", locale).format(Date(item.dueAt))
        }
        return pri + whenStr
    }
}
