package com.paperdesk.launcher.data

import android.content.Context
import java.util.Calendar

/** Profile-aware Madhwa trikala Sandhyavandana in Kannada. */
object SandhyaStore {
    const val PRATAH = "pratah"
    const val MADHYA = "madhya"
    const val SAYAM = "sayam"

    data class Session(val id: String, val title: String, val steps: List<Step>)
    data class Step(val id: String, val n: Int, val title: String, val body: String)

    @Volatile private var loaded = false
    var sessions: List<Session> = emptyList()
        private set
    private var byId: Map<String, Session> = emptyMap()
    private var yajurSessions: List<Session> = emptyList()
    private var yajurById: Map<String, Session> = emptyMap()

    @Synchronized
    fun load(context: Context) {
        if (loaded) return
        val rigveda = runCatching {
            context.assets.open("madhwa/texts/rigveda_sandhyavandana.txt").bufferedReader().use { it.readText() }
        }.getOrDefault("").trim()
        if (rigveda.isNotBlank()) {
            sessions = parseRigveda(rigveda)
            byId = sessions.associateBy { it.id }
        }
        val yajurveda = runCatching {
            context.assets.open("madhwa/texts/yajurveda_sandhyavandana.txt").bufferedReader().use { it.readText() }
        }.getOrDefault("").trim()
        if (yajurveda.isNotBlank()) {
            yajurSessions = parseRigveda(yajurveda)
            yajurById = yajurSessions.associateBy { it.id }
        }
        if (sessions.isNotEmpty()) {
            loaded = true
            return
        }
        val raw = runCatching {
            context.assets.open("library/mantras/Sandhyavandanam.txt").bufferedReader().use { it.readText() }
        }.getOrDefault("")
        sessions = parse(tidy(raw))
        byId = sessions.associateBy { it.id }
        loaded = true
    }

    fun session(id: String): Session? = byId[id] ?: sessions.firstOrNull()

    fun session(context: Context, id: String): Session? {
        load(context)
        val source = if (usesKrishnaYajurveda(context)) yajurById else byId
        val list = if (usesKrishnaYajurveda(context)) yajurSessions else sessions
        return source[id] ?: list.firstOrNull()
    }

    fun sessions(context: Context): List<Session> {
        load(context)
        return if (usesKrishnaYajurveda(context)) yajurSessions else sessions
    }

    fun vedaLabel(context: Context): String = if (usesKrishnaYajurveda(context)) {
        "ಕೃಷ್ಣ ಯಜುರ್ವೇದ — ತೈತ್ತಿರೀಯ"
    } else {
        "ಋಗ್ವೇದ — ಆಶ್ವಲಾಯನ"
    }

    private fun usesKrishnaYajurveda(context: Context): Boolean =
        SettingsStore(context).veda() == SettingsStore.VEDA_KRISHNA_YAJURVEDA && yajurSessions.isNotEmpty()

    fun nowId(): String {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (h) {
            in 4..9 -> PRATAH
            in 10..15 -> MADHYA
            else -> SAYAM
        }
    }

    fun next(session: Session, step: Step): Step? {
        val i = session.steps.indexOfFirst { it.id == step.id }
        if (i < 0 || i + 1 >= session.steps.size) return null
        return session.steps[i + 1]
    }

    fun prev(session: Session, step: Step): Step? {
        val i = session.steps.indexOfFirst { it.id == step.id }
        if (i <= 0) return null
        return session.steps[i - 1]
    }

    fun label(text: String, lang: IndicScript.Lang): String = IndicScript.convert(text, lang)

    fun body(step: Step, lang: IndicScript.Lang): String =
        IndicScript.convert(RitualText.tidy(step.body), lang)

    fun body(context: Context, step: Step, sessionId: String, lang: IndicScript.Lang): String {
        val core = IndicScript.convert(RitualText.tidy(step.body), lang)
        val dynamic = Sankalpa.sandhyaBlock(context, sessionId, lang)
        val withSankalpa = if (step.title.contains("ಸಂಕಲ್ಪ")) dynamic else core
        val abhivadana = Regex(
            "(?m)^.*(?:ಪ್ರವರಾನ್ವಿತ|प्रवरान्वित).*?(?:ಅಭಿವಾದಯೇ|अभिवादये).*?$"
        )
        return if (abhivadana.containsMatchIn(withSankalpa)) {
            withSankalpa.replace(abhivadana, Sankalpa.abhivadanaBlock(context))
        } else {
            withSankalpa
        }
    }

    /** Turns the authoritative trikala source into readable, time-correct ritual sections. */
    internal fun parseRigveda(raw: String): List<Session> = listOf(
        PRATAH to "ಪ್ರಾತಃ ಸಂಧ್ಯಾ",
        MADHYA to "ಮಾಧ್ಯಾಹ್ನಿಕ",
        SAYAM to "ಸಾಯಂ ಸಂಧ್ಯಾ"
    ).map { (id, title) ->
        val steps = splitRigveda(raw).mapIndexedNotNull { index, section ->
            val body = selectKala(section.second, id)
            body.takeIf { it.isNotBlank() }?.let {
                Step("rigveda_${id}_${index + 1}", index + 1, cleanSectionTitle(section.first), it)
            }
        }
        Session(id, title, steps.mapIndexed { index, step -> step.copy(n = index + 1) })
    }

    private fun splitRigveda(raw: String): List<Pair<String, String>> {
        val sections = ArrayList<Pair<String, String>>()
        var title = "ಆರಂಭ ಮತ್ತು ಸೂಚನೆ"
        val body = StringBuilder()

        fun flush() {
            val clean = body.toString().trim()
            if (clean.isNotBlank()) sections += title to clean
            body.setLength(0)
        }

        raw.lineSequence().forEachIndexed { index, source ->
            val line = source.trimEnd()
            val heading = Regex("^(.{2,80}?)\\s*[–—]\\s*$").matchEntire(line.trim())
            if (heading != null) {
                flush()
                title = cleanSectionTitle(heading.groupValues[1])
            } else if (line.trim() in SOURCE_MARKERS) {
                // Keep the optional rite but omit print-only source brackets.
            } else if (index > 1 && !line.startsWith("ಆಧಾರ:") && !line.startsWith("http")) {
                body.append(line).append('\n')
            }
        }
        flush()
        return sections
    }

    private fun cleanSectionTitle(raw: String): String = raw
        .replace(Regex("^\\[\\*?\\s*"), "")
        .replace(Regex("\\*?]$"), "")
        .trim()

    private val SOURCE_MARKERS = setOf("[*", "*]", "[", "]")

    private fun selectKala(raw: String, sessionId: String): String {
        var selected: String? = null
        val out = StringBuilder()
        raw.lineSequence().forEach { line ->
            val marker = when (line.trim()) {
                "(ಪ್ರಾತಃ ಕಾಲೇ)" -> PRATAH
                "(ಮಧ್ಯಾಹ್ನ ಕಾಲೇ)" -> MADHYA
                "(ಸಾಯಂ ಕಾಲೇ)" -> SAYAM
                else -> null
            }
            if (marker != null) {
                selected = marker
            } else if (selected == null || selected == sessionId) {
                out.append(line).append('\n')
            }
        }
        val kala = when (sessionId) {
            PRATAH -> "ಪ್ರಾತಃ"
            MADHYA -> "ಮಾಧ್ಯಾಹ್ನಿಕ"
            else -> "ಸಾಯಂ"
        }
        return out.toString()
            .replace("ಪ್ರಾತಃ/ಮಾಧ್ಯಾಹ್ನಿಕ/ಸಾಯಂ", kala)
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    private val SESSION = mapOf(
        "प्रातः सन्ध्यावन्दनम्" to Pair(PRATAH, "प्रातः सन्ध्या"),
        "माध्याह्निकम्" to Pair(MADHYA, "माध्याह्निकम्"),
        "सायं सन्ध्यावन्दनम्" to Pair(SAYAM, "सायं सन्ध्या")
    )

    private val HEADS = setOf(
        "आचमनम्",
        "अङ्गवन्दनम्",
        "विघ्नेश्वर-ध्यानम्",
        "प्राणायामः",
        "सङ्कल्पः",
        "मार्जनम्",
        "प्राशनम्",
        "पुनर्मार्जनम्",
        "अर्घ्यप्रदानम्",
        "प्रायश्चित्तार्घ्यम्",
        "ऐक्यानुसन्धानम्",
        "देवतर्पणम्",
        "नवग्रहदेवता-तर्पणम्",
        "केशवादि-तर्पणम्",
        "सन्ध्यावन्दन-उत्तरभागः",
        "जप-सङ्कल्पः",
        "प्रणवजपः—प्राणायामः",
        "गायत्री-आवाहनम्",
        "गायत्री-जपः",
        "ध्यानम्",
        "गायत्री-उपस्थानम्",
        "प्रातः सन्ध्या सूर्योपस्थानम्",
        "माध्याह्निक सूर्योपस्थानम्",
        "सायं सन्ध्या सूर्योपस्थानम्",
        "समष्ट्यभिवादनम्",
        "दिग्देवता-वन्दनम्",
        "यम-वन्दनम्",
        "सर्प-रक्षा",
        "हरिहर-वन्दनम्",
        "सूर्यनारायण-वन्दनम्",
        "समर्पणम्",
        "रक्षा"
    )

    private fun parse(raw: String): List<Session> {
        val out = ArrayList<Session>(3)
        var sid = ""
        var stitle = ""
        val steps = ArrayList<Step>()
        var head = ""
        val body = StringBuilder()

        fun flushStep() {
            if (head.isBlank()) return
            val n = steps.size + 1
            steps += Step("${sid}_$n", n, head, body.toString().trim())
            head = ""
            body.setLength(0)
        }

        fun flushSession() {
            flushStep()
            body.setLength(0)
            if (sid.isNotBlank() && steps.isNotEmpty()) {
                out += Session(sid, stitle, steps.toList())
            }
            steps.clear()
            sid = ""
            stitle = ""
        }

        raw.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) {
                if (body.isNotEmpty()) body.append('\n')
                return@forEach
            }
            val session = SESSION[line]
            if (session != null) {
                flushSession()
                sid = session.first
                stitle = session.second
                return@forEach
            }
            if (sid.isBlank()) return@forEach
            if (line in HEADS) {
                flushStep()
                head = line
                return@forEach
            }
            if (body.isNotEmpty()) body.append('\n')
            body.append(line)
        }
        flushSession()
        return out
    }

    private fun tidy(raw: String): String {
        var t = raw
        t = t.replace(Regex("\\[Script=[^\\]]*\\]\\S*"), "")
        t = t.replace(Regex("(?m)^\\s*\\[\\d+]#?\\d*\\s*$"), "")
        t = t.replace(Regex("(?m)^\\s*#\\d+.*$"), "")
        t = t.replace(Regex("(?m)^enumerate\\s*$"), "")
        t = t.replace(Regex("\\[\\d+em\\]\\[l\\]"), "")
        t = t.replace(Regex("(?m)^॥इति.*॥\\s*$"), "")
        t = t.replace(Regex("\\b(shlokacount\\d*|multicols\\d*|flushleft|flushright|centerline|devanumber\\d*|addtocounter)\\b"), "")
        t = t.replace(Regex("[ \\t]{2,}"), " ")
        t = t.replace(Regex("\n{3,}"), "\n\n")
        return t.trim()
    }
}
