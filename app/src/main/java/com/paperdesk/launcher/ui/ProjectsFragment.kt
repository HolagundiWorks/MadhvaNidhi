package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.Project
import com.paperdesk.launcher.data.ProjectStore
import java.util.Locale

class ProjectsFragment : Fragment() {

    private lateinit var store: ProjectStore
    private lateinit var adapter: Adapter
    private val items = mutableListOf<Project>()
    private val locale = Locale("en", "IN")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.page_projects, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        store = ProjectStore(requireContext())
        adapter = Adapter(
            items,
            summary = { store.summary(it.id) },
            onOpen = { startActivity(EntryUi.openProject(requireContext(), it.id)) },
            onDelete = {
                store.delete(it.id)
                reload()
            }
        )
        val list = view.findViewById<RecyclerView>(R.id.projectList)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
        view.findViewById<View>(R.id.fabProject).setOnClickListener { startActivity(EntryUi.project(requireContext())) }
        view.findViewById<View>(R.id.projectCreate).setOnClickListener { startActivity(EntryUi.project(requireContext())) }
        reload()
    }

    override fun onResume() {
        super.onResume()
        if (::store.isInitialized) reload()
    }

    private fun reload() {
        items.clear()
        items.addAll(store.load())
        adapter.notifyDataSetChanged()
        val empty = items.isEmpty()
        view?.findViewById<View>(R.id.projectEmptyBox)?.visibility = if (empty) View.VISIBLE else View.GONE
        view?.findViewById<View>(R.id.projectList)?.visibility = if (empty) View.GONE else View.VISIBLE
    }

    private inner class Adapter(
        private val rows: List<Project>,
        private val summary: (Project) -> com.paperdesk.launcher.data.ProjectSummary,
        private val onOpen: (Project) -> Unit,
        private val onDelete: (Project) -> Unit
    ) : RecyclerView.Adapter<Adapter.VH>() {
        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.projName)
            val no: TextView = view.findViewById(R.id.projNo)
            val meta: TextView = view.findViewById(R.id.projMeta)
            val counts: TextView = view.findViewById(R.id.projCounts)
            val del: TextView = view.findViewById(R.id.projDel)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_project, parent, false))
        }

        override fun getItemCount(): Int = rows.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = rows[position]
            val s = summary(item)
            holder.name.text = item.name
            DeskType.primary(holder.name)
            holder.no.text = item.number.ifBlank { "Project" }
            holder.no.visibility = View.VISIBLE
            DeskType.supporting(holder.no)
            holder.meta.visibility = View.GONE
            holder.counts.text =
                "${s.pendingTodos} pending todos  ·  ${s.upcoming} schedules"
            holder.itemView.setOnClickListener { onOpen(item) }
            holder.del.setOnClickListener { onDelete(item) }
        }
    }
}
