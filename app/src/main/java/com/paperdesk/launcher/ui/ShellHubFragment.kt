package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.OsLang

/** Secondary destinations for Material shell groups (cds--tile--clickable). */
class ShellHubFragment : Fragment() {
    private val hubId: String get() = requireArguments().getString(ARG_HUB).orEmpty()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.page_shell_hub, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val lang = OsLang.get(requireContext())
        view.findViewById<TextView>(R.id.hubTitle).text = OsLang.ui(hubId, lang)
        view.findViewById<TextView>(R.id.hubSubtitle).text = when (hubId) {
            "nityakarma" -> "ಸಂಧ್ಯಾವಂದನೆ, ಜಪ ಮತ್ತು ನಿತ್ಯ ಪಾರಾಯಣ"
            "grantha" -> "ಸ್ತೋತ್ರ, ಸೂಕ್ತ, ಮಂತ್ರ ಮತ್ತು ನಾಮಾವಳಿ"
            "sangeeta" -> "ದಾಸರ ಪದಗಳು, ಸುಳಾದಿಗಳು, ಉಗಾಭೋಗಗಳು ಮತ್ತು ಮೆಚ್ಚಿನವು"
            "samskruti" -> "ಆಯಾದಿ, ರಂಗವಲ್ಲಿ ಮತ್ತು ನೈವೇದ್ಯ / ಪಾಕ"
            else -> ""
        }
        val children = view.findViewById<GridLayout>(R.id.hubChildren)
        children.removeAllViews()
        val inflater = layoutInflater
        val gap = resources.getDimensionPixelSize(R.dimen.ds_space_2)
        val list = childrenFor(hubId)
        list.forEachIndexed { index, child ->
            val button = inflater.inflate(R.layout.item_hub_child, children, false) as MaterialButton
            button.text = OsLang.ui(child, lang)
            button.contentDescription = button.text
            button.setIconResource(FrozenShell.iconFor(child))
            button.setOnClickListener { (activity as? LauncherActivity)?.navigateTo(child) }
            val col = index % 2
            val row = index / 2
            val params = GridLayout.LayoutParams(
                GridLayout.spec(row, GridLayout.FILL, 1f),
                GridLayout.spec(col, GridLayout.FILL, 1f)
            ).apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                setMargins(if (col == 0) 0 else gap / 2, 0, if (col == 0) gap / 2 else 0, gap)
            }
            children.addView(button, params)
        }
        val empties = view.findViewById<LinearLayout>(R.id.hubEmptyStates)
        empties.removeAllViews()
        for (message in emptyMessages(hubId)) {
            val label = TextView(requireContext()).apply {
                this.text = message
                setTextAppearance(R.style.TextAppearance_MadhvaNidhi_Body01)
                setTextColor(requireContext().getColor(R.color.md_text_secondary))
                setBackgroundColor(requireContext().getColor(R.color.md_layer))
                val pad = resources.getDimensionPixelSize(R.dimen.ds_space_4)
                setPadding(pad, pad, pad, pad)
            }
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = resources.getDimensionPixelSize(R.dimen.ds_space_2)
            empties.addView(label, lp)
        }
    }

    companion object {
        private const val ARG_HUB = "hub"
        fun create(hub: String) = ShellHubFragment().apply {
            arguments = Bundle().apply { putString(ARG_HUB, hub) }
        }

        fun childrenFor(hub: String): List<String> = when (hub) {
            "nityakarma" -> listOf("sandhya", "japa", "nitya")
            "grantha" -> listOf("stotra", "suktas", "mantras", "namavali", "wiki")
            "sangeeta" -> listOf("hadugalu", "favorites")
            "samskruti" -> listOf("aayadi", "rangavalli", "recipes")
            else -> emptyList()
        }

        /** Hubs that host children as contained Material tabs (not a flat tile grid). */
        val TAB_HUBS = listOf("grantha", "samskruti", "sangeeta")

        fun emptyMessages(hub: String): List<String> = emptyList()

        val RAIL_IDS = listOf(
            "dashboard", "desk", "nityakarma", "puja", "grantha", "sangeeta", "samskruti", "settings"
        )
    }
}
