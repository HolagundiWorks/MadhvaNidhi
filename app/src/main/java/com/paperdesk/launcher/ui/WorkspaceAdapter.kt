package com.paperdesk.launcher.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.paperdesk.launcher.data.SettingsStore

class WorkspaceAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    private val pages: List<String> = run {
        val settings = SettingsStore(activity)
        settings.pages().ifEmpty { SettingsStore.PAGE_IDS }.filter { id ->
            when (id) {
                "schedule" -> settings.scheduleEnabled()
                "gita" -> settings.gitaEnabled()
                "project" -> settings.projectEnabled()
                "todo" -> settings.todoEnabled()
                "notes" -> settings.notesEnabled()
                "tools" -> settings.calcEnabled()
                else -> true
            }
        }.ifEmpty { listOf("desk") }
    }

    fun pageId(position: Int): String = pages.getOrNull(position) ?: "desk"

    override fun getItemCount(): Int = pages.size

    override fun getItemId(position: Int): Long = pages[position].hashCode().toLong()

    override fun containsItem(itemId: Long): Boolean = pages.any { it.hashCode().toLong() == itemId }

    override fun createFragment(position: Int): Fragment = when (pages.getOrNull(position)) {
        "sandhya" -> ReaderFragment.create(ReaderFragment.SANDHYA)
        "nitya" -> NityaParayanaFragment()
        "mantras" -> ReaderFragment.create(ReaderFragment.MANTRAS)
        "puja" -> ReaderFragment.create(ReaderFragment.PUJA)
        "hadugalu" -> ReaderFragment.create(ReaderFragment.HADUGALU)
        "japa" -> JapaFragment()
        "aayadi" -> AayadiFragment()
        "settings" -> SettingsFragment()
        else -> DeskFragment()
    }
}
