package com.paperdesk.launcher.data

import java.security.MessageDigest

/**
 * Versioned, checksummed text format for moving personal practice data from Madhva Nidhi to Madhva Dvija.
 *
 * Layout (UTF-8 lines, tab-separated fields):
 * ```
 * #madhva-practice-export
 * schema=1
 * source=<package id of the exporting app>
 * created=<ISO-8601 time>
 * file=<preferences name>	enc=<0|1>	count=<n>
 * <type>	<key>	<value>        (type: s = string, l = long, i = int, b = boolean)
 * ...
 * checksum=<hex SHA-256 of every line above this one, each followed by a newline>
 * ```
 * Values escape backslash, newline, carriage return and tab. Decoding rejects a newer schema, a bad checksum,
 * a file that is not on the caller's allow-list, an encryption flag that does not match, duplicate keys,
 * unknown types and any count mismatch. The checksum doubles as the batch id for duplicate-import detection.
 */
object PracticeTransfer {
    const val SCHEMA_VERSION = 1
    private const val MAGIC = "#madhva-practice-export"

    /**
     * The only preferences files that may be exported or imported: personal practice data and the settings it needs.
     * Reader positions, wiki and song favourites, theme, accessibility and reminders are not practice data and stay
     * in Madhva Nidhi. Maps each file name to whether it is encrypted.
     */
    val PRACTICE_FILES: Map<String, Boolean> = linkedMapOf(
        "paperdesk_japa" to false,
        "paperdesk_japa_enc" to true,
        "madhvanidhi_practice_day" to false,
        "paperdesk_profiles" to false,
        "madhvanidhi_profiles_encrypted" to true,
        "madhvanidhi_nitya" to false,
        "paperdesk_os" to false,
        "paperdesk_panchanga" to false
    )

    enum class Type(val code: Char) {
        STRING('s'), LONG('l'), INT('i'), BOOLEAN('b');

        companion object {
            fun of(code: String): Type? = entries.firstOrNull { it.code.toString() == code }
        }
    }

    data class Entry(val type: Type, val key: String, val value: String)

    data class FileSnapshot(val name: String, val encrypted: Boolean, val entries: List<Entry>)

    data class Batch(val source: String, val createdAt: String, val files: List<FileSnapshot>, val batchId: String)

    class TransferException(message: String) : Exception(message)

    fun encode(source: String, createdAt: String, files: List<FileSnapshot>): String {
        val body = StringBuilder()
        body.append(MAGIC).append('\n')
        body.append("schema=").append(SCHEMA_VERSION).append('\n')
        body.append("source=").append(checkField(source)).append('\n')
        body.append("created=").append(checkField(createdAt)).append('\n')
        for (f in files) {
            checkField(f.name)
            body.append("file=").append(f.name).append('\t')
                .append("enc=").append(if (f.encrypted) '1' else '0').append('\t')
                .append("count=").append(f.entries.size).append('\n')
            for (e in f.entries) {
                checkField(e.key)
                body.append(e.type.code).append('\t').append(e.key).append('\t').append(escape(e.value)).append('\n')
            }
        }
        val checksum = sha256Hex(body.toString())
        body.append("checksum=").append(checksum).append('\n')
        return body.toString()
    }

    /**
     * Parses and validates [text]. [allowed] maps each permitted preferences name to whether it is encrypted.
     * Throws [TransferException] with a message that can be shown to the user.
     */
    fun decode(text: String, allowed: Map<String, Boolean>): Batch {
        val lines = text.split('\n')
        if (lines.size < 6 || lines[0] != MAGIC) throw TransferException("Not a practice export file.")
        val checksumIndex = lines.indexOfLast { it.startsWith("checksum=") }
        if (checksumIndex < 0 || checksumIndex != lines.size - 2 || lines.last().isNotEmpty()) {
            throw TransferException("The file is incomplete: checksum missing or not at the end.")
        }
        val body = lines.subList(0, checksumIndex).joinToString("") { it + "\n" }
        val declared = lines[checksumIndex].removePrefix("checksum=")
        if (sha256Hex(body) != declared) throw TransferException("The file is damaged: checksum does not match.")

        val schema = header(lines, "schema=")?.toIntOrNull()
            ?: throw TransferException("Export version is missing.")
        if (schema > SCHEMA_VERSION) {
            throw TransferException("This export was made by a newer version (schema $schema). Update the app to import it.")
        }
        if (schema != SCHEMA_VERSION) throw TransferException("Unsupported export version $schema.")
        val source = header(lines, "source=") ?: throw TransferException("Export source is missing.")
        val created = header(lines, "created=") ?: throw TransferException("Export time is missing.")

        val files = ArrayList<FileSnapshot>()
        val seenFiles = HashSet<String>()
        var i = 4
        while (i < checksumIndex) {
            val head = lines[i]
            if (!head.startsWith("file=")) throw TransferException("Unexpected line ${i + 1}.")
            val parts = head.split('\t')
            if (parts.size != 3) throw TransferException("Bad file header at line ${i + 1}.")
            val name = parts[0].removePrefix("file=")
            val enc = parts[1].removePrefix("enc=")
            val count = parts[2].removePrefix("count=").toIntOrNull()
                ?: throw TransferException("Bad count at line ${i + 1}.")
            if (!allowed.containsKey(name)) throw TransferException("This file contains data the app does not accept: $name.")
            if (!seenFiles.add(name)) throw TransferException("Duplicate section for $name.")
            val encrypted = when (enc) {
                "1" -> true
                "0" -> false
                else -> throw TransferException("Bad encryption flag for $name.")
            }
            if (allowed.getValue(name) != encrypted) throw TransferException("Encryption flag does not match for $name.")
            i++
            val entries = ArrayList<Entry>()
            val keys = HashSet<String>()
            while (i < checksumIndex && !lines[i].startsWith("file=")) {
                val f = lines[i].split('\t')
                if (f.size != 3) throw TransferException("Bad entry at line ${i + 1}.")
                val type = Type.of(f[0]) ?: throw TransferException("Unknown value type at line ${i + 1}.")
                if (f[1].isEmpty()) throw TransferException("Empty key at line ${i + 1}.")
                if (!keys.add(f[1])) throw TransferException("Duplicate key ${f[1]} in $name.")
                entries.add(Entry(type, f[1], unescape(f[2])))
                i++
            }
            if (entries.size != count) throw TransferException("Entry count for $name does not match.")
            files.add(FileSnapshot(name, encrypted, entries))
        }
        return Batch(source = source, createdAt = created, files = files, batchId = declared)
    }

    private fun header(lines: List<String>, prefix: String): String? =
        lines.take(6).firstOrNull { it.startsWith(prefix) }?.removePrefix(prefix)

    private fun checkField(s: String): String {
        require(!s.contains('\t') && !s.contains('\n') && !s.contains('\r')) { "field contains a control character" }
        return s
    }

    private fun escape(s: String): String = buildString {
        for (c in s) {
            when (c) {
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(c)
            }
        }
    }

    private fun unescape(s: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (s[i + 1]) {
                    '\\' -> out.append('\\')
                    'n' -> out.append('\n')
                    'r' -> out.append('\r')
                    't' -> out.append('\t')
                    else -> throw TransferException("Bad escape in a value.")
                }
                i += 2
            } else {
                out.append(c)
                i++
            }
        }
        return out.toString()
    }

    private fun sha256Hex(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
