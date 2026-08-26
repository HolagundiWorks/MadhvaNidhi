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
import com.paperdesk.launcher.weather.WeatherClient
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min

/** Chandra kala beside panchanga. 0 = amavasya, 0.5 = purnima. */
class MoonPhaseView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var phase: Float = WeatherClient.moonPhase()
        set(value) {
            field = value
            invalidate()
        }

    private val face = ContextCompat.getColor(context, R.color.moon_lit)
    private val ink = ContextCompat.getColor(context, R.color.moon_shade)
    private val accent = ContextCompat.getColor(context, R.color.accent)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val oval = RectF()

    override fun onDraw(canvas: Canvas) {
        val r = min(width, height) / 2f - 2f
        val cx = width / 2f
        val cy = height / 2f
        val t = ((phase % 1f) + 1f) % 1f
        val moon = Path()
        moon.addCircle(cx, cy, r, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(moon)
        fill.color = ink
        canvas.drawCircle(cx, cy, r, fill)
        val limb = abs(cos(t * 2.0 * Math.PI)).toFloat()
        fill.color = face
        if (t <= 0.5f) {
            canvas.drawRect(cx, cy - r, cx + r, cy + r, fill)
            oval.set(cx - limb * r, cy - r, cx + limb * r, cy + r)
            fill.color = if (t < 0.25f) ink else face
            canvas.drawOval(oval, fill)
        } else {
            canvas.drawRect(cx - r, cy - r, cx, cy + r, fill)
            oval.set(cx - limb * r, cy - r, cx + limb * r, cy + r)
            fill.color = if (t < 0.75f) face else ink
            canvas.drawOval(oval, fill)
        }
        canvas.restore()
        stroke.color = accent
        stroke.strokeWidth = r * 0.08f
        canvas.drawCircle(cx, cy, r, stroke)
    }
}
