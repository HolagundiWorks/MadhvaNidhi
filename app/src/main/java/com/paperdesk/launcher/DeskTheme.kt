package com.paperdesk.launcher

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.appcompat.app.AppCompatDelegate
import com.paperdesk.launcher.data.SettingsStore

object DeskTheme {
    const val BLACK = "black"
    const val PAPER = "paper"

    fun current(context: Context): String {
        val v = SettingsStore(context).json().optString("theme", BLACK)
        return if (v == PAPER) PAPER else BLACK
    }

    fun apply(context: Context, name: String = current(context)) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        runCatching {
            val bmp = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.WHITE)
            val wm = WallpaperManager.getInstance(context)
            wm.setBitmap(bmp)
            wm.setBitmap(bmp, null, false, WallpaperManager.FLAG_SYSTEM)
            wm.setBitmap(bmp, null, false, WallpaperManager.FLAG_LOCK)
        }
    }

    fun set(context: Context, name: String) {
        val obj = SettingsStore(context).json()
        obj.put("theme", if (name == PAPER) PAPER else BLACK)
        SettingsStore(context).save(obj)
        apply(context, name)
    }
}
