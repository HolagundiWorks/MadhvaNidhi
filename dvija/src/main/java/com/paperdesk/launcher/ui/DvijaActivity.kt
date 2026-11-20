package com.paperdesk.launcher.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.OsLang

/**
 * Madhva Dvija host: one page at a time. The floating dock switches between the practice screens; the raised
 * centre button is the home (dashboard) page.
 */
class DvijaActivity : AppCompatActivity(), PageNavigator {

    private var currentPage = HOME

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dvija)
        currentPage = savedInstanceState?.getString(STATE_PAGE) ?: HOME
        bindDock()
        if (savedInstanceState == null) show(HOME) else highlight(currentPage)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_PAGE, currentPage)
    }

    override fun navigateTo(pageId: String) = show(pageId)

    /** The side panel is not ported yet, so the dashboard's menu button has no effect for now. */
    override fun openNavigation() = Unit

    override fun onBackPressed() {
        if (currentPage != HOME) show(HOME) else super.onBackPressed()
    }

    /** Each dock icon names its page for screen readers and shows a tooltip. */
    private fun bindDock() {
        val lang = OsLang.get(this)
        val icons = linkedMapOf(
            R.id.dockSandhya to "sandhya",
            R.id.dockJapa to "japa",
            R.id.dockPuja to "puja",
            R.id.dockProfile to "user",
            R.id.dockSettings to "settings"
        )
        for ((id, page) in icons) {
            findViewById<ImageButton>(id).apply {
                contentDescription = OsLang.ui(page, lang)
                tooltipText = OsLang.ui(page, lang)
                setOnClickListener { show(page) }
            }
        }
        findViewById<View>(R.id.dockHome).apply {
            contentDescription = OsLang.ui("dashboard", lang)
            tooltipText = OsLang.ui("dashboard", lang)
            setOnClickListener { show(HOME) }
        }
    }

    /** The current page's icon is in the primary colour; the others are muted. */
    private fun highlight(page: String) {
        val selected = when (page) {
            "sandhya", "nitya", "nityakarma" -> R.id.dockSandhya
            "japa" -> R.id.dockJapa
            "puja" -> R.id.dockPuja
            "user" -> R.id.dockProfile
            "settings" -> R.id.dockSettings
            else -> null
        }
        val primary = ContextCompat.getColor(this, R.color.md_theme_primary)
        val muted = ContextCompat.getColor(this, R.color.md_theme_onSurfaceVariant)
        for (id in listOf(R.id.dockSandhya, R.id.dockJapa, R.id.dockPuja, R.id.dockProfile, R.id.dockSettings)) {
            findViewById<ImageButton>(id).imageTintList = ColorStateList.valueOf(if (id == selected) primary else muted)
        }
        findViewById<View>(R.id.dockHomeIcon).alpha = if (selected == null) 1f else 0.85f
    }

    private fun show(pageId: String) {
        val page = canonical(pageId)
        currentPage = page
        highlight(page)
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.pageHost, fragmentFor(page), page)
            .commitAllowingStateLoss()
    }

    private fun fragmentFor(page: String): Fragment = when (page) {
        "sandhya", "nityakarma" -> NityaKarmaFragment.create(NityaKarmaFragment.TAB_SANDHYA)
        "japa" -> NityaKarmaFragment.create(NityaKarmaFragment.TAB_JAPA)
        "nitya" -> NityaKarmaFragment.create(NityaKarmaFragment.TAB_NITYA)
        "puja" -> PujaFragment()
        "user" -> UserProfileFragment()
        "settings" -> DashboardSettingsFragment()
        else -> DashboardFragment()
    }

    /** Pages that Dvija does not have collapse into the dashboard. Page ids follow the shared settings names. */
    private fun canonical(pageId: String): String = when (pageId) {
        "dashboard", "sandhya", "japa", "nitya", "nityakarma", "puja", "user", "settings" -> pageId
        else -> HOME
    }

    companion object {
        private const val HOME = "dashboard"
        private const val STATE_PAGE = "dvijaPage"
    }
}
