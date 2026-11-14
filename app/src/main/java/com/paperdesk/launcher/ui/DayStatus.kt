package com.paperdesk.launcher.ui

import androidx.annotation.ColorRes
import com.paperdesk.launcher.R

/** Shared practice-day status colours (dashboard week + dialogs). */
enum class DayStatus {
    COMPLETE,
    SKIPPED,
    PENDING,
    FUTURE;

    @get:ColorRes
    val accent: Int
        get() = when (this) {
            COMPLETE -> R.color.md_support_success
            SKIPPED -> R.color.md_support_error
            PENDING, FUTURE -> R.color.md_border_strong
        }

    companion object {
        fun sandhyaDay(done: Int, future: Boolean, pendingToday: Boolean): DayStatus = when {
            future -> FUTURE
            done == 3 -> COMPLETE
            pendingToday || done == 0 -> PENDING
            else -> SKIPPED
        }
    }
}
