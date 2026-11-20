package com.paperdesk.launcher.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.rules.RulePackLoader
import com.paperdesk.launcher.domain.aayadi.CandidateResult
import com.paperdesk.launcher.domain.aayadi.Evaluator
import com.paperdesk.launcher.domain.aayadi.ParameterResult
import com.paperdesk.launcher.domain.aayadi.RulePack
import com.paperdesk.launcher.domain.aayadi.Verdict
import com.paperdesk.launcher.domain.measure.Hasta
import com.paperdesk.launcher.domain.search.Search
import com.paperdesk.launcher.domain.search.SortOrder

/**
 * The Aayadi screen, in this order: inputs, Section A (the entered size and its verdict),
 * Section B (the recommended sizes, one card each). The calculation lives in the domain layer;
 * this class only maps results to views.
 */
class AayadiFragment : Fragment() {
    private val inches = (0..11).map { it.toString() }

    /** The shipped rule pack, or null when it could not be read (then a configuration error is shown). */
    private var pack: RulePack? = null
    private var packError: String? = null
    private var sortOrder = SortOrder.VARIATION

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.page_aayadi, container, false)

    override fun onViewCreated(view: View, state: Bundle?) {
        PagePalette.apply(view, "aayadi")
        MaterialLayouts.bind(view)
        listOf(R.id.aayadiTitle, R.id.aayadiSectionAHeader, R.id.aayadiSectionBHeader)
            .forEach { DeskType.apply(view.findViewById(it), DeskType.SEMIBOLD) }
        runCatching { RulePackLoader.load(requireContext()) }.also {
            pack = it.getOrNull()
            packError = it.exceptionOrNull()?.message
        }

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

        // Working is collapsed by default: the verdict and the parameters come first.
        val working = view.findViewById<View>(R.id.aayadiWorking)
        val toggle = view.findViewById<MaterialButton>(R.id.aayadiWorkingToggle)
        toggle.setOnClickListener {
            val show = working.visibility != View.VISIBLE
            working.visibility = if (show) View.VISIBLE else View.GONE
            toggle.text = if (show) "ಲೆಕ್ಕದ ವಿವರ ಮರೆಮಾಡಿ" else "ಲೆಕ್ಕದ ವಿವರ ತೋರಿಸಿ"
        }

        // The sort control is a two-option choice, so it is a toggle group rather than a switch.
        val sortGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.aayadiSortGroup)
        sortGroup.check(R.id.aayadiSortClosest)
        sortGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            sortOrder = if (checkedId == R.id.aayadiSortArea) SortOrder.LARGEST_AREA else SortOrder.VARIATION
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
        val lt = lf.toLong() * 12 + li
        val bt = bf.toLong() * 12 + bi
        val error = v.findViewById<TextView>(R.id.aayadiError)
        error.text = when {
            li !in 0..11 || bi !in 0..11 -> "ಇಂಚು 0 ರಿಂದ 11 ರೊಳಗಿರಬೇಕು"
            lf < 0 || bf < 0 || lt <= 0 || bt <= 0 -> "ಉದ್ದ ಮತ್ತು ಅಗಲ ಸೊನ್ನೆಗಿಂತ ಹೆಚ್ಚಿರಬೇಕು"
            else -> ""
        }
        val valid = error.text.isEmpty()
        error.visibility = if (valid) View.GONE else View.VISIBLE
        val sectionA = v.findViewById<View>(R.id.aayadiSectionA)
        val sectionB = v.findViewById<View>(R.id.aayadiSectionB)
        if (!valid) {
            sectionA.visibility = View.GONE
            sectionB.visibility = View.GONE
            v.findViewById<MaterialCardView>(R.id.aayadiConfigCard).visibility = View.GONE
            return
        }

        val rules = pack
        val configCard = v.findViewById<MaterialCardView>(R.id.aayadiConfigCard)
        if (rules == null) {
            // The pack could not be read: a configuration error, and no judgments or results.
            v.findViewById<TextView>(R.id.aayadiConfigError).text = "ನಿಯಮ ಪಟ್ಟಿ ಓದಲಾಗಲಿಲ್ಲ: ${packError ?: ""}"
            configCard.visibility = View.VISIBLE
            sectionA.visibility = View.GONE
            sectionB.visibility = View.GONE
            return
        }

        val problems = rules.validate()
        configCard.visibility = if (problems.isEmpty()) View.GONE else View.VISIBLE
        v.findViewById<TextView>(R.id.aayadiConfigError).text =
            if (problems.isEmpty()) "" else "ನಿಯಮ ಪಟ್ಟಿ ಅಪೂರ್ಣ: ${problems.joinToString("; ")}"

        val original = Evaluator.evaluate(lt, bt, rules)
        val all = Search.evaluateAll(lt, bt, rules)
        val viable = Search.sort(all.filter { it.verdict == Verdict.PASS }, lt, bt, sortOrder)

        sectionA.visibility = View.VISIBLE
        sectionB.visibility = View.VISIBLE
        fillSectionA(v, original)
        fillSectionB(v, viable, all.size, all.count { it.verdict == Verdict.UNDETERMINED }, lt, bt)
    }

    /** Section A: the size, one verdict, then each parameter with its value and its own verdict. */
    private fun fillSectionA(v: View, r: CandidateResult) {
        v.findViewById<TextView>(R.id.aayadiOriginalDims).text = "${r.length} × ${r.breadth}"
        v.findViewById<TextView>(R.id.aayadiOriginalHasta).text =
            "ಹಸ್ತ: ${r.lengthHasta} × ${r.breadthHasta}  ·  1 ಹಸ್ತ = ${Hasta.INCHES_PER_HASTA} ಇಂಚು (${Hasta.ANGULA_PER_HASTA} ಅಂಗುಲ)"

        val chip = v.findViewById<TextView>(R.id.aayadiVerdictChip)
        styleChip(chip, r.verdict)
        val failing = r.parameters.values.filter { it.mandatory && it.verdict == Verdict.FAIL }
        v.findViewById<TextView>(R.id.aayadiVerdictText).text = when (r.verdict) {
            Verdict.PASS -> "ಎಲ್ಲಾ ಮುಖ್ಯ ಅಂಶಗಳೂ ಅನುಕೂಲ"
            Verdict.FAIL -> "ವಿಫಲ: " + failing.joinToString(", ") { it.displayName }
            Verdict.UNDETERMINED -> "ತೀರ್ಮಾನ ಬಾಕಿ: ${r.reason ?: ""}"
        }

        val list = v.findViewById<LinearLayout>(R.id.aayadiParamList)
        list.removeAllViews()
        for (p in r.parameters.values) {
            val row = layoutInflater.inflate(R.layout.item_aayadi_parameter, list, false)
            row.findViewById<TextView>(R.id.paramName).text = p.displayName
            row.findViewById<TextView>(R.id.paramValue).text = valueText(p)
            styleChip(row.findViewById(R.id.paramChip), p.verdict)
            list.addView(row)
        }

        v.findViewById<TextView>(R.id.aayadiWorkingText).text = buildString {
            appendLine("ಪದಂ = ${r.lengthHasta * r.breadthHasta} ಚದರ ಹಸ್ತ")
            for (p in r.parameters.values) {
                appendLine()
                appendLine("${p.displayName}: ${p.exactValue ?: "—"}")
                appendLine("   ಶೇಷ ${p.remainder ?: "—"} · ${p.explanation}")
            }
        }
    }

    /** Section B: a count line, then one card per passing size, or the empty state. */
    private fun fillSectionB(v: View, viable: List<CandidateResult>, evaluated: Int, undetermined: Int, lt: Long, bt: Long) {
        v.findViewById<TextView>(R.id.aayadiRecommendSummary).text =
            "±1 ಅಡಿ ವ್ಯಾಪ್ತಿಯಲ್ಲಿ $evaluated ಅಳತೆ ಪರಿಶೀಲಿಸಲಾಗಿದೆ · ${viable.size} ಶಿಫಾರಸು" +
                if (undetermined > 0) " · $undetermined ಬಾಕಿ" else ""

        val list = v.findViewById<LinearLayout>(R.id.aayadiCandidateList)
        val empty = v.findViewById<MaterialCardView>(R.id.aayadiEmptyCard)
        list.removeAllViews()
        if (viable.isEmpty()) {
            v.findViewById<TextView>(R.id.aayadiEmptyText).text = "No valid combinations found within ±1 foot."
            empty.visibility = View.VISIBLE
            return
        }
        empty.visibility = View.GONE
        for (c in viable) {
            val card = layoutInflater.inflate(R.layout.item_aayadi_candidate, list, false)
            val isOriginal = c.length.totalInches == lt && c.breadth.totalInches == bt
            card.findViewById<TextView>(R.id.candidateDims).text = "${c.length} × ${c.breadth}"
            card.findViewById<TextView>(R.id.candidateOriginalTag).visibility = if (isOriginal) View.VISIBLE else View.GONE
            styleChip(card.findViewById(R.id.candidateChip), Verdict.PASS, label = if (isOriginal) "Pass (original)" else "Pass")
            card.findViewById<TextView>(R.id.candidateAaya).text = cell(c, "aaya")
            card.findViewById<TextView>(R.id.candidateDhana).text = cell(c, "dhana")
            card.findViewById<TextView>(R.id.candidateRunaVyaya).text = "${cell(c, "runa")} / ${cell(c, "vyaya")}"
            card.findViewById<TextView>(R.id.candidateAyush).text = cell(c, "ayush")
            card.setOnClickListener { showDetail(c) }
            list.addView(card)
        }
    }

    /** The value and its classification, e.g. "7 · ಗಜ". Shows an em dash when the working gives none. */
    private fun valueText(p: ParameterResult): String {
        val rem = p.remainder?.toString() ?: "—"
        return if (p.classification != null) "$rem · ${p.classification}" else rem
    }

    private fun cell(c: CandidateResult, key: String): String {
        val p = c.parameters[key] ?: return "—"
        return valueText(p)
    }

    /** One place for status colour, text and shape, so no status depends on colour alone. */
    private fun styleChip(chip: TextView, verdict: Verdict, label: String? = null) {
        chip.text = label ?: when (verdict) {
            Verdict.PASS -> "Pass"
            Verdict.FAIL -> "Fail"
            Verdict.UNDETERMINED -> "Undetermined"
        }
        val (background, text) = when (verdict) {
            Verdict.PASS -> R.drawable.aayadi_chip_pass to R.color.md_theme_onPrimaryContainer
            Verdict.FAIL -> R.drawable.aayadi_chip_fail to R.color.md_theme_onErrorContainer
            Verdict.UNDETERMINED -> R.drawable.aayadi_chip_neutral to R.color.md_theme_onSurfaceVariant
        }
        chip.setBackgroundResource(background)
        chip.setTextColor(ContextCompat.getColor(requireContext(), text))
        chip.contentDescription = "Result: ${chip.text}"
    }

    /** Derivation, classification and source notes for one passing size. */
    private fun showDetail(c: CandidateResult) {
        val text = buildString {
            appendLine("Length ${c.length} × Breadth ${c.breadth}")
            appendLine("Hasta: ${c.lengthHasta} × ${c.breadthHasta}; area ${c.areaSqFt} sq ft")
            appendLine()
            for (p in c.parameters.values) {
                appendLine("${p.displayName}: ${p.exactValue ?: "—"}  →  remainder ${p.remainder ?: "—"}  ${p.classification ?: ""}")
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
}
