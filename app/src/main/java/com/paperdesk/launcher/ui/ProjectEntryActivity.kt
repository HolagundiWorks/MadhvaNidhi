package com.paperdesk.launcher.ui

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.Project
import com.paperdesk.launcher.data.ProjectStore

class ProjectEntryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (com.paperdesk.launcher.DeskLock.bounce(this)) return
        setContentView(R.layout.activity_entry_project)
        EntryUi.prepare(this)
        val store = ProjectStore(this)
        val id = intent.getStringExtra(EXTRA_ID)
        val existing = id?.let { store.get(it) }
        findViewById<TextView>(R.id.entryTitle).text =
            getString(if (existing == null) R.string.new_project else R.string.edit_project)
        val name = findViewById<EditText>(R.id.entryName)
        val number = findViewById<EditText>(R.id.entryNumber)
        name.setText(existing?.name ?: "")
        number.setText(existing?.number ?: "")
        findViewById<TextView>(R.id.entryCancel).setOnClickListener { finish() }
        findViewById<TextView>(R.id.entrySave).setOnClickListener {
            val t = name.text?.toString()?.trim().orEmpty()
            if (t.isEmpty()) return@setOnClickListener
            val item = (existing ?: Project(name = t)).copy(
                name = t,
                number = number.text?.toString()?.trim().orEmpty(),
                updatedAt = System.currentTimeMillis()
            )
            store.upsert(item)
            runCatching { com.paperdesk.launcher.bridge.OsDataService.projectFolder(item.name) }
            if (existing == null) {
                startActivity(EntryUi.openProject(this, item.id))
            }
            finish()
        }
    }

    companion object {
        const val EXTRA_ID = "id"
    }
}
