package com.paperdesk.launcher.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.MeasureItem
import com.paperdesk.launcher.data.MeasureStore
import com.paperdesk.launcher.data.ProjectStore

class MeasureEntryActivity : AppCompatActivity() {

    private lateinit var store: MeasureStore
    private lateinit var projects: ProjectStore
    private var item = MeasureItem()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (com.paperdesk.launcher.DeskLock.bounce(this)) return
        setContentView(R.layout.activity_entry_measure)
        EntryUi.prepare(this)
        store = MeasureStore(this)
        projects = ProjectStore(this)
        val id = intent.getStringExtra(EXTRA_ID)
        val extraPid = intent.getStringExtra(EntryUi.EXTRA_PROJECT_ID).orEmpty()
        item = id?.let { store.get(it) } ?: MeasureItem(projectId = extraPid)
        if (item.projectId.isBlank() && extraPid.isNotBlank()) {
            item = item.copy(projectId = extraPid)
        }
        bind()
        watch(R.id.mDesc) { item = item.copy(description = it); paint() }
        watch(R.id.mLen) { item = item.copy(length = it); autoKind(); paint() }
        watch(R.id.mBrd) { item = item.copy(breadth = it); autoKind(); paint() }
        watch(R.id.mHt) { item = item.copy(height = it); autoKind(); paint() }
        findViewById<TextView>(R.id.mUom).setOnClickListener {
            item = item.copy(uom = item.uom.next())
            paint()
        }
        findViewById<TextView>(R.id.mKind).setOnClickListener {
            item = item.copy(kind = item.kind.next(), autoKind = false)
            paint()
        }
        findViewById<TextView>(R.id.entryCancel).setOnClickListener { finish() }
        findViewById<TextView>(R.id.entrySave).setOnClickListener {
            persist(); finish()
        }
        findViewById<TextView>(R.id.entrySaveNext).setOnClickListener {
            persist()
            item = MeasureItem(uom = item.uom, projectId = item.projectId)
            findViewById<EditText>(R.id.mDesc).setText("")
            findViewById<EditText>(R.id.mLen).setText("")
            findViewById<EditText>(R.id.mBrd).setText("")
            findViewById<EditText>(R.id.mHt).setText("")
            paint()
            findViewById<EditText>(R.id.mDesc).requestFocus()
        }
    }

    private fun autoKind() {
        if (item.autoKind) item = item.copy(kind = item.inferredKind())
    }

    private fun persist() {
        autoKind()
        if (item.description.isBlank() && item.length.isBlank() && item.breadth.isBlank() && item.height.isBlank()) {
            return
        }
        if (item.projectId.isBlank()) return
        store.upsert(item)
        projects.touch(item.projectId)
    }

    private fun bind() {
        findViewById<EditText>(R.id.mDesc).setText(item.description)
        findViewById<EditText>(R.id.mLen).setText(item.length)
        findViewById<EditText>(R.id.mBrd).setText(item.breadth)
        findViewById<EditText>(R.id.mHt).setText(item.height)
        paint()
    }

    private fun paint() {
        val scoped = if (item.projectId.isBlank()) store.load() else store.forProject(item.projectId)
        val n = scoped.indexOfFirst { it.id == item.id }.let { if (it < 0) scoped.size + 1 else it + 1 }
        findViewById<TextView>(R.id.mItemNo).text = "Item No.  $n"
        findViewById<TextView>(R.id.mUom).text = item.uom.label
        findViewById<TextView>(R.id.mKind).text = item.effectiveKind().fullLabel
        findViewById<TextView>(R.id.mQty).text = item.qtyLabel()
    }

    private fun watch(id: Int, set: (String) -> Unit) {
        findViewById<EditText>(id).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { set(s?.toString().orEmpty()) }
        })
    }

    companion object {
        const val EXTRA_ID = "id"
    }
}
