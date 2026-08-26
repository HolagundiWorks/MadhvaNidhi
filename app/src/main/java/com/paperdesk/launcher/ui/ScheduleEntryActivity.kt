package com.paperdesk.launcher.ui

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.CalendarEvent
import com.paperdesk.launcher.data.EventStore
import com.paperdesk.launcher.data.OsLang
import com.paperdesk.launcher.data.ProjectStore
import java.util.Calendar

class ScheduleEntryActivity : AppCompatActivity() {

    private val cal = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (com.paperdesk.launcher.DeskLock.bounce(this)) return
        setContentView(R.layout.activity_entry_schedule)
        EntryUi.prepare(this)
        val store = EventStore(this)
        val projects = ProjectStore(this)
        val id = intent.getStringExtra(EXTRA_ID)
        val extraPid = intent.getStringExtra(EntryUi.EXTRA_PROJECT_ID).orEmpty()
        val existing = id?.let { store.get(it) }
        val projectId = existing?.projectId?.ifBlank { extraPid } ?: extraPid
        val pname = projects.nameOf(projectId)
        if (existing != null) {
            cal.timeInMillis = existing.dayStart
            cal.set(Calendar.HOUR_OF_DAY, existing.hour)
            cal.set(Calendar.MINUTE, existing.minute)
        } else {
            val day = intent.getLongExtra(EXTRA_DAY, EventStore.startOfDay())
            cal.timeInMillis = day
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
        }
        val lang = OsLang.get(this)
        findViewById<TextView>(R.id.entryTitle).text =
            OsLang.ui(if (existing == null) "new_event" else "edit_event", lang) +
                if (pname.isBlank()) "" else "  -  $pname"
        findViewById<TextView>(R.id.entryCancel).text = OsLang.ui("cancel", lang)
        findViewById<TextView>(R.id.entrySave).text = OsLang.ui("save", lang)
        findViewById<TextView>(R.id.entryActivityLabel).text = OsLang.ui("activity", lang)
        findViewById<TextView>(R.id.entryDateLabel).text = OsLang.ui("date", lang)
        findViewById<TextView>(R.id.entryTimeLabel).text = OsLang.ui("time", lang)
        findViewById<TextView>(R.id.entryNotesLabel).text = OsLang.ui("notes", lang)
        val name = findViewById<EditText>(R.id.entryName)
        val notes = findViewById<EditText>(R.id.entryNotes)
        name.setText(existing?.title ?: "")
        notes.setText(existing?.notes ?: "")
        val dateTv = findViewById<TextView>(R.id.entryDate)
        val timeTv = findViewById<TextView>(R.id.entryTime)
        fun paint() {
            dateTv.text = EntryUi.dateLabel(this, cal)
            timeTv.text = EntryUi.timeLabel(cal)
        }
        paint()
        dateTv.setOnClickListener { EntryUi.pickDate(this, cal) { paint() } }
        timeTv.setOnClickListener { EntryUi.pickTime(this, cal) { paint() } }
        findViewById<TextView>(R.id.entryCancel).setOnClickListener { finish() }
        findViewById<TextView>(R.id.entrySave).setOnClickListener {
            val t = name.text?.toString()?.trim().orEmpty()
            if (t.isEmpty()) return@setOnClickListener
            val start = EventStore.startOfDay(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            val item = (existing ?: CalendarEvent(title = t, dayStart = start, projectId = projectId)).copy(
                title = t,
                dayStart = start,
                hour = cal.get(Calendar.HOUR_OF_DAY),
                minute = cal.get(Calendar.MINUTE),
                notes = notes.text?.toString()?.trim().orEmpty(),
                projectId = projectId
            )
            store.upsert(item)
            projects.touch(projectId)
            finish()
        }
    }

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_DAY = "dayStart"
    }
}
