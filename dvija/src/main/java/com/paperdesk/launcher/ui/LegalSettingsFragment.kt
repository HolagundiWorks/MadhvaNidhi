package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.paperdesk.launcher.R

/** Settings → Legal: developer, privacy, and licence declarations. */
class LegalSettingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.page_settings_legal, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val version = runCatching {
            requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
        }.getOrNull() ?: "1.0.0"
        view.findViewById<TextView>(R.id.legalVersion).text =
            getString(R.string.settings_version, version)
    }
}
