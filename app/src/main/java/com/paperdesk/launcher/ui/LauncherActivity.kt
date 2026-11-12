package com.paperdesk.launcher.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.paperdesk.launcher.DeskLock
import com.paperdesk.launcher.DeskTheme
import com.paperdesk.launcher.DeskWindow
import com.paperdesk.launcher.R
import com.paperdesk.launcher.alarm.AlarmScheduler
import com.paperdesk.launcher.data.OsLang
import com.paperdesk.launcher.data.SettingsStore

class LauncherActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(AppTextSize.wrap(newBase))
    }

    private lateinit var pager: ViewPager2
    private lateinit var toolbar: MaterialToolbar
    private lateinit var shell: FrozenShell
    private var selectorOpen = false
    private var footerAction: (() -> Unit)? = null
    private val langTick: () -> Unit = { bindNavigation() }

    override fun onCreate(savedInstanceState: Bundle?) {
        DeskTheme.apply(this)
        // Stable page IDs let Android restore the selected page and its view state.
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        val dark = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
        setContentView(R.layout.frozen_launcher)
        toolbar = findViewById(R.id.toolbar)
        toolbar.setTitleTextAppearance(this, R.style.TextAppearance_Carbon_Label02)
        toolbar.setTitleTextColor(getColor(R.color.carbon_text_secondary))
        pager = findViewById(R.id.pager)
        val content = findViewById<View>(R.id.materialContent)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(content) { v, insets ->
            val safe = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars() or
                androidx.core.view.WindowInsetsCompat.Type.displayCutout() or androidx.core.view.WindowInsetsCompat.Type.ime())
            v.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            insets
        }
        androidx.core.view.ViewCompat.requestApplyInsets(content)
        pager.adapter = WorkspaceAdapter(this)
        shell = FrozenShell(this, pager)
        pager.offscreenPageLimit = 1
        pager.isUserInputEnabled = false
        val count = pager.adapter!!.itemCount
        val requested = intent?.getStringExtra("page")
        val start = when {
            !requested.isNullOrBlank() -> (pager.adapter as WorkspaceAdapter).indexOf(requested).takeIf { it >= 0 }
            savedInstanceState != null -> savedInstanceState.getInt("page")
            else -> null
        }
        pager.setCurrentItem((start ?: 0).coerceIn(0, count - 1), false)
        listOf(Triple(R.id.toolbarDate, R.drawable.ic_calendar, "ದಿನಾಂಕ ಆಯ್ಕೆಮಾಡಿ"),
            Triple(R.id.textSmaller, R.drawable.ic_text_smaller, "ಅಕ್ಷರ ಗಾತ್ರ ಕಡಿಮೆ ಮಾಡಿ"),
            Triple(R.id.textLarger, R.drawable.ic_text_larger, "ಅಕ್ಷರ ಗಾತ್ರ ಹೆಚ್ಚಿಸಿ"),
            Triple(R.id.themeSwitch, R.drawable.ic_color_switch, "Switch color theme")).forEachIndexed { index, (id, icon, label) ->
            toolbar.menu.add(Menu.NONE, id, index * 2, label).apply {
                setIcon(icon)
                setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS)
            }
        }
        toolbar.menu.add(Menu.NONE, R.id.textScaleLabel, 3, "Text size").apply {
            actionView = com.google.android.material.textview.MaterialTextView(this@LauncherActivity).apply {
                text = "${(AppTextSize.scale(this@LauncherActivity)*100).toInt()}%"
                setTextColor(getColor(R.color.carbon_text_secondary))
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,12*resources.displayMetrics.density)
                gravity=android.view.Gravity.CENTER
                layoutParams=android.view.ViewGroup.LayoutParams((44*resources.displayMetrics.density).toInt(),-1)
                contentDescription="App text size $text"
            }
            setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        val iconColor = com.google.android.material.color.MaterialColors.getColor(toolbar, com.google.android.material.R.attr.colorOnSurface)
        for (index in 0 until toolbar.menu.size()) toolbar.menu.getItem(index).icon?.mutate()?.setTint(iconColor)
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.textSmaller -> resizeText(-0.25f)
                R.id.textLarger -> resizeText(0.25f)
                R.id.toolbarDate -> supportFragmentManager.fragments.filterIsInstance<DeskFragment>().firstOrNull()?.showDatePicker()
                R.id.themeSwitch -> DeskTheme.set(this, if(DeskTheme.current(this)==DeskTheme.BLACK) DeskTheme.PAPER else DeskTheme.BLACK)
            }
            true
        }
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                supportFragmentManager.fragments.forEach { it.view?.let { root -> MaterialChoices.dismiss(root) } }
                bindNavigation()
            }
        })
        bindNavigation()
        AlarmScheduler.rescheduleAll(this)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 42)
        }
    }

    private fun bindNavigation() {
        if (!::pager.isInitialized) return
        val adapter = pager.adapter as WorkspaceAdapter
        val lang = OsLang.get(this)
        toolbar.title = if (resources.configuration.fontScale > 1.5f) "" else OsLang.ui(adapter.pageId(pager.currentItem), lang)
        if (::shell.isInitialized) shell.bind()
        toolbar.menu.findItem(R.id.textSmaller)?.isEnabled = AppTextSize.scale(this) > 1f
        toolbar.menu.findItem(R.id.textLarger)?.isEnabled = AppTextSize.scale(this) < 2f
        toolbar.menu.findItem(R.id.toolbarDate)?.isVisible = adapter.pageId(pager.currentItem) == "desk"
        toolbar.menu.findItem(R.id.themeSwitch)?.title = if(DeskTheme.current(this)==DeskTheme.BLACK) "Switch to light theme" else "Switch to dark theme"
        val page = adapter.pageId(pager.currentItem)
        findViewById<View>(R.id.contentControlsHost).visibility =
            if (page == "desk" || page == "dashboard" || page in ShellHubFragment.RAIL_IDS) View.GONE else View.VISIBLE
        pager.post {
            val current = supportFragmentManager.fragments.firstOrNull { it.isResumed }?.view
            if (current != null) ScreenPages.attachControls(current, findViewById(R.id.contentControlsHost))
        }

    }

    private fun resizeText(delta: Float) {
        AppTextSize.save(this, (AppTextSize.scale(this) + delta).coerceIn(1f, 2f))
        recreate()
    }

    fun setSelectorOpen(open: Boolean) { selectorOpen = open; bindNavigation() }
    fun setFooterAction(description: String? = null, action: (() -> Unit)? = null) {
        footerAction = action

    }
    fun navigateTo(pageId: String) {
        if (!::pager.isInitialized) return
        val adapter = pager.adapter as? WorkspaceAdapter ?: return
        val index = adapter.indexOf(pageId)
        if (index < 0) return
        if (pager.currentItem == index) {
            bindNavigation()
            return
        }
        pager.setCurrentItem(index, false)
        // ViewPager2 updates currentItem synchronously for smoothScroll=false, but
        // fragment resume/layout can lag — refresh chrome after the next frame.
        pager.post { bindNavigation() }
    }
    fun goHome() {
        supportFragmentManager.fragments.filterIsInstance<ReaderFragment>().forEach { it.closeSelector() }
        navigateTo("dashboard")
        if ((pager.adapter as WorkspaceAdapter).indexOf("dashboard") < 0) {
            pager.setCurrentItem(0, false)
            bindNavigation()
        }
    }
    fun applyLockChrome() {
        if (DeskLock.isLocked(this@LauncherActivity)) pager.setCurrentItem(0, false)
        bindNavigation()
    }
    fun applyShadeChrome() = Unit
    fun applyAlwaysOn() = DeskWindow.keepAwake(this, SettingsStore(this).alwaysOn())

    override fun onResume() { super.onResume(); OsLang.addListener(langTick); applyAlwaysOn(); applyLockChrome() }
    override fun onPause() { OsLang.removeListener(langTick); super.onPause() }
    override fun onSaveInstanceState(outState: Bundle) { outState.putInt("page", pager.currentItem); super.onSaveInstanceState(outState) }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val page = intent.getStringExtra("page")
        if (!page.isNullOrBlank()) navigateTo(page) else goHome()
    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (::shell.isInitialized && shell.close()) return
        if (supportFragmentManager.fragments.filterIsInstance<ReaderFragment>().any { it.closeSelector() }) return
        if (DeskLock.isLocked(this@LauncherActivity)) return
        if (pager.currentItem != 0) goHome() else super.onBackPressed()
    }
}
