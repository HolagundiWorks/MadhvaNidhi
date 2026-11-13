package com.paperdesk.launcher.ui

import android.content.res.ColorStateList
import com.google.android.material.button.MaterialButton
import com.paperdesk.launcher.R

/** Shared pressed chrome for Nitya/Puja day-action FABs (mockup `aria-pressed`). */
object NityaDayChrome {
    fun paint(btn: MaterialButton, active: Boolean) {
        val ctx = btn.context
        btn.backgroundTintList = ColorStateList.valueOf(
            ctx.getColor(if (active) R.color.carbon_layer_selected else android.R.color.transparent)
        )
        btn.iconTint = ColorStateList.valueOf(
            ctx.getColor(if (active) R.color.carbon_interactive else R.color.carbon_icon_secondary)
        )
        btn.strokeColor = ColorStateList.valueOf(
            ctx.getColor(if (active) R.color.carbon_border_interactive else R.color.carbon_border_strong)
        )
        btn.strokeWidth = ctx.resources.getDimensionPixelSize(R.dimen.carbon_border_width).coerceAtLeast(1)
    }
}
