package com.paperdesk.launcher.data

/** A catalogue group: a heading with its Kannada, Sanskrit and English titles. */
data class CatalogueGroup(
    val id: String,
    val kn: String,
    val sa: String,
    val en: String,
    val collection: String = ""
)

/** One catalogue entry. Titles are catalogue data; bodies are loaded from local texts only. */
data class CatalogueItem(
    val id: String,
    val group: String,
    val kn: String,
    val sa: String,
    val en: String,
    val type: String,
    val deity: String,
    val author: String,
    val file: String,
    val script: String,
    val url: String
)
