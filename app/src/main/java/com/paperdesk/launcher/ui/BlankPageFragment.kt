package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.IndicScript
import com.paperdesk.launcher.data.OsLang

/** Empty shell for a grantha page. Content is added later. */
class BlankPageFragment : Fragment() {

    private var pageId = "stotra"
    private var lang = IndicScript.Lang.KN
    private val langTick: () -> Unit = { paint() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageId = arguments?.getString(ARG_ID).orEmpty().ifBlank { "stotra" }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.page_blank, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        paint()
    }

    override fun onResume() {
        super.onResume()
        OsLang.addListener(langTick)
        paint()
    }

    override fun onPause() {
        OsLang.removeListener(langTick)
        super.onPause()
    }

    private fun paint() {
        val v = view ?: return
        lang = OsLang.get(requireContext())
        v.findViewById<TextView>(R.id.blankTitle).text = OsLang.ui(pageId, lang)
        v.findViewById<TextView>(R.id.blankMeta).text = OsLang.ui("empty_page", lang)
        DeskType.apply(v.findViewById(R.id.blankTitle), DeskType.SEMIBOLD)
    }

    companion object {
        private const val ARG_ID = "page_id"

        fun create(id: String): BlankPageFragment {
            val f = BlankPageFragment()
            f.arguments = Bundle().apply { putString(ARG_ID, id) }
            return f
        }
    }
}
