package com.paperdesk.launcher.util

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import java.util.Locale

object CalculatorEngine {
    fun evaluate(expr: String): String {
        return try {
            val normalized = expr
                .replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-")
                .replace("pi", PI.toString())
                .replace("%", "/100")
                .replace("^", "^")
                .trim()
            if (normalized.isEmpty()) return "0.00"
            val result = evalSimple(normalized)
            format(result)
        } catch (_: Exception) {
            "Error"
        }
    }

    fun eval(expr: String): Double {
        return try {
            val normalized = expr
                .replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-")
                .replace("pi", PI.toString())
                .replace("%", "/100")
                .trim()
            if (normalized.isEmpty()) 0.0 else evalSimple(normalized)
        } catch (_: Exception) {
            0.0
        }
    }

    fun applyFn(value: String, fn: String): String {
        val v = value.toDoubleOrNull() ?: evaluate(value).toDoubleOrNull() ?: return "Error"
        val r = when (fn) {
            "sin" -> sin(Math.toRadians(v))
            "cos" -> cos(Math.toRadians(v))
            "tan" -> tan(Math.toRadians(v))
            "log" -> log10(v)
            "ln" -> ln(v)
            "sqrt" -> sqrt(v)
            "x2" -> v * v
            "1/x" -> 1.0 / v
            "+/-" -> -v
            else -> v
        }
        return format(r)
    }

    fun power(base: String, exp: String): String {
        val a = base.toDoubleOrNull() ?: return "Error"
        val b = exp.toDoubleOrNull() ?: return "Error"
        return format(a.pow(b))
    }

    fun format(result: Double): String {
        if (result.isNaN() || result.isInfinite()) return "Error"
        return String.format(Locale.US, "%.2f", result)
    }

    private fun evalSimple(expr: String): Double {
        val values = ArrayDeque<Double>()
        val ops = ArrayDeque<Char>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isWhitespace() -> i++
                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) i++
                    values.addLast(expr.substring(start, i).toDouble())
                }
                c == '(' -> {
                    ops.addLast(c); i++
                }
                c == ')' -> {
                    while (ops.isNotEmpty() && ops.last() != '(') apply(values, ops.removeLast())
                    if (ops.isNotEmpty() && ops.last() == '(') ops.removeLast()
                    i++
                }
                c == '+' || c == '-' || c == '*' || c == '/' || c == '^' || c == 'x' -> {
                    val op = if (c == 'x') '*' else c
                    if (c == '-' && (i == 0 || expr[i - 1] in "+-*/(^x")) {
                        i++
                        val start = i
                        while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) i++
                        values.addLast(-expr.substring(start, i).toDouble())
                    } else {
                        while (ops.isNotEmpty() && precedence(ops.last()) >= precedence(op)) {
                            apply(values, ops.removeLast())
                        }
                        ops.addLast(op)
                        i++
                    }
                }
                else -> throw IllegalArgumentException("bad char")
            }
        }
        while (ops.isNotEmpty()) apply(values, ops.removeLast())
        return values.last()
    }

    private fun precedence(op: Char): Int = when (op) {
        '+', '-' -> 1
        '*', '/' -> 2
        '^' -> 3
        else -> 0
    }

    private fun apply(values: ArrayDeque<Double>, op: Char) {
        val b = values.removeLast()
        val a = values.removeLast()
        values.addLast(
            when (op) {
                '+' -> a + b
                '-' -> a - b
                '*' -> a * b
                '/' -> a / b
                '^' -> a.pow(b)
                else -> throw IllegalArgumentException()
            }
        )
    }
}
