package com.paperdesk.launcher.data

/** Small offline phonetic input for names: English keystrokes are stored in Kannada. */
object KannadaPhonetic {
    private val vowels = linkedMapOf(
        "a" to Pair("ಅ", ""), "aa" to Pair("ಆ", "ಾ"),
        "i" to Pair("ಇ", "ಿ"), "ii" to Pair("ಈ", "ೀ"), "ee" to Pair("ಈ", "ೀ"),
        "u" to Pair("ಉ", "ು"), "uu" to Pair("ಊ", "ೂ"), "oo" to Pair("ಊ", "ೂ"),
        "ai" to Pair("ಐ", "ೈ"), "au" to Pair("ಔ", "ೌ"),
        "e" to Pair("ಎ", "ೆ"), "o" to Pair("ಒ", "ೊ")
    )
    private val consonants = linkedMapOf(
        "ksh" to "ಕ್ಷ", "jn" to "ಜ್ಞ", "gy" to "ಜ್ಞ",
        "kh" to "ಖ", "gh" to "ಘ", "chh" to "ಛ", "ch" to "ಚ", "jh" to "ಝ",
        "th" to "ಥ", "dh" to "ಧ", "ph" to "ಫ", "bh" to "ಭ", "sh" to "ಶ",
        "ng" to "ಙ", "ny" to "ಞ",
        "k" to "ಕ", "g" to "ಗ", "c" to "ಚ", "j" to "ಜ", "t" to "ತ", "d" to "ದ",
        "n" to "ನ", "p" to "ಪ", "b" to "ಬ", "m" to "ಮ", "y" to "ಯ", "r" to "ರ",
        "l" to "ಲ", "v" to "ವ", "w" to "ವ", "s" to "ಸ", "h" to "ಹ", "f" to "ಫ",
        "q" to "ಕ", "x" to "ಕ್ಸ", "z" to "ಜ"
    )

    fun transliterate(input: String): String = Regex("[A-Za-z]+").replace(input) { word ->
        word(word.value.lowercase())
    }

    private fun word(raw: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < raw.length) {
            val consonant = consonants.keys.firstOrNull { raw.startsWith(it, i) }
            if (consonant != null) {
                val base = consonants.getValue(consonant)
                i += consonant.length
                val vowel = vowels.keys.sortedByDescending { it.length }.firstOrNull { raw.startsWith(it, i) }
                if (vowel != null) {
                    out.append(base).append(vowels.getValue(vowel).second)
                    i += vowel.length
                } else {
                    val anotherConsonant = consonants.keys.any { raw.startsWith(it, i) }
                    out.append(base)
                    if (anotherConsonant) out.append('್')
                }
                continue
            }
            val vowel = vowels.keys.sortedByDescending { it.length }.firstOrNull { raw.startsWith(it, i) }
            if (vowel != null) {
                out.append(vowels.getValue(vowel).first)
                i += vowel.length
            } else {
                out.append(raw[i++])
            }
        }
        return out.toString()
    }
}
