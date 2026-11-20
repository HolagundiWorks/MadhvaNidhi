package com.paperdesk.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SandhyaInstructionTest {
    private val source = File("src/main/assets/madhwa/texts/yajurveda_sandhyavandana.txt").readText()

    @Test fun yajurSourceHasInstructionLinesAndThreeSessions() {
        val instructionLines = source.lines().count { it.startsWith(SandhyaStore.INSTRUCTION_MARK) }
        assertTrue("instruction lines: $instructionLines", instructionLines >= 20)
        val sessions = SandhyaStore.parseRigveda(source)
        assertEquals(listOf(SandhyaStore.PRATAH, SandhyaStore.MADHYA, SandhyaStore.SAYAM), sessions.map { it.id })
    }

    @Test fun instructionMarkSurvivesTidyAndScriptConversion() {
        val line = "${SandhyaStore.INSTRUCTION_MARK} ಬಲಗೈಯನ್ನು ಗೋವಿನ ಕಿವಿಯ ಆಕಾರದಲ್ಲಿ ಮಾಡಿ"
        val tidied = RitualText.tidy(line)
        assertTrue(tidied, tidied.startsWith(SandhyaStore.INSTRUCTION_MARK))
        for (lang in IndicScript.Lang.values()) {
            val converted = IndicScript.convert(tidied, lang)
            assertTrue("$lang: $converted", converted.startsWith(SandhyaStore.INSTRUCTION_MARK))
        }
    }
}
