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
    private var japa: JapaStore? = null
    private lateinit var practice: PracticeDayStore
    private var panchangaJob: kotlinx.coroutines.Job? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.page_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        practice = PracticeDayStore(requireContext())
        wireNav(view)
        bindSandhya(view)
        bindStotra(view)
        // Date label is cheap; live tithi/japa wait for IO (MasterKey / network).
        paintDate(view)
        viewLifecycleOwner.lifecycleScope.launch {
            val app = requireContext().applicationContext
            val store = withContext(Dispatchers.IO) { JapaStore(app) }
            if (!isAdded) return@launch
            japa = store
            bindJapa(view)
        }
        bindPanchanga(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { root ->
            if (japa != null) bindJapa(root)
            bindSandhya(root)
            bindStotra(root)
            bindPanchanga(root)
        }
    }

    private fun wireNav(view: View) {
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

    private fun paintDate(view: View) {
        val lang = OsLang.get(requireContext())
        val date = PanchangaStore.dateKey()
        val cal = Calendar.getInstance().apply {
            val parts = date.split("-")
            if (parts.size == 3) set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        }
        view.findViewById<TextView>(R.id.dashboardDate).text = OsLang.dateLine(cal, lang)
    }

    private fun bindPanchanga(view: View) {
        val lang = OsLang.get(requireContext())
        val date = PanchangaStore.dateKey()
        paintDate(view)
        panchangaJob?.cancel()
        panchangaJob = viewLifecycleOwner.lifecycleScope.launch {
            val day = withContext(Dispatchers.IO) {
                FestivalStore.load(requireContext().applicationContext)
                PanchangaStore.forDate(requireContext().applicationContext, date)
            }
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
        val store = japa ?: return
        val state = store.state()
        view.findViewById<TextView>(R.id.dashboardJapaCount).text = state.current.toString()
        val malas = if (state.target > 0) state.current / state.target else 0
        view.findViewById<TextView>(R.id.dashboardJapaMeta).text =
            getString(R.string.dashboard_japa_meta, state.current, malas, state.target)
        view.findViewById<MaterialButton>(R.id.dashboardJapaInc).setOnClickListener {
            store.add(1)
            bindJapa(view)
        }
        view.findViewById<MaterialButton>(R.id.dashboardJapaUndo).setOnClickListener {
            store.undo()
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
