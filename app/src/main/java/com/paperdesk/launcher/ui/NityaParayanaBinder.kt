package com.paperdesk.launcher.ui

import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.NityaParayanaStore
import com.paperdesk.launcher.data.OsLang
import com.paperdesk.launcher.data.PracticeDayStore

/** Shared binder for Nitya Parayana reading surface (Nitya Karma tab or standalone). */
object NityaParayanaBinder {
    fun bind(fragment: Fragment, root: View) {
        val context = fragment.requireContext()
        val practice = PracticeDayStore(context)
        val sections = NityaParayanaStore.load(context)
        val prefs = context.getSharedPreferences("madhvanidhi_nitya", 0)
        var selected = prefs.getInt("section", 0).coerceIn(0, (sections.size - 1).coerceAtLeast(0))
        val lang = OsLang.get(context)

        fun paintDay() {
            val skipped = practice.parayanaSkipped()
            val complete = practice.parayanaComplete()
            root.findViewById<MaterialButton>(R.id.parayanaSkip)?.let { NityaDayChrome.paint(it, skipped) }
            root.findViewById<MaterialButton>(R.id.parayanaComplete)?.let { NityaDayChrome.paint(it, complete) }
        }

        fun show(index: Int) {
            val title = sections.getOrNull(index)?.title.orEmpty()
            root.findViewById<TextView>(R.id.nityaReading).apply {
                text = ""
                visibility = View.GONE
            }
            root.findViewById<TextView>(R.id.parayanaCrumb)?.text =
                listOf(OsLang.ui("nityakarma", lang), OsLang.ui("nitya", lang), title)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                    .ifBlank { "ನಿತ್ಯಕರ್ಮ · ನಿತ್ಯ ಪಾರಾಯಣ" }
            prefs.edit().putInt("section", index).apply()
        }

        if (sections.isNotEmpty()) {
            MaterialChoices.bind(root.findViewById(R.id.nityaSection), sections.map { it.title }, selected) {
                selected = it
                show(it)
                fragment.startActivity(ContentReaderActivity.forNityaParayana(context, it))
            }
            show(selected)
        }

        root.findViewById<View>(R.id.parayanaRead).apply {
            isEnabled = sections.isNotEmpty()
            setOnClickListener {
                fragment.startActivity(ContentReaderActivity.forNityaParayana(context, selected))
            }
        }

        root.findViewById<View>(R.id.parayanaSkip)?.setOnClickListener {
            val on = !practice.parayanaSkipped()
            practice.setParayanaDay(complete = false, skipped = on)
            paintDay()
        }
        root.findViewById<View>(R.id.parayanaComplete)?.setOnClickListener {
            val on = !practice.parayanaComplete()
            practice.setParayanaDay(complete = on, skipped = false)
            paintDay()
        }
        paintDay()
    }
}
