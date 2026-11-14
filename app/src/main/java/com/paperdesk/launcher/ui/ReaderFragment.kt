package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.CatalogueStore
import com.paperdesk.launcher.data.IndicScript
import com.paperdesk.launcher.data.MadhwaStore
import com.paperdesk.launcher.data.OsLang
import com.paperdesk.launcher.data.SandhyaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Portrait selection screen for catalogue items (stotra/suktas/mantras/namavali/hadugalu), launching landscape ContentReaderActivity. */
class ReaderFragment : Fragment() {

    private var pageId = SANDHYA
    private var lang = IndicScript.Lang.KN
    private var sessionId = ""
    private var itemId = ""
    private var subpageId = MANTRA_SUBPAGE
    private lateinit var adapter: SelectAdapter
    private var paintJob: Job? = null
    private val langTick: () -> Unit = { paint() }

    private val prefs by lazy {
        requireContext().getSharedPreferences("paperdesk_reader_$pageId", 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageId = arguments?.getString(ARG_ID).orEmpty().ifBlank { SANDHYA }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.page_reader, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        PagePalette.apply(view, pageId)
        lang = OsLang.get(requireContext())
        adapter = SelectAdapter { pick(it) }
        view.findViewById<RecyclerView>(R.id.selectorList).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ReaderFragment.adapter
        }
        view.findViewById<View>(R.id.readerSelect).setOnClickListener { openSelector() }
        view.findViewById<View>(R.id.readerSelectBtn).setOnClickListener { openSelector() }
        view.findViewById<View>(R.id.readerSelectLabel).setOnClickListener { openSelector() }
        view.findViewById<View>(R.id.selectorBack).setOnClickListener { closeSelector() }
        if (pageId == SANDHYA) setupKalaDropdown(view)
        sessionId = prefs.getString("session", "").orEmpty()
        itemId = prefs.getString("item", "").orEmpty()
        subpageId = lockedSubpage() ?: MANTRA_SUBPAGE
        viewLifecycleOwner.lifecycleScope.launch {
            val app = requireContext().applicationContext
            withContext(Dispatchers.IO) { ensureStoresLoaded(app) }
            if (!isAdded) return@launch
            resolveSelection()
            paint()
        }
    }

    private fun ensureStoresLoaded(app: android.content.Context) {
        if (pageId == SANDHYA) SandhyaStore.load(app)
        if (isCataloguePage()) CatalogueStore.load(app)
        if (pageId == PUJA) MadhwaStore.load(app)
    }

    private fun resolveSelection() {
        if (pageId == SANDHYA) {
            if (sessionId.isBlank() || SandhyaStore.session(requireContext(), sessionId) == null) {
                sessionId = SandhyaStore.nowId()
            }
            val steps = SandhyaStore.session(requireContext(), sessionId)?.steps.orEmpty()
            if (steps.none { it.id == itemId }) itemId = steps.firstOrNull()?.id.orEmpty()
        }
        if (isCataloguePage() && !catalogueItemAllowed(CatalogueStore.item(itemId))) {
            itemId = if (usesSubpageFilter()) CatalogueStore.firstReadable(pageId, subpageId)?.id.orEmpty()
            else CatalogueStore.firstReadable(pageId)?.id.orEmpty()
            sessionId = CatalogueStore.item(itemId)?.group.orEmpty()
        }
        if (pageId == PUJA) {
            val list = pujaItems()
            if (list.none { it.id == itemId }) itemId = list.firstOrNull()?.id.orEmpty()
            sessionId = "puja"
        }
    }

    override fun onResume() {
        super.onResume()
        OsLang.addListener(langTick)
        paint()
    }

    override fun onPause() {
        view?.let { MaterialChoices.dismiss(it) }
        OsLang.removeListener(langTick)
        super.onPause()
    }

    fun closeSelector(): Boolean {
        val dropdown = view?.findViewById<MaterialAutoCompleteTextView>(R.id.readerItem)
        if (dropdown?.isPopupShowing == true) {
            dropdown.dismissDropDown()
            return true
        }
        return false
    }

    fun openSelector() {
        if (pageId == SANDHYA) return
        view?.findViewById<MaterialAutoCompleteTextView>(R.id.readerItem)?.showDropDown()
    }

    private fun pick(row: Row) {
        if (row.section && pageId == SANDHYA) {
            pickKala(row.group)
            return
        }
        if (row.section) return
        sessionId = row.group
        itemId = row.id
        prefs.edit().putString("session", sessionId).putString("item", itemId).apply()
        paint()
        openContent()
    }

    private fun pickKala(id: String) {
        if (pageId != SANDHYA) return
        sessionId = id
        val next = SandhyaStore.session(requireContext(), id)
        itemId = next?.steps?.firstOrNull()?.id.orEmpty()
        prefs.edit().putString("session", sessionId).putString("item", itemId).apply()
        paint()
        view?.findViewById<NestedScrollView>(R.id.readerScroll)?.scrollTo(0, 0)
    }

    private fun openContent() {
        val ctx = requireContext()
        val intent = when {
            pageId == SANDHYA -> ContentReaderActivity.forSandhya(
                ctx,
                sessionId.ifBlank { SandhyaStore.nowId() },
                fullSession = true
            )
            pageId == PUJA -> ContentReaderActivity.forPuja(ctx, itemId)
            isCataloguePage() && itemId.isNotBlank() -> ContentReaderActivity.forCatalogue(
                ctx,
                pageId,
                itemId,
                lockedSubpage()
            )
            else -> return
        }
        startActivity(intent)
    }

    private fun paint() {
        val v = view ?: return
        lang = OsLang.get(requireContext())
        v.findViewById<TextView>(R.id.readerTitle).text = OsLang.ui(pageId, lang)
        v.findViewById<View>(R.id.readerTitle).visibility = View.GONE
        v.findViewById<TextView>(R.id.selectorTitle).text = OsLang.ui("select_$pageId", lang)
        DeskType.apply(v.findViewById(R.id.readerTitle), DeskType.SEMIBOLD)
        DeskType.apply(v.findViewById(R.id.selectorTitle), DeskType.SEMIBOLD)

        val sandhya = pageId == SANDHYA
        v.findViewById<View>(R.id.readerSubpages).visibility = View.GONE
        v.findViewById<View>(R.id.readerSelect).visibility = View.GONE
        v.findViewById<View>(R.id.readerItemField).visibility = if (sandhya) View.GONE else View.VISIBLE
        if (!sandhya) bindItemDropdown(v)
        v.findViewById<View>(R.id.readerKalaField).visibility = if (sandhya) View.VISIBLE else View.GONE
        if (sandhya) bindKala(v)

        v.findViewById<View>(R.id.readerActionBar).visibility = View.GONE

        val token = "${pageId}|$sessionId|$itemId|$subpageId|${lang.name}"
        paintJob?.cancel()
        paintJob = viewLifecycleOwner.lifecycleScope.launch {
            val app = requireContext().applicationContext
            val emptyLabel = OsLang.ui("empty_page", lang)
            val result = withContext(Dispatchers.IO) {
                ensureStoresLoaded(app)
                val shown = currentSummary(app)
                Triple(shown, breadcrumb(app, shown), rows())
            }
            if (!isAdded || view == null) return@launch
            if (token != "${pageId}|$sessionId|$itemId|$subpageId|${lang.name}") return@launch
            val (item, crumbText, selectorRows) = result

            val selectedTitleView = v.findViewById<TextView>(R.id.readerSelectedTitle)
            selectedTitleView.text = item?.label ?: emptyLabel
            DeskType.apply(selectedTitleView, DeskType.SEMIBOLD)

            val crumb = v.findViewById<TextView>(R.id.readerCrumb)
            crumb.visibility = View.VISIBLE
            crumb.text = crumbText
            crumb.contentDescription = crumbText
            DeskType.apply(crumb, DeskType.REGULAR)

            val list = v.findViewById<RecyclerView>(R.id.selectorList)
            val emptyView = v.findViewById<TextView>(R.id.selectorEmpty)
            val scrollPreview = v.findViewById<View>(R.id.readerScroll)

            if (sandhya) {
                list.visibility = View.GONE
                emptyView.visibility = View.GONE
                scrollPreview.visibility = View.GONE
                v.findViewById<TextView>(R.id.readerHead).apply {
                    text = item?.head.orEmpty()
                    setTextColor(PagePalette.accent(requireContext(), pageId))
                    DeskType.apply(this, DeskType.SEMIBOLD)
                }
                v.findViewById<TextView>(R.id.readerBody).apply {
                    text = ""
                    DeskType.reading(this)
                }
            } else {
                scrollPreview.visibility = View.GONE
                adapter.lang = lang
                adapter.selected = itemId
                adapter.submit(selectorRows)
                val empty = selectorRows.isEmpty()
                emptyView.visibility = if (empty) View.VISIBLE else View.GONE
                emptyView.text = emptyLabel
                list.visibility = if (empty) View.GONE else View.VISIBLE
                if (!empty) keepSelectedRowVisible(list, selectorRows.indexOfFirst { it.id == itemId })
            }
        }
    }

    private fun breadcrumb(app: android.content.Context, item: Shown?): String {
        val page = OsLang.ui(pageId, lang)
        val parts = ArrayList<String>(3)
        parts += page
        when (pageId) {
            SANDHYA -> {
                parts += SandhyaStore.vedaLabel(app)
                val kala = SandhyaStore.session(app, sessionId)?.title.orEmpty()
                if (kala.isNotBlank()) parts += SandhyaStore.label(kala, lang)
            }
            MANTRAS, HADUGALU, STOTRA, SUKTAS, NAMAVALI -> {
                val cat = CatalogueStore.item(itemId)
                val group = CatalogueStore.groupsFor(pageId).firstOrNull { it.id == (cat?.group ?: sessionId) }
                val section = group?.let { CatalogueStore.groupLabel(it, lang) }.orEmpty()
                val name = item?.label.orEmpty()
                if (section.isNotBlank() && section != page) parts += section
                if (name.isNotBlank() && name != section) parts += name
            }
            PUJA -> {
                val name = item?.label.orEmpty()
                if (name.isNotBlank() && name != page) parts += name
            }
        }
        return parts.distinct().joinToString("  ›  ")
    }

    private fun bindItemDropdown(v: View) {
        val entries = rows().filter { !it.section }
        if (entries.isEmpty()) return
        MaterialChoices.bind(
            v.findViewById(R.id.readerItem),
            entries.map { it.title },
            entries.indexOfFirst { it.id == itemId }.coerceAtLeast(0)
        ) { pick(entries[it]) }
    }

    private fun setupKalaDropdown(v: View) {
        listOf(
            R.id.kalaMorning to SandhyaStore.PRATAH,
            R.id.kalaNoon to SandhyaStore.MADHYA,
            R.id.kalaEvening to SandhyaStore.SAYAM
        ).forEach { (id, session) ->
            v.findViewById<View>(id).setOnClickListener { if (session != sessionId) pickKala(session) }
        }
    }

    private fun bindKala(v: View) {
        val id = when (sessionId) {
            SandhyaStore.MADHYA -> R.id.kalaNoon
            SandhyaStore.SAYAM -> R.id.kalaEvening
            else -> R.id.kalaMorning
        }
        v.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.readerKalaField).check(id)
    }

    private fun keepSelectedRowVisible(list: RecyclerView, position: Int) {
        if (position < 0) return
        list.post {
            val manager = list.layoutManager as? LinearLayoutManager ?: return@post
            val first = manager.findFirstCompletelyVisibleItemPosition()
            val last = manager.findLastCompletelyVisibleItemPosition()
            if (position < first || position > last) {
                manager.scrollToPositionWithOffset(position, 0)
            }
        }
    }

    private fun currentSummary(app: android.content.Context): Shown? {
        when (pageId) {
            SANDHYA -> {
                val session = SandhyaStore.session(app, sessionId)
                    ?: SandhyaStore.session(app, SandhyaStore.nowId()) ?: return null
                sessionId = session.id
                itemId = session.steps.firstOrNull()?.id.orEmpty()
                val label = SandhyaStore.label(session.title, lang)
                val stepPreview = session.steps.take(6).joinToString("\n") { step ->
                    "${OsLang.digits(step.n, lang)}. ${SandhyaStore.label(step.title, lang)}"
                }
                return Shown(head = label, body = stepPreview, label = label)
            }
            MANTRAS, HADUGALU, STOTRA, SUKTAS, NAMAVALI -> {
                val groups = if (usesSubpageFilter()) CatalogueStore.groupsForSubpage(pageId, subpageId)
                else CatalogueStore.groupsFor(pageId)
                val allowed = groups.map { it.id }.toSet()
                val item = CatalogueStore.item(itemId)?.takeIf {
                    it.group in allowed && (!usesSubpageFilter() || CatalogueStore.belongsToSubpage(it, subpageId))
                } ?: if (usesSubpageFilter()) CatalogueStore.firstReadable(pageId, subpageId)
                else CatalogueStore.firstReadable(pageId)
                item ?: return null
                itemId = item.id
                sessionId = item.group
                val label = CatalogueStore.label(item, lang)
                return Shown(head = label, body = "", label = label)
            }
            PUJA -> {
                val list = pujaItems()
                val item = list.firstOrNull { it.id == itemId } ?: list.firstOrNull() ?: return null
                itemId = item.id
                sessionId = "puja"
                val label = MadhwaStore.label(item, lang)
                return Shown(
                    head = label,
                    body = "",
                    label = label
                )
            }
            else -> return null
        }
    }

    private fun rows(): List<Row> {
        when (pageId) {
            SANDHYA -> return emptyList()
            MANTRAS, HADUGALU, STOTRA, SUKTAS, NAMAVALI -> {
                val out = ArrayList<Row>()
                val groups = if (usesSubpageFilter()) CatalogueStore.groupsForSubpage(pageId, subpageId)
                else CatalogueStore.groupsFor(pageId)
                groups.forEach { group ->
                    out += Row(
                        id = group.id,
                        group = group.id,
                        title = CatalogueStore.groupLabel(group, lang),
                        section = true
                    )
                    val entries = if (usesSubpageFilter()) CatalogueStore.inGroup(group.id, subpageId)
                    else CatalogueStore.inGroup(group.id)
                    entries.forEach { item ->
                        out += Row(
                            id = item.id,
                            group = group.id,
                            title = CatalogueStore.label(item, lang),
                            section = false
                        )
                    }
                }
                return out
            }
            PUJA -> {
                return pujaItems().map { item ->
                    Row(id = item.id, group = "puja", title = MadhwaStore.label(item, lang), section = false)
                }
            }
            else -> return emptyList()
        }
    }

    private fun catalogueItemAllowed(item: com.paperdesk.launcher.data.CatalogueItem?): Boolean {
        item ?: return false
        if (item.group !in CatalogueStore.groupsFor(pageId).map { it.id }) return false
        return !usesSubpageFilter() || CatalogueStore.belongsToSubpage(item, subpageId)
    }

    private fun isCataloguePage(): Boolean = pageId in CATALOGUE_PAGES
    private fun usesSubpageFilter(): Boolean = lockedSubpage() != null
    private fun lockedSubpage(): String? = when (pageId) {
        MANTRAS -> MANTRA_SUBPAGE
        STOTRA -> STOTRA_SUBPAGE
        SUKTAS -> SUKTA_SUBPAGE
        NAMAVALI -> NAMAVALI_SUBPAGE
        else -> null
    }

    private fun pujaItems() = MadhwaStore.inCategory("puja") + MadhwaStore.inCategory("festival")

    private data class Shown(val head: String, val body: CharSequence, val label: String)
    data class Row(val id: String, val group: String, val title: String, val section: Boolean)

    private class SelectAdapter(
        private val onPick: (Row) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private val data = mutableListOf<Row>()
        var lang: IndicScript.Lang = IndicScript.Lang.KN
        var selected: String = ""

        fun submit(items: List<Row>) {
            data.clear()
            data.addAll(items)
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int): Int = if (data[position].section) 0 else 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inf = LayoutInflater.from(parent.context)
            return if (viewType == 0) {
                SectionVH(inf.inflate(R.layout.item_select_section, parent, false))
            } else {
                ItemVH(inf.inflate(R.layout.item_select, parent, false))
            }
        }

        override fun getItemCount(): Int = data.size

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val row = data[position]
            if (holder is SectionVH) {
                holder.title.text = row.title
                DeskType.apply(holder.title, DeskType.SEMIBOLD)
            } else if (holder is ItemVH) {
                val on = row.id == selected
                holder.title.text = row.title
                val ctx = holder.itemView.context
                holder.itemView.isSelected = on
                holder.itemView.setBackgroundColor(
                    ctx.getColor(if (on) R.color.md_theme_secondaryContainer else R.color.md_theme_surface)
                )
                holder.itemView.findViewById<View>(R.id.selectMark).visibility = if (on) View.VISIBLE else View.INVISIBLE
                holder.title.setTextColor(
                    ctx.getColor(if (on) R.color.md_interactive else R.color.md_text_primary)
                )
                DeskType.apply(holder.title, if (on) DeskType.SEMIBOLD else DeskType.REGULAR)
                holder.itemView.setOnClickListener { onPick(row) }
            }
        }

        class SectionVH(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.selectSection)
        }

        class ItemVH(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.selectTitle)
        }
    }

    companion object {
        const val SANDHYA = "sandhya"
        const val MANTRAS = "mantras"
        const val PUJA = "puja"
        const val HADUGALU = "hadugalu"
        const val STOTRA = "stotra"
        const val SUKTAS = "suktas"
        const val NAMAVALI = "namavali"
        private const val MANTRA_SUBPAGE = "mantras"
        private const val STOTRA_SUBPAGE = "stotra"
        private const val SUKTA_SUBPAGE = "suktas"
        private const val NAMAVALI_SUBPAGE = "namavali"
        private val CATALOGUE_PAGES = setOf(MANTRAS, HADUGALU, STOTRA, SUKTAS, NAMAVALI)
        private const val ARG_ID = "page_id"

        fun create(id: String): ReaderFragment {
            val f = ReaderFragment()
            f.arguments = Bundle().apply { putString(ARG_ID, id) }
            return f
        }
    }
}
