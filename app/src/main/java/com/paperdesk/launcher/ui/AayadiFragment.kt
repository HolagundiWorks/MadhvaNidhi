package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.Aayadi
import java.text.DecimalFormat

class AayadiFragment : Fragment() {
    private val fmt = DecimalFormat("0.##")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.page_aayadi, container, false)

    override fun onViewCreated(view: View, state: Bundle?) {
        listOf(R.id.aayadiTitle, R.id.aayadiResultTitle).forEach { DeskType.apply(view.findViewById(it), DeskType.SEMIBOLD) }
        bind(view.findViewById(R.id.aayadiLengthFt), (0..500).map { it.toString() }, 30)
        bind(view.findViewById(R.id.aayadiBreadthFt), (0..500).map { it.toString() }, 40)
        bind(view.findViewById(R.id.aayadiLengthIn), (0..11).map { it.toString() }, 0)
        bind(view.findViewById(R.id.aayadiBreadthIn), (0..11).map { it.toString() }, 0)
        view.findViewById<View>(R.id.aayadiCalculate).setOnClickListener { calculate(view) }
        calculate(view)
    }

    private fun calculate(v: View) {
        fun number(id: Int): Double = v.findViewById<Spinner>(id).selectedItem.toString().toDoubleOrNull() ?: 0.0
        val result = runCatching {
            Aayadi.calculate(number(R.id.aayadiLengthFt), number(R.id.aayadiLengthIn), number(R.id.aayadiBreadthFt), number(R.id.aayadiBreadthIn))
        }.getOrNull()
        v.findViewById<TextView>(R.id.aayadiError).visibility = if (result == null) View.VISIBLE else View.GONE
        v.findViewById<View>(R.id.aayadiResults).visibility = if (result == null) View.GONE else View.VISIBLE
        if (result == null) return
        v.findViewById<TextView>(R.id.aayadiPadam).text = fmt.format(result.padam)
        v.findViewById<TextView>(R.id.aayadiAaya).text = "${fmt.format(result.aaya)}  ·  ${result.aayaName}"
        v.findViewById<TextView>(R.id.aayadiVyaya).text = fmt.format(result.vyaya)
        v.findViewById<TextView>(R.id.aayadiRuna).text = fmt.format(result.runa)
        v.findViewById<TextView>(R.id.aayadiDhana).text = fmt.format(result.dhana)
        v.findViewById<TextView>(R.id.aayadiAyush).text = fmt.format(result.ayush)
        v.findViewById<TextView>(R.id.aayadiSummary).text = result.summary
    }

    private fun bind(spinner: Spinner, values: List<String>, initial: Int) {
        spinner.adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown_selected, values).also {
            it.setDropDownViewResource(R.layout.item_dropdown)
        }
        spinner.setSelection(initial.coerceIn(0, values.lastIndex))
    }
}
