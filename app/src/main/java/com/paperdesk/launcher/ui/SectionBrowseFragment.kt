package com.paperdesk.launcher.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.CatalogueStore
import com.paperdesk.launcher.data.IndicScript
import com.paperdesk.launcher.data.MadhwaStore
import com.paperdesk.launcher.data.OsLang

/** Browse screens for hub children that are not full catalogue readers. */
class SectionBrowseFragment : Fragment() {
    private val sectionId: String get() = requireArguments().getString(ARG_SECTION).orEmpty()
    private var lang = IndicScript.Lang.KN
    private var selectedId = ""
    private lateinit var adapter: Adapter
    private var entries: List<Entry> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.page_section_browse, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lang = OsLang.get(requireContext())
        when (sectionId) {
            PLAYLISTS -> CatalogueStore.load(requireContext())
            RECIPES, RANGAVALLI -> MadhwaStore.load(requireContext())
        }
        entries = buildEntries()
        selectedId = savedInstanceState?.getString(STATE_SELECTED)
            ?: entries.firstOrNull()?.id.orEmpty()

        view.findViewById<TextView>(R.id.sectionTitle).text = OsLang.ui(sectionId, lang)
        view.findViewById<TextView>(R.id.sectionSubtitle).text = subtitle()

        adapter = Adapter { select(it) }
        view.findViewById<RecyclerView>(R.id.sectionList).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SectionBrowseFragment.adapter
        }
        adapter.submit(entries, selectedId)
        paintDetail()
        view.findViewById<MaterialButton>(R.id.sectionAction).setOnClickListener { runAction() }
        view.findViewById<MaterialButton>(R.id.sectionOpenContent).setOnClickListener { openEntryInReader() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SELECTED, selectedId)
    }

    private fun select(id: String) {
        selectedId = id
        adapter.submit(entries, selectedId)
        paintDetail()
    }

    private fun paintDetail() {
        val view = view ?: return
        val entry = entries.firstOrNull { it.id == selectedId } ?: entries.firstOrNull()
        view.findViewById<TextView>(R.id.sectionDetailTitle).text = entry?.title.orEmpty()
        view.findViewById<TextView>(R.id.sectionDetailBody).text = entry?.body.orEmpty()
        val openBtn = view.findViewById<MaterialButton>(R.id.sectionOpenContent)
        openBtn.text = OsLang.ui("open_content", lang)
        openBtn.isEnabled = entry != null
        val action = view.findViewById<MaterialButton>(R.id.sectionAction)
        if (entry?.actionLabel.isNullOrBlank()) {
            action.visibility = View.GONE
        } else {
            action.visibility = View.VISIBLE
            action.text = entry?.actionLabel
        }
    }

    private fun openEntryInReader() {
        val entry = entries.firstOrNull { it.id == selectedId } ?: entries.firstOrNull() ?: return
        val body = if (sectionId == PLAYLISTS) {
            val songs = CatalogueStore.inGroup(entry.id)
            if (songs.isNotEmpty()) {
                entry.body + "\n\n" + songs.joinToString("\n") { "• ${CatalogueStore.label(it, lang)}" }
            } else entry.body
        } else {
            entry.body
        }
        startActivity(
            ContentReaderActivity.forInline(
                requireContext(),
                entry.title,
                OsLang.ui(sectionId, lang),
                body
            )
        )
    }

    private fun runAction() {
        val entry = entries.firstOrNull { it.id == selectedId } ?: return
        when (sectionId) {
            PLAYLISTS -> {
                requireContext().getSharedPreferences("paperdesk_reader_hadugalu", 0).edit()
                    .putString("session", entry.id)
                    .putString("item", "")
                    .apply()
                (activity as? LauncherActivity)?.navigateTo("hadugalu")
            }
            RECIPES -> {
                if (entry.file.isNotBlank()) {
                    // Keep selection; body already shown. Optional: open puja catalogue.
                    (activity as? LauncherActivity)?.navigateTo("puja")
                }
            }
        }
    }

    private fun subtitle(): String = when (sectionId) {
        PLAYLISTS -> getString(R.string.section_playlists_subtitle)
        RANGAVALLI -> getString(R.string.section_rangavalli_subtitle)
        RECIPES -> getString(R.string.section_recipes_subtitle)
        else -> ""
    }

    private fun buildEntries(): List<Entry> = when (sectionId) {
        PLAYLISTS -> {
            CatalogueStore.groupsFor("hadugalu").map { group ->
                val count = CatalogueStore.inGroup(group.id).size
                Entry(
                    id = group.id,
                    title = CatalogueStore.groupLabel(group, lang),
                    body = getString(R.string.section_playlist_body, count),
                    actionLabel = getString(R.string.section_open_hadugalu)
                )
            }.ifEmpty {
                listOf(
                    Entry(
                        id = "empty",
                        title = OsLang.ui("hadugalu", lang),
                        body = getString(R.string.section_playlists_empty),
                        actionLabel = getString(R.string.section_open_hadugalu)
                    )
                )
            }
        }
        RANGAVALLI -> listOf(
            Entry(
                id = "meaning",
                title = getString(R.string.rangavalli_meaning_title),
                body = getString(R.string.rangavalli_meaning_body)
            ),
            Entry(
                id = "occasions",
                title = getString(R.string.rangavalli_occasions_title),
                body = getString(R.string.rangavalli_occasions_body)
            ),
            Entry(
                id = "practice",
                title = getString(R.string.rangavalli_practice_title),
                body = getString(R.string.rangavalli_practice_body)
            ),
            Entry(
                id = "packs",
                title = getString(R.string.rangavalli_packs_title),
                body = getString(R.string.rangavalli_packs_body)
            )
        )
        RECIPES -> {
            val festival = MadhwaStore.inCategory("festival")
            val puja = MadhwaStore.inCategory("puja")
            val guides = (festival + puja).filter { item ->
                val hay = "${item.title} ${item.note} ${item.file}".lowercase()
                "festival" in item.file || "ನೈವೇದ್ಯ" in item.note || "naivedya" in hay ||
                    "puja" in item.file || item.file.contains("festival_")
            }.distinctBy { it.id }
            val fromAssets = guides.map { item ->
                Entry(
                    id = item.id,
                    title = MadhwaStore.label(item, lang),
                    body = MadhwaStore.body(requireContext(), item, lang).ifBlank {
                        MadhwaStore.note(item, lang).ifBlank { getString(R.string.section_recipes_fallback) }
                    },
                    actionLabel = getString(R.string.section_open_puja),
                    file = item.file
                )
            }
            listOf(
                Entry(
                    id = "naivedya_intro",
                    title = getString(R.string.recipes_intro_title),
                    body = getString(R.string.recipes_intro_body)
                )
            ) + fromAssets.ifEmpty {
                listOf(
                    Entry(
                        id = "common",
                        title = getString(R.string.recipes_common_title),
                        body = getString(R.string.recipes_common_body),
                        actionLabel = getString(R.string.section_open_puja)
                    )
                )
            }
        }
        else -> emptyList()
    }

    private data class Entry(
        val id: String,
        val title: String,
        val body: String,
        val actionLabel: String = "",
        val file: String = ""
    )

    private class Adapter(private val onSelect: (String) -> Unit) :
        RecyclerView.Adapter<Adapter.VH>() {
        private var items: List<Entry> = emptyList()
        private var selected = ""

        fun submit(next: List<Entry>, selectedId: String) {
            items = next
            selected = selectedId
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_section_row, parent, false)
            return VH(view as MaterialButton)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            val active = item.id == selected
            holder.button.text = item.title
            holder.button.setTextColor(
                holder.button.context.getColor(if (active) R.color.carbon_orange else R.color.carbon_text)
            )
            holder.button.backgroundTintList = ColorStateList.valueOf(
                holder.button.context.getColor(if (active) R.color.carbon_layer_high else R.color.carbon_layer)
            )
            holder.button.setOnClickListener { onSelect(item.id) }
        }

        class VH(val button: MaterialButton) : RecyclerView.ViewHolder(button)
    }

    companion object {
        const val PLAYLISTS = "playlists"
        const val RANGAVALLI = "rangavalli"
        const val RECIPES = "recipes"
        private const val ARG_SECTION = "section"
        private const val STATE_SELECTED = "selected"

        fun create(section: String) = SectionBrowseFragment().apply {
            arguments = Bundle().apply { putString(ARG_SECTION, section) }
        }
    }
}
