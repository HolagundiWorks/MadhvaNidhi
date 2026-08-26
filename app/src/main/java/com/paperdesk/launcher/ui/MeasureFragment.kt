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
import com.paperdesk.launcher.data.MeasureItem
import com.paperdesk.launcher.data.MeasureStore
import com.paperdesk.launcher.data.ProjectStore

class MeasureFragment : Fragment() {

    private lateinit var store: MeasureStore
    private val items = mutableListOf<MeasureItem>()
    private lateinit var adapter: MeasureAdapter
    private var projectId: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.page_measure, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        store = MeasureStore(requireContext())
        projectId = arguments?.getString(ARG_PROJECT).orEmpty()
        adapter = MeasureAdapter(
            items,
            onEdit = {
                if (projectId.isNotBlank()) {
                    startActivity(EntryUi.measure(requireContext(), it.id, projectId))
                }
            },
            onDelete = { item ->
                store.save(store.load().filter { it.id != item.id })
                if (projectId.isNotBlank()) ProjectStore(requireContext()).touch(projectId)
                reload()
            }
        )
        val list = view.findViewById<RecyclerView>(R.id.measureList)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
        val add = {
            if (projectId.isNotBlank()) {
                startActivity(EntryUi.measure(requireContext(), projectId = projectId))
            }
        }
        view.findViewById<View>(R.id.fabMeasure).setOnClickListener { add() }
        view.findViewById<View>(R.id.measureCreate)?.setOnClickListener { add() }
        reload()
    }

    override fun onResume() {
        super.onResume()
        if (::store.isInitialized) reload()
    }

    private fun reload() {
        items.clear()
        if (projectId.isNotBlank()) items.addAll(store.forProject(projectId))
        if (::adapter.isInitialized) adapter.notifyDataSetChanged()
        paintFooter()
    }

    private fun paintFooter() {
        val empty = items.isEmpty()
        view?.findViewById<View>(R.id.measureEmptyBox)?.visibility = if (empty) View.VISIBLE else View.GONE
        view?.findViewById<View>(R.id.measureList)?.visibility = if (empty) View.GONE else View.VISIBLE
        val total = view?.findViewById<TextView>(R.id.measureTotal) ?: return
        val text = MeasureItem.totals(items)
        total.text = text
        total.visibility = if (text.isEmpty()) View.GONE else View.VISIBLE
    }

    companion object {
        const val ARG_PROJECT = "projectId"
        fun newInstance(projectId: String) = MeasureFragment().apply {
            arguments = Bundle().apply { putString(ARG_PROJECT, projectId) }
        }
    }

    private class MeasureAdapter(
        private val items: MutableList<MeasureItem>,
        private val onEdit: (MeasureItem) -> Unit,
        private val onDelete: (MeasureItem) -> Unit
    ) : RecyclerView.Adapter<MeasureAdapter.VH>() {

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val no: TextView = view.findViewById(R.id.mNo)
            val desc: TextView = view.findViewById(R.id.mDesc)
            val len: TextView = view.findViewById(R.id.mLen)
            val brd: TextView = view.findViewById(R.id.mBrd)
            val ht: TextView = view.findViewById(R.id.mHt)
            val uom: TextView = view.findViewById(R.id.mUom)
            val kind: TextView = view.findViewById(R.id.mKind)
            val qty: TextView = view.findViewById(R.id.mQty)
            val del: TextView = view.findViewById(R.id.mDel)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_measure, parent, false))
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.no.text = String.format("%02d", position + 1)
            holder.desc.text = item.description.ifBlank { "-" }
            holder.len.text = item.length.ifBlank { "-" }
            holder.brd.text = item.breadth.ifBlank { "-" }
            holder.ht.text = item.height.ifBlank { "-" }
            holder.uom.text = item.uom.label
            holder.kind.text = item.effectiveKind().label
            holder.qty.text = item.qtyLabel()
            DeskType.apply(holder.qty, DeskType.SEMIBOLD)
            holder.qty.setTextColor(holder.itemView.context.getColor(R.color.white))
            DeskType.supporting(holder.uom)
            DeskType.meta(holder.kind)
            holder.itemView.setOnClickListener { onEdit(item) }
            holder.del.setOnClickListener { onDelete(item) }
        }
    }
}
