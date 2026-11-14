package com.paperdesk.launcher.ui

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.paperdesk.launcher.DeskWindow
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.CatalogueStore
import com.paperdesk.launcher.data.IndicScript
import com.paperdesk.launcher.data.MadhwaStore
import com.paperdesk.launcher.data.NityaParayanaStore
import com.paperdesk.launcher.data.OsLang
import com.paperdesk.launcher.data.PracticeDayStore
import com.paperdesk.launcher.data.SandhyaStore
import com.paperdesk.launcher.data.Sankalpa
import com.paperdesk.launcher.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Landscape reading surface launched from portrait selection screens. */
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

    // Sandhya state
    private var sandhyaKala = SandhyaStore.PRATAH
    private var sandhyaStepIdx = 0
    private var sandhyaAllSteps = false
    private var sandhyaSkipped = false
    private var sandhyaDone = false
    private var sandhyaSteps: List<SandhyaStore.Step> = emptyList()
    private var sandhyaTitle = ""

    // Puja state
    private var pujaItemId = ""
    private var pujaStepIdx = 0
    private var pujaAllSteps = false
    private var pujaDone = false
    private var pujaSteps: List<Pair<String, String>> = emptyList()
    private var pujaTitle = ""

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
            MODE_NITYA -> loadNityaParayanaContent()
            MODE_SANDHYA -> loadSandhyaContent()
            MODE_PUJA -> loadPujaContent()
            MODE_JAPA_ANGA -> loadJapaAngaContent()
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
                if (pageId == "hadugalu") {
                    CatalogueStore.item(itemId)?.let { song ->
                        val favorites = com.paperdesk.launcher.data.SongFavoritesStore(this@ContentReaderActivity)
                        findViewById<MaterialButton>(R.id.readerSongFavorite).apply {
                            visibility = View.VISIBLE
                            fun paintFavorite() {
                                val selected = favorites.contains(song.id)
                                setIconResource(if (selected) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_outline)
                                iconTint = ColorStateList.valueOf(getColor(if (selected) R.color.md_theme_primary else R.color.md_theme_onSurface))
                                contentDescription = (if (selected) "ಮೆಚ್ಚಿನ ಪಟ್ಟಿಯಿಂದ ತೆಗೆದುಹಾಕಿ: " else "ಮೆಚ್ಚಿನ ಪಟ್ಟಿಗೆ ಸೇರಿಸಿ: ") + loaded.first
                                ViewCompat.setStateDescription(this, if (selected) "Favorite" else "Not favorite")
                            }
                            setOnClickListener { favorites.set(song.id,!favorites.contains(song.id)); paintFavorite() }
                            paintFavorite()
                        }
                    }
                }
            } else {
                loadInlineContent()
            }
        }
    }

    private fun loadNityaParayanaContent() {
        val index = intent.getIntExtra(EXTRA_NITYA_INDEX, 0)
        lifecycleScope.launch {
            val section = withContext(Dispatchers.IO) {
                val list = NityaParayanaStore.load(applicationContext)
                list.getOrNull(index.coerceIn(0, (list.size - 1).coerceAtLeast(0)))
            }
            if (isFinishing) return@launch
            if (section != null) {
                val crumb = "${OsLang.ui("nityakarma", lang)}  ›  ${OsLang.ui("nitya", lang)}"
                displayStatic(section.title, crumb, section.title, section.body)
            } else {
                loadInlineContent()
            }
        }
    }

    private fun loadSandhyaContent() {
        sandhyaKala = intent.getStringExtra(EXTRA_KALA_ID).orEmpty().ifBlank { SandhyaStore.nowId() }
        sandhyaStepIdx = intent.getIntExtra(EXTRA_STEP_INDEX, 0)
        sandhyaAllSteps = intent.getBooleanExtra(EXTRA_FULL_SESSION, false)
        val practice = PracticeDayStore(this)
        sandhyaDone = practice.isSandhyaSessionDone(sandhyaKala)

        lifecycleScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                SandhyaStore.load(applicationContext)
                SandhyaStore.session(applicationContext, sandhyaKala)
                    ?: SandhyaStore.session(applicationContext, SandhyaStore.nowId())
            }
            if (isFinishing || loaded == null) return@launch
            sandhyaKala = loaded.id
            sandhyaTitle = SandhyaStore.label(loaded.title, lang)
            sandhyaSteps = loaded.steps
            bindSandhyaStepBar(practice)
            paintSandhya(practice)
        }
    }

    private fun bindSandhyaStepBar(practice: PracticeDayStore) {
        val stepBar = findViewById<View>(R.id.readerStepBar)
        val modeDivider = findViewById<View>(R.id.readerModeDivider)
        stepBar.visibility = View.VISIBLE
        modeDivider.visibility = View.VISIBLE

        val prevBtn = findViewById<MaterialButton>(R.id.readerStepPrev)
        val nextBtn = findViewById<MaterialButton>(R.id.readerStepNext)
        val allBtn = findViewById<MaterialButton>(R.id.readerStepAll)
        val skipBtn = findViewById<MaterialButton>(R.id.readerModeSkip)
        val completeBtn = findViewById<MaterialButton>(R.id.readerModeComplete)

        prevBtn.visibility = View.GONE
        nextBtn.visibility = View.GONE
        allBtn.setIconResource(R.drawable.ic_chevron_down)
        allBtn.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_END
        allBtn.setOnClickListener {
            val labels = listOf(OsLang.ui("full_session", lang)) +
                sandhyaSteps.mapIndexed { index, step -> "${OsLang.digits(index + 1, lang)}. ${SandhyaStore.label(step.title, lang)}" }
            com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(if (lang == IndicScript.Lang.EN) "Select section" else "ಭಾಗ ಆಯ್ಕೆಮಾಡಿ")
                .setSingleChoiceItems(labels.toTypedArray(), if (sandhyaAllSteps) 0 else sandhyaStepIdx + 1) { dialog, which ->
                    sandhyaAllSteps = which == 0
                    if (which > 0) sandhyaStepIdx = which - 1
                    paintSandhya(practice)
                    scrollView.scrollTo(0, 0)
                    dialog.dismiss()
                }
                .setNegativeButton(OsLang.ui("cancel", lang), null).show()
                .getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(getColor(R.color.md_theme_primary))
        }
        fun confirmStatus(complete: Boolean) {
            val targetKala = sandhyaKala
            val date = com.paperdesk.launcher.data.PanchangaStore.dateKey()
            val marked = if (complete) practice.isSandhyaSessionDone(targetKala, date)
                else practice.isSandhyaSkipped(targetKala, date)
            SandhyaDayConfirmation.show(this, sandhyaTitle, complete, marked) {
                practice.setSandhyaDay(targetKala, complete = complete && !marked,
                    skipped = !complete && !marked, date = date)
                paintSandhya(practice)
            }
        }
        skipBtn.setOnClickListener { confirmStatus(false) }
        completeBtn.setOnClickListener { confirmStatus(true) }
    }

    private fun paintSandhya(practice: PracticeDayStore) {
        if (sandhyaSteps.isEmpty()) return
        sandhyaSkipped = practice.isSandhyaSkipped(sandhyaKala)
        sandhyaDone = practice.isSandhyaSessionDone(sandhyaKala)
        sandhyaStepIdx = sandhyaStepIdx.coerceIn(0, sandhyaSteps.lastIndex)

        val prevBtn = findViewById<MaterialButton>(R.id.readerStepPrev)
        val nextBtn = findViewById<MaterialButton>(R.id.readerStepNext)
        val allBtn = findViewById<MaterialButton>(R.id.readerStepAll)
        val skipBtn = findViewById<MaterialButton>(R.id.readerModeSkip)
        val completeBtn = findViewById<MaterialButton>(R.id.readerModeComplete)
        val stepTitle = findViewById<TextView>(R.id.readerStepTitle)
        val progress = findViewById<ProgressBar>(R.id.readerStepProgress)

        allBtn.text = if (lang == IndicScript.Lang.EN) "Sections" else "ಭಾಗಗಳು"
        paintToggleChrome(skipBtn, sandhyaSkipped)
        paintToggleChrome(completeBtn, sandhyaDone && !sandhyaSkipped)

        val baseCrumb = "${OsLang.ui("nityakarma", lang)}  ›  ${OsLang.ui("sandhya", lang)}  ›  $sandhyaTitle"
        titleView.text = sandhyaTitle
        DeskType.apply(titleView, DeskType.SEMIBOLD)

        if (sandhyaAllSteps) {
            stepTitle.text = "${OsLang.ui("full_session", lang)} (${OsLang.digits(sandhyaSteps.size, lang)})"
            progress.progress = if (sandhyaDone) 100 else 0
            prevBtn.isEnabled = true
            nextBtn.isEnabled = false
            crumbView.text = baseCrumb
            headView.visibility = View.GONE
            bodyView.text = buildFullSandhyaSpannable()
        } else {
            val step = sandhyaSteps[sandhyaStepIdx]
            val stepName = SandhyaStore.label(step.title, lang)
            val num = OsLang.digits(sandhyaStepIdx + 1, lang)
            val total = OsLang.digits(sandhyaSteps.size, lang)
            stepTitle.text = "$num / $total · $stepName"
            progress.progress = if (sandhyaDone) 100 else if (sandhyaSkipped) 0 else ((sandhyaStepIdx + 1) * 100 / sandhyaSteps.size).coerceAtMost(99)
            prevBtn.isEnabled = sandhyaStepIdx > 0
            nextBtn.isEnabled = sandhyaStepIdx < sandhyaSteps.lastIndex
            crumbView.text = "$baseCrumb  ›  $stepName"
            headView.visibility = View.VISIBLE
            headView.text = "$num. $stepName"
            bodyView.text = SandhyaStore.body(this, step, sandhyaKala, lang)
        }
        applyReaderScale()
    }

    private fun buildFullSandhyaSpannable(): CharSequence {
        val textPrimary = getColor(R.color.carbon_text)
        val textSecondary = getColor(R.color.carbon_text_secondary)
        val sb = SpannableStringBuilder()
        sandhyaSteps.forEachIndexed { index, step ->
            if (index > 0) sb.append("\n\n")
            val number = OsLang.digits(step.n, lang)
            val title = SandhyaStore.label(step.title, lang)
            val body = SandhyaStore.body(this, step, sandhyaKala, lang).trim()
            val headingStart = sb.length
            sb.append("$number. $title")
            val headingEnd = sb.length
            sb.setSpan(ForegroundColorSpan(textPrimary), headingStart, headingEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.setSpan(StyleSpan(Typeface.BOLD), headingStart, headingEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.setSpan(RelativeSizeSpan(1.18f), headingStart, headingEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.append("\n$body")
            if (body.startsWith("ಸೂಚನೆ:") || body.startsWith("सूचना:") || body.startsWith("Note:")) {
                val noteStart = headingEnd + 1
                val noteEnd = noteStart + body.indexOf('\n').takeIf { it >= 0 }.let { it ?: body.length }
                sb.setSpan(ForegroundColorSpan(textSecondary), noteStart, noteEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(RelativeSizeSpan(0.88f), noteStart, noteEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return sb
    }

    private fun loadPujaContent() {
        pujaItemId = intent.getStringExtra(EXTRA_ITEM_ID).orEmpty()
        pujaStepIdx = intent.getIntExtra(EXTRA_STEP_INDEX, 0)
        lifecycleScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                MadhwaStore.load(applicationContext)
                val item = MadhwaStore.item(pujaItemId)
                    ?: (MadhwaStore.inCategory("puja") + MadhwaStore.inCategory("festival")).firstOrNull()
                item?.let {
                    val label = MadhwaStore.label(it, lang)
                    val body = Sankalpa.injectPuja(
                        applicationContext,
                        MadhwaStore.body(applicationContext, it, lang),
                        lang
                    )
                    // Keep the complete source procedure, in its original order.
                    Pair(label, listOf(label to body))
                }
            }
            if (isFinishing || loaded == null) return@launch
            pujaTitle = loaded.first
            pujaSteps = loaded.second
            pujaAllSteps = true
            findViewById<View>(R.id.readerStepBar).visibility = View.GONE
            findViewById<View>(R.id.readerModeDivider).visibility = View.GONE
            paintPuja()
        }
    }

    private fun bindPujaStepBar() {
        val stepBar = findViewById<View>(R.id.readerStepBar)
        val modeDivider = findViewById<View>(R.id.readerModeDivider)
        stepBar.visibility = View.VISIBLE
        modeDivider.visibility = View.VISIBLE

        val prevBtn = findViewById<MaterialButton>(R.id.readerStepPrev)
        val nextBtn = findViewById<MaterialButton>(R.id.readerStepNext)
        val allBtn = findViewById<MaterialButton>(R.id.readerStepAll)
        val skipBtn = findViewById<MaterialButton>(R.id.readerModeSkip)
        val completeBtn = findViewById<MaterialButton>(R.id.readerModeComplete)

        prevBtn.setOnClickListener {
            if (pujaAllSteps) {
                pujaAllSteps = false
            } else if (pujaStepIdx > 0) {
                pujaStepIdx--
                pujaDone = false
            }
            paintPuja()
            scrollView.scrollTo(0, 0)
        }
        nextBtn.setOnClickListener {
            if (pujaAllSteps) {
                pujaAllSteps = false
            } else if (pujaStepIdx < pujaSteps.lastIndex) {
                pujaStepIdx++
                if (pujaStepIdx == pujaSteps.lastIndex) pujaDone = true
            }
            paintPuja()
            scrollView.scrollTo(0, 0)
        }
        allBtn.setOnClickListener {
            pujaAllSteps = !pujaAllSteps
            paintPuja()
            scrollView.scrollTo(0, 0)
        }
        skipBtn.setOnClickListener {
            pujaDone = false
            pujaStepIdx = 0
            pujaAllSteps = false
            paintPuja()
            scrollView.scrollTo(0, 0)
        }
        completeBtn.setOnClickListener {
            pujaDone = !pujaDone
            if (pujaDone) pujaStepIdx = pujaSteps.lastIndex.coerceAtLeast(0)
            paintPuja()
        }
    }

    private fun paintPuja() {
        if (pujaSteps.isEmpty()) return
        pujaStepIdx = pujaStepIdx.coerceIn(0, pujaSteps.lastIndex)
        val prevBtn = findViewById<MaterialButton>(R.id.readerStepPrev)
        val nextBtn = findViewById<MaterialButton>(R.id.readerStepNext)
        val allBtn = findViewById<MaterialButton>(R.id.readerStepAll)
        val skipBtn = findViewById<MaterialButton>(R.id.readerModeSkip)
        val completeBtn = findViewById<MaterialButton>(R.id.readerModeComplete)
        val stepTitle = findViewById<TextView>(R.id.readerStepTitle)
        val progress = findViewById<ProgressBar>(R.id.readerStepProgress)

        allBtn.text = if (pujaAllSteps) OsLang.ui("steps", lang) else OsLang.ui("full_session", lang)
        paintToggleChrome(skipBtn, false)
        paintToggleChrome(completeBtn, pujaDone)

        titleView.text = pujaTitle
        DeskType.apply(titleView, DeskType.SEMIBOLD)
        val baseCrumb = "${OsLang.ui("puja", lang)}  ›  $pujaTitle"

        if (pujaAllSteps) {
            stepTitle.text = "${OsLang.ui("full_session", lang)} (${OsLang.digits(pujaSteps.size, lang)})"
            progress.progress = 100
            prevBtn.isEnabled = true
            nextBtn.isEnabled = false
            crumbView.text = baseCrumb
            headView.visibility = View.GONE
            bodyView.text = pujaSteps.joinToString("\n\n") { it.second }
        } else {
            val step = pujaSteps[pujaStepIdx]
            val num = OsLang.digits(pujaStepIdx + 1, lang)
            val total = OsLang.digits(pujaSteps.size, lang)
            stepTitle.text = "$num / $total · ${step.first}"
            progress.progress = if (pujaDone) 100 else ((pujaStepIdx + 1) * 100 / pujaSteps.size)
            prevBtn.isEnabled = pujaStepIdx > 0
            nextBtn.isEnabled = pujaStepIdx < pujaSteps.lastIndex
            crumbView.text = "$baseCrumb  ›  ${step.first}"
            headView.visibility = View.VISIBLE
            headView.text = step.first
            bodyView.text = step.second
        }
        applyReaderScale()
    }

    private fun loadJapaAngaContent() {
        val angaTabs = findViewById<TabLayout>(R.id.readerAngaTabs)
        val modeDivider = findViewById<View>(R.id.readerModeDivider)
        angaTabs.visibility = View.VISIBLE
        modeDivider.visibility = View.VISIBLE

        val startTab = intent.getIntExtra(EXTRA_ANGA_TAB, 0).coerceIn(0, 1)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { OsLang.ui("japa", lang) }
        val poorvanga = Sankalpa.block(this, SandhyaStore.nowId(), lang) + "\n\n" +
            IndicScript.convert("श्रीहरिप्रीत्यर्थं जपं करिष्ये।", lang)
        val uttaranga = IndicScript.convert(
            "जपसमाप्तौ\n" +
                "यदक्षरपदभ्रष्टं मात्राहीनं च यद्भवेत्।\n" +
                "तत्सर्वं क्षम्यतां देव नारायण नमोऽस्तु ते॥\n\n" +
                "कायेन वाचा मनसेन्द्रियैर्वा बुद्ध्यात्मना वा प्रकृतेः स्वभावात्।\n" +
                "करोमि यद्यत् सकलं परस्मै नारायणायेति समर्पयामि॥",
            lang
        )

        fun showAnga(tabIdx: Int) {
            val head = if (tabIdx == 0) "ಪೂರ್ವಾಂಗ · Poorvanga" else "ಉತ್ತರಾಂಗ · Uttaranga"
            val body = if (tabIdx == 0) poorvanga else uttaranga
            val crumb = "${OsLang.ui("nityakarma", lang)}  ›  ${OsLang.ui("japa", lang)}  ›  $head"
            displayStatic(title, crumb, head, body)
        }

        angaTabs.removeAllTabs()
        angaTabs.addTab(angaTabs.newTab().setText("ಪೂರ್ವಾಂಗ"), startTab == 0)
        angaTabs.addTab(angaTabs.newTab().setText("ಉತ್ತರಾಂಗ"), startTab == 1)
        CarbonTabs.equalizeLeft(angaTabs)
        angaTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                showAnga(tab.position)
                scrollView.scrollTo(0, 0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        showAnga(startTab)
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

    private fun paintToggleChrome(btn: MaterialButton, active: Boolean) {
        btn.backgroundTintList = ColorStateList.valueOf(
            getColor(if (active) R.color.carbon_layer_selected else android.R.color.transparent)
        )
        btn.iconTint = ColorStateList.valueOf(
            getColor(if (active) R.color.carbon_interactive else R.color.carbon_icon_secondary)
        )
        btn.strokeColor = ColorStateList.valueOf(
            getColor(if (active) R.color.carbon_border_interactive else R.color.carbon_border_strong)
        )
        btn.strokeWidth = resources.getDimensionPixelSize(R.dimen.carbon_border_width).coerceAtLeast(1)
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
        const val MODE_NITYA = "nitya"
        const val MODE_SANDHYA = "sandhya"
        const val MODE_PUJA = "puja"
        const val MODE_JAPA_ANGA = "japa_anga"
        const val MODE_INLINE = "inline"

        const val EXTRA_MODE = "reader_mode"
        const val EXTRA_PAGE_ID = "reader_page_id"
        const val EXTRA_ITEM_ID = "reader_item_id"
        const val EXTRA_SUBPAGE_ID = "reader_subpage_id"
        const val EXTRA_NITYA_INDEX = "reader_nitya_index"
        const val EXTRA_KALA_ID = "reader_kala_id"
        const val EXTRA_STEP_INDEX = "reader_step_index"
        const val EXTRA_FULL_SESSION = "reader_full_session"
        const val EXTRA_ANGA_TAB = "reader_anga_tab"
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

        fun forNityaParayana(context: Context, index: Int): Intent =
            Intent(context, ContentReaderActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_NITYA)
                putExtra(EXTRA_NITYA_INDEX, index)
            }

        fun forSandhya(
            context: Context,
            kalaId: String,
            stepIndex: Int = 0,
            fullSession: Boolean = false
        ): Intent = Intent(context, ContentReaderActivity::class.java).apply {
            putExtra(EXTRA_MODE, MODE_SANDHYA)
            putExtra(EXTRA_KALA_ID, kalaId)
            putExtra(EXTRA_STEP_INDEX, stepIndex)
            putExtra(EXTRA_FULL_SESSION, fullSession)
        }

        fun forPuja(context: Context, itemId: String, stepIndex: Int = 0): Intent =
            Intent(context, ContentReaderActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_PUJA)
                putExtra(EXTRA_ITEM_ID, itemId)
                putExtra(EXTRA_STEP_INDEX, stepIndex)
            }

        fun forJapaAnga(context: Context, title: String, tabIndex: Int): Intent =
            Intent(context, ContentReaderActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_JAPA_ANGA)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_ANGA_TAB, tabIndex)
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
