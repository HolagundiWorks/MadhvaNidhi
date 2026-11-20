package com.paperdesk.launcher.data

import com.paperdesk.launcher.data.rules.RulePackLoader
import com.paperdesk.launcher.domain.aayadi.CandidateResult
import com.paperdesk.launcher.domain.aayadi.Criterion
import com.paperdesk.launcher.domain.aayadi.Evaluator
import com.paperdesk.launcher.domain.aayadi.ReviewStatus
import com.paperdesk.launcher.domain.aayadi.RulePack
import com.paperdesk.launcher.domain.aayadi.Verdict
import com.paperdesk.launcher.domain.measure.FeetInches
import com.paperdesk.launcher.domain.measure.Hasta
import com.paperdesk.launcher.domain.measure.Rational
import com.paperdesk.launcher.domain.search.Search
import com.paperdesk.launcher.domain.search.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AayadiTest {
    // Read from the same JSON asset the app ships, so the tests check the real pack.
    private val pack: RulePack = RulePackLoader.parse(File("src/main/assets/rules/karnataka-shodashavarga.json").readText())
    private val draft: RulePack = pack.copy(reviewStatus = ReviewStatus.DRAFT)

    /** Book table rows (length x breadth, as whole hasta): aaya, dhana, runa, ayush. */
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

    private fun hasta(h: Int): Long = h * 18L

    /** The whole-number remainder for [key]; fails the test if the remainder is fractional. */
    private fun whole(r: CandidateResult, key: String): Long? = r.parameters.getValue(key).remainder?.integerOrNull()

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
        assertEquals(Rational.of(80, 3), Hasta.hastaOf(480))
        assertEquals("80/3", Hasta.hastaOf(480).toString())
        assertEquals("20", Hasta.hastaOf(360).toString())
    }

    @Test fun rationalReducesAndKeepsDenominatorPositive() {
        assertEquals(Rational.of(2, 3), Rational.of(4, 6))
        assertEquals(Rational.of(-2, 3), Rational.of(2, -3))
    }

    // ---- search ------------------------------------------------------------

    @Test fun searchHas625DistinctCandidatesWhenAllPositive() {
        val c = Search.candidates(360, 480)
        assertEquals(625, c.size)
        assertEquals(625, c.toSet().size)
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
        val sorted = Search.sort(Search.evaluateAll(360, 480, pack), 360, 480, SortOrder.VARIATION)
        assertEquals(360L, sorted.first().length.totalInches)
        assertEquals(480L, sorted.first().breadth.totalInches)
    }

    @Test fun sortByVariationPutsCloserCandidatesFirst() {
        val sorted = Search.sort(Search.evaluateAll(360, 480, pack), 360, 480, SortOrder.VARIATION)
        val deviations = sorted.map { Math.abs(it.length.totalInches - 360) + Math.abs(it.breadth.totalInches - 480) }
        assertEquals(deviations.sorted(), deviations)
    }

    @Test fun sortByLargestAreaPutsTheBiggestFirst() {
        val sorted = Search.sort(Search.evaluateAll(360, 480, pack), 360, 480, SortOrder.LARGEST_AREA)
        assertEquals(sorted.maxOf { it.areaSqFt.toDouble() }, sorted.first().areaSqFt.toDouble(), 0.0001)
    }

    // ---- DRAFT gating ------------------------------------------------------

    @Test fun draftPackNeverGivesPassOrFail() {
        for (c in Search.evaluateAll(360, 480, draft)) {
            assertEquals(Verdict.UNDETERMINED, c.verdict)
            for (p in c.parameters.values) assertFalse("${p.key} must not judge", p.verdict == Verdict.PASS || p.verdict == Verdict.FAIL)
        }
    }

    @Test fun draftPackStillShowsTheRemainder() {
        // 20 x 27 hasta = 540 sq hasta; 540 x 9 mod 8 = 4.
        val r = Evaluator.evaluate(hasta(20), hasta(27), draft)
        assertEquals(4L, whole(r, "aaya"))
        assertEquals(Verdict.UNDETERMINED, r.parameters.getValue("aaya").verdict)
    }

    @Test fun fractionalSizeUsesTheExactRemainder() {
        // 360 x 480 inches = 20 x 80/3 hasta = 1600/3 sq hasta. x 9 = 4800, and 4800 mod 8 = 0, so aaya is 8 (kaaka).
        val r = Evaluator.evaluate(360, 480, pack)
        assertEquals(Rational.of(8), r.parameters.getValue("aaya").remainder)
        assertEquals(Verdict.FAIL, r.parameters.getValue("aaya").verdict)
        assertEquals(Verdict.FAIL, r.verdict)
    }

    @Test fun fractionalRemaindersAreExactNotRounded() {
        // dhana for 20 x 80/3 hasta: (1600/3 x 8) mod 12 = 20/3, kept exact.
        val r = Evaluator.evaluate(360, 480, pack)
        assertEquals(Rational.of(20, 3), r.parameters.getValue("dhana").remainder)
    }

    @Test fun incompletePackReportsItsProblems() {
        assertTrue(draft.validate().isNotEmpty())
        assertTrue(pack.validate().isEmpty())
    }

    // ---- the book table, read from the working -----------------------------

    @Test fun everyTableRowMatchesAayaAyushRunaAndDhana() {
        for (row in table) {
            val r = Evaluator.evaluate(hasta(row.l), hasta(row.b), pack)
            val tag = "${row.l}x${row.b}"
            assertEquals(tag, row.aaya, whole(r, "aaya"))
            assertEquals(tag, row.ayush, whole(r, "ayush"))
            assertEquals(tag, row.runa, whole(r, "runa"))
            if (!(row.l == 29 && row.b == 43)) assertEquals(tag, row.dhana, whole(r, "dhana"))
        }
    }

    // ---- the filter: every mandatory parameter must pass --------------------

    @Test fun originalDimensionsPassWhenAllMandatoryParametersPass() {
        // 17 x 21 hasta: aaya 5 (auspicious), dhana 12 > runa 7, vyaya 3 < aaya 5, ayush 93 > 60.
        val r = Evaluator.evaluate(hasta(17), hasta(21), pack)
        assertEquals(Verdict.PASS, r.verdict)
        assertEquals("ವೃಷಭ", r.parameters.getValue("aaya").classification)
    }

    @Test fun failsWhenAyushFailsEvenIfTheOthersPass() {
        // 13 x 21 hasta: aaya 1 (auspicious), dhana 12 > runa 3, ayush 57 (not above 60); vyaya 7 is not less than 1.
        val r = Evaluator.evaluate(hasta(13), hasta(21), pack)
        assertEquals(Verdict.PASS, r.parameters.getValue("aaya").verdict)
        assertEquals(Verdict.FAIL, r.parameters.getValue("ayush").verdict)
        assertEquals(Verdict.FAIL, r.verdict)
    }

    @Test fun failsWhenVyayaIsNotLessThanAaya() {
        // 15 x 21 hasta: aaya 3, vyaya 5 (not less than 3).
        val r = Evaluator.evaluate(hasta(15), hasta(21), pack)
        assertEquals(Verdict.FAIL, r.parameters.getValue("vyaya").verdict)
        assertEquals(Verdict.FAIL, r.verdict)
    }

    @Test fun failsWhenDhanaIsNotGreaterThanRuna() {
        // 29 x 43 hasta: dhana 4, runa 5.
        val r = Evaluator.evaluate(hasta(29), hasta(43), pack)
        assertEquals(Verdict.FAIL, r.parameters.getValue("dhana").verdict)
        assertEquals(Verdict.FAIL, r.verdict)
    }

    @Test fun runaIsReportedButNotJudgedOnItsOwn() {
        val r = Evaluator.evaluate(hasta(17), hasta(21), pack).parameters.getValue("runa")
        assertEquals(Verdict.UNDETERMINED, r.verdict)
        assertFalse(r.mandatory)
    }

    @Test fun fractionalHastaIsEvaluatedUnderTheExactPolicy() {
        // 361 inches is 361/18 hasta: the pack's policy is exact, so the candidate is judged, not refused.
        val r = Evaluator.evaluate(361, hasta(21), pack)
        assertFalse(r.lengthHasta.isInteger)
        assertTrue(r.reason == null)
    }

    @Test fun fractionalHastaIsUndeterminedUnderAWholeHastaPolicy() {
        // The same size under a whole-hasta policy is refused, with the reason given.
        val wholeOnly = pack.copy(fractionalPolicy = com.paperdesk.launcher.domain.aayadi.FractionalPolicy.WHOLE_HASTA_ONLY)
        val r = Evaluator.evaluate(361, hasta(21), wholeOnly)
        assertEquals(Verdict.UNDETERMINED, r.verdict)
        assertTrue(r.reason!!.contains("ಭಿನ್ನ"))
    }

    @Test fun zeroOrNegativeCandidateIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { Evaluator.evaluate(0, 360, pack) }
        assertThrows(IllegalArgumentException::class.java) { Evaluator.evaluate(360, -1, pack) }
    }

    @Test fun jsonPackMatchesTheApplicationStandard() {
        assertEquals(ReviewStatus.APPLICATION_STANDARD, pack.reviewStatus)
        assertEquals(18L, Hasta.INCHES_PER_HASTA)
        assertEquals(listOf("aaya", "dhana", "runa", "ayush", "vyaya"), pack.parameters.map { it.key })
        assertTrue(pack.notApplicable.isEmpty())
        val aaya = pack.parameters.first { it.key == "aaya" }
        assertEquals(8L, aaya.formula.modulus)
        assertEquals("ಕಾಕ", aaya.labels[8L])
        assertEquals(Criterion.RemainderIn(setOf(1L, 3L, 5L, 7L)), aaya.criterion)
        assertEquals(Criterion.Compare("aaya", com.paperdesk.launcher.domain.aayadi.Relation.LESS), pack.parameters.first { it.key == "vyaya" }.criterion)
    }
}
