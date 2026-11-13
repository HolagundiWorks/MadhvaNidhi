package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.paperdesk.launcher.R

/** Standalone Puja destination (moved out of Nitya Karma tabs). */
class PujaFragment : Fragment() {
    private var pujaRite = 0
    private var pujaIndex = 0
    private var pujaDone = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.page_nitya_puja, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        PagePalette.apply(view, "puja")
        bindPuja(view)
    }

    private fun bindPuja(root: View) {
        val rites = listOf("ನಿತ್ಯ ಪೂಜೆ", "ವಿಷ್ಣು ಪೂಜೆ", "ಗಣಪತಿ ಪೂಜೆ")
        val steps = listOf(
            "ಸಂಕಲ್ಪ" to "ಶ್ರೀಮದ್ ಭಾರತವರ್ಷೇ ಭರತಖಂಡೇ …\n\nSample sankalpa. Masa / paksha / tithi bind from Panchanga later.",
            "ಆವಾಹನ" to "ಆವಾಹನ sample.\nInvite the chosen murti for the selected ಪೂಜಾ ಕ್ರಮ.",
            "ಉಪಚಾರ" to "ಷೋಡಶೋಪಚಾರ sample block.\nOffer the sequential upacharas for the active rite.",
            "ಸಮರ್ಪಣ" to "ಸಮರ್ಪಣ / ಉದ್ವಾಸನ sample.\nClose the puja with dedication and farewell."
        )
        val riteView = root.findViewById<MaterialAutoCompleteTextView>(R.id.pujaRite)
        MaterialChoices.bind(riteView, rites, 0) {
            pujaRite = it
            pujaIndex = 0
            pujaDone = false
            paintPuja(root, rites, steps)
        }
        fun paint() = paintPuja(root, rites, steps)
        root.findViewById<View>(R.id.pujaPrev).setOnClickListener {
            if (pujaIndex > 0) {
                pujaIndex--
                pujaDone = false
                paint()
            }
        }
        root.findViewById<View>(R.id.pujaNext).setOnClickListener {
            if (pujaIndex < steps.lastIndex) {
                pujaIndex++
                paint()
            }
        }
        root.findViewById<View>(R.id.pujaSkip).setOnClickListener {
            pujaDone = false
            pujaIndex = 0
            paint()
        }
        root.findViewById<View>(R.id.pujaComplete).setOnClickListener {
            pujaDone = true
            pujaIndex = steps.lastIndex
            paint()
        }
        paint()
    }

    private fun paintPuja(root: View, rites: List<String>, steps: List<Pair<String, String>>) {
        pujaIndex = pujaIndex.coerceIn(0, steps.lastIndex)
        val step = steps[pujaIndex]
        val pct = if (pujaDone) 100 else ((pujaIndex + 1) * 100 / steps.size)
        root.findViewById<TextView>(R.id.pujaStep).text = step.first
        root.findViewById<TextView>(R.id.pujaBody).text = step.second
        root.findViewById<TextView>(R.id.pujaCrumb).text =
            "ಪೂಜೆ · ${rites.getOrElse(pujaRite) { rites.first() }} · ${step.first}"
        root.findViewById<ProgressBar>(R.id.pujaProgress).progress = pct
        root.findViewById<View>(R.id.pujaPrev).isEnabled = pujaIndex > 0
        root.findViewById<View>(R.id.pujaNext).isEnabled = !pujaDone && pujaIndex < steps.lastIndex
    }
}
