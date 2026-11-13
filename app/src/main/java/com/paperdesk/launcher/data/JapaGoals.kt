package com.paperdesk.launcher.data

/** Daily / Sankalpa ladders and auto next-level resolution (numbers only). */
object JapaGoals {
    val DAILY = listOf(11L, 101L, 1_001L, 5_001L)
    /** Sankalpa ladder (formerly “major”). */
    val SANKALPA = listOf(5_001L, 10_001L, 50_001L, 1_00_001L, 10_00_001L)

    const val DEFAULT_DAILY = 101L
    /** 0 means “not set → auto from cumulative”. */
    const val SANKALPA_AUTO = 0L

    /** Next Sankalpa rung strictly above [cumulative]; last rung if already past all. */
    fun autoSankalpa(cumulative: Long): Long =
        SANKALPA.firstOrNull { it > cumulative.coerceAtLeast(0) } ?: SANKALPA.last()

    fun resolveSankalpa(configured: Long, cumulative: Long): Long =
        if (configured > 0) configured else autoSankalpa(cumulative)

    fun nearestDaily(value: Long): Long =
        DAILY.minByOrNull { kotlin.math.abs(it - value) } ?: DEFAULT_DAILY
}
