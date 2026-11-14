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
        val settings = SettingsStore(context)
        val obj = settings.json()
        if (obj.optString("theme") != BLACK) {
            obj.put("theme", BLACK)
            settings.save(obj)
        }
        return BLACK
    }

    fun apply(context: Context, name: String = current(context)) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        val appContext = context.applicationContext
        val wallpaperState = appContext.getSharedPreferences(WALLPAPER_PREFS, Context.MODE_PRIVATE)
        if (wallpaperState.getBoolean(WALLPAPER_APPLIED, false)) return
        // Mark first: Samsung restarts the active launcher when its wallpaper changes.
        wallpaperState.edit().putBoolean(WALLPAPER_APPLIED, true).commit()
        Thread({
            val bmp = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
            try {
                bmp.eraseColor(Color.BLACK)
                val wm = WallpaperManager.getInstance(appContext)
                wm.setBitmap(bmp, null, false, WallpaperManager.FLAG_SYSTEM)
                wm.setBitmap(bmp, null, false, WallpaperManager.FLAG_LOCK)
            } catch (_: Exception) {
                // The fixed in-app black background remains available if Samsung rejects a wallpaper write.
                wallpaperState.edit().putBoolean(WALLPAPER_APPLIED, false).apply()
            } finally {
                bmp.recycle()
            }
        }, "paperdesk-wallpaper").start()
    }

    fun set(context: Context, name: String) {
        val obj = SettingsStore(context).json()
        obj.put("theme", BLACK)
        SettingsStore(context).save(obj)
        apply(context, BLACK)
    }

    private const val WALLPAPER_PREFS = "paperdesk_theme"
    private const val WALLPAPER_APPLIED = "black_wallpaper_applied"
}
