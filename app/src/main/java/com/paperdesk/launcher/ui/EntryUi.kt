package com.paperdesk.launcher.ui

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.WindowManager
import com.paperdesk.launcher.DeskWindow
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.OsLang
import java.util.Calendar

object EntryUi {
    const val EXTRA_PROJECT_ID = "projectId"

    fun prepare(activity: Activity) {
        DeskWindow.cutout(activity)
        activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        activity.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        DeskWindow.padCutout(activity.findViewById(android.R.id.content))
    }

    fun dateLabel(cal: Calendar): String = OsLang.dateLine(cal, com.paperdesk.launcher.data.IndicScript.Lang.EN)

    fun dateLabel(context: Context, cal: Calendar): String =
        OsLang.dateLine(cal, OsLang.get(context))

    fun timeLabel(cal: Calendar): String =
        String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))

    fun task(context: Context, id: String? = null, projectId: String? = null): Intent {
        return Intent(context, TaskEntryActivity::class.java).apply {
            if (id != null) putExtra(TaskEntryActivity.EXTRA_ID, id)
            if (!projectId.isNullOrBlank()) putExtra(EXTRA_PROJECT_ID, projectId)
        }
    }

    fun measure(context: Context, id: String? = null, projectId: String? = null): Intent {
        return Intent(context, MeasureEntryActivity::class.java).apply {
            if (id != null) putExtra(MeasureEntryActivity.EXTRA_ID, id)
            if (!projectId.isNullOrBlank()) putExtra(EXTRA_PROJECT_ID, projectId)
        }
    }

    fun schedule(context: Context, id: String? = null, dayStart: Long? = null, projectId: String? = null): Intent {
        return Intent(context, ScheduleEntryActivity::class.java).apply {
            if (id != null) putExtra(ScheduleEntryActivity.EXTRA_ID, id)
            if (dayStart != null) putExtra(ScheduleEntryActivity.EXTRA_DAY, dayStart)
            if (!projectId.isNullOrBlank()) putExtra(EXTRA_PROJECT_ID, projectId)
        }
    }

    fun note(context: Context, projectId: String = "", id: String? = null): Intent {
        return Intent(context, NoteEntryActivity::class.java).apply {
            putExtra(EXTRA_PROJECT_ID, projectId)
            if (id != null) putExtra(NoteEntryActivity.EXTRA_ID, id)
        }
    }

    fun photoNote(context: Context, projectId: String): Intent {
        return Intent(context, NotePhotoActivity::class.java).apply {
            putExtra(EXTRA_PROJECT_ID, projectId)
        }
    }

    fun minutes(context: Context, projectId: String, id: String? = null): Intent {
        return Intent(context, MeetingEntryActivity::class.java).apply {
            putExtra(EXTRA_PROJECT_ID, projectId)
            if (id != null) putExtra(MeetingEntryActivity.EXTRA_ID, id)
        }
    }

    fun project(context: Context, id: String? = null): Intent {
        return Intent(context, ProjectEntryActivity::class.java).apply {
            if (id != null) putExtra(ProjectEntryActivity.EXTRA_ID, id)
        }
    }

    fun openProject(context: Context, projectId: String, section: String = ProjectActivity.HOME): Intent {
        return Intent(context, ProjectActivity::class.java).apply {
            putExtra(ProjectActivity.EXTRA_ID, projectId)
            putExtra(ProjectActivity.EXTRA_SECTION, section)
        }
    }

    fun pickDate(activity: Activity, cal: Calendar, onPicked: () -> Unit) {
        DatePickerDialog(
            activity,
            R.style.Theme_PaperDesk_Dialog,
            { _, y, m, d ->
                cal.set(Calendar.YEAR, y)
                cal.set(Calendar.MONTH, m)
                cal.set(Calendar.DAY_OF_MONTH, d)
                onPicked()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun pickTime(activity: Activity, cal: Calendar, onPicked: () -> Unit) {
        TimePickerDialog(
            activity,
            R.style.Theme_PaperDesk_Dialog,
            { _, h, min ->
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, min)
                onPicked()
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        ).show()
    }
}
