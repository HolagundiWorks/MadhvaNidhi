package com.paperdesk.launcher.ui

import android.app.Instrumentation
import android.content.ComponentName
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.GridLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.PanchangaCalendar
import java.io.File
import kotlin.math.abs

/** Checks the complete native calendar on the connected device at caller-specified display settings. */
class PanchangaUiInstrumentation : Instrumentation() {
    private var requestedScale = 1f
    override fun onCreate(arguments: Bundle?) {
        requestedScale = arguments?.getString("textScale")?.toFloatOrNull() ?: 1f
        super.onCreate(arguments); start()
    }
    override fun onStart() {
        val original = 1f
        val result = Bundle()
        try {
            AppTextSize.save(targetContext, requestedScale)
            val activity = startActivitySync(Intent(Intent.ACTION_MAIN).apply {
                component = ComponentName(targetContext, LauncherActivity::class.java)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }) as LauncherActivity
            fun main(block: () -> Unit) {
                var failure: Throwable? = null
                runOnMainSync { try { block() } catch (problem: Throwable) { failure = problem } }
                failure?.let { throw it }
            }
            fun settle() { waitForIdleSync(); SystemClock.sleep(1600); waitForIdleSync() }
            settle()
            main {
                activity.supportFragmentManager.setFragmentResult("panchangaDate", Bundle().apply { putString("date", "2026-04-28") })
            }
            settle()
            val captures = File(targetContext.getExternalFilesDir(null), "panchanga-review").apply { mkdirs() }
            for (phase in 0..1) {
                main {
                    val button = activity.findViewById<View>(if (phase == 0) R.id.calendarPrevious else R.id.calendarNext)
                    if (button.isEnabled) button.performClick()
                }
                settle()
                main {
                    val destination = activity.findViewById<TextView>(R.id.pageDestination)
                    check(destination.text.isNotBlank() && destination.layout.height <= destination.height - destination.totalPaddingTop - destination.totalPaddingBottom) { "Destination label is clipped: ${destination.layout.height} / ${destination.height} padding ${destination.totalPaddingTop},${destination.totalPaddingBottom}" }
                    val root = activity.supportFragmentManager.fragments.filterIsInstance<DeskFragment>().first().requireView()
                    val area = root.findViewById<View>(R.id.pakshaScroll)
                    val areaBounds = Rect().also { area.getGlobalVisibleRect(it) }
                    val eventBounds = Rect().also { root.findViewById<View>(R.id.dayEventsCard).getGlobalVisibleRect(it) }
                    val grids = listOf(root.findViewById<GridLayout>(R.id.shuklaCalendarRow), root.findViewById(R.id.krishnaCalendarRow))
                    for (grid in grids.filter { (it.parent as View).visibility == View.VISIBLE }) {
                        val tiles = (0 until grid.childCount).map { grid.getChildAt(it) as MaterialButton }
                        check(tiles.flatMap { (it.tag as PanchangaCalendar.Tile).tithis } == (1..15).toList()) { "Missing tithis" }
                        for (tile in tiles) {
                            val span = (tile.tag as PanchangaCalendar.Tile).tithis.size
                            val gap = root.resources.getDimensionPixelSize(R.dimen.carbon_spacing_02)
                            check(abs(tile.width - (tile.height * span + gap * (span - 1))) <= 1) { "Non-square tile" }
                            val bounds = Rect().also { tile.getGlobalVisibleRect(it) }
                            check(bounds.height() == tile.height && bounds.width() == tile.width) { "Clipped tile ${tile.text}" }
                            check(bounds.bottom <= eventBounds.top && bounds.top >= areaBounds.top) { "Calendar overlaps event box" }
                            val layout = tile.layout
                            check(layout != null && layout.height <= tile.height - tile.totalPaddingTop - tile.totalPaddingBottom) { "Clipped text ${tile.text}: ${layout?.height} / ${tile.height}" }
                        }
                    }
                    check(root.findViewById<TextView>(R.id.dayEvent).text.isNotBlank()) { "Missing event state" }
                    check(root.findViewById<TextView>(R.id.panchangaHeader).text.contains("2026")) { "Date was not updated" }
                }
                uiAutomation.takeScreenshot().let { bitmap -> File(captures, "phase-$phase.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle() }
            }
            main { activity.findViewById<View>(R.id.eventDetails).performClick() }
            settle()
            check(uiAutomation.rootInActiveWindow.findAccessibilityNodeInfosByText("ಸರಿ").isNotEmpty()) { "Event details missing" }
            uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            main { activity.findViewById<View>(R.id.chooseDate).performClick() }
            settle()
            main {
                val fragment = activity.supportFragmentManager.findFragmentByTag("date") as DateEntryDialog
                val dialog = fragment.requireDialog() as androidx.appcompat.app.AlertDialog
                dialog.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.dateEntryValue)!!.setText("2026-02-30")
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).performClick()
            }
            settle()
            main {
                val dialog = (activity.supportFragmentManager.findFragmentByTag("date") as DateEntryDialog).requireDialog() as androidx.appcompat.app.AlertDialog
                val field = dialog.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.dateEntryField)!!
                check(!field.error.isNullOrBlank()) { "Invalid date accepted" }
                val error = field.findViewById<TextView>(com.google.android.material.R.id.textinput_error)
                val bounds = Rect().also { error.getGlobalVisibleRect(it) }
                check(bounds.height() == error.height) { "Date error is clipped" }
                dialog.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.dateEntryValue)!!.setText("2026-04-23")
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).performClick()
            }
            settle()
            main {
                check(activity.findViewById<TextView>(R.id.panchangaHeader).text.contains("23")) { "Date entry did not select the requested day" }
                activity.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.pager).setCurrentItem(1, false)
            }
            settle()
            main { activity.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.pager).setCurrentItem(0, false) }
            settle()
            main { check(activity.findViewById<View>(R.id.calendarNext).parent != null) { "Calendar controls were lost after navigation" } }
            runOnMainSync { AppTextSize.save(targetContext, original) }
            result.putString("stream", "PASS: complete square grids, phase navigation, event details and date update at scale $requestedScale")
            finish(0, result)
        } catch (failure: Throwable) {
            val bitmap = uiAutomation.takeScreenshot()
            File(targetContext.getExternalFilesDir(null), "panchanga-failure.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
            AppTextSize.save(targetContext, original)
            result.putString("stream", android.util.Log.getStackTraceString(failure));finish(1,result)
        } finally { AppTextSize.save(targetContext, original) }
    }
}
