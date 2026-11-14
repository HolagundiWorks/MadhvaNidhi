package com.paperdesk.launcher.ui

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.IndicScript
import com.paperdesk.launcher.data.OsLang

/** No day-status mutation until the explicit positive action; dismiss and cancel are inert. */
object SandhyaDayConfirmation {
    fun show(context: Context, session: String, complete: Boolean, marked: Boolean,
             onConfirmed: () -> Unit): AlertDialog {
        val english = OsLang.get(context) == IndicScript.Lang.EN
        val action = if (english) {
            if (complete) "Completed today" else "Skipped today"
        } else {
            if (complete) "ಇಂದು ಪೂರ್ಣ" else "ಇಂದು ಬಿಟ್ಟಿದೆ"
        }
        val title = if (marked) {
            if (english) "Remove this status?" else "ಈ ಸ್ಥಿತಿಯನ್ನು ತೆಗೆದುಹಾಕುವುದೇ?"
        } else {
            if (english) "$action?" else "$action ಎಂದು ಗುರುತಿಸುವುದೇ?"
        }
        val message = if (marked) {
            if (english) "$session\nRemove ‘$action’ for today's selected session?"
            else "$session\nಇಂದಿನ ಈ ಸಮಯದ ‘$action’ ಸ್ಥಿತಿಯನ್ನು ತೆಗೆದುಹಾಕುವುದೇ?"
        } else {
            if (english) "$session\nMark today's selected session as ‘$action’?"
            else "$session\nಇಂದಿನ ಈ ಸಮಯದ ಸಂಧ್ಯಾವಂದನೆಯನ್ನು ‘$action’ ಎಂದು ಗುರುತಿಸುವುದೇ?"
        }
        return MaterialAlertDialogBuilder(context)
            .setTitle(title).setMessage(message)
            .setNegativeButton(OsLang.ui("cancel", OsLang.get(context)), null)
            .setPositiveButton(if (english) "Confirm" else "ದೃಢೀಕರಿಸಿ") { _, _ -> onConfirmed() }
            .show().also { dialog ->
                val accent = context.getColor(R.color.md_theme_primary)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(accent)
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(accent)
            }
    }
}
