package com.paperdesk.launcher.ui

import android.content.res.ColorStateList
import com.google.android.material.button.MaterialButton
import com.paperdesk.launcher.R

/** Shared pressed chrome for Nitya/Puja day-action FABs (mockup `aria-pressed`). */
object NityaDayChrome {
    fun paint(btn: MaterialButton, active: Boolean) {
        btn.isSelected = active
        androidx.core.view.ViewCompat.setStateDescription(btn, if (active) "Selected" else "Not selected")
        val ctx = btn.context
        btn.backgroundTintList = ColorStateList.valueOf(
            ctx.getColor(if (active) R.color.md_theme_secondaryContainer else android.R.color.transparent)
        )
        btn.iconTint = ColorStateList.valueOf(
            ctx.getColor(if (active) R.color.md_theme_primary else R.color.md_theme_onSurfaceVariant)
        )
        btn.strokeColor = ColorStateList.valueOf(
            ctx.getColor(if (active) R.color.md_theme_primary else R.color.md_theme_outline)
        )
        btn.strokeWidth = ctx.resources.getDimensionPixelSize(R.dimen.carbon_border_width).coerceAtLeast(1)
    }
}
