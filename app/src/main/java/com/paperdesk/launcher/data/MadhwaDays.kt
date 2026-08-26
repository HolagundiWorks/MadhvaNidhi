package com.paperdesk.launcher.data

/** Lunar Madhwa observances keyed by masa + paksha + tithi. */
object MadhwaDays {

    fun name(masa: String, paksha: String, tithi: String): String {
        val m = masa.uppercase()
        val p = if ("KRISHNA" in paksha.uppercase()) "K" else "S"
        val t = tithi.uppercase()
        fun has(token: String) = token in t
        return when {
            has("EKADASHI") -> "EKADASHI"
            m.contains("CHAITRA") && p == "S" && has("PRATIPADA") -> "UGADI"
            m.contains("CHAITRA") && p == "S" && has("NAVAMI") -> "RAMA NAVAMI"
            m.contains("CHAITRA") && p == "S" && has("PURNIMA") -> "HANUMAN JAYANTI"
            m.contains("VAISHAKHA") && p == "S" && has("TRITIYA") -> "AKSHAYA TRITIYA"
            m.contains("VAISHAKHA") && p == "S" && has("CHATURDASHI") -> "NARASIMHA JAYANTI"
            m.contains("VAISHAKHA") && p == "S" && has("PURNIMA") -> "KURMA JAYANTI"
            m.contains("JYESHTHA") && p == "S" && has("PURNIMA") -> "VATA SAVITRI"
            m.contains("ASHADHA") && p == "S" && has("EKADASHI") -> "SHAYANI EKADASHI"
            m.contains("SHRAVANA") && p == "K" && has("DWITIYA") -> "RAGHAVENDRA ARADHANA"
            m.contains("SHRAVANA") && p == "K" && has("ASHTAMI") -> "KRISHNA JANMASHTAMI"
            m.contains("BHADRAPADA") && p == "S" && has("CHATURTHI") -> "GANESHA CHATURTHI"
            m.contains("ASHWAYUJA") && p == "S" && has("DASHAMI") -> "VIJAYADASHAMI"
            m.contains("KARTIKA") && p == "K" && has("CHATURDASHI") -> "NARAKA CHATURDASHI"
            m.contains("KARTIKA") && p == "S" && has("DWADASHI") -> "TULASI VIVHA"
            m.contains("MAGHA") && p == "S" && has("NAVAMI") -> "MADHWA NAVAMI"
            m.contains("MAGHA") && p == "S" && has("PANCHAMI") -> "VASANTA PANCHAMI"
            has("PURNIMA") -> "PURNIMA"
            has("AMAVASYA") -> "AMAVASYA"
            else -> ""
        }
    }

    fun isMarked(masa: String, paksha: String, tithi: String): Boolean {
        val n = name(masa, paksha, tithi)
        return n.isNotBlank()
    }
}
