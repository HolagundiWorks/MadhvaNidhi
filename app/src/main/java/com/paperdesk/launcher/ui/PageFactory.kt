package com.paperdesk.launcher.ui

import androidx.fragment.app.Fragment

/** Creates workspace destinations. One fragment at a time — not a ViewPager roster. */
object PageFactory {
    fun create(pageId: String): Fragment = when (pageId) {
        "dashboard" -> DashboardFragment()
        "nityakarma" -> NityaKarmaFragment.create(NityaKarmaFragment.TAB_SANDHYA)
        "puja" -> PujaFragment()
        "grantha" -> GranthaFragment()
        "samskruti", "sangeeta" -> GranthaFragment().apply {
            arguments = android.os.Bundle().apply { putString("group", pageId) }
        }
        "nitya" -> NityaKarmaFragment.create(NityaKarmaFragment.TAB_NITYA)
        "stotra" -> ReaderFragment.create(ReaderFragment.STOTRA)
        "suktas" -> ReaderFragment.create(ReaderFragment.SUKTAS)
        "mantras" -> ReaderFragment.create(ReaderFragment.MANTRAS)
        "namavali" -> ReaderFragment.create(ReaderFragment.NAMAVALI)
        "hadugalu" -> ReaderFragment.create(ReaderFragment.HADUGALU)
        "playlists" -> SectionBrowseFragment.create(SectionBrowseFragment.PLAYLISTS)
        "aayadi" -> AayadiFragment()
        "rangavalli" -> SectionBrowseFragment.create(SectionBrowseFragment.RANGAVALLI)
        "recipes" -> SectionBrowseFragment.create(SectionBrowseFragment.RECIPES)
        "user" -> UserProfileFragment()
        "settings" -> SettingsFragment()
        "desk" -> DeskFragment()
        else -> DeskFragment()
    }
}
