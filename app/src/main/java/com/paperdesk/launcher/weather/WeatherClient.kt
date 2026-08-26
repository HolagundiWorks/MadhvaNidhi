package com.paperdesk.launcher.weather

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.util.Locale

data class WeatherSnapshot(
    val tempC: Double,
    val highC: Double,
    val lowC: Double,
    val humidity: Int,
    val windKmh: Double,
    val condition: String,
    val kind: WeatherKind,
    val isDay: Boolean,
    val moonPhase: Float,
    val updatedAt: Long,
    val cached: Boolean = false
)

enum class WeatherKind { SUN, CLOUD_SUN, CLOUD, RAIN, STORM, FOG, SNOW }

object WeatherClient {
    const val PREFS = "paperdesk_weather"

    fun fetch(context: Context): WeatherSnapshot? {
        ingestUsb(context)
        return cached(context)
    }

    fun ingestUsb(context: Context): Boolean {
        val root = File(android.os.Environment.getExternalStorageDirectory(), "Vishwakarma")
        val inbox = File(root, "inbox/weather.json")
        val live = File(root, "weather.json")
        val src = when {
            inbox.exists() -> inbox
            live.exists() -> live
            else -> return false
        }
        val raw = runCatching { src.readText() }.getOrNull() ?: return false
        if (raw.isBlank()) return false
        save(context, raw)
        if (inbox.exists()) {
            live.parentFile?.mkdirs()
            runCatching { live.writeText(raw) }
        }
        return parse(raw) != null
    }

    fun saveJson(context: Context, obj: JSONObject) {
        save(context, obj.toString())
        val root = File(android.os.Environment.getExternalStorageDirectory(), "Vishwakarma")
        root.mkdirs()
        runCatching { File(root, "weather.json").writeText(obj.toString(2)) }
    }

    fun cached(context: Context): WeatherSnapshot? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString("json", null) ?: return null
        val at = prefs.getLong("at", 0L)
        return parse(raw)?.copy(cached = true, updatedAt = at)
    }

    fun storedJson(context: Context): JSONObject? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("json", null) ?: return null
        return runCatching { JSONObject(raw) }.getOrNull()
    }

    private fun save(context: Context, json: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("json", json)
            .putLong("at", System.currentTimeMillis())
            .commit()
    }

    fun parse(raw: String): WeatherSnapshot? {
        return try {
            val o = JSONObject(raw)
            if (o.has("tempC")) return snapshot(o)
            val cur = o.getJSONObject("current")
            val daily = o.getJSONObject("daily")
            val high = daily.getJSONArray("temperature_2m_max").optDouble(0)
            val low = daily.getJSONArray("temperature_2m_min").optDouble(0)
            val code = cur.getInt("weather_code")
            val isDay = if (cur.has("is_day")) {
                cur.getInt("is_day") == 1
            } else {
                val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                h in 6..17
            }
            WeatherSnapshot(
                tempC = cur.getDouble("temperature_2m"),
                highC = high,
                lowC = low,
                humidity = cur.getInt("relative_humidity_2m"),
                windKmh = cur.getDouble("wind_speed_10m"),
                condition = condition(code, isDay),
                kind = kind(code),
                isDay = isDay,
                moonPhase = moonPhase(),
                updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun snapshot(o: JSONObject): WeatherSnapshot {
        val isDay = o.optBoolean("isDay", true)
        val kindName = o.optString("kind", "CLOUD")
        val kind = runCatching { WeatherKind.valueOf(kindName) }.getOrDefault(WeatherKind.CLOUD)
        return WeatherSnapshot(
            tempC = o.getDouble("tempC"),
            highC = o.optDouble("highC", o.getDouble("tempC")),
            lowC = o.optDouble("lowC", o.getDouble("tempC")),
            humidity = o.optInt("humidity", 0),
            windKmh = o.optDouble("windKmh", 0.0),
            condition = o.optString("condition").ifBlank { if (!isDay) moonPhaseName(moonPhase()) else "Hospet" },
            kind = kind,
            isDay = isDay,
            moonPhase = if (o.has("moonPhase")) o.getDouble("moonPhase").toFloat() else moonPhase(),
            updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
        )
    }

    fun formatTemp(v: Double): String = String.format(Locale.US, "%.0f°", v)

    fun kind(code: Int): WeatherKind = when (code) {
        0, 1 -> WeatherKind.SUN
        2 -> WeatherKind.CLOUD_SUN
        3 -> WeatherKind.CLOUD
        45, 48 -> WeatherKind.FOG
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> WeatherKind.RAIN
        71, 73, 75, 77, 85, 86 -> WeatherKind.SNOW
        95, 96, 99 -> WeatherKind.STORM
        else -> WeatherKind.CLOUD
    }

    fun moonPhase(atMs: Long = System.currentTimeMillis()): Float {
        val synodic = 29.530588853
        val jd = atMs / 86_400_000.0 + 2_440_587.5
        val knownNew = 2_451_550.1
        val age = ((jd - knownNew) % synodic + synodic) % synodic
        return (age / synodic).toFloat()
    }

    fun moonPhaseName(phase: Float): String {
        val p = ((phase % 1f) + 1f) % 1f
        return when {
            p < 0.03f || p >= 0.97f -> "New moon"
            p < 0.22f -> "Waxing crescent"
            p < 0.28f -> "First quarter"
            p < 0.47f -> "Waxing gibbous"
            p < 0.53f -> "Full moon"
            p < 0.72f -> "Waning gibbous"
            p < 0.78f -> "Last quarter"
            else -> "Waning crescent"
        }
    }

    private fun condition(code: Int, isDay: Boolean): String {
        if (!isDay && (code == 0 || code == 1)) return moonPhaseName(moonPhase())
        return when (code) {
            0 -> "Clear sky"
            1 -> "Mainly clear"
            2 -> "Partly cloudy"
            3 -> "Overcast"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            56, 57 -> "Freezing drizzle"
            61, 63, 65 -> "Rain"
            66, 67 -> "Freezing rain"
            71, 73, 75, 77 -> "Snow"
            80, 81, 82 -> "Rain showers"
            85, 86 -> "Snow showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm with hail"
            else -> "Hospet"
        }
    }
}
