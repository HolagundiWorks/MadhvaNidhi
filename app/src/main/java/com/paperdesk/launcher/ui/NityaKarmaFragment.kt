package com.paperdesk.launcher.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.OsLang
import com.paperdesk.launcher.data.SandhyaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Tabbed Nitya Karma shell — Sandhya · Japa · Nitya Parayana (Puja is a separate destination). */
class NityaKarmaFragment : Fragment() {
    private var initialTab = TAB_SANDHYA
    private var sandhyaRoot: View? = null
    private var japaRoot: View? = null
    private var parayanaRoot: View? = null
    private var sandhyaBound = false
    private var japaBound = false
    private var parayanaBound = false

    private var kalaId = SandhyaStore.PRATAH
    private var sandhyaIndex = 0
    private var sandhyaDone = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val requested = arguments?.getString(ARG_TAB).orEmpty().ifBlank { TAB_SANDHYA }
        initialTab = when (requested) {
            TAB_JAPA -> TAB_JAPA
            TAB_NITYA -> TAB_NITYA
            else -> TAB_SANDHYA
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.page_nitya_karma, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        PagePalette.apply(view, "nityakarma")
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) { SandhyaStore.load(requireContext().applicationContext) }
            if (!isAdded) return@launch
            kalaId = SandhyaStore.nowId()
            showTab(initialTab)
        }

        val tabs = view.findViewById<TabLayout>(R.id.nityaTabs)
        TABS.forEach { tag ->
            tabs.addTab(tabs.newTab().setText(OsLang.ui(tag, OsLang.get(requireContext()))).setTag(tag), false)
        }
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                showTab(tab.tag as? String ?: TAB_SANDHYA)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        val start = TABS.indexOf(initialTab).coerceAtLeast(0)
        ensureTab(TABS[start])
        tabs.getTabAt(start)?.select()
        showTab(TABS[start])
    }

    fun selectTab(tab: String) {
        if (tab == "puja") {
            (activity as? LauncherActivity)?.navigateTo("puja")
            return
        }
        val tabs = view?.findViewById<TabLayout>(R.id.nityaTabs) ?: return
        val index = TABS.indexOf(tab).coerceAtLeast(0)
        tabs.getTabAt(index)?.select()
    }

    private fun showTab(tab: String) {
        ensureTab(tab)
        sandhyaRoot?.visibility = if (tab == TAB_SANDHYA) View.VISIBLE else View.GONE
        japaRoot?.visibility = if (tab == TAB_JAPA) View.VISIBLE else View.GONE
        parayanaRoot?.visibility = if (tab == TAB_NITYA) View.VISIBLE else View.GONE
    }

    private fun ensureTab(tab: String) {
        val host = view?.findViewById<ViewGroup>(R.id.nityaPanelHost) ?: return
        val inflater = layoutInflater
        val match = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        when (tab) {
            TAB_SANDHYA -> if (sandhyaRoot == null) {
                sandhyaRoot = inflater.inflate(R.layout.page_nitya_sandhya, host, false).also {
                    host.addView(it, match)
                    if (!sandhyaBound) {
                        bindSandhya(it)
                        sandhyaBound = true
                    }
                }
            }
            TAB_JAPA -> if (japaRoot == null) {
                japaRoot = inflater.inflate(R.layout.page_japa, host, false).also {
                    host.addView(it, match)
                    it.visibility = View.GONE
                    if (!japaBound) {
                        JapaPadBinder.bind(this, it)
                        japaBound = true
                    }
                }
            }
            TAB_NITYA -> if (parayanaRoot == null) {
                parayanaRoot = inflater.inflate(R.layout.page_nitya_parayana, host, false).also {
                    host.addView(it, match)
                    it.visibility = View.GONE
                    if (!parayanaBound) {
                        NityaParayanaBinder.bind(this, it)
                        parayanaBound = true
                    }
                }
            }
        }
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
                listOf(
                    OsLang.ui("nityakarma", OsLang.get(requireContext())),
                    OsLang.ui("sandhya", OsLang.get(requireContext())),
                    kala,
                    step.title
                ).filter { it.isNotBlank() }.joinToString(" · ")
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

    companion object {
        const val TAB_SANDHYA = "sandhya"
        const val TAB_JAPA = "japa"
        const val TAB_NITYA = "nitya"
        private val TABS = listOf(TAB_SANDHYA, TAB_JAPA, TAB_NITYA)
        private const val ARG_TAB = "tab"

        fun create(tab: String = TAB_SANDHYA) = NityaKarmaFragment().apply {
            arguments = Bundle().apply { putString(ARG_TAB, tab) }
        }
    }
}
