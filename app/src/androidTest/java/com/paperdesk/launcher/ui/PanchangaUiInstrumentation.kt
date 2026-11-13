package com.paperdesk.launcher.ui

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.PanchangaCalendar
import java.io.File
import kotlin.math.abs

/** Checks the native calendar on the connected device at caller-specified display settings. */
class PanchangaUiInstrumentation : Instrumentation() {
    private var requestedScale = 1f
    override fun onCreate(arguments: Bundle?) {
        requestedScale = arguments?.getString("textScale")?.toFloatOrNull() ?: 1f
        super.onCreate(arguments)
        start()
    }

    override fun onStart() {
        val result = Bundle()
        try {
            AppTextSize.save(targetContext, requestedScale)
            val activity = startActivitySync(
                Intent(Intent.ACTION_MAIN).apply {
                    component = ComponentName(targetContext, LauncherActivity::class.java)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            ) as LauncherActivity
            fun main(block: () -> Unit) {
                var failure: Throwable? = null
                runOnMainSync {
                    try {
                        block()
                    } catch (problem: Throwable) {
                        failure = problem
                    }
                }
                failure?.let { throw it }
            }
            fun settle() {
                waitForIdleSync()
                SystemClock.sleep(1600)
                waitForIdleSync()
            }
            settle()
            main { activity.navigateTo("desk") }
            settle()
            main {
                activity.supportFragmentManager.setFragmentResult(
                    "panchangaDate",
                    Bundle().apply { putString("date", "2026-04-28") }
                )
            }
            settle()
            val captures = File(targetContext.getExternalFilesDir(null), "panchanga-review").apply { mkdirs() }
            for (phase in 0..1) {
                main {
                    val button = activity.findViewById<View>(
                        if (phase == 0) R.id.calendarPrevious else R.id.calendarNext
                    )
                    if (button.isEnabled) button.performClick()
                }
                settle()
                main {
                    val title = activity.findViewById<TextView>(R.id.headerTitle)
                    check(title.text.isNotBlank() || title.visibility != View.VISIBLE) {
                        "Header title unexpected"
                    }
                    val root = activity.supportFragmentManager.fragments
                        .filterIsInstance<DeskFragment>().first().requireView()
                    val area = root.findViewById<View>(R.id.pakshaScroll)
                    val areaBounds = Rect().also { area.getGlobalVisibleRect(it) }
                    val eventBounds = Rect().also {
                        root.findViewById<View>(R.id.dayEventsCard).getGlobalVisibleRect(it)
                    }
                    val grids = listOf(
                        root.findViewById<FrameLayout>(R.id.shuklaCalendarRow),
                        root.findViewById(R.id.krishnaCalendarRow)
                    )
                    for (grid in grids.filter { it.visibility == View.VISIBLE }) {
                        val cells = (0 until grid.childCount).mapNotNull { grid.getChildAt(it) as? FrameLayout }
                        check(cells.flatMap { (it.tag as PanchangaCalendar.Tile).tithis } == (1..15).toList()) {
                            "Missing tithis"
                        }
                        for (cell in cells) {
                            val tile = cell.getChildAt(0) as MaterialButton
                            val span = (cell.tag as PanchangaCalendar.Tile).tithis.size
                            val gap = root.resources.getDimensionPixelSize(R.dimen.carbon_spacing_03)
                            check(abs(cell.width - (cell.height * span + gap * (span - 1))) <= 2) {
                                "Non-square tile"
                            }
                            val bounds = Rect().also { cell.getGlobalVisibleRect(it) }
                            check(bounds.height() == cell.height && bounds.width() == cell.width) {
                                "Clipped tile ${tile.text}"
                            }
                            check(bounds.bottom <= eventBounds.top + 2 && bounds.top >= areaBounds.top - 2) {
                                "Calendar overlaps event box"
                            }
                        }
                    }
                    check(root.findViewById<TextView>(R.id.dayEvent).text.isNotBlank()) { "Missing event state" }
                    check(root.findViewById<TextView>(R.id.selectedDateText).text.contains("28")) {
                        "Date was not updated"
                    }
                }
                uiAutomation.takeScreenshot().let { bitmap ->
                    File(captures, "phase-$phase.png").outputStream().use {
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                    }
                    bitmap.recycle()
                }
            }
            result.putString("stream", "PASS: panchanga phases at scale $requestedScale")
            finish(Activity.RESULT_OK, result)
        } catch (e: Throwable) {
            result.putString("stream", android.util.Log.getStackTraceString(e))
            finish(Activity.RESULT_CANCELED, result)
        } finally {
            AppTextSize.save(targetContext, 1f)
        }
    }
}
