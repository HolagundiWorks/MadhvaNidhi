package com.paperdesk.launcher.data

import java.util.Calendar
import java.util.Locale

/** Civil dates grouped into the 15 tithis of each paksha, using the existing local model. */
object PanchangaCalendar {
    data class Cycle(val start: String, val end: String,
                     val shukla: List<List<String>>, val krishna: List<List<String>>)
    data class Tile(val tithis: List<Int>, val dates: List<String>)

    /** Only merge when the source explicitly assigns an adjacent tithi to the same date. */
    fun tiles(days: List<List<String>>): List<Tile> {
        require(days.size == 15)
        val result = mutableListOf<Tile>()
        days.forEachIndexed { index, dates ->
            val previous = result.lastOrNull()
            if (previous != null && previous.dates.any { it in dates }) {
                result[result.lastIndex] = Tile(previous.tithis + (index + 1), (previous.dates + dates).distinct())
            } else result += Tile(listOf(index + 1), dates.distinct())
        }
        return result
    }

    private val names = listOf("PRATIPADA", "DWITIYA", "TRITIYA", "CHATURTHI", "PANCHAMI",
        "SHASHTHI", "SAPTAMI", "ASHTAMI", "NAVAMI", "DASHAMI", "EKADASHI", "DWADASHI",
        "TRAYODASHI", "CHATURDASHI")
    fun cycle(selected: String): Cycle {
        fun dark(date: String) = PanchangaStore.local(date).paksha.contains("KRISHNA", true)
        var start = selected
        var found = false
        for (offset in 0 until 40) {
            val previous = PanchangaStore.shiftKey(start, -1)
            if (!dark(start) && dark(previous)) { found = true; break }
            start = previous
        }
        check(found) { "Unable to locate lunar-cycle boundary" }
        val shukla = List(15) { mutableListOf<String>() }
        val krishna = List(15) { mutableListOf<String>() }
        var current = start
        var end = start
        for (offset in 0 until 36) {
            val day = PanchangaStore.local(current)
            val tithi = day.tithi.uppercase(Locale.US)
            val index = if (PanchangaStore.isPurnima(tithi) || PanchangaStore.isAmavasya(tithi)) 14
                else names.indexOf(tithi)
            check(index >= 0) { "Unknown local tithi: $tithi" }
            (if (dark(current)) krishna else shukla)[index].add(current)
            end = current
            val next = PanchangaStore.shiftKey(current, 1)
            if (offset > 20 && dark(current) && !dark(next)) break
            current = next
        }
        return Cycle(start, end, shukla, krishna)
    }

    fun header(day: PanchangaDay, cycle: Cycle): String {
        val lang = IndicScript.Lang.KN
        val samvatsara = OsLang.script(PanchangaStore.samvatsaraFor(day), lang)
        val masa = OsLang.script(day.masa.uppercase(Locale.US).replace("KARTEEKA", "KARTIKA"), lang)
        val paksha = OsLang.script(day.paksha, lang)
        val first = PanchangaStore.parse(cycle.start)
        val last = PanchangaStore.parse(cycle.end)
        val firstMonth = OsLang.month(first, lang)
        val lastMonth = OsLang.month(last, lang)
        val firstYear = first.get(Calendar.YEAR)
        val lastYear = last.get(Calendar.YEAR)
        val civil = when {
            firstYear != lastYear -> "$firstMonth $firstYear–$lastMonth $lastYear"
            first.get(Calendar.MONTH) != last.get(Calendar.MONTH) -> "$firstMonth–$lastMonth $lastYear"
            else -> "$firstMonth $lastYear"
        }
        return "ಶ್ರೀ ಶಾಲಿವಾಹನ ಗತಶಕೆ ${PanchangaStore.shakaYear(day)} ಶ್ರೀ $samvatsara ನಾಮ ಸಂವತ್ಸರ ${masa}ಮಾಸ ${paksha}ಪಕ್ಷ · $civil"
    }
}
