package com.paperdesk.launcher.data

import kotlin.math.abs

/**
 * House-size Aayadi from the Shodashavarga table (Kannada book, page 97).
 * Dimensions are whole feet; the area (padam) is length x breadth in square feet.
 */
object Aayadi {
    data class Result(
        val length: Double,
        val breadth: Double,
        val padam: Double,
        val aaya: Double,
        /** Null when the length is not whole feet: vyaya is defined on whole numbers only. */
        val vyaya: Double?,
        val runa: Double,
        val dhana: Double,
        val ayush: Double,
        val aayaName: String,
        val summary: String,
        /** Aaya is one of the auspicious ordinals 1, 3, 5, 7 of the book. */
        val aayaOk: Boolean,
        /** Vyaya is less than aaya (Vastu texts: "aaya more, vyaya less"). Not in the book's table. */
        val vyayaOk: Boolean?,
        /** Ayush is above 60 (book: "ಆಯವರ್ಗದಲ್ಲಿ 60 ಮೇಲ್ಪಟ್ಟು ಆಯಸ್ಸು ಇರಬೇಕು"). */
        val ayushOk: Boolean
    )

    /** Inches are limited to quarter-foot steps. The area must still be a whole number of square feet. */
    private val INCH_STEPS = setOf(0.0, 3.0, 6.0, 9.0)

    /** Why the measurement cannot be calculated, so the screen can say what to change. */
    enum class Problem { EMPTY, INCHES, AREA }

    class InputProblem(val problem: Problem) : IllegalArgumentException(problem.name)

    /** Breadth inch steps (0, 3, 6, 9) that give a whole-square-foot area with this length. */
    fun wholeAreaBreadthInches(lengthFeet: Double, lengthInches: Double, breadthFeet: Double): List<Int> {
        val length = lengthFeet + lengthInches / 12.0
        return INCH_STEPS.map { it.toInt() }.filter { inches ->
            val area = length * (breadthFeet + inches / 12.0)
            area % 1.0 == 0.0
        }
    }

    fun calculate(lengthFeet: Double, breadthFeet: Double): Result = calculate(lengthFeet, 0.0, breadthFeet, 0.0)

    fun calculate(lengthFeet: Double, lengthInches: Double, breadthFeet: Double, breadthInches: Double): Result {
        if (lengthFeet < 0 || breadthFeet < 0) throw InputProblem(Problem.EMPTY)
        if (lengthInches !in INCH_STEPS || breadthInches !in INCH_STEPS) throw InputProblem(Problem.INCHES)
        val length = lengthFeet + lengthInches / 12.0
        val breadth = breadthFeet + breadthInches / 12.0
        if (!(length > 0 && breadth > 0)) throw InputProblem(Problem.EMPTY)
        val padam = length * breadth
        if (padam % 1.0 != 0.0) throw InputProblem(Problem.AREA)
        val aaya = rem(padam * 9, 8)
        val dhana = rem(padam * 8, 12)
        val runa = rem(padam * 3, 8)
        val ayush = rem(padam * 9, 120)
        val vyaya: Double? = if (length % 1.0 == 0.0) rem(length * 9, 10) else null
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
        val aayaOk = ordinal in setOf(1, 3, 5, 7)
        val vyayaOk: Boolean? = vyaya?.let { it < aaya }
        val ayushOk = ayush > 60
        return Result(length, breadth, padam, aaya, vyaya, runa, dhana, ayush, name, tests.joinToString("  ·  "), aayaOk, vyayaOk, ayushOk)
    }

    // A zero remainder is reported as the divisor, as in the book.
    private fun rem(value: Double, divisor: Int): Double {
        val raw = value % divisor
        return if (abs(raw) < 0.000001) divisor.toDouble() else raw
    }
}
