package com.paperdesk.launcher.data

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class PanchangaKaranaPadaTest {
    private fun <T> inIndia(block: () -> T): T {
        val previous = TimeZone.getDefault()
        return try { TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata")); block() }
        finally { TimeZone.setDefault(previous) }
    }
    @Test fun allSixtyHalfTithisHaveCorrectFixedSlotsAndRepeatCounts() {
        val names = (0 until 60).map { PanchangaStore.karanaFromElongation(it * 6.0 + 1.0) }
        assertEquals("KIMSTUGHNA", names[0])
        assertEquals(listOf("BAVA","BALAVA","KAULAVA","TAITILA","GARA","VANIJA","VISHTI"), names.subList(1,8))
        assertEquals("VISHTI", names[56])
        assertEquals(listOf("SHAKUNI","CHATUSHPADA","NAGA"), names.subList(57,60))
        for (name in names.subList(1,8)) assertEquals(8, names.count { it == name })
        assertEquals(11, names.distinct().size)
    }
    @Test fun karanaTransitionsOccurAtEverySixDegreesAndWrapAtNewMoon() {
        assertEquals("KIMSTUGHNA", PanchangaStore.karanaFromElongation(5.999999))
        assertEquals("BAVA", PanchangaStore.karanaFromElongation(6.0))
        assertEquals("BALAVA", PanchangaStore.karanaFromElongation(12.0))
        assertEquals("SHAKUNI", PanchangaStore.karanaFromElongation(342.0))
        assertEquals("CHATUSHPADA", PanchangaStore.karanaFromElongation(348.0))
        assertEquals("NAGA", PanchangaStore.karanaFromElongation(354.0))
        assertEquals("NAGA", PanchangaStore.karanaFromElongation(-0.1))
        assertEquals("KIMSTUGHNA", PanchangaStore.karanaFromElongation(360.0))
    }
    @Test fun everyNakshatraContainsFourQuarters() {
        val width = 360.0 / 108.0
        for (quarter in 0 until 108) {
            assertEquals(quarter % 4 + 1, PanchangaStore.padaFromSiderealLongitude((quarter + 0.5) * width))
        }
        assertEquals(1, PanchangaStore.padaFromSiderealLongitude(0.0))
        assertEquals(4, PanchangaStore.padaFromSiderealLongitude(359.9999))
        assertEquals(1, PanchangaStore.padaFromSiderealLongitude(360.0))
    }
    @Test fun julianDayUsesTheUtcInstantRatherThanWallClockHour() {
        val india = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).apply { clear(); set(2000,0,1,17,30,0) }
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = india.timeInMillis }
        assertEquals(2451545.0, PanchangaStore.julian(india), 1e-9)
        assertEquals(PanchangaStore.julian(utc), PanchangaStore.julian(india), 1e-9)
        utc.add(Calendar.MINUTE,30)
        assertEquals(1.0/48.0, PanchangaStore.julian(utc)-PanchangaStore.julian(india), 1e-8)
    }
    @Test fun localSamplesAgreeWithOfficialKaranaNamesAtSixAmIst() = inIndia {
        // SRS daily page facts retrieved 14 Nov 2026; provider sunrise and 06:00 agree for these samples.
        val examples = mapOf("2026-11-14" to "BAVA", "2026-11-15" to "KAULAVA", "2026-11-16" to "GARA", "2026-11-17" to "VISHTI", "2026-04-28" to "BAVA")
        examples.forEach { (date, expected) -> assertEquals(date, expected, PanchangaStore.local(date).karana) }
        assertEquals("PURVA ASHADHA", PanchangaStore.local("2026-11-14").nakshatra)
        assertEquals("DHANUS", PanchangaStore.chandraRashi("2026-11-14"))
    }
    @Test fun localPadaIsNotAttachedToAMismatchingOfficialNakshatra() = inIndia {
        val day = PanchangaStore.local("2026-11-14")
        assertEquals(2, PanchangaStore.nakshatraPada(day.date))
        assertEquals(2, PanchangaStore.nakshatraPada(day.copy(nakshatra="Purvaashadha")))
        assertNull(PanchangaStore.nakshatraPada(day.copy(nakshatra="ASHWINI")))
    }
    @Test fun officialKaranaAliasesAreNormalizedWithoutGuessingUnknownValues() {
        fun html(karana: String) = "<div><p>Sri Parabhavasamvatsare, Dakshinayaana SharadRutu, KarteekaMase, ShuklaPakshe, Panchami Shubha Tithou Saturday, Purvaashadha Nakshatre Shoola Yoge, $karana Karana</p></div>"
        assertEquals("GARA", PanchangaStore.parseSrs(html("Garaja"),"2026-11-14")?.karana)
        assertEquals("VISHTI", PanchangaStore.parseSrs(html("Bhadra"),"2026-11-14")?.karana)
        assertEquals("NAGA", PanchangaStore.parseSrs(html("Naaga"),"2026-11-14")?.karana)
        assertEquals("", PanchangaStore.parseSrs(html("NAVAN"),"2026-11-14")?.karana)
    }
    @Test fun specialKaranaNamesRenderWithoutLatinLetters() {
        val text = OsLang.script("SHAKUNI CHATUSHPADA NAGA KIMSTUGHNA", IndicScript.Lang.KN)
        assertFalse(text.any { it in 'A'..'Z' || it in 'a'..'z' })
    }
    @Test fun civilDateParsingIsStrictAndClearsSubsecondClockState() = inIndia {
        val parsed=PanchangaStore.parse("2026-11-14")
        assertEquals(6,parsed.get(Calendar.HOUR_OF_DAY))
        assertEquals(0,parsed.get(Calendar.MINUTE));assertEquals(0,parsed.get(Calendar.SECOND));assertEquals(0,parsed.get(Calendar.MILLISECOND))
        assertEquals("2026-11-14",PanchangaStore.local("2026-11-14").date)
        assertNotEquals("2026-03-02",PanchangaStore.dateKey(PanchangaStore.parse("2026-02-30")))
    }
}
