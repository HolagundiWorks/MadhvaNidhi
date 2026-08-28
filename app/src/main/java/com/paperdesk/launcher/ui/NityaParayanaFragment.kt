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
import com.paperdesk.launcher.data.NityaParayanaStore

class NityaParayanaFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.page_nitya_parayana, container, false)

    override fun onViewCreated(view: View, state: Bundle?) {
        PagePalette.apply(view, "nitya")
        val sections = NityaParayanaStore.load(requireContext())
        view.findViewById<RecyclerView>(R.id.nityaList).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = SectionAdapter(sections)
            setHasFixedSize(false)
        }
    }

    private class SectionAdapter(
        private val data: List<NityaParayanaStore.Section>
    ) : RecyclerView.Adapter<SectionViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder =
            SectionViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_nitya_parayana, parent, false)
            )

        override fun getItemCount(): Int = data.size

        override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
            val section = data[position]
            holder.title.text = section.title
            holder.body.text = section.body
            DeskType.apply(holder.title, DeskType.SEMIBOLD)
            DeskType.apply(holder.body, DeskType.REGULAR)
        }
    }

    private class SectionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.nityaPageTitle)
        val body: TextView = view.findViewById(R.id.nityaPageBody)
    }
}
