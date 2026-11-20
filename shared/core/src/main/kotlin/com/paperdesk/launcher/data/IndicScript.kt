package com.paperdesk.launcher.data

/** Devanagari source → Kannada script or English IAST. Vedic svaras kept in Sanskrit only. */
object IndicScript {
    enum class Lang { SA, KN, EN }

    fun parse(raw: String): Lang = when (raw.lowercase()) {
        "kn", "kannada" -> Lang.KN
        "en", "english" -> Lang.EN
        else -> Lang.SA
    }

    fun key(lang: Lang): String = when (lang) {
        Lang.SA -> "sa"
        Lang.KN -> "kn"
        Lang.EN -> "en"
    }

    fun convert(text: String, lang: Lang): String {
        val src = fromNative(text)
        return when (lang) {
            Lang.SA -> src
            Lang.KN -> toKannada(src)
            Lang.EN -> toIast(src)
        }
    }

    /** Kannada source → Devanagari so SA / EN conversion works. */
    fun fromNative(text: String): String {
        val out = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val n = Character.charCount(cp)
            if (cp in 0x0C80..0x0CFF) out.appendCodePoint(cp - 0x0380)
            else out.appendCodePoint(cp)
            i += n
        }
        return out.toString()
    }

    private fun toKannada(text: String): String {
        val out = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val n = Character.charCount(cp)
            when {
                cp == 0x0950 -> out.append("ಓಂ")
                cp == 0x0964 -> out.append("।")
                cp == 0x0965 -> out.append("॥")
                cp in 0x0951..0x0954 -> {}
                cp in 0x1CD0..0x1CFF -> {}
                cp in 0xA8E0..0xA8FF -> {}
                cp in 0x0900..0x097F -> {
                    val kn = cp + 0x0380
                    if (Character.getType(kn) == Character.UNASSIGNED.toInt()) out.appendCodePoint(cp)
                    else out.appendCodePoint(kn)
                }
                else -> out.appendCodePoint(cp)
            }
            i += n
        }
        return out.toString()
    }

    private val CONS = hashMapOf(
        0x0915 to "k", 0x0916 to "kh", 0x0917 to "g", 0x0918 to "gh", 0x0919 to "ṅ",
        0x091A to "c", 0x091B to "ch", 0x091C to "j", 0x091D to "jh", 0x091E to "ñ",
        0x091F to "ṭ", 0x0920 to "ṭh", 0x0921 to "ḍ", 0x0922 to "ḍh", 0x0923 to "ṇ",
        0x0924 to "t", 0x0925 to "th", 0x0926 to "d", 0x0927 to "dh", 0x0928 to "n",
        0x092A to "p", 0x092B to "ph", 0x092C to "b", 0x092D to "bh", 0x092E to "m",
        0x092F to "y", 0x0930 to "r", 0x0932 to "l", 0x0933 to "ḷ", 0x0935 to "v",
        0x0936 to "ś", 0x0937 to "ṣ", 0x0938 to "s", 0x0939 to "h",
        0x0931 to "r", 0x0929 to "n"
    )
    private val INDEP = hashMapOf(
        0x0905 to "a", 0x0906 to "ā", 0x0907 to "i", 0x0908 to "ī",
        0x0909 to "u", 0x090A to "ū", 0x090B to "ṛ", 0x0960 to "ṝ",
        0x090C to "ḷ", 0x0961 to "ḹ", 0x090F to "e", 0x0910 to "ai",
        0x0913 to "o", 0x0914 to "au", 0x090D to "ê", 0x090E to "e",
        0x0911 to "ô", 0x0912 to "o"
    )
    private val MATRA = hashMapOf(
        0x093E to "ā", 0x093F to "i", 0x0940 to "ī", 0x0941 to "u", 0x0942 to "ū",
        0x0943 to "ṛ", 0x0944 to "ṝ", 0x0962 to "ḷ", 0x0963 to "ḹ",
        0x0947 to "e", 0x0948 to "ai", 0x094B to "o", 0x094C to "au",
        0x0945 to "ê", 0x0946 to "e", 0x0949 to "ô", 0x094A to "o"
    )
    private const val VIRAMA = 0x094D
    private const val ANUSVARA = 0x0902
    private const val VISARGA = 0x0903
    private const val CANDRA = 0x0901
    private const val AVAGRAHA = 0x093D
    private const val OM = 0x0950
    private const val NUKTA = 0x093C

    private fun toIast(text: String): String {
        val out = StringBuilder(text.length)
        val cps = IntArray(text.codePointCount(0, text.length))
        var p = 0
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            cps[p++] = cp
            i += Character.charCount(cp)
        }
        var x = 0
        while (x < cps.size) {
            val cp = cps[x]
            when {
                cp == OM -> {
                    out.append("om")
                    x++
                }
                cp == AVAGRAHA -> {
                    out.append("'")
                    x++
                }
                cp in 0x0951..0x0954 || cp in 0x1CD0..0x1CFF || cp in 0xA8E0..0xA8FF || cp == NUKTA -> x++
                cp == ANUSVARA -> {
                    out.append("ṃ")
                    x++
                }
                cp == VISARGA -> {
                    out.append("ḥ")
                    x++
                }
                cp == CANDRA -> {
                    out.append("m̐")
                    x++
                }
                cp in 0x0966..0x096F -> {
                    out.append(('0'.code + (cp - 0x0966)).toChar())
                    x++
                }
                INDEP.containsKey(cp) -> {
                    out.append(INDEP[cp])
                    x++
                }
                CONS.containsKey(cp) -> {
                    out.append(CONS[cp])
                    val next = cps.getOrNull(x + 1)
                    when {
                        next == VIRAMA -> {
                            x += 2
                        }
                        next != null && MATRA.containsKey(next) -> {
                            out.append(MATRA[next])
                            x += 2
                        }
                        else -> {
                            out.append('a')
                            x++
                        }
                    }
                }
                MATRA.containsKey(cp) -> {
                    out.append(MATRA[cp])
                    x++
                }
                cp == VIRAMA -> x++
                else -> {
                    out.appendCodePoint(cp)
                    x++
                }
            }
        }
        return out.toString()
    }
}
