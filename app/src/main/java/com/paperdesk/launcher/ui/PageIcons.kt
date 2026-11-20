package com.paperdesk.launcher.ui

import com.paperdesk.launcher.R

/** Icon for each shell page or shortcut. Shared by the shell and by practice screens, which must not depend on the shell class. */
object PageIcons {
    fun iconFor(id: String): Int = when (id) {
        "dashboard" -> R.drawable.ic_nav_home
        "desk" -> R.drawable.ic_nav_calendar
        "nityakarma" -> R.drawable.ic_nav_daily
        "sandhya" -> R.drawable.ic_sun
        "japa" -> R.drawable.ic_japa_eda
        "puja" -> R.drawable.ic_nav_puja
        "nitya" -> R.drawable.ic_nav_library
        "grantha" -> R.drawable.ic_nav_library
        "wiki" -> R.drawable.ic_book
        "stotra", "suktas", "mantras", "namavali" -> R.drawable.ic_nav_library
        "sangeeta", "hadugalu", "playlists", "favorites" -> R.drawable.ic_nav_songs
        "samskruti" -> R.drawable.ic_nav_culture
        "rangavalli" -> R.drawable.ic_dashboard
        "recipes" -> R.drawable.ic_nav_puja
        "user" -> R.drawable.ic_user
        "settings" -> R.drawable.ic_nav_settings
        else -> R.drawable.ic_information
    }
}
