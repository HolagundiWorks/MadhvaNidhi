package com.paperdesk.launcher.ui

import android.app.Dialog
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
import com.paperdesk.launcher.data.MadhwaItem
import com.paperdesk.launcher.data.MadhwaStore
import com.paperdesk.launcher.data.OsLang

class GranthaFragment : Fragment() {

    private val prefs by lazy { requireContext().getSharedPreferences("paperdesk_grantha", 0) }
    private var category = MadhwaStore.CATEGORIES.first()
    private var leafKey = ""
    private var lang = IndicScript.Lang.KN
    private val langTick: () -> Unit = { paint() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.page_grantha, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        MadhwaStore.load(requireContext())
        lang = OsLang.get(requireContext())
        val saved = prefs.getString("book", "").orEmpty()
        category = if (saved in MadhwaStore.CATEGORIES) saved else MadhwaStore.CATEGORIES.first()
        leafKey = prefs.getString("leaf", "").orEmpty()
        view.findViewById<View>(R.id.granthaBook).setOnClickListener { pickCategory() }
        view.findViewById<View>(R.id.granthaLeaf).setOnClickListener { pickLeaf(false) }
        view.findViewById<View>(R.id.granthaSearch).setOnClickListener { pickLeaf(true) }
        view.findViewById<View>(R.id.granthaPrev).setOnClickListener { step(-1) }
        view.findViewById<View>(R.id.granthaNext).setOnClickListener { step(1) }
        view.findViewById<View>(R.id.granthaStar).setOnClickListener { toggleStar() }
        if (leafKey.isBlank()) leafKey = firstLeaf()
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

    private fun persist() {
        prefs.edit().putString("book", category).putString("leaf", leafKey).apply()
    }

    private fun firstLeaf(): String = leaves().firstOrNull()?.id.orEmpty()

    private fun leaves(): List<MadhwaItem> {
        MadhwaStore.load(requireContext())
        return MadhwaStore.inCategory(category)
    }

    private fun paint() {
        val v = view ?: return
        lang = OsLang.get(requireContext())
        val rows = leaves()
        if (rows.none { it.id == leafKey }) leafKey = rows.firstOrNull()?.id.orEmpty()
        val cur = rows.firstOrNull { it.id == leafKey } ?: rows.firstOrNull()
        v.findViewById<TextView>(R.id.granthaBook).text = OsLang.ui(category, lang)
        v.findViewById<TextView>(R.id.granthaLeaf).text =
            cur?.let { MadhwaStore.label(it, lang) } ?: OsLang.ui("texts", lang)
        val author = cur?.let { MadhwaStore.author(it, lang) }.orEmpty()
        v.findViewById<TextView>(R.id.granthaHead).text =
            if (cur == null) OsLang.ui("texts", lang)
            else MadhwaStore.label(cur, lang) + if (author.isBlank()) "" else "  ·  $author"
        v.findViewById<TextView>(R.id.granthaBody).text =
            if (cur == null) "" else MadhwaStore.body(requireContext(), cur, lang)
        DeskType.apply(v.findViewById(R.id.granthaHead), DeskType.SEMIBOLD)
        DeskType.apply(v.findViewById(R.id.granthaBook), DeskType.SEMIBOLD)
        DeskType.apply(v.findViewById(R.id.granthaLeaf), DeskType.REGULAR)
        paintStar(cur)
        persist()
    }

    private fun paintStar(cur: MadhwaItem?) {
        val star = view?.findViewById<TextView>(R.id.granthaStar) ?: return
        if (cur == null || !FavStore.canStar(MadhwaStore.STARS)) {
            star.visibility = View.GONE
            return
        }
        star.visibility = View.VISIBLE
        val on = FavStore.starred(requireContext(), MadhwaStore.STARS, cur.id)
        star.text = getString(if (on) R.string.star_on else R.string.star_off)
        star.setTextColor(star.context.getColor(if (on) R.color.accent else R.color.white_55))
        star.setBackgroundResource(if (on) R.drawable.bg_circle_on else R.drawable.bg_circle)
    }

    private fun toggleStar() {
        val cur = leaves().firstOrNull { it.id == leafKey } ?: return
        FavStore.toggle(requireContext(), MadhwaStore.STARS, cur.id)
        paint()
    }

    private fun step(delta: Int) {
        val rows = leaves()
        if (rows.isEmpty()) return
        val i = rows.indexOfFirst { it.id == leafKey }.let { if (it < 0) 0 else it }
        leafKey = rows[(i + delta).coerceIn(0, rows.lastIndex)].id
        persist()
        paint()
    }

    private fun pickCategory() {
        val rows = MadhwaStore.CATEGORIES.map { PickRow(it, OsLang.ui(it, lang)) }
        showPick(false, false, rows, category) { id ->
            category = id
            leafKey = firstLeaf()
            persist()
            paint()
        }
    }

    private fun pickLeaf(searchAll: Boolean) {
        val src = if (searchAll) MadhwaStore.load(requireContext()) else leaves()
        val rows = src.map { item ->
            val cat = if (searchAll) OsLang.ui(item.category, lang) + "  ·  " else ""
            PickRow(item.id, cat + MadhwaStore.label(item, lang))
        }
        showPick(true, searchAll, rows, leafKey) { id ->
            val item = MadhwaStore.item(id) ?: return@showPick
            category = item.category
            leafKey = item.id
            persist()
            paint()
        }
    }

    private fun showPick(search: Boolean, searchAll: Boolean, rows: List<PickRow>, selected: String, onPick: (String) -> Unit) {
        val dlg = Dialog(requireContext(), R.style.Theme_PaperDesk_Dialog)
        dlg.setContentView(R.layout.popup_pick)
        val field = dlg.findViewById<EditText>(R.id.pickSearch)
        field.visibility = if (search) View.VISIBLE else View.GONE
        field.hint = OsLang.ui("search", lang)
        val list = dlg.findViewById<RecyclerView>(R.id.pickList)
        val adapter = PickAdapter(selected) { id ->
            onPick(id)
            dlg.dismiss()
        }
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
        adapter.submit(rows)
        if (search) {
            field.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val q = s?.toString().orEmpty().trim()
                    if (q.isBlank()) {
                        adapter.submit(rows)
                        return
                    }
                    val found = MadhwaStore.search(q, if (searchAll) "" else category)
                    adapter.submit(found.map { item ->
                        val cat = if (searchAll) OsLang.ui(item.category, lang) + "  ·  " else ""
                        PickRow(item.id, cat + MadhwaStore.label(item, lang))
                    })
                }
            })
        }
        DeskDialogs.show(dlg, 0.56f)
        val dm = resources.displayMetrics
        dlg.window?.setLayout(
            (dm.widthPixels * 0.56f).toInt(),
            (dm.heightPixels * 0.72f).toInt()
        )
    }

    private data class PickRow(val key: String, val label: String)

    private class PickAdapter(
        private var selected: String,
        private val onOpen: (String) -> Unit
    ) : RecyclerView.Adapter<PickAdapter.VH>() {
        private val rows = mutableListOf<PickRow>()

        fun submit(items: List<PickRow>) {
            rows.clear()
            rows.addAll(items)
            notifyDataSetChanged()
        }

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
            val on = item.key == selected
            holder.title.text = item.label
            holder.meta.visibility = View.GONE
            holder.star.visibility = View.GONE
            val ctx = holder.itemView.context
            holder.title.setTextColor(ctx.getColor(if (on) R.color.accent else R.color.white))
            DeskType.apply(holder.title, if (on) DeskType.SEMIBOLD else DeskType.REGULAR)
            holder.itemView.setOnClickListener { onOpen(item.key) }
        }
    }
}
