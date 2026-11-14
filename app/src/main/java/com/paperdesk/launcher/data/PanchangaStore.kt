package com.paperdesk.launcher.data

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Locale
import kotlin.math.floor
import kotlin.math.sin

data class PanchangaDay(
    val date: String,
    val samvatsara: String,
    val ayana: String,
    val ritu: String,
    val masa: String,
    val paksha: String,
    val tithi: String,
    val vara: String,
    val nakshatra: String,
    val yoga: String,
    val karana: String,
    val source: String
)

/** Tradition-aware Panchanga. Official live data is used only where a provider exposes it. */
object PanchangaStore {
    private const val PREFS = "paperdesk_panchanga"
    private const val SRS = "https://srsmatha.org/panchanga.php?pdate="

    /** Hot in-memory cache so Sankalpa / dashboard / desk do not re-hit disk/network. */
    private val memory = object : LinkedHashMap<String, PanchangaDay>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, PanchangaDay>?): Boolean = size > 32
    }

    fun today(context: Context): PanchangaDay = forDate(context, dateKey())

    fun forDate(context: Context, date: String): PanchangaDay {
        val tradition = PanchangaTraditions.find(SettingsStore(context).panchangaTradition())
        val memKey = "${tradition.id}|$date"
        synchronized(memory) { memory[memKey] }?.let { return it }
        cached(context, date, tradition.id)?.let {
            synchronized(memory) { memory[memKey] = it }
            return it
        }
        if (tradition.liveDailyData) {
            fetchSrs(date)?.copy(source = "${tradition.kn} · ಅಧಿಕೃತ")?.let {
                save(context, it, tradition.id)
                synchronized(memory) { memory[memKey] = it }
                return it
            }
        }
        return local(date).copy(source = "${tradition.kn} · ಸ್ಥಳೀಯ ಅಂದಾಜು (06:00)").also {
            synchronized(memory) { memory[memKey] = it }
        }
    }

    fun local(date: String): PanchangaDay = compute(date)

    fun samvatsaraFor(day: PanchangaDay): String =
        SAMVAT.firstOrNull { name -> Regex("\\b$name\\b", RegexOption.IGNORE_CASE).containsMatchIn(day.samvatsara) }
            ?: local(day.date).samvatsara

    fun shakaYear(day: PanchangaDay): Int {
        val year = parse(day.date).get(Calendar.YEAR)
        val index = SAMVAT.indexOf(samvatsaraFor(day))
        val previous = ((year - 1 - 1987) % 60 + 60) % 60
        return year - if (index == previous) 79 else 78
    }


    fun shortTithi(tithi: String): String {
        val t = tithi.uppercase(Locale.US)
        return when {
            "PURNIMA" in t -> "PURN"
            "AMAVASYA" in t -> "AMA"
            "PRATIPADA" in t -> "PRA"
            "DWITIYA" in t -> "DWI"
            "TRITIYA" in t -> "TRI"
            "CHATURTHI" in t -> "CHT"
            "PANCHAMI" in t -> "PAN"
            "SHASHTHI" in t -> "SHA"
            "SAPTAMI" in t -> "SAP"
            "ASHTAMI" in t -> "ASH"
            "NAVAMI" in t -> "NAV"
            "DASHAMI" in t -> "DAS"
            "EKADASHI" in t -> "EKA"
            "DWADASHI" in t -> "DWA"
            "TRAYODASHI" in t -> "TRA"
            "CHATURDASHI" in t -> "CHD"
            else -> t.take(3)
        }
    }

    private fun cached(context: Context, date: String, tradition: String): PanchangaDay? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString("json_v2_${tradition}_$date", null) ?: return null
        val day = parseJson(raw) ?: return null
        return if (day.date == date) day else null
    }

    private fun save(context: Context, day: PanchangaDay, tradition: String) {
        val obj = JSONObject()
            .put("date", day.date)
            .put("samvatsara", day.samvatsara)
            .put("ayana", day.ayana)
            .put("ritu", day.ritu)
            .put("masa", day.masa)
            .put("paksha", day.paksha)
            .put("tithi", day.tithi)
            .put("vara", day.vara)
            .put("nakshatra", day.nakshatra)
            .put("yoga", day.yoga)
            .put("karana", day.karana)
            .put("source", day.source)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("json_v2_${tradition}_${day.date}", obj.toString())
            .apply()
    }

    private fun fetchSrs(date: String): PanchangaDay? {
        var conn: HttpURLConnection? = null
        return try {
            conn = URL(SRS + date).openConnection() as HttpURLConnection
            conn.connectTimeout = 12000
            conn.readTimeout = 15000
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "MadhvaNidhi/1.0")
            if (conn.responseCode != 200) return null
            val html = conn.inputStream.bufferedReader().use { it.readText() }
            parseSrs(html, date)
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    fun parseSrs(html: String, date: String): PanchangaDay? {
        val block = Regex("<div><p>(.*?)</p></div>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            .find(html)?.groupValues?.get(1)
        val t = (block ?: html)
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace(Regex("[,\\s]+"), " ")
            .trim()
        if (!t.contains("Tithou", ignoreCase = true)) return null
        val sam = Regex("(?:Sri\\s+)?(\\S+?)samvatsare", RegexOption.IGNORE_CASE).find(t)?.groupValues?.get(1)
        val ayana = Regex("(Dakshinayaana|Dakshinayana|Uttarayana\\w*)", RegexOption.IGNORE_CASE).find(t)?.groupValues?.get(1)
        val ritu = Regex("(Vasanta|Grishma|Varsha|Sharad|Hemanta|Shishira|Sisira)Rutu", RegexOption.IGNORE_CASE)
            .find(t)?.groupValues?.get(1)
        val masa = Regex("(\\S+?)Mase", RegexOption.IGNORE_CASE).find(t)?.groupValues?.get(1)
        val paksha = Regex("(Shukla|Krishna)Pakshe", RegexOption.IGNORE_CASE).find(t)?.groupValues?.get(1)
        val tithi = Regex("(\\S+)\\s+Shubha\\s+Tithou", RegexOption.IGNORE_CASE).find(t)?.groupValues?.get(1)
        val vara = Regex("(Sunday|Monday|Tuesday|Wednesday|Thursday|Friday|Saturday)").find(t)?.groupValues?.get(1)
        val nak = Regex("(\\S+)\\s+Nakshatre", RegexOption.IGNORE_CASE).find(t)?.groupValues?.get(1)
        val yoga = Regex("(\\S+)\\s+Yoge", RegexOption.IGNORE_CASE).find(t)?.groupValues?.get(1)
        val karana = Regex("(\\S+)\\s+Karana", RegexOption.IGNORE_CASE).find(t)?.groupValues?.get(1)
        if (tithi.isNullOrBlank() || masa.isNullOrBlank()) return null
        return PanchangaDay(
            date = date,
            samvatsara = neat(sam),
            ayana = neat(ayana)
                .replace("DAKSHINAYAANA", "DAKSHINAYANA")
                .replace("UTTARAYANA", "UTTARAYANA"),
            ritu = neat(ritu),
            masa = neat(masa).replace("SHRAAVANA", "SHRAVANA"),
            paksha = neat(paksha),
            tithi = neat(tithi),
            vara = neat(vara),
            nakshatra = neat(nak).replace("SHRAAVANA", "SHRAVANA"),
            yoga = neat(yoga),
            karana = canonicalKarana(neat(karana)),
            source = "SRS MANTRALAYA"
        )
    }

    private fun parseJson(raw: String): PanchangaDay? {
        return runCatching {
            val o = JSONObject(raw)
            PanchangaDay(
                date = o.getString("date"),
                samvatsara = o.optString("samvatsara"),
                ayana = o.optString("ayana"),
                ritu = o.optString("ritu"),
                masa = o.optString("masa"),
                paksha = o.optString("paksha"),
                tithi = o.optString("tithi"),
                vara = o.optString("vara"),
                nakshatra = o.optString("nakshatra"),
                yoga = o.optString("yoga"),
                karana = o.optString("karana"),
                source = o.optString("source")
            )
        }.getOrNull()
    }

    private fun neat(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw.replace("-", " ").uppercase(Locale.US)
    }

    fun dateKey(cal: Calendar = Calendar.getInstance()): String {
        return String.format(Locale.US, "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }

    /** Selected civil date at 06:00 in the device time zone; invalid input falls back safely. */
    fun parse(date: String): Calendar {
        fun fallback() = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 6); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val parts = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$").matchEntire(date)?.groupValues ?: return fallback()
        return runCatching {
            Calendar.getInstance().apply {
                clear()
                isLenient = false
                set(parts[1].toInt(), parts[2].toInt() - 1, parts[3].toInt(), 6, 0, 0)
                timeInMillis // validate month and day without lenient rollover
            }
        }.getOrElse { fallback() }
    }

    fun shiftKey(date: String, days: Int): String {
        val c = parse(date)
        c.add(Calendar.DAY_OF_MONTH, days)
        return dateKey(c)
    }

    fun isEkadasi(tithi: String): Boolean {
        val t = tithi.uppercase(Locale.US)
        return "EKADASHI" in t || "EKADASI" in t
    }

    fun isPurnima(tithi: String): Boolean {
        val t = tithi.uppercase(Locale.US)
        return "PURNIMA" in t || "POORNIMA" in t || "POURNA" in t || "HUNNIME" in t
    }

    fun isAmavasya(tithi: String): Boolean {
        val t = tithi.uppercase(Locale.US)
        return "AMAVASYA" in t || "AMAVASYE" in t
    }

    fun isMarked(tithi: String): Boolean =
        isEkadasi(tithi) || isPurnima(tithi) || isAmavasya(tithi)

    /** Approximate Chandra rashi from the same local sidereal Moon used for nakshatra/pada. */
    fun chandraRashi(date: String): String {
        val jd = julian(parse(date))
        val moon = siderealLongitude(moonLon(jd), jd)
        return RASHI[(floor(norm(moon) / 30.0).toInt() % 12)]
    }

    /** Approximate sidereal Surya rashi at 06:00 device-local time. */
    fun suryaRashi(date: String): String {
        val jd = julian(parse(date))
        val sun = siderealLongitude(sunLon(jd), jd)
        return RASHI[(floor(norm(sun) / 30.0).toInt() % 12)]
    }

    /** Approximate sidereal quarter at 06:00 local time; never an official-provider value. */
    fun nakshatraPada(date: String): Int {
        val jd = julian(parse(date))
        return padaFromSiderealLongitude(siderealLongitude(moonLon(jd), jd))
    }

    /** Do not attach a local quarter to a different provider-reported nakshatra. */
    fun nakshatraPada(day: PanchangaDay): Int? =
        if (nakshatraKey(day.nakshatra) == nakshatraKey(local(day.date).nakshatra)) nakshatraPada(day.date) else null

    private fun nakshatraKey(name: String): String = name.uppercase(Locale.US)
        .replace(Regex("[^A-Z]"), "").replace("AA", "A")
        .replace("DHANISHTHA", "DHANISHTA").replace("SHATABISHA", "SHATABHISHA")

    internal fun padaFromSiderealLongitude(longitude: Double): Int {
        require(longitude.isFinite())
        val quarter = floor(norm(longitude) / (360.0 / 108.0)).toInt()
        return quarter % 4 + 1
    }

    /** Low-precision Lahiri-like mean correction, not a tradition-specific ephemeris. */
    internal fun siderealLongitude(tropical: Double, jd: Double): Double {
        val centuries = (jd - 2451545.0) / 36525.0
        val ayanamsha = 23.857092333333 + 1.3969713 * centuries + 0.0003086 * centuries * centuries
        return norm(tropical - ayanamsha)
    }

    /** Zero-based half-tithi slots: fixed 0 and 57–59, seven names repeated in 1–56. */
    internal fun karanaFromElongation(elongation: Double): String {
        require(elongation.isFinite())
        val half = floor(norm(elongation) / 6.0).toInt()
        return when (half) {
            0 -> "KIMSTUGHNA"
            57 -> "SHAKUNI"
            58 -> "CHATUSHPADA"
            59 -> "NAGA"
            else -> KARANA[(half - 1) % 7]
        }
    }

    internal fun canonicalKarana(raw: String): String = when (raw.uppercase(Locale.US).replace(Regex("[^A-Z]"), "")) {
        "BAVA", "BHAVA" -> "BAVA"
        "BALAVA", "BAALAVA" -> "BALAVA"
        "KAULAVA" -> "KAULAVA"
        "TAITILA", "TAITULA" -> "TAITILA"
        "GARA", "GARAJA", "GARIJA", "GARI" -> "GARA"
        "VANIJA", "VANIJ", "VANIJYA" -> "VANIJA"
        "VISHTI", "VISTI", "BHADRA" -> "VISHTI"
        "SHAKUNI", "SAKUNI" -> "SHAKUNI"
        "CHATUSHPADA", "CHATUSPADA" -> "CHATUSHPADA"
        "NAGA", "NAAGA" -> "NAGA"
        "KIMSTUGHNA", "KIMSTHUGHNA", "KINSTUGHNA" -> "KIMSTUGHNA"
        else -> ""
    }

    /** Fifteen civil days of the current paksha: Amavasya→Purnima or Purnima→Amavasya. */
    fun pakshaSpan(date: String): List<String> {
        val start = pakshaStart(date)
        return (0 until 15).map { shiftKey(start, it) }
    }

    fun pakshaStart(date: String): String {
        val shukla = "KRISHNA" !in local(date).paksha.uppercase(Locale.US)
        var cur = date
        repeat(20) {
            val t = local(cur).tithi.uppercase(Locale.US)
            val marker = if (shukla) "AMAVASYA" in t else "PURNIMA" in t
            if (marker) return shiftKey(cur, 1)
            cur = shiftKey(cur, -1)
        }
        return date
    }

    private val TITHI = arrayOf(
        "PRATIPADA", "DWITIYA", "TRITIYA", "CHATURTHI", "PANCHAMI", "SHASHTHI", "SAPTAMI",
        "ASHTAMI", "NAVAMI", "DASHAMI", "EKADASHI", "DWADASHI", "TRAYODASHI", "CHATURDASHI", "PURNIMA"
    )
    private val TITHI_K = arrayOf(
        "PRATIPADA", "DWITIYA", "TRITIYA", "CHATURTHI", "PANCHAMI", "SHASHTHI", "SAPTAMI",
        "ASHTAMI", "NAVAMI", "DASHAMI", "EKADASHI", "DWADASHI", "TRAYODASHI", "CHATURDASHI", "AMAVASYA"
    )
    private val NAK = arrayOf(
        "ASHWINI", "BHARANI", "KRITTIKA", "ROHINI", "MRIGASHIRA", "ARDRA", "PUNARVASU", "PUSHYA",
        "ASHLESHA", "MAGHA", "PURVA PHALGUNI", "UTTARA PHALGUNI", "HASTA", "CHITRA", "SWATI",
        "VISHAKHA", "ANURADHA", "JYESHTHA", "MULA", "PURVA ASHADHA", "UTTARA ASHADHA", "SHRAVANA",
        "DHANISHTA", "SHATABHISHA", "PURVA BHADRAPADA", "UTTARA BHADRAPADA", "REVATI"
    )
    private val MASA = arrayOf(
        "CHAITRA", "VAISHAKHA", "JYESHTHA", "ASHADHA", "SHRAVANA", "BHADRAPADA",
        "ASHWAYUJA", "KARTIKA", "MARGASHIRSHA", "PUSHYA", "MAGHA", "PHALGUNA"
    )
    private val RASHI = arrayOf(
        "MESHA", "VRISHABHA", "MITHUNA", "KARKATAKA", "SIMHA", "KANYA",
        "TULA", "VRISHCHIKA", "DHANUS", "MAKARA", "KUMBHA", "MEENA"
    )
    private val YOGA = arrayOf(
        "VISHKAMBHA", "PRITI", "AYUSHMAN", "SAUBHAGYA", "SHOBHANA", "ATI GANDA", "SUKARMA", "DHRITI",
        "SHOOLA", "GANDA", "VRIDDHI", "DHRUVA", "VYAGHATA", "HARSHANA", "VAJRA", "SIDDHI", "VYATIPATA",
        "VARIYAN", "PARIGHA", "SHIVA", "SIDDHA", "SADHYA", "SHUBHA", "SHUKLA", "BRAHMA", "INDRA", "VAIDHRITI"
    )
    private val KARANA = arrayOf(
        "BAVA", "BALAVA", "KAULAVA", "TAITILA", "GARA", "VANIJA", "VISHTI"
    )
    private val SAMVAT = arrayOf(
        "PRABHAVA", "VIBHAVA", "SHUKLA", "PRAMODA", "PRAJAPATI", "ANGIRA", "SHRIMUKHA", "BHAVA",
        "YUVA", "DHATA", "ISHVARA", "BAHUDHANYA", "PRAMATHI", "VIKRAMA", "VRISHA", "CHITRABHANU",
        "SVABHANU", "TARANA", "PARTHIVA", "VYAYA", "SARVAJIT", "SARVADHARI", "VIRODHI", "VIKRITI",
        "KHARA", "NANDANA", "VIJAYA", "JAYA", "MANMATHA", "DURMUKHI", "HEVILAMBI", "VILAMBI",
        "VIKARI", "SHARVARI", "PLAVA", "SHUBHAKRIT", "SHOBHAKRIT", "KRODHI", "VISHVAVASU", "PARABHAVA",
        "PLAVANGA", "KILAKA", "SAUMYA", "SADHARANA", "VIRODHIKRIT", "PARIDHAVI", "PRAMADI", "ANANDA",
        "RAKSHASA", "ANALA", "PINGALA", "KALAYUKTI", "SIDDHARTHI", "RAUDRA", "DURMATI", "DUNDUBHI",
        "RUDHIRODGARI", "RAKTAKSHA", "KRODHANA", "AKSHAYA"
    )

    /** South-Indian Chandramana year; Prabhava = Ugadi 1987. Turns near March. */
    private fun chandramanaYear(cal: Calendar): Int {
        var y = cal.get(Calendar.YEAR)
        if (cal.get(Calendar.MONTH) < Calendar.MARCH) y -= 1
        return y
    }

    private fun compute(date: String): PanchangaDay {
        val cal = parse(date)
        val jd = julian(cal)
        val sun = sunLon(jd)
        val moon = moonLon(jd)
        val elong = norm(moon - sun)
        val tithiN = (floor(elong / 12.0).toInt() % 30)
        val shukla = tithiN < 15
        val tithi = if (shukla) TITHI[tithiN] else TITHI_K[tithiN - 15]
        val siderealMoon = siderealLongitude(moon, jd)
        val siderealSun = siderealLongitude(sun, jd)
        val nak = NAK[(floor(siderealMoon / (360.0 / 27.0)).toInt() % 27)]
        val yogaN = (floor(norm(siderealSun + siderealMoon) / (360.0 / 27.0)).toInt() % 27)
        val karana = karanaFromElongation(elong)
        val masa = MASA[floor(norm(sun - 30.0) / 30.0).toInt() % 12]
        val ayana = if (norm(sun) >= 180) "DAKSHINAYANA" else "UTTARAYANA"
        val rituN = (floor(norm(sun - 30.0) / 60.0).toInt() % 6)
        val ritu = arrayOf("VASANTA", "GRISHMA", "VARSHA", "SHARAD", "HEMANTA", "SHISHIRA")[rituN]
        val shakaYear = chandramanaYear(cal)
        val sam = SAMVAT[((shakaYear - 1987) % 60 + 60) % 60]
        val vara = arrayOf("SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY")[
            cal.get(Calendar.DAY_OF_WEEK) - 1
        ]
        return PanchangaDay(
            date = dateKey(cal),
            samvatsara = sam,
            ayana = ayana,
            ritu = ritu,
            masa = masa,
            paksha = if (shukla) "SHUKLA" else "KRISHNA",
            tithi = tithi,
            vara = vara,
            nakshatra = nak,
            yoga = YOGA[yogaN],
            karana = karana,
            source = ""
        )
    }

    /** Julian days use the UTC instant, including zone offset, minutes and seconds. */
    internal fun julian(cal: Calendar): Double = cal.timeInMillis / 86400000.0 + 2440587.5

    private fun sunLon(jd: Double): Double {
        val t = (jd - 2451545.0) / 36525.0
        val l0 = 280.46646 + 36000.76983 * t
        val m = rad(357.52911 + 35999.05029 * t)
        val c = 1.914602 * sin(m) + 0.019993 * sin(2 * m)
        return norm(l0 + c)
    }

    private fun moonLon(jd: Double): Double {
        val t = (jd - 2451545.0) / 36525.0
        val lp = 218.3164477 + 481267.88123421 * t
        val d = rad(297.8501921 + 445267.1114034 * t)
        val m = rad(357.5291092 + 35999.0502909 * t)
        val mp = rad(134.9633964 + 477198.867398 * t)
        val f = rad(93.272095 + 483202.0175233 * t)
        val lon = lp + 6.289 * sin(mp) + 1.274 * sin(2 * d - mp) + 0.658 * sin(2 * d) +
            0.214 * sin(2 * mp) - 0.186 * sin(m) - 0.114 * sin(2 * f)
        return norm(lon)
    }

    private fun rad(deg: Double) = Math.toRadians(norm(deg))
    private fun norm(deg: Double): Double {
        var x = deg % 360.0
        if (x < 0) x += 360.0
        return x
    }
}
