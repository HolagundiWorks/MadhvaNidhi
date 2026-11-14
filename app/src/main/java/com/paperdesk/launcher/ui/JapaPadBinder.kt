package com.paperdesk.launcher.ui

import android.text.InputType
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.JapaCatalog
import com.paperdesk.launcher.data.JapaStore
import com.paperdesk.launcher.data.PracticeDayStore
import com.paperdesk.launcher.data.ProfileStore
import java.text.NumberFormat
import java.util.Locale

/** Guided procedure: preparation → counting → closing → explicit completion. */
object JapaPadBinder {
    private val number = NumberFormat.getIntegerInstance(Locale("en", "IN"))

    fun bind(host: Fragment, root: View) {
        val context = host.requireContext()
        val store = JapaStore(context)
        val practice = PracticeDayStore(context)
        val profile = ProfileStore(context).active()
        val ids = profile.dikshaMantras.filter { id -> JapaCatalog.entries.any { it.id == id } }
            .ifEmpty { listOf("gayatri") }
        val entries = ids.map { id -> JapaCatalog.entries.first { it.id == id } }
        var activeId = store.activeMantraId().takeIf { it in ids } ?: ids.first()
        fun button(id: Int) = root.findViewById<MaterialButton>(id)
        fun text(id: Int) = root.findViewById<TextView>(id)
        fun syncDayCompletion() {
            // Completing one mantra must not mark every other mantra complete.
            practice.setJapaDay(complete = ids.all { store.procedurePhase(it) == 3 }, skipped = false)
        }
        var lastPaintedPhase = -1
        fun paint() {
            val c = store.counts(activeId)
            val phase = store.procedurePhase(activeId)
            text(R.id.japaFlowTitle).text = listOf("1 · ಪೂರ್ವಾಂಗ", "2 · ಜಪ ಎಣಿಕೆ", "3 · ಉತ್ತರಾಂಗ", "ಜಪ ಪೂರ್ಣ")[phase]
            text(R.id.japaFlowHint).text = listOf(
                "ಪೂರ್ವಾಂಗ ಓದಿ, ಸಿದ್ಧವಾದಾಗ ಎಣಿಕೆ ಪ್ರಾರಂಭಿಸಿ.",
                "ಪ್ರತಿ ಜಪಕ್ಕೆ +1 ಒತ್ತಿ ಅಥವಾ ಮಾಡಿದ ಜಪದ ಸಂಖ್ಯೆಯನ್ನು ಸೇರಿಸಿ.",
                "ಉತ್ತರಾಂಗ ಓದಿ, ನಂತರ ಜಪ ಪೂರ್ಣಗೊಳಿಸಿ.",
                "ಎಣಿಕೆ ಉಳಿಸಲಾಗಿದೆ. ಪೂರ್ಣಗೊಳಿಸುವುದರಿಂದ ಸಂಖ್ಯೆ ಬದಲಾಗುವುದಿಲ್ಲ."
            )[phase]
            root.findViewById<LinearProgressIndicator>(R.id.japaFlowProgress).progress = phase
            text(R.id.japaCountDone).text = number.format(c.daily)
            text(R.id.japaCountTarget).text = number.format(c.dailyTarget)
            text(R.id.japaStatus).text = "ಒಟ್ಟು ${number.format(c.cumulative)} · ಸಂಕಲ್ಪ ${number.format(c.sankalpa)}"
            text(R.id.japaTargetNote).text = if (c.daily >= c.dailyTarget)
                "ಇಂದಿನ ಗುರಿ ತಲುಪಿದೆ · ಎಣಿಕೆ ಮುಂದುವರಿಸಬಹುದು" else "ಇಂದಿನ ಗುರಿ · ಗುರಿಯನ್ನು Settings ನಲ್ಲಿ ಬದಲಾಯಿಸಿ"
            root.findViewById<View>(R.id.japaCountControls).visibility = if (phase == 1) View.VISIBLE else View.GONE
            button(R.id.japaReadPoorvanga).visibility = if (phase == 0) View.VISIBLE else View.GONE
            button(R.id.japaReadUttaranga).visibility = if (phase == 2) View.VISIBLE else View.GONE
            button(R.id.japaUndo).apply {
                val last = store.lastCountDelta(activeId)
                isEnabled = last > 0
                text = if (last > 0) "+${number.format(last)} ರದ್ದು" else "ಎಣಿಕೆ ರದ್ದು"
            }
            button(R.id.japaFlowNext).text = listOf("ಎಣಿಕೆ ಪ್ರಾರಂಭಿಸಿ", "ಉತ್ತರಾಂಗಕ್ಕೆ ಮುಂದುವರಿಸಿ", "ಜಪ ಪೂರ್ಣಗೊಳಿಸಿ", "ಮತ್ತೆ ಎಣಿಕೆ ಮುಂದುವರಿಸಿ")[phase]
            button(R.id.japaFlowNext).isEnabled = phase != 1 || c.daily > 0
            button(R.id.japaFlowBack).visibility = if (phase in 1..2) View.VISIBLE else View.GONE
            button(R.id.japaFlowBack).text = "ಹಿಂದೆ"
            androidx.core.view.ViewCompat.setStateDescription(root, text(R.id.japaFlowTitle).text)
            if (lastPaintedPhase != phase) {
                lastPaintedPhase = phase
                root.post {
                    fun redraw(view: View) {
                        if (view is android.view.ViewGroup) for (index in 0 until view.childCount) redraw(view.getChildAt(index))
                        view.invalidate()
                    }
                    redraw(root)
                }
            }
        }
        fun record(delta: Long) {
            if (store.procedurePhase(activeId) != 1) return
            store.add(delta, activeId)
            syncDayCompletion()
            paint()
        }
        val mantra = root.findViewById<MaterialAutoCompleteTextView>(R.id.japaMantra)
        MaterialChoices.bind(mantra, entries.map { it.label }, entries.indexOfFirst { it.id == activeId }) {
            activeId = entries[it].id
            store.setActiveMantraId(activeId)
            paint()
        }
        MaterialChoices.wrapLabel(mantra)
        listOf(R.id.japaReadPoorvanga, R.id.japaReadUttaranga).forEachIndexed { index, id ->
            button(id).setOnClickListener {
                host.startActivity(ContentReaderActivity.forJapaAnga(context, entries.first { it.id == activeId }.label, index))
            }
        }
        button(R.id.japaTap).setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            record(1)
        }
        button(R.id.japaUndo).setOnClickListener {
            if (store.procedurePhase(activeId) == 1) { store.undo(activeId); syncDayCompletion(); paint() }
        }
        button(R.id.japaRecordBatch).setOnClickListener {
            val field = TextInputLayout(context).apply {
                hint = "ಮಾಡಿದ ಜಪಗಳ ಸಂಖ್ಯೆ"
                val inset = (24 * resources.displayMetrics.density).toInt()
                setPadding(inset, inset / 3, inset, 0)
                helperText = "ಇದು ಇಂದಿನ ಮತ್ತು ಒಟ್ಟು ಎಣಿಕೆಗೆ ಸೇರುತ್ತದೆ; ಗುರಿ ಬದಲಾಗುವುದಿಲ್ಲ."
            }
            val input = TextInputEditText(context).apply { inputType = InputType.TYPE_CLASS_NUMBER }
            field.addView(input)
            val dialog = MaterialAlertDialogBuilder(context).setTitle("ಮಾಡಿದ ಜಪಗಳನ್ನು ಸೇರಿಸಿ")
                .setView(field).setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("ಎಣಿಕೆಗೆ ಸೇರಿಸಿ", null).create()
            dialog.setOnShowListener {
                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val count = input.text?.toString()?.trim()?.toLongOrNull()
                    if (count == null || count <= 0) field.error = "ಶೂನ್ಯಕ್ಕಿಂತ ಹೆಚ್ಚಿನ ಸರಿಯಾದ ಸಂಖ್ಯೆ ನಮೂದಿಸಿ"
                    else { record(count); dialog.dismiss() }
                }
            }
            dialog.show()
        }
        button(R.id.japaFlowBack).setOnClickListener {
            val phase = store.procedurePhase(activeId)
            if (phase in 1..2) store.setProcedurePhase(activeId, phase - 1)
            paint()
        }
        button(R.id.japaFlowNext).setOnClickListener {
            val phase = store.procedurePhase(activeId)
            fun advance() {
                store.setProcedurePhase(activeId, if (phase == 3) 1 else (phase + 1).coerceAtMost(3))
                if (phase >= 2) syncDayCompletion()
                paint()
            }
            if (phase == 2 && store.counts(activeId).let { it.daily < it.dailyTarget }) {
                MaterialAlertDialogBuilder(context).setTitle("ಇಂದಿನ ಗುರಿ ಇನ್ನೂ ತಲುಪಿಲ್ಲ")
                    .setMessage("ಮಾಡಿದ ಜಪಗಳ ಎಣಿಕೆಯನ್ನು ಹಾಗೆಯೇ ಉಳಿಸಿ ಪೂರ್ಣಗೊಳಿಸುವುದೇ?")
                    .setNegativeButton("ಹಿಂತಿರುಗಿ", null).setPositiveButton("ಪೂರ್ಣಗೊಳಿಸಿ") { _, _ -> advance() }.show()
            } else advance()
        }
        store.setActiveMantraId(activeId)
        host.viewLifecycleOwner.lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onResume(owner: androidx.lifecycle.LifecycleOwner) { paint() }
        })
        paint()
    }
}
