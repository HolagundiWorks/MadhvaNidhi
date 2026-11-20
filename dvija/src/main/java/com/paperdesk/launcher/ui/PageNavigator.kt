package com.paperdesk.launcher.ui

/**
 * What a screen may ask its host to do. Practice and library screens depend on this interface,
 * not on the Madhva Nidhi shell, so they can run in another host (Madhva Dvija).
 */
interface PageNavigator {
    fun navigateTo(pageId: String)
    fun openNavigation()
}
