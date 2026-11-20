package com.paperdesk.launcher.ui

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.paperdesk.launcher.DeskWindow
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.CatalogueStore
import com.paperdesk.launcher.data.IndicScript
import com.paperdesk.launcher.data.MadhwaStore
import com.paperdesk.launcher.data.OsLang
import com.paperdesk.launcher.data.SettingsStore
import com.paperdesk.launcher.data.SongFavoritesStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Landscape reading surface for the public library: catalogue texts, puja texts and inline text.
 * Practice (Sandhyavandana, Japa, Nitya Parayana and their completion marks) belongs to Madhva Dvija.
 */
class ContentReaderActivity : AppCompatActivity() {

    private lateinit var titleView: TextView
    private lateinit var crumbView: TextView
    private lateinit var headView: TextView
    private lateinit var bodyView: TextView
    private lateinit var scrollView: NestedScrollView
    private lateinit var fontLabel: TextView
    private lateinit var fontDecreaseBtn: MaterialButton
    private lateinit var fontIncreaseBtn: MaterialButton

    private var readerScale = ReaderTextScale.DEFAULT_SCALE
    private var mode = MODE_INLINE
    private var lang = IndicScript.Lang.KN

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppTextSize.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val dark = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
        setContentView(R.layout.activity_content_reader)

        val root = findViewById<View>(R.id.readerActivityRoot)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val safe = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)

        lang = OsLang.get(this)
        readerScale = ReaderTextScale.get(this)
        mode = intent.getStringExtra(EXTRA_MODE).orEmpty().ifBlank { MODE_INLINE }

        titleView = findViewById(R.id.readerContentTitle)
        crumbView = findViewById(R.id.readerContentCrumb)
        headView = findViewById(R.id.readerContentHead)
        bodyView = findViewById(R.id.readerContentBody)
        scrollView = findViewById(R.id.readerContentScroll)
        fontLabel = findViewById(R.id.readerFontSizeLabel)
        fontDecreaseBtn = findViewById(R.id.readerFontDecrease)
        fontIncreaseBtn = findViewById(R.id.readerFontIncrease)

        // The library reader has no step bar and no completion marks.
        findViewById<View>(R.id.readerStepBar).visibility = View.GONE
        findViewById<View>(R.id.readerModeDivider).visibility = View.GONE
        findViewById<View>(R.id.readerModeSkip).visibility = View.GONE
        findViewById<View>(R.id.readerModeComplete).visibility = View.GONE

        findViewById<MaterialButton>(R.id.readerMenuBack).apply {
            text = OsLang.ui("back_to_menu", lang)
            contentDescription = OsLang.ui("back_to_menu", lang)
            setOnClickListener { finish() }
        }
        fontDecreaseBtn.contentDescription = OsLang.ui("text_smaller", lang)
        fontIncreaseBtn.contentDescription = OsLang.ui("text_larger", lang)

        fontDecreaseBtn.setOnClickListener {
            readerScale = ReaderTextScale.set(this, ReaderTextScale.stepDown(readerScale))
            applyReaderScale()
        }
        fontIncreaseBtn.setOnClickListener {
            readerScale = ReaderTextScale.set(this, ReaderTextScale.stepUp(readerScale))
            applyReaderScale()
        }

        applyReaderScale()
        loadContent()
    }

    override fun onResume() {
        super.onResume()
        DeskWindow.keepAwake(this, SettingsStore(this).alwaysOn())
    }

    private fun applyReaderScale() {
        fontLabel.text = ReaderTextScale.percentLabel(readerScale)
        fontDecreaseBtn.isEnabled = readerScale > ReaderTextScale.MIN_SCALE + 0.01f
        fontIncreaseBtn.isEnabled = readerScale < ReaderTextScale.MAX_SCALE - 0.01f
        headView.setTextSize(TypedValue.COMPLEX_UNIT_SP, ReaderTextScale.headSp(readerScale))
        DeskType.apply(headView, DeskType.SEMIBOLD)
        bodyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, ReaderTextScale.bodySp(readerScale))
        DeskType.reading(bodyView, ReaderTextScale.lineHeightSp(readerScale))
    }

    private fun loadContent() {
        when (mode) {
            MODE_CATALOGUE -> loadCatalogueContent()
            MODE_PUJA -> loadPujaContent()
            else -> loadInlineContent()
        }
    }

    private fun loadInlineContent() {
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val crumb = intent.getStringExtra(EXTRA_CRUMB).orEmpty()
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
        displayStatic(title, crumb, title, body)
    }

    private fun loadCatalogueContent() {
        val pageId = intent.getStringExtra(EXTRA_PAGE_ID).orEmpty()
        val itemId = intent.getStringExtra(EXTRA_ITEM_ID).orEmpty()
        val subpageId = intent.getStringExtra(EXTRA_SUBPAGE_ID).orEmpty().ifBlank { null }
        lifecycleScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                CatalogueStore.load(applicationContext)
                val item = CatalogueStore.item(itemId)
                    ?: if (subpageId != null) CatalogueStore.firstReadable(pageId, subpageId)
                    else CatalogueStore.firstReadable(pageId)
                item?.let {
                    val label = CatalogueStore.label(it, lang)
                    val group = CatalogueStore.groupsFor(pageId).firstOrNull { g -> g.id == it.group }
                    val groupLabel = group?.let { g -> CatalogueStore.groupLabel(g, lang) }.orEmpty()
                    val crumb = listOf(OsLang.ui(pageId, lang), groupLabel, label)
                        .filter { part -> part.isNotBlank() }
                        .distinct()
                        .joinToString("  ›  ")
                    val rawBody = CatalogueStore.body(applicationContext, it, lang)
                    val cleanedBody = withoutRepeatedHeading(rawBody, label)
                    Triple(label, crumb, cleanedBody)
                }
            }
            if (isFinishing) return@launch
            if (loaded != null) {
                displayStatic(loaded.first, loaded.second, loaded.first, loaded.third)
                if (pageId == "hadugalu") bindSongFavorite(itemId, loaded.first)
            } else {
                loadInlineContent()
            }
        }
    }

    /** A song can be kept in the device's favourites list. The list is not tied to a person's profile. */
    private fun bindSongFavorite(itemId: String, title: String) {
        val song = CatalogueStore.item(itemId) ?: return
        val favorites = SongFavoritesStore(this)
        findViewById<MaterialButton>(R.id.readerSongFavorite).apply {
            visibility = View.VISIBLE
            fun paintFavorite() {
                val selected = favorites.contains(song.id)
                setIconResource(if (selected) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_outline)
                iconTint = ColorStateList.valueOf(getColor(if (selected) R.color.md_theme_primary else R.color.md_theme_onSurface))
                contentDescription = (if (selected) "ಮೆಚ್ಚಿನ ಪಟ್ಟಿಯಿಂದ ತೆಗೆದುಹಾಕಿ: " else "ಮೆಚ್ಚಿನ ಪಟ್ಟಿಗೆ ಸೇರಿಸಿ: ") + title
                ViewCompat.setStateDescription(this, if (selected) "Favorite" else "Not favorite")
            }
            setOnClickListener { favorites.set(song.id, !favorites.contains(song.id)); paintFavorite() }
            paintFavorite()
        }
    }

    /** The whole puja procedure, in its source order, without any personal values. */
    private fun loadPujaContent() {
        val itemId = intent.getStringExtra(EXTRA_ITEM_ID).orEmpty()
        lifecycleScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                MadhwaStore.load(applicationContext)
                val item = MadhwaStore.item(itemId)
                    ?: (MadhwaStore.inCategory("puja") + MadhwaStore.inCategory("festival")).firstOrNull()
                item?.let {
                    Pair(MadhwaStore.label(it, lang), MadhwaStore.body(applicationContext, it, lang))
                }
            }
            if (isFinishing || loaded == null) return@launch
            val crumb = OsLang.ui("puja", lang) + "  ›  " + loaded.first
            displayStatic(loaded.first, crumb, "", loaded.second)
        }
    }

    private fun displayStatic(title: String, crumb: String, head: String, body: CharSequence) {
        titleView.text = title
        DeskType.apply(titleView, DeskType.SEMIBOLD)
        crumbView.text = crumb
        DeskType.apply(crumbView, DeskType.REGULAR)
        crumbView.visibility = if (crumb.isBlank()) View.GONE else View.VISIBLE
        if (head.isBlank()) {
            headView.visibility = View.GONE
        } else {
            headView.visibility = View.VISIBLE
            headView.text = head
        }
        bodyView.text = body
        applyReaderScale()
    }

    private fun withoutRepeatedHeading(body: String, heading: String): String {
        val lines = body.lines()
        val first = lines.indexOfFirst { it.isNotBlank() }
        if (first < 0) return body
        fun comparable(value: String) = value.lowercase()
            .replace(Regex("[\\s·:।॥|—–_-]+"), "")
        if (comparable(lines[first]) != comparable(heading)) return body
        return lines.filterIndexed { index, _ -> index != first }.joinToString("\n").trimStart()
    }

    companion object {
        const val MODE_CATALOGUE = "catalogue"
        const val MODE_PUJA = "puja"
        const val MODE_INLINE = "inline"

        const val EXTRA_MODE = "reader_mode"
        const val EXTRA_PAGE_ID = "reader_page_id"
        const val EXTRA_ITEM_ID = "reader_item_id"
        const val EXTRA_SUBPAGE_ID = "reader_subpage_id"
        const val EXTRA_TITLE = "reader_title"
        const val EXTRA_CRUMB = "reader_crumb"
        const val EXTRA_BODY = "reader_body"

        fun forCatalogue(
            context: Context,
            pageId: String,
            itemId: String,
            subpageId: String? = null
        ): Intent = Intent(context, ContentReaderActivity::class.java).apply {
            putExtra(EXTRA_MODE, MODE_CATALOGUE)
            putExtra(EXTRA_PAGE_ID, pageId)
            putExtra(EXTRA_ITEM_ID, itemId)
            if (!subpageId.isNullOrBlank()) putExtra(EXTRA_SUBPAGE_ID, subpageId)
        }

        fun forPuja(context: Context, itemId: String): Intent =
            Intent(context, ContentReaderActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_PUJA)
                putExtra(EXTRA_ITEM_ID, itemId)
            }

        fun forInline(
            context: Context,
            title: String,
            crumb: String,
            body: String
        ): Intent = Intent(context, ContentReaderActivity::class.java).apply {
            putExtra(EXTRA_MODE, MODE_INLINE)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_CRUMB, crumb)
            putExtra(EXTRA_BODY, body)
        }
    }
}
