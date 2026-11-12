package com.paperdesk.launcher.data

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class PanchangaCalendarTest {
    @Test fun sharedDatesMergeWithoutLosingTithisOrDates() {
        val days = List(15) { listOf("2026-04-${(it + 10).toString().padStart(2, '0')}") }.toMutableList()
        days[7] = days[6]
        days[10] = days[9]
        days[2] = days[2] + "2026-04-30"
        val tiles = PanchangaCalendar.tiles(days)
        assertEquals((1..15).toList(), tiles.flatMap { it.tithis })
        assertEquals(days.flatten().toSet(), tiles.flatMap { it.dates }.toSet())
        assertEquals(listOf(7, 8), tiles.first { 7 in it.tithis }.tithis)
        assertEquals(listOf(10, 11), tiles.first { 11 in it.tithis }.tithis)
        assertEquals(2, tiles.first { 3 in it.tithis }.dates.size)
    }
    @Test fun missingSunriseTithisAreNotAssignedAnInventedDate() {
        val tiles = PanchangaCalendar.tiles(List(15) { emptyList() })
        assertEquals(15, tiles.size)
        assertTrue(tiles.all { it.dates.isEmpty() })
    }

    @Test fun cyclesRetainEveryCivilDateAcrossYearAndLeapDayBoundaries() {
        for (anchor in listOf("2026-04-20", "2026-11-11", "2026-12-31", "2024-02-29", "2026-03-18")) {
            val cycle = PanchangaCalendar.cycle(anchor)
            assertEquals(15, cycle.shukla.size)
            assertEquals(15, cycle.krishna.size)
            val dates = (cycle.shukla + cycle.krishna).flatten()
            assertEquals(dates.size, dates.distinct().size)
            assertTrue(anchor in dates)
            val count = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(cycle.start), LocalDate.parse(cycle.end)).toInt() + 1
            assertTrue(count in 29..30)
            assertEquals(count, dates.size)
            assertEquals((0 until count).map { LocalDate.parse(cycle.start).plusDays(it.toLong()).toString() }.sorted(), dates.sorted())
            assertEquals("SHUKLA", PanchangaStore.local(cycle.start).paksha)
            assertEquals("KRISHNA", PanchangaStore.local(cycle.end).paksha)
        }
    }
    @Test fun requestedHeaderUsesParabhavaWithoutMatchingTheShorterBhavaName() {
        val day = PanchangaStore.local("2026-04-20").copy(samvatsara = "Sri PARABHAVA", masa = "VAISHAKHA", paksha = "SHUKLA")
        val cycle = PanchangaCalendar.Cycle("2026-04-18", "2026-05-16", List(15) { emptyList() }, List(15) { emptyList() })
        val header = PanchangaCalendar.header(day, cycle)
        assertTrue(header.startsWith("ಶ್ರೀ ಶಾಲಿವಾಹನ ಗತಶಕೆ 1948"))
        assertTrue(header.contains("ಪರಾಭವ ನಾಮ ಸಂವತ್ಸರ"))
        assertTrue(header.contains("ವೈಶಾಖಮಾಸ ಶುಕ್ಲಪಕ್ಷ"))
        assertTrue(header.contains("ಏಪ್ರಿಲ್–ಮೇ 2026"))
        assertFalse(header.any { it in 'A'..'Z' || it in 'a'..'z' })
    }
    @Test fun shakaYearUsesTheSelectedSamvatsaraBeforeAndAfterUgadi() {
        val before = PanchangaStore.local("2026-03-18").copy(samvatsara = "VISHVAVASU")
        assertEquals(1947, PanchangaStore.shakaYear(before))
        assertEquals(1948, PanchangaStore.shakaYear(before.copy(samvatsara = "PARABHAVA")))
    }
}
