package com.paperdesk.launcher.ui

import android.app.Dialog
import android.view.ViewGroup
import android.view.WindowManager
import com.paperdesk.launcher.R

object DeskDialogs {
    fun show(dialog: Dialog, widthFraction: Float = 0.78f) {
        dialog.show()
        val dm = dialog.context.resources.displayMetrics
        val width = (dm.widthPixels * widthFraction).toInt().coerceAtMost(dm.widthPixels - 48)
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog)
    }
}
