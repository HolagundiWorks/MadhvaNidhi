package com.paperdesk.launcher.data

import org.junit.Assert.*
import org.junit.Test

class CatalogueClassificationTest {
    private fun group(id: String, collection: String = "") = CatalogueGroup(id, "", "", "", collection)
    private fun item(group: String, type: String) = CatalogueItem("sample", group, "", "", "", type, "", "", "local.txt", "", "")

    @Test fun knownAndFutureSongCategoriesStayInSongs() {
        CatalogueClassification.MAIN_SONG_GROUPS.forEach { assertTrue(CatalogueClassification.isSongGroup(group(it), emptyList())) }
        assertTrue(CatalogueClassification.isSongGroup(group("future", "songs"), emptyList()))
        assertTrue(CatalogueClassification.isSongGroup(group("bhajans"), listOf(item("bhajans", "song"))))
        assertFalse(CatalogueClassification.isSongGroup(group("stotras"), listOf(item("stotras", "stotra"))))
    }

    @Test fun scriptureListsKeepTheirOwnContent() {
        assertEquals("namavali", CatalogueClassification.subpage(item("namavalis", "namavali")))
        assertEquals("suktas", CatalogueClassification.subpage(item("rigveda_suktas", "mantra")))
        assertEquals("stotra", CatalogueClassification.subpage(item("stotras", "stotra")))
        assertEquals("mantras", CatalogueClassification.subpage(item("mantras", "mantra")))
    }
}
