package tui.smartlauncher.core

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.io.File

/**
 * Alias Manager - Handles user-defined command aliases
 * Supports parameter expansion and nested aliases
 */
class AliasManager(private val context: Context) {

    companion object {
        private const val TAG = "AliasManager"
        private const val PREFS_NAME = "tui_alias_prefs"
        private const val KEY_ALIASES = "user_aliases"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    // Default system aliases
    private val defaultAliases = mapOf(
        "ll" to "ls -la",
        "la" to "ls -a",
        "l" to "ls -CF",
        ".." to "cd ..",
        "..." to "cd ../..",
        "home" to "cd ~",
        "clear" to "cls",
        "q" to "exit",
        "exit" to "quit",
        "g" to "git",
        "gc" to "git commit",
        "gp" to "git push",
        "gl" to "git pull",
        "gs" to "git status",
        "py" to "python",
        "node" to "nodejs",
        "npml" to "npm list",
        "c" to "calc",
        "sv" to "serve",
        "ipinfo" to "localip",
        "pingg" to "ping google.com",
        "mem" to "system --memory",
        "cpu" to "system --cpu"
    )

    /**
     * Gets all aliases (user + system)
     */
    fun getAliases(): Map<String, String> {
        val userAliases = getUserAliases()
        return defaultAliases + userAliases
    }

    /**
     * Gets only user-defined aliases
     */
    fun getUserAliases(): Map<String, String> {
        val json = prefs.getString(KEY_ALIASES, "{}") ?: "{}"
        return parseAliasesJson(json)
    }

    /**
     * Expands aliases in input string
     */
    fun expand(input: String): String {
        val parts = input.split(" ")
        if (parts.isEmpty()) return input

        val firstWord = parts[0]
        val aliases = getAliases()

        // Check for direct alias match
        val expansion = aliases[firstWord]
        if (expansion != null) {
            val remainingArgs = parts.drop(1)
            return if (remainingArgs.isEmpty()) {
                expansion
            } else {
                // Handle parameter substitution
                expandWithParams(expansion, remainingArgs)
            }
        }

        // Check for partial matches and suggest
        val partialMatch = aliases.entries.find { it.key.startsWith(firstWord) }
        if (partialMatch != null && partialMatch.key != firstWord) {
            Log.d(TAG, "Did you mean '${partialMatch.key}'?")
        }

        return input
    }

    /**
     * Expands alias with parameter substitution
     * $1, $2, etc. are replaced with positional arguments
     * $* is replaced with all arguments
     */
    private fun expandWithParams(alias: String, args: List<String>): String {
        var result = alias

        // Replace positional parameters
        args.forEachIndexed { index, arg ->
            result = result.replace("\$${index + 1}", arg)
        }

        // Replace all arguments with quoted versions
        val allArgs = args.joinToString(" ") { "\"$it\"" }
        result = result.replace("$*", allArgs)

        return result
    }

    /**
     * Adds or updates a user alias
     */
    fun addAlias(name: String, expansion: String) {
        val aliases = getUserAliases().toMutableMap()
        aliases[name] = expansion
        saveUserAliases(aliases)
        Log.d(TAG, "Alias added: $name -> $expansion")
    }

    /**
     * Removes a user alias
     */
    fun removeAlias(name: String): Boolean {
        val aliases = getUserAliases().toMutableMap()
        val removed = aliases.remove(name)
        if (removed != null) {
            saveUserAliases(aliases)
            Log.d(TAG, "Alias removed: $name")
        }
        return removed != null
    }

    /**
     * Checks if an alias exists
     */
    fun hasAlias(name: String): Boolean {
        return getAliases().containsKey(name)
    }

    /**
     * Exports all aliases as JSON
     */
    fun exportAliases(): String {
        val allAliases = getAliases()
        return buildJsonExport(allAliases)
    }

    /**
     * Imports aliases from JSON
     */
    fun importAliases(json: String, overwrite: Boolean = false): ImportResult {
        return try {
            val imported = parseAliasesJson(json)
            val current = getUserAliases().toMutableMap()

            val conflicts = imported.keys.filter { current.containsKey(it) }
            if (conflicts.isNotEmpty() && !overwrite) {
                return ImportResult.Conflicts(conflicts)
            }

            if (overwrite) {
                current.putAll(imported)
            } else {
                imported.forEach { (key, value) ->
                    if (!current.containsKey(key)) {
                        current[key] = value
                    }
                }
            }

            saveUserAliases(current)
            ImportResult.Success(imported.size)
        } catch (e: Exception) {
            ImportResult.Error("Invalid JSON format")
        }
    }

    /**
     * Resets to default aliases only
     */
    fun resetToDefaults() {
        prefs.edit().remove(KEY_ALIASES).apply()
        Log.d(TAG, "Aliases reset to defaults")
    }

    private fun saveUserAliases(aliases: Map<String, String>) {
        val json = aliases.entries.joinToString(",", "{", "}") { (k, v) ->
            "\"$k\":\"$v\""
        }
        prefs.edit().putString(KEY_ALIASES, json).apply()
    }

    private fun parseAliasesJson(json: String): Map<String, String> {
        if (json == "{}" || json.isBlank()) return emptyMap()

        return try {
            val cleanJson = json.trim().removeSurrounding("{", "}")
            if (cleanJson.isBlank()) return emptyMap()

            cleanJson.split(",").associate { pair ->
                val parts = pair.split(":")
                if (parts.size == 2) {
                    val key = parts[0].trim().removeSurrounding("\"")
                    val value = parts[1].trim().removeSurrounding("\"")
                    key to value
                } else {
                    "" to ""
                }
            }.filter { it.first.isNotEmpty() }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing aliases JSON: ${e.message}")
            emptyMap()
        }
    }

    private fun buildJsonExport(aliases: Map<String, String>): String {
        return aliases.entries.joinToString(",", "{", "}") { (k, v) ->
            "\"$k\":\"$v\""
        }
    }

    sealed class ImportResult {
        data class Success(val count: Int) : ImportResult()
        data class Conflicts(val names: List<String>) : ImportResult()
        data class Error(val message: String) : ImportResult()
    }
}
