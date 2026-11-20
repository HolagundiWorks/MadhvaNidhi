package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.OsLang

/**
 * Madhva Dvija host: one page at a time, with a bottom bar to the practice screens. The dashboard is the home page.
 * The side panel of Madhva Nidhi is not ported; the dashboard's menu button has no effect yet (see docs/split/AUDIT.md).
 */
class DvijaActivity : AppCompatActivity(), PageNavigator {

    private var currentPage = HOME

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dvija)
        currentPage = savedInstanceState?.getString(STATE_PAGE) ?: HOME
        bindBar()
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

    private fun bindBar() {
        val lang = OsLang.get(this)
        val buttons = linkedMapOf(
            R.id.navDashboard to "dashboard",
            R.id.navSandhya to "sandhya",
            R.id.navJapa to "japa",
            R.id.navPuja to "puja",
            R.id.navProfile to "user",
            R.id.navSettings to "settings"
        )
        for ((id, page) in buttons) {
            findViewById<MaterialButton>(id).apply {
                text = OsLang.ui(page, lang)
                setOnClickListener { show(page) }
            }
        }
    }

    /** The selected page's button is at full strength; the others are dimmed. */
    private fun highlight(page: String) {
        val buttons = mapOf(
            "dashboard" to R.id.navDashboard,
            "sandhya" to R.id.navSandhya,
            "nitya" to R.id.navSandhya,
            "japa" to R.id.navJapa,
            "puja" to R.id.navPuja,
            "user" to R.id.navProfile,
            "settings" to R.id.navSettings
        )
        val selected = buttons[page]
        for (id in listOf(R.id.navDashboard, R.id.navSandhya, R.id.navJapa, R.id.navPuja, R.id.navProfile, R.id.navSettings)) {
            findViewById<View>(id).alpha = if (id == selected) 1f else 0.55f
        }
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
