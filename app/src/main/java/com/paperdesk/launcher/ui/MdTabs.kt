package com.paperdesk.launcher.ui

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.tabs.TabLayout
import com.paperdesk.launcher.R

/**
 * Contained Material tabs: left-aligned strip; equal tab widths within one TabLayout
 * (sized to the widest label on that screen only).
 */
object MdTabs {
    fun equalizeLeft(tabs: TabLayout) {
        tabs.tabMode = TabLayout.MODE_FIXED
        tabs.tabGravity = TabLayout.GRAVITY_FILL
        tabs.isTabIndicatorFullWidth = true
        val lp = tabs.layoutParams
        if (lp != null) {
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT
            when (lp) {
                is LinearLayout.LayoutParams -> lp.gravity = Gravity.START
                is android.widget.FrameLayout.LayoutParams -> lp.gravity = Gravity.START
            }
            tabs.layoutParams = lp
        }
    }


}
