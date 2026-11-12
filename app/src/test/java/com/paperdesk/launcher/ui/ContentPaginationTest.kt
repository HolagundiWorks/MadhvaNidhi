package com.paperdesk.launcher.ui

import org.junit.Assert.*
import org.junit.Test

class ContentPaginationTest {
    private fun verify(regions: List<IntRange>, height: Int, total: Int) {
        val boundaries = listOf(0, total) + regions.flatMap { listOf(it.first, it.last) }
        val pages = ContentPagination.paginate(height, total, boundaries,
            regions.map { (it.first + 1) until it.last }, regions, 10)
        assertTrue(pages.all { it.end - it.start in 1..height })
        assertTrue(regions.all { ink -> pages.any { ink.first >= it.start && ink.last <= it.end } })
        assertTrue(pages.none { page -> regions.none { it.first < page.end && it.last > page.start } })
        assertTrue(pages.zipWithNext().all { (a, b) -> a.end <= b.start })
    }

    @Test fun largeParagraphGapsDoNotCutTheFollowingGlyphs() {
        verify(listOf(0..35, 265..302, 530..565), 120, 600)
    }

    @Test fun controlsRemainWholeAcrossSparseSections() {
        verify(listOf(20..90, 240..315, 610..690), 100, 750)
    }

    @Test fun closeLinesAndParagraphBreaksKeepEveryGlyph() {
        verify(listOf(10..80, 84..154, 158..228, 500..570), 145, 600)
    }
}
