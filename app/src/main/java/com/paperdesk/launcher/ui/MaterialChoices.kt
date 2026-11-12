package com.paperdesk.launcher.ui

import android.widget.ArrayAdapter
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.paperdesk.launcher.R

/** Binding helpers for the standard Material exposed dropdown; no custom views. */
object MaterialChoices {
    fun dismiss(root: android.view.View) {
        if (root is MaterialAutoCompleteTextView) root.dismissDropDown()
        if (root is android.view.ViewGroup) {
            for (index in 0 until root.childCount) dismiss(root.getChildAt(index))
        }
    }

    fun bind(view: MaterialAutoCompleteTextView, choices: List<String>, selected: Int = 0, onSelected: (Int) -> Unit = {}) {
        view.setTextAppearance(R.style.TextAppearance_Carbon_BodyCompact01)
        view.setSingleLine(true)
        view.ellipsize = android.text.TextUtils.TruncateAt.END
        view.setAdapter(ArrayAdapter(view.context, R.layout.item_dropdown, choices))
        select(view, selected)
        view.setOnItemClickListener { _, _, position, _ ->
            view.tag = position
            onSelected(position)
        }
    }
    fun select(view: MaterialAutoCompleteTextView, position: Int) {
        val safe = position.coerceIn(0, (view.adapter?.count ?: 1) - 1)
        view.tag = safe
        view.setText(view.adapter?.getItem(safe)?.toString().orEmpty(), false)
    }
    fun position(view: MaterialAutoCompleteTextView): Int = view.tag as? Int ?: 0
    fun value(view: MaterialAutoCompleteTextView): String =
        view.adapter?.getItem(position(view))?.toString().orEmpty().ifBlank { view.text?.toString().orEmpty() }
}
