package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.JapaStore
import java.text.NumberFormat
import java.util.Locale

class JapaFragment : Fragment() {
    private lateinit var store: JapaStore
    private val countFormat = NumberFormat.getIntegerInstance(Locale("en", "IN"))

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.page_japa, container, false)

    override fun onViewCreated(view: View, state: Bundle?) {
        store = JapaStore(requireContext())
        listOf(R.id.japaTitle, R.id.japaRitualTitle).forEach { DeskType.apply(view.findViewById(it), DeskType.SEMIBOLD) }
        view.findViewById<View>(R.id.japaTap).setOnClickListener { saveConfig(view); paint(view, store.add(1)) }
        view.findViewById<View>(R.id.japa108).setOnClickListener { saveConfig(view); paint(view, store.add(108)) }
        view.findViewById<View>(R.id.japaUndo).setOnClickListener { paint(view, store.undo()) }
        view.findViewById<View>(R.id.japaComplete).setOnClickListener { saveConfig(view); paint(view, store.complete()) }
        view.findViewById<View>(R.id.japaReset).setOnClickListener {
            AlertDialog.Builder(requireContext()).setTitle("ಪ್ರಸ್ತುತ ಎಣಿಕೆ ಮರುಹೊಂದಿಸಬೇಕೇ?")
                .setMessage("ಜೀವಮಾನ ಒಟ್ಟು ಅಳಿಸಲಾಗುವುದಿಲ್ಲ.")
                .setNegativeButton("ರದ್ದು", null)
                .setPositiveButton("ಮರುಹೊಂದಿಸು") { _, _ -> paint(view, store.resetCurrent()) }.show()
        }
        paint(view, store.state(), initial = true)
    }

    private fun saveConfig(v: View) {
        val mantra = v.findViewById<EditText>(R.id.japaMantra).text.toString()
        val target = v.findViewById<EditText>(R.id.japaTarget).text.toString().replace(",", "").toLongOrNull() ?: 108L
        store.configure(mantra, target)
    }

    private fun paint(v: View, state: JapaStore.State, initial: Boolean = false) {
        if (initial) {
            v.findViewById<EditText>(R.id.japaMantra).setText(state.mantra)
            v.findViewById<EditText>(R.id.japaTarget).setText(state.target.toString())
        }
        v.findViewById<TextView>(R.id.japaCurrent).text = countFormat.format(state.current)
        v.findViewById<TextView>(R.id.japaProgress).text = "ಗುರಿ ${countFormat.format(state.target)}  ·  ${((state.current * 100.0 / state.target).coerceAtMost(9999.0)).toInt()}%"
        v.findViewById<TextView>(R.id.japaLifetime).text = "ಜೀವಮಾನ  ${countFormat.format(state.lifetime)}"
        v.findViewById<TextView>(R.id.japaSessions).text = "ದಾಖಲಿಸಿದ ಅವಧಿಗಳು  ${countFormat.format(state.sessions)}"
    }
}
