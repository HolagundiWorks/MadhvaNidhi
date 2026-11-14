package com.paperdesk.launcher.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textview.MaterialTextView
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.NaivedyaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Full-width Kannada recipe cards inside the Samskruti / Naivedya tab. */
class NaivedyaFragment : Fragment(R.layout.page_naivedya) {
    private var selectedId = ""
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        selectedId = savedInstanceState?.getString("recipe").orEmpty()
        val context = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val book = withContext(Dispatchers.IO) { NaivedyaStore.load(context) }
            view.findViewById<MaterialTextView>(R.id.naivedyaOfferingNote).text = book.offeringNote
            val selected = book.recipes.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
            val selector = view.findViewById<MaterialAutoCompleteTextView>(R.id.naivedyaSelector)
            MaterialChoices.bind(selector, book.recipes.map { it.title }, selected) { index -> showRecipe(book.recipes[index]) }
            MaterialChoices.wrapLabel(selector)
            // Changing line mode can filter ArrayAdapter to the current selected title.
            // Rebind the full non-editable choice list after enabling wrapped labels.
            selector.setAdapter(android.widget.ArrayAdapter(requireContext(), R.layout.item_dropdown, book.recipes.map { it.title }))
            selector.setOnClickListener { selector.showDropDown() }
            book.recipes.getOrNull(selected)?.let { showRecipe(it) }
        }
    }
    private fun showRecipe(recipe: NaivedyaStore.Recipe) {
        val root = view ?: return
        selectedId = recipe.id
        val context = requireContext()
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        val body = root.findViewById<LinearLayout>(R.id.naivedyaContent)
        body.removeAllViews()
        fun label(value: String, style: Int, secondary: Boolean = false) = MaterialTextView(context).apply {
            text = value
            setTextAppearance(style)
            setTextColor(context.getColor(if (secondary) R.color.md_theme_onSurfaceVariant else R.color.md_theme_onSurface))
            setLineSpacing(0f, 1.2f)
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply { bottomMargin = dp(8) }
        }
        val summary = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16),dp(16),dp(16),dp(16)) }
        summary.addView(label(recipe.title, R.style.TextAppearance_DS_Headline))
        summary.addView(label(recipe.englishTitle, R.style.TextAppearance_DS_BodySmall, true))
        summary.addView(label(recipe.time, R.style.TextAppearance_DS_Body))
        summary.addView(label(recipe.batch, R.style.TextAppearance_DS_Label, true))
        body.addView(MaterialCardView(context).apply {
            setCardBackgroundColor(context.getColor(R.color.md_theme_surfaceContainer))
            cardElevation = 0f
            radius = dp(16).toFloat()
            addView(summary)
        }, LinearLayout.LayoutParams(-1,-2))
        fun heading(value: String) = label(value, R.style.TextAppearance_DS_Title).apply {
            setPadding(0,dp(24),0,dp(8))
        }
        body.addView(heading("ಪದಾರ್ಥಗಳು"))
        recipe.ingredients.forEach { ingredient ->
            val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0,dp(8),0,dp(8)); isBaselineAligned = false }
            row.addView(label(ingredient.name, R.style.TextAppearance_DS_Body), LinearLayout.LayoutParams(0,-2, 1f))
            row.addView(label(ingredient.quantity, R.style.TextAppearance_DS_Body).apply {
                gravity = android.view.Gravity.END
                setPadding(dp(12),0,0,0)
            }, LinearLayout.LayoutParams(0,-2, 1f))
            body.addView(row)
            body.addView(View(context).apply { setBackgroundColor(context.getColor(R.color.md_theme_outlineVariant)) }, LinearLayout.LayoutParams(-1,dp(1)))
        }
        body.addView(heading("ತಯಾರಿಸುವ ವಿಧಾನ"))
        recipe.steps.forEachIndexed { index, step ->
            body.addView(label("${index+1}. $step", R.style.TextAppearance_DS_Body).apply { setPadding(0,0,0,dp(12)) })
        }
        if (recipe.note.isNotBlank()) {
            body.addView(heading("ಪಾಕದ ಸೂಚನೆ"))
            body.addView(label(recipe.note, R.style.TextAppearance_DS_Body))
        }
        body.addView(heading("ಮೂಲ ಉಲ್ಲೇಖ"))
        body.addView(label(recipe.author, R.style.TextAppearance_DS_BodySmall, true))
        body.addView(MaterialButton(context, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
            text = "ಮೂಲ ಪಾಕವಿಧಾನ ನೋಡಿ"
            minHeight = dp(64)
            setTextColor(context.getColor(R.color.md_theme_primary))
            setOnClickListener {
                val uri = Uri.parse(recipe.sourceUrl)
                if (uri.scheme == "https" && uri.host in listOf("udupi-recipes.com", "smithakalluraya.com", "www.sailusfood.com", "swayampaaka.com")) {
                    try { startActivity(Intent(Intent.ACTION_VIEW,uri)) }
                    catch (_: ActivityNotFoundException) { android.widget.Toast.makeText(context,"Browser unavailable",android.widget.Toast.LENGTH_SHORT).show() }
                }
            }
        })
        root.findViewById<NestedScrollView>(R.id.naivedyaScroll).scrollTo(0,0)
    }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); outState.putString("recipe",selectedId) }
}
