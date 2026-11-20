package com.paperdesk.launcher.ui

import androidx.fragment.app.Fragment

/** Creates workspace destinations. One fragment at a time — not a ViewPager roster. */
object PageFactory {
    fun create(pageId: String): Fragment = when (pageId) {
        "home" -> NidhiHomeFragment()
        "puja" -> PujaListFragment()
        "grantha" -> GranthaFragment()
        "samskruti" -> GranthaFragment().apply {
            arguments = android.os.Bundle().apply { putString("group", pageId) }
        }
        "stotra" -> CatalogueListFragment.create("stotra")
        "suktas" -> CatalogueListFragment.create("suktas")
        "mantras" -> CatalogueListFragment.create("mantras")
        "namavali" -> CatalogueListFragment.create("namavali")
        "wiki" -> WikiFragment()
        "sangeeta", "hadugalu" -> SongLibraryFragment.create()
        "favorites", "playlists" -> SongLibraryFragment.create(favorites = true)
        "rangavalli" -> RangavalliFragment()
        "recipes" -> NaivedyaFragment()
        "settings" -> SettingsFragment()
        "desk" -> DeskFragment()
        else -> DeskFragment()
    }
}
