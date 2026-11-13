package com.paperdesk.launcher.ui

import android.content.res.ColorStateList
import android.view.View
import android.widget.LinearLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.paperdesk.launcher.DeskLock
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.OsLang
import com.paperdesk.launcher.data.SettingsStore

/** Carbon side rail and overlay panel. Primary destinations match the HTML shell sim. */
class FrozenShell(private val activity: LauncherActivity, private val pager: ViewPager2) {
    private val panel: View = activity.findViewById(R.id.sidePanel)
    private val backdrop: View = activity.findViewById(R.id.sidePanelBackdrop)
    private var lastRailKey = ""
    private var lastPanelKey = ""
    private var lastActiveRail = ""
    private var lastCurrent = ""

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
        val pageSet = (0 until adapter.itemCount).map { adapter.pageId(it) }.toSet()
        val hasNitya = "nityakarma" in pageSet
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
            id in ShellHubFragment.RAIL_IDS ||
                id in pageSet ||
                (id in SettingsStore.NITYA_TAB_ALIASES && hasNitya)
        }

        bindHost(
            hostId = R.id.railDestinations,
            entries = ShellHubFragment.RAIL_IDS,
            key = ShellHubFragment.RAIL_IDS.joinToString(),
            lastKey = lastRailKey,
            labeled = false,
            current = current,
            activeRail = activeRail,
            density = density
        ).also { lastRailKey = it }

        bindHost(
            hostId = R.id.panelDestinations,
            entries = panelEntries,
            key = panelEntries.joinToString(),
            lastKey = lastPanelKey,
            labeled = true,
            current = current,
            activeRail = activeRail,
            density = density
        ).also { lastPanelKey = it }

        lastActiveRail = activeRail
        lastCurrent = current
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
            for (id in entries) {
                host.addView(createButton(id, labeled, density), LinearLayout.LayoutParams(-1, (48 * density).toInt()))
            }
        } else if (current == lastCurrent && activeRail == lastActiveRail) {
            return key
        }
        for (i in 0 until host.childCount) {
            val button = host.getChildAt(i) as? MaterialButton ?: continue
            val id = button.tag as? String ?: continue
            val active = if (labeled) {
                when (id) {
                    "sandhya", "japa", "puja" -> current == "nityakarma"
                    else -> current == id
                }
            } else {
                activeRail == id
            }
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
            iconSize = (24 * density).toInt()
            iconPadding = if (text.isEmpty()) 0 else (12 * density).toInt()
            iconGravity = if (text.isEmpty()) MaterialButton.ICON_GRAVITY_TEXT_START else MaterialButton.ICON_GRAVITY_START
            minimumWidth = 0
            minimumHeight = 0
            insetTop = 0
            insetBottom = 0
            setPadding((8 * density).toInt(), 0, (8 * density).toInt(), 0)
            cornerRadius = 0
            rippleColor = ColorStateList.valueOf(activity.getColor(R.color.carbon_background_hover))
            maxLines = 1
            androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                this, 10, 14, 1, android.util.TypedValue.COMPLEX_UNIT_SP
            )
            androidx.core.view.ViewCompat.setTooltipText(this, label)
            setOnClickListener {
                close()
                activity.navigateTo(id)
            }
            paintButton(this, false)
        }
    }

    private fun paintButton(button: MaterialButton, active: Boolean) {
        val color = activity.getColor(
            if (active) R.color.carbon_interactive else R.color.carbon_icon_secondary
        )
        button.iconTint = ColorStateList.valueOf(color)
        button.setTextColor(
            activity.getColor(
                if (active) R.color.carbon_text_primary else R.color.carbon_text_secondary
            )
        )
        button.backgroundTintList = ColorStateList.valueOf(
            if (active) activity.getColor(R.color.carbon_layer_selected)
            else android.graphics.Color.TRANSPARENT
        )
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
