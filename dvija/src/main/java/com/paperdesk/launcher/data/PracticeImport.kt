package com.paperdesk.launcher.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.paperdesk.launcher.data.PracticeTransfer.Type
import com.paperdesk.launcher.data.PracticeTransfer.TransferException
import java.io.IOException

/**
 * Imports a practice export made by Madhva Nidhi into this app's own preferences.
 *
 * Steps: read the file (size-limited), decrypt it with the passphrase, validate it with [PracticeTransfer.decode]
 * against the shared allow-list. Then:
 * - A file already imported (same batch id) is refused.
 * - Import is allowed only when this device has no practice history yet. Defaults that the app writes on first
 *   launch (a zero best streak, the default profile, migration flags) do not count as history.
 * - Otherwise the device's practice files are replaced by the file's contents. Madhva Nidhi is not changed.
 */
object PracticeImport {
    private const val LEDGER = "madhvadvija_transfer"
    private const val MAX_BYTES = 5_000_000

    /** Keys the app writes by itself on first use. They are not practice history. */
    private val DEFAULT_KEYS = setOf("best_sandhya_streak", "profiles_migrated", "lifetime_removed", "active", "items")

    sealed class Outcome {
        data class Imported(val records: Int) : Outcome()
        data class Refused(val reason: String) : Outcome()
    }

    /** Reads the picked file. Returns null when it cannot be read or is too large. */
    fun readFile(context: Context, uri: Uri): ByteArray? = try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val bytes = input.readBytes()
            if (bytes.size > MAX_BYTES) null else bytes
        }
    } catch (e: IOException) {
        null
    }

    /** Decrypts [sealed] with [passphrase], validates it and imports it. The passphrase array is cleared afterwards. */
    fun importSealed(context: Context, sealed: ByteArray, passphrase: CharArray): Outcome {
        val plain = try {
            PracticeCrypto.open(sealed, passphrase)
        } catch (e: PracticeCrypto.CryptoException) {
            return Outcome.Refused(e.message ?: "The passphrase is wrong.")
        } finally {
            passphrase.fill(' ')
        }
        return importText(context, String(plain, Charsets.UTF_8))
    }

    fun importText(context: Context, text: String): Outcome {
        val batch = try {
            PracticeTransfer.decode(text, PracticeTransfer.PRACTICE_FILES)
        } catch (e: TransferException) {
            return Outcome.Refused(e.message ?: "The file is not a valid practice export.")
        }
        val ledger = context.getSharedPreferences(LEDGER, Context.MODE_PRIVATE)
        val ledgerKey = "batch_${batch.batchId}"
        if (ledger.getBoolean(ledgerKey, false)) return Outcome.Refused("This file was already imported.")

        if (hasPracticeHistory(context)) {
            return Outcome.Refused("This device already has practice records, so nothing was changed. Import on a fresh install.")
        }

        return try {
            for (file in batch.files) {
                val prefs = prefsFor(context, file.name, file.encrypted)
                val editor = prefs.edit().clear()
                for (e in file.entries) {
                    when (e.type) {
                        Type.STRING -> editor.putString(e.key, e.value)
                        Type.LONG -> editor.putLong(e.key, e.value.toLong())
                        Type.INT -> editor.putInt(e.key, e.value.toInt())
                        Type.BOOLEAN -> editor.putBoolean(e.key, e.value.toBoolean())
                    }
                }
                if (!editor.commit()) return Outcome.Refused("Saving ${file.name} failed. Import again on a fresh install.")
            }
            ledger.edit().putBoolean(ledgerKey, true).commit()
            Outcome.Imported(batch.files.sumOf { it.entries.size })
        } catch (e: NumberFormatException) {
            Outcome.Refused("The file has a value that is not a number where one is expected.")
        }
    }

    /** True when this device has recorded practice: a completed session or day, Japa counts, or Japa history. */
    fun hasPracticeHistory(context: Context): Boolean {
        for ((name, encrypted) in PracticeTransfer.PRACTICE_FILES) {
            val all = prefsFor(context, name, encrypted).all
            for ((key, value) in all) {
                if (key in DEFAULT_KEYS) continue
                when (value) {
                    is Boolean -> if (value) return true
                    is Long -> if (value > 0 && (key.contains("cumulative") || key.contains("daily") || key == "sessions")) return true
                    is Int -> if (value > 0 && key.contains("streak")) return true
                    is String -> if (key == "history" && value.length > 2) return true
                }
            }
        }
        return false
    }

    private fun prefsFor(context: Context, name: String, encrypted: Boolean): SharedPreferences =
        if (encrypted) SecurePrefs.get(context, name) else context.getSharedPreferences(name, Context.MODE_PRIVATE)
}
