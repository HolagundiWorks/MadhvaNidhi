package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.fragment.app.Fragment
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.JapaCatalog
import com.paperdesk.launcher.data.JapaStore
import java.text.NumberFormat
import java.util.Locale

class JapaFragment : Fragment() {
    private lateinit var store: JapaStore
    private var selectingJapa = false
    private var selectingRitual = false
    private val countFormat = NumberFormat.getIntegerInstance(Locale("en", "IN"))

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        ScreenPages.wrap(inflater.inflate(R.layout.page_japa, container, false))

    override fun onViewCreated(view: View, state: Bundle?) {
        PagePalette.apply(view, "japa")
        MaterialLayouts.bind(view)
        store = JapaStore(requireContext())
        listOf(R.id.japaTitle, R.id.japaRitualStep).forEach {
            DeskType.apply(view.findViewById(it), DeskType.SEMIBOLD)
        }
        bindRituals(view)
        bindJapas(view)

        view.findViewById<View>(R.id.japaTap).setOnClickListener { button ->
            button.performHapticFeedback(
                HapticFeedbackConstants.KEYBOARD_TAP,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            saveConfig(view)
            paint(view, store.add(1))
        }
        view.findViewById<View>(R.id.japa108).setOnClickListener {
            saveConfig(view)
            paint(view, store.add(108))
        }
        view.findViewById<View>(R.id.japaUndo).setOnClickListener { paint(view, store.undo()) }
        view.findViewById<View>(R.id.japaComplete).setOnClickListener {
            saveConfig(view)
            paint(view, store.complete())
            selectRitual(view, COMPLETION_STEP)
        }
        view.findViewById<View>(R.id.japaReset).setOnClickListener {
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle("ಪ್ರಸ್ತುತ ಎಣಿಕೆ ಮರುಹೊಂದಿಸಬೇಕೇ?")
                .setMessage("ಈ ಕ್ರಿಯೆಯಿಂದ ಪ್ರಸ್ತುತ ಎಣಿಕೆ ಮಾತ್ರ ಶೂನ್ಯವಾಗುತ್ತದೆ; ದಾಖಲಿಸಿದ ಹಳೆಯ ಅವಧಿಗಳು ಉಳಿಯುತ್ತವೆ.")
                .setNegativeButton("ರದ್ದು", null)
                .setPositiveButton("ಮರುಹೊಂದಿಸು") { _, _ -> paint(view, store.resetCurrent()) }
                .create()
            dialog.show()
        }
        paint(view, store.state(), initial = true)
    }

    private fun bindRituals(v: View) {
        val spinner = v.findViewById<MaterialAutoCompleteTextView>(R.id.japaRitual)
        MaterialChoices.bind(spinner, RITUALS.map { it.first }) { showRitual(v, it) }
        val prefs = requireContext().getSharedPreferences("paperdesk_japa_ui", 0)
        val initial = prefs.getInt("ritual", 0).coerceIn(RITUALS.indices)
        selectRitual(v, initial)
        v.findViewById<View>(R.id.japaRitualBack).setOnClickListener {
            selectRitual(v, MaterialChoices.position(spinner) - 1)
        }
        v.findViewById<View>(R.id.japaRitualNext).setOnClickListener {
            selectRitual(v, MaterialChoices.position(spinner) + 1)
        }
    }

    private fun selectRitual(v: View, requested: Int) {
        val position = requested.coerceIn(RITUALS.indices)
        selectingRitual = true
        MaterialChoices.select(v.findViewById(R.id.japaRitual), position)
        selectingRitual = false
        showRitual(v, position)
    }

    private fun showRitual(v: View, position: Int) {
        val ritual = RITUALS[position]
        v.findViewById<TextView>(R.id.japaRitualTitle).text = when (position) {
            JAPA_STEP -> "ಜಪ  ›  ಎಣಿಕೆ"
            COMPLETION_STEP -> "ಜಪ  ›  ಸಮರ್ಪಣೆ"
            else -> "ಜಪ  ›  ಪೂರ್ವ ಸಿದ್ಧತೆ"
        }
        v.findViewById<TextView>(R.id.japaRitualStep).text = ritual.first
        v.findViewById<TextView>(R.id.japaRitualBody).apply {
            text = ritual.second
            DeskType.reading(this, 20f)
        }

        val counting = position == JAPA_STEP
        listOf(R.id.japaComplete, R.id.japaReset).forEach { id ->
            v.findViewById<View>(id).visibility = if (counting) View.VISIBLE else View.GONE
        }
        val nextButton = v.findViewById<com.google.android.material.button.MaterialButton>(R.id.japaRitualNext)
        nextButton.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (counting) android.graphics.Color.TRANSPARENT else requireContext().getColor(R.color.carbon_orange))
        nextButton.setTextColor(requireContext().getColor(if (counting) R.color.carbon_orange else R.color.carbon_background))
        nextButton.strokeWidth = if (counting) resources.getDimensionPixelSize(R.dimen.carbon_spacing_01) / 2 else 0
        nextButton.strokeColor = android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.carbon_border))
        v.findViewById<View>(R.id.japaCounterPane).visibility = if (counting) View.VISIBLE else View.GONE
        v.findViewById<TextView>(R.id.japaWaiting).apply {
            visibility = if (counting) View.GONE else View.VISIBLE
            text = if (position < JAPA_STEP) {
                "ಪೂರ್ವ ಸಿದ್ಧತೆ ${position + 1} / $JAPA_STEP\n\nಕ್ರಮ ಮುಗಿಸಿ ಮುಂದಿನ ಬಾಣ ಒತ್ತಿರಿ.\nಜಪ ಎಣಿಕೆ ನಂತರ ತೆರೆದುಕೊಳ್ಳುತ್ತದೆ."
            } else {
                "ಜಪದ ಎಣಿಕೆ ದಾಖಲಿಸಲಾಗಿದೆ. ಎಡಭಾಗದ ಸಮರ್ಪಣೆ ಕ್ರಮವನ್ನು ಪೂರ್ಣಗೊಳಿಸಿ."
            }
        }

        v.findViewById<View>(R.id.japaRitualBack).apply {
            isEnabled = position > 0
            alpha = if (isEnabled) 1f else 0.35f
        }
        v.findViewById<View>(R.id.japaRitualNext).apply {
            isEnabled = position < RITUALS.lastIndex
            alpha = if (isEnabled) 1f else 0.35f
        }
        requireContext().getSharedPreferences("paperdesk_japa_ui", 0)
            .edit().putInt("ritual", position).apply()
    }

    private fun bindJapas(v: View) {
        val spinner = v.findViewById<MaterialAutoCompleteTextView>(R.id.japaMantra)
        MaterialChoices.bind(spinner, JapaCatalog.entries.map { it.label }, JapaCatalog.indexOf(store.state().mantra)) {
            updateSelectedMantra(v)
            saveConfig(v)
            paint(v, store.state())
        }
        updateSelectedMantra(v)
    }

    private fun updateSelectedMantra(v: View) {
        val item = selectedJapa(v) ?: JapaCatalog.entries.first()
        v.findViewById<TextView>(R.id.japaMantraName).apply { text = item.mantra; DeskType.reading(this) }
        v.findViewById<TextView>(R.id.japaMantraNote).apply {
            visibility = if (item.requiresUpadesha) View.VISIBLE else View.GONE
            text = if (item.requiresUpadesha) "ಗುರು ಉಪದೇಶ ಪಡೆದವರು ಮಾತ್ರ" else ""
        }
    }

    private fun saveConfig(v: View) {
        val mantra = selectedJapa(v)?.mantra ?: JapaCatalog.entries.first().mantra
        store.configure(mantra, store.state().target, false)
    }

    private fun selectedJapa(v: View): JapaCatalog.Entry? =
        JapaCatalog.entries.getOrNull(MaterialChoices.position(v.findViewById(R.id.japaMantra)))

    private fun paint(v: View, state: JapaStore.State, initial: Boolean = false) {
        if (initial) {
            selectingJapa = true
            MaterialChoices.select(v.findViewById(R.id.japaMantra), JapaCatalog.indexOf(state.mantra))
            selectingJapa = false
            updateSelectedMantra(v)
        }
        v.findViewById<TextView>(R.id.japaCurrent).text = countFormat.format(state.current)
        v.findViewById<TextView>(R.id.japaSessions).text =
            "ದಾಖಲಿಸಿದ ಅವಧಿಗಳು  ${countFormat.format(state.sessions)}"
    }

    companion object {
        private const val JAPA_STEP = 6
        private const val COMPLETION_STEP = 7
        private val RITUALS = listOf(
            "೧  ಶುದ್ಧಿ ಮತ್ತು ಆಸನ" to "ಕೈ–ಕಾಲು ತೊಳೆದು ಶುದ್ಧ, ಶಾಂತ ಸ್ಥಳದಲ್ಲಿ ಗುರು ಸೂಚಿಸಿದ ದಿಕ್ಕು ಮತ್ತು ಆಸನದಲ್ಲಿ ಕುಳಿತುಕೊಳ್ಳಿ. ಬೆನ್ನು ಸಹಜವಾಗಿ ನೇರವಾಗಿರಲಿ.",
            "೨  ಆಚಮನ" to "ಶುದ್ಧ ನೀರಿನಿಂದ ಕುಟುಂಬದ ಮಾಧ್ವ ಕ್ರಮದಂತೆ ಆಚಮನ ಮಾಡಿ. ಕೇಶವದಿಂದ ಶ್ರೀಕೃಷ್ಣವರೆಗಿನ ಚತುರ್ವಿಂಶತಿ ನಾಮಗಳನ್ನು ಕಲಿತ ಕ್ರಮದಲ್ಲೇ ಬಳಸಿ.",
            "೩  ಶ್ರೀಹರಿ–ಗುರು ಸ್ಮರಣೆ" to "ಶ್ರೀಹರಿ, ಮುಖ್ಯಪ್ರಾಣ ದೇವರು ಮತ್ತು ಗುರುಪರಂಪರೆಯನ್ನು ಸ್ಮರಿಸಿ ನಮಸ್ಕರಿಸಿ. ಮನಸ್ಸನ್ನು ಜಪಕ್ಕೆ ಸಿದ್ಧಗೊಳಿಸಿ.",
            "೪  ಪ್ರಾಣಾಯಾಮ" to "ಗುರುಗಳಿಂದ ಕಲಿತ ಪ್ರಾಣಾಯಾಮವನ್ನು ನಿಗದಿತ ಮಂತ್ರ ಮತ್ತು ಸಂಖ್ಯೆಯಲ್ಲಿ ಮಾಡಿ. ಉಸಿರನ್ನು ಬಲವಂತವಾಗಿ ಹಿಡಿಯಬೇಡಿ.",
            "೫  ಸಂಕಲ್ಪ" to "ಆಯ್ಕೆ ಮಾಡಿದ ಮಂತ್ರ ಮತ್ತು ಜಪದ ಗುರಿ ಸಂಖ್ಯೆಯನ್ನು ಮನಸ್ಸಿನಲ್ಲಿ ನಿಶ್ಚಯಿಸಿ. ಜಪವನ್ನು ಶ್ರೀಹರಿಯ ಪ್ರೀತ್ಯರ್ಥವಾಗಿ ಮಾಡುವ ಸಂಕಲ್ಪ ಮಾಡಿ.",
            "೬  ನ್ಯಾಸ ಮತ್ತು ಧ್ಯಾನ" to "ವಿನಿಯೋಗ, ಋಷ್ಯಾದಿನ್ಯಾಸ, ಕರನ್ಯಾಸ, ಅಂಗನ್ಯಾಸ ಮತ್ತು ಧ್ಯಾನವನ್ನು ಆ ಮಂತ್ರಕ್ಕೆ ಗುರು ಉಪದೇಶಿಸಿದ್ದರೆ ಮಾತ್ರ ಮಾಡಿ.",
            "೭  ಜಪ" to "ಮಂತ್ರವನ್ನು ಶುದ್ಧ ಉಚ್ಚಾರಣೆ ಮತ್ತು ಸ್ಥಿರ ಗತಿಯಲ್ಲಿ ಜಪಿಸಿ. ಮಾಲೆಯ ಮೇರು ದಾಟಬೇಡಿ. ಪ್ರತಿಯೊಂದು ಜಪಕ್ಕೆ ಬಲಭಾಗದ ಕಿತ್ತಳೆ ವೃತ್ತವನ್ನು ಒತ್ತಿರಿ.",
            "೮  ಸಮರ್ಪಣೆ" to "ಮಾಡಿದ ಜಪದ ಫಲವನ್ನು ಶ್ರೀಮನ್ನಾರಾಯಣನಿಗೆ ಸಮರ್ಪಿಸಿ. ಮನಸ್ಸಿನಲ್ಲಿ ಕೃತಜ್ಞತೆಯಿಂದ ನಮಸ್ಕರಿಸಿ."
        )
    }
}
