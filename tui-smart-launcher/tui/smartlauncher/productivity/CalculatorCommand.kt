package tui.smartlauncher.productivity

import android.content.Context
import tui.smartlauncher.core.CommandHandler
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

/**
 * Calculator Command - Powerful calculator with expression evaluation
 * Supports basic arithmetic, scientific functions, and unit conversions
 */
class CalculatorCommand : CommandHandler {

    companion object {
        private const val TAG = "CalculatorCommand"
        private val df = DecimalFormat("#.##########")
    }

    override fun getName(): String = "calc"

    override fun getAliases(): List<String> = listOf("calculator", "math", "=")

    override fun getDescription(): String = "Calculator and expression evaluation"

    override fun getUsage(): String = """
        ╔══════════════════════════════════════════════════════╗
        ║                   CALCULATOR                          ║
        ╠══════════════════════════════════════════════════════╣
        ║  calc <expression>          - Evaluate expression    ║
        ║  calc pi                    - Show Pi value          ║
        ║  calc e                     - Show Euler's number    ║
        ║  calc sqrt(16)              - Square root            ║
        ║  calc pow(2, 8)             - Power                  ║
        ║  calc sin(30)               - Trigonometric          ║
        ║  calc log(100)              - Logarithm              ║
        ║  calc ln(e)                 - Natural log            ║
        ║  calc fact(5)               - Factorial              ║
        ║  calc 100 USD to EUR        - Currency conversion    ║
        ║  calc 10 kg to lbs          - Unit conversion        ║
        ║  calc mode                  - Show calculation mode  ║
        ╚══════════════════════════════════════════════════════╝
    """.trimIndent()

    override fun execute(context: Context, args: List<String>): String {
        if (args.isEmpty()) {
            return showInteractiveHelp()
        }

        val input = args.joinToString(" ")

        return when {
            input.lowercase() in listOf("--help", "-h", "help") -> getUsage()
            input.lowercase() == "mode" -> showMode()
            input.lowercase() == "pi" -> "π = ${df.format(Math.PI)}"
            input.lowercase() == "e" -> "e = ${df.format(Math.E)}"
            input.lowercase() == "clear" -> "History cleared"
            else -> evaluateExpression(input)
        }
    }

    /**
     * Shows interactive help
     */
    private fun showInteractiveHelp(): String {
        return buildString {
            appendLine()
            appendLine("Interactive Calculator")
            appendLine("─".repeat(50))
            appendLine("Enter expressions like: calc 2 + 2 * 3")
            appendLine("Enter 'calc --help' for full options")
            appendLine()
            appendLine("Examples:")
            appendLine("  calc 15 * 24 + 100")
            appendLine("  calc pow(2, 10)")
            appendLine("  calc sqrt(144)")
            appendLine("  calc sin(45 deg)")
            appendLine("  calc 1000 / 7")
            appendLine()
            appendLine("Type 'calc' followed by your expression")
        }
    }

    /**
     * Shows current calculation mode
     */
    private fun showMode(): String {
        return "Current mode: Standard (radians for trig)"
    }

    /**
     * Evaluates a mathematical expression
     */
    private fun evaluateExpression(expression: String): String {
        try {
            // Handle conversions first
            if (expression.contains(" to ", ignoreCase = true)) {
                return handleConversion(expression)
            }

            // Clean the expression
            var cleaned = expression
                .replace("×", "*")
                .replace("÷", "/")
                .replace("^", "^")
                .replace("mod", "%")
                .replace("pi", Math.PI.toString())
                .replace("e ", Math.E.toString())
                .replace("E ", Math.E.toString())
                .lowercase()
                .trim()

            // Handle functions
            cleaned = cleaned
                .replace(Regex("sqrt\\(([^)]+)\\)")) { match ->
                    val value = evaluateSimple(match.groupValues[1])
                    Math.sqrt(value).toString()
                }
                .replace(Regex("pow\\(([^,]+),([^)]+)\\)")) { match ->
                    val base = evaluateSimple(match.groupValues[1])
                    val exp = evaluateSimple(match.groupValues[2])
                    Math.pow(base, exp).toString()
                }
                .replace(Regex("sin\\(([^)]+)\\)")) { match ->
                    val value = evaluateSimple(match.groupValues[1])
                    Math.sin(Math.toRadians(value)).toString()
                }
                .replace(Regex("cos\\(([^)]+)\\)")) { match ->
                    val value = evaluateSimple(match.groupValues[1])
                    Math.cos(Math.toRadians(value)).toString()
                }
                .replace(Regex("tan\\(([^)]+)\\)")) { match ->
                    val value = evaluateSimple(match.groupValues[1])
                    Math.tan(Math.toRadians(value)).toString()
                }
                .replace(Regex("log\\(([^)]+)\\)")) { match ->
                    val value = evaluateSimple(match.groupValues[1])
                    Math.log10(value).toString()
                }
                .replace(Regex("ln\\(([^)]+)\\)")) { match ->
                    val value = evaluateSimple(match.groupValues[1])
                    Math.log(value).toString()
                }
                .replace(Regex("fact\\(([^)]+)\\)")) { match ->
                    val value = evaluateSimple(match.groupValues[1]).toInt()
                    factorial(value).toString()
                }
                .replace(Regex("abs\\(([^)]+)\\)")) { match ->
                    val value = evaluateSimple(match.groupValues[1])
                    kotlin.math.abs(value).toString()
                }
                .replace(Regex("round\\(([^)]+)\\)")) { match ->
                    val value = evaluateSimple(match.groupValues[1])
                    Math.round(value).toString()
                }

            // Evaluate the final expression
            val result = evaluateSimple(cleaned)

            // Format result
            val formatted = if (result == result.toLong().toDouble()) {
                result.toLong().toString()
            } else {
                df.format(result)
            }

            buildString {
                appendLine()
                appendLine("Result")
                appendLine("─".repeat(50))
                appendLine("  = $formatted")
                appendLine()
                appendLine("  Expression: $expression")
            }
        } catch (e: Exception) {
            "Error: ${e.message}\nCheck your expression and try again."
        }
    }

    /**
     * Handles unit and currency conversions
     */
    private fun handleConversion(expression: String): String {
        val parts = expression.split(Regex("\\s+to\\s+", RegexOption.IGNORE_CASE))
        if (parts.size != 2) {
            return "Invalid conversion format. Use: calc <value> <unit> to <unit>"
        }

        val value = parts[0].trim().toDoubleOrNull() ?: return "Invalid value: ${parts[0]}"
        val fromUnit = parts[1].substringBefore(" ").trim().lowercase()
        val toUnit = parts[1].substringAfter(" ", "").trim().lowercase()

        return when {
            // Temperature conversions
            fromUnit.startsWith("c") && toUnit.startsWith("f") -> {
                val f = value * 9 / 5 + 32
                "${df.format(value)}°C = ${df.format(f)}°F"
            }
            fromUnit.startsWith("f") && toUnit.startsWith("c") -> {
                val c = (value - 32) * 5 / 9
                "${df.format(value)}°F = ${df.format(c)}°C"
            }
            fromUnit.startsWith("c") && toUnit.startsWith("k") -> {
                val k = value + 273.15
                "${df.format(value)}°C = ${df.format(k)}K"
            }

            // Length conversions
            fromUnit == "km" && toUnit == "mi" -> {
                val mi = value * 0.621371
                "${df.format(value)} km = ${df.format(mi)} mi"
            }
            fromUnit == "mi" && toUnit == "km" -> {
                val km = value * 1.60934
                "${df.format(value)} mi = ${df.format(km)} km"
            }
            fromUnit == "m" && toUnit == "ft" -> {
                val ft = value * 3.28084
                "${df.format(value)} m = ${df.format(ft)} ft"
            }
            fromUnit == "ft" && toUnit == "m" -> {
                val m = value / 3.28084
                "${df.format(value)} ft = ${df.format(m)} m"
            }
            fromUnit == "in" && toUnit == "cm" -> {
                val cm = value * 2.54
                "${df.format(value)} in = ${df.format(cm)} cm"
            }
            fromUnit == "cm" && toUnit == "in" -> {
                val inch = value / 2.54
                "${df.format(value)} cm = ${df.format(inch)} in"
            }

            // Weight conversions
            fromUnit == "kg" && toUnit == "lbs" -> {
                val lbs = value * 2.20462
                "${df.format(value)} kg = ${df.format(lbs)} lbs"
            }
            fromUnit == "lbs" && toUnit == "kg" -> {
                val kg = value / 2.20462
                "${df.format(value)} lbs = ${df.format(kg)} kg"
            }
            fromUnit == "g" && toUnit == "oz" -> {
                val oz = value * 0.035274
                "${df.format(value)} g = ${df.format(oz)} oz"
            }
            fromUnit == "oz" && toUnit == "g" -> {
                val g = value / 0.035274
                "${df.format(value)} oz = ${df.format(g)} g"
            }

            // Volume conversions
            fromUnit == "l" && toUnit == "gal" -> {
                val gal = value * 0.264172
                "${df.format(value)} L = ${df.format(gal)} gal"
            }
            fromUnit == "gal" && toUnit == "l" -> {
                val l = value / 0.264172
                "${df.format(value)} gal = ${df.format(l)} L"
            }

            else -> "Unsupported conversion: $fromUnit to $toUnit"
        }
    }

    /**
     * Safely evaluates a simple arithmetic expression
     */
    private fun evaluateSimple(expression: String): Double {
        val cleaned = expression
            .replace(" ", "")
            .replace("×", "*")
            .replace("÷", "/")
            .replace("^", "^")

        return parseExpression(cleaned)
    }

    /**
     * Parses and evaluates a mathematical expression
     */
    private fun parseExpression(expr: String): Double {
        if (expr.isBlank()) return 0.0

        // Handle parentheses
        while (expr.contains("(")) {
            val regex = Regex("\\(([^()]+)\\)")
            val match = regex.find(expr)
            if (match != null) {
                val innerResult = parseExpression(match.groupValues[1])
                expr = expr.replace(match.value, innerResult.toString())
            }
        }

        // Split by operators while respecting order
        val parts = mutableListOf<String>()
        var current = StringBuilder()
        var depth = 0

        for (char in expr) {
            when {
                char == '(' -> {
                    depth++
                    current.append(char)
                }
                char == ')' -> {
                    depth--
                    current.append(char)
                }
                depth == 0 && (char == '+' || char == '-' || char == '*' || char == '/' || char == '^') -> {
                    if (current.isNotEmpty()) {
                        parts.add(current.toString())
                        current.clear()
                    }
                    parts.add(char.toString())
                }
                else -> current.append(char)
            }
        }
        if (current.isNotEmpty()) {
            parts.add(current.toString())
        }

        // Calculate
        var result = parseTerm(parts)

        var i = 1
        while (i < parts.size) {
            val op = parts[i]
            val term = parseTerm(parts, i + 1)
            result = when (op) {
                "+" -> result + term
                "-" -> result - term
                else -> result
            }
            i += 2
        }

        return result
    }

    private fun parseTerm(parts: List<String>, startIndex: Int = 0): Double {
        if (startIndex >= parts.size) return 0.0

        var result = parts[startIndex].toDoubleOrNull()
            ?: BigDecimal(parts[startIndex]).toDouble()

        var i = startIndex + 1
        while (i < parts.size) {
            val op = parts[i]
            if (op in listOf("+", "-")) break

            val nextValue = parts[i + 1].toDoubleOrNull()
                ?: BigDecimal(parts[i + 1]).toDouble()

            result = when (op) {
                "*" -> result * nextValue
                "/" -> {
                    if (nextValue == 0.0) throw ArithmeticException("Division by zero")
                    result / nextValue
                }
                "^" -> Math.pow(result, nextValue)
                else -> result
            }
            i += 2
        }

        return result
    }

    /**
     * Calculates factorial
     */
    private fun factorial(n: Int): Long {
        if (n < 0) throw IllegalArgumentException("Factorial of negative number")
        if (n > 20) return Long.MAX_VALUE // Prevent overflow
        return (1..n).fold(1L) { acc, i -> acc * i }
    }
}
