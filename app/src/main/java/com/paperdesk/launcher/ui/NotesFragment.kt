package com.paperdesk.launcher.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.NoteItem
import com.paperdesk.launcher.data.NoteStore
import com.paperdesk.launcher.data.ProjectStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotesFragment : Fragment() {

    private lateinit var store: NoteStore
    private lateinit var projects: ProjectStore
    private lateinit var projectId: String
    private val items = mutableListOf<NoteItem>()
    private lateinit var adapter: Adapter
    private var query = ""
    private val locale = Locale("en", "IN")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.page_notes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        store = NoteStore(requireContext())
        projects = ProjectStore(requireContext())
        projectId = arguments?.getString(ProjectActivity.EXTRA_ID).orEmpty()
        adapter = Adapter(
            items,
            showProject = projectId.isBlank(),
            nameOf = { projects.nameOf(it) },
            onEdit = { startActivity(EntryUi.note(requireContext(), it.projectId.ifBlank { projectId }, it.id)) },
            onOpen = { item ->
                if (item.projectId.isNotBlank()) {
                    startActivity(EntryUi.openProject(requireContext(), item.projectId, ProjectActivity.NOTES))
                }
            },
            onDelete = {
                store.delete(it.id)
                if (it.projectId.isNotBlank()) projects.touch(it.projectId)
                reload()
            }
        )
        val list = view.findViewById<RecyclerView>(R.id.noteList)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
        view.findViewById<View>(R.id.fabNote).setOnClickListener { createNote() }
        view.findViewById<View>(R.id.noteCreate).setOnClickListener { createNote() }
        view.findViewById<View>(R.id.notePhoto).setOnClickListener { takePhoto() }
        view.findViewById<View>(R.id.notePhotoEmpty).setOnClickListener { takePhoto() }
        view.findViewById<EditText>(R.id.noteSearch).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                query = s?.toString().orEmpty()
                reload()
            }
        })
        reload()
    }

    override fun onResume() {
        super.onResume()
        if (::store.isInitialized) reload()
    }

    private fun takePhoto() {
        if (projectId.isNotBlank()) {
            startActivity(EntryUi.photoNote(requireContext(), projectId))
            return
        }
        val list = projects.load()
        if (list.isEmpty()) {
            startActivity(EntryUi.project(requireContext()))
            return
        }
        val labels = list.map { it.name.uppercase(locale) }
        android.app.AlertDialog.Builder(requireContext(), R.style.Theme_PaperDesk_Dialog)
            .setItems(labels.toTypedArray()) { _, i ->
                startActivity(EntryUi.photoNote(requireContext(), list[i].id))
            }
            .show()
    }

    private fun createNote() {
        if (projectId.isNotBlank()) {
            startActivity(EntryUi.note(requireContext(), projectId))
            return
        }
        val list = projects.load()
        if (list.isEmpty()) {
            startActivity(EntryUi.note(requireContext(), ""))
            return
        }
        val labels = mutableListOf("NO PROJECT")
        labels.addAll(list.map { it.name.uppercase(locale) })
        android.app.AlertDialog.Builder(requireContext(), R.style.Theme_PaperDesk_Dialog)
            .setItems(labels.toTypedArray()) { _, i ->
                val pid = if (i == 0) "" else list[i - 1].id
                startActivity(EntryUi.note(requireContext(), pid))
            }
            .show()
    }

    private fun reload() {
        val q = query.trim().lowercase(locale)
        val src = (if (projectId.isBlank()) store.load().sortedByDescending { it.updatedAt } else store.forProject(projectId))
            .filter {
                val pname = projects.nameOf(it.projectId)
                q.isEmpty() ||
                    it.title.lowercase(locale).contains(q) ||
                    it.body.lowercase(locale).contains(q) ||
                    pname.lowercase(locale).contains(q)
            }
        items.clear()
        items.addAll(src)
        adapter.notifyDataSetChanged()
        val empty = items.isEmpty() && q.isEmpty()
        view?.findViewById<View>(R.id.noteEmptyBox)?.visibility = if (empty) View.VISIBLE else View.GONE
        view?.findViewById<View>(R.id.noteList)?.visibility = if (empty) View.GONE else View.VISIBLE
    }

    private inner class Adapter(
        private val rows: List<NoteItem>,
        private val showProject: Boolean,
        private val nameOf: (String) -> String,
        private val onEdit: (NoteItem) -> Unit,
        private val onOpen: (NoteItem) -> Unit,
        private val onDelete: (NoteItem) -> Unit
    ) : RecyclerView.Adapter<Adapter.VH>() {
        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val thumb: android.widget.ImageView = view.findViewById(R.id.noteThumb)
            val title: TextView = view.findViewById(R.id.noteTitle)
            val body: TextView = view.findViewById(R.id.noteBody)
            val project: TextView = view.findViewById(R.id.noteProject)
            val whenTv: TextView = view.findViewById(R.id.noteWhen)
            val open: TextView = view.findViewById(R.id.noteOpen)
            val del: TextView = view.findViewById(R.id.noteDel)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false))
        }

        override fun getItemCount(): Int = rows.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = rows[position]
            holder.title.text = item.title.ifBlank { item.body.ifBlank { getString(R.string.photo) } }
            holder.body.text = if (item.title.isBlank()) "" else item.body
            holder.body.visibility = if (item.title.isBlank() || item.body.isBlank()) View.GONE else View.VISIBLE
            com.paperdesk.launcher.data.DeskPhoto.bind(holder.thumb, item.photo, 160)
            val pname = nameOf(item.projectId)
            val tagged = showProject && pname.isNotBlank()
            holder.project.text = "[$pname]"
            holder.project.visibility = if (tagged) View.VISIBLE else View.GONE
            holder.open.visibility = if (tagged) View.VISIBLE else View.GONE
            holder.whenTv.text = SimpleDateFormat("d MMM yyyy  HH:mm", locale).format(Date(item.updatedAt))
            holder.itemView.setOnClickListener { onEdit(item) }
            holder.open.setOnClickListener { onOpen(item) }
            holder.del.setOnClickListener { onDelete(item) }
        }
    }

    companion object {
        fun newInstance(projectId: String) = NotesFragment().apply {
            arguments = Bundle().apply { putString(ProjectActivity.EXTRA_ID, projectId) }
        }
    }
}
