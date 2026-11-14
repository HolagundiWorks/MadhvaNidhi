package com.paperdesk.launcher.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.WikiStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Offline Madhva wiki: category chips, search, article view with sources and bookmarks. Material 3 styling. */
class WikiFragment : Fragment() {
    private var book: WikiStore.Book? = null
    private var category = "all"
    private var query = ""
    private var openId: String? = null
    private lateinit var content: LinearLayout
    private lateinit var listHost: LinearLayout

    private val density get() = resources.displayMetrics.density
    private fun dp(value: Int) = (value * density).toInt()

    override fun onCreateView(inflater: android.view.LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
        }
        return NestedScrollView(context).apply {
            addView(content, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        openId = savedInstanceState?.getString("article")
        viewLifecycleOwner.lifecycleScope.launch {
            book = withContext(Dispatchers.IO) { WikiStore.load(requireContext().applicationContext) }
            render()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        openId?.let { outState.putString("article", it) }
    }

    private fun render() {
        val current = book ?: return
        content.removeAllViews()
        val open = openId?.let { current.article(it) }
        if (open != null) renderArticle(open) else renderList(current)
    }

    private fun renderList(current: WikiStore.Book) {
        content.addView(label("Madhva wiki", com.google.android.material.R.style.TextAppearance_Material3_HeadlineSmall))
        content.addView(label("Offline reference. Pending entries are structure only until sourced.", com.google.android.material.R.style.TextAppearance_Material3_BodySmall, secondary = true))

        val search = TextInputEditText(requireContext()).apply {
            hint = "Search matha, Peethadhipati, concept"
            setText(query)
            setSingleLine(true)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    query = s?.toString().orEmpty()
                    renderListItems(book ?: return)
                }
            })
        }
        content.addView(com.google.android.material.textfield.TextInputLayout(requireContext()).apply {
            addView(search)
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })

        val chips = ChipGroup(requireContext()).apply { isSingleSelection = true }
        listOf(WikiStore.Category("all", "All", "ಎಲ್ಲ")).plus(current.categories).forEach { cat ->
            chips.addView(Chip(requireContext()).apply {
                text = cat.titleEn
                isCheckable = true
                isChecked = category == cat.id
                setOnClickListener { category = cat.id; renderListItems(book ?: return@setOnClickListener) }
            })
        }
        content.addView(chips, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })

        listHost = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        content.addView(listHost, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        renderListItems(current)
    }

    private fun renderListItems(current: WikiStore.Book) {
        if (!::listHost.isInitialized) return
        listHost.removeAllViews()
        val bookmarks = WikiStore.bookmarks(requireContext())
        val q = query.trim().lowercase()
        val items = current.articles.filter { a ->
            (category == "all" || a.category == category) &&
                (q.isEmpty() || listOf(a.titleEn, a.titleKn, a.summary).joinToString(" ").lowercase().contains(q))
        }
        if (items.isEmpty()) listHost.addView(label("No matching article.", com.google.android.material.R.style.TextAppearance_Material3_BodyLarge, secondary = true))
        items.forEach { a ->
            val card = MaterialCardView(requireContext()).apply {
                setCardBackgroundColor(context.getColor(R.color.md_theme_surfaceContainer))
                cardElevation = 0f
                radius = dp(16).toFloat()
                isClickable = true
                setOnClickListener { openId = a.id; render() }
            }
            val inner = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)) }
            inner.addView(label(a.titleEn, com.google.android.material.R.style.TextAppearance_Material3_TitleMedium))
            if (a.titleKn.isNotBlank()) inner.addView(label(a.titleKn, com.google.android.material.R.style.TextAppearance_Material3_BodyMedium, secondary = true))
            inner.addView(label(a.summary, com.google.android.material.R.style.TextAppearance_Material3_BodyMedium))
            inner.addView(label(statusText(a.status) + if (a.id in bookmarks) " · Bookmarked" else "", com.google.android.material.R.style.TextAppearance_Material3_LabelLarge, secondary = true))
            card.addView(inner)
            listHost.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        }
    }

    private fun renderArticle(a: WikiStore.Article) {
        val context = requireContext()
        val bookmarks = WikiStore.bookmarks(context)
        val saved = a.id in bookmarks

        val toolbar = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        toolbar.addView(MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "Back"
            setOnClickListener { openId = null; render() }
        })
        toolbar.addView(MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = if (saved) "Bookmarked" else "Bookmark"
            isChecked = saved
            setOnClickListener {
                WikiStore.setBookmarked(context, a.id, !saved)
                render()
            }
        }, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(8) })
        content.addView(toolbar)

        content.addView(label(a.titleEn, com.google.android.material.R.style.TextAppearance_Material3_HeadlineSmall).apply { setPadding(0, dp(12), 0, 0) })
        if (a.titleKn.isNotBlank()) content.addView(label(a.titleKn, com.google.android.material.R.style.TextAppearance_Material3_TitleMedium, secondary = true))
        content.addView(label(a.summary, com.google.android.material.R.style.TextAppearance_Material3_BodyLarge))
        content.addView(label(statusText(a.status), com.google.android.material.R.style.TextAppearance_Material3_LabelLarge, secondary = true))

        a.sections.forEach { s ->
            val card = MaterialCardView(context).apply {
                setCardBackgroundColor(context.getColor(R.color.md_theme_surfaceContainer))
                cardElevation = 0f
                radius = dp(16).toFloat()
            }
            val inner = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)) }
            inner.addView(label(s.heading, com.google.android.material.R.style.TextAppearance_Material3_TitleMedium))
            val body = if (s.body.isNotBlank()) s.body else "Pending source. Content not yet verified."
            inner.addView(label(body, com.google.android.material.R.style.TextAppearance_Material3_BodyMedium, secondary = s.body.isBlank()))
            card.addView(inner)
            content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) })
        }

        content.addView(label("Sources", com.google.android.material.R.style.TextAppearance_Material3_TitleLarge).apply { setPadding(0, dp(24), 0, dp(8)) })
        if (a.sources.isEmpty()) {
            content.addView(label("No citation yet. Add one before this article is marked sourced.", com.google.android.material.R.style.TextAppearance_Material3_BodyMedium, secondary = true))
        }
        a.sources.forEach { src ->
            content.addView(MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = "${src.title} · accessed ${src.accessed.ifBlank { "not recorded" }}"
                setOnClickListener { openUrl(src.url) }
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
            if (src.note.isNotBlank()) content.addView(label(src.note, com.google.android.material.R.style.TextAppearance_Material3_BodySmall, secondary = true))
        }

        if (a.related.isNotEmpty()) {
            content.addView(label("Related", com.google.android.material.R.style.TextAppearance_Material3_TitleLarge).apply { setPadding(0, dp(24), 0, dp(8)) })
            val related = ChipGroup(context)
            a.related.mapNotNull { book?.article(it) }.forEach { r ->
                related.addView(Chip(context).apply {
                    text = r.titleEn
                    setOnClickListener { openId = r.id; render() }
                })
            }
            content.addView(related)
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            // No browser available; the citation text is still shown above.
        }
    }

    private fun statusText(status: String) = when (status) {
        "sourced" -> "Sourced"
        "partial" -> "Partly sourced"
        else -> "Pending source"
    }

    private fun label(value: String, style: Int, secondary: Boolean = false) = MaterialTextView(requireContext()).apply {
        text = value
        setTextAppearance(style)
        setTextColor(context.getColor(if (secondary) R.color.md_theme_onSurfaceVariant else R.color.md_theme_onSurface))
        setLineSpacing(0f, 1.2f)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(4) }
    }
}
