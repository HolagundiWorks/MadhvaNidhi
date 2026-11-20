package com.paperdesk.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PanchangaTraditionsTest {
    @Test
    fun includesTwoMajorMathasAndAllEightUdupiMathas() {
        assertEquals(10, PanchangaTraditions.all.size)
        assertEquals(10, PanchangaTraditions.all.map { it.id }.distinct().size)
        assertTrue(PanchangaTraditions.all.any { it.id == PanchangaTraditions.SRS })
        assertTrue(PanchangaTraditions.all.any { it.id == PanchangaTraditions.UTTARADI })
        assertEquals(8, PanchangaTraditions.all.count { it.id.startsWith("udupi_") })
    }

    @Test
    fun onlyVerifiedOfficialDailyProviderIsMarkedLive() {
        val live = PanchangaTraditions.all.filter { it.liveDailyData }
        assertEquals(listOf(PanchangaTraditions.SRS), live.map { it.id })
        assertTrue(live.single().sourceUrl.startsWith("https://"))
        assertFalse(PanchangaTraditions.find("unknown").id == "unknown")
    }
}
