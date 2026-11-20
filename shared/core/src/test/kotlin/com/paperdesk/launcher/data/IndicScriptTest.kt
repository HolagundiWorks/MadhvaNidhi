package com.paperdesk.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IndicScriptTest {
    @Test
    fun devanagariTextRendersInKannadaScript() {
        val rendered = IndicScript.convert("श्री कृष्णाय नमः", IndicScript.Lang.KN)

        assertEquals("ಶ್ರೀ ಕೃಷ್ಣಾಯ ನಮಃ", rendered)
        assertTrue(rendered.any { it.code in 0x0C80..0x0CFF })
        assertFalse(rendered.any { it.code in 0x0900..0x097F })
    }

    @Test
    fun mixedKannadaAndDevanagariNormalisesToKannada() {
        val rendered = IndicScript.convert("ಹರಿ ॐ नमो नारायणाय", IndicScript.Lang.KN)

        assertEquals("ಹರಿ ಓಂ ನಮೋ ನಾರಾಯಣಾಯ", rendered)
        assertFalse(rendered.any { it.code in 0x0900..0x097F })
    }
}
