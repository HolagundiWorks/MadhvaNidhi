package com.paperdesk.launcher.ui

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.PracticeCrypto
import com.paperdesk.launcher.data.PracticeExport
import com.paperdesk.launcher.data.SettingsStore

class DashboardSettingsFragment : Fragment(R.layout.page_dashboard_settings) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        PagePalette.apply(view, "settings")
        SettingsScreenBinder.bindJapaTables(view, requireContext(), SettingsStore(requireContext()))
        view.findViewById<View>(R.id.practiceExport).setOnClickListener { askPassphraseThenExport() }
    }

    override fun onResume() {
        super.onResume()
        view?.let { SettingsScreenBinder.bindJapaTables(it, requireContext(), SettingsStore(requireContext())) }
    }

    /** The export is encrypted, so the user sets a passphrase first; it is needed again to import. */
    private fun askPassphraseThenExport() {
        val ctx = requireContext()
        val pad = (16 * resources.displayMetrics.density).toInt()
        val first = passphraseField(R.string.practice_passphrase)
        val again = passphraseField(R.string.practice_passphrase_again)
        val column = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
            addView(first)
            addView(again)
        }
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.practice_export)
            .setMessage(R.string.practice_passphrase_note)
            .setView(column)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val a = first.text.toString()
                val b = again.text.toString()
                when {
                    a.length < PracticeCrypto.MIN_PASSPHRASE -> toast(R.string.practice_passphrase_short)
                    a != b -> toast(R.string.practice_passphrase_mismatch)
                    else -> runCatching { PracticeExport.share(ctx, a.toCharArray()) }
                        .onFailure { toast(R.string.practice_export_failed) }
                }
            }
            .show()
    }

    private fun passphraseField(hint: Int): EditText = EditText(requireContext()).apply {
        setHint(hint)
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
    }

    private fun toast(message: Int) = Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
}
