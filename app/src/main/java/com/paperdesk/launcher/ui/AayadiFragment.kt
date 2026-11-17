package com.paperdesk.launcher.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.Aayadi
import java.text.DecimalFormat

class AayadiFragment : Fragment() {
    private val fmt = DecimalFormat("0.##")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.page_aayadi, container, false)

    override fun onViewCreated(view: View, state: Bundle?) {
        PagePalette.apply(view, "aayadi")
        MaterialLayouts.bind(view)
        listOf(R.id.aayadiTitle, R.id.aayadiResultTitle).forEach { DeskType.apply(view.findViewById(it), DeskType.SEMIBOLD) }
        view.findViewById<TextView>(R.id.aayadiLengthFt).text = "30"
        view.findViewById<TextView>(R.id.aayadiBreadthFt).text = "40"
        bind(view.findViewById(R.id.aayadiLengthIn), listOf("0", "3", "6", "9"), 0)
        bind(view.findViewById(R.id.aayadiBreadthIn), listOf("0", "3", "6", "9"), 0)
        // Results update as the user types, so there is no separate calculate step.
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = calculate(view)
        }
        listOf(R.id.aayadiLengthFt, R.id.aayadiLengthIn, R.id.aayadiBreadthFt, R.id.aayadiBreadthIn)
            .forEach { view.findViewById<TextView>(it).addTextChangedListener(watcher) }
        // The detailed values are hidden until asked for (progressive disclosure).
        val details = view.findViewById<View>(R.id.aayadiDetails)
        val toggle = view.findViewById<MaterialButton>(R.id.aayadiDetailsToggle)
        toggle.setOnClickListener {
            val show = details.visibility != View.VISIBLE
            details.visibility = if (show) View.VISIBLE else View.GONE
            toggle.text = if (show) "ವಿವರ ಮರೆಮಾಡಿ" else "ವಿವರ ತೋರಿಸಿ"
        }
        view.findViewById<View>(R.id.aayadiInfo).setOnClickListener { showInfo() }
        calculate(view)
    }

    private fun showInfo() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("ಆಯಾದಿ ಮಾಹಿತಿ")
            .setMessage(R.string.aayadi_information)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun calculate(v: View) {
        fun number(id: Int): Double = v.findViewById<TextView>(id).text.toString().toDoubleOrNull() ?: 0.0
        var problem: Aayadi.Problem? = null
        val result = try {
            Aayadi.calculate(number(R.id.aayadiLengthFt), number(R.id.aayadiLengthIn), number(R.id.aayadiBreadthFt), number(R.id.aayadiBreadthIn))
        } catch (e: Aayadi.InputProblem) {
            problem = e.problem
            null
        }
        val error = v.findViewById<TextView>(R.id.aayadiError)
        error.text = when (problem) {
            Aayadi.Problem.EMPTY -> "ಉದ್ದ ಮತ್ತು ಅಗಲ ಸೊನ್ನೆಗಿಂತ ಹೆಚ್ಚಿರಬೇಕು"
            Aayadi.Problem.INCHES -> "ಇಂಚು 0, 3, 6 ಅಥವಾ 9 ಆಗಿರಬೇಕು"
            Aayadi.Problem.AREA -> {
                val options = Aayadi.wholeAreaBreadthInches(number(R.id.aayadiLengthFt), number(R.id.aayadiLengthIn), number(R.id.aayadiBreadthFt))
                if (options.isEmpty()) "ಕ್ಷೇತ್ರಫಲ ಪೂರ್ಣ ಚದರ ಅಡಿ ಆಗಬೇಕು. ಈ ಉದ್ದಕ್ಕೆ ಯಾವ ಅಗಲ ಇಂಚೂ ಸರಿಹೊಂದುವುದಿಲ್ಲ; ಉದ್ದ ಬದಲಿಸಿ"
                else "ಕ್ಷೇತ್ರಫಲ ಪೂರ್ಣ ಚದರ ಅಡಿ ಆಗಬೇಕು. ಈ ಉದ್ದಕ್ಕೆ ಅಗಲದ ಇಂಚು ಆಯ್ಕೆ ಮಾಡಿ: ${options.joinToString(", ")}"
            }
            null -> ""
        }
        error.visibility = if (result == null) View.VISIBLE else View.GONE
        v.findViewById<View>(R.id.aayadiResults).visibility = if (result == null) View.GONE else View.VISIBLE
        if (result == null) return
        v.findViewById<TextView>(R.id.aayadiPadam).text = fmt.format(result.padam)
        v.findViewById<TextView>(R.id.aayadiAaya).text = "${fmt.format(result.aaya)}  ·  ${result.aayaName}"
        v.findViewById<TextView>(R.id.aayadiVyaya).text = result.vyaya?.let { fmt.format(it) } ?: "—"
        v.findViewById<TextView>(R.id.aayadiRuna).text = fmt.format(result.runa)
        v.findViewById<TextView>(R.id.aayadiDhana).text = fmt.format(result.dhana)
        v.findViewById<TextView>(R.id.aayadiAyush).text = fmt.format(result.ayush)
        val failing = buildList {
            if (!result.aayaOk) add("ಆಯ")
            if (result.vyayaOk == false) add("ವ್ಯಯ")
            if (!result.ayushOk) add("ಆಯುಷ್ಯ")
        }
        val summary = v.findViewById<TextView>(R.id.aayadiSummary)
        summary.text = if (failing.isEmpty()) "ಎಲ್ಲಾ ಅಂಶಗಳೂ ಅನುಕೂಲ" else "ಪರಿಶೀಲಿಸಿ: ${failing.joinToString(", ")}"
        alertColour(summary, failing.isEmpty())
        alertColour(v.findViewById(R.id.aayadiAaya), result.aayaOk)
        alertColour(v.findViewById(R.id.aayadiVyaya), result.vyayaOk != false)
        alertColour(v.findViewById(R.id.aayadiAyush), result.ayushOk)
    }

    private fun bind(spinner: com.google.android.material.textfield.MaterialAutoCompleteTextView, values: List<String>, initial: Int) {
        MaterialChoices.bind(spinner, values, initial.coerceIn(0, values.lastIndex))
    }

    /** Red when the value falls outside its parameter; the normal text colour otherwise. */
    private fun alertColour(tile: TextView, ok: Boolean) {
        val colour = if (ok) R.color.md_theme_onSurface else R.color.md_error
        tile.setTextColor(ContextCompat.getColor(requireContext(), colour))
    }
}
