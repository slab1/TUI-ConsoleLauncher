package tui.smartlauncher.core

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import com.google.gson.Gson
import tui.smartlauncher.ThemeColors

/**
 * Theme Command - Customize terminal colors and theme presets.
 *
 * Allows users to view, apply, and customize the terminal color scheme.
 * Supports named presets, individual color overrides, and persistent storage
 * via SharedPreferences.
 */
class ThemeCommand : CommandHandler {

    override fun getName(): String = "theme"

    override fun getAliases(): List<String> = listOf("colors", "palette")

    override fun getDescription(): String = "Customize terminal colors and theme"

    override fun getUsage(): String = """
        ╔══════════════════════════════════════════════════════════╗
        ║                    THEME COMMAND                        ║
        ╠══════════════════════════════════════════════════════════╣
        ║  theme                  Show current theme              ║
        ║  theme <name>           Apply a named preset            ║
        ║  theme presets          List all available presets      ║
        ║  theme input <color>    Set input color (hex)           ║
        ║  theme output <color>   Set output color (hex)          ║
        ║  theme error <color>    Set error color (hex)           ║
        ║  theme info <color>     Set info color (hex)            ║
        ║  theme bg <color>       Set background color (hex)      ║
        ║  theme reset            Reset to default theme          ║
        ║  theme --help           Show this help                  ║
        ╚══════════════════════════════════════════════════════════╝
    """.trimIndent()

    /**
     * Named color presets for quick theme switching.
     */
    private val presets = mapOf(
        "default" to ThemeColors(
            inputColor = Color.parseColor("#00FF00"),
            outputColor = Color.WHITE,
            errorColor = Color.parseColor("#FF5252"),
            infoColor = Color.parseColor("#2196F3"),
            backgroundColor = Color.BLACK
        ),
        "matrix" to ThemeColors(
            inputColor = Color.parseColor("#00FF41"),
            outputColor = Color.parseColor("#00FF41"),
            errorColor = Color.parseColor("#FF0000"),
            infoColor = Color.parseColor("#0088FF"),
            backgroundColor = Color.parseColor("#000000")
        ),
        "light" to ThemeColors(
            inputColor = Color.parseColor("#006400"),
            outputColor = Color.parseColor("#333333"),
            errorColor = Color.parseColor("#CC0000"),
            infoColor = Color.parseColor("#0066CC"),
            backgroundColor = Color.parseColor("#FFFFFF")
        ),
        "ocean" to ThemeColors(
            inputColor = Color.parseColor("#00BCD4"),
            outputColor = Color.parseColor("#E0F7FA"),
            errorColor = Color.parseColor("#FF5252"),
            infoColor = Color.parseColor("#80DEEA"),
            backgroundColor = Color.parseColor("#0D47A1")
        ),
        "monokai" to ThemeColors(
            inputColor = Color.parseColor("#A6E22E"),
            outputColor = Color.parseColor("#F8F8F2"),
            errorColor = Color.parseColor("#F92672"),
            infoColor = Color.parseColor("#66D9EF"),
            backgroundColor = Color.parseColor("#272822")
        ),
        "solarized" to ThemeColors(
            inputColor = Color.parseColor("#859900"),
            outputColor = Color.parseColor("#93A1A1"),
            errorColor = Color.parseColor("#DC322F"),
            infoColor = Color.parseColor("#268BD2"),
            backgroundColor = Color.parseColor("#002B36")
        ),
        "ubuntu" to ThemeColors(
            inputColor = Color.parseColor("#4E9A06"),
            outputColor = Color.parseColor("#EEEEEE"),
            errorColor = Color.parseColor("#CC0000"),
            infoColor = Color.parseColor("#3465A4"),
            backgroundColor = Color.parseColor("#300A24")
        )
    )

    override fun execute(context: Context, args: List<String>): String {
        if (args.isEmpty()) {
            return displayCurrentTheme()
        }

        val subcommand = args[0].lowercase()

        return when (subcommand) {
            "--help", "-h" -> getUsage()
            "presets" -> listPresets()
            "reset" -> applyPreset(context, "default")
            "input", "output", "error", "info", "bg" -> {
                if (args.size < 2) {
                    "Usage: theme $subcommand <color>\nProvide a hex color like #FF0000"
                } else {
                    setColorComponent(context, subcommand, args[1])
                }
            }
            else -> {
                // Try to apply as a preset name
                if (presets.containsKey(subcommand)) {
                    applyPreset(context, subcommand)
                } else {
                    buildString {
                        appendLine("Unknown theme or subcommand: '$subcommand'")
                        appendLine()
                        append("Run 'theme --help' for usage or 'theme presets' to list available presets.")
                    }
                }
            }
        }
    }

    /**
     * Displays the currently active theme colors in a formatted table.
     */
    private fun displayCurrentTheme(): String {
        val t = currentTheme
        return buildString {
            appendLine()
            appendLine("╔═══════════════════════════════════════════════════════════╗")
            appendLine("║               CURRENT THEME COLORS                       ║")
            appendLine("╠═══════════════════════════════════════════════════════════╣")
            appendLine("║  Input:       ${colorPreview(t.inputColor)}  ${formatHex(t.inputColor)}")
            appendLine("║  Output:      ${colorPreview(t.outputColor)}  ${formatHex(t.outputColor)}")
            appendLine("║  Error:       ${colorPreview(t.errorColor)}  ${formatHex(t.errorColor)}")
            appendLine("║  Info:        ${colorPreview(t.infoColor)}  ${formatHex(t.infoColor)}")
            appendLine("║  Background:  ${colorPreview(t.backgroundColor)}  ${formatHex(t.backgroundColor)}")
            appendLine("╚═══════════════════════════════════════════════════════════╝")
            appendLine()
            appendLine("Use 'theme presets' to see available themes.")
            appendLine("Use 'theme <name>' to apply a preset.")
        }
    }

    /**
     * Lists all available preset themes with their descriptions.
     */
    private fun listPresets(): String {
        return buildString {
            appendLine()
            appendLine("╔═══════════════════════════════════════════════════════════╗")
            appendLine("║               AVAILABLE THEME PRESETS                    ║")
            appendLine("╠═══════════════════════════════════════════════════════════╣")
            for ((name, colors) in presets) {
                appendLine("║  ${name.padEnd(12)} ${colorDot(colors.inputColor)}${colorDot(colors.outputColor)}${colorDot(colors.errorColor)}${colorDot(colors.infoColor)}${colorDot(colors.backgroundColor)}  ${name}")
            }
            appendLine("╚═══════════════════════════════════════════════════════════╝")
            appendLine()
            appendLine("Apply a preset: theme <name>")
            appendLine("Example: theme matrix")
        }
    }

    /**
     * Applies a named preset and persists it.
     */
    private fun applyPreset(context: Context, name: String): String {
        val preset = presets[name] ?: return "Unknown preset: '$name'"
        currentTheme = preset
        saveTheme(context, preset)
        return buildString {
            appendLine("Theme '$name' applied successfully!")
            appendLine()
            appendLine("  ${colorDot(preset.inputColor)}  Input:       ${formatHex(preset.inputColor)}")
            appendLine("  ${colorDot(preset.outputColor)}  Output:      ${formatHex(preset.outputColor)}")
            appendLine("  ${colorDot(preset.errorColor)}  Error:       ${formatHex(preset.errorColor)}")
            appendLine("  ${colorDot(preset.infoColor)}  Info:        ${formatHex(preset.infoColor)}")
            appendLine("  ${colorDot(preset.backgroundColor)}  Background:  ${formatHex(preset.backgroundColor)}")
        }
    }

    /**
     * Sets an individual color component and persists the updated theme.
     */
    private fun setColorComponent(context: Context, component: String, colorHex: String): String {
        // Validate hex color format
        val cleanHex = if (colorHex.startsWith("#")) colorHex else "#$colorHex"
        val parsedColor: Int = try {
            Color.parseColor(cleanHex)
        } catch (e: IllegalArgumentException) {
            return "Invalid color: '$colorHex'. Use a hex format like #FF0000 or FF0000."
        }

        val updated = when (component) {
            "input" -> currentTheme.copy(inputColor = parsedColor)
            "output" -> currentTheme.copy(outputColor = parsedColor)
            "error" -> currentTheme.copy(errorColor = parsedColor)
            "info" -> currentTheme.copy(infoColor = parsedColor)
            "bg" -> currentTheme.copy(backgroundColor = parsedColor)
            else -> return "Unknown color component: '$component'"
        }

        currentTheme = updated
        saveTheme(context, updated)

        return "Theme $component color set to ${formatHex(parsedColor)}"
    }

    /**
     * Returns a colored block character for visual preview.
     */
    private fun colorPreview(color: Int): String {
        val hex = formatHex(color)
        // Use a colored block character representation in the terminal
        return "■"
    }

    /**
     * Returns a filled circle for the preset color swatch row.
     */
    private fun colorDot(color: Int): String {
        return "●"
    }

    /**
     * Formats an ARGB int color to a hex string like #AARRGGBB or #RRGGBB.
     */
    private fun formatHex(color: Int): String {
        return String.format("#%06X", 0xFFFFFF and color)
    }

    companion object {
        /**
         * The currently active theme, readable by MainActivity or other components.
         */
        var currentTheme: ThemeColors = ThemeColors()

        private const val PREFS_NAME = "tui_theme_prefs"
        private const val KEY_THEME = "theme_colors"

        /**
         * Loads the persisted theme from SharedPreferences.
         * Returns the default ThemeColors if none is saved.
         */
        fun loadTheme(context: Context): ThemeColors {
            val prefs: SharedPreferences =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_THEME, null) ?: return ThemeColors()
            return try {
                val gson = Gson()
                val theme = gson.fromJson(json, ThemeColors::class.java)
                theme ?: ThemeColors()
            } catch (e: Exception) {
                ThemeColors()
            }
        }

        /**
         * Persists the given theme to SharedPreferences.
         */
        fun saveTheme(context: Context, theme: ThemeColors) {
            val prefs: SharedPreferences =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val gson = Gson()
            val json = gson.toJson(theme)
            prefs.edit().putString(KEY_THEME, json).apply()
        }
    }
}
