package com.paperdesk.launcher.ui

import androidx.fragment.app.Fragment

/** Creates workspace destinations. One fragment at a time — not a ViewPager roster. */
object PageFactory {
    fun create(pageId: String): Fragment = when (pageId) {
        "dashboard" -> DashboardFragment()
        "nityakarma" -> NityaKarmaFragment.create(NityaKarmaFragment.TAB_SANDHYA)
        "puja" -> ReaderFragment.create(ReaderFragment.PUJA)
        "grantha" -> GranthaFragment()
        "samskruti" -> GranthaFragment().apply {
            arguments = android.os.Bundle().apply { putString("group", pageId) }
        }
        "nitya" -> NityaKarmaFragment.create(NityaKarmaFragment.TAB_NITYA)
        "stotra" -> CatalogueListFragment.create("stotra")
        "suktas" -> CatalogueListFragment.create("suktas")
        "mantras" -> CatalogueListFragment.create("mantras")
        "namavali" -> CatalogueListFragment.create("namavali")
        "wiki" -> WikiFragment()
        "sangeeta", "hadugalu" -> SongLibraryFragment.create()
        "favorites", "playlists" -> SongLibraryFragment.create(favorites = true)
        "aayadi" -> AayadiFragment()
        "rangavalli" -> RangavalliFragment()
        "recipes" -> NaivedyaFragment()
        "user" -> UserProfileFragment()
        "settings" -> SettingsFragment()
        "desk" -> DeskFragment()
        else -> DeskFragment()
    }
}
