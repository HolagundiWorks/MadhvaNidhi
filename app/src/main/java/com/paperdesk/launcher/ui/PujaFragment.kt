package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.IndicScript
import com.paperdesk.launcher.data.MadhwaItem
import com.paperdesk.launcher.data.MadhwaStore
import com.paperdesk.launcher.data.OsLang
import com.paperdesk.launcher.data.Sankalpa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Standalone Puja destination — rite select + stepped body from Madhwa catalogue. */
class PujaFragment : Fragment() {
    private var pujaRite = 0
    private var pujaIndex = 0
    private var pujaDone = false
    private var rites: List<MadhwaItem> = emptyList()
    private var steps: List<Pair<String, String>> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.page_nitya_puja, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        PagePalette.apply(view, "puja")
        viewLifecycleOwner.lifecycleScope.launch {
            val app = requireContext().applicationContext
            val loaded = withContext(Dispatchers.IO) {
                MadhwaStore.load(app)
                preferredRites()
            }
            if (!isAdded) return@launch
            rites = loaded
            bindPuja(view)
        }
    }

    private fun preferredRites(): List<MadhwaItem> {
        val preferred = listOf(
            "madhwa_deva_pooja_guide",
            "puja_udupi",
            "festival_ganesha_chaturthi",
            "puja_anushtana"
        )
        val byId = preferred.mapNotNull { MadhwaStore.item(it) }
        if (byId.size >= 3) return byId.take(3)
        val catalog = MadhwaStore.inCategory("puja") + MadhwaStore.inCategory("festival")
        return (byId + catalog.filter { c -> byId.none { it.id == c.id } }).take(3)
    }

    private fun bindPuja(root: View) {
        if (rites.isEmpty()) {
            root.findViewById<TextView>(R.id.pujaBody).text = "—"
            return
        }
        val labels = rites.map { MadhwaStore.label(it, OsLang.get(requireContext())) }
        val riteView = root.findViewById<MaterialAutoCompleteTextView>(R.id.pujaRite)
        MaterialChoices.bind(riteView, labels, 0) {
            pujaRite = it
            pujaIndex = 0
            pujaDone = false
            rebuildSteps()
            paintPuja(root)
        }
        rebuildSteps()
        root.findViewById<View>(R.id.pujaPrev).setOnClickListener {
            if (pujaIndex > 0) {
                pujaIndex--
                pujaDone = false
                paintPuja(root)
            }
        }
        root.findViewById<View>(R.id.pujaNext).setOnClickListener {
            if (pujaIndex < steps.lastIndex) {
                pujaIndex++
                paintPuja(root)
            }
        }
        root.findViewById<View>(R.id.pujaSkip).setOnClickListener {
            pujaDone = false
            pujaIndex = 0
            paintPuja(root)
        }
        root.findViewById<View>(R.id.pujaComplete).setOnClickListener {
            pujaDone = true
            pujaIndex = steps.lastIndex.coerceAtLeast(0)
            paintPuja(root)
        }
        paintPuja(root)
    }

    private fun rebuildSteps() {
        val item = rites.getOrNull(pujaRite) ?: return
        val lang = OsLang.get(requireContext())
        val body = Sankalpa.injectPuja(
            requireContext(),
            MadhwaStore.body(requireContext(), item, lang),
            lang
        )
        val chunks = body.split(Regex("\n{2,}")).map { it.trim() }.filter { it.isNotBlank() }
        val sankalpa = Sankalpa.pujaBlock(requireContext(), lang)
        val mid = chunks.drop(1).dropLast(1)
        val half = (mid.size / 2).coerceAtLeast(0)
        val samarpana = chunks.lastOrNull().orEmpty().ifBlank {
            IndicScript.convert(
                "कायेन वाचा मनसेन्द्रियैर्वा बुद्ध्यात्मना वा प्रकृतेः स्वभावात्।\n" +
                    "करोमि यद्यत् सकलं परस्मै नारायणायेति समर्पयामि॥\n\n" +
                    "भारतीरमण मुख्यप्राणान्तर्गत श्रीलक्ष्मीनारायण प्रीत्यर्थं कृतमिदं पूजाकर्म श्रीकृष्णार्पणमस्तु।",
                lang
            )
        }
        steps = listOf(
            "ಸಂಕಲ್ಪ" to sankalpa,
            "ಆವಾಹನ" to (chunks.firstOrNull() ?: body),
            "ಉಪಚಾರ" to mid.drop(half).ifEmpty { mid }.joinToString("\n\n").ifBlank { body },
            "ಸಮರ್ಪಣ" to samarpana
        )
    }

    private fun paintPuja(root: View) {
        if (steps.isEmpty()) return
        pujaIndex = pujaIndex.coerceIn(0, steps.lastIndex)
        val step = steps[pujaIndex]
        val pct = if (pujaDone) 100 else ((pujaIndex + 1) * 100 / steps.size)
        val riteLabel = rites.getOrNull(pujaRite)?.let {
            MadhwaStore.label(it, OsLang.get(requireContext()))
        }.orEmpty()
        root.findViewById<TextView>(R.id.pujaStep).text = step.first
        root.findViewById<TextView>(R.id.pujaBody).text = step.second
        root.findViewById<TextView>(R.id.pujaCrumb).text = "ಪೂಜೆ · $riteLabel · ${step.first}"
        root.findViewById<ProgressBar>(R.id.pujaProgress).progress = pct
        root.findViewById<View>(R.id.pujaPrev).isEnabled = pujaIndex > 0
        root.findViewById<View>(R.id.pujaNext).isEnabled = !pujaDone && pujaIndex < steps.lastIndex
    }
}
