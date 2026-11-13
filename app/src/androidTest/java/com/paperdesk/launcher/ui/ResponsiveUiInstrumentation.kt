package com.paperdesk.launcher.ui

import android.app.Activity
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.paperdesk.launcher.R
import java.io.File

/** Visits all Material screens; repeat using adb display/font overrides. */
class ResponsiveUiInstrumentation : Instrumentation() {
    private var expectedDashboardColumns = 0
    private var requestedScale = 1f
    override fun onCreate(arguments: Bundle?) {
        expectedDashboardColumns = arguments?.getString("dashboardColumns")?.toIntOrNull() ?: 0
        requestedScale = arguments?.getString("textScale")?.toFloatOrNull() ?: 1f
        super.onCreate(arguments)
        start()
    }
    @Suppress("DEPRECATION")
    override fun onStart() {
        val power = targetContext.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        val awake = power.newWakeLock(android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK or android.os.PowerManager.ON_AFTER_RELEASE, "MadhvaNidhi:ui-audit")
        awake.acquire(180_000L)

        val result = Bundle()
        var launcher: LauncherActivity? = null
        val originalScale = AppTextSize.scale(targetContext)
        AppTextSize.save(targetContext, requestedScale)
        try {
            val startupMonitor = addMonitor(LauncherActivity::class.java.name, null, false)
            launcher = startActivitySync(Intent(Intent.ACTION_MAIN).apply {
                component = ComponentName(targetContext, LauncherActivity::class.java)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }) as LauncherActivity
            for (attempt in 0 until 20) {
                waitForIdleSync()
                SystemClock.sleep(150)
                val candidate = startupMonitor.lastActivity as? LauncherActivity ?: requireNotNull(launcher)
                var ready = false
                onMain { ready = !candidate.isDestroyed && !candidate.isFinishing && candidate.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE }
                if (ready) { launcher = candidate; break }
            }
            removeMonitor(startupMonitor)
            val activity = requireNotNull(launcher)
            onMain {
                check(com.google.android.material.color.MaterialColors.getColor(activity, com.google.android.material.R.attr.colorPrimary, 0) == android.graphics.Color.parseColor("#FF832B")) { "Orange accent is missing" }
                check(com.google.android.material.color.MaterialColors.getColor(activity, com.google.android.material.R.attr.colorSurface, 0) == android.graphics.Color.parseColor("#161616")) { "Carbon background is missing" }
                check(activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) { "App is not landscape" }
                val expected = targetContext.resources.configuration.fontScale * requestedScale
                check(kotlin.math.abs(activity.resources.configuration.fontScale - expected) < 0.05f) { "App text scaling was not applied" }
            }
            val captures = File(targetContext.getExternalFilesDir(null), "ui-audit").apply { mkdirs() }
            var pages = 0
            onMain { pages = activity.findViewById<ViewPager2>(R.id.pager).adapter!!.itemCount }
            repeat(pages) { index ->
                onMain { activity.findViewById<ViewPager2>(R.id.pager).setCurrentItem(index, false) }
                settle()
                lateinit var current: androidx.fragment.app.Fragment
                onMain {
                    val pager = activity.findViewById<View>(R.id.pager)
                    val toolbar = activity.findViewById<MaterialToolbar>(R.id.toolbar)
                    val bounds = Rect().also { pager.getGlobalVisibleRect(it) }
                    val bar = Rect().also { toolbar.getGlobalVisibleRect(it) }
                    check(bounds.width() > 0 && bounds.height() > 0) { "Page $index has no content area" }
                    check(bounds.top >= bar.bottom) { "Page $index overlaps Material toolbar" }
                    inspectChrome(toolbar)
                    inspectChrome(activity.findViewById(R.id.bottomNavigation))
                    current = activity.supportFragmentManager.fragments.first { it.isResumed }
                    inspect(current.requireView(), index)

                }
                capture(captures, "page-$index")
                if (index == 1) {
                    onMain {
                        val root = current.requireView()
                        val group = root.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.readerKalaField)
                        val original = group.checkedButtonId
                        val bodies = mutableSetOf<String>()
                        for (id in listOf(R.id.kalaMorning, R.id.kalaNoon, R.id.kalaEvening)) {
                            val button = root.findViewById<com.google.android.material.button.MaterialButton>(id)
                            check(button.icon != null && !button.contentDescription.isNullOrBlank()) { "Time-of-day icon is missing its label" }
                            button.performClick()
                            check(group.checkedButtonId == id) { "Time-of-day selection failed" }
                            bodies += root.findViewById<TextView>(R.id.readerBody).text.toString()
                        }
                        check(bodies.size == 3) { "Morning/noon/evening did not select different readings" }
                        root.findViewById<View>(original).performClick()
                    }
                    settle()
                }
                if (index == 7) {
                    onMain {
                        check(current.requireView().findViewById<View>(R.id.aayadiLengthFt) is com.google.android.material.textfield.TextInputEditText) { "Feet should use numeric entry" }
                        check(current.requireView().findViewById<View>(R.id.aayadiLengthIn) is MaterialAutoCompleteTextView) { "Inches should use bounded dropdown choices" }
                        current.requireView().findViewById<View>(R.id.aayadiCalculate).performClick()
                    }
                    settle()
                    onMain {
                        val root = current.requireView()
                        val viewport = root.findViewById<ViewGroup>(R.id.contentViewport)
                        val document = viewport.getChildAt(0) as ViewGroup
                        val title = root.findViewById<TextView>(R.id.aayadiResultTitle)
                        val rect = Rect(0, 0, title.width, title.height)
                        document.offsetDescendantRectToMyCoords(title, rect)
                        val top = -document.translationY.toInt()
                        val bottom = top + viewport.clipBounds!!.height()
                        check(glyphs(title, 0).all { rect.top + title.totalPaddingTop + it.first >= top && rect.top + title.totalPaddingTop + it.last <= bottom }) { "Calculate did not reveal results" }
                    }
                    capture(captures, "aayadi-calculated")
                    onMain { ScreenPages.first(current.requireView()) }
                }
                if (index == 0) {
                    var oldHeader = ""
                    var expectedPaksha = ""
                    onMain {
                        val home = current.requireView()
                        oldHeader = home.findViewById<TextView>(R.id.panchangaHeader).text.toString()
                        check(oldHeader.contains("ಶಾಲಿವಾಹನ")) { "Dynamic Panchanga header is missing" }
                        val shukla = home.findViewById<android.widget.GridLayout>(R.id.shuklaCalendarRow)
                        val krishna = home.findViewById<android.widget.GridLayout>(R.id.krishnaCalendarRow)
                        check(listOf(shukla, krishna).all { grid -> (0 until grid.childCount).flatMap { (grid.getChildAt(it).tag as com.paperdesk.launcher.data.PanchangaCalendar.Tile).tithis } == (1..15).toList() }) { "Paksha grids must retain all 15 tithis" }
                        val opposite = if (oldHeader.contains("ಶುಕ್ಲಪಕ್ಷ")) krishna else shukla
                        expectedPaksha = if (opposite === krishna) "ಕೃಷ್ಣಪಕ್ಷ" else "ಶುಕ್ಲಪಕ್ಷ"
                        (0 until opposite.childCount).map { opposite.getChildAt(it) }.first { it.isEnabled && (it.tag as com.paperdesk.launcher.data.PanchangaCalendar.Tile).dates.size == 1 }.performClick()
                    }
                    settle()
                    onMain {
                        val header = current.requireView().findViewById<TextView>(R.id.panchangaHeader).text.toString()
                        check(header != oldHeader && header.contains(expectedPaksha)) { "Selecting a tithi did not update the header's Paksha" }
                        inspect(current.requireView(), index)
                    }
                    capture(captures, "calendar-paksha-selection")
                    onMain { current.requireView().findViewById<View>(R.id.todayDate).performClick() }
                    settle()
                }
                var previousRitual = 0
                onMain {
                    if (current is NityaKarmaFragment) {
                        val ritual = current.requireView().findViewById<MaterialAutoCompleteTextView>(R.id.japaMantra)
                        if (ritual != null && ritual.adapter != null && ritual.adapter.count > 6) {
                            previousRitual = MaterialChoices.position(ritual)
                            ritual.setText(ritual.adapter.getItem(6).toString(), false)
                            ritual.onItemClickListener?.onItemClick(null, null, 6, 6)
                        }
                    }
                    if (current is ReaderFragment) (current as ReaderFragment).openSelector()
                }
                settle()
                if (current is ReaderFragment) {
                    capture(captures, "page-$index-selector")
                    onMain {
                        (current as ReaderFragment).closeSelector()
                        check(current.requireView().findViewById<MaterialAutoCompleteTextView>(R.id.readerItem)?.isPopupShowing != true) { "Reading dropdown stayed open" }
                    }
                    settle()
                }
                val originalLabels = mutableListOf<Pair<MaterialAutoCompleteTextView, String>>()
                if (requestedScale >= 2f || targetContext.resources.configuration.fontScale >= 2f) {
                    onMain {
                        fun longest(view: View) {
                            if (view.visibility != View.VISIBLE) return
                            if (view is MaterialAutoCompleteTextView && view.adapter != null && view.adapter.count > 0) {
                                val label = (0 until view.adapter.count).map { view.adapter.getItem(it).toString() }
                                    .maxByOrNull { view.paint.measureText(it) }.orEmpty()
                                originalLabels += view to view.text.toString()
                                view.setText(label, false)
                            }
                            if (view is ViewGroup) for (i in 0 until view.childCount) longest(view.getChildAt(i))
                        }
                        longest(current.requireView())
                    }
                    settle()
                }
                onMain { inspect(current.requireView(), index); verifyContentPages(current.requireView(), index) }
                settle()
                capture(captures, "page-$index-expanded")
                onMain { originalLabels.forEach { (field, original) -> field.setText(original, false) } }
                settle()
                onMain {
                    if (current is NityaKarmaFragment) {
                        val ritual = current.requireView().findViewById<MaterialAutoCompleteTextView>(R.id.japaMantra)
                        if (ritual != null && ritual.adapter != null) {
                            MaterialChoices.select(ritual, previousRitual)
                            ritual.onItemClickListener?.onItemClick(null, null, previousRitual, previousRitual.toLong())
                        }
                    }
                    if (current is ReaderFragment) (current as ReaderFragment).closeSelector()
                }
            }
            onMain { activity.goHome() }
            settle()
            onMain { activity.findViewById<View>(R.id.chooseDate).performClick() }
            settle()
            onMain {
                val picker = activity.supportFragmentManager.findFragmentByTag("date") as androidx.fragment.app.DialogFragment
                inspect(picker.requireDialog().window!!.decorView, 0)
                inspectDialogWindow(picker.requireDialog().window!!.decorView)
            }
            capture(captures, "material-date-picker")
            onMain {
                val date = activity.supportFragmentManager.findFragmentByTag("date") as DateEntryDialog
                val dialog = date.requireDialog() as androidx.appcompat.app.AlertDialog
                dialog.findViewById<TextView>(R.id.dateEntryValue)!!.text = "2026-02-30"
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).performClick()
                check(!dialog.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.dateEntryField)!!.error.isNullOrEmpty()) { "Invalid dates are accepted" }
            }
            settle()
            onMain {
                val date = activity.supportFragmentManager.findFragmentByTag("date") as DateEntryDialog
                inspectDialogWindow(date.requireDialog().window!!.decorView)
                val dialog = date.requireDialog() as androidx.appcompat.app.AlertDialog
                dialog.findViewById<TextView>(R.id.dateEntryValue)!!.text = "2026-04-20"
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).performClick()
            }
            settle()
            onMain {
                val root = activity.supportFragmentManager.fragments.first { it.isResumed }.requireView()
                val cells = listOf(R.id.shuklaCalendarRow, R.id.krishnaCalendarRow).flatMap { id ->
                    val row = root.findViewById<android.widget.GridLayout>(id)
                    (0 until row.childCount).map { row.getChildAt(it) as com.google.android.material.button.MaterialButton }
                }
                check(cells.any { (it.tag as List<*>).contains("2026-04-20") && it.isChecked }) { "Date entry did not select the requested calendar date" }
                root.findViewById<View>(R.id.todayDate).performClick()
            }
            settle()
            onMain {
                val bottom = activity.findViewById<View>(R.id.bottomNavigation)
                val pager = activity.findViewById<View>(R.id.pager)
                check(bottom.top >= pager.bottom) { "Navigation is not below the content" }
                val choices = activity.findViewById<MaterialAutoCompleteTextView>(R.id.pageDestination)
                check(choices.adapter.count == 8) { "Settings is duplicated in destinations" }
                MaterialChoices.select(choices, 1)
                choices.onItemClickListener?.onItemClick(null, null, 1, 1)
            }
            settle()
            onMain { check(activity.findViewById<ViewPager2>(R.id.pager).currentItem == 1) { "Bottom dropdown did not navigate" } }
            capture(captures, "material-navigation")
            onMain { activity.findViewById<MaterialToolbar>(R.id.toolbar).menu.performIdentifierAction(R.id.openSettings, 0) }
            settle()
            onMain {
                check(activity.findViewById<ViewPager2>(R.id.pager).currentItem == 8) { "Material navigation did not open settings" }
            }
            val monitor = addMonitor(LauncherActivity::class.java.name, null, false)
            val nextScale = if (requestedScale < 2f) requestedScale + 0.25f else requestedScale - 0.25f
            onMain {
                activity.findViewById<MaterialToolbar>(R.id.toolbar).menu.performIdentifierAction(if (requestedScale < 2f) R.id.textLarger else R.id.textSmaller, 0)
            }
            val resized = monitor.waitForActivityWithTimeout(8000) as? LauncherActivity
                ?: error("Text-size button did not recreate the screen")
            launcher = resized
            removeMonitor(monitor)
            settle()
            onMain {
                check(kotlin.math.abs(AppTextSize.scale(targetContext) - nextScale) < 0.01f) { "Text-size button did not save preference" }
                val expected = targetContext.resources.configuration.fontScale * nextScale
                check(kotlin.math.abs(resized.resources.configuration.fontScale - expected) < 0.05f) { "Recreated screen did not scale text" }
                val displayedScale = resized.findViewById<com.google.android.material.slider.Slider>(R.id.settingsTextSize).value
                check(kotlin.math.abs(displayedScale - nextScale * 100f) < 1f) { "Text-size slider restored an obsolete value" }
                check(resized.findViewById<TextView>(R.id.settingsTextSizeLabel).text.contains("${(nextScale * 100f).toInt()}%")) { "Text-size label is out of sync" }
                check(resized.findViewById<ViewPager2>(R.id.pager).currentItem == 8) { "Changing text size lost the Settings page" }
                inspectChrome(resized.findViewById(R.id.toolbar))
                inspectChrome(resized.findViewById(R.id.bottomNavigation))
                inspect(resized.supportFragmentManager.fragments.first { it.isResumed }.requireView(), 8)
            }
            capture(captures, "material-text-size")
            AppTextSize.save(targetContext, originalScale)
            result.putString("stream", "PASS: 9 Material pages in landscape; toolbar clearance; text height; no custom views; japa; morning/noon/evening selection; numeric inputs; selectors; date picker; bottom dropdown; line-safe content pages; app text scale $requestedScale; A+/A- persistence and recreation.")
            onMain { resized.finish() }
            if (awake.isHeld) awake.release()
            finish(Activity.RESULT_OK, result)
        } catch (error: Throwable) {
            capture(File(targetContext.getExternalFilesDir(null), "ui-audit"), "failure")
            AppTextSize.save(targetContext, originalScale)
            result.putString("stream", "FAILED: ${error.message}\n${error.stackTraceToString()}")
            launcher?.let { onMain { it.finish() } }
            if (awake.isHeld) awake.release()
            finish(Activity.RESULT_CANCELED, result)
        }
    }
    private fun onMain(action: () -> Unit) {
        var failure: Throwable? = null
        super.runOnMainSync { try { action() } catch (error: Throwable) { failure = error } }
        failure?.let { throw it }
    }
    private fun settle() { waitForIdleSync(); SystemClock.sleep(600) }
    private fun capture(directory: File, name: String) {
        uiAutomation.takeScreenshot()?.let { bitmap ->
            File(directory, "$name.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
    private fun verifyContentPages(root: View, page: Int) {
        // The native calendar uses complete paksha grids instead of document pagination.
        if (root.findViewById<View>(R.id.panchangaScreen) != null) return
        val viewport = root.findViewById<ViewGroup>(R.id.contentViewport) ?: error("Page $page has no paged viewport")
        val controls = requireNotNull(ScreenPages.controls(root))
        val next = controls.findViewById<View>(R.id.contentNext)
        val previous = controls.findViewById<View>(R.id.contentPrevious)
        val content = viewport.getChildAt(0) as ViewGroup
        ScreenPages.first(root.findViewById<View>(R.id.readerScroll) ?: root)
        var count = 0
        do {
            val top = -content.translationY.toInt()
            val bottom = top + (viewport.clipBounds?.height() ?: viewport.height)
            check(bottom - top <= viewport.height) { "Page $page exceeds the viewport" }
            fun lines(view: View) {
                if (view.visibility != View.VISIBLE) return
                if (view.tag == "section" && view.height <= viewport.height) {
                    val rect = Rect(0, 0, view.width, view.height)
                    content.offsetDescendantRectToMyCoords(view, rect)
                    if (rect.top < bottom && rect.bottom > top) {
                        check(rect.top >= top && rect.bottom <= bottom) { "Page $page separates a section heading from its content" }
                    }
                }
                if (view is NestedScrollView || view is android.widget.HorizontalScrollView || view is androidx.recyclerview.widget.RecyclerView) {
                    error("Page $page still has a scrolling content view")
                }
                if (view is TextView && view.text.isNotBlank() && view.layout != null) {
                    val rect = Rect(0, 0, view.width, view.height)
                    content.offsetDescendantRectToMyCoords(view, rect)
                    val start = rect.top + view.totalPaddingTop
                    for (line in 0 until view.layout.lineCount) {
                        for (ink in glyphs(view, line)) {
                            val a = start + ink.first
                            val b = start + ink.last
                            if (a < bottom && b > top) {
                                check(a >= top && b <= bottom) { "Page $page content ${count + 1} cuts drawn glyphs [${view.text.take(50)}]: $a..$b, viewport $top..$bottom; line=$line" }
                            }
                        }
                    }
                }
                if (view is ViewGroup) for (i in 0 until view.childCount) lines(view.getChildAt(i))
            }
            lines(content)
            count++
            check(count < 2000) { "Content paging does not terminate" }
            val more = next.isEnabled
            if (more) next.performClick()
        } while (more)
        check(!next.isEnabled) { "Last content page is not reachable" }
        if (count > 1) { check(previous.isEnabled); previous.performClick(); check(next.isEnabled) }
        ScreenPages.first(root.findViewById<View>(R.id.readerScroll) ?: root)
    }
    private fun glyphs(view: TextView, line: Int): List<IntRange> {
        val text = view.text
        val layout = view.layout
        val end = layout.getLineEnd(line).coerceAtMost(text.length)
        var start = layout.getLineStart(line)
        val bounds = mutableListOf<IntRange>()
        while (start < end) {
            val next = if (text is android.text.Spanned) text.nextSpanTransition(start, end, android.text.style.MetricAffectingSpan::class.java) else end
            val paint = android.text.TextPaint(view.paint)
            if (text is android.text.Spanned) text.getSpans(start, next, android.text.style.MetricAffectingSpan::class.java).forEach { it.updateMeasureState(paint) }
            val value = text.subSequence(start, next).toString().trimEnd('\n', '\r')
            if (value.isNotBlank()) {
                val ink = Rect()
                paint.getTextBounds(value, 0, value.length, ink)
                val baseline = layout.getLineBaseline(line) + paint.baselineShift
                bounds += (baseline + ink.top)..(baseline + ink.bottom)
            }
            start = next
        }
        return bounds
    }

    private fun inspectDialogWindow(view: View) {
        if (view.visibility != View.VISIBLE) return
        if (view is TextView && view.text.isNotBlank() && view.layout != null) {
            val visible = Rect()
            check(view.getGlobalVisibleRect(visible)) { "Dialog text is outside its window" }
            val location = IntArray(2)
            view.getLocationInWindow(location)
            for (line in 0 until view.layout.lineCount) {
                val value = view.text.subSequence(view.layout.getLineStart(line), view.layout.getLineEnd(line).coerceAtMost(view.text.length)).toString().trim()
                if (value.isBlank()) continue
                val ink = Rect()
                view.paint.getTextBounds(value, 0, value.length, ink)
                val baseline = location[1] + view.totalPaddingTop + view.layout.getLineBaseline(line)
                check(baseline + ink.top >= visible.top && baseline + ink.bottom <= visible.bottom) { "Dialog clips [${view.text}]: ${baseline + ink.top}..${baseline + ink.bottom}, window=$visible" }
            }
        }
        if (view is ViewGroup) for (i in 0 until view.childCount) inspectDialogWindow(view.getChildAt(i))
    }
    private fun inspectChrome(view: View) {
        if (view.visibility != View.VISIBLE) return
        if (view is TextView && view.text.isNotBlank() && view.layout != null) {
            check(view.height - view.compoundPaddingTop - view.compoundPaddingBottom >= view.layout.height) {
                "Navigation chrome clips [${view.text}]: ${view.height} vs ${view.layout.height}"
            }
        }
        if (view is ViewGroup) for (i in 0 until view.childCount) inspectChrome(view.getChildAt(i))
    }
    private fun inspect(view: View, page: Int) {
        if (view.visibility != View.VISIBLE) return
        check(!view.javaClass.name.startsWith("com.paperdesk.")) { "Page $page still uses custom view ${view.javaClass.name}" }
        if (view is TextView && view.text.isNotBlank() && view.maxLines != 1 && view.ellipsize == null) {
            val textHeight = view.layout?.height ?: 0
            check(view.height - view.compoundPaddingTop - view.compoundPaddingBottom >= textHeight) {
                "Page $page clips ${if (view.id == View.NO_ID) view.javaClass.simpleName else view.resources.getResourceEntryName(view.id)} [${view.text.take(100)}]: text=$textHeight view=${view.height} parent=${view.parent.javaClass.simpleName}"
            }
        }
        val radius = 4f * view.resources.displayMetrics.density
        if (view is com.google.android.material.card.MaterialCardView) {
            check(kotlin.math.abs(view.radius - radius) <= 1f) { "Card does not use 4dp corners" }
        }
        if (view is com.google.android.material.button.MaterialButton) {
            check(kotlin.math.abs(view.cornerRadius - radius) <= 1f) { "Button ${if (view.id == View.NO_ID) view.javaClass.simpleName else view.resources.getResourceEntryName(view.id)} has radius ${view.cornerRadius}, expected $radius" }
        }
        if (view is android.widget.GridLayout) {
            val children = (0 until view.childCount).map { view.getChildAt(it) }.filter { it.visibility != View.GONE }
            for ((index, child) in children.withIndex()) {
                check(child.left >= view.paddingLeft && child.right <= view.width - view.paddingRight) { "Page $page grid ${view.resources.getResourceEntryName(view.id)} child ${child.id} exceeds width: ${child.left}..${child.right}, grid=${view.width}" }
                if (child is com.google.android.material.card.MaterialCardView) {
                    for (other in children.drop(index + 1).filter { it.top == child.top && it is com.google.android.material.card.MaterialCardView }) {
                        check(child.height == other.height) { "Page $page side-by-side cards have different heights" }
                    }
                }
                val bounds = Rect(child.left, child.top, child.right, child.bottom)
                for (other in children.drop(index + 1)) {
                    check(!Rect.intersects(bounds, Rect(other.left, other.top, other.right, other.bottom))) { "Page $page grid panels overlap" }
                }
            }
        }
        if (view is ViewGroup) for (i in 0 until view.childCount) inspect(view.getChildAt(i), page)
    }
}
