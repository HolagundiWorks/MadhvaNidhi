package com.paperdesk.launcher.ui

import android.view.View
import android.widget.GridLayout
import com.paperdesk.launcher.R
import kotlin.math.floor

/** Reflows stock GridLayouts using usable width and the actual configured text size. */
object MaterialLayouts {
    fun bind(root: View) {
        configure(root, R.id.aayadiGrid, 360f, 1)
        configure(root, R.id.aayadiInputsGrid, 150f, 2, gapToken = R.dimen.ds_space_2)
        configure(root, R.id.aayadiResultsGrid, 156f, 3, gapToken = R.dimen.ds_space_2)
        configure(root, R.id.settingsGrid, 360f, 2)
        configure(root, R.id.profileFieldsGrid, 280f, 2, gapToken = R.dimen.ds_space_2)
        fun centerIcons(view: View) {
            if (view is com.google.android.material.button.MaterialButton && view.text.isNullOrEmpty() && view.icon != null) {
                view.iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_START
                androidx.core.view.ViewCompat.setTooltipText(view, view.contentDescription)
            }
            if (view is android.view.ViewGroup) for (i in 0 until view.childCount) centerIcons(view.getChildAt(i))
        }
        centerIcons(root)
    }

    private fun configure(root: View, id: Int, minimumWidth: Float, maximum: Int,
                          firstSpans: Boolean = false, gapToken: Int = R.dimen.ds_space_4) {
        val grid = root.findViewById<GridLayout>(id) ?: return
        var previousColumns = 0
        var previousWidth = 0
        fun reflow() {
            if (grid.width == 0) return
            val density = grid.resources.displayMetrics.density
            val fontScale = grid.resources.configuration.fontScale.coerceAtLeast(1f)
            val usable = (grid.width - grid.paddingLeft - grid.paddingRight) / density / fontScale
            val columns = floor(usable / minimumWidth).toInt().coerceIn(1, maximum)
            if (columns == previousColumns && grid.width == previousWidth) return
            previousColumns = columns
            previousWidth = grid.width
            val gap = grid.resources.getDimensionPixelSize(gapToken)
            val availablePixels = grid.width - grid.paddingLeft - grid.paddingRight
            val cellWidth = (availablePixels - gap * (columns - 1)) / columns
            val children = (0 until grid.childCount).map { grid.getChildAt(it) }
            grid.removeAllViews()
            grid.columnCount = columns
            var cursor = 0
            for ((index, child) in children.withIndex()) {
                val row = cursor / columns
                val column = cursor % columns
                val rowSpan = 1
                val columnSpan = if (firstSpans && index == 0) columns else 1
                val params = GridLayout.LayoutParams(
                    GridLayout.spec(row, rowSpan, GridLayout.FILL),
                    GridLayout.spec(column, columnSpan, GridLayout.START)
                ).apply {
                    width = cellWidth * columnSpan + gap * (columnSpan - 1)
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    rightMargin = if (column + columnSpan < columns) gap else 0
                    bottomMargin = gap
                }
                grid.addView(child, params)
                cursor += columnSpan
            }
            grid.requestLayout()
        }
        grid.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> grid.post { reflow() } }
        grid.post { reflow() }
    }
}
