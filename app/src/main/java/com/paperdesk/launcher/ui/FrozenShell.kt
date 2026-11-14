package com.paperdesk.launcher.ui

import android.content.res.ColorStateList
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButton
import com.paperdesk.launcher.DeskLock
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.OsLang

/** Material 3 side rail and overlay panel. Primary destinations match the HTML shell sim. */
class FrozenShell(private val activity: LauncherActivity) {
    private val panel: View = activity.findViewById(R.id.sidePanel)
    private val backdrop: View = activity.findViewById(R.id.sidePanelBackdrop)
    private var lastRailKey = ""
    private var lastPanelKey = ""
    private var lastActiveRail = ""
    private var lastCurrent = ""
    private var built = false

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
        if (value) {
            if (!built) bind()
            activity.findViewById<View>(R.id.closeSidePanel).requestFocus()
        }
    }

    fun bind() {
        val current = activity.currentPageId()
        val activeRail = railForPage(current)
        val density = activity.resources.displayMetrics.density
        // Expanded panel matches mockup: labeled primary rail only (hubs open children).
        val panelEntries = ShellHubFragment.RAIL_IDS

        bindHost(
            hostId = R.id.railDestinations,
            entries = ShellHubFragment.RAIL_IDS,
            key = ShellHubFragment.RAIL_IDS.joinToString() + "|${activity.resources.getDimensionPixelSize(R.dimen.shell_rail_item_height)}",
            lastKey = lastRailKey,
            labeled = false,
            current = current,
            activeRail = activeRail,
            density = density
        ).also { lastRailKey = it }

        bindHost(
            hostId = R.id.panelDestinations,
            entries = panelEntries,
            key = panelEntries.joinToString() + "|${activity.resources.getDimensionPixelSize(R.dimen.shell_rail_item_height)}",
            lastKey = lastPanelKey,
            labeled = true,
            current = current,
            activeRail = activeRail,
            density = density
        ).also { lastPanelKey = it }

        lastActiveRail = activeRail
        lastCurrent = current
        built = true
    }

    private fun bindHost(
        hostId: Int,
        entries: List<String>,
        key: String,
        lastKey: String,
        labeled: Boolean,
        current: String,
        activeRail: String,
        density: Float
    ): String {
        val host = activity.findViewById<LinearLayout>(hostId)
        val structureChanged = key != lastKey || host.childCount != entries.size
        if (structureChanged) {
            host.removeAllViews()
            val row = activity.resources.getDimensionPixelSize(R.dimen.shell_rail_item_height)
            for (id in entries) {
                val lp = if (labeled) {
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, row)
                } else {
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                }
                host.addView(createButton(id, labeled, density), lp)
            }
        } else if (current == lastCurrent && activeRail == lastActiveRail) {
            return key
        }
        for (i in 0 until host.childCount) {
            val button = host.getChildAt(i) as? MaterialButton ?: continue
            val id = button.tag as? String ?: continue
            val active = activeRail == id || current == id
            paintButton(button, active)
            button.isEnabled = !DeskLock.isLocked(activity)
        }
        return key
    }

    private fun createButton(id: String, labeled: Boolean, density: Float): MaterialButton {
        val label = OsLang.ui(id, OsLang.get(activity))
        return MaterialButton(
            activity,
            null,
            com.google.android.material.R.attr.borderlessButtonStyle
        ).apply {
            tag = id
            text = if (labeled) label else ""
            contentDescription = label
            setIconResource(iconFor(id))
            iconSize = activity.resources.getDimensionPixelSize(R.dimen.shell_icon_size)
            iconPadding = if (!labeled) 0 else activity.resources.getDimensionPixelSize(R.dimen.carbon_spacing_05)
            iconGravity = if (!labeled) MaterialButton.ICON_GRAVITY_TEXT_START else MaterialButton.ICON_GRAVITY_START
            minimumWidth = if (!labeled) (48 * density).toInt() else 0
            minimumHeight = (48 * density).toInt()
            insetTop = 0
            insetBottom = 0
            val pad = if (labeled) activity.resources.getDimensionPixelSize(R.dimen.carbon_spacing_05) else 0
            setPadding(pad, 0, pad, 0)
            cornerRadius = (12 * density).toInt()
            rippleColor = ColorStateList.valueOf(activity.getColor(R.color.md_theme_hover))
            backgroundTintList = null
            setBackgroundResource(if (labeled) R.drawable.bg_rail_item else R.drawable.bg_rail_item_bottom)
            maxLines = 1
            gravity = if (labeled) {
                android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START
            } else {
                android.view.Gravity.CENTER
            }
            if (labeled) {
                androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                    this, 10, 14, 1, android.util.TypedValue.COMPLEX_UNIT_SP
                )
            }
            androidx.core.view.ViewCompat.setTooltipText(this, label)
            setOnClickListener {
                close()
                activity.navigateTo(id)
            }
            paintButton(this, false)
        }
    }

    private fun paintButton(button: MaterialButton, active: Boolean) {
        button.isSelected = active
        val color = activity.getColor(
            if (active) R.color.md_theme_onSecondaryContainer else R.color.md_theme_onSurfaceVariant
        )
        button.iconTint = ColorStateList.valueOf(color)
        button.setTextColor(color)
    }

    companion object {
        fun railForPage(pageId: String): String = when (pageId) {
            "dashboard" -> "dashboard"
            "desk" -> "desk"
            "sandhya", "japa", "nitya", "nityakarma" -> "nityakarma"
            "puja" -> "puja"
            "stotra", "suktas", "mantras", "namavali", "grantha" -> "grantha"
            "hadugalu", "playlists", "sangeeta" -> "sangeeta"
            "aayadi", "rangavalli", "recipes", "samskruti" -> "samskruti"
            "user", "settings" -> "settings"
            else -> pageId
        }

        fun iconFor(id: String): Int = when (id) {
            "dashboard" -> R.drawable.ic_dashboard
            "desk" -> R.drawable.ic_calendar
            "nityakarma" -> R.drawable.ic_time_plot
            "sandhya" -> R.drawable.ic_sun
            "japa" -> R.drawable.ic_renew
            "puja" -> R.drawable.ic_transform_instructions
            "nitya" -> R.drawable.ic_book
            "grantha" -> R.drawable.ic_document_multiple_01
            "stotra", "suktas", "mantras", "namavali" -> R.drawable.ic_book
            "sangeeta", "hadugalu", "playlists" -> R.drawable.ic_music
            "samskruti", "aayadi", "rangavalli", "recipes" -> R.drawable.ic_mac_command
            "user" -> R.drawable.ic_user
            "settings" -> R.drawable.ic_settings
            else -> R.drawable.ic_information
        }
    }
}
