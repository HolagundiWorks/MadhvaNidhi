package com.paperdesk.launcher.ui

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.ProjectStore
import com.paperdesk.launcher.data.TaskItem
import com.paperdesk.launcher.data.TaskStore
import java.util.Calendar

class TaskEntryActivity : AppCompatActivity() {

    private val cal = Calendar.getInstance()
    private var priority = TaskItem.PRIORITY_MED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (com.paperdesk.launcher.DeskLock.bounce(this)) return
        setContentView(R.layout.activity_entry_task)
        EntryUi.prepare(this)
        val store = TaskStore(this)
        val projects = ProjectStore(this)
        val id = intent.getStringExtra(EXTRA_ID)
        val extraPid = intent.getStringExtra(EntryUi.EXTRA_PROJECT_ID).orEmpty()
        val existing = id?.let { store.get(it) }
        val projectId = existing?.projectId?.ifBlank { extraPid } ?: extraPid
        val pname = projects.nameOf(projectId)
        if (existing != null) {
            cal.timeInMillis = existing.dueAt
            priority = existing.priority
        }
        findViewById<TextView>(R.id.entryTitle).text =
            getString(if (existing == null) R.string.new_task else R.string.edit_task) +
                if (pname.isBlank()) "" else "  -  $pname"
        val name = findViewById<EditText>(R.id.entryName)
        val notes = findViewById<EditText>(R.id.entryNotes)
        name.setText(existing?.text ?: "")
        notes.setText(existing?.notes ?: "")
        val dateTv = findViewById<TextView>(R.id.entryDate)
        val timeTv = findViewById<TextView>(R.id.entryTime)
        val priTv = findViewById<TextView>(R.id.entryPriority)
        fun paint() {
            dateTv.text = EntryUi.dateLabel(cal)
            timeTv.text = EntryUi.timeLabel(cal)
            priTv.text = when (priority) {
                TaskItem.PRIORITY_HIGH -> "HIGH"
                TaskItem.PRIORITY_LOW -> "LOW"
                else -> "MED"
            }
        }
        paint()
        dateTv.setOnClickListener { EntryUi.pickDate(this, cal) { paint() } }
        timeTv.setOnClickListener { EntryUi.pickTime(this, cal) { paint() } }
        priTv.setOnClickListener {
            priority = (priority + 1) % 3
            paint()
        }
        findViewById<TextView>(R.id.entryCancel).setOnClickListener { finish() }
        findViewById<TextView>(R.id.entrySave).setOnClickListener {
            val t = name.text?.toString()?.trim().orEmpty()
            if (t.isEmpty()) return@setOnClickListener
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val item = (existing ?: TaskItem(text = t, projectId = projectId)).copy(
                text = t,
                notes = notes.text?.toString()?.trim().orEmpty(),
                dueAt = cal.timeInMillis,
                timeLabel = EntryUi.timeLabel(cal),
                priority = priority,
                projectId = projectId,
                project = pname
            )
            store.upsert(item)
            projects.touch(projectId)
            finish()
        }
    }

    companion object {
        const val EXTRA_ID = "id"
    }
}
