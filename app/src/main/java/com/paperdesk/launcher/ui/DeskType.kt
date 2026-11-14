package com.paperdesk.launcher.ui

import android.content.Context
import android.graphics.Typeface
import android.widget.TextView
import com.paperdesk.launcher.R
import com.paperdesk.launcher.data.IndicScript
import com.paperdesk.launcher.data.OsLang
import java.util.concurrent.ConcurrentHashMap

object DeskType {
    const val REGULAR = 400
    const val MEDIUM = 500
    const val SEMIBOLD = 600

    private val cache = ConcurrentHashMap<String, Typeface>()

    fun face(weight: Int): Typeface = latin(weight)

    fun face(context: Context, weight: Int): Typeface = assetFace(context, OsLang.get(context), weight)

    fun apply(tv: TextView, weight: Int) {
        val text = tv.text?.toString().orEmpty()
        val hasSa = text.any { it in '\u0900'..'\u097F' }
        val hasKn = text.any { it in '\u0C80'..'\u0CFF' }
        val lang = when {
            hasSa && !hasKn -> IndicScript.Lang.SA
            hasKn -> IndicScript.Lang.KN
            else -> OsLang.get(tv.context)
        }
        tv.typeface = assetFace(tv.context, lang, weight)
        tv.includeFontPadding = true
        tv.paint.isFakeBoldText = false
        tv.setLineSpacing(0f, 1f)
    }

    fun primary(tv: TextView) {
        apply(tv, SEMIBOLD)
        tv.alpha = 1f
        tv.setTextColor(tv.context.getColor(R.color.white))
    }

    fun secondary(tv: TextView) {
        apply(tv, MEDIUM)
        tv.alpha = 1f
        tv.setTextColor(tv.context.getColor(R.color.white_90))
    }

    fun supporting(tv: TextView) {
        apply(tv, REGULAR)
        tv.alpha = 1f
        tv.setTextColor(tv.context.getColor(R.color.white_70))
    }

    fun meta(tv: TextView) {
        apply(tv, REGULAR)
        tv.alpha = 1f
        tv.setTextColor(tv.context.getColor(R.color.white_55))
    }

    private fun assetFace(context: Context, lang: IndicScript.Lang, weight: Int): Typeface {
        val asset = assetFor(lang, weight) ?: return latin(weight)
        return cache.getOrPut(asset) {
            runCatching { Typeface.createFromAsset(context.assets, asset) }.getOrElse { latin(weight) }
        }
    }

    private fun assetFor(lang: IndicScript.Lang, weight: Int): String? {
        val bold = weight >= 600
        return when (lang) {
            IndicScript.Lang.KN ->
                if (bold) "fonts/NotoSansKannada-SemiBold.ttf" else "fonts/NotoSansKannada-Regular.ttf"
            IndicScript.Lang.SA ->
                if (bold) "fonts/NotoSansDevanagari-SemiBold.ttf" else "fonts/NotoSansDevanagari-Regular.ttf"
            IndicScript.Lang.EN -> null
        }
    }

    private fun latin(weight: Int): Typeface {
        return if (android.os.Build.VERSION.SDK_INT >= 28) {
            Typeface.create(Typeface.SANS_SERIF, weight, false)
        } else if (weight >= 600) {
            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        } else {
            Typeface.SANS_SERIF
        }
    }
}
