package com.paperdesk.launcher.bridge

import android.content.Context
import android.provider.Settings
import com.paperdesk.launcher.OsRuntime
import org.json.JSONObject

object OsChrome {
    private const val QS_TILES =
        "custom(com.paperdesk.launcher/com.paperdesk.launcher.bridge.SyncTileService)," +
            "Flashlight,SoundMode,RotationLock,Dnd"

    fun apply(context: Context, @Suppress("UNUSED_PARAMETER") s: JSONObject) {
        runCatching {
            Settings.Secure.putString(context.contentResolver, "sysui_qs_tiles", QS_TILES)
        }
        runCatching {
            Settings.Secure.putInt(context.contentResolver, "sysui_qqs_count", 5)
        }
        OsRuntime.su("settings put secure sysui_qs_tiles '$QS_TILES'")
        OsRuntime.su("settings put secure sysui_qqs_count 5")
    }

    fun applyShade(context: Context, locked: Boolean) {
        val policy = if (locked) "immersive.full=*" else ""
        runCatching {
            Settings.Global.putString(context.contentResolver, "policy_control", policy)
        }
        OsRuntime.su("settings put global policy_control '$policy'")
        if (locked) collapse(context)
    }

    fun collapse(context: Context) {
        runCatching {
            val sb = context.getSystemService("statusbar") ?: return
            sb.javaClass.getMethod("collapsePanels").invoke(sb)
        }
        OsRuntime.su("cmd statusbar collapse")
        OsRuntime.su("service call statusbar 2")
    }
}
