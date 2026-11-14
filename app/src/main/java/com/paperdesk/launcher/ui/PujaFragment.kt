package com.paperdesk.launcher.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.IndicScript
import com.paperdesk.launcher.data.MadhwaItem
import com.paperdesk.launcher.data.MadhwaStore
import com.paperdesk.launcher.data.OsLang
import com.paperdesk.launcher.data.PracticeDayStore
import com.paperdesk.launcher.data.Sankalpa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Frozen Puja: continuous rite scroll + day Skip/Complete (PracticeDayStore). */
class PujaFragment : Fragment(R.layout.page_nitya_puja) {
    private data class Rite(val item: MadhwaItem, val body: String? = null)

    private var rites: List<Rite> = emptyList()
    private var selected = 0
    private var loading: Job? = null
    private lateinit var practice: PracticeDayStore
    private val prefs get() = requireContext().getSharedPreferences("madhvanidhi_puja_reader", Context.MODE_PRIVATE)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        PagePalette.apply(view, "puja")
        practice = PracticeDayStore(requireContext())

        view.findViewById<MaterialButton>(R.id.pujaSkip).setOnClickListener {
            val skipped = !practice.pujaSkipped()
            practice.setPujaDay(complete = false, skipped = skipped)
            paintChrome(view)
        }
        view.findViewById<MaterialButton>(R.id.pujaComplete).setOnClickListener {
            val done = !practice.pujaComplete()
            practice.setPujaDay(complete = done, skipped = false)
            paintChrome(view)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val app = requireContext().applicationContext
            rites = withContext(Dispatchers.IO) {
                MadhwaStore.load(app)
                val preferred = listOf(
                    "madhwa_deva_pooja_guide",
                    "puja_udupi",
                    "festival_ganesha_chaturthi",
                    "puja_anushtana"
                )
                (preferred.mapNotNull { MadhwaStore.item(it) } +
                    MadhwaStore.inCategory("puja") + MadhwaStore.inCategory("festival"))
                    .distinctBy { it.id }
                    .map { Rite(it) }
            }
            if (rites.isEmpty()) {
                view.findViewById<TextView>(R.id.pujaBody).text = "ಪೂಜಾ ಕ್ರಮ ಲಭ್ಯವಿಲ್ಲ"
                return@launch
            }
            selected = rites.indexOfFirst { it.item.id == prefs.getString("rite", "") }.coerceAtLeast(0)
            val lang = OsLang.get(requireContext())
            MaterialChoices.bind(
                view.findViewById(R.id.pujaRite),
                rites.map { MadhwaStore.label(it.item, lang) },
                selected
            ) {
                selected = it
                practice.setPujaDay(complete = false, skipped = false)
                paint(view)
            }
            paint(view)
        }
    }

    private fun paint(root: View) {
        val rite = rites.getOrNull(selected) ?: return
        val lang = OsLang.get(requireContext())
        val label = MadhwaStore.label(rite.item, lang)
        root.findViewById<TextView>(R.id.pujaTitle).text = label
        root.findViewById<TextView>(R.id.pujaCrumb).text = "ಪೂಜೆ · $label"
        paintChrome(root)

        if (rite.body == null) {
            loading?.cancel()
            root.findViewById<TextView>(R.id.pujaBody).text = "…"
            val index = selected
            loading = viewLifecycleOwner.lifecycleScope.launch {
                val content = withContext(Dispatchers.IO) {
                    MadhwaStore.body(requireContext().applicationContext, rite.item, IndicScript.Lang.KN)
                }
                rites = rites.toMutableList().also { it[index] = rite.copy(body = content) }
                if (selected == index) paint(root)
            }
            return
        }

        root.findViewById<TextView>(R.id.pujaBody).text =
            Sankalpa.injectPuja(requireContext(), OsLang.script(rite.body, lang), lang)
        root.findViewById<NestedScrollView>(R.id.pujaScroll).scrollTo(0, 0)
        prefs.edit().putString("rite", rite.item.id).apply()
    }

    private fun paintChrome(root: View) {
        val complete = practice.pujaComplete()
        val skipped = practice.pujaSkipped()
        NityaDayChrome.paint(root.findViewById(R.id.pujaSkip), skipped)
        NityaDayChrome.paint(root.findViewById(R.id.pujaComplete), complete && !skipped)
        root.findViewById<ProgressBar>(R.id.pujaDayProgress).apply {
            progress = if (complete && !skipped) 100 else 0
            contentDescription = if (complete && !skipped) "Day complete 100%" else "Day progress 0%"
        }
    }
}
