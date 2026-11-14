package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.NityaParayanaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NityaParayanaFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.page_nitya_parayana, container, false)

    override fun onViewCreated(view: View, state: Bundle?) {
        PagePalette.apply(view, "nitya")
        val sectionAdapter = SectionAdapter()
        view.findViewById<RecyclerView>(R.id.nityaList).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = sectionAdapter
            setHasFixedSize(false)
        }
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val sections = withContext(Dispatchers.IO) { NityaParayanaStore.load(appContext) }
            if (view != null) sectionAdapter.submit(sections)
        }
    }

    private class SectionAdapter : RecyclerView.Adapter<SectionViewHolder>() {
        private var data: List<NityaParayanaStore.Section> = emptyList()

        fun submit(items: List<NityaParayanaStore.Section>) {
            data = items
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
            val holder = SectionViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_nitya_parayana, parent, false)
            )
            DeskType.apply(holder.title, DeskType.SEMIBOLD)
            DeskType.apply(holder.body, DeskType.REGULAR)
            return holder
        }

        override fun getItemCount(): Int = data.size

        override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
            val section = data[position]
            holder.title.text = section.title
            holder.body.text = section.body
        }
    }

    private class SectionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.nityaPageTitle)
        val body: TextView = view.findViewById(R.id.nityaPageBody)
    }
}
