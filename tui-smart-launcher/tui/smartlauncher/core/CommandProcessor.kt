package tui.smartlauncher.core

import android.content.Context

/**
 * Command Processor - The central nervous system of T-UI Smart Launcher
 * Handles command parsing, execution routing, and output management
 */
class CommandProcessor(private val context: Context) {

    companion object {
        private const val TAG = "CommandProcessor"
    }

    private val commandRegistry = mutableMapOf<String, CommandHandler>()
    private val aliasManager = AliasManager(context)
    private val historyManager = CommandHistory(context)

    /**
     * Registers a new command handler
     */
    fun registerCommand(name: String, handler: CommandHandler) {
        commandRegistry[name.lowercase()] = handler
        handler.onRegister(context)
    }

    /**
     * Unregisters a command
     */
    fun unregisterCommand(name: String) {
        commandRegistry[name.lowercase()]?.onUnregister(context)
        commandRegistry.remove(name.lowercase())
    }

    /**
     * Processes user input and returns the result
     */
    fun processInput(input: String): ProcessingResult {
        if (input.isBlank()) {
            return ProcessingResult.Empty
        }

        // Save to history
        historyManager.addToHistory(input)

        // Expand aliases
        val expandedInput = aliasManager.expand(input)

        // Parse command and arguments
        val parts = parseInput(expandedInput)
        val commandName = parts[0].lowercase()
        val arguments = parts.drop(1).toMutableList()

        // Check for AI prefix
        val isAIRequest = commandName == "??" || commandName == "ai"
        if (isAIRequest && arguments.isNotEmpty()) {
            if (commandName == "ai") {
                arguments.removeAt(0) // Remove the "ai" part
            }
            return ProcessingResult.AIRequest(arguments.joinToString(" "))
        }

        // Find and execute command
        val handler = commandRegistry[commandName]
        if (handler != null) {
            return try {
                val result = handler.execute(context, arguments)
                ProcessingResult.Success(result)
            } catch (e: CommandException) {
                ProcessingResult.Error(e.message ?: "Command failed")
            } catch (e: Exception) {
                ProcessingResult.Error("Unexpected error: ${e.message}")
            }
        }

        // Check if it might be an app name
        return ProcessingResult.AppLaunch(commandName)
    }

    /**
     * Parses input into command and arguments
     */
    private fun parseInput(input: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var quoteChar = ' '

        for (char in input) {
            when {
                char == '"' || char == '\'' -> {
                    if (inQuotes && char == quoteChar) {
                        inQuotes = false
                    } else if (!inQuotes) {
                        inQuotes = true
                        quoteChar = char
                    } else {
                        current.append(char)
                    }
                }
                char == ' ' && !inQuotes -> {
                    if (current.isNotEmpty()) {
                        result.add(current.toString())
                        current.clear()
                    }
                }
                else -> current.append(char)
            }
        }

        if (current.isNotEmpty()) {
            result.add(current.toString())
        }

        return result
    }

    /**
     * Gets list of registered commands
     */
    fun getRegisteredCommands(): List<String> {
        return commandRegistry.keys.toList()
    }

    /**
     * Gets command suggestions based on partial input
     */
    fun getSuggestions(partial: String): List<String> {
        val lowerPartial = partial.lowercase()
        val suggestions = mutableListOf<String>()

        // Add matching commands
        commandRegistry.keys.filter { it.startsWith(lowerPartial) }.forEach {
            suggestions.add(it)
        }

        // Add matching aliases
        aliasManager.getAliases().filter { it.startsWith(lowerPartial) }.forEach {
            suggestions.add(it)
        }

        // Add matching apps
        val appLauncher = commandRegistry["launch"]
        if (appLauncher is AppLauncherCommand) {
            appLauncher.getSuggestions(partial).forEach {
                suggestions.add(it)
            }
        }

        return suggestions.distinct().take(10)
    }

    /**
     * Clears command history
     */
    fun clearHistory() {
        historyManager.clearHistory()
    }
}

/**
 * Result types from command processing
 */
sealed class ProcessingResult {
    data object Empty : ProcessingResult()
    data class Success(val output: String) : ProcessingResult()
    data class Error(val message: String) : ProcessingResult()
    data class AIRequest(val query: String) : ProcessingResult()
    data class AppLaunch(val appName: String) : ProcessingResult()
}

/**
 * Custom exception for command failures
 */
class CommandException(message: String) : Exception(message)

/**
 * Interface for command handlers
 */
interface CommandHandler {
    fun onRegister(context: Context) {}
    fun onUnregister(context: Context) {}
    fun getName(): String
    fun getAliases(): List<String>
    fun getDescription(): String
    fun getUsage(): String
    fun execute(context: Context, args: List<String>): String
}
