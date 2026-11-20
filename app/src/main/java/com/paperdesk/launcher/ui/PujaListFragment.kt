package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.MadhwaStore
import com.paperdesk.launcher.data.OsLang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Puja procedures from the public library. Each one opens in the landscape reader; nothing is tracked. */
class PujaListFragment : Fragment(R.layout.page_puja_list) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        PagePalette.apply(view, "puja")
        val ctx = requireContext()
        val lang = OsLang.get(ctx)
        view.findViewById<TextView>(R.id.pujaListTitle).text = OsLang.ui("puja", lang)
        val list = view.findViewById<LinearLayout>(R.id.pujaList)
        viewLifecycleOwner.lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                MadhwaStore.load(ctx)
                (MadhwaStore.inCategory("puja") + MadhwaStore.inCategory("festival"))
                    .map { it.id to MadhwaStore.label(it, lang) }
            }
            if (!isAdded) return@launch
            list.removeAllViews()
            for ((id, label) in items) {
                list.addView(MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                    text = label
                    minHeight = resources.getDimensionPixelSize(R.dimen.ds_touch_min)
                    setOnClickListener { startActivity(ContentReaderActivity.forPuja(ctx, id)) }
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        .apply { bottomMargin = resources.getDimensionPixelSize(R.dimen.ds_space_2) }
                })
            }
        }
    }
}
