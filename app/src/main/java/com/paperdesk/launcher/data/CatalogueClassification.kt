package com.paperdesk.launcher.data

/** Explicit song metadata keeps future song categories out of scripture collections. */
object CatalogueClassification {
    val MAIN_SONG_GROUPS = listOf("dasara_padagalu", "suladhigalu", "ugabhogagalu")
    private val songTypes = setOf("song", "hadugalu", "dasara_pada", "suladhi", "ugabhoga", "bhajan", "kirtana")
    fun isSongGroup(group: CatalogueGroup, items: List<CatalogueItem>): Boolean =
        group.id in MAIN_SONG_GROUPS || group.collection.equals("songs", true) ||
            items.any { it.group == group.id && it.type.lowercase() in songTypes }
    fun subpage(item: CatalogueItem): String {
        val text = "${item.id} ${item.kn} ${item.sa} ${item.en}".lowercase()
        return when {
            item.type.equals("namavali", true) || item.group == "namavalis" -> "namavali"
            item.group == "rigveda_suktas" || "sukta" in text || "सूक्त" in text || "ಸೂಕ್ತ" in text -> "suktas"
            item.type.equals("stotra", true) || item.group == "stotras" || item.group == "sanskrit_parayana" -> "stotra"
            else -> "mantras"
        }
    }
}
