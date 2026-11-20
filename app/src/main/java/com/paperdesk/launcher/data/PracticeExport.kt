package com.paperdesk.launcher.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.paperdesk.launcher.data.PracticeTransfer.Entry
import com.paperdesk.launcher.data.PracticeTransfer.FileSnapshot
import com.paperdesk.launcher.data.PracticeTransfer.Type
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Builds the practice export from this app's preferences and shares it as a file through the share sheet.
 * Nothing is uploaded and the original data is not changed. Madhva Dvija imports the file.
 */
object PracticeExport {

    /** The export text for the current data. Throws if a stored value has a type the format does not carry. */
    fun build(context: Context, nowMillis: Long = System.currentTimeMillis()): String {
        val files = PracticeTransfer.PRACTICE_FILES.map { (name, encrypted) ->
            FileSnapshot(name, encrypted, read(context, name, encrypted))
        }
        return PracticeTransfer.encode(context.packageName, isoTime(nowMillis), files)
    }

    /** Writes the export to the app's share folder and opens the share sheet. Returns the file name. */
    fun share(context: Context, nowMillis: Long = System.currentTimeMillis()): String {
        val text = build(context, nowMillis)
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val name = "madhva-practice-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(nowMillis)}.mnexport"
        val file = File(dir, name).apply { writeText(text, Charsets.UTF_8) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.shareprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return name
    }

    private fun read(context: Context, name: String, encrypted: Boolean): List<Entry> {
        val prefs = if (encrypted) SecurePrefs.get(context, name)
        else context.getSharedPreferences(name, Context.MODE_PRIVATE)
        return prefs.all.map { (key, value) ->
            when (value) {
                is String -> Entry(Type.STRING, key, value)
                is Long -> Entry(Type.LONG, key, value.toString())
                is Int -> Entry(Type.INT, key, value.toString())
                is Boolean -> Entry(Type.BOOLEAN, key, value.toString())
                else -> throw IllegalStateException("Unsupported stored value for $key in $name")
            }
        }.sortedBy { it.key }
    }

    private fun isoTime(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(millis)
}
