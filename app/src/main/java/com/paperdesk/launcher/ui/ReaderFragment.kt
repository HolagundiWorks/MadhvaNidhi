package com.paperdesk.launcher.ui

import android.os.Bundle
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.CatalogueStore
import com.paperdesk.launcher.data.IndicScript
import com.paperdesk.launcher.data.MadhwaStore
import com.paperdesk.launcher.data.OsLang
import com.paperdesk.launcher.data.SandhyaStore
import com.paperdesk.launcher.data.Sankalpa

class ReaderFragment : Fragment() {

    private var pageId = SANDHYA
    private var lang = IndicScript.Lang.KN
    private var sessionId = ""
    private var itemId = ""
    private var subpageId = MANTRA_SUBPAGE
    private lateinit var adapter: SelectAdapter
    private var bindingKala = false
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
        view.findViewById<View>(R.id.selectorPage).setBackgroundColor(PagePalette.surface(requireContext(), pageId))
        if (pageId == SANDHYA) SandhyaStore.load(requireContext())
        if (pageId == MANTRAS || pageId == HADUGALU) CatalogueStore.load(requireContext())
        if (pageId == PUJA) MadhwaStore.load(requireContext())
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
        if (pageId == MANTRAS) setupSubpages(view)
        sessionId = prefs.getString("session", "").orEmpty()
        itemId = prefs.getString("item", "").orEmpty()
        subpageId = prefs.getString("subpage", MANTRA_SUBPAGE).orEmpty().takeIf { it in SUBPAGES } ?: MANTRA_SUBPAGE
        if (pageId == SANDHYA) {
            if (sessionId.isBlank() || SandhyaStore.session(requireContext(), sessionId) == null) {
                sessionId = SandhyaStore.nowId()
            }
            val steps = SandhyaStore.session(requireContext(), sessionId)?.steps.orEmpty()
            if (steps.none { it.id == itemId }) itemId = steps.firstOrNull()?.id.orEmpty()
        }
        if ((pageId == MANTRAS || pageId == HADUGALU) && !catalogueItemAllowed(CatalogueStore.item(itemId))) {
            itemId = if (pageId == MANTRAS) CatalogueStore.firstReadable(pageId, subpageId)?.id.orEmpty()
            else CatalogueStore.firstReadable(pageId)?.id.orEmpty()
            sessionId = CatalogueStore.item(itemId)?.group.orEmpty()
        }
        if (pageId == PUJA) {
            val list = pujaItems()
            if (list.none { it.id == itemId }) itemId = list.firstOrNull()?.id.orEmpty()
            sessionId = "puja"
        }
        paint()
    }

    override fun onResume() {
        super.onResume()
        OsLang.addListener(langTick)
        paint()
    }

    override fun onPause() {
        OsLang.removeListener(langTick)
        super.onPause()
    }

    fun closeSelector(): Boolean {
        val page = view?.findViewById<View>(R.id.selectorPage) ?: return false
        if (page.visibility != View.VISIBLE) return false
        page.visibility = View.GONE
        view?.findViewById<View>(R.id.readerMain)?.visibility = View.VISIBLE
        gate()?.setSelectorOpen(false)
        return true
    }

    private fun selectorOpen(): Boolean =
        view?.findViewById<View>(R.id.selectorPage)?.visibility == View.VISIBLE

    fun openSelector() {
        if (pageId == SANDHYA) return
        val v = view ?: return
        v.findViewById<View>(R.id.readerMain).visibility = View.GONE
        v.findViewById<View>(R.id.selectorPage).visibility = View.VISIBLE
        gate()?.setSelectorOpen(true)
        bindSelector()
    }

    private fun pick(row: Row) {
        if (row.section && pageId == SANDHYA) {
            pickKala(row.group)
            closeSelector()
            return
        }
        if (row.section) return
        sessionId = row.group
        itemId = row.id
        prefs.edit().putString("session", sessionId).putString("item", itemId).apply()
        closeSelector()
        paint()
        view?.findViewById<DeskScrollView>(R.id.readerScroll)?.scrollTo(0, 0)
    }

    private fun pickKala(id: String) {
        if (pageId != SANDHYA) return
        sessionId = id
        val next = SandhyaStore.session(requireContext(), id)
        itemId = next?.steps?.firstOrNull()?.id.orEmpty()
        prefs.edit().putString("session", sessionId).putString("item", itemId).apply()
        paint()
        view?.findViewById<DeskScrollView>(R.id.readerScroll)?.scrollTo(0, 0)
    }

    private fun setupSubpages(v: View) {
        v.findViewById<View>(R.id.readerSubpages).visibility = View.VISIBLE
        v.findViewById<View>(R.id.subpageMantras).setOnClickListener { pickSubpage(MANTRA_SUBPAGE) }
        v.findViewById<View>(R.id.subpageStotras).setOnClickListener { pickSubpage(STOTRA_SUBPAGE) }
        v.findViewById<View>(R.id.subpageSuktas).setOnClickListener { pickSubpage(SUKTA_SUBPAGE) }
    }

    private fun pickSubpage(id: String) {
        if (pageId != MANTRAS || id !in SUBPAGES || id == subpageId) return
        subpageId = id
        itemId = CatalogueStore.firstReadable(MANTRAS, subpageId)?.id.orEmpty()
        sessionId = CatalogueStore.item(itemId)?.group.orEmpty()
        prefs.edit().putString("subpage", subpageId).putString("session", sessionId).putString("item", itemId).apply()
        paint()
        view?.findViewById<DeskScrollView>(R.id.readerScroll)?.scrollTo(0, 0)
    }

    private fun paint() {
        val v = view ?: return
        lang = OsLang.get(requireContext())
        v.findViewById<TextView>(R.id.readerTitle).text = OsLang.ui(pageId, lang)
        v.findViewById<View>(R.id.readerTitle).visibility = if (pageId == SANDHYA) View.INVISIBLE else View.GONE
        v.findViewById<TextView>(R.id.selectorTitle).text = OsLang.ui("select_$pageId", lang)
        DeskType.apply(v.findViewById(R.id.readerTitle), DeskType.SEMIBOLD)
        DeskType.apply(v.findViewById(R.id.selectorTitle), DeskType.SEMIBOLD)
        val item = current()
        val sandhya = pageId == SANDHYA
        v.findViewById<View>(R.id.readerSubpages).visibility = if (pageId == MANTRAS) View.VISIBLE else View.GONE
        if (pageId == MANTRAS) bindSubpages(v)
        v.findViewById<View>(R.id.readerSelect).visibility = View.GONE
        v.findViewById<View>(R.id.readerKala).visibility = if (sandhya) View.VISIBLE else View.GONE
        v.findViewById<View>(R.id.readerHead).visibility = if (item?.head.isNullOrBlank()) View.GONE else View.VISIBLE
        if (sandhya) bindKala(v)
        v.findViewById<TextView>(R.id.readerHead).text = item?.head.orEmpty()
        v.findViewById<TextView>(R.id.readerHead).setTextColor(PagePalette.accent(requireContext(), pageId))
        v.findViewById<TextView>(R.id.readerBody).text = item?.body ?: ""
        v.findViewById<TextView>(R.id.readerSelectLabel).text =
            item?.label ?: OsLang.ui("empty_page", lang)
        v.findViewById<TextView>(R.id.readerSelectLabel).visibility = View.GONE
        val crumb = v.findViewById<TextView>(R.id.readerCrumb)
        crumb.text = breadcrumb(item)
        crumb.contentDescription = crumb.text
        DeskType.apply(crumb, DeskType.REGULAR)
        v.findViewById<TextView>(R.id.readerSelectLabel).alpha = if (item == null) 0.45f else 1f
        DeskType.apply(v.findViewById(R.id.readerHead), DeskType.SEMIBOLD)
        DeskType.apply(v.findViewById(R.id.readerBody), DeskType.REGULAR)
        DeskType.apply(v.findViewById(R.id.readerSelectLabel), DeskType.REGULAR)
        v.findViewById<View>(R.id.readerSelect).contentDescription =
            "${OsLang.ui("select_$pageId", lang)}. ${item?.label ?: OsLang.ui("empty_page", lang)}"
        v.findViewById<View>(R.id.selectorBack).contentDescription = OsLang.ui("back", lang)
        if (selectorOpen()) bindSelector()
    }

    private fun breadcrumb(item: Shown?): String {
        val page = OsLang.ui(pageId, lang)
        val parts = ArrayList<String>(3)
        parts += page
        when (pageId) {
            SANDHYA -> {
                parts += SandhyaStore.vedaLabel(requireContext())
                val kala = SandhyaStore.session(requireContext(), sessionId)?.title.orEmpty()
                if (kala.isNotBlank()) parts += SandhyaStore.label(kala, lang)
            }
            MANTRAS, HADUGALU -> {
                val cat = CatalogueStore.item(itemId)
                val group = CatalogueStore.groupsFor(pageId).firstOrNull { it.id == (cat?.group ?: sessionId) }
                val section = group?.let { CatalogueStore.groupLabel(it, lang) }.orEmpty()
                val name = item?.label.orEmpty()
                if (pageId == MANTRAS) parts += OsLang.ui(subpageId, lang)
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

    private fun setupKalaDropdown(v: View) {
        val spinner = v.findViewById<Spinner>(R.id.readerKala)
        spinner.visibility = View.VISIBLE
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (bindingKala) return
                val selected = SandhyaStore.sessions(requireContext()).getOrNull(position)?.id ?: return
                if (selected != sessionId) pickKala(selected)
            }
        }
    }

    private fun bindKala(v: View) {
        val sessions = SandhyaStore.sessions(requireContext())
        val labels = sessions.map { SandhyaStore.label(it.title, lang) }
        val spinner = v.findViewById<Spinner>(R.id.readerKala)
        bindingKala = true
        spinner.adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown_selected, labels).also {
            it.setDropDownViewResource(R.layout.item_dropdown)
        }
        spinner.setSelection(sessions.indexOfFirst { it.id == sessionId }.coerceAtLeast(0), false)
        spinner.contentDescription = "ಸಂಧ್ಯಾ ಕಾಲ ಆಯ್ಕೆ. ${labels.getOrNull(spinner.selectedItemPosition).orEmpty()}"
        bindingKala = false
    }

    private fun bindSubpages(v: View) {
        val tabs = listOf(
            Triple(R.id.subpageMantras, MANTRA_SUBPAGE, OsLang.ui("mantras", lang)),
            Triple(R.id.subpageStotras, STOTRA_SUBPAGE, OsLang.ui("stotra", lang)),
            Triple(R.id.subpageSuktas, SUKTA_SUBPAGE, OsLang.ui("suktas", lang))
        )
        tabs.forEach { (viewId, id, title) ->
            v.findViewById<TextView>(viewId).apply {
                text = title
                contentDescription = title
                val active = id == subpageId
                setBackgroundResource(if (active) R.drawable.bg_subpage_on else R.drawable.bg_subpage)
                setTextColor(requireContext().getColor(if (active) R.color.accent else R.color.white))
                DeskType.apply(this, if (active) DeskType.SEMIBOLD else DeskType.REGULAR)
            }
        }
    }

    private fun bindSelector() {
        val v = view ?: return
        val rows = rows()
        adapter.lang = lang
        adapter.selected = itemId
        adapter.submit(rows)
        val empty = rows.isEmpty()
        v.findViewById<View>(R.id.selectorEmpty).visibility = if (empty) View.VISIBLE else View.GONE
        v.findViewById<TextView>(R.id.selectorEmpty).text = OsLang.ui("empty_page", lang)
        val list = v.findViewById<RecyclerView>(R.id.selectorList)
        list.visibility = if (empty) View.GONE else View.VISIBLE
        if (!empty) keepSelectedRowFullyVisible(list, rows.indexOfFirst { it.id == itemId })
    }

    /** Put the active choice on a clean row instead of leaving it clipped by either edge. */
    private fun keepSelectedRowFullyVisible(list: RecyclerView, position: Int) {
        if (position < 0) return
        list.post {
            val manager = list.layoutManager as? LinearLayoutManager ?: return@post
            manager.scrollToPositionWithOffset(position, 0)
        }
    }

    private fun current(): Shown? {
        when (pageId) {
            SANDHYA -> {
                val session = SandhyaStore.session(requireContext(), sessionId)
                    ?: SandhyaStore.session(requireContext(), SandhyaStore.nowId()) ?: return null
                sessionId = session.id
                itemId = session.steps.firstOrNull()?.id.orEmpty()
                val completeRitual = SpannableStringBuilder()
                session.steps.forEachIndexed { index, step ->
                    if (index > 0) completeRitual.append("\n\n")
                    val number = OsLang.digits(step.n, lang)
                    val title = SandhyaStore.label(step.title, lang)
                    val body = SandhyaStore.body(requireContext(), step, session.id, lang).trim()
                    val headingStart = completeRitual.length
                    completeRitual.append("$number. $title")
                    val headingEnd = completeRitual.length
                    completeRitual.setSpan(
                        ForegroundColorSpan(PagePalette.accent(requireContext(), SANDHYA)),
                        headingStart,
                        headingEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    completeRitual.setSpan(
                        StyleSpan(Typeface.BOLD),
                        headingStart,
                        headingEnd,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    completeRitual.append("\n$body")
                }
                return Shown(
                    head = "",
                    body = completeRitual,
                    label = SandhyaStore.label(session.title, lang)
                )
            }
            MANTRAS, HADUGALU -> {
                val groups = if (pageId == MANTRAS) CatalogueStore.groupsForSubpage(pageId, subpageId)
                else CatalogueStore.groupsFor(pageId)
                val allowed = groups.map { it.id }.toSet()
                val item = CatalogueStore.item(itemId)?.takeIf {
                    it.group in allowed && (pageId != MANTRAS || CatalogueStore.belongsToSubpage(it, subpageId))
                } ?: if (pageId == MANTRAS) CatalogueStore.firstReadable(pageId, subpageId)
                else CatalogueStore.firstReadable(pageId)
                item ?: return null
                itemId = item.id
                sessionId = item.group
                val label = CatalogueStore.label(item, lang)
                return Shown(
                    head = label,
                    body = withoutRepeatedHeading(CatalogueStore.body(requireContext(), item, lang), label),
                    label = label
                )
            }
            PUJA -> {
                val list = pujaItems()
                val item = list.firstOrNull { it.id == itemId } ?: list.firstOrNull() ?: return null
                itemId = item.id
                sessionId = "puja"
                return Shown(
                    head = MadhwaStore.label(item, lang),
                    body = Sankalpa.injectPuja(
                        requireContext(), MadhwaStore.body(requireContext(), item, lang), lang
                    ),
                    label = MadhwaStore.label(item, lang)
                )
            }
            else -> return null
        }
    }

    private fun rows(): List<Row> {
        when (pageId) {
            SANDHYA -> {
                return emptyList()
            }
            MANTRAS, HADUGALU -> {
                val out = ArrayList<Row>()
                val groups = if (pageId == MANTRAS) CatalogueStore.groupsForSubpage(pageId, subpageId)
                else CatalogueStore.groupsFor(pageId)
                groups.forEach { group ->
                    out += Row(
                        id = group.id,
                        group = group.id,
                        title = CatalogueStore.groupLabel(group, lang),
                        section = true
                    )
                    val entries = if (pageId == MANTRAS) CatalogueStore.inGroup(group.id, subpageId)
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

    private fun gate(): LauncherActivity? = activity as? LauncherActivity

    private fun catalogueItemAllowed(item: com.paperdesk.launcher.data.CatalogueItem?): Boolean {
        item ?: return false
        if (item.group !in CatalogueStore.groupsFor(pageId).map { it.id }) return false
        return pageId != MANTRAS || CatalogueStore.belongsToSubpage(item, subpageId)
    }

    private fun withoutRepeatedHeading(body: String, heading: String): String {
        val lines = body.lines()
        val first = lines.indexOfFirst { it.isNotBlank() }
        if (first < 0) return body
        fun comparable(value: String) = value.lowercase()
            .replace(Regex("[\\s·:।॥|—–_-]+"), "")
        if (comparable(lines[first]) != comparable(heading)) return body
        return lines.filterIndexed { index, _ -> index != first }.joinToString("\n").trimStart()
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
                holder.mark.setBackgroundResource(if (on) R.drawable.bg_mark_on else R.drawable.bg_mark)
                val ctx = holder.itemView.context
                holder.title.setTextColor(ctx.getColor(if (on) R.color.accent else R.color.white))
                DeskType.apply(holder.title, if (on) DeskType.SEMIBOLD else DeskType.REGULAR)
                holder.itemView.setOnClickListener { onPick(row) }
            }
        }

        class SectionVH(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.selectSection)
        }

        class ItemVH(view: View) : RecyclerView.ViewHolder(view) {
            val mark: View = view.findViewById(R.id.selectMark)
            val title: TextView = view.findViewById(R.id.selectTitle)
        }
    }

    companion object {
        const val SANDHYA = "sandhya"
        const val MANTRAS = "mantras"
        const val PUJA = "puja"
        const val HADUGALU = "hadugalu"
        private const val MANTRA_SUBPAGE = "mantras"
        private const val STOTRA_SUBPAGE = "stotra"
        private const val SUKTA_SUBPAGE = "suktas"
        private val SUBPAGES = setOf(MANTRA_SUBPAGE, STOTRA_SUBPAGE, SUKTA_SUBPAGE)
        private const val ARG_ID = "page_id"

        fun create(id: String): ReaderFragment {
            val f = ReaderFragment()
            f.arguments = Bundle().apply { putString(ARG_ID, id) }
            return f
        }
    }
}
