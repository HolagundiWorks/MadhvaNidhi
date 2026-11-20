package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.FestivalStore
import com.paperdesk.launcher.data.OsLang
import com.paperdesk.launcher.data.PanchangaStore

/**
 * Madhva Nidhi home: a content-discovery page instead of the practice dashboard. Today's Panchanga and observance
 * first, then entry points into the public library. Practice progress, streaks and Japa counters are in Madhva Dvija.
 */
class NidhiHomeFragment : Fragment(R.layout.page_nidhi_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        PagePalette.apply(view, "dashboard")
        val ctx = requireContext()
        val lang = OsLang.get(ctx)

        val day = PanchangaStore.today(ctx)
        FestivalStore.load(ctx)
        view.findViewById<TextView>(R.id.homeTithi).text = OsLang.script(day.tithi.uppercase().ifBlank { "-" }, lang)
        view.findViewById<TextView>(R.id.homePaksha).text = OsLang.script(day.paksha.uppercase().ifBlank { "-" }, lang)
        view.findViewById<TextView>(R.id.homeNakshatra).text = OsLang.nakshatra(day.nakshatra, lang)
        val observance = FestivalStore.label(day.date, lang)
        view.findViewById<TextView>(R.id.homeObservance).apply {
            text = observance
            visibility = if (observance.isBlank()) View.GONE else View.VISIBLE
        }

        val nav = activity as? PageNavigator
        mapOf(
            R.id.homeStotra to "stotra",
            R.id.homeGrantha to "grantha",
            R.id.homeSampada to "wiki",
            R.id.homeSangeeta to "sangeeta",
            R.id.homeSamskruti to "samskruti",
            R.id.homePuja to "puja",
            R.id.homeSettings to "settings"
        ).forEach { (id, page) ->
            view.findViewById<MaterialButton>(id).apply {
                text = OsLang.ui(page, lang)
                setOnClickListener { nav?.navigateTo(page) }
            }
        }
    }
}
