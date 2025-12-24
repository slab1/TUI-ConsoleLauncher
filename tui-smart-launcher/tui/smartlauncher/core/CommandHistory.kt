package tui.smartlauncher.core

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Command History Manager - Stores and manages command history
 * Supports history navigation, search, and statistics
 */
class CommandHistory(private val context: Context) {

    companion object {
        private const val TAG = "CommandHistory"
        private const val PREFS_NAME = "tui_history_prefs"
        private const val KEY_HISTORY = "command_history"
        private const val MAX_HISTORY_SIZE = 1000
        private const val MAX_LINE_LENGTH = 200
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    private data class HistoryEntry(
        val command: String,
        val timestamp: Long,
        val success: Boolean
    )

    /**
     * Adds a command to history
     */
    fun addToHistory(command: String, success: Boolean = true) {
        val trimmed = command.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_LINE_LENGTH) return

        val history = getHistory().toMutableList()

        // Remove duplicates (keep most recent)
        history.removeAll { it.command == trimmed }

        // Add new entry at the beginning
        history.add(0, HistoryEntry(trimmed, System.currentTimeMillis(), success))

        // Trim to max size
        if (history.size > MAX_HISTORY_SIZE) {
            history.subList(MAX_HISTORY_SIZE, history.size).clear()
        }

        saveHistory(history)
        Log.d(TAG, "Added to history: $trimmed")
    }

    /**
     * Gets full command history
     */
    fun getHistory(): List<HistoryEntry> {
        val json = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        return parseHistoryJson(json)
    }

    /**
     * Gets history for specific command
     */
    fun getHistoryFor(command: String): List<HistoryEntry> {
        return getHistory().filter { it.command.startsWith(command) }
    }

    /**
     * Gets recent commands
     */
    fun getRecent(count: Int = 20): List<String> {
        return getHistory().take(count).map { it.command }
    }

    /**
     * Searches history for matching commands
     */
    fun search(query: String): List<HistoryEntry> {
        val lowerQuery = query.lowercase()
        return getHistory().filter {
            it.command.lowercase().contains(lowerQuery)
        }
    }

    /**
     * Gets history statistics
     */
    fun getStatistics(): HistoryStats {
        val history = getHistory()
        if (history.isEmpty()) {
            return HistoryStats(0, 0, emptyList(), emptyMap())
        }

        val successful = history.count { it.success }
        val failed = history.size - successful

        // Most used commands
        val commandCounts = history.groupingBy { it.command }.eachCount()
        val mostUsed = commandCounts.entries.sortedByDescending { it.value }.take(5)

        // Activity by hour
        val hourActivity = history.groupingBy {
            java.util.Calendar.getInstance().apply {
                timeInMillis = it.timestamp
            }.get(java.util.Calendar.HOUR_OF_DAY)
        }.eachCount()

        return HistoryStats(
            totalCommands = history.size,
            successRate = if (history.isNotEmpty()) (successful * 100.0 / history.size) else 0.0,
            mostUsedCommands = mostUsed.map { it.key },
            activityByHour = hourActivity
        )
    }

    /**
     * Clears command history
     */
    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
        Log.d(TAG, "Command history cleared")
    }

    /**
     * Exports history as JSON
     */
    fun exportHistory(): String {
        return buildString {
            appendLine("[")
            getHistory().forEachIndexed { index, entry ->
                appendLine("  {")
                appendLine("    \"command\": \"${entry.command.escapeJson()}\",")
                appendLine("    \"timestamp\": ${entry.timestamp},")
                appendLine("    \"success\": ${entry.success}")
                appendLine("  }${if (index < getHistory().size - 1) "," else ""}")
            }
            appendLine("]")
        }
    }

    /**
     * Imports history from JSON
     */
    fun importHistory(json: String): Int {
        return try {
            val imported = parseHistoryJson(json)
            val current = getHistory().toMutableList()

            // Merge, avoiding duplicates
            imported.forEach { entry ->
                if (current.none { it.command == entry.command }) {
                    current.add(entry)
                }
            }

            // Trim to max size
            if (current.size > MAX_HISTORY_SIZE) {
                current.subList(MAX_HISTORY_SIZE, current.size).clear()
            }

            saveHistory(current)
            imported.size
        } catch (e: Exception) {
            Log.e(TAG, "Error importing history: ${e.message}")
            0
        }
    }

    private fun saveHistory(history: List<HistoryEntry>) {
        val json = history.joinToString(",", "[", "]") { entry ->
            "{\"cmd\":\"${entry.command.escapeJson()}\",\"ts\":${entry.timestamp},\"ok\":${entry.success}}"
        }
        prefs.edit().putString(KEY_HISTORY, json).apply()
    }

    private fun parseHistoryJson(json: String): List<HistoryEntry> {
        if (json == "[]" || json.isBlank()) return emptyList()

        return try {
            val cleanJson = json.trim().removeSurrounding("[", "]")
            if (cleanJson.isBlank()) return emptyList()

            cleanJson.split("},{").mapIndexed { index, segment ->
                val clean = segment.removeSurrounding("{", "}")
                val parts = clean.split(",")
                val cmdPart = parts.find { it.contains("cmd") }?.substringAfter(":")?.trim()?.removeSurrounding("\"") ?: ""
                val tsPart = parts.find { it.contains("ts") }?.substringAfter(":")?.trim()?.toLongOrNull() ?: 0L
                val okPart = parts.find { it.contains("ok") }?.substringAfter(":")?.trim()?.toBooleanStrictOrNull() ?: true

                HistoryEntry(cmdPart, tsPart, okPart)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing history JSON: ${e.message}")
            emptyList()
        }
    }

    private fun String.escapeJson(): String {
        return this.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    data class HistoryStats(
        val totalCommands: Int,
        val successRate: Double,
        val mostUsedCommands: List<String>,
        val activityByHour: Map<Int, Int>
    )
}
