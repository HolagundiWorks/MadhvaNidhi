package com.paperdesk.launcher.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Portrait title lists; bodies open only in the landscape reader. */
class CatalogueListFragment : Fragment(R.layout.page_catalogue_list) {
    private val page get() = arguments?.getString("page") ?: "stotra"
    private val songs get() = page == "hadugalu"
    private val favoritesOnly get() = arguments?.getBoolean("favorites") == true
    private var query = ""
    private var all = emptyList<CatalogueItem>()
    private lateinit var favorites: SongFavoritesStore
    private lateinit var adapter: ListAdapter
    private var loaded = false
    private var lang = IndicScript.Lang.KN
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        query = savedInstanceState?.getString("query").orEmpty()
        lang = OsLang.get(requireContext())
        favorites = SongFavoritesStore(requireContext())
        adapter = ListAdapter(songs, favorites, { open(it) }, { applyFilter() })
        view.findViewById<RecyclerView>(R.id.catalogueList).apply { layoutManager=LinearLayoutManager(context); adapter=this@CatalogueListFragment.adapter }
        view.findViewById<TextInputEditText>(R.id.catalogueSearch).apply {
            setText(query)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { query=s?.toString().orEmpty(); applyFilter() }
                override fun afterTextChanged(s: Editable?) = Unit
            })
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val context = requireContext().applicationContext
            all = kotlinx.coroutines.withContext(Dispatchers.IO) {
                CatalogueStore.load(context)
                val requested = arguments?.getStringArrayList("groups")?.toSet()
                val groups = if (songs) CatalogueStore.groupsFor(page) else CatalogueStore.groupsForSubpage(page,page)
                groups.filter { requested == null || it.id in requested }.flatMap { group ->
                    if (songs) CatalogueStore.inGroup(group.id) else CatalogueStore.inGroup(group.id,page)
                }.distinctBy { it.id }
            }
            loaded = true
            applyFilter()
        }
    }
    override fun onResume() { super.onResume(); if (loaded) applyFilter() }
    private fun applyFilter() {
        val root = view ?: return
        if (!::adapter.isInitialized) return
        val q = query.trim()
        val shown = all.filter { (!favoritesOnly || favorites.contains(it.id)) &&
            (q.isBlank() || listOf(it.kn,it.sa,it.en,it.author).any { field -> field.contains(q,true) }) }
        adapter.submit(shown,lang)
        root.findViewById<TextView>(R.id.catalogueListCount).text = "${shown.size} ${if (songs) "ಹಾಡುಗಳು" else "ಪಠ್ಯಗಳು"} · ಆಯ್ಕೆಮಾಡಿ ಓದಿ"
        root.findViewById<TextView>(R.id.catalogueListEmpty).apply {
            visibility = if (loaded && shown.isEmpty()) View.VISIBLE else View.GONE
            text = if (favoritesOnly && q.isBlank()) "ಮೆಚ್ಚಿನ ಹಾಡುಗಳಿಲ್ಲ. ಹಾಡಿನ ಪಕ್ಕದ ನಕ್ಷತ್ರ ಒತ್ತಿ ಸೇರಿಸಿ." else "ಹೊಂದುವ ಪಠ್ಯಗಳು ಲಭ್ಯವಿಲ್ಲ."
        }
    }
    private fun open(item: CatalogueItem) {
        startActivity(ContentReaderActivity.forCatalogue(requireContext(),page,item.id,if (songs) null else page))
    }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); outState.putString("query",query) }
    private class ListAdapter(val songs: Boolean,val favorites: SongFavoritesStore,val open: (CatalogueItem)->Unit,val changed: ()->Unit) : RecyclerView.Adapter<ListAdapter.Holder>() {
        private var items=emptyList<CatalogueItem>();private var lang=IndicScript.Lang.KN
        class Holder(val card:MaterialCardView):RecyclerView.ViewHolder(card)
        fun submit(next:List<CatalogueItem>,language:IndicScript.Lang){items=next;lang=language;notifyDataSetChanged()}
        override fun getItemCount()=items.size
        override fun onCreateViewHolder(parent:ViewGroup,type:Int)=Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_catalogue_list,parent,false) as MaterialCardView)
        override fun onBindViewHolder(holder:Holder,position:Int){
            val item=items[position];val title=CatalogueStore.label(item,lang)
            holder.card.findViewById<TextView>(R.id.catalogueRowTitle).text=title
            holder.card.findViewById<TextView>(R.id.catalogueRowMeta).apply{text=item.author;visibility=if(item.author.isBlank())View.GONE else View.VISIBLE}
            holder.card.contentDescription="$title. ಅಡ್ಡ ಪರದೆಯಲ್ಲಿ ಓದಿ"
            holder.card.setOnClickListener{open(item)}
            holder.card.findViewById<MaterialButton>(R.id.catalogueRowFavorite).apply{
                visibility=if(songs)View.VISIBLE else View.GONE
                val selected=favorites.contains(item.id)
                setIconResource(if(selected)R.drawable.ic_favorite_filled else R.drawable.ic_favorite_outline)
                iconTint=android.content.res.ColorStateList.valueOf(context.getColor(if(selected)R.color.md_theme_primary else R.color.md_theme_onSurfaceVariant))
                contentDescription=(if(selected)"ಮೆಚ್ಚಿನ ಪಟ್ಟಿಯಿಂದ ತೆಗೆದುಹಾಕಿ: " else "ಮೆಚ್ಚಿನ ಪಟ್ಟಿಗೆ ಸೇರಿಸಿ: ")+title
                androidx.core.view.ViewCompat.setStateDescription(this,if(selected)"Favorite" else "Not favorite")
                setOnClickListener{favorites.set(item.id,!favorites.contains(item.id));changed()}
            }
        }
    }
    companion object {
        fun create(page:String,groups:List<String>?=null,favorites:Boolean=false)=CatalogueListFragment().apply{
            arguments=Bundle().apply{putString("page",page);if(groups!=null)putStringArrayList("groups",ArrayList(groups));putBoolean("favorites",favorites)}
        }
    }
}
