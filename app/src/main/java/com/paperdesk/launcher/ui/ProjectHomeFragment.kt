package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.ProjectStore

class ProjectHomeFragment : Fragment() {

    private lateinit var projectId: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.page_project_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        projectId = arguments?.getString(ProjectActivity.EXTRA_ID).orEmpty()
        val host = requireActivity() as ProjectActivity
        view.findViewById<View>(R.id.cardMeasure).setOnClickListener { host.showSection(ProjectActivity.MEASURE) }
        view.findViewById<View>(R.id.cardNotes).setOnClickListener { host.showSection(ProjectActivity.NOTES) }
        view.findViewById<View>(R.id.cardNotesPhoto).setOnClickListener {
            startActivity(EntryUi.photoNote(requireContext(), projectId))
        }
        view.findViewById<View>(R.id.cardMinutes).setOnClickListener { host.showSection(ProjectActivity.MINUTES) }
        view.findViewById<View>(R.id.cardSchedule).setOnClickListener { host.showSection(ProjectActivity.SCHEDULE) }
        view.findViewById<View>(R.id.cardTodo).setOnClickListener { host.showSection(ProjectActivity.TODO) }
        val measureOn = com.paperdesk.launcher.data.SettingsStore(requireContext()).measureEnabled()
        view.findViewById<View>(R.id.cardMeasure).visibility = if (measureOn) View.VISIBLE else View.GONE
        paint()
    }

    override fun onResume() {
        super.onResume()
        if (::projectId.isInitialized) paint()
    }

    private fun paint() {
        val v = view ?: return
        val s = ProjectStore(requireContext()).summary(projectId)
        v.findViewById<TextView>(R.id.cardMeasureCount).text = "${s.measures} entries"
        v.findViewById<TextView>(R.id.cardNotesCount).text = "${s.notes} notes"
        v.findViewById<TextView>(R.id.cardMinutesCount).text = "${s.minutes} minutes"
        v.findViewById<TextView>(R.id.cardScheduleCount).text = "${s.upcoming} upcoming"
        v.findViewById<TextView>(R.id.cardTodoCount).text = "${s.pendingTodos} pending"
        listOf(R.id.cardMeasureCount, R.id.cardNotesCount, R.id.cardMinutesCount, R.id.cardScheduleCount, R.id.cardTodoCount).forEach {
            DeskType.meta(v.findViewById(it))
        }
    }

    companion object {
        fun newInstance(projectId: String) = ProjectHomeFragment().apply {
            arguments = Bundle().apply { putString(ProjectActivity.EXTRA_ID, projectId) }
        }
    }
}
