package com.paperdesk.launcher.ui

import android.content.Context
import android.os.SystemClock
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewParent
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

class ClockLockLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    var onDoubleTap: (() -> Unit)? = null

    private var lastUp = 0L

    init {
        isClickable = true
        isFocusable = true
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
            disallowPager()
        }
        if (ev.actionMasked == MotionEvent.ACTION_UP) {
            val now = SystemClock.uptimeMillis()
            if (now - lastUp in 40L..1500L) {
                lastUp = 0L
                onDoubleTap?.invoke()
            } else {
                lastUp = now
            }
        }
        return true
    }

    private fun disallowPager() {
        var p: ViewParent? = parent
        while (p != null) {
            if (p is RecyclerView || p is ViewPager2) {
                p.requestDisallowInterceptTouchEvent(true)
                return
            }
            p = (p as? View)?.parent
        }
    }
}
