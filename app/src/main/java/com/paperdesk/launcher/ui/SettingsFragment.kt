package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.paperdesk.launcher.R

class SettingsFragment : Fragment(R.layout.page_settings_tabs) {
    private var selected = 0
    fun openUsers() {
        view?.findViewById<TabLayout>(R.id.settingsTabs)?.getTabAt(1)?.select()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        selected = savedInstanceState?.getInt("tab") ?: 0
        val tabs = view.findViewById<TabLayout>(R.id.settingsTabs)
        listOf("UI Settings", "Users", "Backup").forEachIndexed { index, label ->
            tabs.addTab(tabs.newTab().setText(label), index == selected)
        }
        CarbonTabs.equalizeLeft(tabs)
        fun show(index: Int) {
            selected = index
            val page = when (index) {
                1 -> UserProfileFragment()
                2 -> ProfileBackupFragment()
                else -> UiSettingsFragment()
            }
            childFragmentManager.beginTransaction().replace(R.id.settingsPanelHost, page).commitNow()
        }
        if (childFragmentManager.findFragmentById(R.id.settingsPanelHost) == null) show(selected)
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
