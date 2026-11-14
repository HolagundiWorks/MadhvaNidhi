package com.paperdesk.launcher.ui

import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.ProgressBar
import com.google.android.material.button.MaterialButton
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.SettingsStore
import java.util.concurrent.atomic.AtomicReference

/**
 * Default connected UI smoke runner.
 * Opens every shipped destination, asserts the process stays alive, and exercises
 * Sandhya / Dashboard / Panchanga controls that previously crashed.
 */
class ResponsiveUiInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        start()
    }

    override fun onStart() {
        val result = Bundle()
        val active = AtomicReference<LauncherActivity>()
        val application = targetContext.applicationContext as Application
        val callbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(a: Activity) {
                if (a is LauncherActivity) active.set(a)
            }
            override fun onActivityCreated(a: Activity, b: Bundle?) = Unit
            override fun onActivityStarted(a: Activity) = Unit
            override fun onActivityPaused(a: Activity) = Unit
            override fun onActivityStopped(a: Activity) = Unit
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) = Unit
            override fun onActivityDestroyed(a: Activity) = Unit
        }
        application.registerActivityLifecycleCallbacks(callbacks)
        fun main(block: () -> Unit) {
            var error: Throwable? = null
            runOnMainSync {
                try {
                    block()
                } catch (e: Throwable) {
                    error = e
                }
            }
            error?.let { throw it }
        }
        fun settle(ms: Long = 900) {
            // Sleep-only: waitForIdleSync can hang if a screen keeps posting layout work.
            SystemClock.sleep(ms)
        }
        fun alive() {
            val a = active.get()
            check(a != null && !a.isFinishing && !a.isDestroyed) { "Launcher quit or destroyed" }
            check(a.findViewById<View>(R.id.pageHost) != null) { "pageHost missing" }
        }

        try {
            main {
                targetContext.startActivity(
                    Intent(Intent.ACTION_MAIN).apply {
                        component = ComponentName(targetContext, LauncherActivity::class.java)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                )
            }
            settle(1600)
            alive()

            val pages = SettingsStore.PAGE_IDS + listOf("sandhya", "japa", "nitya")
            for (page in pages) {
                main { active.get().navigateTo(page) }
                settle()
                alive()
                main {
                    val host = active.get().supportFragmentManager.findFragmentById(R.id.pageHost)
                    check(host != null && host.isAdded) { "No fragment for $page" }
                    check(host.requireView().width > 0 && host.requireView().height > 0) {
                        "Empty view for $page"
                    }
                }
            }

            // Dashboard progress dialog
            main { active.get().navigateTo("dashboard") }
            settle()
            main {
                val root = active.get().supportFragmentManager.findFragmentById(R.id.pageHost)!!.requireView()
                root.findViewById<android.widget.LinearLayout>(R.id.dashboardWeek).getChildAt(0).performClick()
            }
            settle()
            alive()
            uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            settle(400)

            // Sandhya: kala selector and direct landscape-reader entry
            main { active.get().navigateTo("sandhya") }
            settle()
            main {
                val root = active.get().supportFragmentManager.findFragmentById(R.id.pageHost)!!.requireView()
                root.findViewById<MaterialButton>(R.id.sandhyaKalaMadhya).performClick()
                check(root.findViewById<MaterialButton>(R.id.sandhyaRead).isEnabled)
            }
            settle()
            alive()

            // Japa pad tap
            main { active.get().navigateTo("japa") }
            settle()
            main {
                val root = active.get().supportFragmentManager.findFragmentById(R.id.pageHost)!!.requireView()
                root.findViewById<View>(R.id.japaTap).performClick()
            }
            settle()
            alive()

            // Puja complete
            main { active.get().navigateTo("puja") }
            settle(1200)
            main {
                val root = active.get().supportFragmentManager.findFragmentById(R.id.pageHost)!!.requireView()
                root.findViewById<View>(R.id.pujaComplete)?.performClick()
            }
            settle()
            alive()

            // Panchanga reflow (prior coerceIn crash)
            main { active.get().navigateTo("desk") }
            settle(1400)
            main {
                val root = active.get().supportFragmentManager.fragments
                    .filterIsInstance<DeskFragment>().first().requireView()
                val shukla = root.findViewById<View>(R.id.shuklaCalendarRow)
                check(shukla.width > 0 && shukla.height > 0) { "Panchanga calendar empty" }
                root.findViewById<View>(R.id.calendarNext).performClick()
            }
            settle()
            alive()
            main {
                active.get().findViewById<View>(R.id.sidePanelToggle).performClick()
            }
            settle(400)
            main {
                check(active.get().findViewById<View>(R.id.sidePanel).visibility == View.VISIBLE) {
                    "Side panel did not open"
                }
                active.get().findViewById<View>(R.id.sidePanelBackdrop).performClick()
            }
            settle(400)
            alive()

            result.putString(
                "stream",
                "PASS: ${pages.size} destinations + dashboard/sandhya/japa/puja/desk interactions"
            )
            finish(Activity.RESULT_OK, result)
        } catch (e: Throwable) {
            result.putString("stream", android.util.Log.getStackTraceString(e))
            finish(Activity.RESULT_CANCELED, result)
        } finally {
            application.unregisterActivityLifecycleCallbacks(callbacks)
        }
    }
}
