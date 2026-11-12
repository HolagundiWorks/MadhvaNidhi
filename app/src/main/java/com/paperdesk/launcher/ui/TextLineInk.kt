package com.paperdesk.launcher.ui

import android.graphics.Rect
import android.text.Spanned
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import android.widget.TextView

/** Shaped ink bounds for the actual styled runs, including Indic marks. */
object TextLineInk {
    fun bounds(view: TextView, line: Int): IntRange? {
        val layout = view.layout ?: return null
        val text = view.text
        val end = layout.getLineEnd(line).coerceAtMost(text.length)
        var start = layout.getLineStart(line)
        var top = Int.MAX_VALUE
        var bottom = Int.MIN_VALUE
        while (start < end) {
            val next = if (text is Spanned) text.nextSpanTransition(start, end, MetricAffectingSpan::class.java) else end
            val value = text.subSequence(start, next).toString().trimEnd('\n', '\r')
            if (value.isNotBlank()) {
                val paint = TextPaint(view.paint)
                if (text is Spanned) text.getSpans(start, next, MetricAffectingSpan::class.java).forEach { it.updateMeasureState(paint) }
                val ink = Rect()
                paint.getTextBounds(value, 0, value.length, ink)
                top = minOf(top, ink.top + paint.baselineShift)
                bottom = maxOf(bottom, ink.bottom + paint.baselineShift)
            }
            start = next
        }
        if (top == Int.MAX_VALUE) return null
        val baseline = layout.getLineBaseline(line)
        return (baseline + top - 2)..(baseline + bottom + 2)
    }
}
