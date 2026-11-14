package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    private var refreshSandhya: (() -> Unit)? = null
    private var sandhyaBound = false
    private var japaBound = false
    private var parayanaBound = false
    private lateinit var practice: PracticeDayStore

    private var kalaId = SandhyaStore.PRATAH
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
            sandhyaSkipped = practice.isSandhyaSkipped(kalaId)
            sandhyaDone = !sandhyaSkipped && practice.isSandhyaSessionDone(kalaId)
            showTab(initialTab)
        }

        val tabs = view.findViewById<TabLayout>(R.id.nityaTabs)
        TABS.forEach { tag ->
            tabs.addTab(tabs.newTab().setText(OsLang.ui(tag, OsLang.get(requireContext()))).setIcon(FrozenShell.iconFor(tag)).setTag(tag), false)
        }
        tabs.tabMode = TabLayout.MODE_SCROLLABLE
        tabs.tabGravity = TabLayout.GRAVITY_START
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

    override fun onResume() {
        super.onResume()
        refreshSandhya?.invoke()
    }

    override fun onDestroyView() {
        refreshSandhya = null
        super.onDestroyView()
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
        // Background store loading must honour the user's latest selected tab.
        initialTab = tab
        ensureTab(tab)
        sandhyaRoot?.visibility = if (tab == TAB_SANDHYA) View.VISIBLE else View.GONE
        japaRoot?.visibility = if (tab == TAB_JAPA) View.VISIBLE else View.GONE
        parayanaRoot?.visibility = if (tab == TAB_NITYA) View.VISIBLE else View.GONE
        // Tabs are inflated hidden. Rebuild child display lists after their first visible layout.
        val selected = when (tab) {
            TAB_JAPA -> japaRoot
            TAB_NITYA -> parayanaRoot
            else -> sandhyaRoot
        }
        selected?.post {
            fun redraw(view: View) {
                if (view is ViewGroup) for (index in 0 until view.childCount) redraw(view.getChildAt(index))
                view.invalidate()
            }
            redraw(selected)
        }
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
        fun paint() {
            sandhyaSkipped = practice.isSandhyaSkipped(kalaId)
            sandhyaDone = practice.isSandhyaSessionDone(kalaId)
            root.findViewById<TextView>(R.id.sandhyaPracticeStatus).text = when {
                sandhyaDone -> "ಪೂರ್ಣ · Completed"
                sandhyaSkipped -> "ಬಿಟ್ಟಿದೆ · Skipped"
                else -> "ಆಚರಣೆ ಬಾಕಿ · Not marked complete"
            }
            root.findViewById<View>(R.id.sandhyaRead).isEnabled =
                SandhyaStore.session(requireContext(), kalaId)?.steps?.isNotEmpty() == true
            paintSandhyaChrome(root)
        }
        refreshSandhya = { paint() }
        fun selectKala(id: String) { kalaId = id; paint() }
        fun confirmStatus(complete: Boolean) {
            val targetKala = kalaId
            val date = com.paperdesk.launcher.data.PanchangaStore.dateKey()
            val marked = if (complete) practice.isSandhyaSessionDone(targetKala, date)
                else practice.isSandhyaSkipped(targetKala, date)
            val label = SandhyaStore.session(requireContext(), targetKala)?.title.orEmpty()
            SandhyaDayConfirmation.show(requireContext(), SandhyaStore.label(label, OsLang.get(requireContext())), complete, marked) {
                practice.setSandhyaDay(targetKala, complete = complete && !marked,
                    skipped = !complete && !marked, date = date)
                if (view != null) paint()
            }
        }
        root.findViewById<View>(R.id.sandhyaKalaPratah).setOnClickListener { selectKala(SandhyaStore.PRATAH) }
        root.findViewById<View>(R.id.sandhyaKalaMadhya).setOnClickListener { selectKala(SandhyaStore.MADHYA) }
        root.findViewById<View>(R.id.sandhyaKalaSayam).setOnClickListener { selectKala(SandhyaStore.SAYAM) }
        root.findViewById<View>(R.id.sandhyaRead).setOnClickListener {
            startActivity(ContentReaderActivity.forSandhya(requireContext(), kalaId, fullSession = true))
        }
        root.findViewById<View>(R.id.sandhyaSkip).setOnClickListener { confirmStatus(false) }
        root.findViewById<View>(R.id.sandhyaComplete).setOnClickListener { confirmStatus(true) }
        paint()
    }

    /** Match mockup `.nitya-fab[aria-pressed=true]` — selected layer + interactive border/glyph. */
    private fun paintSandhyaChrome(root: View) {
        NityaDayChrome.paint(root.findViewById(R.id.sandhyaKalaPratah), kalaId == SandhyaStore.PRATAH)
        NityaDayChrome.paint(root.findViewById(R.id.sandhyaKalaMadhya), kalaId == SandhyaStore.MADHYA)
        NityaDayChrome.paint(root.findViewById(R.id.sandhyaKalaSayam), kalaId == SandhyaStore.SAYAM)
        NityaDayChrome.paint(root.findViewById(R.id.sandhyaSkip), sandhyaSkipped)
        NityaDayChrome.paint(root.findViewById(R.id.sandhyaComplete), sandhyaDone && !sandhyaSkipped)
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
