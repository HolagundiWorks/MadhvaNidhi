package com.paperdesk.launcher.ui

import android.app.TimePickerDialog
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.paperdesk.launcher.R
import com.paperdesk.launcher.alarm.AlarmItem
import com.paperdesk.launcher.alarm.AlarmScheduler
import com.paperdesk.launcher.alarm.AlarmStore
import com.paperdesk.launcher.data.PracticeCrypto
import com.paperdesk.launcher.data.PracticeImport
import com.paperdesk.launcher.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardSettingsFragment : Fragment(R.layout.page_dashboard_settings) {

    private val pickExport = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) askPassphraseThenImport(uri)
    }

    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        PagePalette.apply(view, "settings")
        SettingsScreenBinder.bindJapaTables(view, requireContext(), SettingsStore(requireContext()))
        view.findViewById<MaterialButton>(R.id.reminderAdd).setOnClickListener { addReminder() }
        bindReminders(view)
        view.findViewById<View>(R.id.practiceImport).setOnClickListener {
            pickExport.launch(arrayOf("*/*"))
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let {
            SettingsScreenBinder.bindJapaTables(it, requireContext(), SettingsStore(requireContext()))
            bindReminders(it)
        }
    }

    /** Lists the personal reminders. Tapping one offers to remove it. */
    private fun bindReminders(view: View) {
        val ctx = requireContext()
        val list = view.findViewById<LinearLayout>(R.id.reminderList)
        list.removeAllViews()
        val items = AlarmStore(ctx).load().sortedWith(compareBy({ it.hour }, { it.minute }))
        if (items.isEmpty()) {
            list.addView(TextView(ctx).apply { setText(R.string.reminder_none) })
            return
        }
        for (item in items) {
            list.addView(TextView(ctx).apply {
                text = "${item.timeLabel()}  ·  ${item.daysLabel()}"
                textSize = 18f
                setPadding(0, 12, 0, 12)
                setOnClickListener { confirmRemove(item) }
            })
        }
    }

    private fun addReminder() {
        val ctx = requireContext()
        if (Build.VERSION.SDK_INT >= 33) askNotifications.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        val now = java.util.Calendar.getInstance()
        TimePickerDialog(ctx, { _, hour, minute ->
            val store = AlarmStore(ctx)
            val item = AlarmItem(hour = hour, minute = minute)
            store.save(store.load() + item)
            AlarmScheduler.schedule(ctx, item)
            view?.let { bindReminders(it) }
        }, now.get(java.util.Calendar.HOUR_OF_DAY), now.get(java.util.Calendar.MINUTE), true).show()
    }

    private fun confirmRemove(item: AlarmItem) {
        val ctx = requireContext()
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.reminder_title)
            .setMessage(getString(R.string.reminder_remove_question, item.timeLabel()))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.reminder_remove) { _, _ ->
                val store = AlarmStore(ctx)
                AlarmScheduler.cancel(ctx, item.id)
                store.save(store.load().filter { it.id != item.id })
                view?.let { bindReminders(it) }
            }
            .show()
    }

    /** The file is encrypted: read it, then ask for the passphrase that was set when it was exported. */
    private fun askPassphraseThenImport(uri: Uri) {
        val ctx = requireContext()
        val pad = (16 * resources.displayMetrics.density).toInt()
        val field = EditText(ctx).apply {
            setHint(R.string.practice_passphrase)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val column = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
            addView(field)
        }
        MaterialAlertDialogBuilder(ctx)
            .setTitle(R.string.dvija_import_title)
            .setMessage(R.string.practice_import_passphrase_note)
            .setView(column)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val passphrase = field.text.toString().toCharArray()
                if (passphrase.size < PracticeCrypto.MIN_PASSPHRASE) {
                    showResult(getString(R.string.dvija_import_refused, getString(R.string.practice_passphrase_short)))
                    return@setPositiveButton
                }
                runImport(uri, passphrase)
            }
            .show()
    }

    private fun runImport(uri: Uri, passphrase: CharArray) {
        val ctx = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val message = withContext(Dispatchers.IO) {
                val sealed = PracticeImport.readFile(ctx, uri)
                if (sealed == null) {
                    passphrase.fill(' ')
                    return@withContext getString(R.string.dvija_import_refused, getString(R.string.practice_file_unreadable))
                }
                when (val outcome = PracticeImport.importSealed(ctx, sealed, passphrase)) {
                    is PracticeImport.Outcome.Imported -> getString(R.string.dvija_import_done, outcome.records)
                    is PracticeImport.Outcome.Refused -> getString(R.string.dvija_import_refused, outcome.reason)
                }
            }
            if (isAdded) {
                showResult(message)
                view?.let { bindReminders(it) }
            }
        }
    }

    private fun showResult(text: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dvija_import_title)
            .setMessage(text)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
