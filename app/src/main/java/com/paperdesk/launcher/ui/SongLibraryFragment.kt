package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.CatalogueClassification
import com.paperdesk.launcher.data.CatalogueStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Songs have their own collections; Favorites replaces the old playlist destination. */
class SongLibraryFragment : Fragment(R.layout.page_song_library) {
    private var selected = "dasara_padagalu"
    private val keys = mutableListOf<String>()
    private var extraGroups = emptyList<String>()
    fun selectCollection(id: String) {
        selected = if (id == "favorites" || id == "playlists") "favorites" else "dasara_padagalu"
        val index = keys.indexOf(selected)
        if (index >= 0) view?.findViewById<TabLayout>(R.id.songCollectionTabs)?.getTabAt(index)?.select()
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        selected = savedInstanceState?.getString("collection") ?: arguments?.getString("collection") ?: selected
        viewLifecycleOwner.lifecycleScope.launch {
            val app = requireContext().applicationContext
            extraGroups = withContext(Dispatchers.IO) {
                CatalogueStore.load(app)
                CatalogueStore.groupsFor("hadugalu").filter { it.id !in CatalogueClassification.MAIN_SONG_GROUPS }.map { it.id }
            }
            keys.clear(); keys.addAll(CatalogueClassification.MAIN_SONG_GROUPS); keys.add("favorites")
            if (extraGroups.isNotEmpty()) keys.add("other")
            val tabs = view.findViewById<TabLayout>(R.id.songCollectionTabs)
            val names = mapOf("dasara_padagalu" to "ದಾಸರ ಪದಗಳು", "suladhigalu" to "ಸುಳಾದಿಗಳು", "ugabhogagalu" to "ಉಗಾಭೋಗಗಳು", "favorites" to "ಮೆಚ್ಚಿನವು", "other" to "ಇತರೆ ಹಾಡುಗಳು")
            keys.forEach { key -> tabs.addTab(tabs.newTab().setText(names[key]).setTag(key),false) }
            tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) { selected=tab.tag as String; showCollection() }
                override fun onTabUnselected(tab: TabLayout.Tab) = Unit
                override fun onTabReselected(tab: TabLayout.Tab) = Unit
            })
            tabs.getTabAt(keys.indexOf(selected).coerceAtLeast(0))?.select()
        }
    }
    private fun showCollection() {
        val groups = when(selected) { "favorites" -> null; "other" -> extraGroups; else -> listOf(selected) }
        childFragmentManager.beginTransaction().replace(R.id.songCollectionHost,
            CatalogueListFragment.create("hadugalu",groups,selected=="favorites")).commitNow()
    }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState);outState.putString("collection",selected) }
    companion object {
        fun create(favorites: Boolean=false)=SongLibraryFragment().apply { arguments=Bundle().apply { putString("collection",if(favorites)"favorites" else "dasara_padagalu") } }
    }
}
