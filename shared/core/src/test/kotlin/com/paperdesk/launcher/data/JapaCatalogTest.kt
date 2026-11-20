package com.paperdesk.launcher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JapaCatalogTest {
    @Test fun catalogueHasUniqueIdsAndMantras() {
        assertTrue(JapaCatalog.entries.size >= 12)
        assertEquals(JapaCatalog.entries.size, JapaCatalog.entries.map { it.id }.distinct().size)
        assertEquals(JapaCatalog.entries.size, JapaCatalog.entries.map { it.mantra }.distinct().size)
        assertTrue(JapaCatalog.entries.all { it.label.isNotBlank() && it.mantra.isNotBlank() })
    }

    @Test fun gayatriIsMarkedAsUpadeshaOnly() {
        assertTrue(JapaCatalog.entries.first { it.id == "gayatri" }.requiresUpadesha)
    }
}
