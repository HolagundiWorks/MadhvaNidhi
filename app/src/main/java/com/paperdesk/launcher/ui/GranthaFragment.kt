package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.OsLang

class GranthaFragment : Fragment(R.layout.page_grantha_tabs) {
    private var selected = 0
    private val groupId get() = arguments?.getString("group") ?: "grantha"
    private val destinations get() = ShellHubFragment.childrenFor(groupId)

    fun selectReader(id: String) {
        view?.findViewById<TabLayout>(R.id.granthaTabs)?.getTabAt(destinations.indexOf(id))?.select()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        selected = savedInstanceState?.getInt("tab") ?: 0
        val lang = OsLang.get(requireContext())
        view.findViewById<TextView>(R.id.hubTitle).text = OsLang.ui(groupId, lang)
        val tabs = view.findViewById<TabLayout>(R.id.granthaTabs)
        destinations.forEachIndexed { index, id ->
            tabs.addTab(tabs.newTab().setText(OsLang.ui(id, lang)), index == selected)
        }
        CarbonTabs.equalizeLeft(tabs)
        fun show(index: Int) {
            selected = index
            val page = PageFactory.create(destinations[index])
            childFragmentManager.beginTransaction().replace(R.id.granthaPanelHost, page).commitNow()
        }
        if (childFragmentManager.findFragmentById(R.id.granthaPanelHost) == null) show(selected)
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = show(tab.position)
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("tab", selected)
    }
}
