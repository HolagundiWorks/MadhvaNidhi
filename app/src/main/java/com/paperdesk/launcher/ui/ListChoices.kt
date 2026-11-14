package com.paperdesk.launcher.ui

import android.content.res.ColorStateList
import android.view.Gravity
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButton
import com.paperdesk.launcher.R

/** Choices shown as a vertical list of rows, in place of an exposed dropdown. */
object ListChoices {
    fun bind(container: LinearLayout, choices: List<String>, selected: Int = 0, onSelected: (Int) -> Unit) {
        val ctx = container.context
        val gap = ctx.resources.getDimensionPixelSize(R.dimen.ds_space_2)
        val radius = ctx.resources.getDimensionPixelSize(R.dimen.ds_shape_md)
        container.removeAllViews()
        choices.forEachIndexed { index, label ->
            val row = MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = label
                isAllCaps = false
                gravity = Gravity.CENTER_VERTICAL or Gravity.START
                strokeWidth = 0
                cornerRadius = radius
                backgroundTintList = ColorStateList.valueOf(
                    ctx.getColor(if (index == selected) R.color.md_layer_selected else R.color.md_layer_01)
                )
                setTextColor(ctx.getColor(R.color.md_theme_onSurface))
                setOnClickListener {
                    onSelected(index)
                    bind(container, choices, index, onSelected)
                }
            }
            container.addView(
                row,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = gap }
            )
        }
    }
}
