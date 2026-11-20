package com.paperdesk.launcher.data.rules

import android.content.Context
import com.paperdesk.launcher.domain.aayadi.Criterion
import com.paperdesk.launcher.domain.aayadi.Formula
import com.paperdesk.launcher.domain.aayadi.FractionalPolicy
import com.paperdesk.launcher.domain.aayadi.Operand
import com.paperdesk.launcher.domain.aayadi.Parameter
import com.paperdesk.launcher.domain.aayadi.Relation
import com.paperdesk.launcher.domain.aayadi.ReviewStatus
import com.paperdesk.launcher.domain.aayadi.RulePack
import org.json.JSONObject

/**
 * Reads a versioned rule pack from a JSON asset. A pack that cannot be read throws, and the screen
 * shows a configuration error rather than guessing a rule.
 */
object RulePackLoader {
    const val KARNATAKA_ASSET = "rules/karnataka-shodashavarga.json"

    fun load(context: Context, asset: String = KARNATAKA_ASSET): RulePack =
        parse(context.assets.open(asset).bufferedReader(Charsets.UTF_8).use { it.readText() })

    fun parse(text: String): RulePack {
        val o = JSONObject(text)
        val source = o.getJSONObject("source")
        val params = o.getJSONArray("parameters")
        val parameters = (0 until params.length()).map { i -> parseParameter(params.getJSONObject(i)) }
        val notApplicable = o.getJSONArray("notApplicable").let { arr -> (0 until arr.length()).map { arr.getString(it) } }
        return RulePack(
            id = o.getString("id"),
            displayName = o.getString("displayName"),
            version = o.getString("version"),
            sourceTitle = source.getString("title"),
            chapter = source.getString("chapter"),
            verseOrPage = source.getString("verseOrPage"),
            edition = source.getString("edition"),
            translatorOrEditor = source.getString("translatorOrEditor"),
            reviewStatus = ReviewStatus.valueOf(o.getString("reviewStatus")),
            fractionalPolicy = FractionalPolicy.valueOf(o.getString("fractionalPolicy")),
            parameters = parameters,
            notApplicable = notApplicable
        )
    }

    private fun parseParameter(p: JSONObject): Parameter {
        val f = p.getJSONObject("formula")
        val labelsObj = p.getJSONObject("labels")
        val labels = labelsObj.keys().asSequence().associate { k -> k.toLong() to labelsObj.getString(k) }
        return Parameter(
            key = p.getString("key"),
            displayName = p.getString("displayName"),
            formula = Formula(
                operand = Operand.valueOf(f.getString("operand")),
                multiplier = f.getLong("multiplier"),
                modulus = f.getLong("modulus")
            ),
            labels = labels,
            criterion = parseCriterion(p.getJSONObject("criterion")),
            mandatory = p.getBoolean("mandatory"),
            sourceRef = p.getString("sourceRef")
        )
    }

    private fun parseCriterion(c: JSONObject): Criterion = when (val type = c.getString("type")) {
        "in" -> c.getJSONArray("set").let { arr -> Criterion.RemainderIn((0 until arr.length()).map { arr.getLong(it) }.toSet()) }
        "above" -> Criterion.RemainderAbove(c.getLong("threshold"))
        "compare" -> Criterion.Compare(c.getString("other"), Relation.valueOf(c.getString("relation")))
        "unsourced" -> Criterion.Unsourced(c.getString("reason"))
        "none" -> Criterion.NoCriterion
        else -> throw IllegalArgumentException("unknown criterion type: $type")
    }
}
