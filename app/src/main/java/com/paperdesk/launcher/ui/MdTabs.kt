package com.paperdesk.launcher.ui

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.tabs.TabLayout
import com.paperdesk.launcher.R
import kotlin.math.ceil

/**
 * Contained Material tabs: left-aligned strip; equal tab widths within one TabLayout
 * (sized to the widest label on that screen only).
 */
object MdTabs {
    fun equalizeLeft(tabs: TabLayout) {
        tabs.tabMode = TabLayout.MODE_SCROLLABLE
        tabs.tabGravity = TabLayout.GRAVITY_START
        tabs.isTabIndicatorFullWidth = true
        val lp = tabs.layoutParams
        if (lp != null) {
            lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
            when (lp) {
                is LinearLayout.LayoutParams -> lp.gravity = Gravity.START
                is android.widget.FrameLayout.LayoutParams -> lp.gravity = Gravity.START
            }
            tabs.layoutParams = lp
        }
        tabs.post { applyEqualWidths(tabs) }
    }

    private fun applyEqualWidths(tabs: TabLayout) {
        val count = tabs.tabCount
        if (count == 0) return
        val pad = tabs.resources.getDimensionPixelSize(R.dimen.md_spacing_05) * 2
        var maxText = 0
        for (i in 0 until count) {
            val tab = tabs.getTabAt(i) ?: continue
            val label = tab.text?.toString().orEmpty()
            val tv = findLabel(tab.view)
            val measured = if (tv != null) {
                ceil(tv.paint.measureText(label)).toInt()
            } else {
                tab.view.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(
                        tabs.height.coerceAtLeast(1),
                        View.MeasureSpec.EXACTLY
                    )
                )
                (tab.view.measuredWidth - pad).coerceAtLeast(0)
            }
            maxText = maxOf(maxText, measured)
        }
        val width = (maxText + pad).coerceAtLeast(
            tabs.resources.getDimensionPixelSize(R.dimen.md_spacing_07)
        )
        for (i in 0 until count) {
            val tabView = tabs.getTabAt(i)?.view ?: continue
            tabView.minimumWidth = width
            val tlp = tabView.layoutParams
            if (tlp is LinearLayout.LayoutParams) {
                tlp.width = width
                tlp.weight = 0f
                tabView.layoutParams = tlp
            } else {
                tabView.layoutParams =
                    LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT)
            }
            tabView.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            findLabel(tabView)?.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        }
        tabs.requestLayout()
    }

    private fun findLabel(root: View): TextView? {
        if (root is TextView) return root
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                findLabel(root.getChildAt(i))?.let { return it }
            }
        }
        return null
    }
}
