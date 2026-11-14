package com.paperdesk.launcher.ui

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import com.paperdesk.launcher.R

/** Exercises the actual Material dialog without writing any practice flags. */
class SandhyaFlowInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    private fun main(block: () -> Unit) {
        var error: Throwable? = null
        runOnMainSync { try { block() } catch (failure: Throwable) { error = failure } }
        error?.let { throw it }
    }
    override fun onStart() {
        val result = Bundle()
        var code = Activity.RESULT_OK
        var activity: Activity? = null
        try {
            val host = startActivitySync(Intent(targetContext, LauncherActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK).putExtra("page", "sandhya"))
            activity = host
            var ready = false
            repeat(40) {
                if (!ready) {
                    main { ready = host.findViewById<MaterialButton>(R.id.sandhyaRead)?.isShown == true }
                    if (!ready) SystemClock.sleep(250)
                }
            }
            check(ready) { "Sandhya entry did not load" }
            var confirmations = 0
            for (complete in listOf(false, true)) {
                for (marked in listOf(false, true)) {
                    lateinit var dialog: AlertDialog
                    main { dialog = SandhyaDayConfirmation.show(host, "Test session", complete, marked) { confirmations++ } }
                    waitForIdleSync()
                    check(confirmations == 0) { "Opening dialog invoked action" }
                    main { dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick() }
                    waitForIdleSync()
                    check(confirmations == 0) { "Cancel invoked action" }
                    main { SandhyaDayConfirmation.show(host, "Test session", complete, marked) { confirmations++ }.dismiss() }
                    waitForIdleSync()
                    check(confirmations == 0) { "Dismiss invoked action" }
                }
            }
            for (complete in listOf(false, true)) {
                lateinit var dialog: AlertDialog
                main { dialog = SandhyaDayConfirmation.show(host, "Test session", complete, false) { confirmations++ } }
                waitForIdleSync()
                main { dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick() }
                waitForIdleSync()
            }
            check(confirmations == 2) { "Confirm did not invoke exactly once per action" }
            main {
                check(host.findViewById<MaterialButton>(R.id.sandhyaRead).isEnabled)
                val previous = host.resources.getIdentifier("sandhyaPrevious", "id", host.packageName)
                val next = host.resources.getIdentifier("sandhyaNext", "id", host.packageName)
                check(previous == 0 || host.findViewById<android.view.View>(previous) == null)
                check(next == 0 || host.findViewById<android.view.View>(next) == null)
            }
            result.putString("stream", "PASS: Sandhya entry; cancel/dismiss are inert for complete/skip and undo; confirm callbacks run once; entry has no previous/next controls. No practice data changed.")
        } catch (error: Throwable) {
            code = Activity.RESULT_CANCELED
            result.putString("stream", android.util.Log.getStackTraceString(error))
        } finally {
            activity?.let { host -> main { host.finish() } }
        }
        finish(code, result)
    }
}
