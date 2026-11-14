package com.paperdesk.launcher.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.paperdesk.launcher.DeskLock
import com.paperdesk.launcher.DeskWindow
import com.paperdesk.launcher.R
import com.paperdesk.launcher.alarm.AlarmScheduler
import com.paperdesk.launcher.data.IndicScript
import com.paperdesk.launcher.data.OsLang
import com.paperdesk.launcher.data.PanchangaStore
import com.paperdesk.launcher.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class LauncherActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(AppTextSize.wrap(newBase))
    }

    private lateinit var headerTitle: TextView
    private lateinit var headerDate: TextView
    private lateinit var appHeader: View
    private lateinit var headerRule: View
    private lateinit var shell: FrozenShell
    private lateinit var pages: List<String>
    private var currentPageId = "dashboard"
    private var selectorOpen = false
    private var footerAction: (() -> Unit)? = null
    private var headerDateJob: Job? = null
    private val langTick: () -> Unit = { bindNavigation() }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Theme already applied in Application; only re-apply after explicit theme toggles.
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        val dark = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        androidx.core.view.WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
        setContentView(R.layout.frozen_launcher)
        appHeader = findViewById(R.id.appHeader)
        headerRule = findViewById(R.id.headerRule)
        headerTitle = findViewById(R.id.headerTitle)
        headerDate = findViewById(R.id.headerDate)
        headerDate.setOnClickListener {
            if (currentPageId == "desk") {
                supportFragmentManager.fragments.filterIsInstance<DeskFragment>().firstOrNull()?.showDatePicker()
            }
        }
        val content = findViewById<View>(R.id.materialContent)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(content) { v, insets ->
            val safe = insets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.systemBars() or
                    androidx.core.view.WindowInsetsCompat.Type.displayCutout() or
                    androidx.core.view.WindowInsetsCompat.Type.ime()
            )
            v.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            insets
        }
        androidx.core.view.ViewCompat.requestApplyInsets(content)

        // Fixed shipped destinations — do not parse Settings JSON on the launch path.
        pages = SettingsStore.PAGE_IDS
        shell = FrozenShell(this)
        val requested = intent?.getStringExtra("page")?.takeIf { SettingsStore.isKnownPage(it) }
        val requestedHub = ShellHubFragment.TAB_HUBS.firstOrNull { requested in ShellHubFragment.childrenFor(it) }
            ?: if (requested == "playlists") "sangeeta" else null
        val startId = when {
            !requested.isNullOrBlank() -> requestedHub ?: SettingsStore.canonicalizePageId(requested)
                .takeIf { it in pages } ?: pages.first()
            savedInstanceState != null ->
                savedInstanceState.getString(STATE_PAGE)?.takeIf { it in pages } ?: pages.first()
            else -> pages.first()
        }
        val nityaTab = when (requested) {
            "sandhya", "japa", "nitya" -> requested
            else -> null
        }
        // Draw shell chrome first; attach the start fragment on the next frame so TTID drops.
        window.decorView.post {
            if (isFinishing) return@post
            showPage(startId, nityaTab, force = true)
            if (requested != null && requested != startId && nityaTab == null) navigateTo(requested)
            shell.bind()
            Thread({ AlarmScheduler.rescheduleAll(applicationContext) }, "alarm-reschedule").start()
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 42)
        }
    }

    fun currentPageId(): String = currentPageId
    fun shellPages(): List<String> = pages
    fun pageCount(): Int = pages.size
    fun pageIdAt(index: Int): String = pages.getOrElse(index) { pages.first() }
    fun openPageAt(index: Int) {
        if (index in pages.indices) showPage(pages[index])
    }

    private fun showPage(pageId: String, nityaTab: String? = null, force: Boolean = false) {
        val tabGroup = ShellHubFragment.TAB_HUBS.firstOrNull { pageId in ShellHubFragment.childrenFor(it) }
        if (tabGroup != null) {
            showPage(tabGroup)
            val host = supportFragmentManager.findFragmentById(R.id.pageHost)
            if (host is SongLibraryFragment) host.selectCollection(pageId)
            else (host as? GranthaFragment)?.selectReader(pageId)
            return
        }
        if (pageId == "user") {
            showPage("settings")
            (supportFragmentManager.findFragmentById(R.id.pageHost) as? SettingsFragment)?.openUsers()
            return
        }
        val target = if (pageId in pages) pageId else SettingsStore.canonicalizePageId(pageId)
            .takeIf { it in pages } ?: return
        if (!force && currentPageId == target) {
            if (nityaTab != null) {
                (supportFragmentManager.findFragmentById(R.id.pageHost) as? NityaKarmaFragment)
                    ?.selectTab(nityaTab)
            }
            bindNavigation()
            return
        }
        currentPageId = target
        val fragment: Fragment = PageFactory.create(target)
        supportFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.pageHost, fragment, target)
            .commitNowAllowingStateLoss()
        if (nityaTab != null) {
            (supportFragmentManager.findFragmentById(R.id.pageHost) as? NityaKarmaFragment)
                ?.selectTab(nityaTab)
        }
        bindNavigation()
    }

    private fun bindNavigation() {
        if (!::headerTitle.isInitialized) return
        val lang = OsLang.get(this)
        val page = currentPageId
        // One header pattern: the shared app bar carries the destination title on every page.
        appHeader.visibility = View.VISIBLE
        headerRule.visibility = View.VISIBLE
        headerTitle.text =
            if (resources.configuration.fontScale > 1.5f) "" else OsLang.ui(page, lang)
        refreshHeaderDate(lang)
        if (::shell.isInitialized) shell.bind()
        findViewById<View>(R.id.contentControlsHost).visibility =
            if (page == "desk" || page == "dashboard" || page == "user" || page in ShellHubFragment.RAIL_IDS ||
                page == "nityakarma" || page == "puja"
            ) View.GONE else View.VISIBLE
        findViewById<View>(R.id.pageHost).post {
            val current = supportFragmentManager.findFragmentById(R.id.pageHost)?.view
            if (current != null) ScreenPages.attachControls(current, findViewById(R.id.contentControlsHost))
        }
    }

    private fun refreshHeaderDate(lang: IndicScript.Lang) {
        val cal = Calendar.getInstance()
        val dateLine = OsLang.dateLine(cal, lang)
        headerDate.text = dateLine
        headerDateJob?.cancel()
        headerDateJob = lifecycleScope.launch {
            val day = withContext(Dispatchers.IO) {
                PanchangaStore.forDate(applicationContext, PanchangaStore.dateKey())
            }
            if (isFinishing) return@launch
            val tithi = OsLang.script(day.tithi.ifBlank { "—" }, lang)
            headerDate.text = "$dateLine · $tithi"
        }
    }

    fun setSelectorOpen(open: Boolean) {
        selectorOpen = open
        bindNavigation()
    }

    fun setFooterAction(description: String? = null, action: (() -> Unit)? = null) {
        footerAction = action
    }

    fun navigateTo(pageId: String) {
        val id = when (pageId) {
            "panchanga" -> "desk"
            "playlists" -> "favorites"
            else -> pageId
        }
        if (!SettingsStore.isKnownPage(id)) return
        val nityaTab = when (id) {
            "sandhya", "japa", "nitya" -> id
            "nityakarma" -> "sandhya"
            else -> null
        }
        val targetId = when {
            nityaTab != null -> "nityakarma"
            else -> id
        }
        showPage(targetId, nityaTab)
    }

    fun goHome() {
        supportFragmentManager.fragments.filterIsInstance<ReaderFragment>().forEach { it.closeSelector() }
        navigateTo("dashboard")
        if ("dashboard" !in pages) showPage(pages.first())
    }

    fun applyLockChrome() {
        if (DeskLock.isLocked(this)) showPage(pages.firstOrNull() ?: "dashboard")
        else bindNavigation()
    }

    fun applyShadeChrome() = Unit
    fun applyAlwaysOn() = DeskWindow.keepAwake(this, SettingsStore(this).alwaysOn())

    override fun onResume() {
        super.onResume()
        OsLang.addListener(langTick)
        applyAlwaysOn()
        if (DeskLock.isLocked(this)) applyLockChrome()
    }

    override fun onPause() {
        OsLang.removeListener(langTick)
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_PAGE, currentPageId)
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val page = intent.getStringExtra("page")?.takeIf { SettingsStore.isKnownPage(it) }
        if (!page.isNullOrBlank()) navigateTo(page) else goHome()
    }

    /** Hierarchical back: leaf → hub → dashboard (not flat dump to home). */
    private fun parentPage(pageId: String): String? = when (pageId) {
        "playlists", "favorites", "hadugalu" -> "sangeeta"
        "stotra", "suktas", "mantras", "namavali" -> "grantha"
        "aayadi", "rangavalli", "recipes" -> "samskruti"
        "user" -> "settings"
        "dashboard" -> null
        else -> if (pageId in ShellHubFragment.RAIL_IDS || pageId == "desk") "dashboard" else null
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (::shell.isInitialized && shell.close()) return
        if (supportFragmentManager.fragments.filterIsInstance<ReaderFragment>().any { it.closeSelector() }) return
        if (DeskLock.isLocked(this)) return
        if ((supportFragmentManager.findFragmentById(R.id.pageHost) as? SettingsFragment)?.consumeBack() == true) return
        val parent = parentPage(currentPageId)
        when {
            parent != null -> navigateTo(parent)
            else -> super.onBackPressed()
        }
    }

    companion object {
        private const val STATE_PAGE = "pageId"
    }
}
