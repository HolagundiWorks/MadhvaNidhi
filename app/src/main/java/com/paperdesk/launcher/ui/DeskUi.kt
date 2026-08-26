package com.paperdesk.launcher.ui

import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import com.paperdesk.launcher.R

object DeskUi {
    fun chip(tv: TextView, selected: Boolean) {
        val d = tv.resources.displayMetrics.density
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        tv.setPadding((10 * d).toInt(), (8 * d).toInt(), (10 * d).toInt(), (8 * d).toInt())
        tv.gravity = Gravity.CENTER
        tv.minHeight = tv.resources.getDimensionPixelSize(R.dimen.chip_h)
        tv.minWidth = tv.resources.getDimensionPixelSize(R.dimen.chip_h)
        tv.setBackgroundResource(if (selected) R.drawable.bg_chip_on else R.drawable.bg_chip)
        tv.setTextColor(tv.context.getColor(if (selected) R.color.black else R.color.white_70))
        DeskType.apply(tv, if (selected) DeskType.SEMIBOLD else DeskType.MEDIUM)
        tv.alpha = 1f
    }

    fun dayCell(tv: TextView, selected: Boolean, today: Boolean, ekadasi: Boolean = false, compact: Boolean = false) {
        val d = tv.resources.displayMetrics.density
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, if (compact) 9f else 12f)
        tv.gravity = Gravity.CENTER
        tv.minHeight = if (compact) 0 else tv.resources.getDimensionPixelSize(R.dimen.day_h)
        tv.setPadding(0, (4 * d).toInt(), 0, (4 * d).toInt())
        when {
            selected && ekadasi -> {
                tv.setBackgroundResource(R.drawable.bg_cell_fest)
                tv.setTextColor(tv.context.getColor(R.color.accent))
                DeskType.apply(tv, DeskType.SEMIBOLD)
                tv.alpha = 1f
            }
            selected -> {
                tv.setBackgroundResource(R.drawable.bg_cell_on)
                tv.setTextColor(tv.context.getColor(R.color.accent))
                DeskType.apply(tv, DeskType.SEMIBOLD)
                tv.alpha = 1f
            }
            ekadasi -> {
                tv.setBackgroundResource(R.drawable.bg_cell_fest)
                tv.setTextColor(tv.context.getColor(if (today) R.color.accent else R.color.white_70))
                DeskType.apply(tv, DeskType.MEDIUM)
                tv.alpha = 1f
            }
            today -> {
                tv.setBackgroundResource(R.drawable.bg_cell)
                tv.setTextColor(tv.context.getColor(R.color.accent))
                DeskType.apply(tv, DeskType.MEDIUM)
                tv.alpha = 1f
            }
            else -> {
                tv.setBackgroundResource(R.drawable.bg_cell)
                tv.setTextColor(tv.context.getColor(R.color.white_70))
                DeskType.apply(tv, DeskType.REGULAR)
                tv.alpha = 1f
            }
        }
    }
}
