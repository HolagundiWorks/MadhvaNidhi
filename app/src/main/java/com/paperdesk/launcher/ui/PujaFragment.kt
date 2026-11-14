package com.paperdesk.launcher.ui

import android.os.Bundle
import android.content.Context
import android.util.TypedValue
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.IndicScript
import com.paperdesk.launcher.data.MadhwaItem
import com.paperdesk.launcher.data.MadhwaStore
import com.paperdesk.launcher.data.OsLang
import com.paperdesk.launcher.data.Sankalpa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** Reading position is independent from practice completion. */
class PujaFragment : Fragment(R.layout.page_nitya_puja) {
    private data class Section(val title: String, val body: String)
    private data class Rite(val item: MadhwaItem, val sections: List<Section>)
    private var rites: List<Rite> = emptyList()
    private var selected = 0
    private var section = 0
    private var readingScale = 1f
    private var baseTextPixels = 0f
    private var loading: kotlinx.coroutines.Job? = null
    private val prefs get() = requireContext().getSharedPreferences("madhvanidhi_puja_reader", Context.MODE_PRIVATE)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        PagePalette.apply(view, "puja")
        baseTextPixels = view.findViewById<TextView>(R.id.pujaBody).textSize
        readingScale = prefs.getFloat("text_scale", 1f).coerceIn(1f, 2f)
        view.findViewById<View>(R.id.pujaTextIncrease).setOnClickListener { changeText(view, .1f) }
        view.findViewById<View>(R.id.pujaTextDecrease).setOnClickListener { changeText(view, -.1f) }
        applyText(view)
        view.findViewById<View>(R.id.pujaBreadcrumb).setOnClickListener {
            val rite = rites.getOrNull(selected) ?: return@setOnClickListener
            if (rite.sections.isEmpty()) return@setOnClickListener
            val lang = OsLang.get(requireContext())
            val titles = rite.sections.map { OsLang.script(it.title, lang) }.toTypedArray()
            com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select section")
                .setSingleChoiceItems(titles, section) { dialog, index ->
                    section = index
                    paint(view)
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val app = requireContext().applicationContext
            rites = withContext(Dispatchers.IO) {
                MadhwaStore.load(app)
                val preferred = listOf("madhwa_deva_pooja_guide", "puja_udupi", "festival_ganesha_chaturthi", "puja_anushtana")
                (preferred.mapNotNull { MadhwaStore.item(it) } +
                    MadhwaStore.inCategory("puja") + MadhwaStore.inCategory("festival"))
                    .distinctBy { it.id }
                    .map { Rite(it, emptyList()) }
            }
            if (rites.isEmpty()) {
                view.findViewById<TextView>(R.id.pujaBody).text = "ಪೂಜಾ ಕ್ರಮ ಲಭ್ಯವಿಲ್ಲ"
                return@launch
            }
            selected = rites.indexOfFirst { it.item.id == prefs.getString("rite", "") }.coerceAtLeast(0)
            section = prefs.getInt("section_" + rites[selected].item.id, 0)
            val lang = OsLang.get(requireContext())
            MaterialChoices.bind(view.findViewById<MaterialAutoCompleteTextView>(R.id.pujaRite),
                rites.map { MadhwaStore.label(it.item, lang) }, selected) {
                selected = it
                section = prefs.getInt("section_" + rites[it].item.id, 0)
                paint(view)
            }
            paint(view)
        }
    }

    private fun sections(text: String): List<Section> {
        val headings = setOf("ದ್ವಾರಪಾಲಕರಿಗೆ ನಮಸ್ಕಾರ", "ತಾರತಮ್ಯ ಸ್ತೋತ್ರ", "ದೀಪಪ್ರಜ್ವಲನಂ",
            "ನಿರ್ಮಾಲ್ಯ ವಿಸರ್ಜನಂ", "ನಿರ್ಮಾಲ್ಯ ಅಭಿಷೇಕ :", "ಅಥ ಷೋಡಶೋಪಚಾರಾದಿ ಮಹಾಪೂಜಾ", "ಮಹಾಮಂಗಳಾರತಿ")
        val numbered = Regex("^[೦-೯0-9]+[.、)]\\s.*")
        val result = mutableListOf<Section>()
        var title = "ಪರಿಚಯ"
        val body = StringBuilder()
        fun flush() {
            if (body.isNotBlank()) result += Section(title, body.toString().trim())
            body.clear()
        }
        text.lines().forEach { line ->
            if (line.trim() in headings || numbered.matches(line)) {
                flush()
                title = line.trim()
            } else body.appendLine(line)
        }
        flush()
        return result.ifEmpty { listOf(Section("ಪರಿಚಯ", text.ifBlank { "ಪಠ್ಯ ಲಭ್ಯವಿಲ್ಲ" })) }
    }

    private fun paint(root: View) {
        val rite = rites.getOrNull(selected) ?: return
        if (rite.sections.isEmpty()) {
            loading?.cancel()
            root.findViewById<TextView>(R.id.pujaBody).text = "Loading Puja…"
            val app = requireContext().applicationContext
            val index = selected
            loading = viewLifecycleOwner.lifecycleScope.launch {
                val content = withContext(Dispatchers.IO) {
                    sections(MadhwaStore.body(app, rite.item, IndicScript.Lang.KN))
                }
                rites = rites.toMutableList().also { it[index] = rite.copy(sections = content) }
                if (selected == index) paint(root)
            }
            return
        }
        section = section.coerceIn(0, rite.sections.lastIndex)
        val current = rite.sections[section]
        val lang = OsLang.get(requireContext())
        val title = OsLang.script(current.title, lang)
        root.findViewById<TextView>(R.id.pujaBreadcrumb).text = MadhwaStore.label(rite.item, lang) + " / " + title + " ⌄"
        root.findViewById<TextView>(R.id.pujaSectionHeading).text = title
        root.findViewById<TextView>(R.id.pujaBody).text =
            Sankalpa.injectPuja(requireContext(), OsLang.script(current.body, lang), lang)
        root.findViewById<ProgressBar>(R.id.pujaSectionProgress).apply {
            progress = ((section + 1) * 100f / rite.sections.size).roundToInt()
            contentDescription = "Section ${section + 1} of ${rite.sections.size}"
        }
        root.findViewById<NestedScrollView>(R.id.pujaScroll).scrollTo(0, 0)
        prefs.edit().putString("rite", rite.item.id).putInt("section_" + rite.item.id, section).apply()
    }

    private fun changeText(root: View, delta: Float) {
        readingScale = (((readingScale + delta) * 10).roundToInt() / 10f).coerceIn(1f, 2f)
        prefs.edit().putFloat("text_scale", readingScale).apply()
        applyText(root)
    }

    private fun applyText(root: View) {
        root.findViewById<TextView>(R.id.pujaBody).setTextSize(TypedValue.COMPLEX_UNIT_PX, baseTextPixels * readingScale)
        root.findViewById<View>(R.id.pujaTextIncrease).isEnabled = readingScale < 2f
        root.findViewById<View>(R.id.pujaTextDecrease).isEnabled = readingScale > 1f
    }
}
