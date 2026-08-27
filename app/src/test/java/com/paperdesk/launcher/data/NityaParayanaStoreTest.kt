package com.paperdesk.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NityaParayanaStoreTest {
    private val source = """
        ನಿತ್ಯ ಪಾರಾಯಣ ಶ್ಲೋಕಾಃ

        ಪ್ರಭಾತ ಶ್ಲೋಕಃ
        ಕರಾಗ್ರೇ ವಸತೇ ಲಕ್ಷ್ಮೀಃ ।

        ದೇವತಾ ಸ್ತೋತ್ರಾಃ

        ವಿಷ್ಣು ಸ್ತೋತ್ರಂ
        ಶಾಂತಾಕಾರಂ ಭುಜಗಶಯನಮ್ ॥

        ವಿಶೇಷ ಮಂತ್ರಾಃ
        ಅಷ್ಟಾಕ್ಷರೀ ಮಂತ್ರಂ - ಓಂ ನಮೋ ನಾರಾಯಣಾಯ
    """.trimIndent()

    @Test fun createsOneSectionPerReadingForTheVerticalList() {
        val sections = NityaParayanaStore.parse(source)
        assertEquals(listOf("ಪ್ರಭಾತ ಶ್ಲೋಕಃ", "ವಿಷ್ಣು ಸ್ತೋತ್ರಂ", "ವಿಶೇಷ ಮಂತ್ರಾಃ"), sections.map { it.title })
        assertTrue(sections.first().body.contains("ಕರಾಗ್ರೇ"))
    }

    @Test fun skipsGroupingHeadingsWithoutContent() {
        val pages = NityaParayanaStore.parse(source)
        assertFalse(pages.any { it.title == "ದೇವತಾ ಸ್ತೋತ್ರಾಃ" })
        assertTrue(pages.all { it.body.isNotBlank() })
    }
}
