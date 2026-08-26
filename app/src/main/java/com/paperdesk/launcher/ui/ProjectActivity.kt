package com.paperdesk.launcher.ui

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.paperdesk.launcher.DeskLock
import com.paperdesk.launcher.DeskWindow
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.ProjectStore

class ProjectActivity : AppCompatActivity() {

    private lateinit var projectId: String
    private lateinit var store: ProjectStore
    private var section = HOME

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (DeskLock.bounce(this)) return
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        setContentView(R.layout.activity_project)
        DeskWindow.padCutout(findViewById(R.id.projectRoot))
        DeskWindow.immersive(this)
        store = ProjectStore(this)
        projectId = intent.getStringExtra(EXTRA_ID).orEmpty()
        if (projectId.isBlank() || store.get(projectId) == null) {
            finish()
            return
        }
        findViewById<View>(R.id.projectBack).setOnClickListener { goBack() }
        findViewById<View>(R.id.projectEdit).setOnClickListener {
            startActivity(EntryUi.project(this, projectId))
        }
        showSection(savedInstanceState?.getString(EXTRA_SECTION) ?: intent.getStringExtra(EXTRA_SECTION) ?: HOME)
    }

    override fun onResume() {
        super.onResume()
        if (DeskLock.bounce(this)) return
        DeskWindow.immersive(this)
        val p = store.get(projectId)
        if (p == null) {
            finish()
            return
        }
        findViewById<TextView>(R.id.projectTitle).text = p.name
        DeskType.primary(findViewById(R.id.projectTitle))
        val info = p.number.ifBlank { "Project" }
        findViewById<TextView>(R.id.projectInfo).text = info
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) DeskWindow.immersive(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EXTRA_SECTION, section)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        goBack()
    }

    fun showSection(name: String) {
        section = name
        val frag: Fragment = when (name) {
            MEASURE -> MeasureFragment.newInstance(projectId)
            NOTES -> NotesFragment.newInstance(projectId)
            MINUTES -> MinutesFragment.newInstance(projectId)
            SCHEDULE -> ScheduleFragment.newInstance(projectId)
            TODO -> TasksFragment.newInstance(projectId)
            else -> {
                section = HOME
                ProjectHomeFragment.newInstance(projectId)
            }
        }
        supportFragmentManager.beginTransaction().replace(R.id.projectFrame, frag).commit()
    }

    private fun goBack() {
        if (section != HOME) showSection(HOME) else finish()
    }

    companion object {
        const val EXTRA_ID = "projectId"
        const val EXTRA_SECTION = "section"
        const val HOME = "home"
        const val MEASURE = "measure"
        const val NOTES = "notes"
        const val MINUTES = "minutes"
        const val SCHEDULE = "schedule"
        const val TODO = "todo"
    }
}
