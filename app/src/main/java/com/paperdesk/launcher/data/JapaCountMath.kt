package com.paperdesk.launcher.data

/** One accepted count change is applied equally to today and cumulative. */
object JapaCountMath {
    data class Change(val daily: Long, val cumulative: Long, val delta: Long)
    fun add(daily: Long, cumulative: Long, requested: Long): Change {
        val d = daily.coerceAtLeast(0)
        val c = cumulative.coerceAtLeast(0)
        val accepted = requested.coerceAtLeast(0).coerceAtMost(Long.MAX_VALUE - d).coerceAtMost(Long.MAX_VALUE - c)
        return Change(d + accepted, c + accepted, accepted)
    }
    fun undo(daily: Long, cumulative: Long, last: Long): Change {
        val d = daily.coerceAtLeast(0)
        val c = cumulative.coerceAtLeast(0)
        val removed = last.coerceAtLeast(0).coerceAtMost(d).coerceAtMost(c)
        return Change(d - removed, c - removed, removed)
    }
}
