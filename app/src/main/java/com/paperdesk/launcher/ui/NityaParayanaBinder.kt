package com.paperdesk.launcher.ui

import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.NityaParayanaStore

/** Shared binder for Nitya Parayana reading surface (Nitya Karma tab or standalone). */
object NityaParayanaBinder {
    fun bind(fragment: Fragment, root: View) {
        val context = fragment.requireContext()
        val sections = NityaParayanaStore.load(context)
        val prefs = context.getSharedPreferences("madhvanidhi_nitya", 0)
        val selected = prefs.getInt("section", 0).coerceIn(0, (sections.size - 1).coerceAtLeast(0))
        fun show(index: Int) {
            root.findViewById<TextView>(R.id.nityaReading).apply {
                text = sections.getOrNull(index)?.body.orEmpty()
                DeskType.reading(this)
            }
            prefs.edit().putInt("section", index).apply()
            ScreenPages.first(root)
        }
        if (sections.isNotEmpty()) {
            MaterialChoices.bind(root.findViewById(R.id.nityaSection), sections.map { it.title }, selected) { show(it) }
            show(selected)
        }
    }
}
