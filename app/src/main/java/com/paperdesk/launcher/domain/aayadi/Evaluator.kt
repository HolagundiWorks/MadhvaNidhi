package com.paperdesk.launcher.domain.aayadi

import com.paperdesk.launcher.domain.measure.FeetInches
import com.paperdesk.launcher.domain.measure.Rational
import java.math.BigInteger

/** One parameter's exact working for a candidate. Every field is shown, or null when not supported. */
data class ParameterResult(
    val key: String,
    val displayName: String,
    /** The operand, multiplier and modulus as written, e.g. "1200 × 9 mod 8". Null when not computed. */
    val exactValue: String?,
    /** The remainder, after the zero-to-modulus rule. Null when not computed. */
    val remainder: Long?,
    /** Traditional category, only for REVIEWED packs. Null for DRAFT packs or absent labels. */
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
     * Judgments come only from a REVIEWED pack; a DRAFT pack gives UNDETERMINED for every parameter.
     */
    fun evaluate(lengthInches: Long, breadthInches: Long, pack: RulePack): CandidateResult {
        require(lengthInches > 0 && breadthInches > 0) { "candidate dimensions must be positive" }
        val standard = pack.hastaStandard
        val lh = standard.hastaOf(lengthInches)
        val bh = standard.hastaOf(breadthInches)
        val area = Rational.of(lengthInches * breadthInches, 144)
        val packProblems = pack.validate()
        val draft = pack.reviewStatus != ReviewStatus.REVIEWED || packProblems.isNotEmpty()
        val fractional = !lh.isInteger || !bh.isInteger

        val results = LinkedHashMap<String, ParameterResult>()
        for (p in pack.parameters) {
            results[p.key] = evaluateParameter(p, lh, bh, pack, draft, fractional)
        }
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

    private fun evaluateParameter(p: Parameter, lh: Rational, bh: Rational, pack: RulePack, draft: Boolean, fractional: Boolean): ParameterResult {
        val operand = when (p.formula.operand) {
            Operand.PADAM -> lh * bh
            Operand.LENGTH_HASTA -> lh
            Operand.BREADTH_HASTA -> bh
        }
        val exact = "${operand} × ${p.formula.multiplier} mod ${p.formula.modulus}"
        fun undetermined(reason: String, remainder: Long? = null) = ParameterResult(
            key = p.key, displayName = p.displayName, exactValue = exact, remainder = remainder, classification = null,
            verdict = Verdict.UNDETERMINED, mandatory = p.mandatory, explanation = reason, sourceRef = p.sourceRef
        )

        // The remainder is shown whenever the operand is whole, even for a DRAFT pack, so the working can be checked.
        if (!operand.isInteger) return undetermined("ಭಿನ್ನ ಮೌಲ್ಯಕ್ಕೆ ಶೇಷದ ನಿಯಮ ಮೂಲದಲ್ಲಿ ಇಲ್ಲ")
        if (fractional && pack.fractionalPolicy == FractionalPolicy.WHOLE_HASTA_ONLY) {
            return undetermined("ಭಿನ್ನ ಹಸ್ತ: ಪೂರ್ಣ ಹಸ್ತ ಮಾತ್ರ")
        }

        val value = operand.numerator.multiply(BigInteger.valueOf(p.formula.multiplier))
        val raw = value.mod(BigInteger.valueOf(p.formula.modulus)).toLong()
        val remainder = if (raw == 0L) p.formula.modulus else raw
        if (draft) return undetermined("ನಿಯಮ ಪಟ್ಟಿ DRAFT: ತೀರ್ಮಾನ ನೀಡುವುದಿಲ್ಲ", remainder)
        val label = p.labels[remainder]
        return when (val c = p.criterion) {
            is Criterion.RemainderIn -> ParameterResult(
                p.key, p.displayName, exact, remainder, label,
                if (remainder in c.favourable) Verdict.PASS else Verdict.FAIL, p.mandatory,
                if (remainder in c.favourable) "ಶುಭ ವರ್ಗ" else "ಅಶುಭ ವರ್ಗ", p.sourceRef
            )
            is Criterion.RemainderAbove -> ParameterResult(
                p.key, p.displayName, exact, remainder, label,
                if (remainder > c.threshold) Verdict.PASS else Verdict.FAIL, p.mandatory,
                "ಶೇಷ ${c.threshold} ಕ್ಕಿಂತ ಹೆಚ್ಚು ಇರಬೇಕು", p.sourceRef
            )
            is Criterion.Unsourced -> undetermined(c.reason, remainder)
        }
    }
}
