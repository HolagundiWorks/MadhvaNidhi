package com.paperdesk.launcher.domain.search

import com.paperdesk.launcher.domain.aayadi.CandidateResult
import com.paperdesk.launcher.domain.aayadi.Evaluator
import com.paperdesk.launcher.domain.aayadi.RulePack

enum class SortOrder {
    /** Guide default: smallest total deviation, then smallest largest deviation, then largest area, then length, breadth. */
    VARIATION,

    /** Alternative: largest area first. */
    LARGEST_AREA
}

object Search {
    /** Each dimension may move up to one foot either way, in whole-inch steps. */
    const val RADIUS_INCHES = 12L

    /**
     * Candidate lengths and breadths in total inches: every combination within the radius, inclusive.
     * Non-positive candidates are excluded. The input appears exactly once, at delta 0,0.
     */
    fun candidates(lengthInches: Long, breadthInches: Long): List<Pair<Long, Long>> {
        val out = ArrayList<Pair<Long, Long>>()
        for (dl in -RADIUS_INCHES..RADIUS_INCHES) {
            for (db in -RADIUS_INCHES..RADIUS_INCHES) {
                val l = lengthInches + dl
                val b = breadthInches + db
                if (l <= 0 || b <= 0) continue
                out.add(l to b)
            }
        }
        return out
    }

    /** Evaluates every candidate. The result holds every candidate, not only PASS rows. */
    fun evaluateAll(lengthInches: Long, breadthInches: Long, pack: RulePack): List<CandidateResult> =
        candidates(lengthInches, breadthInches).map { (l, b) -> Evaluator.evaluate(l, b, pack) }

    /** Sorts candidates for display. The base is the entered size, used for the deviation. */
    fun sort(results: List<CandidateResult>, baseLengthInches: Long, baseBreadthInches: Long, order: SortOrder): List<CandidateResult> {
        val comparator: Comparator<CandidateResult> = when (order) {
            SortOrder.VARIATION -> compareBy<CandidateResult> { totalDeviation(it, baseLengthInches, baseBreadthInches) }
                .thenBy { maxDeviation(it, baseLengthInches, baseBreadthInches) }
                .thenByDescending { it.areaSqFt }
                .thenBy { it.length.totalInches }
                .thenBy { it.breadth.totalInches }
            SortOrder.LARGEST_AREA -> compareByDescending<CandidateResult> { it.areaSqFt }
                .thenBy { it.length.totalInches }
                .thenBy { it.breadth.totalInches }
        }
        return results.sortedWith(comparator)
    }

    private fun totalDeviation(c: CandidateResult, bl: Long, bb: Long): Long =
        Math.abs(c.length.totalInches - bl) + Math.abs(c.breadth.totalInches - bb)

    private fun maxDeviation(c: CandidateResult, bl: Long, bb: Long): Long =
        maxOf(Math.abs(c.length.totalInches - bl), Math.abs(c.breadth.totalInches - bb))
}
