package com.paperdesk.launcher.ui

/** Keeps drawn content whole while omitting empty paragraph space at page boundaries. */
object ContentPagination {
    data class Page(val start: Int, val end: Int)

    fun paginate(height: Int, total: Int, boundaries: Collection<Int>, forbidden: List<IntRange>,
                 meaningful: List<IntRange>, leadingGap: Int): List<Page> {
        require(height > 0)
        val safe = boundaries.filter { it in 0..total && forbidden.none { range -> it in range } }.sorted()
        val regions = meaningful.sortedBy { it.first }
        val pages = mutableListOf<Page>()
        var cursor = 0
        while (cursor < total) {
            val next = regions.firstOrNull { it.last > cursor } ?: break
            var start = cursor
            if (cursor > 0 && next.first - cursor > leadingGap) {
                val proposed = (next.first - leadingGap).coerceAtLeast(cursor)
                if (forbidden.none { proposed in it }) start = proposed
            }
            val limit = (start + height).coerceAtMost(total)
            val end = safe.lastOrNull { it > start && it <= limit } ?: limit
            if (regions.any { it.first < end && it.last > start }) pages += Page(start, end)
            cursor = end
        }
        return pages.ifEmpty { listOf(Page(0, total.coerceIn(1, height))) }
    }
}
