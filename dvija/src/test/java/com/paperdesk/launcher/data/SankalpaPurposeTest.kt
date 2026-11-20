package com.paperdesk.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SankalpaPurposeTest {
    @Test fun readsFestivalSpecificPurposeMarker() {
        val body = """
            ೨. ಸಂಕಲ್ಪ
            {{SANKALPA_PURPOSE:ಶ್ರೀಬಾಲಕೃಷ್ಣ ಪ್ರೀತ್ಯರ್ಥಂ ಪೂಜಾಂ ಕರಿಷ್ಯೇ ।}}
            ೩. ವಿಶೇಷ ಧ್ಯಾನ
        """.trimIndent()

        assertEquals(
            "ಶ್ರೀಬಾಲಕೃಷ್ಣ ಪ್ರೀತ್ಯರ್ಥಂ ಪೂಜಾಂ ಕರಿಷ್ಯೇ ।",
            Sankalpa.pujaPurpose(body)
        )
    }

    @Test fun missingMarkerUsesTheGenericPurpose() {
        assertEquals("", Sankalpa.pujaPurpose("ಸಂಕಲ್ಪ\nಯಥಾಶಕ್ತಿ ಪೂಜೆ"))
    }
}
