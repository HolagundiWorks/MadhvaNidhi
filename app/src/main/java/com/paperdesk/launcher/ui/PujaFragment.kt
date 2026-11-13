package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.MadhwaItem
import com.paperdesk.launcher.data.MadhwaStore
import com.paperdesk.launcher.data.OsLang
import com.paperdesk.launcher.data.PracticeDayStore
import com.paperdesk.launcher.data.Sankalpa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Standalone Puja destination — rite select + single continuous scroll body. */
class PujaFragment : Fragment() {
    private var pujaRite = 0
    private lateinit var practice: PracticeDayStore
    private var rites: List<MadhwaItem> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.page_nitya_puja, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        PagePalette.apply(view, "puja")
        practice = PracticeDayStore(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            val app = requireContext().applicationContext
            val loaded = withContext(Dispatchers.IO) {
                MadhwaStore.load(app)
                preferredRites()
            }
            if (!isAdded) return@launch
            rites = loaded
            bindPuja(view)
        }
    }

    private fun preferredRites(): List<MadhwaItem> {
        val preferred = listOf(
            "madhwa_deva_pooja_guide",
            "puja_udupi",
            "festival_ganesha_chaturthi",
            "puja_anushtana"
        )
        val byId = preferred.mapNotNull { MadhwaStore.item(it) }
        if (byId.size >= 3) return byId.take(3)
        val catalog = MadhwaStore.inCategory("puja") + MadhwaStore.inCategory("festival")
        return (byId + catalog.filter { c -> byId.none { it.id == c.id } }).take(3)
    }

    private fun bindPuja(root: View) {
        if (rites.isEmpty()) {
            root.findViewById<TextView>(R.id.pujaBody).text = "—"
            return
        }
        val labels = rites.map { MadhwaStore.label(it, OsLang.get(requireContext())) }
        val riteView = root.findViewById<MaterialAutoCompleteTextView>(R.id.pujaRite)
        MaterialChoices.bind(riteView, labels, 0) {
            pujaRite = it
            practice.setPujaDay(complete = false, skipped = false)
            paintPuja(root)
        }
        root.findViewById<View>(R.id.pujaSkip).setOnClickListener {
            val on = !practice.pujaSkipped()
            practice.setPujaDay(complete = false, skipped = on)
            paintPuja(root)
        }
        root.findViewById<View>(R.id.pujaComplete).setOnClickListener {
            val on = !practice.pujaComplete()
            practice.setPujaDay(complete = on, skipped = false)
            paintPuja(root)
        }
        paintPuja(root)
    }

    private fun paintPuja(root: View) {
        val item = rites.getOrNull(pujaRite) ?: return
        val lang = OsLang.get(requireContext())
        val riteLabel = MadhwaStore.label(item, lang)
        val body = Sankalpa.injectPuja(
            requireContext(),
            MadhwaStore.body(requireContext(), item, lang),
            lang
        )
        val done = practice.pujaComplete()
        val skipped = practice.pujaSkipped()
        root.findViewById<TextView>(R.id.pujaStep).text = riteLabel
        root.findViewById<TextView>(R.id.pujaBody).text = body
        root.findViewById<TextView>(R.id.pujaCrumb).text = "ಪೂಜೆ · $riteLabel"
        root.findViewById<ProgressBar>(R.id.pujaProgress).progress = if (done) 100 else 0
        NityaDayChrome.paint(root.findViewById(R.id.pujaSkip), skipped)
        NityaDayChrome.paint(root.findViewById(R.id.pujaComplete), done && !skipped)
    }
}
