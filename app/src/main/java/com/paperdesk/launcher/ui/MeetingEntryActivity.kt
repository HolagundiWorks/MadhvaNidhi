package com.paperdesk.launcher.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.DeskPhoto
import com.paperdesk.launcher.data.MeetingItem
import com.paperdesk.launcher.data.MeetingStore
import com.paperdesk.launcher.data.ProjectStore
import java.util.Calendar
import java.util.UUID

class MeetingEntryActivity : AppCompatActivity() {

    private val cal = Calendar.getInstance()
    private lateinit var itemId: String
    private lateinit var pid: String
    private var photoPath = ""
    private lateinit var dest: java.io.File

    private val askCam = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) shoot()
    }

    private val takePic = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok && dest.exists() && dest.length() > 0L) {
            photoPath = dest.absolutePath
            DeskPhoto.bind(findViewById(R.id.entryPhoto), photoPath, 720)
        } else {
            DeskPhoto.delete(dest.absolutePath)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (com.paperdesk.launcher.DeskLock.bounce(this)) return
        setContentView(R.layout.activity_entry_meeting)
        EntryUi.prepare(this)
        val store = MeetingStore(this)
        val projects = ProjectStore(this)
        val extraPid = intent.getStringExtra(EntryUi.EXTRA_PROJECT_ID).orEmpty()
        val id = intent.getStringExtra(EXTRA_ID)
        val existing = id?.let { store.get(it) }
        pid = existing?.projectId ?: extraPid
        itemId = existing?.id ?: UUID.randomUUID().toString()
        photoPath = existing?.photo.orEmpty()
        dest = DeskPhoto.file(this, pid, itemId)
        val pname = projects.nameOf(pid)
        findViewById<TextView>(R.id.entryTitle).text =
            getString(if (existing == null) R.string.new_minutes else R.string.edit_minutes) +
                if (pname.isBlank()) "" else "  -  $pname"
        val title = findViewById<EditText>(R.id.entryName)
        val attendees = findViewById<EditText>(R.id.entryAttendees)
        val body = findViewById<EditText>(R.id.entryNotes)
        title.setText(existing?.title ?: "")
        attendees.setText(existing?.attendees ?: "")
        body.setText(existing?.body ?: "")
        if (existing != null) cal.timeInMillis = existing.dayStart
        val dateTv = findViewById<TextView>(R.id.entryDate)
        fun paintDate() {
            dateTv.text = EntryUi.dateLabel(cal)
        }
        paintDate()
        dateTv.setOnClickListener { EntryUi.pickDate(this, cal) { paintDate() } }
        DeskPhoto.bind(findViewById(R.id.entryPhoto), photoPath, 720)
        findViewById<TextView>(R.id.entryPhotoBtn).setOnClickListener {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                shoot()
            } else {
                askCam.launch(Manifest.permission.CAMERA)
            }
        }
        findViewById<TextView>(R.id.entryCancel).setOnClickListener { finish() }
        findViewById<TextView>(R.id.entrySave).setOnClickListener {
            val t = title.text?.toString()?.trim().orEmpty()
            val a = attendees.text?.toString()?.trim().orEmpty()
            val b = body.text?.toString()?.trim().orEmpty()
            if (t.isEmpty() && a.isEmpty() && b.isEmpty() && photoPath.isEmpty()) return@setOnClickListener
            val start = com.paperdesk.launcher.data.EventStore.startOfDay(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            val label = t.ifBlank { "MINUTES  ·  ${EntryUi.dateLabel(cal)}" }
            val item = (existing ?: MeetingItem(id = itemId, projectId = pid)).copy(
                id = itemId,
                projectId = pid,
                title = label,
                attendees = a,
                body = b,
                photo = photoPath,
                dayStart = start,
                updatedAt = System.currentTimeMillis()
            )
            store.upsert(item)
            projects.touch(pid)
            finish()
        }
    }

    private fun shoot() {
        dest = DeskPhoto.file(this, pid, itemId)
        val uri = runCatching { DeskPhoto.uri(this, dest) }.getOrNull() ?: return
        takePic.launch(uri)
    }

    companion object {
        const val EXTRA_ID = "id"
    }
}
