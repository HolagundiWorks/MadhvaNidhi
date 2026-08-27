package com.paperdesk.launcher.data

import android.content.Context

/** Splits the supplied daily-recitation collection into named sections for the vertical reader. */
object NityaParayanaStore {
    data class Section(val title: String, val body: String)

    fun load(context: Context): List<Section> {
        val raw = runCatching {
            context.assets.open("madhwa/texts/nitya_parayana_shlokas.txt")
                .bufferedReader().use { it.readText() }
        }.getOrDefault("")
        return parse(raw)
    }

    fun parse(raw: String): List<Section> {
        val out = ArrayList<Section>()
        var title = ""
        val body = ArrayList<String>()

        fun flush() {
            val text = body.joinToString("\n").replace(Regex("\n{3,}"), "\n\n").trim()
            if (title.isNotBlank() && text.isNotBlank()) out += Section(title, text)
            body.clear()
        }

        raw.replace("\r\n", "\n").lineSequence().forEachIndexed { index, source ->
            val line = source.trim()
            if (index == 0 && line == COLLECTION_TITLE) return@forEachIndexed
            if (isHeading(line)) {
                flush()
                title = line
            } else if (title.isNotBlank()) {
                body += line
            }
        }
        flush()
        return out
    }

    private fun isHeading(line: String): Boolean {
        if (line.isBlank()) return false
        return line == "ಬೌದ್ಧ ಪ್ರಾರ್ಥನ" || HEADING_ENDINGS.any(line::endsWith)
    }

    private const val COLLECTION_TITLE = "ನಿತ್ಯ ಪಾರಾಯಣ ಶ್ಲೋಕಾಃ"
    private val HEADING_ENDINGS = listOf(
        "ಶ್ಲೋಕಃ", "ಶ್ಲೋಕಾಃ", "ಸ್ತೋತ್ರಂ", "ಸ್ತೋತ್ರಾಃ", "ಸ್ತೋತ್ರಾಂ", "ಮಂತ್ರಂ", "ಮಂತ್ರಾಃ"
    )
}
