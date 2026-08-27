package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.ProfileStore

class ProfileFragment : Fragment() {
    private lateinit var store: ProfileStore
    private var editingId = ""
    private var selecting = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.page_profile, container, false)

    override fun onViewCreated(view: View, state: Bundle?) {
        store = ProfileStore(requireContext())
        listOf(R.id.profileTitle, R.id.profileEditorTitle).forEach { DeskType.apply(view.findViewById(it), DeskType.SEMIBOLD) }
        bindOptions(view.findViewById(R.id.profileVeda), ProfileStore.VEDA_SHAKHAS)
        bindOptions(view.findViewById(R.id.profileGotra), ProfileStore.GOTRAS)
        bindOptions(view.findViewById(R.id.profileNakshatra), ProfileStore.NAKSHATRAS)
        refreshProfiles(view, store.active().id)
        view.findViewById<Spinner>(R.id.profileList).onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, selected: View?, position: Int, id: Long) {
                if (selecting) return
                store.profiles().getOrNull(position)?.let { store.setActive(it.id); show(view, it) }
            }
        }
        view.findViewById<View>(R.id.profileNew).setOnClickListener {
            editingId = ""
            view.findViewById<EditText>(R.id.profileName).setText("")
            view.findViewById<Spinner>(R.id.profileVeda).setSelection(0)
            view.findViewById<Spinner>(R.id.profileGotra).setSelection(0)
            view.findViewById<EditText>(R.id.profilePravara).setText("")
            view.findViewById<Spinner>(R.id.profileNakshatra).setSelection(0)
            view.findViewById<TextView>(R.id.profileEditorTitle).text = "ಹೊಸ ವ್ಯಕ್ತಿ"
            view.findViewById<EditText>(R.id.profileName).requestFocus()
        }
        view.findViewById<View>(R.id.profileSave).setOnClickListener {
            val name = view.findViewById<EditText>(R.id.profileName).text.toString().trim()
            if (name.isBlank()) {
                Toast.makeText(requireContext(), "ಹೆಸರನ್ನು ನಮೂದಿಸಿ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val saved = store.save(ProfileStore.Profile(
                editingId, name,
                view.findViewById<Spinner>(R.id.profileVeda).selectedItem.toString(),
                view.findViewById<Spinner>(R.id.profileGotra).selectedItem.toString(),
                view.findViewById<EditText>(R.id.profilePravara).text.toString(),
                view.findViewById<Spinner>(R.id.profileNakshatra).selectedItem.toString()
            ))
            refreshProfiles(view, saved.id)
            Toast.makeText(requireContext(), "ಪ್ರೊಫೈಲ್ ಉಳಿಸಲಾಗಿದೆ", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshProfiles(v: View, selectedId: String) {
        val all = store.profiles()
        selecting = true
        bindOptions(v.findViewById(R.id.profileList), all.map { it.name })
        val index = all.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
        v.findViewById<Spinner>(R.id.profileList).setSelection(index)
        selecting = false
        show(v, all[index])
    }

    private fun show(v: View, p: ProfileStore.Profile) {
        editingId = p.id
        v.findViewById<TextView>(R.id.profileEditorTitle).text = "ವೈಯಕ್ತಿಕ ವಿವರಗಳು"
        v.findViewById<EditText>(R.id.profileName).setText(p.name)
        select(v.findViewById(R.id.profileVeda), ProfileStore.VEDA_SHAKHAS, p.vedaShakha)
        select(v.findViewById(R.id.profileGotra), ProfileStore.GOTRAS, p.gotra)
        v.findViewById<EditText>(R.id.profilePravara).setText(p.pravara)
        select(v.findViewById(R.id.profileNakshatra), ProfileStore.NAKSHATRAS, p.nakshatra)
        val pravara = p.pravara.ifBlank { "ಪ್ರವರ ನಮೂದಿಸಿಲ್ಲ" }
        v.findViewById<TextView>(R.id.profileSummary).text = "${p.vedaShakha}\n${p.gotra} ಗೋತ್ರ  ·  $pravara  ·  ${p.nakshatra} ನಕ್ಷತ್ರ"
    }

    private fun bindOptions(spinner: Spinner, values: List<String>) {
        spinner.adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown_selected, values).also {
            it.setDropDownViewResource(R.layout.item_dropdown)
        }
    }

    private fun select(spinner: Spinner, values: List<String>, value: String) {
        spinner.setSelection(values.indexOf(value).coerceAtLeast(0))
    }
}
