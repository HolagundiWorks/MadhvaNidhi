package com.paperdesk.launcher.domain.aayadi


enum class Verdict { PASS, FAIL, UNDETERMINED }

/**
 * DRAFT packs never produce PASS or FAIL judgments.
 * APPLICATION_STANDARD packs use the criteria the application documents, but the source has not been
 * checked against the printed book yet. REVIEWED packs have been checked against the source.
 */
enum class ReviewStatus { DRAFT, APPLICATION_STANDARD, REVIEWED }

/** How a fractional hasta is treated. Chosen by the rule pack, never globally. */
enum class FractionalPolicy { WHOLE_HASTA_ONLY, EXACT_FRACTION }

/** The value a formula works on: the area in square hasta, or one side in hasta. */
enum class Operand { PADAM, LENGTH_HASTA, BREADTH_HASTA }

/** result = (operand x multiplier) mod modulus. A zero remainder is reported as the modulus, as in the book. */
data class Formula(val operand: Operand, val multiplier: Long, val modulus: Long)

enum class Relation { GREATER, LESS }

/** How a parameter's value becomes a favourable or unfavourable judgment. */
sealed interface Criterion {
    /** Favourable when the remainder is one of [favourable]. */
    data class RemainderIn(val favourable: Set<Long>) : Criterion

    /** Favourable when the remainder is above [threshold]. */
    data class RemainderAbove(val threshold: Long) : Criterion

    /** Favourable when this remainder is [relation] the remainder of the parameter [other]. */
    data class Compare(val other: String, val relation: Relation) : Criterion

    /** Stated as needing a source. Gives UNDETERMINED, never PASS, until the source is supplied. */
    data class Unsourced(val reason: String) : Criterion

    /** No favourable test; the value is reported only. */
    object NoCriterion : Criterion
}

data class Parameter(
    val key: String,
    val displayName: String,
    val formula: Formula,
    /** Traditional category for each remainder. Empty when the source gives none. */
    val labels: Map<Long, String>,
    val criterion: Criterion,
    /** A mandatory parameter that is not PASS blocks an overall PASS. */
    val mandatory: Boolean,
    val sourceRef: String
)

data class RulePack(
    val id: String,
    val displayName: String,
    val version: String,
    val sourceTitle: String,
    val chapter: String,
    val verseOrPage: String,
    val edition: String,
    val translatorOrEditor: String,
    val reviewStatus: ReviewStatus,
    val fractionalPolicy: FractionalPolicy,
    val parameters: List<Parameter>,
    /** Parameters the guide lists that this pack does not define. Shown as "Not applicable". */
    val notApplicable: List<String>
) {
    /** Problems that stop a pack from giving judgments. Empty when the pack is complete. */
    fun validate(): List<String> = buildList {
        if (sourceTitle.isBlank()) add("ಮೂಲ ಗ್ರಂಥದ ಹೆಸರು ದಾಖಲಾಗಿಲ್ಲ")
        if (verseOrPage.isBlank()) add("ಪುಟ ಸಂಖ್ಯೆ ದಾಖಲಾಗಿಲ್ಲ")
        if (reviewStatus == ReviewStatus.DRAFT) add("ನಿಯಮ ಪಟ್ಟಿ ಇನ್ನೂ ಪರಿಶೀಲನೆಯಲ್ಲಿದೆ (DRAFT)")
        for (p in parameters) {
            if (p.criterion is Criterion.RemainderIn && p.labels.isEmpty()) add("${p.displayName}: ಶೇಷ ವರ್ಗ ಕೋಷ್ಟಕ ಇಲ್ಲ")
            val c = p.criterion
            if (c is Criterion.Unsourced) add("${p.displayName}: ${c.reason}")
            if (c is Criterion.Compare && parameters.none { it.key == c.other }) add("${p.displayName}: ಹೋಲಿಕೆ ನಿಯತಾಂಕ ${c.other} ಇಲ್ಲ")
        }
    }
}
