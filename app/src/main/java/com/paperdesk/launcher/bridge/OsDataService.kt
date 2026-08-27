package com.paperdesk.launcher.bridge

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.paperdesk.launcher.data.SettingsStore
import com.paperdesk.launcher.data.JapaStore
import com.paperdesk.launcher.data.ProfileStore
import com.paperdesk.launcher.weather.WeatherClient
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object OsDataService {
    const val ROOT = "/sdcard/Vishwakarma"
    const val BUNDLE = "$ROOT/os-bundle.json"
    const val INBOX = "$ROOT/inbox/os-bundle.json"
    const val SETTINGS_INBOX = "$ROOT/inbox/settings.json"
    const val WEATHER = "$ROOT/weather.json"
    const val WEATHER_INBOX = "$ROOT/inbox/weather.json"
    const val DEVICE = "$ROOT/device.json"
    const val PROJECTS_DIR = "$ROOT/Projects"
    const val BACKUPS = "$ROOT/backups"

    fun rootDir(): File {
        val ext = Environment.getExternalStorageDirectory()
        val dir = File(ext, "Vishwakarma")
        return if (dir.mkdirs() || dir.isDirectory) dir else File(ROOT)
    }

    fun ensureLayout(context: Context) {
        val root = rootDir()
        File(root, "inbox").mkdirs()
        File(root, "backups").mkdirs()
        File(root, "Projects").mkdirs()
        File(root, "gita").mkdirs()
        val names = prefsArray(context, "paperdesk_projects")
        for (i in 0 until names.length()) {
            val name = names.optJSONObject(i)?.optString("name").orEmpty()
            projectFolder(name)
        }
    }

    fun projectFolder(name: String): File {
        val folder = sanitize(name.ifBlank { "Project" })
        val base = File(File(rootDir(), "Projects"), folder)
        listOf("Measurements", "Notes", "Drawings", "PDFs", "Photos", "Minutes").forEach {
            File(base, it).mkdirs()
        }
        return base
    }

    fun export(context: Context): File {
        ensureLayout(context)
        val bundle = JSONObject()
            .put("version", 1)
            .put("exportedAt", System.currentTimeMillis())
            .put("os", "AcharyaOS")
            .put("osVersion", "2.5.0")
            .put("device", deviceJson(context))
            .put("settings", SettingsStore(context).json())
            .put("projects", prefsArray(context, "paperdesk_projects"))
            .put("schedules", prefsArray(context, "paperdesk_events"))
            .put("todos", prefsArray(context, "paperdesk_tasks"))
            .put("notes", prefsArray(context, "paperdesk_notes"))
            .put("minutes", prefsArray(context, "paperdesk_minutes"))
            .put("measurements", prefsArray(context, "paperdesk_measure"))
            .put("alarms", prefsArray(context, "paperdesk_alarms"))
            .put("calculatorHistory", prefsArray(context, "paperdesk_calc"))
            .put("profiles", ProfileStore(context).exportJson())
            .put("japaByProfile", JapaStore.exportAll(context))
        WeatherClient.storedJson(context)?.let { bundle.put("weather", it) }
        val root = rootDir()
        val out = File(root, "os-bundle.json")
        out.parentFile?.mkdirs()
        out.writeText(bundle.toString(2))
        File(root, "device.json").writeText(deviceJson(context).toString(2))
        return out
    }

    fun requestCompanionSync(context: Context): File {
        val out = export(context)
        File(rootDir(), "sync-request.json").writeText(
            JSONObject().put("at", System.currentTimeMillis()).toString()
        )
        return out
    }

    fun importInbox(context: Context): Boolean {
        val inbox = File(rootDir(), "inbox/os-bundle.json")
        if (!inbox.exists()) return false
        return importText(context, inbox.readText())
    }

    fun importText(context: Context, raw: String): Boolean {
        val obj = runCatching { JSONObject(raw) }.getOrNull() ?: return false
        writePrefs(context, "paperdesk_projects", obj.optJSONArray("projects"))
        writePrefs(context, "paperdesk_events", obj.optJSONArray("schedules"))
        writePrefs(context, "paperdesk_tasks", obj.optJSONArray("todos"))
        writePrefs(context, "paperdesk_notes", obj.optJSONArray("notes"))
        writePrefs(context, "paperdesk_minutes", obj.optJSONArray("minutes"))
        writePrefs(context, "paperdesk_measure", obj.optJSONArray("measurements"))
        writePrefs(context, "paperdesk_alarms", obj.optJSONArray("alarms"))
        writePrefs(context, "paperdesk_calc", obj.optJSONArray("calculatorHistory"))
        obj.optJSONObject("profiles")?.let { ProfileStore(context).importJson(it) }
        obj.optJSONObject("japaByProfile")?.let { JapaStore.importAll(context, it) }
            ?: obj.optJSONObject("japa")?.let { JapaStore(context).importJson(it) }
        obj.optJSONObject("settings")?.let {
            SettingsStore(context).save(it)
        }
        obj.optJSONObject("weather")?.let { WeatherClient.saveJson(context, it) }
        applyWeatherFile(context)
        ensureLayout(context)
        return true
    }

    fun applyWeatherFile(context: Context): Boolean {
        ensureLayout(context)
        return WeatherClient.ingestUsb(context)
    }

    fun applySettingsFile(context: Context): Boolean {
        val f = File(rootDir(), "inbox/settings.json")
        if (!f.exists()) return false
        val obj = runCatching { JSONObject(f.readText()) }.getOrNull() ?: return false
        SettingsStore(context).save(obj)
        return true
    }

    fun deviceJson(context: Context): JSONObject {
        val stat = StatFs(Environment.getDataDirectory().path)
        val block = stat.blockSizeLong
        val total = stat.blockCountLong * block
        val avail = stat.availableBlocksLong * block
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val bundle = File(rootDir(), "os-bundle.json")
        return JSONObject()
            .put("name", Build.MODEL)
            .put("manufacturer", Build.MANUFACTURER)
            .put("android", Build.VERSION.RELEASE)
            .put("sdk", Build.VERSION.SDK_INT)
            .put("os", "AcharyaOS")
            .put("osVersion", "2.5.0")
            .put("build", Build.DISPLAY)
            .put("storageTotal", total)
            .put("storageAvail", avail)
            .put("battery", bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY))
            .put("databaseBytes", bundle.takeIf { it.exists() }?.length() ?: 0L)
    }

    private fun prefsArray(context: Context, name: String): JSONArray {
        val raw = context.getSharedPreferences(name, Context.MODE_PRIVATE).getString("items", "[]") ?: "[]"
        return runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
    }

    private fun writePrefs(context: Context, name: String, arr: JSONArray?) {
        if (arr == null) return
        context.getSharedPreferences(name, Context.MODE_PRIVATE)
            .edit().putString("items", arr.toString()).commit()
    }

    private fun sanitize(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]+"), "_").trim('_').ifBlank { "Project" }
}
