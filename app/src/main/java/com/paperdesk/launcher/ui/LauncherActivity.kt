package com.paperdesk.launcher.ui

import android.Manifest
import android.app.AlarmManager
import android.content.res.ColorStateList
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.media.AudioManager
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.paperdesk.launcher.data.GitaAudio
import com.paperdesk.launcher.data.OsLang
import com.paperdesk.launcher.data.SettingsStore
import androidx.viewpager2.widget.ViewPager2
import com.paperdesk.launcher.DeskLock
import com.paperdesk.launcher.DeskWindow
import com.paperdesk.launcher.LockGuard
import com.paperdesk.launcher.R
import com.paperdesk.launcher.ShadeLock
import com.paperdesk.launcher.alarm.AlarmScheduler

class LauncherActivity : AppCompatActivity() {

    private var askedManage = false
    private var askedExact = false
    private lateinit var pager: ViewPager2
    private lateinit var pagerHost: ZonePagerHost
    private lateinit var pageNav: View
    private lateinit var navPrev: ImageButton
    private lateinit var pageCurrent: TextView
    private lateinit var hariMotto: TextView
    private lateinit var topMotto: TextView
    private lateinit var navAction: ImageButton
    private lateinit var navNext: ImageButton
    private lateinit var controls: ControlPanel
    private val langTick: () -> Unit = { bindLang() }
    private var selectorOpen = false
    private var footerAction: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LockGuard.apply(this)
        com.paperdesk.launcher.DeskTheme.apply(this)
        val settings = com.paperdesk.launcher.data.SettingsStore(this)
        requestedOrientation = if (settings.landscape()) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        setContentView(R.layout.activity_launcher)
        applyShadeChrome()
        AlarmScheduler.rescheduleAll(this)

        pagerHost = findViewById(R.id.pagerHost)
        pager = findViewById(R.id.pager)
        pager.orientation = ViewPager2.ORIENTATION_HORIZONTAL

        pager.adapter = WorkspaceAdapter(this)
        pager.offscreenPageLimit = 4
        pager.isUserInputEnabled = false
        pager.setCurrentItem(0, false)
        controls = ControlPanel(this)
        controls.attach()
        pageNav = findViewById(R.id.pageNav)
        navPrev = findViewById(R.id.navPrev)
        pageCurrent = findViewById(R.id.pageCurrent)
        hariMotto = findViewById(R.id.hariMotto)
        topMotto = findViewById(R.id.topMotto)
        navAction = findViewById(R.id.navAction)
        navNext = findViewById(R.id.navNext)
        DeskWindow.wrapCutout(findViewById(R.id.launcherRoot), pagerHost, pageNav)
        navPrev.setOnClickListener { shiftPage(-1) }
        navNext.setOnClickListener { shiftPage(1) }
        navAction.setOnClickListener { footerAction?.invoke() }
        placeContentNav()
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                supportFragmentManager.fragments.filterIsInstance<ReaderFragment>().forEach { it.closeSelector() }
                if (position != 0 && DeskLock.isLocked(this@LauncherActivity)) {
                    pager.setCurrentItem(0, false)
                    return
                }
                bindNav()
            }
        })
        bindLang()
        applyLockChrome()
        applyShadeChrome()
        if (!DeskLock.isLocked(this)) requestRuntimePerms()
    }

    private fun requestRuntimePerms() {
        val need = mutableListOf<String>()
        if (Build.VERSION.SDK_INT < 30) {
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ).forEach { if (checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED) need += it }
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            need += Manifest.permission.POST_NOTIFICATIONS
        }
        if (need.isNotEmpty()) requestPermissions(need.toTypedArray(), 42)
        else runCatching { com.paperdesk.launcher.bridge.OsDataService.ensureLayout(this) }
    }

    private fun requestSpecialAccess() {
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            if (!askedManage) {
                askedManage = true
                runCatching {
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                    )
                }
            }
            return
        }
        if (Build.VERSION.SDK_INT >= 31) {
            val am = getSystemService(AlarmManager::class.java)
            if (am != null && !am.canScheduleExactAlarms() && !askedExact) {
                askedExact = true
                runCatching {
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).setData(Uri.parse("package:$packageName"))
                    )
                }
                return
            }
        }
        runCatching { com.paperdesk.launcher.bridge.OsDataService.ensureLayout(this) }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 42) {
            runCatching { com.paperdesk.launcher.bridge.OsDataService.ensureLayout(this) }
        }
    }

    fun applyLockChrome() {
        val locked = DeskLock.isLocked(this)
        applyPagerGate()
        if (locked) pager.setCurrentItem(0, false)
        findViewById<View>(R.id.pageNav).visibility =
            if (locked) View.GONE else View.VISIBLE
        bindNav()
        supportFragmentManager.fragments.filterIsInstance<DeskFragment>().forEach { it.applyLockState() }
        applyShadeChrome()
    }

    fun setSelectorOpen(open: Boolean) {
        selectorOpen = open
        applyPagerGate()
        findViewById<View>(R.id.pageNav).visibility =
            if (DeskLock.isLocked(this)) View.GONE else View.VISIBLE
        bindNav()
    }

    fun setFooterAction(description: String? = null, action: (() -> Unit)? = null) {
        footerAction = action
        if (!::navAction.isInitialized) return
        navAction.contentDescription = description ?: ""
        navAction.visibility = if (action == null || selectorOpen) View.GONE else View.VISIBLE
    }

    private fun placeContentNav() {
        if (!::pageNav.isInitialized) return
        val lp = pageNav.layoutParams as FrameLayout.LayoutParams
        lp.gravity = Gravity.BOTTOM
        lp.height = FrameLayout.LayoutParams.WRAP_CONTENT
        lp.bottomMargin = 0
        pageNav.layoutParams = lp
    }

    private fun applyPagerGate() {
        if (::pagerHost.isInitialized) pagerHost.pageSwipeAllowed = false
        if (::pager.isInitialized) pager.isUserInputEnabled = false
        bindNav()
    }

    fun goHome() {
        if (::controls.isInitialized && controls.isOpen()) controls.close()
        supportFragmentManager.fragments.filterIsInstance<ReaderFragment>().forEach { it.closeSelector() }
        if (!::pager.isInitialized) return
        pager.post {
            pager.setCurrentItem(0, false)
            applyLockChrome()
        }
    }

    private fun shiftPage(delta: Int) {
        if (DeskLock.isLocked(this) || selectorOpen) return
        if (!::pager.isInitialized) return
        val count = (pager.adapter?.itemCount ?: 1).coerceAtLeast(1)
        val from = pager.currentItem
        val next = (from + delta % count + count) % count
        if (next == from) return
        val wrap = (delta > 0 && from == count - 1) || (delta < 0 && from == 0)
        pager.setCurrentItem(next, !wrap)
        bindNav()
    }

    private fun bindNav() {
        if (!::navPrev.isInitialized || !::navNext.isInitialized || !::pager.isInitialized) return
        val allow = !DeskLock.isLocked(this) && !selectorOpen
        val count = (pager.adapter?.itemCount ?: 1).coerceAtLeast(1)
        val i = pager.currentItem
        val lang = OsLang.get(this)
        val adapter = pager.adapter as? WorkspaceAdapter
        fun title(pos: Int) = OsLang.ui(adapter?.pageId(pos) ?: "desk", lang)
        val currentId = adapter?.pageId(i) ?: "desk"
        val readerPage = currentId in setOf("mantras", "puja", "hadugalu")
        val earthAccent = PagePalette.accent(this, currentId)
        pageNav.background = SareeFooterDrawable(this, earthAccent, currentId)
        navPrev.background = FooterMedallionDrawable(this, earthAccent)
        navAction.background = FooterMedallionDrawable(this, earthAccent)
        navNext.background = FooterMedallionDrawable(this, earthAccent)
        val iconTint = ColorStateList.valueOf(getColor(R.color.white))
        navPrev.imageTintList = iconTint
        navAction.imageTintList = iconTint
        navNext.imageTintList = iconTint
        hariMotto.background = FooterMottoDrawable(this, earthAccent)
        hariMotto.setTextColor(earthAccent)
        footerAction = if (readerPage) {
            { supportFragmentManager.fragments.filterIsInstance<ReaderFragment>()
                .firstOrNull { it.isResumed }?.openSelector() }
        } else null
        if (::navAction.isInitialized) {
            navAction.contentDescription = if (readerPage) OsLang.ui("select_$currentId", lang) else ""
        }
        val prevPos = (i - 1 + count) % count
        val nextPos = (i + 1) % count
        navPrev.contentDescription = "${OsLang.ui("prev", lang)}. ${title(prevPos)}"
        navNext.contentDescription = "${OsLang.ui("next", lang)}. ${title(nextPos)}"
        pageCurrent.visibility = View.GONE
        hariMotto.text = title(i)
        hariMotto.contentDescription = title(i)
        DeskType.apply(hariMotto, DeskType.SEMIBOLD)
        navPrev.isEnabled = allow
        navNext.isEnabled = allow
        navPrev.visibility = if (selectorOpen) View.INVISIBLE else View.VISIBLE
        navNext.visibility = if (selectorOpen) View.INVISIBLE else View.VISIBLE
        if (::navAction.isInitialized) {
            navAction.visibility = if (!selectorOpen && footerAction != null) View.VISIBLE else View.GONE
            navAction.isEnabled = allow && footerAction != null
        }
        navPrev.alpha = if (navPrev.isEnabled) 1f else 0.4f
        navNext.alpha = if (navNext.isEnabled) 1f else 0.4f
        navPrev.importantForAccessibility =
            if (navPrev.isEnabled) View.IMPORTANT_FOR_ACCESSIBILITY_YES else View.IMPORTANT_FOR_ACCESSIBILITY_NO
        navNext.importantForAccessibility =
            if (navNext.isEnabled) View.IMPORTANT_FOR_ACCESSIBILITY_YES else View.IMPORTANT_FOR_ACCESSIBILITY_NO
        bindMotto()
    }

    private fun bindMotto() {
        if (!::topMotto.isInitialized) return
        val lang = OsLang.get(this)
        topMotto.text = OsLang.ui("hari_motto", lang)
        topMotto.contentDescription = OsLang.ui("hari_motto", lang)
        DeskType.apply(topMotto, DeskType.REGULAR)
    }

    fun applyShadeChrome() {
        val locked = ShadeLock.isLocked(this)
        findViewById<View>(R.id.shadeGuard)?.visibility = if (locked) View.VISIBLE else View.GONE
        com.paperdesk.launcher.bridge.OsChrome.applyShade(this, locked)
        DeskWindow.immersive(this)
        val on = SettingsStore(this).alwaysOn()
        DeskWindow.keepAwake(this, on)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && ShadeLock.isVolumeKey(event.keyCode)) {
            if (GitaAudio.isPlaying() || controls.isOpen()) {
                val dir = if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    AudioManager.ADJUST_RAISE
                } else {
                    AudioManager.ADJUST_LOWER
                }
                val flags = if (controls.isOpen()) 0 else AudioManager.FLAG_SHOW_UI
                getSystemService(AudioManager::class.java)
                    ?.adjustStreamVolume(AudioManager.STREAM_MUSIC, dir, flags)
            }
            if (ShadeLock.onVolume()) {
                ShadeLock.toggle(this)
                applyShadeChrome()
            }
            return true
        }
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_HOME) {
            goHome()
            return true
        }
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (controls.isOpen()) {
                controls.close()
                return true
            }
            if (supportFragmentManager.fragments.filterIsInstance<ReaderFragment>().any { it.closeSelector() }) {
                return true
            }
        }
        if (DeskLock.isLocked(this) && event.action == KeyEvent.ACTION_DOWN) {
            if (event.keyCode == KeyEvent.KEYCODE_BACK) return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        goHome()
    }

    override fun onRestart() {
        super.onRestart()
        if (intent.action == Intent.ACTION_MAIN) goHome()
    }

    override fun onResume() {
        super.onResume()
        OsLang.addListener(langTick)
        LockGuard.apply(this)
        applyShadeChrome()
        runCatching {
            val s = com.paperdesk.launcher.data.SettingsStore(this).json()
            com.paperdesk.launcher.bridge.OsRadios.apply(this, s)
        }
        applyLockChrome()
        applyAlwaysOn()
        placeContentNav()
        if (!DeskLock.isLocked(this)) requestSpecialAccess()
    }

    override fun onPause() {
        OsLang.removeListener(langTick)
        super.onPause()
    }

    private fun bindLang() {
        supportFragmentManager.fragments.filterIsInstance<DeskFragment>().forEach { it.bindLang() }
        if (::controls.isInitialized) controls.bindLang()
        bindNav()
    }

    fun applyAlwaysOn() {
        val on = SettingsStore(this).alwaysOn()
        DeskWindow.keepAwake(this, on)
        runCatching {
            com.paperdesk.launcher.bridge.OsRadios.apply(this, SettingsStore(this).json())
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyShadeChrome()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (controls.isOpen()) {
            controls.close()
            return
        }
        val closed = supportFragmentManager.fragments.filterIsInstance<ReaderFragment>().any { it.closeSelector() }
        if (closed) return
        if (DeskLock.isLocked(this)) return
        if (pager.currentItem != 0) goHome()
    }
}
