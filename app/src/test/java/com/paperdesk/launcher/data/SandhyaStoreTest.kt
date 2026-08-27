package com.paperdesk.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SandhyaStoreTest {
    private val source = """
        ಋಗ್ವೇದೀಯ ತ್ರಿಕಾಲ ಸಂಧ್ಯಾವಂದನೆ
        ಆಶ್ವಲಾಯನ ಸೂತ್ರ

        ಆಚಮ್ಯ –
        ಸಾಮಾನ್ಯ ಆಚಮನ

        ಮಂತ್ರಾಚಮನಂ –
        (ಪ್ರಾತಃ ಕಾಲೇ)
        ಸೂರ್ಯಶ್ಚ ಮಾ
        (ಮಧ್ಯಾಹ್ನ ಕಾಲೇ)
        ಆಪಃ ಪುನಂತು
        (ಸಾಯಂ ಕಾಲೇ)
        ಅಗ್ನಿಶ್ಚ ಮಾ

        ಸಂಕಲ್ಪಂ (ದೇಶಕಾಲ ಸಂಕೀರ್ತನಂ) –
        ಪ್ರಾತಃ/ಮಾಧ್ಯಾಹ್ನಿಕ/ಸಾಯಂ ಸಂಧ್ಯಾವಂದನಂ
    """.trimIndent()

    @Test fun createsThreeCompleteSessionsWithOnlyTheirKalaText() {
        val sessions = SandhyaStore.parseRigveda(source)
        assertEquals(3, sessions.size)
        sessions.forEach { assertEquals(3, it.steps.size) }

        val pratah = sessions.first { it.id == SandhyaStore.PRATAH }
        val morning = pratah.steps.joinToString("\n") { it.body }
        assertTrue(morning.contains("ಸೂರ್ಯಶ್ಚ ಮಾ"))
        assertFalse(morning.contains("ಆಪಃ ಪುನಂತು"))
        assertFalse(morning.contains("ಅಗ್ನಿಶ್ಚ ಮಾ"))
        assertTrue(morning.contains("ಪ್ರಾತಃ ಸಂಧ್ಯಾವಂದನಂ"))
    }

    @Test fun preservesCommonSectionsForEverySession() {
        SandhyaStore.parseRigveda(source).forEach { session ->
            assertTrue(session.steps.joinToString("\n") { it.body }.contains("ಸಾಮಾನ್ಯ ಆಚಮನ"))
        }
    }
}
