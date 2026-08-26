package com.paperdesk.launcher.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.DeskPhoto
import com.paperdesk.launcher.data.MeetingItem
import com.paperdesk.launcher.data.MeetingStore
import com.paperdesk.launcher.data.ProjectStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MinutesFragment : Fragment() {

    private lateinit var store: MeetingStore
    private lateinit var projects: ProjectStore
    private lateinit var projectId: String
    private val items = mutableListOf<MeetingItem>()
    private lateinit var adapter: Adapter
    private var query = ""
    private val locale = Locale("en", "IN")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.page_minutes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        store = MeetingStore(requireContext())
        projects = ProjectStore(requireContext())
        projectId = arguments?.getString(ProjectActivity.EXTRA_ID).orEmpty()
        adapter = Adapter(
            items,
            onEdit = { startActivity(EntryUi.minutes(requireContext(), projectId, it.id)) },
            onDelete = {
                store.delete(it.id)
                if (it.projectId.isNotBlank()) projects.touch(it.projectId)
                reload()
            }
        )
        val list = view.findViewById<RecyclerView>(R.id.minutesList)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
        view.findViewById<View>(R.id.fabMinutes).setOnClickListener { create() }
        view.findViewById<View>(R.id.minutesCreate).setOnClickListener { create() }
        view.findViewById<EditText>(R.id.minutesSearch).addTextChangedListener(object : TextWatcher {
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

    private fun create() {
        startActivity(EntryUi.minutes(requireContext(), projectId))
    }

    private fun reload() {
        val q = query.trim().lowercase(locale)
        val src = store.forProject(projectId).filter {
            q.isEmpty() ||
                it.title.lowercase(locale).contains(q) ||
                it.attendees.lowercase(locale).contains(q) ||
                it.body.lowercase(locale).contains(q)
        }
        items.clear()
        items.addAll(src)
        adapter.notifyDataSetChanged()
        val empty = items.isEmpty() && q.isEmpty()
        view?.findViewById<View>(R.id.minutesEmptyBox)?.visibility = if (empty) View.VISIBLE else View.GONE
        view?.findViewById<View>(R.id.minutesList)?.visibility = if (empty) View.GONE else View.VISIBLE
    }

    private inner class Adapter(
        private val rows: List<MeetingItem>,
        private val onEdit: (MeetingItem) -> Unit,
        private val onDelete: (MeetingItem) -> Unit
    ) : RecyclerView.Adapter<Adapter.VH>() {
        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val thumb: ImageView = view.findViewById(R.id.minutesThumb)
            val title: TextView = view.findViewById(R.id.minutesTitle)
            val body: TextView = view.findViewById(R.id.minutesBody)
            val whenTv: TextView = view.findViewById(R.id.minutesWhen)
            val del: TextView = view.findViewById(R.id.minutesDel)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_minutes, parent, false))
        }

        override fun getItemCount(): Int = rows.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = rows[position]
            holder.title.text = item.title.ifBlank { item.body }
            val extra = listOf(item.attendees, item.body).filter { it.isNotBlank() && it != holder.title.text }
            holder.body.text = extra.firstOrNull().orEmpty()
            holder.body.visibility = if (holder.body.text.isBlank()) View.GONE else View.VISIBLE
            holder.whenTv.text = SimpleDateFormat("EEE  d MMM yyyy", locale).format(Date(item.dayStart))
            DeskPhoto.bind(holder.thumb, item.photo, 160)
            holder.itemView.setOnClickListener { onEdit(item) }
            holder.del.setOnClickListener { onDelete(item) }
        }
    }

    companion object {
        fun newInstance(projectId: String) = MinutesFragment().apply {
            arguments = Bundle().apply { putString(ProjectActivity.EXTRA_ID, projectId) }
        }
    }
}
