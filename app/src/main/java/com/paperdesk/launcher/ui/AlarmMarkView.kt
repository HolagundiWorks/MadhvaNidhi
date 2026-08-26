package com.paperdesk.launcher.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.paperdesk.launcher.R
import kotlin.math.min

class AlarmMarkView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var active: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val path = Path()
    private val oval = RectF()
    private val white = ContextCompat.getColor(context, R.color.white)
    private val accent = ContextCompat.getColor(context, R.color.accent)

    override fun onDraw(canvas: Canvas) {
        val d = min(width, height).toFloat()
        val cx = width / 2f
        val cy = height / 2f
        val color = if (active) accent else white
        paint.color = color
        fill.color = color
        paint.strokeWidth = d * 0.08f

        oval.set(cx - d * 0.28f, cy - d * 0.32f, cx + d * 0.28f, cy + d * 0.18f)
        canvas.drawArc(oval, 200f, 140f, false, paint)
        path.reset()
        path.moveTo(cx - d * 0.3f, cy + d * 0.08f)
        path.quadTo(cx, cy + d * 0.22f, cx + d * 0.3f, cy + d * 0.08f)
        canvas.drawPath(path, paint)
        canvas.drawLine(cx, cy - d * 0.38f, cx, cy - d * 0.28f, paint)
        canvas.drawCircle(cx, cy + d * 0.3f, d * 0.045f, fill)
    }
}
