package com.paperdesk.launcher.domain.measure

import java.math.BigInteger

/** A length entered as feet and inches. Inches must be 0 to 11 and feet nonnegative. */
data class FeetInches(val feet: Int, val inches: Int) {
    init {
        require(feet >= 0) { "feet must be nonnegative" }
        require(inches in 0..11) { "inches must be 0 to 11" }
    }

    val totalInches: Long get() = feet.toLong() * 12 + inches

    companion object {
        /** Splits a total in inches back into feet and inches (round trip for the candidate search). */
        fun ofInches(totalInches: Long): FeetInches {
            require(totalInches >= 0) { "length must be nonnegative" }
            return FeetInches((totalInches / 12).toInt(), (totalInches % 12).toInt())
        }
    }

    override fun toString(): String = "$feet′$inches″"
}

/** An exact fraction. Denominator is always positive and the fraction is reduced. */
class Rational private constructor(val numerator: BigInteger, val denominator: BigInteger) : Comparable<Rational> {
    val isInteger: Boolean get() = denominator == BigInteger.ONE

    operator fun times(other: Rational): Rational = of(numerator * other.numerator, denominator * other.denominator)

    operator fun div(other: Rational): Rational {
        require(other.numerator.signum() != 0) { "division by zero" }
        return of(numerator * other.denominator, denominator * other.numerator)
    }

    operator fun minus(other: Rational): Rational =
        of(numerator * other.denominator - other.numerator * denominator, denominator * other.denominator)

    val isZero: Boolean get() = numerator.signum() == 0

    /** This value reduced into [0, m): exact, with no rounding. Used for the remainder in the Aayadi formulas. */
    fun modulo(m: Long): Rational {
        require(m > 0) { "modulus must be positive" }
        val mm = of(m)
        // floor(this / m) for a positive denominator
        val (q0, r0) = (this / mm).let { it.numerator.divideAndRemainder(it.denominator) }
        val q = if (r0.signum() < 0) q0 - BigInteger.ONE else q0
        return this - mm * of(q, BigInteger.ONE)
    }

    /** The whole number when this is an integer, otherwise null. */
    fun integerOrNull(): Long? = if (isInteger) numerator.toLong() else null

    override fun compareTo(other: Rational): Int = (numerator * other.denominator).compareTo(other.numerator * denominator)

    override fun equals(other: Any?): Boolean =
        other is Rational && numerator == other.numerator && denominator == other.denominator

    override fun hashCode(): Int = 31 * numerator.hashCode() + denominator.hashCode()

    /** Whole number when integer, otherwise "n/d", e.g. "80/3". */
    override fun toString(): String = if (isInteger) numerator.toString() else "$numerator/$denominator"

    fun toDouble(): Double = numerator.toDouble() / denominator.toDouble()

    companion object {
        fun of(numerator: Long, denominator: Long = 1): Rational = of(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator))

        fun of(numerator: BigInteger, denominator: BigInteger): Rational {
            require(denominator.signum() != 0) { "denominator must not be zero" }
            val sign = if (denominator.signum() < 0) BigInteger.ONE.negate() else BigInteger.ONE
            val gcd = numerator.gcd(denominator).let { if (it.signum() == 0) BigInteger.ONE else it }
            return Rational(numerator * sign / gcd, denominator * sign / gcd)
        }
    }
}

/**
 * The fixed hasta standard: 1 hasta = 24 angula = 18 inches. Not configurable.
 * Every conversion in the application uses this one factor.
 */
object Hasta {
    const val INCHES_PER_HASTA: Long = 18
    const val ANGULA_PER_HASTA: Int = 24

    /** Exact hasta for a total in inches. Never rounded. */
    fun hastaOf(totalInches: Long): Rational = Rational.of(totalInches, INCHES_PER_HASTA)
}
