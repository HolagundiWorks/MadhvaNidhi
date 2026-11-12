package com.paperdesk.launcher.ui

import android.app.Dialog
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.paperdesk.launcher.R
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Standard Material dialog and text field, with strict date validation. */
class DateEntryDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val content = layoutInflater.inflate(R.layout.dialog_date_entry, null)
        val field = content.findViewById<TextInputLayout>(R.id.dateEntryField)
        val edit = content.findViewById<TextInputEditText>(R.id.dateEntryValue)
        edit.setText(requireArguments().getString("date"))
        val dialog = MaterialAlertDialogBuilder(requireContext()).apply {
            if (resources.configuration.fontScale > 2.5f) {
                setBackgroundInsetTop(0); setBackgroundInsetBottom(0)
            }
        }
            .setView(content).setPositiveButton("ಸರಿ", null).setNegativeButton("ರದ್ದು", null).create()
        dialog.setOnShowListener {
            fun submit() {
                val value = edit.text.toString().trim()
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                    isLenient = false
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val valid = value.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) &&
                    runCatching { format.format(requireNotNull(format.parse(value))) == value }.getOrDefault(false)
                (requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(edit.windowToken, 0)
                if (valid) {
                    parentFragmentManager.setFragmentResult("panchangaDate", Bundle().apply { putString("date", value) })
                    dismiss()
                } else field.error = "ದಿನಾಂಕ ಸರಿಯಿಲ್ಲ"
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { submit() }
            edit.setOnEditorActionListener { _, _, _ -> submit(); true }
        }
        return dialog
    }

    override fun onStart() {
        super.onStart()
        val density = resources.displayMetrics.density
        val parent = requireActivity().findViewById<android.view.View>(R.id.materialContent)
        val available = parent?.let { it.width - it.paddingLeft - it.paddingRight } ?: resources.displayMetrics.widthPixels
        val width = minOf(available - resources.getDimensionPixelSize(R.dimen.carbon_spacing_05) * 2, (560 * density).toInt())
        requireDialog().window?.apply {
            setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
            setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        }
    }

    companion object {
        fun create(date: String) = DateEntryDialog().apply { arguments = Bundle().apply { putString("date", date) } }
    }
}
