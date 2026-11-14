package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.SettingsStore

class DashboardSettingsFragment : Fragment(R.layout.page_dashboard_settings) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        PagePalette.apply(view, "settings")
        SettingsScreenBinder.bindJapaTables(view, requireContext(), SettingsStore(requireContext()))
    }

    override fun onResume() {
        super.onResume()
        view?.let { SettingsScreenBinder.bindJapaTables(it, requireContext(), SettingsStore(requireContext())) }
    }
}
