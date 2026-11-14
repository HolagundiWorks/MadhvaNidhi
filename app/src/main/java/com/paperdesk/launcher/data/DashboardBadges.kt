package com.paperdesk.launcher.data

/**
 * Gamified dashboard badges for Sandhyavandana streaks, weekly completeness,
 * and japa cumulative milestones. Pure evaluation — no prefs of its own.
 */
object DashboardBadges {

    enum class Kind { STREAK, WEEKLY, JAPA }
    enum class Finish { BROWN, SILVER, GOLD }

    /** Lower, middle and upper thirds of each existing milestone ladder. */
    fun finishFor(tier: Tier): Finish {
        val ladder = when (tier.kind) {
            Kind.STREAK -> STREAK_TIERS
            Kind.WEEKLY -> WEEKLY_TIERS
            Kind.JAPA -> JAPA_TIERS
        }
        val rank = ladder.indexOfFirst { it.id == tier.id }.coerceAtLeast(0)
        return when (rank * 3 / ladder.size) {
            0 -> Finish.BROWN
            1 -> Finish.SILVER
            else -> Finish.GOLD
        }
    }

    data class Tier(
        val id: String,
        val kind: Kind,
        val threshold: Long,
        val titleKn: String,
        val titleEn: String,
        val hint: String
    )

    data class Earned(
        val tier: Tier,
        val value: Long,
        val progressToNext: Float,
        val next: Tier?
    )

    /** Sandhyavandana day-streak ladder. */
    val STREAK_TIERS = listOf(
        Tier("streak_1", Kind.STREAK, 1, "ನವಾರಂಭ", "Spark", "First complete day"),
        Tier("streak_3", Kind.STREAK, 3, "ತ್ರಿದಿನ", "Ember", "3-day streak"),
        Tier("streak_7", Kind.STREAK, 7, "ಸಾಪ್ತಾಹಿಕ", "Week flame", "7-day streak"),
        Tier("streak_21", Kind.STREAK, 21, "ಏಕವಿಂಶ", "Steady", "21-day streak"),
        Tier("streak_40", Kind.STREAK, 40, "ನಲವತ್ತು", "Dedicated", "40-day streak"),
        Tier("streak_108", Kind.STREAK, 108, "ಶತಾಷ್ಟೋತ್ತರ", "Sacred 108", "108-day streak"),
        Tier("streak_365", Kind.STREAK, 365, "ಸಂವತ್ಸರ", "Year keeper", "365-day streak")
    )

    /** Complete Sandhya days in the rolling 7-day window. */
    val WEEKLY_TIERS = listOf(
        Tier("week_3", Kind.WEEKLY, 3, "ಪಾದ", "Footing", "3 of 7 days complete"),
        Tier("week_5", Kind.WEEKLY, 5, "ಮಾರ್ಗ", "Path", "5 of 7 days complete"),
        Tier("week_7", Kind.WEEKLY, 7, "ಪರಿಪೂರ್ಣ", "Perfect week", "All 7 days complete")
    )

    /** Lifetime japa cumulative (sum across mantras). */
    val JAPA_TIERS = listOf(
        Tier("japa_108", Kind.JAPA, 108, "ಬೀಜ", "Seed", "108 cumulative"),
        Tier("japa_1001", Kind.JAPA, 1_001, "ಮಾಲೆ", "Mala", "1,001 cumulative"),
        Tier("japa_10001", Kind.JAPA, 10_001, "ಸಹಸ್ರ", "Thousand", "10,001 cumulative"),
        Tier("japa_100001", Kind.JAPA, 1_00_001, "ಲಕ್ಷ", "Lakh", "1,00,001 cumulative"),
        Tier("japa_1000001", Kind.JAPA, 10_00_001, "ದಶಲಕ್ಷ", "Ten lakh", "10,00,001 cumulative")
    )

    fun highestReached(tiers: List<Tier>, value: Long): Tier? =
        tiers.filter { value >= it.threshold }.maxByOrNull { it.threshold }

    fun nextTier(tiers: List<Tier>, value: Long): Tier? =
        tiers.firstOrNull { value < it.threshold }

    fun progressToNext(tiers: List<Tier>, value: Long): Float {
        val next = nextTier(tiers, value) ?: return 1f
        val prev = tiers.filter { it.threshold <= value }.maxByOrNull { it.threshold }?.threshold ?: 0L
        val span = (next.threshold - prev).coerceAtLeast(1L)
        return ((value - prev).toFloat() / span.toFloat()).coerceIn(0f, 1f)
    }

    fun evaluate(tiers: List<Tier>, value: Long): Earned? {
        val current = highestReached(tiers, value) ?: return null
        return Earned(
            tier = current,
            value = value,
            progressToNext = progressToNext(tiers, value),
            next = nextTier(tiers, value)
        )
    }

    fun allEarned(streakDays: Int, weekDays: Int, japaCumulative: Long): List<Earned> {
        val out = mutableListOf<Earned>()
        evaluate(STREAK_TIERS, streakDays.toLong())?.let { out.add(it) }
        evaluate(WEEKLY_TIERS, weekDays.toLong())?.let { out.add(it) }
        evaluate(JAPA_TIERS, japaCumulative)?.let { out.add(it) }
        return out
    }

    /** Preview badges when live history is empty. */
    fun demoEarned(): List<Earned> = listOf(
        Earned(STREAK_TIERS[2], 7, progressToNext(STREAK_TIERS, 7), nextTier(STREAK_TIERS, 7)),
        Earned(WEEKLY_TIERS[1], 5, progressToNext(WEEKLY_TIERS, 5), nextTier(WEEKLY_TIERS, 5)),
        Earned(JAPA_TIERS[2], 10_001, progressToNext(JAPA_TIERS, 10_001), nextTier(JAPA_TIERS, 10_001))
    )

    fun label(tier: Tier): String = "${tier.titleKn} · ${tier.titleEn}"
}
