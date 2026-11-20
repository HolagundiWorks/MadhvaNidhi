package com.paperdesk.launcher.ui

import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textview.MaterialTextView
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.rules.RulePackLoader
import com.paperdesk.launcher.domain.aayadi.CandidateResult
import com.paperdesk.launcher.domain.measure.Hasta
import com.paperdesk.launcher.domain.aayadi.Evaluator
import com.paperdesk.launcher.domain.aayadi.ParameterResult
import com.paperdesk.launcher.domain.aayadi.RulePack
import com.paperdesk.launcher.domain.aayadi.Verdict
import com.paperdesk.launcher.domain.search.Search
import com.paperdesk.launcher.domain.search.SortOrder
import java.text.DecimalFormat

class AayadiFragment : Fragment() {
    private val fmt = DecimalFormat("0.##")
    private val inches = (0..11).map { it.toString() }

    /** The shipped rule pack, or null when it could not be read (then a configuration error is shown). */
    private var pack: RulePack? = null
    private var packError: String? = null
    private var sortOrder = SortOrder.VARIATION

    /** Columns of the recommended table, as the specification lists them. */
    private val tableColumns = listOf("Length", "Breadth", "Aaya", "Dhana", "Runa / Vyaya", "Aayushu", "Result")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.page_aayadi, container, false)

    override fun onViewCreated(view: View, state: Bundle?) {
        PagePalette.apply(view, "aayadi")
        MaterialLayouts.bind(view)
        runCatching { RulePackLoader.load(requireContext()) }.also {
            pack = it.getOrNull()
            packError = it.exceptionOrNull()?.message
        }
        listOf(R.id.aayadiTitle, R.id.aayadiResultTitle).forEach { DeskType.apply(view.findViewById(it), DeskType.SEMIBOLD) }
        view.findViewById<TextView>(R.id.aayadiLengthFt).text = "30"
        view.findViewById<TextView>(R.id.aayadiBreadthFt).text = "40"
        MaterialChoices.bind(view.findViewById<MaterialAutoCompleteTextView>(R.id.aayadiLengthIn), inches, 0)
        MaterialChoices.bind(view.findViewById<MaterialAutoCompleteTextView>(R.id.aayadiBreadthIn), inches, 0)
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
        view.findViewById<MaterialSwitch>(R.id.aayadiSortSwitch).setOnCheckedChangeListener { _, checked ->
            sortOrder = if (checked) SortOrder.LARGEST_AREA else SortOrder.VARIATION
            calculate(view)
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
        fun number(id: Int): Int = v.findViewById<TextView>(id).text.toString().toIntOrNull() ?: 0
        val lf = number(R.id.aayadiLengthFt)
        val li = number(R.id.aayadiLengthIn)
        val bf = number(R.id.aayadiBreadthFt)
        val bi = number(R.id.aayadiBreadthIn)
        val error = v.findViewById<TextView>(R.id.aayadiError)
        val lt = lf.toLong() * 12 + li
        val bt = bf.toLong() * 12 + bi
        error.text = when {
            li !in 0..11 || bi !in 0..11 -> "ಇಂಚು 0 ರಿಂದ 11 ರೊಳಗಿರಬೇಕು"
            lf < 0 || bf < 0 || lt <= 0 || bt <= 0 -> "ಉದ್ದ ಮತ್ತು ಅಗಲ ಸೊನ್ನೆಗಿಂತ ಹೆಚ್ಚಿರಬೇಕು"
            else -> ""
        }
        val valid = error.text.isEmpty()
        error.visibility = if (valid) View.GONE else View.VISIBLE
        listOf(R.id.aayadiSummaryCard, R.id.aayadiResults, R.id.aayadiTableTitle, R.id.aayadiSortSwitch, R.id.aayadiTableCard)
            .forEach { v.findViewById<View>(it).visibility = if (valid) View.VISIBLE else View.GONE }
        if (!valid) return

        val rules = pack
        if (rules == null) {
            // The pack could not be read: a configuration error, and no judgments or results.
            v.findViewById<View>(R.id.aayadiResults).visibility = View.GONE
            v.findViewById<TextView>(R.id.aayadiSummaryText).text = ""
            v.findViewById<TextView>(R.id.aayadiConfigError).apply {
                text = "ನಿಯಮ ಪಟ್ಟಿ ಓದಲಾಗಲಿಲ್ಲ: ${packError ?: ""}"
                visibility = View.VISIBLE
            }
            fillTable(v, emptyList(), lt, bt, packIncomplete = true)
            return
        }

        val original = Evaluator.evaluate(lt, bt, rules)
        val all = Search.evaluateAll(lt, bt, rules)
        val viable = Search.sort(all.filter { it.verdict == Verdict.PASS }, lt, bt, sortOrder)
        val problems = rules.validate()

        v.findViewById<TextView>(R.id.aayadiSummaryText).text = buildString {
            appendLine("ನೀಡಿದ ಅಳತೆ: ${original.length} × ${original.breadth}")
            appendLine("ಹಸ್ತ: ${original.lengthHasta} × ${original.breadthHasta} (1 ಹಸ್ತ = ${Hasta.INCHES_PER_HASTA} ಇಂಚು)")
            appendLine("ಹುಡುಕಾಟ ವ್ಯಾಪ್ತಿ: ±1 ಅಡಿ, ${all.size} ಅಳತೆ")
            append("ಮೌಲ್ಯಮಾಪನ: ${all.size} · Pass: ${viable.size} · ಬಾಕಿ: ${all.count { it.verdict == Verdict.UNDETERMINED }}")
        }
        val configError = v.findViewById<TextView>(R.id.aayadiConfigError)
        configError.text = if (problems.isEmpty()) "" else "ನಿಯಮ ಪಟ್ಟಿ ಅಪೂರ್ಣ: ${problems.joinToString("; ")}"
        configError.visibility = if (problems.isEmpty()) View.GONE else View.VISIBLE

        fillOriginal(v, original)
        fillTable(v, viable, lt, bt, packIncomplete = problems.isNotEmpty())
    }

    private fun fillOriginal(v: View, r: CandidateResult) {
        val p = r.parameters
        v.findViewById<TextView>(R.id.aayadiSummary).text = when (r.verdict) {
            Verdict.PASS -> "Pass: all mandatory conditions are met"
            Verdict.FAIL -> "Fail: " + p.values.filter { it.mandatory && it.verdict == Verdict.FAIL }.joinToString(", ") { it.displayName }
            Verdict.UNDETERMINED -> "Undetermined: ${r.reason ?: ""}"
        }
        v.findViewById<TextView>(R.id.aayadiAaya).text = tileText(p["aaya"])
        v.findViewById<TextView>(R.id.aayadiVyaya).text = tileText(p["vyaya"])
        v.findViewById<TextView>(R.id.aayadiAyush).text = tileText(p["ayush"])
        v.findViewById<TextView>(R.id.aayadiDhana).text = "Dhana: ${p["dhana"]?.remainder ?: "—"} (${verdictWord(p["dhana"])})"
        v.findViewById<TextView>(R.id.aayadiRuna).text = "Runa: ${p["runa"]?.remainder ?: "—"} (${verdictWord(p["runa"])})"
        v.findViewById<TextView>(R.id.aayadiPadam).text = "${r.lengthHasta * r.breadthHasta} ಚದರ ಹಸ್ತ"
        alertColour(v.findViewById(R.id.aayadiAaya), p["aaya"]?.verdict != Verdict.FAIL)
        alertColour(v.findViewById(R.id.aayadiVyaya), p["vyaya"]?.verdict != Verdict.FAIL)
        alertColour(v.findViewById(R.id.aayadiAyush), p["ayush"]?.verdict != Verdict.FAIL)
        alertColour(v.findViewById(R.id.aayadiSummary), r.verdict != Verdict.FAIL)
    }

    /** Remainder, the traditional name when the pack gives one, and the verdict word. */
    private fun tileText(p: ParameterResult?): String {
        if (p == null) return "—"
        val rem = p.remainder?.toString() ?: "—"
        val name = p.classification?.let { "  ·  $it" } ?: ""
        return "$rem$name  (${verdictWord(p)})"
    }

    private fun verdictWord(p: ParameterResult?): String = when (p?.verdict) {
        Verdict.PASS -> "Pass"
        Verdict.FAIL -> "Fail"
        Verdict.UNDETERMINED, null -> "Undetermined"
    }

    /** Lists the sizes that pass every mandatory condition, or the empty and configuration states. */
    private fun fillTable(v: View, viable: List<CandidateResult>, originalLt: Long, originalBt: Long, packIncomplete: Boolean) {
        val grid = v.findViewById<GridLayout>(R.id.aayadiTableGrid)
        val scroll = v.findViewById<View>(R.id.aayadiTableScroll)
        val empty = v.findViewById<TextView>(R.id.aayadiTableEmpty)
        grid.removeAllViews()
        when {
            packIncomplete -> {
                empty.text = "ನಿಯಮ ಪಟ್ಟಿ ಅಪೂರ್ಣ: No valid combinations can be shown"
                scroll.visibility = View.GONE
            }
            viable.isEmpty() -> {
                empty.text = "No valid combinations found within ±1 foot."
                scroll.visibility = View.GONE
            }
            else -> {
                empty.text = ""
                scroll.visibility = View.VISIBLE
                grid.columnCount = tableColumns.size
                tableColumns.forEach { addCell(grid, it, header = true, bold = false, onClick = null) }
                viable.forEach { c ->
                    val isOriginal = c.length.totalInches == originalLt && c.breadth.totalInches == originalBt
                    val open = { showDetail(c) }
                    addCell(grid, "${c.length}", header = false, bold = isOriginal, onClick = open)
                    addCell(grid, "${c.breadth}", header = false, bold = isOriginal, onClick = open)
                    addCell(grid, cell(c, "aaya"), header = false, bold = isOriginal, onClick = open)
                    addCell(grid, cell(c, "dhana"), header = false, bold = isOriginal, onClick = open)
                    addCell(grid, "${cell(c, "runa")} / ${cell(c, "vyaya")}", header = false, bold = isOriginal, onClick = open)
                    addCell(grid, cell(c, "ayush"), header = false, bold = isOriginal, onClick = open)
                    addCell(grid, if (isOriginal) "Pass (original)" else "Pass", header = false, bold = isOriginal, onClick = open)
                }
            }
        }
    }

    /** The remainder, with the traditional name for aaya. */
    private fun cell(c: CandidateResult, key: String): String {
        val p = c.parameters[key] ?: return "—"
        val rem = p.remainder?.toString() ?: "—"
        return if (key == "aaya" && p.classification != null) "$rem ${p.classification}" else rem
    }

    private fun addCell(grid: GridLayout, text: String, header: Boolean, bold: Boolean, onClick: (() -> Unit)?) {
        val tv = MaterialTextView(requireContext())
        TextViewCompat.setTextAppearance(tv, if (header) R.style.TextAppearance_DS_Caption else R.style.TextAppearance_DS_Body)
        tv.text = text
        tv.gravity = Gravity.CENTER
        tv.setPadding(16, 12, 16, 12)
        if (header || bold) tv.setTypeface(tv.typeface, Typeface.BOLD)
        if (onClick != null) {
            tv.isClickable = true
            tv.isFocusable = true
            tv.setOnClickListener { onClick() }
            tv.contentDescription = "Details: $text"
        }
        grid.addView(tv)
    }

    /** Derivation, classification and source notes for one passing row. */
    private fun showDetail(c: CandidateResult) {
        val text = buildString {
            appendLine("Length ${c.length} × Breadth ${c.breadth}")
            appendLine("Hasta: ${c.lengthHasta} × ${c.breadthHasta}; area ${c.areaSqFt} sq ft")
            appendLine()
            for (p in c.parameters.values) {
                appendLine("${p.displayName}: ${p.exactValue ?: "—"}  →  remainder ${p.remainder ?: "—"}  ${p.classification ?: ""}  (${verdictWord(p)})")
                appendLine("   ${p.explanation}")
                appendLine("   Source: ${p.sourceRef}")
            }
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Details")
            .setMessage(text)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    /** Red when the value falls outside its parameter; the normal text colour otherwise. */
    private fun alertColour(tile: TextView, ok: Boolean) {
        val colour = if (ok) R.color.md_theme_onSurface else R.color.md_error
        tile.setTextColor(ContextCompat.getColor(requireContext(), colour))
    }
}
