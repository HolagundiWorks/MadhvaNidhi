package com.paperdesk.launcher.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import androidx.core.content.FileProvider
import com.paperdesk.launcher.bridge.OsDataService
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DeskPhoto {
    fun file(context: Context, projectId: String, itemId: String): File {
        val name = ProjectStore(context).nameOf(projectId).ifBlank { "Inbox" }
        val dir = File(OsDataService.projectFolder(name), "Photos")
        dir.mkdirs()
        return File(dir, "$itemId.jpg")
    }

    fun uri(context: Context, file: File): Uri {
        file.parentFile?.mkdirs()
        if (!file.exists()) file.createNewFile()
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    fun decode(path: String, maxPx: Int): Bitmap? {
        if (path.isBlank()) return null
        val f = File(path)
        if (!f.exists() || f.length() == 0L) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        val w = bounds.outWidth.coerceAtLeast(1)
        val h = bounds.outHeight.coerceAtLeast(1)
        while (w / sample > maxPx || h / sample > maxPx) sample *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    fun bind(view: ImageView, path: String, maxPx: Int) {
        (view.tag as? Bitmap)?.recycle()
        val bmp = decode(path, maxPx)
        view.tag = bmp
        view.setImageBitmap(bmp)
        view.visibility = if (bmp != null) android.view.View.VISIBLE else android.view.View.GONE
    }

    fun label(): String {
        val fmt = SimpleDateFormat("d MMM yyyy  HH:mm", Locale("en", "IN"))
        return "PHOTO  ·  ${fmt.format(Date())}"
    }

    fun delete(path: String) {
        if (path.isBlank()) return
        runCatching { File(path).delete() }
    }
}
