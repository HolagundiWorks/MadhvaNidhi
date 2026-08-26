package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.GitaAudio
import com.paperdesk.launcher.data.GitaStore
import com.paperdesk.launcher.data.IndicScript
import com.paperdesk.launcher.data.OsLang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GitaFragment : Fragment() {

    private val prefs by lazy {
        requireContext().getSharedPreferences("paperdesk_gita", 0)
    }
    private var verse: GitaStore.Verse? = null
    private lateinit var adapter: ChapterAdapter
    private var followPlay = false
    private var playGen = 0
    private var loadingPlay = false
    private val langTick: () -> Unit = { verse?.let { show(it) } }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.page_gita, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = ChapterAdapter { openChapter(it) }
        view.findViewById<RecyclerView>(R.id.gitaChapters).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@GitaFragment.adapter
        }
        view.findViewById<View>(R.id.gitaPrev).setOnClickListener {
            verse?.let { v ->
                GitaStore.prev(v)?.let { show(it) }
            }
        }
        view.findViewById<View>(R.id.gitaNext).setOnClickListener {
            verse?.let { v ->
                GitaStore.next(v)?.let { show(it) }
            }
        }
        view.findViewById<View>(R.id.gitaPlay).setOnClickListener { togglePlay() }
        bindPlay()
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) { GitaStore.load(requireContext()) }
            val v = view ?: return@launch
            adapter.submit(GitaStore.chapters)
            v.findViewById<TextView>(R.id.gitaMeta).text =
                getString(R.string.gita_counts, GitaStore.chapters.size, GitaStore.verses.size)
            val c = prefs.getInt("c", 1)
            val n = prefs.getInt("v", 1)
            val found = GitaStore.versesIn(c).firstOrNull { it.v == n }
                ?: GitaStore.versesIn(1).firstOrNull()
            if (found != null) show(found) else adapter.selected = 1
        }
    }

    override fun onResume() {
        super.onResume()
        OsLang.addListener(langTick)
        verse?.let { show(it) }
    }

    override fun onPause() {
        OsLang.removeListener(langTick)
        super.onPause()
    }

    override fun onStop() {
        haltPlay()
        super.onStop()
    }

    override fun onDestroyView() {
        haltPlay()
        super.onDestroyView()
    }

    private fun openChapter(ch: GitaStore.Chapter) {
        GitaStore.versesIn(ch.n).firstOrNull()?.let { show(it) }
    }

    private fun togglePlay() {
        val item = verse ?: return
        if (followPlay || GitaAudio.isPlaying()) {
            haltPlay()
            return
        }
        followPlay = true
        playVerse(item)
    }

    private fun haltPlay() {
        followPlay = false
        loadingPlay = false
        playGen++
        GitaAudio.stop()
        bindPlay()
    }

    private fun playVerse(item: GitaStore.Verse) {
        val gen = ++playGen
        loadingPlay = true
        bindPlay()
        viewLifecycleOwner.lifecycleScope.launch {
            val file = withContext(Dispatchers.IO) {
                GitaAudio.ensureFile(requireContext(), item.c, item.v)
            }
            if (gen != playGen || view == null) return@launch
            loadingPlay = false
            if (file == null) {
                followPlay = false
                bindPlay()
                return@launch
            }
            GitaAudio.start(
                requireContext(),
                file,
                item.c,
                item.v,
                onComplete = {
                    if (gen != playGen) return@start
                    val cur = verse
                    if (!followPlay || cur == null || GitaStore.isLast(cur)) {
                        followPlay = false
                        bindPlay()
                        return@start
                    }
                    GitaStore.next(cur)?.let { show(it) }
                },
                onError = {
                    if (gen != playGen) return@start
                    followPlay = false
                    loadingPlay = false
                    bindPlay()
                }
            )
            bindPlay()
        }
    }

    private fun bindPlay() {
        val btn = view?.findViewById<TextView>(R.id.gitaPlay) ?: return
        val playing = followPlay && !loadingPlay && GitaAudio.isPlaying()
        btn.isEnabled = !loadingPlay
        when {
            loadingPlay -> {
                btn.text = getString(R.string.gita_wait)
                btn.setBackgroundResource(R.drawable.bg_btn_secondary)
                btn.setTextColor(btn.context.getColor(R.color.white))
            }
            playing || followPlay -> {
                btn.text = getString(R.string.gita_stop)
                btn.setBackgroundResource(R.drawable.bg_btn_primary)
                btn.setTextColor(btn.context.getColor(R.color.black))
            }
            else -> {
                btn.text = getString(R.string.gita_play)
                btn.setBackgroundResource(R.drawable.bg_btn_primary)
                btn.setTextColor(btn.context.getColor(R.color.black))
            }
        }
        DeskType.apply(btn, DeskType.SEMIBOLD)
    }

    private fun show(item: GitaStore.Verse) {
        val v = view ?: return
        verse = item
        adapter.selected = item.c
        val lang = OsLang.get(requireContext())
        val ch = GitaStore.chapter(item.c)
        val chName = ch?.name?.let { IndicScript.convert(it, lang) }.orEmpty()
        v.findViewById<TextView>(R.id.gitaVerseHead).text =
            "CH ${item.c}  ·  V ${item.v}" + if (chName.isNotBlank()) "  ·  $chName" else ""
        v.findViewById<TextView>(R.id.gitaSlok).text = IndicScript.convert(item.slok, lang)
        v.findViewById<TextView>(R.id.gitaTr).text = item.tr
        v.findViewById<TextView>(R.id.gitaEn).text = item.en
        v.findViewById<TextView>(R.id.gitaHi).text = item.hi
        DeskType.apply(v.findViewById(R.id.gitaTitle), DeskType.SEMIBOLD)
        DeskType.apply(v.findViewById(R.id.gitaVerseHead), DeskType.SEMIBOLD)
        prefs.edit().putInt("c", item.c).putInt("v", item.v).apply()
        val idx = GitaStore.chapters.indexOfFirst { it.n == item.c }
        if (idx >= 0) v.findViewById<RecyclerView>(R.id.gitaChapters).scrollToPosition(idx)
        if (followPlay && !GitaAudio.isPlaying(item.c, item.v)) playVerse(item)
    }

    private class ChapterAdapter(
        private val onOpen: (GitaStore.Chapter) -> Unit
    ) : RecyclerView.Adapter<ChapterAdapter.VH>() {
        private val rows = mutableListOf<GitaStore.Chapter>()
        var selected: Int = 1
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        fun submit(items: List<GitaStore.Chapter>) {
            rows.clear()
            rows.addAll(items)
            notifyDataSetChanged()
        }

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val num: TextView = view.findViewById(R.id.gitaChapterNum)
            val name: TextView = view.findViewById(R.id.gitaChapterName)
            val meaning: TextView = view.findViewById(R.id.gitaChapterMeaning)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_gita_chapter, parent, false))
        }

        override fun getItemCount(): Int = rows.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = rows[position]
            val on = item.n == selected
            holder.num.text = item.n.toString()
            holder.name.text = item.name
            holder.meaning.text = item.meaning
            val ctx = holder.itemView.context
            holder.num.setTextColor(ctx.getColor(if (on) R.color.accent else R.color.white_55))
            holder.name.setTextColor(ctx.getColor(if (on) R.color.accent else R.color.white))
            DeskType.apply(holder.name, if (on) DeskType.SEMIBOLD else DeskType.REGULAR)
            holder.itemView.setOnClickListener { onOpen(item) }
        }
    }
}
