package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.OsLang

/** Secondary destinations for Carbon shell groups (cds--tile--clickable). */
class ShellHubFragment : Fragment() {
    private val hubId: String get() = requireArguments().getString(ARG_HUB).orEmpty()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.page_shell_hub, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val lang = OsLang.get(requireContext())
        view.findViewById<TextView>(R.id.hubTitle).text = OsLang.ui(hubId, lang)
        view.findViewById<TextView>(R.id.hubSubtitle).text = when (hubId) {
            "nityakarma" -> "ಸಂಧ್ಯಾವಂದನೆ, ಜಪ ಮತ್ತು ಪೂಜೆ"
            "grantha" -> "ನಿತ್ಯ ಪಾರಾಯಣ, ಸ್ತೋತ್ರ, ಸೂಕ್ತ, ಮಂತ್ರ ಮತ್ತು ನಾಮಾವಳಿ"
            "sangeeta" -> "ಹಾಡುಗಳು ಮತ್ತು ಪ್ಲೇಲಿಸ್ಟ್‌ಗಳು"
            "samskruti" -> "ಆಯಾದಿ, ರಂಗವಲ್ಲಿ ಮತ್ತು ನೈವೇದ್ಯ / ಪಾಕ"
            else -> ""
        }
        val children = view.findViewById<LinearLayout>(R.id.hubChildren)
        children.removeAllViews()
        val inflater = layoutInflater
        for (child in childrenFor(hubId)) {
            val button = inflater.inflate(R.layout.item_hub_child, children, false) as MaterialButton
            button.text = OsLang.ui(child, lang)
            button.contentDescription = button.text
            button.setIconResource(FrozenShell.iconFor(child))
            button.setOnClickListener { (activity as? LauncherActivity)?.navigateTo(child) }
            children.addView(button)
        }
        val empties = view.findViewById<LinearLayout>(R.id.hubEmptyStates)
        empties.removeAllViews()
        for (message in emptyMessages(hubId)) {
            val label = TextView(requireContext()).apply {
                this.text = message
                setTextAppearance(R.style.TextAppearance_Carbon_Body01)
                setTextColor(requireContext().getColor(R.color.carbon_text_secondary))
                setBackgroundColor(requireContext().getColor(R.color.carbon_layer))
                val pad = resources.getDimensionPixelSize(R.dimen.carbon_spacing_05)
                setPadding(pad, pad, pad, pad)
            }
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = resources.getDimensionPixelSize(R.dimen.carbon_spacing_03)
            empties.addView(label, lp)
        }
    }

    companion object {
        private const val ARG_HUB = "hub"
        fun create(hub: String) = ShellHubFragment().apply {
            arguments = Bundle().apply { putString(ARG_HUB, hub) }
        }

        fun childrenFor(hub: String): List<String> = when (hub) {
            "nityakarma" -> listOf("sandhya", "japa", "puja")
            "grantha" -> listOf("nitya", "stotra", "suktas", "mantras", "namavali")
            "sangeeta" -> listOf("hadugalu", "playlists")
            "samskruti" -> listOf("aayadi", "rangavalli", "recipes")
            else -> emptyList()
        }

        fun emptyMessages(hub: String): List<String> = emptyList()

        val RAIL_IDS = listOf("dashboard", "desk", "nityakarma", "grantha", "sangeeta", "samskruti", "settings")
    }
}
