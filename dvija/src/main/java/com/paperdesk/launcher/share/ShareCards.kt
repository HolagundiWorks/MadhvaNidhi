package com.paperdesk.launcher.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.paperdesk.launcher.ui.DeskType
import java.io.File

/**
 * Renders share images (9:16, status-sized) for WhatsApp status and other apps.
 * Pure drawing on a Canvas; no network and no personal identifiers beyond what the card shows.
 */
object ShareCards {

    enum class Template(val label: String) {
        STREAK("Streak"),
        BADGES("Badges"),
        TODAY("Today")
    }

    data class Data(
        val matha: String,
        val date: String,
        val current: Int,
        val best: Int,
        val sessionsToday: Int,
        val tithi: String,
        val nakshatra: String,
        val badgeCount: Int,
        val badgeLines: List<String>
    )

    private const val WIDTH = 1080
    private const val HEIGHT = 1920
    private const val ACCENT = 0xFFFF4F18.toInt()
    private const val BG_TOP = 0xFF101010.toInt()
    private const val BG_BOTTOM = 0xFF262626.toInt()
    private const val TEXT = 0xFFFFFFFF.toInt()
    private const val MUTED = 0xFFB0B0B0.toInt()

    fun render(context: Context, template: Template, data: Data): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawRect(
            0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(),
            Paint().apply {
                shader = LinearGradient(0f, 0f, 0f, HEIGHT.toFloat(), BG_TOP, BG_BOTTOM, Shader.TileMode.CLAMP)
            }
        )
        val regular = DeskType.face(context, 400)
        val bold = DeskType.face(context, 700)

        text(canvas, "MadhvaNidhi", 170f, 60f, ACCENT, bold)
        text(canvas, data.matha, 240f, 40f, MUTED, regular)
        text(canvas, data.date, 330f, 40f, MUTED, regular)

        when (template) {
            Template.STREAK -> {
                text(canvas, "Sandhyavandana streak", 560f, 60f, TEXT, bold)
                text(canvas, data.current.toString(), 920f, 280f, ACCENT, bold)
                text(canvas, "days in a row", 1010f, 56f, TEXT, regular)
                text(canvas, "Best continuous: ${data.best} days", 1180f, 52f, TEXT, regular)
            }
            Template.BADGES -> {
                text(canvas, "Badges earned", 560f, 60f, TEXT, bold)
                text(canvas, data.badgeCount.toString(), 920f, 280f, ACCENT, bold)
                data.badgeLines.take(8).forEachIndexed { index, line ->
                    text(canvas, line, 1100f + index * 84f, 46f, TEXT, regular)
                }
            }
            Template.TODAY -> {
                text(canvas, "Today", 560f, 60f, TEXT, bold)
                text(canvas, "Sessions ${data.sessionsToday} of 3", 800f, 64f, ACCENT, bold)
                text(canvas, data.tithi, 1000f, 56f, TEXT, regular)
                text(canvas, data.nakshatra, 1090f, 52f, TEXT, regular)
            }
        }

        text(canvas, "Shared from MadhvaNidhi", HEIGHT - 140f, 38f, MUTED, regular)
        return bitmap
    }

    /** Saves the bitmap in the app cache and returns a share intent that grants read access. */
    fun shareIntent(context: Context, bitmap: Bitmap): Intent {
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "madhvanidhi_share.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".shareprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            // ClipData gives the share sheet a preview and carries the read grant to the receiving app.
            clipData = android.content.ClipData.newRawUri("share", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun text(canvas: Canvas, value: String, y: Float, size: Float, color: Int, face: Typeface) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = face
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(value, WIDTH / 2f, y, paint)
    }
}
