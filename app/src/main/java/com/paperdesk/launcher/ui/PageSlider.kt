package com.paperdesk.launcher.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.paperdesk.launcher.R
import kotlin.math.roundToInt

/** Slim footer scrubber. Page changes only while pressing this bar. */
class PageSlider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.white_45)
    }
    private val thumb = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.accent)
    }
    private val rect = RectF()
    private var pager: ViewPager2? = null
    private var pages = 1
    private var progress = 0f
    private var dragging = false

    fun attach(target: ViewPager2) {
        pager = target
        pages = (target.adapter?.itemCount ?: 1).coerceAtLeast(1)
        progress = target.currentItem.toFloat()
        target.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                if (dragging) return
                progress = position + positionOffset
                invalidate()
            }

            override fun onPageSelected(position: Int) {
                if (dragging) return
                progress = position.toFloat()
                invalidate()
            }
        })
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragging = true
                parent.requestDisallowInterceptTouchEvent(true)
                seek(event.x)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragging) seek(event.x)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (dragging) {
                    seek(event.x)
                    snap()
                    dragging = false
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun seek(x: Float) {
        val w = (width - paddingLeft - paddingRight).toFloat().coerceAtLeast(1f)
        val t = ((x - paddingLeft) / w).coerceIn(0f, 1f)
        val max = (pages - 1).coerceAtLeast(1)
        progress = t * max
        val idx = progress.roundToInt().coerceIn(0, pages - 1)
        val p = pager ?: return
        if (p.currentItem != idx) p.setCurrentItem(idx, false)
        invalidate()
    }

    private fun snap() {
        val idx = progress.roundToInt().coerceIn(0, pages - 1)
        progress = idx.toFloat()
        pager?.setCurrentItem(idx, true)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val left = paddingLeft.toFloat()
        val right = (width - paddingRight).toFloat()
        val cy = height / 2f
        val trackH = 2f * d
        rect.set(left, cy - trackH / 2f, right, cy + trackH / 2f)
        canvas.drawRoundRect(rect, trackH, trackH, track)

        val max = (pages - 1).coerceAtLeast(1)
        val t = (progress / max).coerceIn(0f, 1f)
        val thumbW = 18f * d
        val thumbH = 4f * d
        val x = left + t * (right - left)
        rect.set(x - thumbW / 2f, cy - thumbH / 2f, x + thumbW / 2f, cy + thumbH / 2f)
        canvas.drawRoundRect(rect, thumbH, thumbH, thumb)
    }
}
