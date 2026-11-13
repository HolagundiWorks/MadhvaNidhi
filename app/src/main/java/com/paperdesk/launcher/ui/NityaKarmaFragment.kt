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
import com.paperdesk.launcher.data.PracticeDayStore
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
    private lateinit var practice: PracticeDayStore

    private var kalaId = SandhyaStore.PRATAH
    private var sandhyaIndex = 0
    private var sandhyaDone = false
    private var sandhyaSkipped = false

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
        practice = PracticeDayStore(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) { SandhyaStore.load(requireContext().applicationContext) }
            if (!isAdded) return@launch
            kalaId = SandhyaStore.nowId()
            sandhyaDone = practice.isSandhyaSessionDone(kalaId)
            showTab(initialTab)
        }

        val tabs = view.findViewById<TabLayout>(R.id.nityaTabs)
        TABS.forEach { tag ->
            tabs.addTab(tabs.newTab().setText(OsLang.ui(tag, OsLang.get(requireContext()))).setTag(tag), false)
        }
        CarbonTabs.equalizeLeft(tabs)
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
        // Carbon sm (32) FABs — five controls + pathya must fit landscape phone chrome.
        val fab = resources.getDimensionPixelSize(R.dimen.carbon_layout_size_height_sm)
        listOf(
            R.id.sandhyaKalaPratah, R.id.sandhyaKalaMadhya, R.id.sandhyaKalaSayam,
            R.id.sandhyaSkip, R.id.sandhyaComplete
        ).forEach { id ->
            root.findViewById<MaterialButton>(id).apply {
                minimumWidth = 0
                minimumHeight = 0
                iconSize = (16 * resources.displayMetrics.density).toInt().coerceAtMost(fab - 8)
                layoutParams = layoutParams.apply {
                    width = fab
                    height = fab
                }
            }
        }
        root.findViewById<TextView>(R.id.sandhyaPathy).minimumWidth = 0
        fun steps() = SandhyaStore.session(requireContext(), kalaId)?.steps.orEmpty()
        fun lang() = OsLang.get(requireContext())
        fun paint() {
            val list = steps()
            if (list.isEmpty()) {
                root.findViewById<TextView>(R.id.sandhyaPathy).text = "—"
                root.findViewById<TextView>(R.id.sandhyaBody).text = ""
                root.findViewById<ProgressBar>(R.id.sandhyaProgress).progress = 0
                paintSandhyaChrome(root)
                return
            }
            if (!sandhyaSkipped) {
                sandhyaDone = practice.isSandhyaSessionDone(kalaId)
            }
            sandhyaIndex = sandhyaIndex.coerceIn(0, list.lastIndex)
            if (sandhyaDone) sandhyaIndex = list.lastIndex
            val step = list[sandhyaIndex]
            val pct = if (sandhyaDone) 100 else ((sandhyaIndex + 1) * 100 / list.size)
            root.findViewById<TextView>(R.id.sandhyaPathy).text =
                SandhyaStore.label(step.title, lang())
            root.findViewById<TextView>(R.id.sandhyaBody).text =
                SandhyaStore.body(requireContext(), step, kalaId, lang())
            val kala = SandhyaStore.session(requireContext(), kalaId)?.title.orEmpty()
            root.findViewById<TextView>(R.id.sandhyaCrumb).text =
                listOf(
                    OsLang.ui("nityakarma", lang()),
                    OsLang.ui("sandhya", lang()),
                    SandhyaStore.label(kala, lang()),
                    SandhyaStore.label(step.title, lang())
                ).filter { it.isNotBlank() }.joinToString(" · ")
            root.findViewById<ProgressBar>(R.id.sandhyaProgress).progress = pct
            paintSandhyaChrome(root)
        }
        fun selectKala(id: String) {
            kalaId = id
            sandhyaIndex = 0
            sandhyaSkipped = false
            sandhyaDone = practice.isSandhyaSessionDone(kalaId)
            val list = steps()
            if (sandhyaDone && list.isNotEmpty()) sandhyaIndex = list.lastIndex
            paint()
        }
        fun goPrev() {
            if (sandhyaIndex > 0) {
                sandhyaIndex--
                sandhyaSkipped = false
                if (practice.isSandhyaSessionDone(kalaId)) {
                    practice.setSandhya(kalaId, false)
                    sandhyaDone = false
                }
                paint()
            }
        }
        fun goNext() {
            val list = steps()
            if (list.isEmpty()) return
            if (sandhyaIndex < list.lastIndex) {
                sandhyaIndex++
                sandhyaSkipped = false
                if (sandhyaIndex == list.lastIndex) {
                    practice.setSandhya(kalaId, true)
                    sandhyaDone = true
                }
                paint()
            }
        }
        root.findViewById<View>(R.id.sandhyaKalaPratah).setOnClickListener { selectKala(SandhyaStore.PRATAH) }
        root.findViewById<View>(R.id.sandhyaKalaMadhya).setOnClickListener { selectKala(SandhyaStore.MADHYA) }
        root.findViewById<View>(R.id.sandhyaKalaSayam).setOnClickListener { selectKala(SandhyaStore.SAYAM) }
        root.findViewById<View>(R.id.sandhyaProgress).setOnClickListener { goNext() }
        root.findViewById<View>(R.id.sandhyaProgress).setOnLongClickListener {
            goPrev()
            true
        }
        root.findViewById<View>(R.id.sandhyaSkip).setOnClickListener {
            // Toggle like mockup aria-pressed
            if (sandhyaSkipped) {
                sandhyaSkipped = false
            } else {
                practice.setSandhya(kalaId, false)
                sandhyaDone = false
                sandhyaSkipped = true
                sandhyaIndex = 0
            }
            paint()
        }
        root.findViewById<View>(R.id.sandhyaComplete).setOnClickListener {
            if (sandhyaDone && !sandhyaSkipped) {
                practice.setSandhya(kalaId, false)
                sandhyaDone = false
            } else {
                val list = steps()
                practice.setSandhya(kalaId, true)
                sandhyaDone = true
                sandhyaSkipped = false
                sandhyaIndex = list.lastIndex.coerceAtLeast(0)
            }
            paint()
        }
        paint()
    }

    /** Match mockup `.nitya-fab[aria-pressed=true]` — selected layer + interactive border/glyph. */
    private fun paintSandhyaChrome(root: View) {
        fun style(btn: MaterialButton, active: Boolean) {
            val ctx = requireContext()
            btn.backgroundTintList = ColorStateList.valueOf(
                ctx.getColor(if (active) R.color.carbon_layer_selected else android.R.color.transparent)
            )
            btn.iconTint = ColorStateList.valueOf(
                ctx.getColor(if (active) R.color.carbon_interactive else R.color.carbon_icon_secondary)
            )
            btn.strokeColor = ColorStateList.valueOf(
                ctx.getColor(if (active) R.color.carbon_border_interactive else R.color.carbon_border_strong)
            )
            btn.strokeWidth = resources.getDimensionPixelSize(R.dimen.carbon_border_width).coerceAtLeast(1)
        }
        style(root.findViewById(R.id.sandhyaKalaPratah), kalaId == SandhyaStore.PRATAH)
        style(root.findViewById(R.id.sandhyaKalaMadhya), kalaId == SandhyaStore.MADHYA)
        style(root.findViewById(R.id.sandhyaKalaSayam), kalaId == SandhyaStore.SAYAM)
        style(root.findViewById(R.id.sandhyaSkip), sandhyaSkipped)
        style(root.findViewById(R.id.sandhyaComplete), sandhyaDone && !sandhyaSkipped)
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
