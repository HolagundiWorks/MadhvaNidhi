package com.paperdesk.launcher.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.FavStore
import com.paperdesk.launcher.data.IndicScript
import com.paperdesk.launcher.data.LibraryItem
import com.paperdesk.launcher.data.LibraryStore
import com.paperdesk.launcher.data.OsLang

class LibraryFragment : Fragment() {

    private var collection = LibraryStore.MANTRAS
    private var titleRes = R.string.page_mantras
    private var items: List<LibraryItem> = emptyList()
    private lateinit var adapter: Adapter
    private var selectedId = ""
    private var lang = IndicScript.Lang.KN
    private val langTick: () -> Unit = { applyLang() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collection = arguments?.getString(ARG_COL) ?: LibraryStore.MANTRAS
        titleRes = arguments?.getInt(ARG_TITLE) ?: R.string.page_mantras
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.page_library, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lang = OsLang.get(requireContext())
        view.findViewById<TextView>(R.id.libTitle).setText(titleRes)
        adapter = Adapter(
            collection = collection,
            canStar = FavStore.canStar(collection),
            onOpen = { show(it) },
            onStar = { toggleStar(it) }
        )
        view.findViewById<RecyclerView>(R.id.libList).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@LibraryFragment.adapter
        }
        items = LibraryStore.load(requireContext(), collection)
        view.findViewById<EditText>(R.id.libSearch).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                bindList(s?.toString().orEmpty())
            }
        })
        val star = view.findViewById<TextView>(R.id.libStar)
        if (FavStore.canStar(collection)) {
            star.visibility = View.VISIBLE
            star.setOnClickListener {
                items.firstOrNull { it.id == selectedId }?.let { toggleStar(it) }
            }
        } else {
            star.visibility = View.GONE
        }
        bindList("")
        val first = if (FavStore.canStar(collection)) {
            FavStore.sort(items, FavStore.ids(requireContext(), collection)).firstOrNull()
        } else {
            items.firstOrNull()
        }
        first?.let { show(it) }
        DeskType.apply(view.findViewById(R.id.libTitle), DeskType.SEMIBOLD)
    }

    override fun onResume() {
        super.onResume()
        OsLang.addListener(langTick)
        applyLang()
    }

    override fun onPause() {
        OsLang.removeListener(langTick)
        super.onPause()
    }

    private fun applyLang() {
        lang = OsLang.get(requireContext())
        bindList(view?.findViewById<EditText>(R.id.libSearch)?.text?.toString().orEmpty())
        items.firstOrNull { it.id == selectedId }?.let { show(it) }
    }

    private fun toggleStar(item: LibraryItem) {
        FavStore.toggle(requireContext(), collection, item.id)
        bindList(view?.findViewById<EditText>(R.id.libSearch)?.text?.toString().orEmpty())
        paintStar(item.id)
    }

    private fun bindList(query: String) {
        val found = LibraryStore.search(items, query)
        val rows = if (FavStore.canStar(collection)) {
            FavStore.sort(found, FavStore.ids(requireContext(), collection))
        } else {
            found
        }
        adapter.lang = lang
        adapter.submit(rows, selectedId)
        view?.findViewById<TextView>(R.id.libMeta)?.text =
            if (query.isBlank()) getString(R.string.library_counts, items.size)
            else getString(R.string.library_hits, rows.size, items.size)
    }

    private fun show(item: LibraryItem) {
        val v = view ?: return
        selectedId = item.id
        adapter.selected = item.id
        val title = LibraryStore.label(item, lang)
        val section = if (item.section.isBlank()) "" else IndicScript.convert(item.section, lang)
        v.findViewById<TextView>(R.id.libHead).text =
            title + if (section.isBlank()) "" else "  ·  ${section.uppercase(java.util.Locale.US)}"
        v.findViewById<TextView>(R.id.libBody).text =
            LibraryStore.text(requireContext(), collection, item.id, lang)
        DeskType.apply(v.findViewById(R.id.libHead), DeskType.SEMIBOLD)
        paintStar(item.id)
        val idx = adapter.indexOf(item.id)
        if (idx >= 0) v.findViewById<RecyclerView>(R.id.libList).scrollToPosition(idx)
    }

    private fun paintStar(id: String) {
        val star = view?.findViewById<TextView>(R.id.libStar) ?: return
        if (!FavStore.canStar(collection)) return
        val on = FavStore.starred(requireContext(), collection, id)
        star.text = getString(if (on) R.string.star_on else R.string.star_off)
        star.setTextColor(star.context.getColor(if (on) R.color.accent else R.color.white_55))
        adapter.notifyDataSetChanged()
    }

    private class Adapter(
        private val collection: String,
        private val canStar: Boolean,
        private val onOpen: (LibraryItem) -> Unit,
        private val onStar: (LibraryItem) -> Unit
    ) : RecyclerView.Adapter<Adapter.VH>() {
        private val rows = mutableListOf<LibraryItem>()
        var lang: IndicScript.Lang = IndicScript.Lang.KN
        var selected: String = ""
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        fun submit(items: List<LibraryItem>, sel: String) {
            rows.clear()
            rows.addAll(items)
            selected = sel
        }

        fun indexOf(id: String) = rows.indexOfFirst { it.id == id }

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.libItemTitle)
            val meta: TextView = view.findViewById(R.id.libItemMeta)
            val star: TextView = view.findViewById(R.id.libItemStar)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_library, parent, false))
        }

        override fun getItemCount(): Int = rows.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = rows[position]
            val on = item.id == selected
            val ctx = holder.itemView.context
            holder.title.text = LibraryStore.label(item, lang)
            holder.meta.text = IndicScript.convert(item.section, lang)
            holder.title.setTextColor(ctx.getColor(if (on) R.color.accent else R.color.white))
            DeskType.apply(holder.title, if (on) DeskType.SEMIBOLD else DeskType.REGULAR)
            holder.itemView.setOnClickListener { onOpen(item) }
            if (canStar) {
                holder.star.visibility = View.VISIBLE
                val starred = FavStore.starred(ctx, collection, item.id)
                holder.star.text = ctx.getString(if (starred) R.string.star_on else R.string.star_off)
                holder.star.setTextColor(ctx.getColor(if (starred) R.color.accent else R.color.white_55))
                holder.star.setOnClickListener { onStar(item) }
            } else {
                holder.star.visibility = View.GONE
            }
        }
    }

    companion object {
        private const val ARG_COL = "col"
        private const val ARG_TITLE = "title"

        fun mantras() = create(LibraryStore.MANTRAS, R.string.page_mantras)
        fun puja() = create(LibraryStore.PUJA, R.string.page_puja)
        fun stotra() = create(LibraryStore.STOTRA, R.string.page_stotra)
        fun namavali() = create(LibraryStore.NAMAVALI, R.string.page_namavali)

        private fun create(col: String, title: Int) = LibraryFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_COL, col)
                putInt(ARG_TITLE, title)
            }
        }
    }
}
