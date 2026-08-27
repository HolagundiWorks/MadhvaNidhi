package com.paperdesk.launcher.data

import kotlin.math.abs

/** Karnataka Navavarga working convention, with a Kamika Shadvarga Vyaya cross-check. */
object Aayadi {
    data class Result(
        val length: Double,
        val breadth: Double,
        val padam: Double,
        val aaya: Double,
        val vyaya: Double,
        val runa: Double,
        val dhana: Double,
        val ayush: Double,
        val aayaName: String,
        val summary: String
    )

    fun calculate(lengthFeet: Double, lengthInches: Double, breadthFeet: Double, breadthInches: Double): Result {
        require(lengthFeet >= 0 && lengthInches >= 0 && breadthFeet >= 0 && breadthInches >= 0)
        val length = lengthFeet + lengthInches / 12.0
        val breadth = breadthFeet + breadthInches / 12.0
        require(length > 0 && breadth > 0)
        val padam = length * breadth
        val aaya = rem(padam * 9, 8)
        val dhana = rem(padam * 8, 12)
        val runa = rem(padam * 3, 8)
        val ayush = rem(padam * 9, 120)
        val vyaya = rem(length * 9, 10)
        val ordinal = aaya.toInt().takeIf { abs(aaya - it) < 0.001 }
        val name = when (ordinal) {
            1 -> "ಧ್ವಜ"
            2 -> "ಧೂಮ"
            3 -> "ಸಿಂಹ"
            4 -> "ಶ್ವಾನ"
            5 -> "ವೃಷಭ"
            6 -> "ಖರ"
            7 -> "ಗಜ"
            8 -> "ಕಾಕ"
            else -> "ಭಿನ್ನಾಂಶ ಫಲ"
        }
        val tests = listOf(
            if (ordinal in setOf(1, 3, 5, 7)) "ಆಯ ಅನುಕೂಲ" else "ಆಯ ಪರಿಶೀಲಿಸಿ",
            if (dhana > runa) "ಧನ > ಋಣ" else "ಧನ ≤ ಋಣ",
            if (ayush > 60) "ಆಯುಷ್ಯ > 60" else "ಆಯುಷ್ಯ ≤ 60"
        )
        return Result(length, breadth, padam, aaya, vyaya, runa, dhana, ayush, name, tests.joinToString("  ·  "))
    }

    private fun rem(value: Double, divisor: Int): Double {
        val raw = value % divisor
        return if (abs(raw) < 0.000001) divisor.toDouble() else raw
    }
}
