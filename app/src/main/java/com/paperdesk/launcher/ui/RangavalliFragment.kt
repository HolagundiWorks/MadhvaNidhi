package com.paperdesk.launcher.ui

import android.content.Intent
import android.content.Context
import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.RangavalliStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Offline, theme-aware line-art gallery within the Samskruti tabs. */
class RangavalliFragment : Fragment(R.layout.page_rangavalli) {
    private var filter = 0
    private var detail: androidx.appcompat.app.AlertDialog? = null
    private val categories = listOf("all", "floral", "geometric", "lamps")
    private fun category(id: String) = when (id) {
        "floral" -> "ಹೂವಿನ ವಿನ್ಯಾಸ"
        "geometric" -> "ಜ್ಯಾಮಿತಿ ವಿನ್ಯಾಸ"
        "lamps" -> "ದೀಪ ವಿನ್ಯಾಸ"
        else -> "ಎಲ್ಲ ವಿನ್ಯಾಸಗಳು"
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        filter = savedInstanceState?.getInt("filter")?.coerceIn(0, categories.lastIndex) ?: 0
        val grid = view.findViewById<RecyclerView>(R.id.rangavalliGrid)
        grid.layoutManager = GridLayoutManager(requireContext(), ((resources.configuration.screenWidthDp - 32) / 160).coerceIn(1,4))
        viewLifecycleOwner.lifecycleScope.launch {
            val context = requireContext().applicationContext
            val entries = withContext(Dispatchers.IO) { RangavalliStore.load(context).map { it to bitmap(context, it.asset, 4) } }
            fun show() {
                val items = entries.filter { filter == 0 || it.first.category == categories[filter] }
                grid.adapter = GalleryAdapter(items) { showDesign(it) }
                view.findViewById<TextView>(R.id.rangavalliCount).text = "${items.size} ವಿನ್ಯಾಸಗಳು · ಆಫ್‌ಲೈನ್ ಚಿತ್ರಗಳು"
            }
            MaterialChoices.bind(view.findViewById<MaterialAutoCompleteTextView>(R.id.rangavalliFilter),
                categories.map { category(it) }, filter) { filter = it; show() }
            view.findViewById<MaterialAutoCompleteTextView>(R.id.rangavalliFilter).apply {
                setOnClickListener { showDropDown() }
            }
            show()
        }
    }
    private fun bitmap(context: Context, path: String, sample: Int): Bitmap = context.assets.open(path).use {
        requireNotNull(BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }))
    }
    private fun showDesign(design: RangavalliStore.Design) {
        viewLifecycleOwner.lifecycleScope.launch {
            val context = requireContext()
            val image = withContext(Dispatchers.IO) { bitmap(context.applicationContext, design.asset, 2) }
            val density = resources.displayMetrics.density
            val pad = (16 * density).toInt()
            val body = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(pad,pad,pad,pad) }
            body.addView(ImageView(context).apply {
                setImageBitmap(image)
                setColorFilter(context.getColor(R.color.md_theme_onSurface), PorterDuff.Mode.SRC_IN)
                scaleType = ImageView.ScaleType.FIT_CENTER
                contentDescription = design.titleKn
            }, LinearLayout.LayoutParams(-1, (280 * density).toInt()))
            fun paragraph(value: String) = TextView(context).apply {
                text = value
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge)
                setTextColor(context.getColor(R.color.md_theme_onSurface))
                setPadding(0,pad,0,0)
            }
            body.addView(paragraph("ವಿಧಾನ · ಮುಕ್ತಹಸ್ತ ಅಭ್ಯಾಸ"))
            design.steps.forEachIndexed { i, step -> body.addView(paragraph("${i+1}. $step")) }
            body.addView(paragraph("MadhvaNidhi ರೇಖಾ ವಿನ್ಯಾಸ · ಪರಂಪರೆಯ ಜ್ಯಾಮಿತಿ ಆಧಾರಿತ ಅಭ್ಯಾಸಚಿತ್ರ."))
            body.addView(MaterialButton(context, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
                text = "ಉಲ್ಲೇಖ ಬ್ಲಾಗ್ ನೋಡಿ"
                minHeight = (56 * density).toInt()
                setOnClickListener {
                    val uri = Uri.parse(design.sourceUrl)
                    if (uri.scheme == "https" && uri.host in listOf("www.rangoli-sans-dots.com", "rangolidesigns.net")) {
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        try { startActivity(intent) } catch (_: ActivityNotFoundException) {
                            android.widget.Toast.makeText(context, "Browser unavailable", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
            detail?.dismiss()
            detail = MaterialAlertDialogBuilder(context).setTitle(design.titleKn)
                .setView(ScrollView(context).apply { addView(body) })
                .setPositiveButton("ಮುಚ್ಚಿ", null).show()
            detail?.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(context.getColor(R.color.md_theme_primary))
        }
    }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); outState.putInt("filter",filter) }
    override fun onDestroyView() { detail?.dismiss(); detail = null; super.onDestroyView() }
    private class GalleryAdapter(private val entries: List<Pair<RangavalliStore.Design,Bitmap>>, private val select: (RangavalliStore.Design) -> Unit) : RecyclerView.Adapter<GalleryAdapter.Holder>() {
        class Holder(val card: MaterialCardView) : RecyclerView.ViewHolder(card)
        override fun onCreateViewHolder(parent: ViewGroup, type: Int) = Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_rangavalli, parent, false) as MaterialCardView)
        override fun getItemCount() = entries.size
        override fun onBindViewHolder(holder: Holder, position: Int) {
            val (design,image) = entries[position]
            holder.card.findViewById<ImageView>(R.id.rangavalliImage).setImageBitmap(image)
            holder.card.findViewById<TextView>(R.id.rangavalliTitle).text = design.titleKn
            holder.card.findViewById<TextView>(R.id.rangavalliCategory).text = when(design.category) { "floral" -> "ಹೂವು"; "lamps" -> "ದೀಪ"; else -> "ಜ್ಯಾಮಿತಿ" }
            holder.card.contentDescription = "${design.titleKn}. ದೊಡ್ಡ ಚಿತ್ರ ಮತ್ತು ವಿಧಾನ ನೋಡಿ"
            holder.card.setOnClickListener { select(design) }
        }
    }
}
