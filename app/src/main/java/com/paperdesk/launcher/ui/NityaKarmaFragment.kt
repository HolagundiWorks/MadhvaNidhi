package com.paperdesk.launcher.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.OsLang
import com.paperdesk.launcher.data.SandhyaStore

/** Tabbed Nitya Karma shell — Sandhya · Japa · Puja (frozen mockup contract). */
class NityaKarmaFragment : Fragment() {
    private var initialTab = TAB_SANDHYA
    private var sandhyaRoot: View? = null
    private var japaRoot: View? = null
    private var pujaRoot: View? = null

    private var kalaId = SandhyaStore.PRATAH
    private var sandhyaIndex = 0
    private var sandhyaDone = false

    private var pujaRite = 0
    private var pujaIndex = 0
    private var pujaDone = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialTab = arguments?.getString(ARG_TAB).orEmpty().ifBlank { TAB_SANDHYA }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.page_nitya_karma, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        PagePalette.apply(view, "nityakarma")
        SandhyaStore.load(requireContext())
        kalaId = SandhyaStore.nowId()

        val host = view.findViewById<ViewGroup>(R.id.nityaPanelHost)
        val inflater = layoutInflater
        val match = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        sandhyaRoot = inflater.inflate(R.layout.page_nitya_sandhya, host, false).also { host.addView(it, match) }
        japaRoot = inflater.inflate(R.layout.page_japa, host, false).also {
            host.addView(it, match)
            it.visibility = View.GONE
        }
        pujaRoot = inflater.inflate(R.layout.page_nitya_puja, host, false).also {
            host.addView(it, match)
            it.visibility = View.GONE
        }

        bindSandhya(sandhyaRoot!!)
        bindPuja(pujaRoot!!)
        JapaPadBinder.bind(this, japaRoot!!)

        val tabs = view.findViewById<TabLayout>(R.id.nityaTabs)
        listOf(
            OsLang.ui("sandhya", OsLang.get(requireContext())) to TAB_SANDHYA,
            OsLang.ui("japa", OsLang.get(requireContext())) to TAB_JAPA,
            OsLang.ui("puja", OsLang.get(requireContext())) to TAB_PUJA
        ).forEach { (label, tag) ->
            tabs.addTab(tabs.newTab().setText(label).setTag(tag), false)
        }
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                showTab(tab.tag as? String ?: TAB_SANDHYA)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        val start = when (initialTab) {
            TAB_JAPA -> 1
            TAB_PUJA -> 2
            else -> 0
        }
        tabs.getTabAt(start)?.select()
        showTab(listOf(TAB_SANDHYA, TAB_JAPA, TAB_PUJA)[start])
    }

    fun selectTab(tab: String) {
        val tabs = view?.findViewById<TabLayout>(R.id.nityaTabs) ?: return
        val index = when (tab) {
            TAB_JAPA -> 1
            TAB_PUJA -> 2
            else -> 0
        }
        tabs.getTabAt(index)?.select()
    }

    private fun showTab(tab: String) {
        sandhyaRoot?.visibility = if (tab == TAB_SANDHYA) View.VISIBLE else View.GONE
        japaRoot?.visibility = if (tab == TAB_JAPA) View.VISIBLE else View.GONE
        pujaRoot?.visibility = if (tab == TAB_PUJA) View.VISIBLE else View.GONE
    }

    private fun bindSandhya(root: View) {
        fun steps() = SandhyaStore.session(requireContext(), kalaId)?.steps.orEmpty()
        fun paint() {
            val list = steps()
            if (list.isEmpty()) {
                root.findViewById<TextView>(R.id.sandhyaPathy).text = "—"
                root.findViewById<TextView>(R.id.sandhyaBody).text = ""
                root.findViewById<ProgressBar>(R.id.sandhyaProgress).progress = 0
                return
            }
            sandhyaIndex = sandhyaIndex.coerceIn(0, list.lastIndex)
            val step = list[sandhyaIndex]
            val pct = if (sandhyaDone) 100 else ((sandhyaIndex + 1) * 100 / list.size)
            root.findViewById<TextView>(R.id.sandhyaPathy).text = step.title
            root.findViewById<TextView>(R.id.sandhyaBody).text = step.body
            val kala = SandhyaStore.session(requireContext(), kalaId)?.title.orEmpty()
            root.findViewById<TextView>(R.id.sandhyaCrumb).text =
                listOf(OsLang.ui("nityakarma", OsLang.get(requireContext())), OsLang.ui("sandhya", OsLang.get(requireContext())), kala, step.title)
                    .filter { it.isNotBlank() }.joinToString(" · ")
            root.findViewById<ProgressBar>(R.id.sandhyaProgress).progress = pct
            root.findViewById<View>(R.id.sandhyaPrev).isEnabled = sandhyaIndex > 0
            root.findViewById<View>(R.id.sandhyaNext).isEnabled = !sandhyaDone && sandhyaIndex < list.lastIndex
            paintKala(root)
        }
        root.findViewById<View>(R.id.sandhyaKalaPratah).setOnClickListener {
            kalaId = SandhyaStore.PRATAH; sandhyaIndex = 0; sandhyaDone = false; paint()
        }
        root.findViewById<View>(R.id.sandhyaKalaMadhya).setOnClickListener {
            kalaId = SandhyaStore.MADHYA; sandhyaIndex = 0; sandhyaDone = false; paint()
        }
        root.findViewById<View>(R.id.sandhyaKalaSayam).setOnClickListener {
            kalaId = SandhyaStore.SAYAM; sandhyaIndex = 0; sandhyaDone = false; paint()
        }
        root.findViewById<View>(R.id.sandhyaPrev).setOnClickListener {
            if (sandhyaIndex > 0) { sandhyaIndex--; sandhyaDone = false; paint() }
        }
        root.findViewById<View>(R.id.sandhyaNext).setOnClickListener {
            val list = steps()
            if (sandhyaIndex < list.lastIndex) { sandhyaIndex++; paint() }
        }
        root.findViewById<View>(R.id.sandhyaSkip).setOnClickListener {
            sandhyaDone = false; sandhyaIndex = 0; paint()
        }
        root.findViewById<View>(R.id.sandhyaComplete).setOnClickListener {
            val list = steps()
            sandhyaDone = true
            sandhyaIndex = list.lastIndex.coerceAtLeast(0)
            paint()
        }
        paint()
    }

    private fun paintKala(root: View) {
        val density = resources.displayMetrics.density
        fun style(btn: MaterialButton, active: Boolean) {
            val color = requireContext().getColor(if (active) R.color.carbon_interactive else R.color.carbon_icon_secondary)
            btn.iconTint = ColorStateList.valueOf(color)
            btn.strokeColor = ColorStateList.valueOf(
                requireContext().getColor(if (active) R.color.carbon_interactive else R.color.carbon_border)
            )
            btn.strokeWidth = (1 * density).toInt().coerceAtLeast(1)
        }
        style(root.findViewById(R.id.sandhyaKalaPratah), kalaId == SandhyaStore.PRATAH)
        style(root.findViewById(R.id.sandhyaKalaMadhya), kalaId == SandhyaStore.MADHYA)
        style(root.findViewById(R.id.sandhyaKalaSayam), kalaId == SandhyaStore.SAYAM)
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
            if (pujaIndex > 0) { pujaIndex--; pujaDone = false; paint() }
        }
        root.findViewById<View>(R.id.pujaNext).setOnClickListener {
            if (pujaIndex < steps.lastIndex) { pujaIndex++; paint() }
        }
        root.findViewById<View>(R.id.pujaSkip).setOnClickListener {
            pujaDone = false; pujaIndex = 0; paint()
        }
        root.findViewById<View>(R.id.pujaComplete).setOnClickListener {
            pujaDone = true; pujaIndex = steps.lastIndex; paint()
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
            "ನಿತ್ಯಕರ್ಮ · ಪೂಜೆ · ${rites.getOrElse(pujaRite) { rites.first() }} · ${step.first}"
        root.findViewById<ProgressBar>(R.id.pujaProgress).progress = pct
        root.findViewById<View>(R.id.pujaPrev).isEnabled = pujaIndex > 0
        root.findViewById<View>(R.id.pujaNext).isEnabled = !pujaDone && pujaIndex < steps.lastIndex
    }

    companion object {
        const val TAB_SANDHYA = "sandhya"
        const val TAB_JAPA = "japa"
        const val TAB_PUJA = "puja"
        private const val ARG_TAB = "tab"

        fun create(tab: String = TAB_SANDHYA) = NityaKarmaFragment().apply {
            arguments = Bundle().apply { putString(ARG_TAB, tab) }
        }
    }
}
