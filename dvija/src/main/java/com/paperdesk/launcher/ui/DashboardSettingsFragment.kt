package com.paperdesk.launcher.ui

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.PracticeImport
import com.paperdesk.launcher.data.SettingsStore

class DashboardSettingsFragment : Fragment(R.layout.page_dashboard_settings) {

    private val pickExport = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        when (val outcome = PracticeImport.importFrom(requireContext(), uri)) {
            is PracticeImport.Outcome.Imported -> message(getString(R.string.dvija_import_done, outcome.records))
            is PracticeImport.Outcome.Refused -> message(getString(R.string.dvija_import_refused, outcome.reason))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        PagePalette.apply(view, "settings")
        SettingsScreenBinder.bindJapaTables(view, requireContext(), SettingsStore(requireContext()))
        view.findViewById<View>(R.id.practiceImport).setOnClickListener {
            pickExport.launch(arrayOf("*/*"))
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let { SettingsScreenBinder.bindJapaTables(it, requireContext(), SettingsStore(requireContext())) }
    }

    private fun message(text: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dvija_import_title)
            .setMessage(text)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
