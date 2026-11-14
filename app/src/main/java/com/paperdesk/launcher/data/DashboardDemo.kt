package com.paperdesk.launcher.data

import android.content.Context

/** @deprecated Prefer [DemoUser.ensure]. Kept for call-site compatibility. */
object DashboardDemo {
    fun seedIfEmpty(context: Context, practice: PracticeDayStore, japa: JapaStore) {
        DemoUser.ensure(context)
    }
}
