package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.FestivalStore
import com.paperdesk.launcher.data.JapaStore
import com.paperdesk.launcher.data.MadhwaDays
import com.paperdesk.launcher.data.OsLang
import com.paperdesk.launcher.data.PanchangaStore
import com.paperdesk.launcher.data.PracticeDayStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class DashboardFragment : Fragment() {
    private lateinit var japa: JapaStore
    private lateinit var practice: PracticeDayStore

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.page_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        japa = JapaStore(requireContext())
        practice = PracticeDayStore(requireContext())
        bindPanchanga(view)
        bindJapa(view)
        bindSandhya(view)
        bindStotra(view)
        view.findViewById<MaterialButton>(R.id.dashboardOpenPanchanga).setOnClickListener {
            (activity as? LauncherActivity)?.navigateTo("desk")
        }
        view.findViewById<MaterialButton>(R.id.dashboardQuickPanchanga).setOnClickListener {
            (activity as? LauncherActivity)?.navigateTo("desk")
        }
        view.findViewById<MaterialButton>(R.id.dashboardOpenJapa).setOnClickListener {
            (activity as? LauncherActivity)?.navigateTo("japa")
        }
        view.findViewById<MaterialButton>(R.id.dashboardOpenSandhya).setOnClickListener {
            (activity as? LauncherActivity)?.navigateTo("sandhya")
        }
        view.findViewById<MaterialButton>(R.id.dashboardOpenGrantha).setOnClickListener {
            (activity as? LauncherActivity)?.navigateTo("grantha")
        }
        view.findViewById<MaterialButton>(R.id.dashboardQuickHistory).setOnClickListener {
            (activity as? LauncherActivity)?.navigateTo("japa")
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let {
            bindJapa(it)
            bindSandhya(it)
            bindStotra(it)
            bindPanchanga(it)
        }
    }

    private fun bindPanchanga(view: View) {
        val lang = OsLang.get(requireContext())
        val date = PanchangaStore.dateKey()
        val cal = Calendar.getInstance().apply {
            val parts = date.split("-")
            if (parts.size == 3) set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        }
        view.findViewById<TextView>(R.id.dashboardDate).text = OsLang.dateLine(cal, lang)
        viewLifecycleOwner.lifecycleScope.launch {
            val day = withContext(Dispatchers.IO) { PanchangaStore.forDate(requireContext(), date) }
            if (!isAdded) return@launch
            val tithi = listOf(day.tithi, day.paksha).filter { it.isNotBlank() }.joinToString(" · ")
            view.findViewById<TextView>(R.id.dashboardTithi).text =
                OsLang.script(tithi.ifBlank { "—" }, lang)
            view.findViewById<TextView>(R.id.dashboardNakshatra).text =
                "ನಕ್ಷತ್ರ · " + OsLang.script(day.nakshatra.ifBlank { "—" }, lang)
            val fest = FestivalStore.label(date, lang).ifBlank {
                MadhwaDays.name(day.masa, day.paksha, day.tithi)
            }
            view.findViewById<TextView>(R.id.dashboardObservance).text = fest
            view.findViewById<TextView>(R.id.dashboardObservance).visibility =
                if (fest.isBlank()) View.GONE else View.VISIBLE
        }
    }

    private fun bindJapa(view: View) {
        val state = japa.state()
        view.findViewById<TextView>(R.id.dashboardJapaCount).text = state.current.toString()
        val malas = if (state.target > 0) state.current / state.target else 0
        view.findViewById<TextView>(R.id.dashboardJapaMeta).text =
            getString(R.string.dashboard_japa_meta, state.current, malas, state.target)
        view.findViewById<MaterialButton>(R.id.dashboardJapaInc).setOnClickListener {
            japa.add(1)
            bindJapa(view)
        }
        view.findViewById<MaterialButton>(R.id.dashboardJapaUndo).setOnClickListener {
            japa.undo()
            bindJapa(view)
        }
    }

    private fun bindSandhya(view: View) {
        val (pratah, madhya, sayam) = practice.sandhya()
        val pratahBox = view.findViewById<MaterialCheckBox>(R.id.dashboardSandhyaPratah)
        val madhyaBox = view.findViewById<MaterialCheckBox>(R.id.dashboardSandhyaMadhyahna)
        val sayamBox = view.findViewById<MaterialCheckBox>(R.id.dashboardSandhyaSayam)
        pratahBox.setOnCheckedChangeListener(null)
        madhyaBox.setOnCheckedChangeListener(null)
        sayamBox.setOnCheckedChangeListener(null)
        pratahBox.isChecked = pratah
        madhyaBox.isChecked = madhya
        sayamBox.isChecked = sayam
        pratahBox.setOnCheckedChangeListener { _, checked -> practice.setSandhya("pratah", checked) }
        madhyaBox.setOnCheckedChangeListener { _, checked -> practice.setSandhya("madhyahna", checked) }
        sayamBox.setOnCheckedChangeListener { _, checked -> practice.setSandhya("sayam", checked) }
    }

    private fun bindStotra(view: View) {
        val one = view.findViewById<MaterialCheckBox>(R.id.dashboardStotra1)
        val two = view.findViewById<MaterialCheckBox>(R.id.dashboardStotra2)
        one.setOnCheckedChangeListener(null)
        two.setOnCheckedChangeListener(null)
        one.isChecked = practice.stotra("hari_vayu")
        two.isChecked = practice.stotra("rama_dwadasha")
        one.setOnCheckedChangeListener { _, checked -> practice.setStotra("hari_vayu", checked) }
        two.setOnCheckedChangeListener { _, checked -> practice.setStotra("rama_dwadasha", checked) }
    }
}
