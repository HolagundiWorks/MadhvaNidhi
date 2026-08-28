package com.paperdesk.launcher.ui

import android.R.attr.state_pressed
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.ColorInt
import com.paperdesk.launcher.R
import kotlin.math.min

/** Page palettes and woven chrome are kept here so layouts consume one coherent token system. */
object PagePalette {
    @ColorInt
    fun surface(context: Context, pageId: String): Int = context.getColor(
        when (pageId) {
            "sandhya" -> R.color.earth_canvas_sandhya
            "nitya" -> R.color.earth_canvas_nitya
            "mantras" -> R.color.earth_canvas_mantras
            "puja" -> R.color.earth_canvas_puja
            "hadugalu" -> R.color.earth_canvas_hadugalu
            "japa" -> R.color.earth_canvas_japa
            "aayadi" -> R.color.earth_canvas_aayadi
            else -> R.color.earth_canvas_desk
        }
    )

    @ColorInt
    fun accent(context: Context, pageId: String): Int = context.getColor(
        when (pageId) {
            "sandhya" -> R.color.earth_accent_sandhya
            "nitya" -> R.color.earth_accent_nitya
            "mantras" -> R.color.earth_accent_mantras
            "puja" -> R.color.earth_accent_puja
            "hadugalu" -> R.color.earth_accent_hadugalu
            "japa" -> R.color.earth_accent_japa
            "aayadi" -> R.color.earth_accent_aayadi
            else -> R.color.earth_accent_desk
        }
    )

    fun apply(view: View, pageId: String) {
        view.setBackgroundColor(surface(view.context, pageId))
    }
}

/** Repeating diamonds and warp lines evoke a restrained woven saree border. */
class SareeFooterDrawable(context: Context, @ColorInt private val accent: Int) : Drawable() {
    private val density = context.resources.displayMetrics.density
    private val base = context.getColor(R.color.earth_footer_base)
    private val shadow = context.getColor(R.color.earth_footer_shadow)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var drawableAlpha = 255

    override fun draw(canvas: Canvas) {
        val b = bounds
        paint.style = Paint.Style.FILL
        paint.color = withAlpha(base, drawableAlpha)
        canvas.drawRect(b, paint)

        val edge = 2f * density
        paint.color = withAlpha(accent, drawableAlpha)
        canvas.drawRect(b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.top + edge, paint)
        canvas.drawRect(b.left.toFloat(), b.bottom - edge, b.right.toFloat(), b.bottom.toFloat(), paint)

        val mid = b.exactCenterY()
        val unit = 8f * density
        val repeat = unit * 2f
        var x = b.left.toFloat() - repeat
        while (x < b.right + repeat) {
            path.reset()
            path.moveTo(x, mid)
            path.lineTo(x + unit, mid - unit * 0.62f)
            path.lineTo(x + repeat, mid)
            path.lineTo(x + unit, mid + unit * 0.62f)
            path.close()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f * density
            paint.color = withAlpha(accent, (drawableAlpha * 0.58f).toInt())
            canvas.drawPath(path, paint)
            paint.style = Paint.Style.FILL
            paint.color = withAlpha(shadow, drawableAlpha)
            canvas.drawCircle(x + unit, mid, 2.2f * density, paint)
            paint.color = withAlpha(accent, drawableAlpha)
            canvas.drawCircle(x + unit, mid, 0.9f * density, paint)
            x += repeat
        }

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f * density
        paint.color = withAlpha(accent, (drawableAlpha * 0.35f).toInt())
        canvas.drawLine(b.left.toFloat(), mid - unit, b.right.toFloat(), mid - unit, paint)
        canvas.drawLine(b.left.toFloat(), mid + unit, b.right.toFloat(), mid + unit, paint)
    }

    override fun setAlpha(alpha: Int) {
        drawableAlpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Android")
    override fun getOpacity(): Int = PixelFormat.OPAQUE

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
}

/** Ornamental medallion used by footer controls so each button belongs to the weave. */
class FooterMedallionDrawable(context: Context, @ColorInt private val accent: Int) : Drawable() {
    private val density = context.resources.displayMetrics.density
    private val base = context.getColor(R.color.earth_footer_base)
    private val shadow = context.getColor(R.color.earth_footer_shadow)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var pressed = false
    private var drawableAlpha = 255

    override fun draw(canvas: Canvas) {
        val cx = bounds.exactCenterX()
        val cy = bounds.exactCenterY()
        val radius = min(bounds.width(), bounds.height()) / 2f - density
        paint.style = Paint.Style.FILL
        paint.color = withAlpha(if (pressed) accent else base, drawableAlpha)
        canvas.drawCircle(cx, cy, radius, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f * density
        paint.color = withAlpha(accent, drawableAlpha)
        canvas.drawCircle(cx, cy, radius, paint)
        paint.strokeWidth = 1f * density
        paint.color = withAlpha(if (pressed) shadow else accent, (drawableAlpha * 0.62f).toInt())
        canvas.drawCircle(cx, cy, radius - 4f * density, paint)

        paint.style = Paint.Style.FILL
        paint.color = withAlpha(accent, drawableAlpha)
        val dotRadius = 1.2f * density
        val orbit = radius - 2.5f * density
        canvas.drawCircle(cx, cy - orbit, dotRadius, paint)
        canvas.drawCircle(cx + orbit, cy, dotRadius, paint)
        canvas.drawCircle(cx, cy + orbit, dotRadius, paint)
        canvas.drawCircle(cx - orbit, cy, dotRadius, paint)
    }

    override fun isStateful(): Boolean = true

    override fun onStateChange(state: IntArray): Boolean {
        val next = state.any { it == state_pressed }
        if (next == pressed) return false
        pressed = next
        invalidateSelf()
        return true
    }

    override fun setAlpha(alpha: Int) {
        drawableAlpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Android")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
}
