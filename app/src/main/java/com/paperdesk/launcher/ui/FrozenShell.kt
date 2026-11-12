package com.paperdesk.launcher.ui

import android.content.res.ColorStateList
import android.view.View
import android.widget.LinearLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.paperdesk.launcher.DeskLock
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.OsLang

/** Carbon side rail and overlay panel. Primary destinations match the HTML shell sim. */
class FrozenShell(private val activity: LauncherActivity, private val pager: ViewPager2) {
    private val panel: View = activity.findViewById(R.id.sidePanel)
    private val backdrop: View = activity.findViewById(R.id.sidePanelBackdrop)

    init {
        activity.findViewById<View>(R.id.sidePanelToggle).setOnClickListener { open(true) }
        activity.findViewById<View>(R.id.closeSidePanel).setOnClickListener { open(false) }
        backdrop.setOnClickListener { open(false) }
    }

    fun close(): Boolean {
        val wasOpen = panel.visibility == View.VISIBLE
        open(false)
        return wasOpen
    }

    private fun open(value: Boolean) {
        panel.visibility = if (value) View.VISIBLE else View.GONE
        backdrop.visibility = panel.visibility
        activity.findViewById<View>(R.id.sidePanelToggle).contentDescription =
            if (value) "Close navigation" else "Open navigation"
        if (value) activity.findViewById<View>(R.id.closeSidePanel).requestFocus()
    }

    fun bind() {
        val adapter = pager.adapter as WorkspaceAdapter
        val current = adapter.pageId(pager.currentItem)
        val activeRail = railForPage(current)
        val density = activity.resources.displayMetrics.density
        val panelEntries = buildList {
            addAll(ShellHubFragment.RAIL_IDS)
            addAll(
                listOf(
                    "sandhya", "japa", "puja",
                    "nitya", "stotra", "suktas", "mantras", "namavali",
                    "hadugalu", "playlists",
                    "aayadi", "rangavalli", "recipes"
                )
            )
        }.distinct().filter { id ->
            id in ShellHubFragment.RAIL_IDS || (0 until adapter.itemCount).any { adapter.pageId(it) == id }
        }

        for ((hostId, entries) in listOf(
            R.id.railDestinations to ShellHubFragment.RAIL_IDS,
            R.id.panelDestinations to panelEntries
        )) {
            val host = activity.findViewById<LinearLayout>(hostId)
            host.removeAllViews()
            for (id in entries) {
                if (hostId == R.id.panelDestinations && id !in ShellHubFragment.RAIL_IDS) {
                    if ((0 until adapter.itemCount).none { adapter.pageId(it) == id }) continue
                }
                val icon = iconFor(id)
                val label = OsLang.ui(id, OsLang.get(activity))
                val button = MaterialButton(
                    activity,
                    null,
                    com.google.android.material.R.attr.borderlessButtonStyle
                ).apply {
                    text = if (hostId == R.id.panelDestinations) label else ""
                    contentDescription = label
                    setIconResource(icon)
                    iconSize = (24 * density).toInt()
                    iconPadding = if (text.isEmpty()) 0 else (12 * density).toInt()
                    iconGravity = if (text.isEmpty()) MaterialButton.ICON_GRAVITY_TEXT_START else MaterialButton.ICON_GRAVITY_START
                    minimumWidth = 0
                    minimumHeight = 0
                    insetTop = 0
                    insetBottom = 0
                    setPadding((8 * density).toInt(), 0, (8 * density).toInt(), 0)
                    val active = if (hostId == R.id.railDestinations) activeRail == id else current == id
                    val color = activity.getColor(
                        if (active) R.color.carbon_interactive else R.color.carbon_icon_secondary
                    )
                    iconTint = ColorStateList.valueOf(color)
                    setTextColor(
                        activity.getColor(
                            if (active) R.color.carbon_text_primary else R.color.carbon_text_secondary
                        )
                    )
                    backgroundTintList = ColorStateList.valueOf(
                        if (active) activity.getColor(R.color.carbon_layer_selected)
                        else android.graphics.Color.TRANSPARENT
                    )
                    cornerRadius = 0
                    rippleColor = ColorStateList.valueOf(activity.getColor(R.color.carbon_background_hover))
                    maxLines = 1
                    androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                        this, 10, 14, 1, android.util.TypedValue.COMPLEX_UNIT_SP
                    )
                    isEnabled = !DeskLock.isLocked(activity)
                    androidx.core.view.ViewCompat.setTooltipText(this, label)
                    setOnClickListener {
                        close()
                        activity.navigateTo(id)
                    }
                }
                host.addView(button, LinearLayout.LayoutParams(-1, (48 * density).toInt()))
            }
        }
    }

    companion object {
        fun railForPage(pageId: String): String = when (pageId) {
            "dashboard" -> "dashboard"
            "desk" -> "desk"
            "sandhya", "japa", "puja", "nityakarma" -> "nityakarma"
            "nitya", "stotra", "suktas", "mantras", "namavali", "grantha" -> "grantha"
            "hadugalu", "playlists", "sangeeta" -> "sangeeta"
            "aayadi", "rangavalli", "recipes", "samskruti" -> "samskruti"
            "user" -> "user"
            "settings" -> "settings"
            else -> pageId
        }

        /** Carbon Design System icons only (32px paths). */
        fun iconFor(id: String): Int = when (id) {
            "dashboard" -> R.drawable.ic_dashboard
            "desk" -> R.drawable.ic_calendar
            "nityakarma", "sandhya" -> R.drawable.ic_sun
            "japa" -> R.drawable.ic_renew
            "grantha", "nitya", "stotra", "suktas", "mantras", "namavali", "puja" -> R.drawable.ic_book
            "sangeeta", "hadugalu", "playlists" -> R.drawable.ic_music
            "samskruti", "aayadi", "rangavalli", "recipes" -> R.drawable.ic_image
            "user" -> R.drawable.ic_user
            "settings" -> R.drawable.ic_settings
            else -> R.drawable.ic_information
        }
    }
}
