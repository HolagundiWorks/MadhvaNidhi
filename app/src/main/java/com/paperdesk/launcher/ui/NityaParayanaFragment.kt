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
        ScreenPages.wrap(inflater.inflate(R.layout.page_nitya_parayana, container, false))

    override fun onViewCreated(view: View, state: Bundle?) {
        PagePalette.apply(view, "nitya")
        val sections = NityaParayanaStore.load(requireContext())
        val prefs = requireContext().getSharedPreferences("madhvanidhi_nitya", 0)
        val selected = prefs.getInt("section", 0).coerceIn(0, (sections.size - 1).coerceAtLeast(0))
        fun show(index: Int) {
            view.findViewById<TextView>(R.id.nityaReading).apply {
                text = sections.getOrNull(index)?.body.orEmpty()
                DeskType.reading(this)
            }
            prefs.edit().putInt("section", index).apply()
            ScreenPages.first(view)
        }
        if (sections.isNotEmpty()) {
            MaterialChoices.bind(view.findViewById(R.id.nityaSection), sections.map { it.title }, selected) { show(it) }
            show(selected)
        }
    }
}
