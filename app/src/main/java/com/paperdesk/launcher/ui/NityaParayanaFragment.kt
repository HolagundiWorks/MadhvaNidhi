package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.paperdesk.launcher.R

/** Standalone host kept for legacy page ids; primary entry is the Nitya Karma tab. */
class NityaParayanaFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        ScreenPages.wrap(inflater.inflate(R.layout.page_nitya_parayana, container, false))

    override fun onViewCreated(view: View, state: Bundle?) {
        PagePalette.apply(view, "nitya")
        NityaParayanaBinder.bind(this, view)
    }
}
