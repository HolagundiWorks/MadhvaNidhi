package com.paperdesk.launcher.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.paperdesk.launcher.data.SettingsStore

class WorkspaceAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    private val pages: List<String> = run {
        val settings = SettingsStore(activity)
        settings.pages().ifEmpty { SettingsStore.PAGE_IDS }.ifEmpty { listOf("dashboard") }
    }

    fun pageId(position: Int): String = pages.getOrNull(position) ?: "dashboard"

    fun indexOf(pageId: String): Int {
        val direct = pages.indexOf(pageId)
        if (direct >= 0) return direct
        val canonical = SettingsStore.canonicalizePageId(pageId)
        return pages.indexOf(canonical)
    }

    override fun getItemCount(): Int = pages.size

    // Prefer stable PAGE_IDS ordinals: String.hashCode collides for "nitya" / "nityakarma".
    override fun getItemId(position: Int): Long {
        val id = pages[position]
        val ordinal = SettingsStore.PAGE_IDS.indexOf(id)
        return if (ordinal >= 0) ordinal.toLong() else id.hashCode().toLong()
    }

    override fun containsItem(itemId: Long): Boolean =
        pages.indices.any { getItemId(it) == itemId }

    override fun createFragment(position: Int): Fragment = when (val id = pages.getOrNull(position)) {
        "dashboard" -> DashboardFragment()
        "nityakarma" -> NityaKarmaFragment.create(NityaKarmaFragment.TAB_SANDHYA)
        "grantha", "sangeeta", "samskruti" -> ShellHubFragment.create(id)
        "nitya" -> NityaParayanaFragment()
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
