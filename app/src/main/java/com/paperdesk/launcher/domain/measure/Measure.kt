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
 * A selectable hasta standard: how many inches make one hasta, with its citation.
 * The guide notes 18 inches is a conventional configuration, not a universal text standard.
 */
data class HastaStandard(val id: String, val displayName: String, val inchesPerHasta: Long, val citation: String) {
    /** Exact hasta for a total in inches. Never rounded. */
    fun hastaOf(totalInches: Long): Rational = Rational.of(totalInches, inchesPerHasta)

    companion object {
        /** 1 hasta = 24 angula = 18 inches. Conventional; the Karnataka pack records its own citation. */
        val CONVENTIONAL_18 = HastaStandard(
            id = "conventional-18in",
            displayName = "1 ಹಸ್ತ = 18 ಇಂಚು (24 ಅಂಗುಲ)",
            inchesPerHasta = 18,
            citation = "Conventional configuration; not a universal textual standard"
        )
    }
}
