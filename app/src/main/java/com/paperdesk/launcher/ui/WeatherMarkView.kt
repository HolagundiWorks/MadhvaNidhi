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
import com.paperdesk.launcher.weather.WeatherKind
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class WeatherMarkView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var kind: WeatherKind = WeatherKind.SUN
        set(value) {
            field = value
            invalidate()
        }
    var night: Boolean = false
        set(value) {
            field = value
            invalidate()
        }
    var moonPhase: Float = 0.5f
        set(value) {
            field = value
            invalidate()
        }

    private val black = ContextCompat.getColor(context, R.color.black)
    private val white = ContextCompat.getColor(context, R.color.white)
    private val accent = ContextCompat.getColor(context, R.color.accent)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()
    private val oval = RectF()

    override fun onDraw(canvas: Canvas) {
        val d = min(width, height).toFloat()
        val cx = width / 2f
        val cy = height / 2f
        stroke.strokeWidth = d * 0.045f
        when (kind) {
            WeatherKind.SUN -> {
                if (night) drawMoon(canvas, cx, cy, d * 0.28f)
                else drawSun(canvas, cx, cy, d * 0.28f, d)
            }
            WeatherKind.CLOUD_SUN -> {
                drawCloud(canvas, cx + d * 0.04f, cy + d * 0.08f, d * 0.78f, translucent = true)
                val mx = cx - d * 0.16f
                val my = cy - d * 0.18f
                fill.color = black
                fill.alpha = 255
                if (night) {
                    canvas.drawCircle(mx, my, d * 0.26f, fill)
                    drawMoon(canvas, mx, my, d * 0.16f)
                } else {
                    canvas.drawCircle(mx, my, d * 0.32f, fill)
                    drawSun(canvas, mx, my, d * 0.16f, d * 0.7f)
                }
            }
            WeatherKind.CLOUD, WeatherKind.FOG -> {
                drawCloud(canvas, cx, cy - d * 0.04f, d * 0.82f)
                if (kind == WeatherKind.FOG) {
                    stroke.color = white
                    val y = cy + d * 0.28f
                    canvas.drawLine(cx - d * 0.28f, y, cx + d * 0.28f, y, stroke)
                    canvas.drawLine(cx - d * 0.2f, y + d * 0.1f, cx + d * 0.22f, y + d * 0.1f, stroke)
                }
            }
            WeatherKind.RAIN -> {
                drawCloud(canvas, cx, cy - d * 0.12f, d * 0.72f)
                stroke.color = accent
                val y0 = cy + d * 0.18f
                canvas.drawLine(cx - d * 0.16f, y0, cx - d * 0.22f, y0 + d * 0.2f, stroke)
                canvas.drawLine(cx, y0, cx - d * 0.06f, y0 + d * 0.22f, stroke)
                canvas.drawLine(cx + d * 0.16f, y0, cx + d * 0.1f, y0 + d * 0.2f, stroke)
            }
            WeatherKind.SNOW -> {
                drawCloud(canvas, cx, cy - d * 0.12f, d * 0.72f)
                fill.color = white
                canvas.drawCircle(cx - d * 0.14f, cy + d * 0.26f, d * 0.035f, fill)
                canvas.drawCircle(cx, cy + d * 0.32f, d * 0.035f, fill)
                canvas.drawCircle(cx + d * 0.14f, cy + d * 0.26f, d * 0.035f, fill)
            }
            WeatherKind.STORM -> {
                drawCloud(canvas, cx, cy - d * 0.16f, d * 0.72f)
                fill.color = accent
                path.reset()
                path.moveTo(cx + d * 0.02f, cy + d * 0.02f)
                path.lineTo(cx - d * 0.08f, cy + d * 0.18f)
                path.lineTo(cx + d * 0.02f, cy + d * 0.18f)
                path.lineTo(cx - d * 0.06f, cy + d * 0.38f)
                path.lineTo(cx + d * 0.14f, cy + d * 0.14f)
                path.lineTo(cx + d * 0.04f, cy + d * 0.14f)
                path.close()
                canvas.drawPath(path, fill)
            }
        }
    }

    private fun drawMoon(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val t = ((moonPhase % 1f) + 1f) % 1f
        val moon = Path()
        moon.addCircle(cx, cy, r, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(moon)
        val limb = kotlin.math.abs(kotlin.math.cos(t * 2.0 * Math.PI)).toFloat()
        fill.color = white
        if (t <= 0.5f) {
            canvas.drawRect(cx, cy - r, cx + r, cy + r, fill)
            oval.set(cx - limb * r, cy - r, cx + limb * r, cy + r)
            fill.color = if (t < 0.25f) black else white
            canvas.drawOval(oval, fill)
        } else {
            canvas.drawRect(cx - r, cy - r, cx, cy + r, fill)
            oval.set(cx - limb * r, cy - r, cx + limb * r, cy + r)
            fill.color = if (t < 0.75f) white else black
            canvas.drawOval(oval, fill)
        }
        canvas.restore()
        stroke.color = white
        stroke.strokeWidth = r * 0.08f
        canvas.drawCircle(cx, cy, r, stroke)
    }

    private fun drawSun(canvas: Canvas, cx: Float, cy: Float, r: Float, d: Float) {
        fill.color = accent
        canvas.drawCircle(cx, cy, r, fill)
        stroke.color = accent
        stroke.strokeWidth = d * 0.035f
        val inner = r + d * 0.06f
        val outer = r + d * 0.14f
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0)
            canvas.drawLine(
                cx + inner * cos(a).toFloat(),
                cy + inner * sin(a).toFloat(),
                cx + outer * cos(a).toFloat(),
                cy + outer * sin(a).toFloat(),
                stroke
            )
        }
    }

    private fun drawCloud(canvas: Canvas, cx: Float, cy: Float, d: Float, translucent: Boolean = false) {
        fill.color = white
        fill.alpha = if (translucent) 128 else 255
        canvas.drawCircle(cx - d * 0.16f, cy, d * 0.16f, fill)
        canvas.drawCircle(cx + d * 0.14f, cy + d * 0.02f, d * 0.14f, fill)
        canvas.drawCircle(cx, cy - d * 0.08f, d * 0.18f, fill)
        oval.set(cx - d * 0.32f, cy - d * 0.02f, cx + d * 0.3f, cy + d * 0.18f)
        canvas.drawRoundRect(oval, d * 0.1f, d * 0.1f, fill)
        fill.alpha = 255
    }
}
