package com.paperdesk.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Test

class KannadaPhoneticTest {
    @Test fun convertsCommonNameTyping() {
        assertEquals("ವಿಶ್ವಾಭಿರಾಮ", KannadaPhonetic.transliterate("vishwaabhiraama"))
        assertEquals("ಮಾಧವ", KannadaPhonetic.transliterate("maadhava"))
    }

    @Test fun preservesKannadaAndSpaces() {
        assertEquals("ಶ್ರೀ ರಾಮ", KannadaPhonetic.transliterate("ಶ್ರೀ raama"))
    }
}
