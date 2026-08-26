package com.paperdesk.launcher.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.paperdesk.launcher.R
import com.paperdesk.launcher.weather.WeatherClient
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class AnalogClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var moonPhase: Float = WeatherClient.moonPhase()
        set(value) {
            field = value
            invalidate()
        }

    private val accent = ContextCompat.getColor(context, R.color.accent)
    private val faceColor = ContextCompat.getColor(context, R.color.clock_face)
    private val ink = ContextCompat.getColor(context, R.color.clock_ink)
    private val moonShade = ContextCompat.getColor(context, R.color.moon_shade)

    private val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = faceColor
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ink
        strokeCap = Paint.Cap.SQUARE
    }
    private val handPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ink
        strokeCap = Paint.Cap.BUTT
    }
    private val secondPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = accent
        strokeCap = Paint.Cap.ROUND
    }
    private val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = accent
    }
    private val moonFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        isFilterBitmap = true
    }
    private val moonClip = Path()
    private val moonDest = RectF()
    private val moonOval = RectF()
    private val moonBmp: Bitmap? = runCatching {
        BitmapFactory.decodeResource(resources, R.drawable.moon_full)
    }.getOrNull()

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private val drawHandler = Handler(Looper.getMainLooper())
    private val redraw = object : Runnable {
        override fun run() {
            invalidate()
            drawHandler.postDelayed(this, 200)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        drawHandler.removeCallbacks(redraw)
        drawHandler.post(redraw)
    }

    override fun onDetachedFromWindow() {
        drawHandler.removeCallbacks(redraw)
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val size = (min(w, h) * 0.98f).toInt().coerceAtLeast(1)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        val d = min(width, height).toFloat()
        val cx = width / 2f
        val cy = height / 2f
        val faceR = d / 2f - d * 0.04f

        canvas.drawCircle(cx, cy, faceR, facePaint)
        tickPaint.strokeWidth = d * 0.008f
        canvas.drawCircle(cx, cy, faceR, tickPaint)

        drawMoon(canvas, cx, cy, d * 0.24f)

        tickPaint.strokeWidth = d * 0.01f
        val tickLen = faceR * 0.09f
        for (i in 0 until 4) {
            val a = Math.toRadians((i * 90).toDouble())
            val inner = faceR - tickLen
            canvas.drawLine(
                cx + inner * sin(a).toFloat(),
                cy - inner * cos(a).toFloat(),
                cx + (faceR - d * 0.012f) * sin(a).toFloat(),
                cy - (faceR - d * 0.012f) * cos(a).toFloat(),
                tickPaint
            )
        }

        val cal = Calendar.getInstance()
        val hourAngle = Math.toRadians(((cal.get(Calendar.HOUR) + cal.get(Calendar.MINUTE) / 60f) * 30f).toDouble())
        val minAngle = Math.toRadians(((cal.get(Calendar.MINUTE) + cal.get(Calendar.SECOND) / 60f) * 6f).toDouble())
        val secAngle = Math.toRadians(((cal.get(Calendar.SECOND) + cal.get(Calendar.MILLISECOND) / 1000f) * 6f).toDouble())

        handPaint.strokeWidth = d * 0.028f
        drawHand(canvas, cx, cy, hourAngle, faceR * 0.42f, handPaint)
        handPaint.strokeWidth = d * 0.018f
        drawHand(canvas, cx, cy, minAngle, faceR * 0.62f, handPaint)
        secondPaint.strokeWidth = d * 0.007f
        drawHand(canvas, cx, cy, secAngle, faceR * 0.72f, secondPaint)
        canvas.drawCircle(cx, cy, d * 0.022f, hubPaint)
    }

    private fun drawMoon(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val t = ((moonPhase % 1f) + 1f) % 1f
        moonClip.reset()
        moonClip.addCircle(cx, cy, r, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(moonClip)
        val bmp = moonBmp
        if (bmp != null) {
            val z = r * 1.08f
            moonDest.set(cx - z, cy - z, cx + z, cy + z)
            canvas.drawBitmap(bmp, null, moonDest, moonFill)
        } else {
            moonFill.color = moonShade
            canvas.drawCircle(cx, cy, r, moonFill)
        }
        val limb = abs(cos(t * 2.0 * Math.PI)).toFloat()
        moonOval.set(cx - limb * r, cy - r, cx + limb * r, cy + r)
        val ovalPath = Path()
        ovalPath.addOval(moonOval, Path.Direction.CW)
        moonFill.color = 0xD9121212.toInt()
        if (t < 0.03f || t >= 0.97f) {
            canvas.drawCircle(cx, cy, r, moonFill)
        } else if (t <= 0.5f) {
            canvas.save()
            canvas.clipRect(cx - r, cy - r, cx, cy + r)
            if (t >= 0.25f) {
                @Suppress("DEPRECATION")
                canvas.clipPath(ovalPath, android.graphics.Region.Op.DIFFERENCE)
            }
            canvas.drawRect(cx - r, cy - r, cx, cy + r, moonFill)
            canvas.restore()
            if (t < 0.25f) canvas.drawOval(moonOval, moonFill)
        } else {
            canvas.save()
            canvas.clipRect(cx, cy - r, cx + r, cy + r)
            if (t <= 0.75f) {
                @Suppress("DEPRECATION")
                canvas.clipPath(ovalPath, android.graphics.Region.Op.DIFFERENCE)
            }
            canvas.drawRect(cx, cy - r, cx + r, cy + r, moonFill)
            canvas.restore()
            if (t > 0.75f) canvas.drawOval(moonOval, moonFill)
        }
        canvas.restore()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean = false

    private fun drawHand(canvas: Canvas, cx: Float, cy: Float, angle: Double, length: Float, paint: Paint) {
        canvas.drawLine(
            cx,
            cy,
            cx + length * sin(angle).toFloat(),
            cy - length * cos(angle).toFloat(),
            paint
        )
    }
}
