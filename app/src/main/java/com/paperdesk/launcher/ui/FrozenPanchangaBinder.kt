package com.paperdesk.launcher.ui

import android.content.res.ColorStateList
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.*
import java.util.Calendar
import kotlin.math.roundToInt

/** Frozen five-column calendar made entirely of stock Material/Android views. */
object FrozenPanchangaBinder {
    private val names = listOf(
        "ಪಾಡ್ಯಮಿ", "ಬಿದಿಗೆ", "ತದಿಗೆ", "ಚೌತಿ", "ಪಂಚಮಿ", "ಷಷ್ಠಿ", "ಸಪ್ತಮಿ",
        "ಅಷ್ಟಮಿ", "ನವಮಿ", "ದಶಮಿ", "ಏಕಾದಶಿ", "ದ್ವಾದಶಿ", "ತ್ರಯೋದಶಿ", "ಚತುರ್ದಶಿ"
    )
    fun tithiName(tithi: Int, phase: Int): String =
        if (tithi == 15) {
            if (phase == 0) "ಪೌರ್ಣಿಮಾ" else "ಅಮಾವಾಸ್ಯೆ"
        } else names[tithi - 1]

    fun bind(root: View, cycle: PanchangaCalendar.Cycle, selected: String, onPick: (String) -> Unit) {
        val area = root.findViewById<View>(R.id.pakshaScroll)
        val density = root.resources.displayMetrics.density
        val gap = root.resources.getDimensionPixelSize(R.dimen.carbon_spacing_03)
        fun mockScale(): Float {
            (root.getTag(R.id.panchangaScreen) as? Float)?.let { return it }
            if (root.width <= 0 || root.height <= 0) return 1f
            val portrait = root.resources.configuration.orientation ==
                android.content.res.Configuration.ORIENTATION_PORTRAIT
            return if (portrait) {
                (root.width / density / 400f).coerceIn(0.85f, 1.35f)
            } else {
                minOf(root.width / density / 960f, root.height / density / 540f)
                    .coerceIn(0.55f, 1.75f)
            }
        }
        val groups = listOf(PanchangaCalendar.tiles(cycle.shukla), PanchangaCalendar.tiles(cycle.krishna))
        val grids = listOf(
            root.findViewById<FrameLayout>(R.id.shuklaCalendarRow),
            root.findViewById(R.id.krishnaCalendarRow)
        )
        val shuklaButton = root.findViewById<MaterialButton>(R.id.calendarPrevious)
        val krishnaButton = root.findViewById<MaterialButton>(R.id.calendarNext)
        shuklaButton.isCheckable = true
        krishnaButton.isCheckable = true
        var phase = if (cycle.krishna.any { selected in it }) 1 else 0
        var signature = ""
        fun syncPakshaChrome() {
            shuklaButton.isCheckable = true
            krishnaButton.isCheckable = true
            shuklaButton.isChecked = phase == 0
            krishnaButton.isChecked = phase == 1
            val active = root.context.getColor(R.color.carbon_text)
            val idle = root.context.getColor(R.color.carbon_text_secondary)
            shuklaButton.setTextColor(if (phase == 0) active else idle)
            krishnaButton.setTextColor(if (phase == 1) active else idle)
            shuklaButton.iconTint = ColorStateList.valueOf(if (phase == 0) active else idle)
            krishnaButton.iconTint = ColorStateList.valueOf(if (phase == 1) active else idle)
            // Drive background directly — MaterialButton checked state is unreliable with custom bg.
            shuklaButton.backgroundTintList = null
            krishnaButton.backgroundTintList = null
            shuklaButton.background = androidx.appcompat.content.res.AppCompatResources.getDrawable(
                root.context,
                if (phase == 0) R.drawable.bg_paksha_choice_checked else R.drawable.bg_paksha_choice_idle
            )
            krishnaButton.background = androidx.appcompat.content.res.AppCompatResources.getDrawable(
                root.context,
                if (phase == 1) R.drawable.bg_paksha_choice_checked else R.drawable.bg_paksha_choice_idle
            )
        }
        fun rows(tiles: List<PanchangaCalendar.Tile>): Int {
            var row = 1
            var column = 0
            for (tile in tiles) {
                if (column + tile.tithis.size > 5) {
                    row++
                    column = 0
                }
                column += tile.tithis.size
            }
            return row
        }
        fun updateMoonStrip() {
            val strip = root.findViewById<View>(R.id.phaseCycle)
            val start = root.findViewById<ImageView>(R.id.phaseMoonStart)
            val mid = root.findViewById<ImageView>(R.id.phaseMoonMid)
            val end = root.findViewById<ImageView>(R.id.phaseMoonEnd)
            if (phase == 0) {
                start.setImageResource(R.drawable.ic_new_moon)
                mid.setImageResource(R.drawable.ic_half_moon)
                end.setImageResource(R.drawable.ic_full_moon)
                strip.contentDescription = "New moon to full moon"
            } else {
                start.setImageResource(R.drawable.ic_full_moon)
                mid.setImageResource(R.drawable.ic_wane_moon)
                end.setImageResource(R.drawable.ic_new_moon)
                strip.contentDescription = "Full moon to new moon"
            }
        }
        fun buildTile(tile: PanchangaCalendar.Tile, index: Int, cellWidth: Int, side: Int): FrameLayout {
            val cell = FrameLayout(root.context).apply { tag = tile }
            val button = LayoutInflater.from(root.context)
                .inflate(R.layout.item_panchanga_day, cell, false) as MaterialButton
            val civil = tile.dates.joinToString("–") {
                PanchangaStore.parse(it).get(Calendar.DAY_OF_MONTH).toString()
            }.ifBlank { "—" }
            val weekday = tile.dates.map {
                OsLang.weekdayShort(PanchangaStore.parse(it), IndicScript.Lang.KN)
            }.distinct().joinToString("/").ifBlank { "—" }
            val tithi = tile.tithis.joinToString(" + ") { tithiName(it, index) }
            // Live mock append order: .date (civil) → .weekday → .num (tithi)
            val text = "$civil\n$weekday\n$tithi"
            val scale = mockScale()
            // Mock density CSS clamps — keep ranges ordered so tiny cells never crash coerceIn.
            fun clampPx(preferred: Float, minPx: Float, maxPx: Float): Int {
                val lo = minOf(minPx, maxPx)
                val hi = maxOf(minPx, maxPx)
                return preferred.coerceIn(lo, hi).roundToInt()
            }
            // Mock density CSS: date = clamp(10, side*0.2, 21*d); weekday = clamp(8, side*0.11, 12*d); num = clamp(8, side*0.13, 14*d)
            val datePx = clampPx(side * 0.2f, 10 * density, 21 * density * scale)
            val weekdayPx = clampPx(side * 0.11f, 8 * density, 12 * density * scale)
            val numPx = clampPx(side * 0.13f, 8 * density, 14 * density * scale)
            // --tile-pad: clamp(4px, side*0.145, 16*density)
            val tilePad = clampPx(side * 0.145f, 4 * density, 16 * density * scale)
            button.text = android.text.SpannableString(text).apply {
                setSpan(
                    android.text.style.AbsoluteSizeSpan(datePx),
                    0,
                    civil.length,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                setSpan(
                    android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                    0,
                    civil.length,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                setSpan(
                    android.text.style.ForegroundColorSpan(
                        root.context.getColor(R.color.carbon_text_primary)
                    ),
                    0,
                    civil.length,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                val weekdayStart = civil.length + 1
                setSpan(
                    android.text.style.AbsoluteSizeSpan(weekdayPx),
                    weekdayStart,
                    weekdayStart + weekday.length,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                setSpan(
                    android.text.style.ForegroundColorSpan(
                        root.context.getColor(R.color.carbon_text_secondary)
                    ),
                    weekdayStart,
                    weekdayStart + weekday.length,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                val tithiStart = weekdayStart + weekday.length + 1
                setSpan(
                    android.text.style.AbsoluteSizeSpan(numPx),
                    tithiStart,
                    text.length,
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                if (11 in tile.tithis) {
                    setSpan(
                        android.text.style.ForegroundColorSpan(
                            root.context.getColor(R.color.carbon_ekadashi)
                        ),
                        tithiStart,
                        text.length,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                } else {
                    setSpan(
                        android.text.style.ForegroundColorSpan(
                            root.context.getColor(R.color.carbon_text_primary)
                        ),
                        tithiStart,
                        text.length,
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            DeskType.apply(button, DeskType.REGULAR)
            val marker = 11 in tile.tithis || 15 in tile.tithis
            val stripH = root.resources.getDimensionPixelSize(R.dimen.carbon_spacing_02)
            val topPad = if (11 in tile.tithis) stripH + (2 * density).roundToInt() else tilePad
            // TOP gravity + tighter font padding keeps Kannada tithi ink inside the fixed cell.
            val bottomPad = (tilePad * 0.65f).roundToInt().coerceAtLeast((2 * density).roundToInt())
            button.setPadding(tilePad, topPad, tilePad, bottomPad)
            button.setLineSpacing(0f, 1.0f)
            button.includeFontPadding = false
            button.maxLines = 3
            button.minimumWidth = 0
            button.minimumHeight = 0
            button.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            androidx.core.widget.TextViewCompat.setAutoSizeTextTypeWithDefaults(
                button, androidx.core.widget.TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE
            )
            val active = selected in tile.dates
            button.isCheckable = true
            button.isChecked = active
            button.isEnabled = tile.dates.isNotEmpty()
            button.alpha = if (button.isEnabled) 1f else .38f
            button.backgroundTintList = ColorStateList.valueOf(
                root.context.getColor(
                    if (active) R.color.carbon_layer_selected else R.color.carbon_layer_01
                )
            )
            button.setTextColor(root.context.getColor(R.color.carbon_text_primary))
            // Selected = layer fill only — no stroke (frozen contract)
            button.strokeWidth = 0
            button.strokeColor = ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
            button.tag = tile
            button.contentDescription =
                "${if (index == 0) "ಶುಕ್ಲಪಕ್ಷ" else "ಕೃಷ್ಣಪಕ್ಷ"}, $tithi. ${
                    tile.dates.joinToString {
                        OsLang.dateLine(PanchangaStore.parse(it), IndicScript.Lang.KN)
                    }.ifBlank { "ಸೂರ್ಯೋದಯಕ್ಕೆ ಈ ತಿಥಿ ಇಲ್ಲ" }
                }"
            androidx.core.view.ViewCompat.setTooltipText(button, button.contentDescription)
            button.setOnClickListener {
                if (tile.dates.size == 1) onPick(tile.dates.first())
                else MaterialAlertDialogBuilder(root.context)
                    .setTitle("ದಿನಾಂಕ ಆಯ್ಕೆಮಾಡಿ")
                    .setItems(
                        tile.dates.map {
                            OsLang.dateLine(PanchangaStore.parse(it), IndicScript.Lang.KN)
                        }.toTypedArray()
                    ) { _, choice -> onPick(tile.dates[choice]) }
                    .show()
            }
            cell.addView(button, FrameLayout.LayoutParams(cellWidth, side))
            if (11 in tile.tithis) {
                // Top Yellow-30 alert strip ($support-warning-inverse)
                val stripH = root.resources.getDimensionPixelSize(R.dimen.carbon_spacing_02)
                cell.addView(
                    View(root.context).apply {
                        setBackgroundColor(root.context.getColor(R.color.carbon_support_warning_inverse))
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    },
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        stripH,
                        Gravity.TOP
                    )
                )
            }
            if (marker) {
                // Mock: clamp(10px, tile-side*0.18, 16px)
                val size = (side * 0.18f).coerceIn(10 * density, 16 * density).roundToInt()
                val image = ImageView(root.context).apply {
                    setImageResource(
                        when {
                            11 in tile.tithis -> R.drawable.ic_transparency_01
                            index == 0 -> R.drawable.ic_full_moon
                            else -> R.drawable.ic_new_moon
                        }
                    )
                    imageTintList = ColorStateList.valueOf(
                        root.context.getColor(
                            if (11 in tile.tithis) {
                                R.color.carbon_support_warning_inverse
                            } else {
                                R.color.carbon_text_primary
                            }
                        )
                    )
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    tag = "tile-marker"
                }
                val markerTop = if (11 in tile.tithis) {
                    root.resources.getDimensionPixelSize(R.dimen.carbon_spacing_02) + (2 * density).roundToInt()
                } else {
                    tilePad
                }
                cell.addView(
                    image,
                    FrameLayout.LayoutParams(size, size, Gravity.TOP or Gravity.END).apply {
                        topMargin = markerTop
                        rightMargin = tilePad
                    }
                )
            }
            return cell
        }
        fun forceTileLayout(host: FrameLayout) {
            for (i in 0 until host.childCount) {
                val child = host.getChildAt(i)
                val lp = child.layoutParams as FrameLayout.LayoutParams
                if (lp.width <= 0 || lp.height <= 0) continue
                child.measure(
                    View.MeasureSpec.makeMeasureSpec(lp.width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(lp.height, View.MeasureSpec.EXACTLY)
                )
                val left = lp.leftMargin
                val top = lp.topMargin
                child.layout(left, top, left + lp.width, top + lp.height)
                if (child is FrameLayout) {
                    for (j in 0 until child.childCount) {
                        val nested = child.getChildAt(j)
                        val nestedLp = nested.layoutParams as FrameLayout.LayoutParams
                        val nestedW = if (nestedLp.width > 0) nestedLp.width else lp.width
                        val nestedH = if (nestedLp.height > 0) nestedLp.height else lp.height
                        nested.measure(
                            View.MeasureSpec.makeMeasureSpec(nestedW, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(nestedH, View.MeasureSpec.EXACTLY)
                        )
                        val nLeft = when (nestedLp.gravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
                            Gravity.END, Gravity.RIGHT -> lp.width - nestedW - nestedLp.rightMargin
                            Gravity.START, Gravity.LEFT -> nestedLp.leftMargin
                            else -> nestedLp.leftMargin
                        }
                        val nTop = when (nestedLp.gravity and Gravity.VERTICAL_GRAVITY_MASK) {
                            Gravity.BOTTOM -> lp.height - nestedH - nestedLp.bottomMargin
                            else -> nestedLp.topMargin
                        }
                        nested.layout(nLeft, nTop, nLeft + nestedW, nTop + nestedH)
                    }
                }
            }
        }
        fun paintSelection(host: FrameLayout) {
            for (i in 0 until host.childCount) {
                val cell = host.getChildAt(i) as? FrameLayout ?: continue
                val button = cell.getChildAt(0) as? MaterialButton ?: continue
                val tile = button.tag as? PanchangaCalendar.Tile ?: continue
                val active = selected in tile.dates
                button.isChecked = active
                button.backgroundTintList = ColorStateList.valueOf(
                    root.context.getColor(
                        if (active) R.color.carbon_layer_selected else R.color.carbon_layer_01
                    )
                )
                button.strokeWidth = 0
                button.strokeColor = ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
            }
        }
        fun buildPhase(index: Int, side: Int) {
            val host = grids[index]
            host.removeAllViews()
            var column = 0
            var rowIndex = 0
            for (tile in groups[index]) {
                val span = tile.tithis.size
                if (column + span > 5) {
                    column = 0
                    rowIndex++
                }
                val cellWidth = side * span + gap * (span - 1)
                val cell = buildTile(tile, index, cellWidth, side)
                host.addView(
                    cell,
                    FrameLayout.LayoutParams(cellWidth, side).apply {
                        leftMargin = column * (side + gap)
                        topMargin = rowIndex * (side + gap)
                    }
                )
                column += span
            }
            val usedRows = (rowIndex + 1).coerceAtLeast(1)
            val totalGridHeight = usedRows * side + gap * (usedRows - 1)
            host.layoutParams = host.layoutParams.apply {
                height = totalGridHeight
            }
            forceTileLayout(host)
            host.tag = "${area.width}:$side:$index"
        }
        fun reflow() {
            if (area.width <= 0 || !root.isAttachedToWindow) return
            val rowCount = groups.maxOf { rows(it) }.coerceAtLeast(1)
            val usableW = (area.width - area.paddingLeft - area.paddingRight).coerceAtLeast(0)
            val portrait = area.resources.configuration.orientation ==
                android.content.res.Configuration.ORIENTATION_PORTRAIT
            val side = if (portrait) {
                (usableW - gap * 4) / 5
            } else {
                val usableH = (area.height - area.paddingTop - area.paddingBottom).coerceAtLeast(0)
                if (usableH <= 0) return
                minOf((usableW - gap * 4) / 5, (usableH - gap * (rowCount - 1)) / rowCount)
            }
            if (side <= 0) return
            if (portrait) {
                val targetAreaH = rowCount * side + gap * (rowCount - 1) + area.paddingTop + area.paddingBottom
                if (area.layoutParams.height != targetAreaH) {
                    area.layoutParams = area.layoutParams.apply { height = targetAreaH }
                }
            }
            val key = "${usableW}:$side:$phase:$selected"
            val hostVisible = grids[phase]
            if (signature == key && hostVisible.childCount > 0 &&
                (0 until hostVisible.childCount).any { hostVisible.getChildAt(it).width > 0 }
            ) return
            val structureKey = "${area.width}:$side:$phase"
            val selectionOnly =
                signature.startsWith("${usableW}:$side:$phase:") &&
                    hostVisible.childCount > 0 &&
                    hostVisible.tag == structureKey
            signature = key
            grids.forEachIndexed { i, grid -> grid.visibility = if (i == phase) View.VISIBLE else View.GONE }
            updateMoonStrip()
            if (selectionOnly) {
                paintSelection(hostVisible)
                return
            }
            // Build only the visible paksha — other side on first switch.
            if (hostVisible.tag != structureKey || hostVisible.childCount == 0) {
                buildPhase(phase, side)
            } else {
                paintSelection(hostVisible)
            }
            hostVisible.post { forceTileLayout(hostVisible) }
        }
        syncPakshaChrome()
        shuklaButton.setOnClickListener {
            val changed = phase != 0
            phase = 0
            // Material checkable buttons toggle themselves; re-assert chrome every click.
            syncPakshaChrome()
            if (changed) reflow()
        }
        krishnaButton.setOnClickListener {
            val changed = phase != 1
            phase = 1
            syncPakshaChrome()
            if (changed) reflow()
        }
        area.setTag(R.id.calendarNext, { reflow() })
        if (area.getTag(R.id.pakshaScroll) == null) {
            area.setTag(R.id.pakshaScroll, true)
            area.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
                    (area.getTag(R.id.calendarNext) as? (() -> Unit))?.invoke()
                }
            }
        }
        area.post { reflow() }
    }
}
