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
}
