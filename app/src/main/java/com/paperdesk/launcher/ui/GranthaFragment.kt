package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.paperdesk.launcher.R

class GranthaFragment : Fragment(R.layout.page_grantha_tabs) {
    private var selected = 0
    private val destinations get() = ShellHubFragment.childrenFor(arguments?.getString("group") ?: "grantha")

    fun selectReader(id: String) {
        view?.findViewById<TabLayout>(R.id.granthaTabs)?.getTabAt(destinations.indexOf(id))?.select()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        selected = savedInstanceState?.getInt("tab") ?: 0
        val tabs = view.findViewById<TabLayout>(R.id.granthaTabs)
        val lang = com.paperdesk.launcher.data.OsLang.get(requireContext())
        destinations.forEachIndexed { index, id ->
            tabs.addTab(tabs.newTab().setText(com.paperdesk.launcher.data.OsLang.ui(id, lang)), index == selected)
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
