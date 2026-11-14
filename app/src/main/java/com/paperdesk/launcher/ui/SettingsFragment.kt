package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.paperdesk.launcher.R

class SettingsFragment : Fragment(R.layout.page_settings_tabs) {
    private var selected = 0
    fun openUsers() {
        view?.findViewById<TabLayout>(R.id.settingsTabs)?.getTabAt(2)?.select()
    }

    /** Back from Users/Backup/Legal returns to UI Settings before leaving the hub. */
    fun consumeBack(): Boolean {
        val tabs = view?.findViewById<TabLayout>(R.id.settingsTabs) ?: return false
        if (selected == 0) return false
        tabs.getTabAt(0)?.select()
        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        selected = savedInstanceState?.getInt("tab") ?: 0
        val tabs = view.findViewById<TabLayout>(R.id.settingsTabs)
        listOf(
            "UI Settings",
            "Dashboard",
            "Users",
            "Backup",
            getString(R.string.settings_tab_legal)
        ).forEachIndexed { index, label ->
            tabs.addTab(tabs.newTab().setText(label), index == selected)
        }
        MdTabs.equalizeLeft(tabs)
        fun show(index: Int) {
            selected = index
            val page = when (index) {
                1 -> DashboardSettingsFragment()
                2 -> UserProfileFragment()
                3 -> ProfileBackupFragment()
                4 -> LegalSettingsFragment()
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
