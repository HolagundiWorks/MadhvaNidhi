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

    fun openPanel() = open(true)

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
            paintButton(button, active, labeled)
            button.isEnabled = !DeskLock.isLocked(activity)
        }
        if (!labeled) fitRail(host, activeRail, density)
        return key
    }

    /** Shows only the dock items that fit whole; the active destination always stays visible. */
    private fun fitRail(host: LinearLayout, activeId: String, density: Float) {
        val apply = Runnable {
            val avail = host.width - host.paddingStart - host.paddingEnd
            val total = host.childCount
            if (avail <= 0 || total == 0) return@Runnable
            val minSlot = (40 * density).toInt()
            val fit = (avail / minSlot).coerceIn(1, total)
            val activeIdx = (0 until total).firstOrNull { (host.getChildAt(it) as? MaterialButton)?.tag == activeId } ?: -1
            val visible = (0 until fit).toMutableSet()
            if (activeIdx >= fit) { visible.remove(fit - 1); visible.add(activeIdx) }
            for (i in 0 until total) {
                host.getChildAt(i).visibility = if (i in visible) View.VISIBLE else View.GONE
            }
        }
        host.post(apply)
        if (host.getTag(R.id.railFitListener) == null) {
            host.setTag(R.id.railFitListener, true)
            host.addOnLayoutChangeListener { _, l, _, r, _, ol, _, or, _ -> if (r - l != or - ol) host.post(apply) }
        }
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
            iconPadding = if (!labeled) 0 else activity.resources.getDimensionPixelSize(R.dimen.ds_space_4)
            iconGravity = if (!labeled) MaterialButton.ICON_GRAVITY_TEXT_START else MaterialButton.ICON_GRAVITY_START
            minWidth = 0
            minHeight = 0
            stateListAnimator = null
            elevation = 0f
            minimumWidth = if (!labeled) (40 * density).toInt() else 0
            minimumHeight = (48 * density).toInt()
            insetTop = 0
            insetBottom = 0
            val pad = if (labeled) activity.resources.getDimensionPixelSize(R.dimen.ds_space_4) else 0
            setPadding(pad, 0, pad, 0)
            cornerRadius = if (labeled) (12 * density).toInt() else (20 * density).toInt()  // circle ripple for rail icons
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
            paintButton(this, false, labeled)
        }
    }

    private fun paintButton(button: MaterialButton, active: Boolean, labeled: Boolean) {
        button.isSelected = active
        androidx.core.view.ViewCompat.setStateDescription(button, if (active) "Current destination" else null)
        val color = activity.getColor(
            if (active && !labeled) R.color.md_theme_onPrimary
            else if (active) R.color.md_theme_onSecondaryContainer else R.color.md_theme_onSurfaceVariant
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
            "stotra", "suktas", "mantras", "namavali", "wiki", "grantha" -> "grantha"
            "hadugalu", "playlists", "favorites", "sangeeta" -> "sangeeta"
            "aayadi", "rangavalli", "recipes", "samskruti" -> "samskruti"
            "user", "settings" -> "settings"
            else -> pageId
        }

        fun iconFor(id: String): Int = when (id) {
            "dashboard" -> R.drawable.ic_nav_home
            "desk" -> R.drawable.ic_nav_calendar
            "nityakarma" -> R.drawable.ic_nav_daily
            "sandhya" -> R.drawable.ic_sun
            "japa" -> R.drawable.ic_japa_eda
            "puja" -> R.drawable.ic_nav_puja
            "nitya" -> R.drawable.ic_nav_library
            "grantha" -> R.drawable.ic_nav_library
            "stotra", "suktas", "mantras", "namavali", "wiki" -> R.drawable.ic_nav_library
            "sangeeta", "hadugalu", "playlists", "favorites" -> R.drawable.ic_nav_songs
            "samskruti" -> R.drawable.ic_nav_culture
            "aayadi" -> R.drawable.ic_nav_culture
            "rangavalli" -> R.drawable.ic_dashboard
            "recipes" -> R.drawable.ic_nav_puja
            "user" -> R.drawable.ic_user
            "settings" -> R.drawable.ic_nav_settings
            else -> R.drawable.ic_information
        }
    }
}
