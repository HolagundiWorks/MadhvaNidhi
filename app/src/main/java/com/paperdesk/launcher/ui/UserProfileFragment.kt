package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.JapaCatalog
import com.paperdesk.launcher.data.JapaGoals
import com.paperdesk.launcher.data.ProfileStore

/** Frozen User profile — view / edit with diksha + japa targets (Daily · Sankalpa). */
class UserProfileFragment : Fragment() {
    private lateinit var store: ProfileStore
    private var editing = false
    private val selectedDiksha = linkedSetOf<String>()
    private val targetDaily = mutableMapOf<String, Long>()
    private val targetSankalpa = mutableMapOf<String, Long>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.page_user_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        PagePalette.apply(view, "user")
        store = ProfileStore(requireContext())
        view.findViewById<View>(R.id.userEdit).setOnClickListener { setEditing(true) }
        view.findViewById<View>(R.id.userCancel).setOnClickListener { setEditing(false); paintView() }
        view.findViewById<View>(R.id.userSave).setOnClickListener { save(); setEditing(false); paintView() }
        paintView()
    }

    private fun setEditing(value: Boolean) {
        editing = value
        val v = view ?: return
        v.findViewById<View>(R.id.userViewMode).visibility = if (value) View.GONE else View.VISIBLE
        v.findViewById<View>(R.id.userEditMode).visibility = if (value) View.VISIBLE else View.GONE
        v.findViewById<View>(R.id.userEdit).visibility = if (value) View.GONE else View.VISIBLE
        v.findViewById<View>(R.id.userSave).visibility = if (value) View.VISIBLE else View.GONE
        v.findViewById<View>(R.id.userCancel).visibility = if (value) View.VISIBLE else View.GONE
        if (value) paintEdit()
    }

    private fun paintView() {
        val v = view ?: return
        val p = store.active()
        v.findViewById<TextView>(R.id.viewName).text = p.name.ifBlank { "—" }
        v.findViewById<TextView>(R.id.viewGotra).text = p.gotra.ifBlank { "—" }
        v.findViewById<TextView>(R.id.viewMarital).text =
            ProfileStore.MARITAL.firstOrNull { it.first == p.marital }?.second ?: p.marital.ifBlank { "—" }
        v.findViewById<TextView>(R.id.viewRashi).text = p.rashi.ifBlank { "—" }
        v.findViewById<TextView>(R.id.viewNakshatra).text = p.nakshatra.ifBlank { "—" }
        v.findViewById<TextView>(R.id.viewVeda).text = p.vedaShakha.ifBlank { "—" }
        v.findViewById<TextView>(R.id.viewMatha).text = p.matha.ifBlank { "—" }
        v.findViewById<TextView>(R.id.viewDiksha).text =
            if (p.dikshaMantras.isEmpty()) "—"
            else p.dikshaMantras.joinToString(", ") { id ->
                JapaCatalog.entries.firstOrNull { it.id == id }?.label ?: id
            }
        JapaTargetsTable.paintReadonly(v.findViewById(R.id.viewJapaTargets), requireContext())
        v.findViewById<TextView>(R.id.userStatus).text = "Loaded encrypted local profile"
    }

    private fun paintEdit() {
        val v = view ?: return
        val p = store.active()
        selectedDiksha.clear()
        selectedDiksha.addAll(p.dikshaMantras)
        targetDaily.clear()
        targetSankalpa.clear()
        p.japaTargets.forEach { (k, pair) ->
            targetDaily[k] = pair.first
            targetSankalpa[k] = pair.second
        }
        v.findViewById<TextInputEditText>(R.id.editName).setText(p.name)
        bindSelect(v.findViewById(R.id.editGotra), ProfileStore.GOTRAS, p.gotra)
        bindSelect(v.findViewById(R.id.editMarital), ProfileStore.MARITAL.map { it.second },
            ProfileStore.MARITAL.indexOfFirst { it.first == p.marital }.coerceAtLeast(0))
        bindSelect(v.findViewById(R.id.editRashi), ProfileStore.RASHIS, p.rashi)
        bindSelect(v.findViewById(R.id.editNakshatra), ProfileStore.NAKSHATRAS, p.nakshatra)
        bindSelect(v.findViewById(R.id.editVeda), ProfileStore.VEDA_SHAKHAS, p.vedaShakha)
        bindSelect(v.findViewById(R.id.editMatha), ProfileStore.MATHAS,
            ProfileStore.MATHAS.indexOf(p.matha).takeIf { it >= 0 } ?: 0)
        rebuildDiksha(v)
        rebuildTargets(v)
    }

    private fun bindSelect(view: MaterialAutoCompleteTextView, items: List<String>, selected: String) {
        val index = items.indexOf(selected).coerceAtLeast(0)
        MaterialChoices.bind(view, items, index) { }
    }

    private fun bindSelect(view: MaterialAutoCompleteTextView, items: List<String>, index: Int) {
        MaterialChoices.bind(view, items, index.coerceIn(0, items.lastIndex)) { }
    }

    private fun rebuildDiksha(v: View) {
        val host = v.findViewById<LinearLayout>(R.id.editDikshaHost)
        host.removeAllViews()
        JapaCatalog.entries.forEach { entry ->
            val box = MaterialCheckBox(requireContext()).apply {
                text = entry.label
                isChecked = entry.id in selectedDiksha
                setOnCheckedChangeListener { _, checked ->
                    if (checked) {
                        selectedDiksha += entry.id
                        targetDaily.putIfAbsent(entry.id, JapaGoals.DEFAULT_DAILY)
                        targetSankalpa.putIfAbsent(entry.id, JapaGoals.SANKALPA_AUTO)
                    } else {
                        selectedDiksha -= entry.id
                        targetDaily.remove(entry.id)
                        targetSankalpa.remove(entry.id)
                    }
                    rebuildTargets(v)
                }
            }
            host.addView(box)
        }
    }

    private fun rebuildTargets(v: View) {
        JapaTargetsTable.paintEditable(
            v.findViewById(R.id.editJapaTargetsHost),
            requireContext(),
            selectedDiksha,
            targetDaily,
            targetSankalpa
        )
    }

    private fun save() {
        val v = view ?: return
        val current = store.active()
        val maritalIndex = MaterialChoices.position(v.findViewById(R.id.editMarital))
        val marital = ProfileStore.MARITAL.getOrElse(maritalIndex) { ProfileStore.MARITAL.first() }.first
        val targets = selectedDiksha.associateWith { id ->
            Pair(
                targetDaily[id] ?: JapaGoals.DEFAULT_DAILY,
                targetSankalpa[id] ?: JapaGoals.SANKALPA_AUTO
            )
        }
        store.save(
            current.copy(
                name = v.findViewById<TextInputEditText>(R.id.editName).text?.toString().orEmpty(),
                gotra = MaterialChoices.value(v.findViewById(R.id.editGotra)),
                marital = marital,
                rashi = MaterialChoices.value(v.findViewById(R.id.editRashi)),
                nakshatra = MaterialChoices.value(v.findViewById(R.id.editNakshatra)),
                vedaShakha = MaterialChoices.value(v.findViewById(R.id.editVeda)),
                matha = MaterialChoices.value(v.findViewById(R.id.editMatha)),
                dikshaMantras = selectedDiksha.toList(),
                japaTargets = targets
            )
        )
        targets.values.firstOrNull()?.first?.let { daily ->
            requireContext().getSharedPreferences("paperdesk_japa_ui", 0).edit()
                .putLong("daily_goal", daily)
                .putLong("base_daily_goal", daily)
                .apply()
        }
        v.findViewById<TextView>(R.id.userStatus).text = "Saved encrypted local profile"
    }
}
