package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.paperdesk.launcher.R

class UiSettingsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ScreenPages.wrap(inflater.inflate(R.layout.activity_app_settings, container, false))

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        PagePalette.apply(view, "settings")
        SettingsScreenBinder.bind(view, requireContext(), showBack = false)
        view.findViewById<View>(R.id.settingsProfileCard).visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        view?.let { SettingsScreenBinder.refreshProfileSummary(it, requireContext()) }
    }
}
