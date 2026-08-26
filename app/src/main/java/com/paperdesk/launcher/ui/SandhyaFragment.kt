package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.IndicScript
import com.paperdesk.launcher.data.OsLang
import com.paperdesk.launcher.data.SandhyaStore

class SandhyaFragment : Fragment() {

    private val prefs by lazy {
        requireContext().getSharedPreferences("paperdesk_sandhya", 0)
    }
    private lateinit var adapter: Adapter
    private var session: SandhyaStore.Session? = null
    private var step: SandhyaStore.Step? = null
    private var lang = IndicScript.Lang.KN
    private val langTick: () -> Unit = { applyLang() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.page_sandhya, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        SandhyaStore.load(requireContext())
        lang = OsLang.get(requireContext())
        adapter = Adapter { show(it) }
        adapter.lang = lang
        view.findViewById<RecyclerView>(R.id.sandhyaSteps).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SandhyaFragment.adapter
        }
        view.findViewById<View>(R.id.sandhyaPratah).setOnClickListener { openSession(SandhyaStore.PRATAH) }
        view.findViewById<View>(R.id.sandhyaMadhya).setOnClickListener { openSession(SandhyaStore.MADHYA) }
        view.findViewById<View>(R.id.sandhyaSayam).setOnClickListener { openSession(SandhyaStore.SAYAM) }
        view.findViewById<View>(R.id.sandhyaPrev).setOnClickListener {
            val s = session ?: return@setOnClickListener
            step?.let { cur -> SandhyaStore.prev(s, cur)?.let { show(it) } }
        }
        view.findViewById<View>(R.id.sandhyaNext).setOnClickListener {
            val s = session ?: return@setOnClickListener
            step?.let { cur -> SandhyaStore.next(s, cur)?.let { show(it) } }
        }
        DeskType.apply(view.findViewById(R.id.sandhyaTitle), DeskType.SEMIBOLD)
        val saved = prefs.getString("session", "").orEmpty()
        val start = if (saved.isNotBlank() && SandhyaStore.session(saved) != null) saved else SandhyaStore.nowId()
        openSession(start, prefs.getString("step", "").orEmpty())
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
        adapter.lang = lang
        val v = view ?: return
        v.findViewById<TextView>(R.id.sandhyaTitle).text = OsLang.ui("sandhya", lang)
        v.findViewById<TextView>(R.id.sandhyaPratah).text = OsLang.ui("pratah", lang)
        v.findViewById<TextView>(R.id.sandhyaMadhya).text = OsLang.ui("madhya", lang)
        v.findViewById<TextView>(R.id.sandhyaSayam).text = OsLang.ui("sayam", lang)
        paintSession()
        adapter.notifyDataSetChanged()
        session?.let { s ->
            v.findViewById<TextView>(R.id.sandhyaMeta).text =
                getString(R.string.sandhya_counts, s.steps.size)
        }
        step?.let { show(it) }
    }

    private fun openSession(id: String, keepStep: String = "") {
        val next = SandhyaStore.session(id) ?: return
        session = next
        prefs.edit().putString("session", next.id).apply()
        paintSession()
        adapter.submit(next.steps)
        view?.findViewById<TextView>(R.id.sandhyaMeta)?.text =
            getString(R.string.sandhya_counts, next.steps.size)
        val found = next.steps.firstOrNull { it.id == keepStep } ?: next.steps.firstOrNull()
        if (found != null) show(found)
    }

    private fun paintSession() {
        val v = view ?: return
        val id = session?.id
        fun chip(viewId: Int, on: Boolean) {
            val tv = v.findViewById<TextView>(viewId)
            tv.setBackgroundResource(android.R.color.transparent)
            tv.setTextColor(tv.context.getColor(if (on) R.color.accent else R.color.white))
            DeskType.apply(tv, if (on) DeskType.SEMIBOLD else DeskType.REGULAR)
            tv.alpha = if (on) 1f else 0.55f
        }
        chip(R.id.sandhyaPratah, id == SandhyaStore.PRATAH)
        chip(R.id.sandhyaMadhya, id == SandhyaStore.MADHYA)
        chip(R.id.sandhyaSayam, id == SandhyaStore.SAYAM)
    }

    private fun show(item: SandhyaStore.Step) {
        val v = view ?: return
        val s = session ?: return
        step = item
        adapter.selected = item.id
        prefs.edit().putString("step", item.id).apply()
        v.findViewById<TextView>(R.id.sandhyaHead).text =
            "${item.n}  ·  ${SandhyaStore.label(item.title, lang)}"
        v.findViewById<TextView>(R.id.sandhyaBody).text = SandhyaStore.body(item, lang)
        DeskType.apply(v.findViewById(R.id.sandhyaHead), DeskType.SEMIBOLD)
        val idx = s.steps.indexOfFirst { it.id == item.id }
        if (idx >= 0) v.findViewById<RecyclerView>(R.id.sandhyaSteps).scrollToPosition(idx)
        val prev = v.findViewById<TextView>(R.id.sandhyaPrev)
        val next = v.findViewById<TextView>(R.id.sandhyaNext)
        val atStart = idx <= 0
        val atEnd = idx < 0 || idx >= s.steps.lastIndex
        prev.isEnabled = !atStart
        next.isEnabled = !atEnd
        prev.alpha = if (atStart) 0.35f else 1f
        next.alpha = if (atEnd) 0.35f else 1f
        prev.setBackgroundResource(R.drawable.bg_circle)
        next.setBackgroundResource(if (atEnd) R.drawable.bg_circle else R.drawable.bg_circle_on)
        next.setTextColor(next.context.getColor(if (atEnd) R.color.white_55 else R.color.accent))
    }

    private class Adapter(
        private val onOpen: (SandhyaStore.Step) -> Unit
    ) : RecyclerView.Adapter<Adapter.VH>() {
        private val rows = mutableListOf<SandhyaStore.Step>()
        var lang: IndicScript.Lang = IndicScript.Lang.KN
        var selected: String = ""
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        fun submit(items: List<SandhyaStore.Step>) {
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
            val on = item.id == selected
            holder.title.text = SandhyaStore.label(item.title, lang)
            holder.meta.text = item.n.toString()
            holder.star.visibility = View.GONE
            val ctx = holder.itemView.context
            holder.title.setTextColor(ctx.getColor(if (on) R.color.accent else R.color.white))
            DeskType.apply(holder.title, if (on) DeskType.SEMIBOLD else DeskType.REGULAR)
            holder.itemView.setOnClickListener { onOpen(item) }
        }
    }
}
