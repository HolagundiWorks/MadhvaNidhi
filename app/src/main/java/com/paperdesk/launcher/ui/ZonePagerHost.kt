package com.paperdesk.launcher.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.viewpager2.widget.ViewPager2
import com.paperdesk.launcher.R

/**
 * Horizontal paging only when the gesture starts in the header or footer.
 * A touch that begins in the middle content band cannot change pages.
 */
class ZonePagerHost @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var pageSwipeAllowed = true
    private val chrome = resources.getDimension(R.dimen.chrome_bar)

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            val pager = findViewById<ViewPager2>(R.id.pager)
            if (pager != null) {
                val h = height.toFloat().coerceAtLeast(1f)
                val y = ev.y
                val inContent = y > chrome && y < h - chrome
                pager.isUserInputEnabled = pageSwipeAllowed && !inContent
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}
