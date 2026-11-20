package com.paperdesk.launcher.data.rules

import com.paperdesk.launcher.domain.aayadi.Criterion
import com.paperdesk.launcher.domain.aayadi.Formula
import com.paperdesk.launcher.domain.aayadi.FractionalPolicy
import com.paperdesk.launcher.domain.aayadi.Operand
import com.paperdesk.launcher.domain.aayadi.Parameter
import com.paperdesk.launcher.domain.aayadi.ReviewStatus
import com.paperdesk.launcher.domain.aayadi.RulePack
import com.paperdesk.launcher.domain.measure.HastaStandard

/**
 * Karnataka / South Indian Shodashavarga pack, as used by the Kannada book's table (page 97).
 *
 * Status: DRAFT. The formulas for aaya, dhana, runa, ayush and vyaya are the book's remainder method as
 * implemented before. Not yet verified: the auspicious aaya set, the dhana/runa comparison, the vyaya < aaya
 * rule (not in the book), the fractional-hasta policy, and golden fixtures. Until those are reviewed, the pack
 * gives no PASS or FAIL judgments.
 */
object KarnatakaShodashavargaPack {
    private const val BOOK = "Kannada book, Shodashavarga table, p.97"

    private val aayaNames = mapOf(
        1L to "ಧ್ವಜ", 2L to "ಧೂಮ", 3L to "ಸಿಂಹ", 4L to "ಶ್ವಾನ",
        5L to "ವೃಷಭ", 6L to "ಖರ", 7L to "ಗಜ", 8L to "ಕಾಕ"
    )

    val draft: RulePack = RulePack(
        id = "karnataka-shodashavarga",
        displayName = "ಷೋಡಶವರ್ಗ (ಕರ್ನಾಟಕ)",
        version = "0.1-draft",
        sourceTitle = BOOK,
        chapter = "not recorded",
        verseOrPage = "p.97",
        edition = "not recorded",
        translatorOrEditor = "",
        reviewStatus = ReviewStatus.DRAFT,
        hastaStandard = HastaStandard.CONVENTIONAL_18,
        fractionalPolicy = FractionalPolicy.WHOLE_HASTA_ONLY,
        parameters = listOf(
            Parameter(
                key = "aaya", displayName = "ಆಯ",
                formula = Formula(Operand.PADAM, multiplier = 9, modulus = 8),
                labels = aayaNames,
                criterion = Criterion.RemainderIn(setOf(1L, 3L, 5L, 7L)),
                mandatory = true,
                sourceRef = "$BOOK: auspicious set 1,3,5,7 not yet verified"
            ),
            Parameter(
                key = "dhana", displayName = "ಧನ",
                formula = Formula(Operand.PADAM, multiplier = 8, modulus = 12),
                labels = emptyMap(),
                criterion = Criterion.Unsourced("ಧನ ಮತ್ತು ಋಣ ಹೋಲಿಕೆಯ ನಿಯಮ ಮೂಲದಲ್ಲಿ ದೃಢಪಟ್ಟಿಲ್ಲ"),
                mandatory = true,
                sourceRef = "$BOOK: value column; comparison not verified"
            ),
            Parameter(
                key = "runa", displayName = "ಋಣ",
                formula = Formula(Operand.PADAM, multiplier = 3, modulus = 8),
                labels = emptyMap(),
                criterion = Criterion.Unsourced("ಧನ ಮತ್ತು ಋಣ ಹೋಲಿಕೆಯ ನಿಯಮ ಮೂಲದಲ್ಲಿ ದೃಢಪಟ್ಟಿಲ್ಲ"),
                mandatory = true,
                sourceRef = "$BOOK: value column; comparison not verified"
            ),
            Parameter(
                key = "ayush", displayName = "ಆಯುಷ್ಯ",
                formula = Formula(Operand.PADAM, multiplier = 9, modulus = 120),
                labels = emptyMap(),
                criterion = Criterion.RemainderAbove(60),
                mandatory = true,
                sourceRef = "$BOOK: ಆಯವರ್ಗದಲ್ಲಿ 60 ಮೇಲ್ಪಟ್ಟು ಆಯಸ್ಸು"
            ),
            Parameter(
                key = "vyaya", displayName = "ವ್ಯಯ",
                formula = Formula(Operand.LENGTH_HASTA, multiplier = 9, modulus = 10),
                labels = emptyMap(),
                criterion = Criterion.Unsourced("ವ್ಯಯ < ಆಯ ನಿಯಮ ಮೂಲ ಗ್ರಂಥದಲ್ಲಿ ಇಲ್ಲ"),
                mandatory = true,
                sourceRef = "$BOOK: value column; vyaya < aaya not in the book"
            ),
        ),
        notApplicable = listOf("yoni", "nakshatra", "vara", "tithi")
    )
}
