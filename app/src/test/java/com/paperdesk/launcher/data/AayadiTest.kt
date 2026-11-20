package com.paperdesk.launcher.data

import com.paperdesk.launcher.data.rules.RulePackLoader
import com.paperdesk.launcher.domain.aayadi.Criterion
import com.paperdesk.launcher.domain.aayadi.Evaluator
import com.paperdesk.launcher.domain.aayadi.ReviewStatus
import com.paperdesk.launcher.domain.aayadi.RulePack
import com.paperdesk.launcher.domain.aayadi.Verdict
import com.paperdesk.launcher.domain.measure.FeetInches
import com.paperdesk.launcher.domain.measure.Rational
import com.paperdesk.launcher.domain.search.Search
import com.paperdesk.launcher.domain.search.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AayadiTest {
    // Read from the same JSON asset the app ships, so the tests check the real pack.
    private val draft: RulePack = RulePackLoader.parse(java.io.File("src/main/assets/rules/karnataka-shodashavarga.json").readText())

    /** Book table rows (length x breadth, as whole hasta): aaya, name, dhana, runa, ayush. */
    private data class Row(val l: Int, val b: Int, val aaya: Long, val dhana: Long, val runa: Long, val ayush: Long)

    private val table = listOf(
        Row(17, 21, 5, 12, 7, 93), Row(15, 21, 3, 12, 1, 75), Row(13, 21, 1, 12, 3, 57),
        Row(21, 29, 1, 12, 3, 81), Row(21, 27, 7, 12, 5, 63), Row(23, 31, 1, 4, 3, 57),
        Row(23, 33, 7, 12, 5, 111), Row(21, 33, 5, 12, 7, 117), Row(25, 43, 3, 8, 1, 75),
        Row(23, 45, 3, 12, 1, 75), Row(25, 41, 1, 4, 3, 105),
        // The printed dhana (11) is not a value the formula can give, so dhana is not compared for this row.
        Row(29, 43, 7, 4, 5, 63), Row(31, 45, 3, 12, 1, 75), Row(31, 43, 5, 8, 7, 117),
        Row(31, 39, 1, 12, 3, 81), Row(33, 39, 7, 12, 5, 63), Row(35, 41, 3, 8, 1, 75),
        Row(35, 43, 1, 4, 3, 105), Row(29, 45, 1, 12, 3, 105),
    )

    /** A pack with every criterion sourced, used only to test the judgment logic (not a real pack). */
    private val testPack: RulePack = draft.copy(
        reviewStatus = ReviewStatus.REVIEWED,
        translatorOrEditor = "test",
        parameters = draft.parameters.filter { it.key == "aaya" || it.key == "ayush" }
    )

    // ---- measurement -------------------------------------------------------

    @Test fun feetAndInchesConvertToTotalInches() {
        assertEquals(480L, FeetInches(40, 0).totalInches)
        assertEquals(360L, FeetInches(30, 0).totalInches)
    }

    @Test fun feetAndInchesRejectBadInput() {
        assertThrows(IllegalArgumentException::class.java) { FeetInches(30, 12) }
        assertThrows(IllegalArgumentException::class.java) { FeetInches(-1, 0) }
    }

    @Test fun feetAndInchesRoundTrip() {
        for (total in 1L..600L) assertEquals(total, FeetInches.ofInches(total).totalInches)
    }

    @Test fun hastaIsExactNotRounded() {
        assertEquals(Rational.of(80, 3), draft.hastaStandard.hastaOf(480))
        assertEquals("80/3", draft.hastaStandard.hastaOf(480).toString())
        assertEquals("20", draft.hastaStandard.hastaOf(360).toString())
    }

    @Test fun rationalReducesAndKeepsDenominatorPositive() {
        assertEquals(Rational.of(2, 3), Rational.of(4, 6))
        assertEquals(Rational.of(-2, 3), Rational.of(2, -3))
    }

    // ---- search ------------------------------------------------------------

    @Test fun searchHas625CandidatesWhenAllPositive() {
        assertEquals(625, Search.candidates(360, 480).size)
    }

    @Test fun searchBoundsAreInclusive() {
        val c = Search.candidates(360, 480).toSet()
        assertTrue(c.contains(348L to 468L))
        assertTrue(c.contains(372L to 492L))
    }

    @Test fun inputCandidateAppearsExactlyOnce() {
        assertEquals(1, Search.candidates(360, 480).count { it == 360L to 480L })
    }

    @Test fun nonPositiveCandidatesAreExcluded() {
        // Length 5 in: deltas below -4 give zero or less, so 17 lengths remain; breadth 100 keeps 25.
        assertEquals(17 * 25, Search.candidates(5, 100).size)
        assertTrue(Search.candidates(5, 100).all { it.first > 0 && it.second > 0 })
    }

    @Test fun everyCandidateRoundTripsToItsIntegerInches() {
        for ((l, b) in Search.candidates(360, 480)) {
            assertEquals(l, FeetInches.ofInches(l).totalInches)
            assertEquals(b, FeetInches.ofInches(b).totalInches)
        }
    }

    @Test fun sortByVariationPutsTheInputFirst() {
        val all = Search.evaluateAll(360, 480, draft)
        val sorted = Search.sort(all, 360, 480, SortOrder.VARIATION)
        assertEquals(360L, sorted.first().length.totalInches)
        assertEquals(480L, sorted.first().breadth.totalInches)
    }

    @Test fun sortByLargestAreaPutsTheBiggestFirst() {
        val sorted = Search.sort(Search.evaluateAll(360, 480, draft), 360, 480, SortOrder.LARGEST_AREA)
        assertEquals(sorted.maxOf { it.areaSqFt.toDouble() }, sorted.first().areaSqFt.toDouble(), 0.0001)
    }

    // ---- DRAFT gating ------------------------------------------------------

    @Test fun draftPackNeverGivesPassOrFail() {
        for (c in Search.evaluateAll(360, 480, draft)) {
            assertEquals(Verdict.UNDETERMINED, c.verdict)
            for (p in c.parameters.values) {
                assertTrue("${p.key} must not judge", p.verdict == Verdict.UNDETERMINED)
                assertNull("${p.key} must not classify", p.classification.takeIf { it != "Not applicable" })
            }
        }
    }

    @Test fun draftPackStillShowsTheRemainder() {
        // 20 x 27 hasta = 540 sq hasta; 540 x 9 mod 8 = 4.
        val r = Evaluator.evaluate(20 * 18L, 27 * 18L, draft)
        assertEquals(4L, r.parameters.getValue("aaya").remainder)
        assertEquals(Verdict.UNDETERMINED, r.parameters.getValue("aaya").verdict)
    }

    @Test fun fractionalSizeShowsNoRemainder() {
        // 360 x 480 inches: 480 in is 80/3 hasta, so the area has no whole remainder.
        val r = Evaluator.evaluate(360, 480, draft)
        assertNull(r.parameters.getValue("aaya").remainder)
    }

    @Test fun draftPackReportsItsProblems() {
        assertTrue(draft.validate().isNotEmpty())
    }

    @Test fun notApplicableParametersAreMarkedNotApplicable() {
        val r = Evaluator.evaluate(360, 480, draft)
        for (key in listOf("yoni", "nakshatra", "vara", "tithi")) {
            val p = r.parameters.getValue(key)
            assertEquals("Not applicable", p.classification)
            assertNull(p.exactValue)
            assertFalse(p.mandatory)
        }
    }

    // ---- formulas on the book table, read from the working ---------------

    @Test fun everyTableRowMatchesAayaAyushRunaAndDhana() {
        for (row in table) {
            val r = Evaluator.evaluate(row.l * 18L, row.b * 18L, draft)
            val tag = "${row.l}x${row.b}"
            assertEquals(tag, row.aaya, r.parameters.getValue("aaya").remainder)
            assertEquals(tag, row.ayush, r.parameters.getValue("ayush").remainder)
            assertEquals(tag, row.runa, r.parameters.getValue("runa").remainder)
            if (!(row.l == 29 && row.b == 43)) assertEquals(tag, row.dhana, r.parameters.getValue("dhana").remainder)
        }
    }

    // ---- judgment logic on a sourced test pack ------------------------------

    @Test fun passWhenAllMandatoryParametersPass() {
        // 17 x 21 hasta: aaya 5 (auspicious), ayush 93 (above 60).
        assertEquals(Verdict.PASS, Evaluator.evaluate(17 * 18L, 21 * 18L, testPack).verdict)
    }

    @Test fun failWhenAMandatoryParameterFails() {
        // 13 x 21 hasta: ayush 57 (not above 60).
        assertEquals(Verdict.FAIL, Evaluator.evaluate(13 * 18L, 21 * 18L, testPack).verdict)
    }

    @Test fun fractionalHastaIsUndeterminedUnderWholeHastaPolicy() {
        // 361 inches is 20.06 hasta: not whole, so the policy refuses to judge it.
        val r = Evaluator.evaluate(361, 21 * 18L, testPack)
        assertEquals(Verdict.UNDETERMINED, r.verdict)
        assertTrue(r.reason!!.contains("ಭಿನ್ನ"))
    }

    @Test fun unsourcedCriterionBlocksPass() {
        // The real pack's vyaya and dhana/runa criteria are unsourced, so even a reviewed copy cannot PASS.
        val reviewed = draft.copy(reviewStatus = ReviewStatus.REVIEWED, translatorOrEditor = "test")
        assertEquals(Verdict.UNDETERMINED, Evaluator.evaluate(17 * 18L, 21 * 18L, reviewed).verdict)
        assertTrue(reviewed.parameters.any { it.criterion is Criterion.Unsourced })
    }

    @Test fun jsonPackMatchesTheDraftRules() {
        assertEquals(ReviewStatus.DRAFT, draft.reviewStatus)
        assertEquals(18L, draft.hastaStandard.inchesPerHasta)
        assertEquals(listOf("aaya", "dhana", "runa", "ayush", "vyaya"), draft.parameters.map { it.key })
        assertEquals(listOf("yoni", "nakshatra", "vara", "tithi"), draft.notApplicable)
        val aaya = draft.parameters.first { it.key == "aaya" }
        assertEquals(9L, aaya.formula.multiplier)
        assertEquals(8L, aaya.formula.modulus)
        assertEquals("ಕಾಕ", aaya.labels[8L])
        assertEquals(Criterion.RemainderIn(setOf(1L, 3L, 5L, 7L)), aaya.criterion)
        assertTrue(draft.validate().isNotEmpty())
    }

    @Test fun zeroOrNegativeCandidateIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { Evaluator.evaluate(0, 360, draft) }
        assertThrows(IllegalArgumentException::class.java) { Evaluator.evaluate(360, -1, draft) }
    }
}
