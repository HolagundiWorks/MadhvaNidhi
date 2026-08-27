package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.JapaStore
import com.paperdesk.launcher.data.ProfileStore
import java.text.NumberFormat
import java.util.Locale

class JapaFragment : Fragment() {
    private lateinit var store: JapaStore
    private lateinit var profiles: ProfileStore
    private var selectingProfile = false
    private val countFormat = NumberFormat.getIntegerInstance(Locale("en", "IN"))

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.page_japa, container, false)

    override fun onViewCreated(view: View, state: Bundle?) {
        profiles = ProfileStore(requireContext())
        store = JapaStore(requireContext(), profiles.active().id)
        listOf(R.id.japaTitle, R.id.japaRitualTitle).forEach { DeskType.apply(view.findViewById(it), DeskType.SEMIBOLD) }
        bindProfiles(view)
        view.findViewById<View>(R.id.japaTap).setOnClickListener { saveConfig(view); paint(view, store.add(1)) }
        view.findViewById<View>(R.id.japa108).setOnClickListener { saveConfig(view); paint(view, store.add(108)) }
        view.findViewById<View>(R.id.japaUndo).setOnClickListener { paint(view, store.undo()) }
        view.findViewById<View>(R.id.japaComplete).setOnClickListener { saveConfig(view); paint(view, store.complete()) }
        view.findViewById<View>(R.id.japaReset).setOnClickListener {
            AlertDialog.Builder(requireContext()).setTitle("ಪ್ರಸ್ತುತ ಎಣಿಕೆ ಮರುಹೊಂದಿಸಬೇಕೇ?")
                .setMessage("ಈ ಕ್ರಿಯೆಯಿಂದ ಪ್ರಸ್ತುತ ಎಣಿಕೆ ಮಾತ್ರ ಶೂನ್ಯವಾಗುತ್ತದೆ; ದಾಖಲಿಸಿದ ಹಳೆಯ ಅವಧಿಗಳು ಉಳಿಯುತ್ತವೆ.")
                .setNegativeButton("ರದ್ದು", null)
                .setPositiveButton("ಮರುಹೊಂದಿಸು") { _, _ -> paint(view, store.resetCurrent()) }.show()
        }
        view.findViewById<CheckBox>(R.id.japaTargetEnabled).setOnCheckedChangeListener { _, enabled ->
            view.findViewById<View>(R.id.japaTarget).visibility = if (enabled) View.VISIBLE else View.GONE
            saveConfig(view)
            paint(view, store.state())
        }
        paint(view, store.state(), initial = true)
    }

    override fun onResume() {
        super.onResume()
        view?.let { bindProfiles(it) }
    }

    private fun bindProfiles(v: View) {
        val all = profiles.profiles()
        val spinner = v.findViewById<Spinner>(R.id.japaProfile)
        selectingProfile = true
        spinner.adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown_selected, all.map { it.name }).also {
            it.setDropDownViewResource(R.layout.item_dropdown)
        }
        spinner.setSelection(all.indexOfFirst { it.id == profiles.active().id }.coerceAtLeast(0))
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, selected: View?, position: Int, id: Long) {
                if (selectingProfile) return
                all.getOrNull(position)?.let { profile ->
                    profiles.setActive(profile.id)
                    store = JapaStore(requireContext(), profile.id)
                    showProfile(v, profile)
                    paint(v, store.state(), initial = true)
                }
            }
        }
        selectingProfile = false
        showProfile(v, profiles.active())
    }

    private fun showProfile(v: View, p: ProfileStore.Profile) {
        v.findViewById<TextView>(R.id.japaProfileMeta).text = "${p.gotra} ಗೋತ್ರ  ·  ${p.nakshatra} ನಕ್ಷತ್ರ"
        v.findViewById<TextView>(R.id.japaSankalpa).text =
            "ಸಂಕಲ್ಪ ಸೂಚನೆ  ·  ${p.name}  ·  ${p.gotra} ಗೋತ್ರ  ·  ${p.pravara.ifBlank { "ಪ್ರವರ ನಮೂದಿಸಿಲ್ಲ" }}  ·  ${p.nakshatra} ನಕ್ಷತ್ರ"
    }

    private fun saveConfig(v: View) {
        val mantra = v.findViewById<EditText>(R.id.japaMantra).text.toString()
        val target = v.findViewById<EditText>(R.id.japaTarget).text.toString().replace(",", "").toLongOrNull() ?: 108L
        store.configure(mantra, target, v.findViewById<CheckBox>(R.id.japaTargetEnabled).isChecked)
    }

    private fun paint(v: View, state: JapaStore.State, initial: Boolean = false) {
        if (initial) {
            v.findViewById<EditText>(R.id.japaMantra).setText(state.mantra)
            v.findViewById<EditText>(R.id.japaTarget).setText(state.target.toString())
            v.findViewById<CheckBox>(R.id.japaTargetEnabled).isChecked = state.targetEnabled
        }
        v.findViewById<View>(R.id.japaTarget).visibility = if (state.targetEnabled) View.VISIBLE else View.GONE
        v.findViewById<TextView>(R.id.japaCurrent).text = countFormat.format(state.current)
        v.findViewById<TextView>(R.id.japaProgress).apply {
            visibility = if (state.targetEnabled) View.VISIBLE else View.GONE
            text = if (state.targetEnabled) "ಗುರಿ ${countFormat.format(state.target)}  ·  ${((state.current * 100.0 / state.target).coerceAtMost(9999.0)).toInt()}%" else ""
        }
        v.findViewById<TextView>(R.id.japaSessions).text = "ದಾಖಲಿಸಿದ ಅವಧಿಗಳು  ${countFormat.format(state.sessions)}"
    }
}
