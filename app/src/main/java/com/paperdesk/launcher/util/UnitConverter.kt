package com.paperdesk.launcher.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class InputUnit { M, FT, IN }

object UnitConverter {

    var raw: String = ""
    var unit: InputUnit = InputUnit.M
    var stickyKind: String? = null
    var stickyImperial: Boolean = false
    var lastExpr: String = ""

    private val ftIn = Regex("""(\d+(?:\.\d+)?)'\s*-?\s*(\d+(?:\.\d+)?)?"?""")
    private val inchOnly = Regex("(\\d+(?:\\.\\d+)?)\"")

    fun display(): String {
        if (raw.isEmpty()) return "0"
        if (isPlainNumber(raw) && stickyKind != null) {
            return resultLabel(stickyKind!!, raw.toDoubleOrNull() ?: 0.0, stickyImperial)
        }
        return pretty(raw)
    }

    fun kindLabel(): String {
        val k = stickyKind ?: measure(raw.ifEmpty { "0" }).kind
        return when (k) {
            "AREA" -> "Area"
            "VOLUME" -> "Volume"
            else -> "Running Length"
        }
    }

    fun summary(): String {
        val src = lastExpr.ifBlank { raw }.ifBlank { "0" }
        val m = if (stickyKind != null && isPlainNumber(raw)) {
            measure(lastExpr.ifBlank { raw }.ifBlank { "0" }).copy(kind = stickyKind!!, imperial = stickyImperial)
        } else {
            measure(src)
        }
        val kind = when (m.kind) {
            "AREA" -> "Area"
            "VOLUME" -> "Volume"
            else -> "Running Length"
        }
        val result = when (m.kind) {
            "AREA" -> resultLabel("AREA", if (m.imperial) m.m2 / 0.09290304 else m.m2, m.imperial)
            "VOLUME" -> resultLabel("VOLUME", if (m.imperial) m.m3 / 0.028316846592 else m.m3, m.imperial)
            else -> resultLabel("LENGTH", if (m.imperial) m.meters / 0.3048 else m.meters, m.imperial)
        }
        return "${formatExpr(src, m)}  →  $kind  →  $result"
    }

    fun clear() {
        raw = ""
        lastExpr = ""
        unit = InputUnit.M
        stickyKind = null
        stickyImperial = false
    }

    fun appendDigit(d: String) {
        stickyKind = null
        stickyImperial = false
        raw = if (raw == "0") d else raw + d
    }

    fun appendFt() {
        stickyKind = null
        if (raw.isNotEmpty() && raw.last().isDigit()) raw += "'"
    }

    fun appendIn() {
        stickyKind = null
        if (raw.isNotEmpty() && raw.last().isDigit()) raw += "\""
    }

    fun appendOp(op: String) {
        stickyKind = null
        val s = if (op == "*") "x" else op
        if (raw.isEmpty()) {
            if (s == "-") raw = "-"
            return
        }
        val last = raw.last()
        if (last == '+' || last == '-' || last == 'x' || last == '*' || last == '/' || last == '^') {
            raw = raw.dropLast(1) + s
        } else {
            raw += s
        }
    }

    fun appendParen(p: String) {
        stickyKind = null
        stickyImperial = false
        raw += p
    }

    fun backspace() {
        stickyKind = null
        stickyImperial = false
        if (raw.isNotEmpty()) raw = raw.dropLast(1)
    }

    fun appendDot() {
        stickyKind = null
        val last = lastNumberChunk()
        if (last.contains(".")) return
        raw += if (raw.isEmpty() || !raw.last().isDigit()) "0." else "."
    }

    fun evaluateInPlace() {
        val expr = raw.trimEnd('x', '*', '+', '-', '/', ' ', '^')
        if (expr.isEmpty()) return
        lastExpr = expr
        val m = measure(expr)
        stickyKind = m.kind
        stickyImperial = m.imperial
        raw = when (m.kind) {
            "AREA" -> numPlain(if (m.imperial) m.m2 / 0.09290304 else m.m2)
            "VOLUME" -> numPlain(if (m.imperial) m.m3 / 0.028316846592 else m.m3)
            else -> {
                if (m.imperial) {
                    unit = InputUnit.FT
                    numPlain(m.meters / 0.3048)
                } else {
                    unit = InputUnit.M
                    CalculatorEngine.format(m.meters)
                }
            }
        }
    }

    fun numberForFn(): String {
        val m = measure(raw.ifEmpty { "0" })
        return CalculatorEngine.format(m.meters)
    }

    data class Grid(
        val m: String,
        val m2: String,
        val m3: String,
        val f: String,
        val f2: String,
        val f3: String
    )

    fun grid(): Grid {
        val m = measure(lastExpr.ifBlank { raw.ifEmpty { "0" } })
        return Grid(
            m = num(m.meters),
            m2 = num(m.m2),
            m3 = num(m.m3),
            f = num(m.meters / 0.3048),
            f2 = num(m.m2 / 0.09290304),
            f3 = num(m.m3 / 0.028316846592)
        )
    }

    data class Measure(
        val meters: Double,
        val m2: Double,
        val m3: Double,
        val kind: String,
        val imperial: Boolean
    )

    fun measure(expr: String): Measure {
        val imperial = expr.contains("'") || expr.contains("\"")
        val parts = splitFactors(expr)
        val meters = parts.map { parseLengthToMeters(it) }
        return when {
            meters.size >= 3 -> {
                val m2 = meters[0] * meters[1]
                val m3 = m2 * meters[2]
                Measure(meters[0], m2, m3, "VOLUME", imperial)
            }
            meters.size == 2 -> {
                val m2 = meters[0] * meters[1]
                Measure(meters[0], m2, 0.0, "AREA", imperial)
            }
            else -> {
                val a = meters.firstOrNull() ?: 0.0
                Measure(a, 0.0, 0.0, "LENGTH", imperial)
            }
        }
    }

    private fun splitFactors(expr: String): List<String> {
        val out = mutableListOf<String>()
        val buf = StringBuilder()
        var depth = 0
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c == '(' -> {
                    depth++; buf.append(c); i++
                }
                c == ')' -> {
                    depth--; buf.append(c); i++
                }
                depth == 0 && (c == 'x' || c == 'X' || c == '*') -> {
                    val t = buf.toString().trim()
                    if (t.isNotEmpty()) out += t
                    buf.clear()
                    i++
                }
                else -> {
                    buf.append(c); i++
                }
            }
        }
        val t = buf.toString().trim()
        if (t.isNotEmpty()) out += t
        return if (out.isEmpty()) listOf(expr.trim().ifBlank { "0" }) else out
    }

    private fun parseLengthToMeters(token: String): Double {
        val expr = imperialToMeters(token)
        return expr.toDoubleOrNull() ?: CalculatorEngine.eval(expr)
    }

    private fun imperialToMeters(token: String): String {
        var s = token.replace("x", "*", ignoreCase = true)
        s = ftIn.replace(s) { m ->
            val ft = m.groupValues[1].toDouble()
            val inch = m.groupValues[2].toDoubleOrNull() ?: 0.0
            (ft * 0.3048 + inch * 0.0254).toString()
        }
        s = inchOnly.replace(s) { m ->
            (m.groupValues[1].toDouble() * 0.0254).toString()
        }
        return s.ifBlank { "0" }
    }

    private fun formatExpr(expr: String, m: Measure): String {
        if (m.imperial) return pretty(expr)
        val factors = splitFactors(expr)
        return if (factors.size >= 2) {
            factors.joinToString(" x ") { "${pretty(it)} m" }
        } else {
            val p = pretty(expr)
            if (p.endsWith(" m")) p else "$p m"
        }
    }

    private fun pretty(expr: String): String {
        if (expr.isEmpty()) return "0"
        var s = expr.replace("x", " x ", ignoreCase = true).replace("*", " x ").replace("/", " / ")
        s = s.replace("+", " + ").replace(Regex("(?<=\\d)-"), " - ")
        s = s.replace(Regex("\\s+"), " ").trim()
        return s
    }

    private fun resultLabel(kind: String, value: Double, imperial: Boolean): String {
        val n = num(value)
        return when (kind) {
            "AREA" -> if (imperial) "$n ft2" else "$n m2"
            "VOLUME" -> if (imperial) "$n ft3" else "$n m3"
            else -> if (imperial) "$n ft" else "$n m"
        }
    }

    fun feetAndInches(meters: Double): String = compactFeet(meters)

    private fun compactFeet(meters: Double): String {
        val totalIn = meters / 0.0254
        val sign = if (totalIn < 0) "-" else ""
        val abs = kotlin.math.abs(totalIn)
        val feet = (abs / 12.0).toInt()
        val inches = abs - feet * 12.0
        return String.format(Locale.US, "%s%d'-%s\"", sign, feet, num(inches))
    }

    private fun lastNumberChunk(): String {
        var i = raw.length - 1
        while (i >= 0 && (raw[i].isDigit() || raw[i] == '.')) i--
        return raw.substring(i + 1)
    }

    private fun isPlainNumber(s: String): Boolean {
        if (s.isEmpty()) return false
        var dot = false
        s.forEach { c ->
            if (c == '.') {
                if (dot) return false
                dot = true
            } else if (!c.isDigit() && c != '-') return false
        }
        return true
    }

    private fun num(v: Double): String {
        val symbols = DecimalFormatSymbols(Locale.US)
        val fmt = DecimalFormat("#,##0.00", symbols)
        return fmt.format(v)
    }

    private fun numPlain(v: Double): String = CalculatorEngine.format(v)
}
