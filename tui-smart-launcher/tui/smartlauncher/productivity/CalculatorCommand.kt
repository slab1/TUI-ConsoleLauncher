package tui.smartlauncher.productivity

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import tui.smartlauncher.core.CommandHandler
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

    // Context is stored when the command is registered
    private lateinit var appContext: Context
    private var lastResult: Double? = null
    private val variables: MutableMap<String, Double> by lazy { loadVariables() }
    private val gson = Gson()
    private val prefs: SharedPreferences by lazy {
        appContext.getSharedPreferences("tui_calc_prefs", Context.MODE_PRIVATE)
    }

    /**
     * Blocked names that cannot be used as variable names
     */
    private val reservedNames = setOf(
        "pi", "e", "ans", "last", "previous",
        "sqrt", "pow", "sin", "cos", "tan", "log", "ln", "fact", "abs", "round",
        "store", "let", "vars", "variables", "mode", "clear", "help",
        "to", "mod"
    )

    override fun onRegister(context: Context) {
        appContext = context
    }

    override fun getName(): String = "calc"

    override fun getAliases(): List<String> = listOf("calculator", "math", "=")

    override fun getDescription(): String = "Calculator and expression evaluation"

    override fun getUsage(): String = """
        ╔══════════════════════════════════════════════════════════╗
        ║                   CALCULATOR                              ║
        ╠══════════════════════════════════════════════════════════╣
        ║  calc <expression>          - Evaluate expression        ║
        ║  calc pi                    - Show Pi value              ║
        ║  calc e                     - Show Euler's number        ║
        ║  calc sqrt(16)              - Square root                ║
        ║  calc pow(2, 8)             - Power                      ║
        ║  calc sin(30)               - Trigonometric              ║
        ║  calc log(100)              - Logarithm                  ║
        ║  calc ln(e)                 - Natural log                ║
        ║  calc fact(5)               - Factorial                  ║
        ║  calc 100 USD to EUR        - Currency conversion        ║
        ║  calc 10 kg to lbs          - Unit conversion            ║
        ║  calc mode                  - Show calculation mode      ║
        ║  calc ans                   - Show previous result       ║
        ║  calc store x = 5           - Store a variable           ║
        ║  calc let x = 5             - Alternate store syntax     ║
        ║  calc x = 5                 - Inline store               ║
        ║  calc x + y                 - Use variables in express.  ║
        ║  calc vars                  - List stored variables      ║
        ╚══════════════════════════════════════════════════════════╝
    """.trimIndent()

    override fun execute(context: Context, args: List<String>): String {
        if (args.isEmpty()) {
            return showInteractiveHelp()
        }

        val input = args.joinToString(" ")

        return when {
            input.lowercase() in listOf("--help", "-h", "help") -> getUsage()
            input.lowercase() == "mode" -> showMode()
            input.lowercase() in listOf("ans", "last", "previous") -> showLastResult()
            input.lowercase().startsWith("store ") || input.lowercase().startsWith("let ") -> storeVariable(input)
            input.lowercase().startsWith("vars") || input.lowercase() == "variables" -> listVariables()
            input.matches(Regex("^[a-zA-Z]\\w*\\s*=\\s*.+")) -> storeVariable(input)
            input.lowercase() == "pi" -> {
                lastResult = Math.PI
                "π = ${df.format(Math.PI)}"
            }
            input.lowercase() == "e" -> {
                lastResult = Math.E
                "e = ${df.format(Math.E)}"
            }
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
            appendLine("Variables:")
            appendLine("  calc store x = 5       Store x = 5")
            appendLine("  calc let radius = 10   Store radius = 10")
            appendLine("  calc radius             Show radius value")
            appendLine("  calc radius * 2         Use variable in expression")
            appendLine("  calc vars               List all variables")
            appendLine()
            appendLine("Previous Result:")
            appendLine("  calc ans                Show last result")
            appendLine("  calc ans + 10            Use last result in expression")
            appendLine()
            appendLine("Type 'calc' followed by your expression")
        }
    }

    /**
     * Shows current calculation mode
     */
    private fun showMode(): String {
        return buildString {
            appendLine("Calculation Mode: Standard (radians for trig)")
            if (variables.isNotEmpty()) {
                appendLine("Stored variables: ${variables.size} (use 'calc vars' to list)")
            }
            if (lastResult != null) {
                appendLine("Last result: ${formatNumber(lastResult!!)} (use 'calc ans')")
            }
            appendLine("Store a variable: calc store <name> = <value>")
            appendLine("Recall last result: calc ans")
        }
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

            // Check if expression is a single variable name for nice display
            val trimmedExpr = expression.trim().lowercase()
            if (trimmedExpr in variables) {
                val value = variables[trimmedExpr]!!
                lastResult = value
                val formatted = formatNumber(value)
                return buildString {
                    appendLine()
                    appendLine("Variable: $trimmedExpr")
                    appendLine("─".repeat(50))
                    appendLine("  $trimmedExpr = $formatted")
                    appendLine()
                }
            }

            // Evaluate via the internal engine (handles ans, variable substitution, functions)
            val result = evaluateToDouble(expression)
            lastResult = result

            val formatted = formatNumber(result)
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
     * Internal evaluation engine: returns a Double.
     * Supports ans, variable substitution, constants, and functions.
     */
    private fun evaluateToDouble(expression: String): Double {
        // Handle "ans" keyword
        val trimmed = expression.trim().lowercase()
        if (trimmed in listOf("ans", "last", "previous")) {
            return lastResult ?: throw IllegalArgumentException(
                "No previous result. Perform a calculation first."
            )
        }

        // Substitute known variable names with their values
        var cleaned = expression
        for ((name, value) in variables) {
            cleaned = cleaned.replace(Regex("\\b$name\\b", RegexOption.IGNORE_CASE), value.toString())
        }

        // Clean the expression
        cleaned = cleaned
            .replace("×", "*")
            .replace("÷", "/")
            .replace("^", "^")
            .replace("mod", "%")
            .lowercase()
            .trim()

        // Substitute "ans" if present after variable substitution (in case "ans" wasn't a variable)
        cleaned = cleaned.replace(Regex("\\bans\\b"), lastResult?.toString() ?: "0")

        // Replace math constants
        cleaned = cleaned
            .replace(Regex("\\bpi\\b"), Math.PI.toString())
            .replace(Regex("\\be\\b"), Math.E.toString())

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
        return evaluateSimple(cleaned)
    }

    /**
     * Formats a Double value for display (removes decimal when .0)
     */
    private fun formatNumber(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            df.format(value)
        }
    }

    /**
     * Shows the last result if available
     */
    private fun showLastResult(): String {
        val result = lastResult
        return if (result != null) {
            val formatted = formatNumber(result)
            buildString {
                appendLine()
                appendLine("Previous Result")
                appendLine("─".repeat(50))
                appendLine("  $formatted")
            }
        } else {
            buildString {
                appendLine()
                appendLine("Previous Result")
                appendLine("─".repeat(50))
                appendLine("  No previous result available.")
                appendLine()
                appendLine("Perform a calculation first, e.g.: calc 2 + 2")
            }
        }
    }

    /**
     * Stores a variable from user input
     * Syntax: calc store <name> = <value> or calc let <name> = <value> or calc <name> = <value>
     */
    private fun storeVariable(input: String): String {
        var expr = input.trimStart()
        // Strip "store " or "let " prefix if present
        if (expr.lowercase().startsWith("store ")) {
            expr = expr.removePrefix("store").trimStart()
        } else if (expr.lowercase().startsWith("let ")) {
            expr = expr.removePrefix("let").trimStart()
        }

        val eqIndex = expr.indexOf('=')
        if (eqIndex == -1) {
            return "Invalid syntax. Use: calc store <name> = <value>"
        }

        val name = expr.substring(0, eqIndex).trim()
        val valueStr = expr.substring(eqIndex + 1).trim()

        // Validate name
        if (!name.matches(Regex("^[a-zA-Z]\\w*$"))) {
            return "Invalid variable name: '$name'. Names must start with a letter and contain only letters, digits, or underscores."
        }

        val nameLower = name.lowercase()
        if (nameLower in reservedNames) {
            return "Cannot overwrite built-in '$nameLower'."
        }

        return try {
            val value = evaluateToDouble(valueStr)
            variables[nameLower] = value
            saveVariables()
            "Variable '$name' = ${formatNumber(value)} stored."
        } catch (e: Exception) {
            "Error evaluating value: ${e.message}"
        }
    }

    /**
     * Lists all stored variables with their values
     */
    private fun listVariables(): String {
        if (variables.isEmpty()) {
            return buildString {
                appendLine()
                appendLine("Variables")
                appendLine("─".repeat(50))
                appendLine("  No variables stored.")
                appendLine()
                appendLine("Store a variable: calc store x = 100")
            }
        }

        return buildString {
            appendLine()
            appendLine("Variables (${variables.size})")
            appendLine("─".repeat(50))
            val sorted = variables.entries.sortedBy { it.key }
            sorted.forEach { (name, value) ->
                appendLine("  $name = ${formatNumber(value)}")
            }
        }
    }

    /**
     * Loads variables from SharedPreferences
     */
    private fun loadVariables(): MutableMap<String, Double> {
        if (!::appContext.isInitialized) return mutableMapOf()
        val json = prefs.getString("calc_variables", "{}") ?: "{}"
        return try {
            if (json == "{}" || json.isBlank()) {
                mutableMapOf()
            } else {
                val type = object : TypeToken<Map<String, Double>>() {}.type
                gson.fromJson(json, type)?.toMutableMap() ?: mutableMapOf()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading variables: ${e.message}")
            mutableMapOf()
        }
    }

    /**
     * Saves variables to SharedPreferences
     */
    private fun saveVariables() {
        if (!::appContext.isInitialized) return
        val json = gson.toJson(variables)
        prefs.edit().putString("calc_variables", json).apply()
    }

    /**
     * Handles unit conversions like "100 km to mi" or "32 c to f"
     */
    private fun handleConversion(expression: String): String {
        val parts = expression.split(Regex("\\s+to\\s+", RegexOption.IGNORE_CASE))
        if (parts.size != 2) {
            return "Invalid conversion format. Use: calc <value> <unit> to <unit>"
        }

        // Parse source side: "100 km" → value=100, fromUnit="km"
        val sourceParts = parts[0].trim().split(Regex("\\s+"))
        if (sourceParts.size < 2) {
            return "Invalid format. Use: calc <value> <unit> to <unit>\nExample: calc 100 km to mi"
        }
        val value = sourceParts[0].toDoubleOrNull() ?: return "Invalid number: ${sourceParts[0]}"
        val fromUnit = sourceParts.drop(1).joinToString(" ").lowercase().trim()

        // Parse target side: "mi" → toUnit="mi"
        val toUnit = parts[1].trim().lowercase()

        return when {
            // Temperature
            fromUnit in listOf("c", "celsius", "°c") && toUnit in listOf("f", "fahrenheit", "°f") -> {
                val f = value * 9.0 / 5.0 + 32
                "${df.format(value)}°C = ${df.format(f)}°F"
            }
            fromUnit in listOf("f", "fahrenheit", "°f") && toUnit in listOf("c", "celsius", "°c") -> {
                val c = (value - 32) * 5.0 / 9.0
                "${df.format(value)}°F = ${df.format(c)}°C"
            }
            fromUnit in listOf("c", "celsius", "°c") && toUnit in listOf("k", "kelvin", "°k") -> {
                val k = value + 273.15
                "${df.format(value)}°C = ${df.format(k)}K"
            }

            // Length
            fromUnit == "km" && toUnit in listOf("mi", "mile", "miles") -> {
                val mi = value * 0.621371
                "${df.format(value)} km = ${df.format(mi)} mi"
            }
            fromUnit in listOf("mi", "mile", "miles") && toUnit == "km" -> {
                val km = value * 1.60934
                "${df.format(value)} mi = ${df.format(km)} km"
            }
            fromUnit == "m" && toUnit in listOf("ft", "feet", "foot") -> {
                val ft = value * 3.28084
                "${df.format(value)} m = ${df.format(ft)} ft"
            }
            fromUnit in listOf("ft", "feet", "foot") && toUnit == "m" -> {
                val m = value / 3.28084
                "${df.format(value)} ft = ${df.format(m)} m"
            }
            fromUnit in listOf("in", "inch", "inches") && toUnit in listOf("cm", "centimeter", "centimeters") -> {
                val cm = value * 2.54
                "${df.format(value)} in = ${df.format(cm)} cm"
            }
            fromUnit in listOf("cm", "centimeter", "centimeters") && toUnit in listOf("in", "inch", "inches") -> {
                val inch = value / 2.54
                "${df.format(value)} cm = ${df.format(inch)} in"
            }

            // Weight
            fromUnit == "kg" && toUnit in listOf("lb", "lbs", "pound", "pounds") -> {
                val lbs = value * 2.20462
                "${df.format(value)} kg = ${df.format(lbs)} lbs"
            }
            fromUnit in listOf("lb", "lbs", "pound", "pounds") && toUnit == "kg" -> {
                val kg = value / 2.20462
                "${df.format(value)} lbs = ${df.format(kg)} kg"
            }
            fromUnit == "g" && toUnit in listOf("oz", "ounce", "ounces") -> {
                val oz = value * 0.035274
                "${df.format(value)} g = ${df.format(oz)} oz"
            }
            fromUnit in listOf("oz", "ounce", "ounces") && toUnit == "g" -> {
                val g = value / 0.035274
                "${df.format(value)} oz = ${df.format(g)} g"
            }

            // Volume
            fromUnit in listOf("l", "liter", "liters") && toUnit in listOf("gal", "gallon", "gallons") -> {
                val gal = value * 0.264172
                "${df.format(value)} L = ${df.format(gal)} gal"
            }
            fromUnit in listOf("gal", "gallon", "gallons") && toUnit in listOf("l", "liter", "liters") -> {
                val l = value / 0.264172
                "${df.format(value)} gal = ${df.format(l)} L"
            }

            else -> "Unsupported conversion: $fromUnit → $toUnit\nSupported: temp (C/F/K), length (km/mi/m/ft/in/cm), weight (kg/lbs/g/oz), volume (L/gal)"
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
     * Parses and evaluates a mathematical expression using recursive descent.
     * Handles proper operator precedence: + - (lowest), * /, ^ (highest)
     */
    private fun parseExpression(expr: String): Double {
        val trimmed = expr.trim()
        if (trimmed.isBlank()) return 0.0

        // Tokenize: numbers, operators, parentheses
        val tokens = tokenize(trimmed)
        if (tokens.isEmpty()) return 0.0

        val parser = ExpressionParser(tokens)
        val result = parser.parseExpression()
        if (parser.hasMore()) {
            throw IllegalArgumentException("Unexpected token after expression: ${parser.peek()}")
        }
        return result
    }

    /**
     * Tokenizes a mathematical expression string into tokens
     */
    private fun tokenize(expr: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0

        while (i < expr.length) {
            val ch = expr[i]
            when {
                ch.isWhitespace() -> {
                    if (current.isNotEmpty()) {
                        tokens.add(current.toString())
                        current.clear()
                    }
                }
                ch in '(' -> {
                    if (current.isNotEmpty()) {
                        tokens.add(current.toString())
                        current.clear()
                    }
                    tokens.add(ch.toString())
                }
                ch in ')' -> {
                    if (current.isNotEmpty()) {
                        tokens.add(current.toString())
                        current.clear()
                    }
                    tokens.add(ch.toString())
                }
                ch in "+-*/^" -> {
                    if (current.isNotEmpty()) {
                        tokens.add(current.toString())
                        current.clear()
                    }
                    tokens.add(ch.toString())
                }
                else -> current.append(ch)
            }
            i++
        }
        if (current.isNotEmpty()) {
            tokens.add(current.toString())
        }
        return tokens
    }

    /**
     * Recursive descent parser with proper operator precedence
     */
    private class ExpressionParser(private val tokens: List<String>) {
        private var pos = 0

        fun hasMore(): Boolean = pos < tokens.size

        fun peek(): String? = if (pos < tokens.size) tokens[pos] else null

        private fun consume(): String = tokens[pos].also { pos++ }

        /**
         * Entry point: expression = term (('+' | '-') term)*
         */
        fun parseExpression(): Double {
            var result = parseTerm()
            while (peek() == "+" || peek() == "-") {
                val op = consume()
                val right = parseTerm()
                result = if (op == "+") result + right else result - right
            }
            return result
        }

        /**
         * Term = factor (('*' | '/') factor)*
         */
        private fun parseTerm(): Double {
            var result = parsePower()
            while (peek() == "*" || peek() == "/") {
                val op = consume()
                val right = parsePower()
                result = if (op == "*") {
                    result * right
                } else {
                    if (right == 0.0) throw ArithmeticException("Division by zero")
                    result / right
                }
            }
            return result
        }

        /**
         * Power = unary ('^' power)*   (right-associative)
         */
        private fun parsePower(): Double {
            val base = parseUnary()
            return if (peek() == "^") {
                consume() // consume ^
                val exponent = parsePower() // right-associative
                Math.pow(base, exponent)
            } else {
                base
            }
        }

        /**
         * Unary = ('+' | '-')* atom
         */
        private fun parseUnary(): Double {
            var sign = 1.0
            while (peek() == "+" || peek() == "-") {
                val op = consume()
                if (op == "-") sign = -sign
            }
            return sign * parseAtom()
        }

        /**
         * Atom = number | '(' expression ')' | function
         */
        private fun parseAtom(): Double {
            val token = peek() ?: throw IllegalArgumentException("Unexpected end of expression")

            // Parenthesized sub-expression
            if (token == "(") {
                consume() // consume '('
                val result = parseExpression()
                if (peek() != ")") throw IllegalArgumentException("Missing closing parenthesis")
                consume() // consume ')'
                return result
            }

            // Number
            val num = token.toDoubleOrNull()
                ?: throw IllegalArgumentException("Invalid number: '$token'")
            consume()
            return num
        }
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
