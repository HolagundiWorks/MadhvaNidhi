package com.paperdesk.launcher.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.paperdesk.launcher.R

/** Landscape talapatra field: leaf fibres and three bind holes. */
class PalmLeafView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val leaf = ContextCompat.getColor(context, R.color.leaf)
    private val fibre = ContextCompat.getColor(context, R.color.leaf_fibre)
    private val hole = ContextCompat.getColor(context, R.color.leaf_hole)
    private val ink = ContextCompat.getColor(context, R.color.clock_ink)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.SQUARE
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        fill.color = leaf
        canvas.drawRect(0f, 0f, w, h, fill)
        stroke.color = fibre
        stroke.strokeWidth = 1f
        var y = h * 0.08f
        while (y < h) {
            canvas.drawLine(0f, y, w, y + 1f, stroke)
            y += h * 0.07f
        }
        val r = (h * 0.035f).coerceAtMost(10f).coerceAtLeast(4f)
        val x = w * 0.028f + r
        fill.color = hole
        stroke.color = ink
        stroke.strokeWidth = 1.2f
        for (i in 0 until 3) {
            val cy = h * (0.22f + i * 0.28f)
            canvas.drawCircle(x, cy, r, fill)
            canvas.drawCircle(x, cy, r, stroke)
        }
        stroke.color = fibre
        stroke.strokeWidth = 2f
        canvas.drawLine(0f, 1f, w, 1f, stroke)
        canvas.drawLine(0f, h - 2f, w, h - 2f, stroke)
    }
}
