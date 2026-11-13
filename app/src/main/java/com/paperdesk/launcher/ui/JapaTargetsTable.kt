package com.paperdesk.launcher.ui

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.JapaCatalog
import com.paperdesk.launcher.data.JapaGoals
import com.paperdesk.launcher.data.JapaStore
import com.paperdesk.launcher.data.ProfileStore
import java.text.NumberFormat
import java.util.Locale

/** Carbon-style japa targets table: Mantra · Daily · Sankalpa · Cumulative. */
object JapaTargetsTable {
    private val countFormat = NumberFormat.getIntegerInstance(Locale("en", "IN"))

    fun paintReadonly(host: LinearLayout, context: Context) {
        host.removeAllViews()
        val profile = ProfileStore(context).active()
        val store = JapaStore(context)
        val ids = (profile.dikshaMantras + profile.japaTargets.keys).distinct()
        if (ids.isEmpty()) {
            host.addView(hint(context, "—"))
            return
        }
        host.addView(headerRow(context))
        ids.forEach { id ->
            val c = store.counts(id)
            val label = JapaCatalog.entries.firstOrNull { it.id == id }?.label ?: id
            val sankalpaLabel = if (c.sankalpaConfigured > 0) {
                countFormat.format(c.sankalpaConfigured)
            } else {
                "Auto · ${countFormat.format(c.sankalpa)}"
            }
            host.addView(
                dataRow(
                    context,
                    label,
                    countFormat.format(c.dailyTarget),
                    sankalpaLabel,
                    countFormat.format(c.cumulative)
                )
            )
        }
    }

    fun paintEditable(
        host: LinearLayout,
        context: Context,
        selectedIds: Collection<String>,
        dailyMap: MutableMap<String, Long>,
        sankalpaMap: MutableMap<String, Long>
    ) {
        host.removeAllViews()
        host.addView(headerRow(context, editable = true))
        if (selectedIds.isEmpty()) {
            host.addView(hint(context, "Select diksha mantras to set targets"))
            return
        }
        val dailyLabels = JapaGoals.DAILY.map { countFormat.format(it) }
        val sankalpaLabels = listOf("Auto") + JapaGoals.SANKALPA.map { countFormat.format(it) }
        selectedIds.forEach { id ->
            val label = JapaCatalog.entries.firstOrNull { it.id == id }?.label ?: id
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(context, 8), 0, dp(context, 8))
            }
            row.addView(cell(context, label, 1.4f, bold = true))
            val daily = MaterialAutoCompleteTextView(context)
            val sankalpa = MaterialAutoCompleteTextView(context)
            val dailyIdx = JapaGoals.DAILY.indexOf(dailyMap[id] ?: JapaGoals.DEFAULT_DAILY).coerceAtLeast(0)
            val configured = sankalpaMap[id] ?: JapaGoals.SANKALPA_AUTO
            val sankalpaIdx = if (configured <= 0) 0
            else (JapaGoals.SANKALPA.indexOf(configured).takeIf { it >= 0 } ?: 0) + 1
            MaterialChoices.bind(daily, dailyLabels, dailyIdx) {
                dailyMap[id] = JapaGoals.DAILY[it]
            }
            MaterialChoices.bind(sankalpa, sankalpaLabels, sankalpaIdx) {
                sankalpaMap[id] = if (it == 0) JapaGoals.SANKALPA_AUTO else JapaGoals.SANKALPA[it - 1]
            }
            daily.hint = "Daily"
            sankalpa.hint = "Sankalpa"
            row.addView(daily, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(context, 8)
            })
            row.addView(sankalpa, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(context, 8)
            })
            host.addView(row)
            host.addView(divider(context))
        }
    }

    private fun headerRow(context: Context, editable: Boolean = false): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(context, 4), 0, dp(context, 4))
        }
        row.addView(cell(context, "Mantra", if (editable) 1.4f else 1.4f, secondary = true))
        row.addView(cell(context, "Daily", 1f, secondary = true))
        row.addView(cell(context, "Sankalpa", 1.2f, secondary = true))
        if (!editable) row.addView(cell(context, "Cumulative", 1f, secondary = true))
        return row.also {
            // subtle bottom rule
        }.also { host ->
            // wrap with divider after
        }.let { header ->
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(header)
                addView(divider(context))
            }
        }
    }

    private fun dataRow(
        context: Context,
        mantra: String,
        daily: String,
        sankalpa: String,
        cumulative: String
    ): View {
        val wrap = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(context, 8), 0, dp(context, 8))
        }
        row.addView(cell(context, mantra, 1.4f))
        row.addView(cell(context, daily, 1f))
        row.addView(cell(context, sankalpa, 1.2f))
        row.addView(cell(context, cumulative, 1f))
        wrap.addView(row)
        wrap.addView(divider(context))
        return wrap
    }

    private fun cell(
        context: Context,
        text: String,
        weight: Float,
        secondary: Boolean = false,
        bold: Boolean = false
    ): TextView = TextView(context).apply {
        this.text = text
        setTextAppearance(
            if (secondary) R.style.TextAppearance_Carbon_Label01
            else R.style.TextAppearance_Carbon_BodyCompact01
        )
        setTextColor(
            context.getColor(
                if (secondary) R.color.carbon_text_secondary else R.color.carbon_text
            )
        )
        if (bold) setTypeface(typeface, Typeface.BOLD)
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight)
    }

    private fun divider(context: Context): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            context.resources.getDimensionPixelSize(R.dimen.carbon_border_width)
        )
        setBackgroundColor(context.getColor(R.color.carbon_border_subtle))
    }

    private fun hint(context: Context, text: String): TextView = TextView(context).apply {
        this.text = text
        setTextAppearance(R.style.TextAppearance_Carbon_BodyCompact01)
        setTextColor(context.getColor(R.color.carbon_text_secondary))
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
