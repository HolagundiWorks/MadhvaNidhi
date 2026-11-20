package com.paperdesk.launcher.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.paperdesk.launcher.R

/**
 * Madhva Dvija host: one page at a time. The dashboard is the home page; practice screens open from it.
 * Navigation chrome (dock and side panel) is not yet ported from Madhva Nidhi; see docs/split/AUDIT.md, Phase 3.
 */
class DvijaActivity : AppCompatActivity(), PageNavigator {

    private var currentPage = HOME

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dvija)
        currentPage = savedInstanceState?.getString(STATE_PAGE) ?: HOME
        if (savedInstanceState == null) show(HOME)
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

    private fun show(pageId: String) {
        val page = canonical(pageId)
        currentPage = page
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.pageHost, fragmentFor(pageId), page)
            .commitAllowingStateLoss()
    }

    private fun fragmentFor(pageId: String): Fragment = when (canonical(pageId)) {
        "sandhya" -> com.paperdesk.launcher.ui.NityaKarmaFragment.create(com.paperdesk.launcher.ui.NityaKarmaFragment.TAB_SANDHYA)
        "japa" -> com.paperdesk.launcher.ui.NityaKarmaFragment.create(com.paperdesk.launcher.ui.NityaKarmaFragment.TAB_JAPA)
        "nitya" -> com.paperdesk.launcher.ui.NityaKarmaFragment.create(com.paperdesk.launcher.ui.NityaKarmaFragment.TAB_NITYA)
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
