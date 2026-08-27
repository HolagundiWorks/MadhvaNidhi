package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
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
    private lateinit var adapter: SelectAdapter
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
        view.findViewById<View>(R.id.kalaPratah).setOnClickListener { pickKala(SandhyaStore.PRATAH) }
        view.findViewById<View>(R.id.kalaMadhya).setOnClickListener { pickKala(SandhyaStore.MADHYA) }
        view.findViewById<View>(R.id.kalaSayam).setOnClickListener { pickKala(SandhyaStore.SAYAM) }
        sessionId = prefs.getString("session", "").orEmpty()
        itemId = prefs.getString("item", "").orEmpty()
        if (pageId == SANDHYA) {
            if (sessionId.isBlank() || SandhyaStore.session(requireContext(), sessionId) == null) {
                sessionId = SandhyaStore.nowId()
            }
            val steps = SandhyaStore.session(requireContext(), sessionId)?.steps.orEmpty()
            if (steps.none { it.id == itemId }) itemId = steps.firstOrNull()?.id.orEmpty()
        }
        if ((pageId == MANTRAS || pageId == HADUGALU) &&
            (CatalogueStore.item(itemId) == null || CatalogueStore.item(itemId)?.group !in CatalogueStore.groupsFor(pageId).map { it.id })
        ) {
            itemId = CatalogueStore.firstReadable(pageId)?.id.orEmpty()
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
        itemId = SandhyaStore.session(requireContext(), id)?.steps?.firstOrNull()?.id.orEmpty()
        prefs.edit().putString("session", sessionId).putString("item", itemId).apply()
        paint()
        view?.findViewById<DeskScrollView>(R.id.readerScroll)?.scrollTo(0, 0)
    }

    private fun paint() {
        val v = view ?: return
        lang = OsLang.get(requireContext())
        v.findViewById<TextView>(R.id.readerTitle).text = OsLang.ui(pageId, lang)
        v.findViewById<View>(R.id.readerTitle).visibility = View.GONE
        v.findViewById<TextView>(R.id.selectorTitle).text = OsLang.ui("select_$pageId", lang)
        DeskType.apply(v.findViewById(R.id.readerTitle), DeskType.SEMIBOLD)
        DeskType.apply(v.findViewById(R.id.selectorTitle), DeskType.SEMIBOLD)
        val item = current()
        val sandhya = pageId == SANDHYA
        v.findViewById<View>(R.id.readerSelect).visibility = View.GONE
        v.findViewById<View>(R.id.readerKalaRow).visibility = View.GONE
        v.findViewById<View>(R.id.readerHead).visibility = if (sandhya || item?.head.isNullOrBlank()) View.GONE else View.VISIBLE
        if (sandhya) bindKala(v)
        v.findViewById<TextView>(R.id.readerHead).text = item?.head.orEmpty()
        v.findViewById<TextView>(R.id.readerBody).text = item?.body.orEmpty()
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
                val kala = item?.label.orEmpty()
                if (kala.isNotBlank() && kala != page) parts += kala
            }
            MANTRAS, HADUGALU -> {
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

    private fun bindKala(v: View) {
        fun chip(id: Int, key: String, on: Boolean) {
            val btn = v.findViewById<ImageButton>(id)
            btn.setBackgroundResource(if (on) R.drawable.bg_circle_fill else R.drawable.bg_circle)
            btn.imageAlpha = if (on) 255 else 200
            btn.contentDescription =
                if (on) "${OsLang.ui(key, lang)}. ${OsLang.ui("selected", lang)}" else OsLang.ui(key, lang)
        }
        chip(R.id.kalaPratah, "pratah", sessionId == SandhyaStore.PRATAH)
        chip(R.id.kalaMadhya, "madhya", sessionId == SandhyaStore.MADHYA)
        chip(R.id.kalaSayam, "sayam", sessionId == SandhyaStore.SAYAM)
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
        v.findViewById<View>(R.id.selectorList).visibility = if (empty) View.GONE else View.VISIBLE
    }

    private fun current(): Shown? {
        when (pageId) {
            SANDHYA -> {
                val session = SandhyaStore.session(requireContext(), sessionId)
                    ?: SandhyaStore.session(requireContext(), SandhyaStore.nowId()) ?: return null
                sessionId = session.id
                val ctx = requireContext()
                val body = session.steps.joinToString("\n\n") { step ->
                    val head = SandhyaStore.label(step.title, lang)
                    val text = SandhyaStore.body(ctx, step, session.id, lang)
                    "$head\n\n$text"
                }
                return Shown(
                    head = "",
                    body = body,
                    label = OsLang.ui(session.id, lang)
                )
            }
            MANTRAS, HADUGALU -> {
                val groups = CatalogueStore.groupsFor(pageId)
                val allowed = groups.map { it.id }.toSet()
                val item = CatalogueStore.item(itemId)?.takeIf { it.group in allowed }
                    ?: CatalogueStore.firstReadable(pageId)
                    ?: return null
                itemId = item.id
                sessionId = item.group
                return Shown(
                    head = CatalogueStore.label(item, lang),
                    body = CatalogueStore.body(requireContext(), item, lang),
                    label = CatalogueStore.label(item, lang)
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
                val out = ArrayList<Row>()
                SandhyaStore.sessions(requireContext()).forEach { session ->
                    out += Row(id = session.id, group = session.id, title = OsLang.ui(session.id, lang), section = true)
                    session.steps.forEach { step ->
                        out += Row(
                            id = step.id,
                            group = session.id,
                            title = SandhyaStore.label(step.title, lang),
                            section = false
                        )
                    }
                }
                return out
            }
            MANTRAS, HADUGALU -> {
                val out = ArrayList<Row>()
                CatalogueStore.groupsFor(pageId).forEach { group ->
                    out += Row(
                        id = group.id,
                        group = group.id,
                        title = CatalogueStore.groupLabel(group, lang),
                        section = true
                    )
                    CatalogueStore.inGroup(group.id).forEach { item ->
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

    private fun pujaItems() = MadhwaStore.inCategory("puja") + MadhwaStore.inCategory("festival")

    private data class Shown(val head: String, val body: String, val label: String)
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
        private const val ARG_ID = "page_id"

        fun create(id: String): ReaderFragment {
            val f = ReaderFragment()
            f.arguments = Bundle().apply { putString(ARG_ID, id) }
            return f
        }
    }
}
