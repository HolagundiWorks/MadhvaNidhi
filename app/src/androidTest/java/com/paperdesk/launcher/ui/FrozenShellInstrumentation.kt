package com.paperdesk.launcher.ui

import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.paperdesk.launcher.DeskTheme
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.PanchangaCalendar
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

/** On-device geometry and interaction regression for the frozen native shell. */
class FrozenShellInstrumentation : Instrumentation() {
    private var scale=1f
    override fun onCreate(arguments: Bundle?) { scale=arguments?.getString("textScale")?.toFloatOrNull()?:1f;super.onCreate(arguments);start() }
    override fun onStart() {
        val result=Bundle();val active=AtomicReference<LauncherActivity>()
        val originalScale=AppTextSize.scale(targetContext)
        val originalTheme=DeskTheme.current(targetContext)
        val application=targetContext.applicationContext as Application
        val callbacks=object:Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(a:Activity){if(a is LauncherActivity)active.set(a)}
            override fun onActivityCreated(a:Activity,b:Bundle?)=Unit
            override fun onActivityStarted(a:Activity)=Unit
            override fun onActivityPaused(a:Activity)=Unit
            override fun onActivityStopped(a:Activity)=Unit
            override fun onActivitySaveInstanceState(a:Activity,b:Bundle)=Unit
            override fun onActivityDestroyed(a:Activity)=Unit
        }
        application.registerActivityLifecycleCallbacks(callbacks)
        fun main(block:()->Unit){var error:Throwable?=null;runOnMainSync{try{block()}catch(e:Throwable){error=e}};error?.let{throw it}}
        fun settle(){SystemClock.sleep(1800)}
        val captures=File(targetContext.getExternalFilesDir(null),"frozen-shell-review").apply{mkdirs()}
        fun capture(name:String){uiAutomation.takeScreenshot().let{b->File(captures,name).outputStream().use{b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};b.recycle()}}
        try {
            AppTextSize.save(targetContext,scale)
            main { targetContext.startActivity(Intent(Intent.ACTION_MAIN).apply{component=ComponentName(targetContext,LauncherActivity::class.java);addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)}) }
            settle()
            main { val a=active.get();a.navigateTo("desk") }
            settle()
            for(theme in listOf(DeskTheme.BLACK,DeskTheme.PAPER)) {
                if(DeskTheme.current(targetContext)!=theme){main{active.get().findViewById<MaterialToolbar>(R.id.toolbar).menu.performIdentifierAction(R.id.themeSwitch,0)};settle()}
                main {
                    val a=active.get()
                    check(a.getColor(R.color.carbon_background)==android.graphics.Color.parseColor(if(theme==DeskTheme.BLACK)"#161616" else "#FFFFFF")){"Wrong theme tokens"}
                    a.supportFragmentManager.setFragmentResult("panchangaDate",Bundle().apply{putString("date","2026-04-28")})
                };settle()
                for(phase in 0..1) {
                    main{active.get().findViewById<View>(if(phase==0)R.id.calendarPrevious else R.id.calendarNext).performClick()};settle()
                    main {
                        val a=active.get();val root=a.findViewById<View>(R.id.panchangaScreen)
                        val area=root.findViewById<View>(R.id.pakshaScroll);val bounds=Rect().also{area.getGlobalVisibleRect(it)}
                        val grid=root.findViewById<GridLayout>(if(phase==0)R.id.shuklaCalendarRow else R.id.krishnaCalendarRow)
                        check(grid.visibility==View.VISIBLE && grid.columnCount==5){"Wrong phase / column count"}
                        val cells=(0 until grid.childCount).map{grid.getChildAt(it) as FrameLayout}
                        check(cells.flatMap{(it.tag as PanchangaCalendar.Tile).tithis}==(1..15).toList()){"Missing tithis"}
                        for(cell in cells){
                            val tile=cell.getChildAt(0) as MaterialButton
                            val span=(cell.tag as PanchangaCalendar.Tile).tithis.size
                            val gap=root.resources.getDimensionPixelSize(R.dimen.carbon_spacing_02)
                            check(abs(cell.width-(cell.height*span+gap*(span-1)))<=1){"Non-square tile"}
                            val r=Rect().also{cell.getGlobalVisibleRect(it)}
                            check(r.height()==cell.height && r.width()==cell.width && bounds.contains(r)){"Clipped calendar tile ${tile.text}: cell=${cell.width}x${cell.height}, visible=$r area=$bounds root=${root.width} context=${root.findViewById<View>(R.id.panchangaContext).width} params=${root.findViewById<View>(R.id.panchangaContext).layoutParams.width}"}
                            val layout=tile.layout
                            check(layout!=null && layout.lineCount==3 && layout.height<=tile.height-tile.totalPaddingTop-tile.totalPaddingBottom){"Clipped tile text ${tile.text}: ${layout?.height} / ${tile.height-tile.totalPaddingTop-tile.totalPaddingBottom}"}
                            for(line in 0 until layout.lineCount)check(layout.getLineWidth(line)<=tile.width-tile.totalPaddingLeft-tile.totalPaddingRight+1){"Tile line overflow ${tile.text}"}
                            check(tile.text.toString().lines().last().any{it in '\u0C80'..'\u0CFF'}){"Tithi names missing"}
                        }
                        val left=root.findViewById<View>(R.id.panchangaContext)
                        val event=root.findViewById<View>(R.id.dayEventsCard)
                        check(event.height>0 && event.bottom<=left.height){"Event section clipped: ${event.bottom}/${left.height}"}
                        check(root.findViewById<TextView>(R.id.selectedDateText).text.contains("28")){"Selected date lost"}
                    }
                    capture("$theme-phase-$phase-scale-$scale.png")
                }
                main{active.get().findViewById<View>(R.id.sidePanelToggle).performClick();check(active.get().findViewById<View>(R.id.sidePanel).visibility==View.VISIBLE)}
                capture("$theme-panel-scale-$scale.png")
                main{active.get().findViewById<View>(R.id.sidePanelBackdrop).performClick();check(active.get().findViewById<View>(R.id.sidePanel).visibility==View.GONE)}
                main{active.get().findViewById<MaterialToolbar>(R.id.toolbar).menu.performIdentifierAction(R.id.toolbarDate,0)};settle()
                main{val a=active.get();val picker=a.supportFragmentManager.findFragmentByTag("panchanga-picker");check(picker is MaterialDatePicker<*>){"Calendar picker missing"};picker.dismiss()};settle()
                main{active.get().findViewById<View>(R.id.selectedDateText).performClick()};settle()
                check(uiAutomation.rootInActiveWindow.findAccessibilityNodeInfosByText("ಸರಿ").isNotEmpty()){"Date details missing"}
                uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK);settle()
            }
            main{
                val a=active.get();val event=a.findViewById<TextView>(R.id.dayEvent);event.text=("ದಿನದ ವಿಶೇಷ ಕಾರ್ಯಕ್ರಮ ವಿವರಗಳು\n").repeat(60)
            };settle()
            main{val scroll=active.get().findViewById<ScrollView>(R.id.eventScroll);check(scroll.canScrollVertically(1)&&!scroll.isVerticalScrollBarEnabled){"Event scrolling / hidden scrollbar failed"}}
            result.putString("stream","PASS: frozen shell, five-column square grids, text bounds, both themes, side panel, Material calendar, date details and event scrolling at app scale $scale")
            finish(0,result)
        }catch(e:Throwable){capture("failure-scale-$scale.png");result.putString("stream",android.util.Log.getStackTraceString(e));finish(1,result)}
        finally{AppTextSize.save(targetContext,originalScale);application.unregisterActivityLifecycleCallbacks(callbacks);runOnMainSync{DeskTheme.set(targetContext,originalTheme)}}
    }
}
