package com.paperdesk.launcher.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.UUID

enum class MeasureUom(val label: String, val run: String, val area: String, val vol: String) {
    MM("mm", "mm", "mm2", "mm3"),
    CM("cm", "cm", "cm2", "cm3"),
    M("m", "m", "m2", "m3"),
    FT("ft", "ft", "ft2", "ft3"),
    IN("in", "in", "in2", "in3");

    fun next(): MeasureUom {
        val all = values()
        return all[(ordinal + 1) % all.size]
    }
}

enum class MeasureKind(val label: String, val fullLabel: String) {
    RUN("RUN", "RUNNING LENGTH"),
    AREA("AREA", "AREA"),
    VOL("VOL", "VOLUME");

    fun next(): MeasureKind {
        val all = values()
        return all[(ordinal + 1) % all.size]
    }
}

data class MeasureItem(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String = "",
    val description: String = "",
    val length: String = "",
    val breadth: String = "",
    val height: String = "",
    val uom: MeasureUom = MeasureUom.M,
    val kind: MeasureKind = MeasureKind.AREA,
    val autoKind: Boolean = true
) {
    fun filled(s: String) = (s.toDoubleOrNull() ?: 0.0) != 0.0 || s.isNotBlank()

    fun inferredKind(): MeasureKind = when {
        filled(height) -> MeasureKind.VOL
        filled(breadth) -> MeasureKind.AREA
        else -> MeasureKind.RUN
    }

    fun effectiveKind(): MeasureKind = if (autoKind) inferredKind() else kind

    fun qty(): Double {
        val l = length.toDoubleOrNull() ?: 0.0
        val b = breadth.toDoubleOrNull() ?: 0.0
        val h = height.toDoubleOrNull() ?: 0.0
        return when (effectiveKind()) {
            MeasureKind.RUN -> l
            MeasureKind.AREA -> l * b
            MeasureKind.VOL -> l * b * h
        }
    }

    fun qtyLabel(): String {
        val k = effectiveKind()
        val unit = when (k) {
            MeasureKind.RUN -> uom.run
            MeasureKind.AREA -> uom.area
            MeasureKind.VOL -> uom.vol
        }
        return "${fmt.format(qty())} $unit"
    }

    companion object {
        private val fmt = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

        fun totals(items: List<MeasureItem>): String {
            var runM = 0.0
            var areaM2 = 0.0
            var volM3 = 0.0
            items.forEach { it ->
                val q = it.qty()
                val m = when (it.uom) {
                    MeasureUom.MM -> 0.001
                    MeasureUom.CM -> 0.01
                    MeasureUom.M -> 1.0
                    MeasureUom.FT -> 0.3048
                    MeasureUom.IN -> 0.0254
                }
                when (it.effectiveKind()) {
                    MeasureKind.RUN -> runM += q * m
                    MeasureKind.AREA -> areaM2 += q * m * m
                    MeasureKind.VOL -> volM3 += q * m * m * m
                }
            }
            val parts = mutableListOf<String>()
            if (runM != 0.0) parts += "${fmt.format(runM)} m"
            if (areaM2 != 0.0) parts += "${fmt.format(areaM2)} m2"
            if (volM3 != 0.0) parts += "${fmt.format(volM3)} m3"
            return if (parts.isEmpty()) "" else "TOTAL  " + parts.joinToString("   ")
        }
    }
}

class MeasureStore(context: Context) {
    private val prefs = context.getSharedPreferences("paperdesk_measure", Context.MODE_PRIVATE)

    fun load(): MutableList<MeasureItem> {
        val raw = prefs.getString("items", "[]") ?: "[]"
        val arr = JSONArray(raw)
        val out = mutableListOf<MeasureItem>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += MeasureItem(
                id = o.optString("id", UUID.randomUUID().toString()),
                projectId = o.optString("projectId", ""),
                description = o.optString("description", ""),
                length = o.optString("length", ""),
                breadth = o.optString("breadth", ""),
                height = o.optString("height", ""),
                uom = runCatching { MeasureUom.valueOf(o.optString("uom", "M")) }.getOrDefault(MeasureUom.M),
                kind = runCatching { MeasureKind.valueOf(o.optString("kind", "AREA")) }.getOrDefault(MeasureKind.AREA),
                autoKind = o.optBoolean("autoKind", true)
            )
        }
        return out
    }

    fun forProject(projectId: String): List<MeasureItem> =
        load().filter { it.projectId == projectId }

    fun get(id: String): MeasureItem? = load().firstOrNull { it.id == id }

    fun deleteProject(projectId: String) {
        save(load().filter { it.projectId != projectId })
    }

    fun upsert(item: MeasureItem) {
        val list = load()
        val i = list.indexOfFirst { it.id == item.id }
        if (i >= 0) list[i] = item else list += item
        save(list)
    }

    fun save(items: List<MeasureItem>) {
        val arr = JSONArray()
        items.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("projectId", it.projectId)
                    .put("description", it.description)
                    .put("length", it.length)
                    .put("breadth", it.breadth)
                    .put("height", it.height)
                    .put("uom", it.uom.name)
                    .put("kind", it.kind.name)
                    .put("autoKind", it.autoKind)
            )
        }
        prefs.edit().putString("items", arr.toString()).apply()
    }
}
