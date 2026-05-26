package tui.smartlauncher.core

import android.content.Context

/**
 * Alias Command - Runtime alias management for T-UI Smart Launcher.
 *
 * Allows users to create, list, remove, import, export, and reset command aliases
 * at runtime through an intuitive command-line interface with boxed formatting.
 *
 * Usage:
 *   alias                          List all aliases
 *   alias <name>                   Show expansion for a specific alias
 *   alias <name>=<expansion>       Create/update alias (equals syntax)
 *   alias <name> <expansion>       Create/update alias (space-separated)
 *   alias -r|--remove|rm <name>    Remove a user-defined alias
 *   alias -e|--export              Export all aliases as JSON
 *   alias --reset                  Reset to default aliases
 *   alias -i|--import <json>       Import aliases from JSON
 *   alias -h|--help                Show usage
 */
class AliasCommand : CommandHandler {

    override fun getName(): String = "alias"

    override fun getAliases(): List<String> = listOf("aliases", "ali")

    override fun getDescription(): String = "Manage command aliases at runtime"

    override fun getUsage(): String {
        return buildString {
            appendLine("╔══════════════════════════════════════════════════════╗")
            appendLine("║                    ALIAS COMMAND                      ║")
            appendLine("╠══════════════════════════════════════════════════════╣")
            appendLine("║  alias                     List all aliases           ║")
            appendLine("║  alias <name>              Show alias expansion       ║")
            appendLine("║  alias <name>=<expansion>  Create/update alias        ║")
            appendLine("║  alias <name> <expansion>  Create/update alias        ║")
            appendLine("║  alias -r <name>           Remove an alias            ║")
            appendLine("║  alias --remove <name>     Remove an alias            ║")
            appendLine("║  alias rm <name>           Remove an alias            ║")
            appendLine("║  alias -e                  Export aliases as JSON     ║")
            appendLine("║  alias --export            Export aliases as JSON     ║")
            appendLine("║  alias --reset             Reset to default aliases   ║")
            appendLine("║  alias -i <json>           Import aliases from JSON   ║")
            appendLine("║  alias --import <json>     Import aliases from JSON   ║")
            appendLine("║  alias -h, --help          Show this help message     ║")
            appendLine("╚══════════════════════════════════════════════════════╝")
        }
    }

    override fun execute(context: Context, args: List<String>): String {
        val aliasManager = AliasManager(context)

        if (args.isEmpty()) {
            return listAliases(aliasManager)
        }

        return when (val first = args[0]) {
            "-h", "--help" -> getUsage()

            "-r", "--remove", "rm" -> handleRemove(aliasManager, args)
            "-e", "--export" -> aliasManager.exportAliases()
            "--reset" -> handleReset(aliasManager)
            "-i", "--import" -> handleImport(aliasManager, args)

            else -> handleDefault(aliasManager, args)
        }
    }

    // ------------------------------------------------------------------
    // Subcommand handlers
    // ------------------------------------------------------------------

    /**
     * Handles: alias -r|--remove|rm <name>
     */
    private fun handleRemove(aliasManager: AliasManager, args: List<String>): String {
        if (args.size < 2) {
            return "Usage: alias -r <name>"
        }
        val name = args[1]

        if (!aliasManager.hasAlias(name)) {
            return "Alias not found: $name"
        }
        if (!aliasManager.getUserAliases().containsKey(name)) {
            return "Cannot remove system alias: $name"
        }
        return if (aliasManager.removeAlias(name)) {
            "Alias removed: $name"
        } else {
            "Failed to remove alias: $name"
        }
    }

    /**
     * Handles: alias --reset
     */
    private fun handleReset(aliasManager: AliasManager): String {
        aliasManager.resetToDefaults()
        return "Aliases reset to defaults."
    }

    /**
     * Handles: alias -i|--import <json>
     */
    private fun handleImport(aliasManager: AliasManager, args: List<String>): String {
        if (args.size < 2) {
            return "Usage: alias -i <json>"
        }
        val json = args.drop(1).joinToString(" ")
        return when (val result = aliasManager.importAliases(json)) {
            is AliasManager.ImportResult.Success -> {
                "Imported ${result.count} alias(es) successfully."
            }
            is AliasManager.ImportResult.Conflicts -> {
                buildString {
                    appendLine("The following aliases already exist:")
                    for (name in result.names) {
                        appendLine("  - $name")
                    }
                    append("Use --overwrite to replace existing aliases during import.")
                }
            }
            is AliasManager.ImportResult.Error -> {
                "Import failed: ${result.message}"
            }
        }
    }

    /**
     * Handles all default (non-flag) forms:
     *   - alias <name>              lookup
     *   - alias <name>=<expansion>  create/update (equals syntax)
     *   - alias <name> <expansion>  create/update (space-separated)
     */
    private fun handleDefault(aliasManager: AliasManager, args: List<String>): String {
        val first = args[0]

        // Equals syntax: alias name=expansion [more args]
        if (first.contains("=")) {
            return handleEqualsSyntax(aliasManager, first, args)
        }

        // Single argument: alias lookup
        if (args.size == 1) {
            return handleLookup(aliasManager, first)
        }

        // Two or more arguments: create/update with space separation
        return handleCreate(aliasManager, first, args.drop(1))
    }

    /**
     * Handles: alias name=expansion [extra args]
     */
    private fun handleEqualsSyntax(
        aliasManager: AliasManager,
        first: String,
        args: List<String>
    ): String {
        val eqIndex = first.indexOf('=')
        if (eqIndex == 0) {
            return "Error: Alias name cannot be empty"
        }

        val name = first.substring(0, eqIndex).trim()
        val expansion = first.substring(eqIndex + 1).trim()

        if (name.isBlank()) {
            return "Error: Alias name cannot be empty"
        }

        // Append any remaining arguments to the expansion
        val fullExpansion = if (args.size > 1) {
            "$expansion ${args.drop(1).joinToString(" ")}"
        } else {
            expansion
        }.trim()

        if (fullExpansion.isEmpty()) {
            return "Error: Alias expansion cannot be empty"
        }

        aliasManager.addAlias(name, fullExpansion)
        return "Alias set: $name -> $fullExpansion"
    }

    /**
     * Handles: alias <name> — shows the expansion for a single alias
     */
    private fun handleLookup(aliasManager: AliasManager, name: String): String {
        val aliases = aliasManager.getAliases()
        val expansion = aliases[name]
        if (expansion != null) {
            val marker = if (aliasManager.getUserAliases().containsKey(name)) "*" else " "
            return "$marker $name -> $expansion"
        }
        return "Alias not found: $name"
    }

    /**
     * Handles: alias <name> <expansion> — creates or updates an alias
     */
    private fun handleCreate(
        aliasManager: AliasManager,
        name: String,
        expansionArgs: List<String>
    ): String {
        if (name.isBlank()) {
            return "Error: Alias name cannot be empty"
        }
        val expansion = expansionArgs.joinToString(" ")
        if (expansion.isBlank()) {
            return "Error: Alias expansion cannot be empty"
        }
        aliasManager.addAlias(name, expansion)
        return "Alias set: $name -> $expansion"
    }

    // ------------------------------------------------------------------
    // Display formatting
    // ------------------------------------------------------------------

    /**
     * Lists all aliases in a boxed table with the ╔═╗ signature style.
     *
     * User-defined aliases are prefixed with '*', system defaults with ' '.
     */
    private fun listAliases(aliasManager: AliasManager): String {
        val allAliases = aliasManager.getAliases()
        val userAliases = aliasManager.getUserAliases()

        if (allAliases.isEmpty()) {
            return "No aliases defined."
        }

        // Build display lines with user-alias markers
        val lines = allAliases.map { (name, expansion) ->
            val prefix = if (userAliases.containsKey(name)) "* " else "  "
            "$prefix$name -> $expansion"
        }

        val maxEntryWidth = lines.maxOf { it.length }
        val innerWidth = maxOf(maxEntryWidth + 4, 40)
        val contentWidth = innerWidth - 4

        val title = "ALIASES (${allAliases.size})"
        val titleCentered = title.center(contentWidth)

        return buildString {
            appendLine("╔" + "═".repeat(innerWidth) + "╗")
            appendLine("║  $titleCentered  ║")
            appendLine("╠" + "═".repeat(innerWidth) + "╣")
            for (line in lines) {
                appendLine("║  ${line.padEnd(contentWidth)}  ║")
            }
            appendLine("╚" + "═".repeat(innerWidth) + "╝")
        }
    }

    /**
     * Centers a string within a given character width by padding with spaces.
     */
    private fun String.center(width: Int): String {
        val padding = width - this.length
        if (padding <= 0) return this
        val leftPad = padding / 2
        val rightPad = padding - leftPad
        return " ".repeat(leftPad) + this + " ".repeat(rightPad)
    }
}
