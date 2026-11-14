package com.paperdesk.launcher.ui

import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.NityaParayanaStore
import com.paperdesk.launcher.data.OsLang

/** Shared binder for Nitya Parayana portrait selection + landscape ContentReaderActivity launch. */
object NityaParayanaBinder {
    fun bind(fragment: Fragment, root: View) {
        val context = fragment.requireContext()
        val sections = NityaParayanaStore.load(context)
        val prefs = context.getSharedPreferences("madhvanidhi_nitya", 0)
        var current = prefs.getInt("section", 0).coerceIn(0, (sections.size - 1).coerceAtLeast(0))
        val openBtn = root.findViewById<MaterialButton>(R.id.nityaOpenContent)
        openBtn.text = OsLang.ui("open_content", OsLang.get(context))
        fun show(index: Int) {
            current = index
            root.findViewById<TextView>(R.id.nityaReading).apply {
                text = sections.getOrNull(index)?.body.orEmpty()
                DeskType.reading(this)
            }
            prefs.edit().putInt("section", index).apply()
        }
        if (sections.isNotEmpty()) {
            MaterialChoices.bind(root.findViewById(R.id.nityaSection), sections.map { it.title }, current) { show(it) }
            show(current)
            openBtn.isEnabled = true
            openBtn.setOnClickListener {
                fragment.startActivity(ContentReaderActivity.forNityaParayana(context, current))
            }
        } else {
            openBtn.isEnabled = false
        }
    }
}
