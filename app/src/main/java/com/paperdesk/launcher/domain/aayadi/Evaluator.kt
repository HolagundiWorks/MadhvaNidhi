package com.paperdesk.launcher.domain.aayadi

import com.paperdesk.launcher.domain.measure.FeetInches
import com.paperdesk.launcher.domain.measure.Hasta
import com.paperdesk.launcher.domain.measure.Rational

/** One parameter's exact working for a candidate. Every field is shown, or null when not supported. */
data class ParameterResult(
    val key: String,
    val displayName: String,
    /** The operand, multiplier and modulus as written, e.g. "4800 × 1 mod 8". Null when not computed. */
    val exactValue: String?,
    /** The exact remainder, after the zero-to-modulus rule. Null when not computed. */
    val remainder: Rational?,
    /** Traditional category, only for a whole remainder with a label. Null otherwise. */
    val classification: String?,
    val verdict: Verdict,
    val mandatory: Boolean,
    val explanation: String,
    val sourceRef: String
)

data class CandidateResult(
    val length: FeetInches,
    val breadth: FeetInches,
    val lengthHasta: Rational,
    val breadthHasta: Rational,
    /** Exact area in square feet: length_in x breadth_in / 144. */
    val areaSqFt: Rational,
    /** Keyed by parameter key, in pack order, then the "not applicable" entries. */
    val parameters: Map<String, ParameterResult>,
    val verdict: Verdict,
    /** Why the overall verdict is not PASS or FAIL, for diagnostics. Null when the verdict is decided. */
    val reason: String?
)

object Evaluator {
    /**
     * Evaluates one candidate against a rule pack. Lengths are total inches and must be positive.
     * A DRAFT pack, or a pack with missing parts, gives UNDETERMINED for every judgment; its remainders are still shown.
     */
    fun evaluate(lengthInches: Long, breadthInches: Long, pack: RulePack): CandidateResult {
        require(lengthInches > 0 && breadthInches > 0) { "candidate dimensions must be positive" }
        val lh = Hasta.hastaOf(lengthInches)
        val bh = Hasta.hastaOf(breadthInches)
        val area = Rational.of(lengthInches * breadthInches, 144)
        val packProblems = pack.validate()
        val draft = pack.reviewStatus == ReviewStatus.DRAFT || packProblems.isNotEmpty()
        val fractional = !lh.isInteger || !bh.isInteger

        // Pass 1: the exact working for every parameter, so comparisons can read another parameter's remainder.
        val working = pack.parameters.associate { p -> p.key to working(p, lh, bh, pack, fractional) }

        // Pass 2: judgments.
        val results = LinkedHashMap<String, ParameterResult>()
        for (p in pack.parameters) results[p.key] = judge(p, working.getValue(p.key), working, draft)
        for (key in pack.notApplicable) {
            results[key] = ParameterResult(
                key = key, displayName = key, exactValue = null, remainder = null, classification = "Not applicable",
                verdict = Verdict.UNDETERMINED, mandatory = false,
                explanation = "ಈ ನಿಯಮ ಪಟ್ಟಿಯಲ್ಲಿ ಈ ಅಂಶಕ್ಕೆ ಮೂಲ ಸೂತ್ರವಿಲ್ಲ", sourceRef = pack.sourceTitle
            )
        }

        val mandatory = results.values.filter { it.mandatory }
        val reason = when {
            draft -> "ನಿಯಮ ಪಟ್ಟಿ DRAFT: ${packProblems.firstOrNull() ?: "ಪರಿಶೀಲನೆ ಬಾಕಿ"}"
            fractional && pack.fractionalPolicy == FractionalPolicy.WHOLE_HASTA_ONLY -> "ಭಿನ್ನ ಹಸ್ತ: ಈ ನಿಯಮ ಪಟ್ಟಿ ಪೂರ್ಣ ಹಸ್ತವನ್ನು ಮಾತ್ರ ಸ್ವೀಕರಿಸುತ್ತದೆ"
            else -> null
        }
        val verdict = when {
            reason != null -> Verdict.UNDETERMINED
            mandatory.any { it.verdict == Verdict.FAIL } -> Verdict.FAIL
            mandatory.all { it.verdict == Verdict.PASS } -> Verdict.PASS
            else -> Verdict.UNDETERMINED
        }
        return CandidateResult(
            length = FeetInches.ofInches(lengthInches),
            breadth = FeetInches.ofInches(breadthInches),
            lengthHasta = lh,
            breadthHasta = bh,
            areaSqFt = area,
            parameters = results,
            verdict = verdict,
            reason = reason ?: if (verdict == Verdict.UNDETERMINED) "ಕೆಲವು ಅಂಶಗಳು ನಿರ್ಧಾರವಾಗಿಲ್ಲ" else null
        )
    }

    /** The exact operand, the expression as written, and the exact remainder when it can be taken. */
    private class Working(val exact: String, val remainder: Rational?, val note: String?)

    private fun working(p: Parameter, lh: Rational, bh: Rational, pack: RulePack, fractional: Boolean): Working {
        val operand = when (p.formula.operand) {
            Operand.PADAM -> lh * bh
            Operand.LENGTH_HASTA -> lh
            Operand.BREADTH_HASTA -> bh
        }
        val exact = "$operand × ${p.formula.multiplier} mod ${p.formula.modulus}"
        if (fractional && pack.fractionalPolicy == FractionalPolicy.WHOLE_HASTA_ONLY) {
            return Working(exact, null, "ಭಿನ್ನ ಹಸ್ತ: ಪೂರ್ಣ ಹಸ್ತ ಮಾತ್ರ")
        }
        // Exact remainder: no rounding. A zero remainder is reported as the modulus, as in the book.
        val r = (operand * Rational.of(p.formula.multiplier)).modulo(p.formula.modulus)
        return Working(exact, if (r.isZero) Rational.of(p.formula.modulus) else r, null)
    }

    private fun judge(p: Parameter, w: Working, all: Map<String, Working>, draft: Boolean): ParameterResult {
        fun undetermined(reason: String) = ParameterResult(
            key = p.key, displayName = p.displayName, exactValue = w.exact, remainder = w.remainder, classification = null,
            verdict = Verdict.UNDETERMINED, mandatory = p.mandatory, explanation = reason, sourceRef = p.sourceRef
        )
        val remainder = w.remainder ?: return undetermined(w.note ?: "ಶೇಷ ಲೆಕ್ಕ ಹಾಕಲಾಗಿಲ್ಲ")
        if (draft) return undetermined("ನಿಯಮ ಪಟ್ಟಿ DRAFT: ತೀರ್ಮಾನ ನೀಡುವುದಿಲ್ಲ")
        val whole = remainder.integerOrNull()
        val label = whole?.let { p.labels[it] }
        return when (val c = p.criterion) {
            is Criterion.RemainderIn -> {
                // A fractional remainder is not one of the listed whole categories, so it is not favourable.
                val ok = whole != null && whole in c.favourable
                ParameterResult(
                    p.key, p.displayName, w.exact, remainder, label,
                    if (ok) Verdict.PASS else Verdict.FAIL, p.mandatory,
                    if (ok) "ಶುಭ ವರ್ಗ" else "ಅಶುಭ ವರ್ಗ", p.sourceRef
                )
            }
            is Criterion.RemainderAbove -> {
                val ok = remainder > Rational.of(c.threshold)
                ParameterResult(
                    p.key, p.displayName, w.exact, remainder, label,
                    if (ok) Verdict.PASS else Verdict.FAIL, p.mandatory,
                    "ಶೇಷ ${c.threshold} ಕ್ಕಿಂತ ಹೆಚ್ಚು ಇರಬೇಕು", p.sourceRef
                )
            }
            is Criterion.Compare -> {
                val other = all[c.other]?.remainder ?: return undetermined("ಹೋಲಿಕೆಯ ಶೇಷ ಲಭ್ಯವಿಲ್ಲ")
                val holds = if (c.relation == Relation.GREATER) remainder > other else remainder < other
                val sign = if (c.relation == Relation.GREATER) ">" else "<"
                ParameterResult(
                    p.key, p.displayName, w.exact, remainder, label,
                    if (holds) Verdict.PASS else Verdict.FAIL, p.mandatory,
                    "${p.displayName} ($remainder) $sign ${c.other} ($other)", p.sourceRef
                )
            }
            is Criterion.Unsourced -> undetermined(c.reason)
            Criterion.NoCriterion -> undetermined("ಮೌಲ್ಯ ಮಾತ್ರ; ನಿರ್ಧಾರ ಮಾನದಂಡ ಇಲ್ಲ")
        }
    }
}
