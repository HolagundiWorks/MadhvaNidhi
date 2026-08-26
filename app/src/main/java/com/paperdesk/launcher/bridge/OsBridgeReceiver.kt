package com.paperdesk.launcher.bridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.paperdesk.launcher.data.SettingsStore
import com.paperdesk.launcher.ui.LauncherActivity

class OsBridgeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        runCatching {
            when (intent?.action) {
                ACTION_EXPORT -> OsDataService.export(context)
                ACTION_IMPORT -> {
                    OsDataService.importInbox(context)
                    reopen(context)
                }
                ACTION_APPLY_SETTINGS -> {
                    OsDataService.applySettingsFile(context)
                    applyRadios(context)
                    com.paperdesk.launcher.DeskTheme.apply(context)
                    reopen(context)
                }
                ACTION_APPLY_WEATHER -> {
                    OsDataService.applyWeatherFile(context)
                    refreshDesk(context)
                }
                ACTION_COMMAND -> handleCommand(context, intent.getStringExtra(EXTRA_OP).orEmpty())
            }
        }
    }

    private fun handleCommand(context: Context, op: String) {
        when (op) {
            "export" -> OsDataService.export(context)
            "import" -> {
                OsDataService.importInbox(context)
                reopen(context)
            }
            "weather" -> {
                OsDataService.applyWeatherFile(context)
                refreshDesk(context)
            }
        }
    }

    private fun applyRadios(context: Context) {
        val s = SettingsStore(context).json()
        OsRadios.apply(context, s)
        runCatching {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                if (s.optBoolean("autoRotate", false)) 1 else 0
            )
        }
    }

    private fun refreshDesk(context: Context) {
        val i = Intent(context, LauncherActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        context.startActivity(i)
    }

    private fun reopen(context: Context) {
        val i = Intent(context, LauncherActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        context.startActivity(i)
    }

    companion object {
        const val ACTION_EXPORT = "com.paperdesk.os.EXPORT"
        const val ACTION_IMPORT = "com.paperdesk.os.IMPORT"
        const val ACTION_APPLY_SETTINGS = "com.paperdesk.os.APPLY_SETTINGS"
        const val ACTION_APPLY_WEATHER = "com.paperdesk.os.APPLY_WEATHER"
        const val ACTION_COMMAND = "com.paperdesk.os.COMMAND"
        const val EXTRA_OP = "op"
    }
}
