package com.paperdesk.launcher.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.ContextCompat
import com.paperdesk.launcher.R
import com.paperdesk.launcher.util.PomodoroClock
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

class PomodoroStripView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val white = ContextCompat.getColor(context, R.color.white)
    private val accent = ContextCompat.getColor(context, R.color.accent)
    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = white
        textAlign = Paint.Align.CENTER
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accent
        textAlign = Paint.Align.CENTER
    }
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.white_55)
        strokeCap = Paint.Cap.SQUARE
    }
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = accent
        strokeCap = Paint.Cap.ROUND
    }
    private val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = accent
    }
    private val arcRect = RectF()
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            invalidate()
            handler.postDelayed(this, 200)
        }
    }

    private val listener: () -> Unit = { invalidate() }
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
    private val doubleTapTimeout = ViewConfiguration.getDoubleTapTimeout().toLong()

    var onLongPress: (() -> Unit)? = null

    private var ringCx = 0f
    private var ringCy = 0f
    private var ringR = 0f
    private var dragging = false
    private var longFired = false
    private var downX = 0f
    private var downY = 0f
    private var lastTapAt = 0L

    private val longPressRun = Runnable {
        if (!dragging) {
            longFired = true
            onLongPress?.invoke()
        }
    }
    private val toggleRun = Runnable { PomodoroClock.toggle() }

    init {
        isClickable = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        ringCx = w / 2f
        ringCy = h * 0.38f
        ringR = minOf(w, h) * 0.28f
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(tick)
        PomodoroClock.addListener(listener)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(tick)
        handler.removeCallbacks(longPressRun)
        handler.removeCallbacks(toggleRun)
        PomodoroClock.removeListener(listener)
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        parent.requestDisallowInterceptTouchEvent(true)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragging = false
                longFired = false
                downX = event.x
                downY = event.y
                handler.removeCallbacks(toggleRun)
                handler.removeCallbacks(longPressRun)
                handler.postDelayed(longPressRun, longPressTimeout)
            }
            MotionEvent.ACTION_MOVE -> {
                val dist = hypot(event.x - downX, event.y - downY)
                if (!dragging && dist > slop) {
                    dragging = true
                    handler.removeCallbacks(longPressRun)
                }
                if (dragging && !longFired) applyAngle(event.x, event.y)
            }
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(longPressRun)
                if (dragging || longFired) {
                    dragging = false
                    return true
                }
                val now = SystemClock.uptimeMillis()
                if (now - lastTapAt <= doubleTapTimeout) {
                    handler.removeCallbacks(toggleRun)
                    lastTapAt = 0L
                    PomodoroClock.reset()
                } else {
                    lastTapAt = now
                    handler.postDelayed(toggleRun, doubleTapTimeout)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(longPressRun)
                dragging = false
            }
        }
        return true
    }

    private fun applyAngle(x: Float, y: Float) {
        val dx = x - ringCx
        val dy = y - ringCy
        if (dx == 0f && dy == 0f) return
        var deg = Math.toDegrees(atan2(dx.toDouble(), -dy.toDouble()))
        if (deg < 0) deg += 360.0
        val minutes = (deg / 360.0 * 60.0).roundToInt().coerceIn(5, 60)
        PomodoroClock.setDurationFromRing(minutes * 60_000L)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        ringCx = w / 2f
        ringCy = h * 0.38f
        ringR = minOf(w, h) * 0.28f
        val stroke = ringR * 0.12f
        trackPaint.strokeWidth = stroke * 0.35f
        arcPaint.strokeWidth = stroke
        arcRect.set(ringCx - ringR, ringCy - ringR, ringCx + ringR, ringCy + ringR)
        canvas.drawArc(arcRect, -90f, 360f, false, trackPaint)
        val sweep = 360f * (PomodoroClock.remainingMs.toFloat() /
            PomodoroClock.MAX_RING_MS.toFloat().coerceAtLeast(1f))
        canvas.drawArc(arcRect, -90f, sweep, false, arcPaint)
        val rad = Math.toRadians((-90.0 + sweep))
        canvas.drawCircle(
            ringCx + ringR * cos(rad).toFloat(),
            ringCy + ringR * sin(rad).toFloat(),
            stroke * 0.55f,
            knobPaint
        )

        val m = (PomodoroClock.remainingMs / 1000) / 60
        val s = (PomodoroClock.remainingMs / 1000) % 60
        timePaint.textSize = h * 0.10f
        canvas.drawText(String.format("%02d:%02d", m, s), ringCx, ringCy + timePaint.textSize * 0.35f, timePaint)
        labelPaint.textSize = h * 0.045f
        canvas.drawText(PomodoroClock.statusLabel(), ringCx, ringCy + ringR + h * 0.10f, labelPaint)
        val sessionPaint = Paint(timePaint)
        sessionPaint.textSize = h * 0.09f
        sessionPaint.color = white
        canvas.drawText(PomodoroClock.completedCycles.toString(), ringCx, ringCy + ringR + h * 0.22f, sessionPaint)
        labelPaint.textSize = h * 0.035f
        canvas.drawText("SESSION", ringCx, ringCy + ringR + h * 0.28f, labelPaint)
    }
}
