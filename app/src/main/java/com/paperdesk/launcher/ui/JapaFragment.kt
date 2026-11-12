package com.paperdesk.launcher.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.paperdesk.launcher.R

/** Standalone japa page — same pad UI as Nitya tab (frozen contract). */
class JapaFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.page_japa, container, false)

    override fun onViewCreated(view: View, state: Bundle?) {
        PagePalette.apply(view, "japa")
        JapaPadBinder.bind(this, view)
    }
}
