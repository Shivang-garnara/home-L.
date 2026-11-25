package com.example.launcherai.search

import java.text.DecimalFormat
import kotlin.math.roundToInt

/**
 * On-device Nova Microresults Engine.
 * Evaluates arithmetic expressions and unit conversions instantaneously with zero network calls.
 */
object MicroresultsEngine {

    data class Microresult(
        val title: String,
        val subtitle: String,
        val copyValue: String
    )

    private val numberFormat = DecimalFormat("#,##0.####")

    fun evaluate(rawQuery: String): Microresult? {
        val query = rawQuery.trim()
        if (query.isEmpty()) return null

        // 1. Check unit conversion patterns (e.g., "10km to miles", "100 c to f", "5kg in lb")
        evaluateUnitConversion(query)?.let { return it }

        // 2. Check arithmetic math expression (e.g., "25*4", "(12+8)/5")
        if (looksLikeMathExpression(query)) {
            evaluateMath(query)?.let { result ->
                val formatted = numberFormat.format(result)
                return Microresult(
                    title = "= $formatted",
                    subtitle = "$query",
                    copyValue = formatted.replace(",", "")
                )
            }
        }

        return null
    }

    private fun looksLikeMathExpression(s: String): Boolean {
        val hasOperator = s.any { it == '+' || it == '-' || it == '*' || it == '/' || it == '%' || it == 'x' || it == '×' || it == '÷' }
        val hasDigit = s.any { it.isDigit() }
        if (!hasOperator || !hasDigit) return false
        return s.all { it.isDigit() || it in "+-*/%(). x×÷" }
    }

    private fun evaluateMath(expr: String): Double? {
        val normalized = expr.replace(" ", "").replace("×", "*").replace("x", "*").replace("÷", "/")
        return try {
            object : Any() {
                var pos = -1
                var ch = 0

                fun nextChar() {
                    ch = if (++pos < normalized.length) normalized[pos].code else -1
                }

                fun eat(charToEat: Int): Boolean {
                    while (ch == ' '.code) nextChar()
                    if (ch == charToEat) {
                        nextChar()
                        return true
                    }
                    return false
                }

                fun parse(): Double {
                    nextChar()
                    val x = parseExpression()
                    if (pos < normalized.length) throw RuntimeException("Unexpected: ${ch.toChar()}")
                    return x
                }

                fun parseExpression(): Double {
                    var x = parseTerm()
                    while (true) {
                        when {
                            eat('+'.code) -> x += parseTerm()
                            eat('-'.code) -> x -= parseTerm()
                            else -> return x
                        }
                    }
                }

                fun parseTerm(): Double {
                    var x = parseFactor()
                    while (true) {
                        when {
                            eat('*'.code) -> x *= parseFactor()
                            eat('/'.code) -> {
                                val divisor = parseFactor()
                                if (divisor == 0.0) throw ArithmeticException("Division by zero")
                                x /= divisor
                            }
                            eat('%'.code) -> x %= parseFactor()
                            else -> return x
                        }
                    }
                }

                fun parseFactor(): Double {
                    if (eat('+'.code)) return parseFactor()
                    if (eat('-'.code)) return -parseFactor()
                    var x: Double
                    val startPos = pos
                    if (eat('('.code)) {
                        x = parseExpression()
                        if (!eat(')'.code)) throw RuntimeException("Missing ')'")
                    } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                        while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                        x = normalized.substring(startPos, pos).toDouble()
                    } else {
                        throw RuntimeException("Unexpected: ${ch.toChar()}")
                    }
                    return x
                }
            }.parse()
        } catch (e: Exception) {
            null
        }
    }

    private fun evaluateUnitConversion(query: String): Microresult? {
        val regex = Regex("""^([+-]?\d+(?:\.\d+)?)\s*([a-zA-Z°]+)\s+(?:to|in)\s+([a-zA-Z°]+)$""", RegexOption.IGNORE_CASE)
        val match = regex.matchEntire(query) ?: return null
        val value = match.groupValues[1].toDoubleOrNull() ?: return null
        val fromUnit = normalizeUnit(match.groupValues[2]) ?: return null
        val toUnit = normalizeUnit(match.groupValues[3]) ?: return null

        val result = convertUnits(value, fromUnit, toUnit) ?: return null
        val formattedVal = numberFormat.format(value)
        val formattedRes = DecimalFormat("#,##0.###").format(result)
        return Microresult(
            title = "$formattedRes ${unitLabel(toUnit)}",
            subtitle = "$formattedVal ${unitLabel(fromUnit)}",
            copyValue = formattedRes.replace(",", "")
        )
    }

    private fun normalizeUnit(raw: String): String? {
        return when (raw.lowercase()) {
            "km", "kilometer", "kilometers" -> "km"
            "mi", "mile", "miles" -> "mile"
            "m", "meter", "meters" -> "m"
            "cm", "centimeter", "centimeters" -> "cm"
            "ft", "foot", "feet" -> "ft"
            "in", "inch", "inches" -> "in"
            "kg", "kilogram", "kilograms" -> "kg"
            "lb", "lbs", "pound", "pounds" -> "lb"
            "g", "gram", "grams" -> "g"
            "oz", "ounce", "ounces" -> "oz"
            "c", "°c", "celsius" -> "c"
            "f", "°f", "fahrenheit" -> "f"
            "gb", "gigabyte", "gigabytes" -> "gb"
            "mb", "megabyte", "megabytes" -> "mb"
            else -> null
        }
    }

    private fun unitLabel(unit: String): String = when (unit) {
        "km" -> "kilometer"
        "mile" -> "mile"
        "m" -> "meter"
        "cm" -> "cm"
        "ft" -> "feet"
        "in" -> "inch"
        "kg" -> "kg"
        "lb" -> "lb"
        "g" -> "gram"
        "oz" -> "oz"
        "c" -> "°C"
        "f" -> "°F"
        "gb" -> "GB"
        "mb" -> "MB"
        else -> unit
    }

    private fun convertUnits(value: Double, from: String, to: String): Double? {
        if (from == to) return value

        // Length (base: meter)
        val lengthToMeters = mapOf("m" to 1.0, "km" to 1000.0, "cm" to 0.01, "mile" to 1609.344, "ft" to 0.3048, "in" to 0.0254)
        if (from in lengthToMeters && to in lengthToMeters) {
            return value * lengthToMeters[from]!! / lengthToMeters[to]!!
        }

        // Mass (base: gram)
        val massToGrams = mapOf("g" to 1.0, "kg" to 1000.0, "lb" to 453.59237, "oz" to 28.349523)
        if (from in massToGrams && to in massToGrams) {
            return value * massToGrams[from]!! / massToGrams[to]!!
        }

        // Data (base: MB)
        val dataToMb = mapOf("mb" to 1.0, "gb" to 1024.0)
        if (from in dataToMb && to in dataToMb) {
            return value * dataToMb[from]!! / dataToMb[to]!!
        }

        // Temperature
        if (from == "c" && to == "f") return value * 9.0 / 5.0 + 32.0
        if (from == "f" && to == "c") return (value - 32.0) * 5.0 / 9.0

        return null
    }
}
