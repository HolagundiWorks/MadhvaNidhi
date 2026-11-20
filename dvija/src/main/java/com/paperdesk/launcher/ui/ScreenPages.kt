package com.paperdesk.launcher.ui

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import com.paperdesk.launcher.R

/** Discrete viewport pages made from stock views. Text is divided only between complete lines. */
object ScreenPages {
    fun first(view: View) {
        (view.getTag(R.id.contentPrevious) as? (() -> Unit))?.invoke()
    }

    fun reveal(root: View, targetId: Int) {
        val owner = controls(root)?.tag as? ViewGroup ?: return
        val target = owner.findViewById<View>(targetId) ?: return
        if (target.visibility == View.VISIBLE) (owner.getTag(R.id.contentNext) as? ((View) -> Unit))?.invoke(target)
    }

    private fun firstText(view: View): TextView? {
        if (view.visibility != View.VISIBLE) return null
        if (view is TextView && view.text.isNotBlank() && view.layout != null) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) firstText(view.getChildAt(i))?.let { return it }
        return null
    }

    fun controls(view: View): ViewGroup? {
        (view.getTag(R.id.screenPaging) as? ViewGroup)?.let { return it }
        if (view is ViewGroup) for (i in 0 until view.childCount) {
            controls(view.getChildAt(i))?.let { return it }
        }
        return null
    }

    /** Place the active page's controls next to the app dropdown, in one bottom bar. */
    fun attachControls(view: View, host: FrameLayout) {
        val incoming = controls(view) ?: return
        if (incoming.parent === host) return
        if (host.childCount > 0) {
            val outgoing = host.getChildAt(0)
            host.removeView(outgoing)
            val owner = outgoing.tag as LinearLayout
            owner.addView(outgoing, LinearLayout.LayoutParams(-1, -2))
        }
        (incoming.parent as? ViewGroup)?.removeView(incoming)
        host.addView(incoming, FrameLayout.LayoutParams(-1, -2))
    }

    fun wrap(view: View): View {
        if (view is NestedScrollView) return replace(view)
        if (view is ViewGroup) {
            for (child in (0 until view.childCount).map { view.getChildAt(it) }) {
                if (child is NestedScrollView) {
                    val index = view.indexOfChild(child)
                    val params = child.layoutParams
                    view.removeView(child)
                    view.addView(replace(child), index, params)
                }
            }
        }
        return view
    }

    private fun collectOpening(node: View, y: Int, opening: MutableList<Int>) {
        if (node.visibility == View.GONE || opening.size >= 2) {
            return
        }
        if (node is ViewGroup) {
            for (i in 0 until node.childCount) {
                val leaf = node.getChildAt(i)
                collectOpening(leaf, y + leaf.top, opening)
            }
        } else if (node is TextView && node.text.isNotBlank() && node.layout != null && node.layout.lineCount > 0) {
            opening += y + node.totalPaddingTop + node.layout.getLineBottom(0)
        } else if (node !is TextView) {
            opening += y + node.height
        }
    }

    private fun replace(scroll: NestedScrollView): View {
        val context = scroll.context
        val content = scroll.getChildAt(0)
        scroll.removeView(content)
        val container = LinearLayout(context).apply {
            id = scroll.id
            orientation = LinearLayout.VERTICAL
            layoutParams = scroll.layoutParams ?: ViewGroup.LayoutParams(-1, -1)
            setTag(R.id.screenPaging, true)
        }
        val viewport = FrameLayout(context).apply {
            id = R.id.contentViewport
            clipChildren = true
            clipToPadding = true
        }
        container.addView(viewport, LinearLayout.LayoutParams(-1, 0, 1f))
        viewport.addView(content, FrameLayout.LayoutParams(-1, -2))
        val blankArea = View(context).apply {
            isClickable = true
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        viewport.addView(blankArea, FrameLayout.LayoutParams(-1, 0))
        data class CardStyle(val background: android.content.res.ColorStateList, val elevation: Float, var plain: Boolean = false, var viewportHeight: Int = 0)
        val cardStyles = mutableMapOf<MaterialCardView, CardStyle>()
        fun prepareCards(view: View) {
            if (view is MaterialCardView) {
                val style = cardStyles.getOrPut(view) { CardStyle(view.cardBackgroundColor, view.cardElevation) }
                val plain = view.height > viewport.height || (style.plain && style.viewportHeight == viewport.height)
                style.viewportHeight = viewport.height
                if (plain != style.plain) {
                    style.plain = plain
                    view.setCardBackgroundColor(if (plain) android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT) else style.background)
                    view.cardElevation = if (plain) 0f else style.elevation
                }
                view.cardElevation = 0f
                view.strokeWidth = if (plain) 0 else context.resources.displayMetrics.density.toInt().coerceAtLeast(1)
                view.setStrokeColor(context.getColor(R.color.md_border))
            }
            if (view is ViewGroup) for (i in 0 until view.childCount) prepareCards(view.getChildAt(i))
        }
        val controls = LinearLayout(context).apply { gravity = android.view.Gravity.CENTER_VERTICAL }
        fun button(id: Int, icon: Int, description: String) = MaterialButton(context, null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            this.id = id
            contentDescription = description
            setIconResource(icon)
            iconPadding = 0
            iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
            strokeWidth = 0
            insetTop = 0
            insetBottom = 0
            minWidth = 0
            minimumWidth = 0
            setPadding(0, 0, 0, 0)
            cornerRadius = (4 * resources.displayMetrics.density).toInt()
        }
        val previous = button(R.id.contentPrevious, R.drawable.ic_nav_prev, "ಹಿಂದಿನ ವಿಷಯ ಪುಟ")
        val next = button(R.id.contentNext, R.drawable.ic_nav_next, "ಮುಂದಿನ ವಿಷಯ ಪುಟ")
        val label = MaterialTextView(context).apply {
            id = R.id.contentPageLabel
            gravity = android.view.Gravity.CENTER
            setTextAppearance(R.style.TextAppearance_MadhvaNidhi_Label01)
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        val touch = (48 * context.resources.displayMetrics.density).toInt()
        controls.addView(previous, LinearLayout.LayoutParams(touch, touch))
        controls.addView(label, LinearLayout.LayoutParams(0, -2, 1f))
        controls.addView(next, LinearLayout.LayoutParams(touch, touch))
        container.addView(controls, LinearLayout.LayoutParams(-1, -2))
        controls.tag = container
        container.setTag(R.id.screenPaging, controls)
        var pages = listOf(ContentPagination.Page(0, 1))
        var page = 0
        var pending = false
        var lastSignature = ""
        var pendingReveal: View? = null
        fun show() {
            page = page.coerceIn(0, pages.lastIndex)
            val start = pages[page].start
            val end = pages[page].end
            content.translationY = -start.toFloat()
            viewport.clipBounds = Rect(0, 0, viewport.width, (end - start).coerceAtMost(viewport.height))
            val usedHeight = (end - start).coerceAtMost(viewport.height)
            blankArea.layoutParams = (blankArea.layoutParams as FrameLayout.LayoutParams).apply {
                topMargin = usedHeight
                height = (viewport.height - usedHeight).coerceAtLeast(0)
            }
            previous.isEnabled = page > 0
            next.isEnabled = page < pages.lastIndex
            val fullLabel = "${page + 1} / ${pages.size}"
            val available = (controls.width - touch * 2).coerceAtLeast(touch)
            label.text = if (label.paint.measureText(fullLabel) > available) "${page + 1}" else fullLabel
            label.contentDescription = "ವಿಷಯ ಪುಟ ${page + 1}, ಒಟ್ಟು ${pages.size}"
        }
        container.setTag(R.id.contentPrevious, { page = 0; show() })
        container.setTag(R.id.contentNext, { target: View -> pendingReveal = target; container.requestLayout() })
        previous.setOnClickListener { page--; show() }
        next.setOnClickListener { page++; show() }
        fun paginate() {
            if (viewport.width == 0 || viewport.height == 0) return
            val density = container.resources.displayMetrics.density
            val outerToken = if (viewport.height / density < 480) R.dimen.ds_space_2
                else if (viewport.width / density >= 1000) R.dimen.ds_space_6 else R.dimen.ds_space_4
            val outer = container.resources.getDimensionPixelSize(outerToken)
            if (content.paddingLeft != outer || content.paddingTop != outer) content.setPadding(outer, outer, outer, outer)
            content.measure(View.MeasureSpec.makeMeasureSpec(viewport.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            val naturalHeight = content.measuredHeight.coerceAtLeast(1)
            if (content.layoutParams.height != naturalHeight) {
                content.layoutParams = content.layoutParams.apply { height = naturalHeight }
                return
            }
            prepareCards(content)
            val intervals = mutableListOf<IntRange>()
            val boundaries = sortedSetOf(0, naturalHeight)
            fun scan(child: View, top: Int) {
                if (child.visibility == View.GONE) return
                val bottom = top + child.height
                boundaries += top.coerceAtLeast(0)
                boundaries += bottom
                if (child is MaterialButton || child is com.google.android.material.materialswitch.MaterialSwitch ||
                    ((child is MaterialCardView || child.tag == "section") && child.height <= viewport.height)) {
                    intervals += (top + 1) until bottom
                    return
                }
                if (child is MaterialCardView) {
                    val opening = mutableListOf<Int>()
                    // Keep the opening border and label with content, avoiding orphaned card headers.
                    collectOpening(child, top, opening)
                    opening.lastOrNull()?.let { end ->
                        if (end - top <= viewport.height) intervals += (top + 1) until end
                    }
                }
                if (child is TextView && child.layout != null) {
                    val layout = child.layout
                    val textTop = top + child.totalPaddingTop
                    for (line in 0 until layout.lineCount) {
                        val ink = TextLineInk.bounds(child, line)
                        if (ink == null) {
                            boundaries += textTop + layout.getLineTop(line)
                            boundaries += textTop + layout.getLineBottom(line)
                        } else {
                            val a = textTop + ink.first
                            val b = textTop + ink.last
                            intervals += (a + 1) until b
                            boundaries += a
                            boundaries += b
                        }
                    }
                } else if (child is com.google.android.material.textfield.TextInputLayout ||
                    child is com.google.android.material.slider.Slider || child is com.google.android.material.button.MaterialButtonToggleGroup) {
                    intervals += (top + 1) until bottom
                } else if (child is ViewGroup) {
                    for (i in 0 until child.childCount) scan(child.getChildAt(i), top + child.getChildAt(i).top)
                } else intervals += (top + 1) until bottom
            }
            scan(content, 0)
            val meaningful = mutableListOf<IntRange>()
            fun collect(child: View, top: Int) {
                if (child.visibility != View.VISIBLE) return
                if (child is MaterialButton || child is com.google.android.material.materialswitch.MaterialSwitch ||
                    child is com.google.android.material.textfield.TextInputLayout ||
                    child is com.google.android.material.slider.Slider ||
                    child is com.google.android.material.button.MaterialButtonToggleGroup) {
                    meaningful += top until (top + child.height)
                } else if (child is TextView && child.layout != null) {
                    val textTop = top + child.totalPaddingTop
                    for (line in 0 until child.layout.lineCount) {
                        TextLineInk.bounds(child, line)?.let { meaningful += (textTop + it.first)..(textTop + it.last) }
                    }
                } else if (child is ViewGroup) {
                    for (index in 0 until child.childCount) {
                        val nested = child.getChildAt(index)
                        collect(nested, top + nested.top)
                    }
                }
            }
            collect(content, 0)
            val result = ContentPagination.paginate(viewport.height, naturalHeight, boundaries, intervals,
                meaningful, (16 * container.resources.displayMetrics.density).toInt())
            val signature = "${viewport.width}:${viewport.height}:$result"
            if (signature != lastSignature) {
                lastSignature = signature
                val oldStart = pages.getOrNull(page)?.start ?: 0
                pages = result
                page = pages.indexOfLast { it.start <= oldStart }.coerceIn(0, pages.lastIndex)
                show()
            }
            pendingReveal?.takeIf { it.height > 0 }?.let { target ->
                val anchor = firstText(target) ?: target
                val rect = Rect(0, 0, anchor.width, anchor.height)
                (content as ViewGroup).offsetDescendantRectToMyCoords(anchor, rect)
                val y = rect.top + if (anchor is TextView) anchor.totalPaddingTop + (TextLineInk.bounds(anchor, 0)?.first ?: 0) else 0
                page = pages.indexOfLast { it.start <= y }.coerceIn(0, pages.lastIndex)
                pendingReveal = null
                show()
            }
        }
        val layoutListener = android.view.ViewTreeObserver.OnGlobalLayoutListener {
            if (!pending) {
                pending = true
                container.post { pending = false; if (container.isAttachedToWindow) paginate() }
            }
        }
        container.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
        container.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) {
                v.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
                v.removeOnAttachStateChangeListener(this)
            }
        })
        return container
    }
}
