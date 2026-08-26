package com.paperdesk.launcher.bridge

import android.content.Context
import android.provider.Settings
import com.paperdesk.launcher.OsRuntime
import org.json.JSONObject

object OsRadios {

    fun apply(context: Context, s: JSONObject) {
        val usb = s.optBoolean("usbDebug", true)
        setUsbDebug(context, usb)
        if (s.optBoolean("developerOptions", true) || usb) {
            putGlobal(context, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1)
        }
        putGlobal(context, Settings.Global.BLUETOOTH_ON, 0)
        putGlobal(context, "ble_scan_always_enabled", 0)
        OsRuntime.su("svc bluetooth disable")
        OsRuntime.su("settings put global bluetooth_on 0")
        OsRuntime.su("settings put global ble_scan_always_enabled 0")
        OsChrome.apply(context, s)
        val timeout = if (s.optBoolean("alwaysOn", false)) Int.MAX_VALUE else 120_000
        runCatching {
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, timeout)
        }
    }

    fun setUsbDebug(context: Context, on: Boolean) {
        if (!on) return
        putGlobal(context, Settings.Global.ADB_ENABLED, 1)
        putGlobal(context, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1)
        OsRuntime.su("settings put global adb_enabled 1")
        OsRuntime.su("settings put global development_settings_enabled 1")
        OsRuntime.su("setprop persist.sys.usb.config mtp,adb")
    }

    private fun putGlobal(context: Context, key: String, value: Int) {
        runCatching { Settings.Global.putInt(context.contentResolver, key, value) }
    }
}
