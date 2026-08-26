package com.paperdesk.launcher.data

/** Strip copy-paste metadata and keep verse / mantra lineation. */
object RitualText {

    fun tidy(raw: String, kannada: Boolean = false): String {
        var t = raw.replace("\r\n", "\n").replace("\u00a0", " ")
        t = t.replace(Regex("\\[Script=[^\\]]*\\]\\S*"), "")
        t = t.replace(Regex("sec:start_\\S+"), "")
        t = t.replace(Regex("\\b(shlokacount\\d*|multicols\\d*|flushleft|flushright|centerline|devanumber\\d*|addtocounter)\\b"), "")
        t = t.replace(Regex("(?m)^\\s*-?\\d+ex\\s*$"), "")
        t = t.replace(Regex("(?m)^\\s*\\[\\d+]#?\\d*\\s*$"), "")
        t = t.replace(Regex("(?m)^enumerate\\s*$"), "")
        t = t.replace(Regex("\\[\\d+em\\]\\[l\\]"), "")
        t = t.replace(Regex("(?m)^https?://\\S+\\s*$"), "")
        t = t.replace(Regex("(?m)^HariKathaamrutaSaara\\s*$"), "")
        t = t.replace(Regex("\\([A-Za-z][^)]*\\)"), "")
        t = t.replace(Regex("(?m)^-- \\d+ of \\d+ --\\s*$"), "")
        t = t.replace(Regex("(?m)^॥इति.*॥\\s*$"), "॥")

        val keep = ArrayList<String>()
        var prev = ""
        t.lineSequence().forEach { line0 ->
            var line = line0.replace(Regex("[ \\t]{2,}"), " ").trim()
            if (!kannada) {
                val hasSa = line.any { it in '\u0900'..'\u097F' }
                if (!hasSa) {
                    line = line.replace(Regex("[A-Za-z\u00C0-\u024F][A-Za-z\u00C0-\u024F'’\\-]*"), " ")
                    line = line.replace(Regex("[ \\t]{2,}"), " ").trim()
                    line = line.replace(Regex("^[\\d.:\\-—]+$"), "")
                }
            }
            if (drop(line)) return@forEach
            if (line.isEmpty()) {
                if (keep.isNotEmpty() && keep.last().isNotEmpty()) keep += ""
                prev = ""
                return@forEach
            }
            if (line == prev) return@forEach
            keep += line.replace("\u00C0", "").replace("\u00E0", "")
            if (stanzaEnd(line) && (keep.isEmpty() || keep.last() != "")) keep += ""
            prev = line
        }
        var out = keep.joinToString("\n")
        out = out.replace(Regex("\n{3,}"), "\n\n")
        return out.trim()
    }

    private fun stanzaEnd(line: String): Boolean {
        val t = line.trim()
        return t.endsWith("॥") || t.endsWith("||") ||
            Regex("\\|\\|\\s*[०-९೦-೯0-9]+\\s*\\|\\|\\s*$").containsMatchIn(t) ||
            Regex("॥\\s*[०-९]+\\s*॥\\s*$").containsMatchIn(t)
    }

    private fun drop(line: String): Boolean {
        if (line.isEmpty()) return false
        val low = line.lowercase()
        if (low.startsWith("field") && low.contains("value")) return true
        if (low.contains("catalogue information") || low.contains("detailed practice file")) return true
        if (low.contains("press ctrl") || low.contains("madhwa festivals")) return true
        if (low.contains("srividya guruprasad")) return true
        if (low.contains("app section") || low.contains("primary language")) return true
        if (low.contains("source   ") || low.contains("category   ")) return true
        if (Regex("^(january|february|march|april|may|june|july|august|september|october|november|december)\\s+\\d", RegexOption.IGNORE_CASE).containsMatchIn(line)) return true
        if (Regex("\\d{4}\\s+srividya", RegexOption.IGNORE_CASE).containsMatchIn(line)) return true
        if (line.any { it.code > 0x7F }) return false
        val letters = line.filter { it.isLetter() }
        if (letters.length >= 4) return true
        return false
    }
}
