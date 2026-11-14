package com.paperdesk.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PanchangaKannadaTest {
    @Test
    fun officialSourceAliasesRenderInKannada() {
        val rendered = OsLang.script(
            "SHRAVANA TITHI · SHATABISHA NAKSHATRA · HUNNIME DINA",
            IndicScript.Lang.KN
        )
        assertEquals("ಶ್ರಾವಣ ತಿಥಿ · ಶತಭಿಷಾ ನಕ್ಷತ್ರ · ಹುಣ್ಣಿಮೆ ದಿನ", rendered)
        assertFalse(rendered.any { it in 'A'..'Z' || it in 'a'..'z' })
    }

    @Test
    fun hunnimeIsRecognisedAsFullMoon() {
        assertTrue(PanchangaStore.isPurnima("HUNNIME"))
    }

    @Test
    fun panchangaLocalMemoizationAndMalformedParseAreSafe() {
        val first = PanchangaStore.local("2026-04-14")
        val second = PanchangaStore.local("2026-04-14")
        assertEquals(first, second)
        assertEquals("2026-04-15", PanchangaStore.shiftKey("2026-04-14", 1))
        val fallback = PanchangaStore.parse("not-a-date")
        assertEquals(0, fallback.get(java.util.Calendar.MILLISECOND))
        val fallbackDay = PanchangaStore.local("bad")
        assertTrue(fallbackDay.tithi.isNotBlank())
    }

    @Test
    fun precompiledOsLangRulesPreserveLongestMatchAndConvertDigits() {
        val compound = OsLang.script("PRATIPADA · CHATURDASHI · PURNIMA", IndicScript.Lang.KN)
        assertTrue(compound.isNotBlank())
        assertFalse(compound.any { it in 'A'..'Z' || it in 'a'..'z' })
        assertEquals("೧೦೮", OsLang.digits(108, IndicScript.Lang.KN))
    }
}
