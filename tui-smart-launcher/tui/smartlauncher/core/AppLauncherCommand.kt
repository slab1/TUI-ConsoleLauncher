package tui.smartlauncher.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import launcher.tui.Andr3as07.tuilibrary.src.main.kotlin.CommandResult
import java.util.concurrent.ConcurrentHashMap

/**
 * App Launcher Command - Intelligent app launching with fuzzy search
 * Supports natural language queries, deep links, and app suggestions
 */
class AppLauncherCommand : CommandHandler {

    companion object {
        private const val TAG = "AppLauncher"
        private const val MAX_SUGGESTIONS = 10
    }

    private var appIndex: ConcurrentHashMap<String, AppEntry>? = null
    private var lastIndexUpdate: Long = 0
    private const val INDEX_CACHE_DURATION = 5 * 60 * 1000L // 5 minutes

    data class AppEntry(
        val packageName: String,
        val appName: String,
        val displayName: String,
        val launchIntent: Intent
    )

    override fun getName(): String = "launch"

    override fun getAliases(): List<String> = listOf("open", "start", "run", "app")

    override fun getDescription(): String = "Launch applications with intelligent search"

    override fun getUsage(): String = """
        ╔══════════════════════════════════════════════════════╗
        ║                  LAUNCH COMMAND                       ║
        ╠══════════════════════════════════════════════════════╣
        ║  launch <name>     - Launch app by name              ║
        ║  launch -l         - List installed apps             ║
        ║  launch -s <name>  - Search for app                  ║
        ║  launch -u <url>   - Open URL/deep link              ║
        ║  launch -i         - Show app info                   ║
        ╚══════════════════════════════════════════════════════╝
    """.trimIndent()

    override fun onRegister(context: Context) {
        // Build app index on background thread
        Thread { buildAppIndex(context) }.start()
    }

    override fun execute(context: Context, args: List<String>): String {
        if (args.isEmpty() || args[0] == "-h" || args[0] == "--help") {
            return getUsage()
        }

        val flags = mutableListOf<String>()
        val searchTerms = mutableListOf<String>()

        for (arg in args) {
            when {
                arg.startsWith("-") -> flags.add(arg.lowercase())
                else -> searchTerms.add(arg.lowercase())
            }
        }

        return when {
            flags.contains("-l") || flags.contains("--list") -> listApps(context)
            flags.contains("-s") || flags.contains("--search") -> searchApps(context, searchTerms)
            flags.contains("-u") || flags.contains("--url") -> openUrl(context, searchTerms.joinToString(" "))
            flags.contains("-i") || flags.contains("--info") -> showAppInfo(context, searchTerms)
            searchTerms.isNotEmpty() -> launchApp(context, searchTerms)
            else -> "Usage: launch <appname>\n${getUsage()}"
        }
    }

    /**
     * Builds index of all installed apps
     */
    private fun buildAppIndex(context: Context) {
        val startTime = System.currentTimeMillis()
        val newIndex = ConcurrentHashMap<String, AppEntry>()

        try {
            val pm = context.packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            for (app in apps) {
                // Skip system apps without launcher activity
                val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                if (launchIntent == null) continue

                val appName = app.loadLabel(pm).toString()
                val normalizedName = normalizeString(appName)

                // Index by multiple variations for better search
                newIndex[normalizedName] = AppEntry(
                    packageName = app.packageName,
                    appName = appName,
                    displayName = appName,
                    launchIntent = launchIntent
                )

                // Also index by package name components
                app.packageName.split(".").forEach { part ->
                    if (part.length > 2) {
                        val normalizedPart = normalizeString(part)
                        if (!newIndex.containsKey(normalizedPart)) {
                            newIndex[normalizedPart] = AppEntry(
                                packageName = app.packageName,
                                appName = appName,
                                displayName = "$appName (${app.packageName})",
                                launchIntent = launchIntent
                            )
                        }
                    }
                }
            }

            appIndex = newIndex
            lastIndexUpdate = System.currentTimeMillis()
            Log.d(TAG, "App index built: ${newIndex.size} apps in ${System.currentTimeMillis() - startTime}ms")
        } catch (e: Exception) {
            Log.e(TAG, "Error building app index: ${e.message}")
        }
    }

    /**
     * Refreshes app index if cache is stale
     */
    private fun ensureIndexUpdated(context: Context) {
        if (appIndex == null || System.currentTimeMillis() - lastIndexUpdate > INDEX_CACHE_DURATION) {
            buildAppIndex(context)
        }
    }

    /**
     * Launches an app by name with fuzzy matching
     */
    private fun launchApp(context: Context, searchTerms: List<String>): String {
        ensureIndexUpdated(context)
        val index = appIndex ?: return "Error: App index not available"

        val query = searchTerms.joinToString(" ").lowercase()
        if (query.isBlank()) return getUsage()

        // Try exact match first
        val normalizedQuery = normalizeString(query)
        val exactMatch = index[normalizedQuery]
        if (exactMatch != null) {
            return executeLaunch(context, exactMatch)
        }

        // Try fuzzy matching
        val candidates = findBestMatches(query, index.values.toList())

        return when {
            candidates.isEmpty() -> "No app found matching: ${searchTerms.joinToString(" ")}"
            candidates.size == 1 -> executeLaunch(context, candidates[0])
            else -> showSuggestions(candidates, query)
        }
    }

    /**
     * Executes app launch and returns result
     */
    private fun executeLaunch(context: Context, appEntry: AppEntry): String {
        return try {
            appEntry.launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(appEntry.launchIntent)
            "Launched: ${appEntry.displayName}"
        } catch (e: Exception) {
            "Failed to launch ${appEntry.displayName}: ${e.message}"
        }
    }

    /**
     * Finds best matching apps for a query
     */
    private fun findBestMatches(query: String, apps: List<AppEntry>): List<AppEntry> {
        val scores = mutableMapOf<AppEntry, Int>()

        for (app in apps) {
            var score = 0

            // Exact prefix match on display name
            if (app.displayName.lowercase().startsWith(query)) {
                score += 100
            }

            // Word-by-word matching
            val queryWords = query.split(" ")
            val appWords = app.displayName.lowercase().split(" ")
            for (queryWord in queryWords) {
                for (appWord in appWords) {
                    when {
                        appWord == queryWord -> score += 50
                        appWord.startsWith(queryWord) -> score += 30
                        appWord.contains(queryWord) -> score += 10
                    }
                }
            }

            // Package name match
            if (app.packageName.lowercase().contains(query.replace(" ", "."))) {
                score += 20
            }

            // Character sequence match (substring)
            if (app.displayName.lowercase().contains(query)) {
                score += 5
            }

            if (score > 0) {
                scores[app] = score
            }
        }

        return scores.entries
            .sortedByDescending { it.value }
            .take(MAX_SUGGESTIONS)
            .filter { it.value > 0 }
            .map { it.key }
    }

    /**
     * Shows app suggestions when multiple matches found
     */
    private fun showSuggestions(apps: List<AppEntry>, query: String): String {
        val builder = StringBuilder()
        builder.appendLine()
        builder.appendLine("Multiple apps found for \"$query\":")
        builder.appendLine("─".repeat(50))

        apps.forEachIndexed { index, app ->
            builder.appendLine("  ${index + 1}. ${app.displayName}")
            builder.appendLine("     ${app.packageName}")
        }

        builder.appendLine()
        builder.appendLine("Use: launch \"<exact name>\" or number 1-${apps.size}")
        return builder.toString()
    }

    /**
     * Lists all installed apps
     */
    private fun listApps(context: Context): String {
        ensureIndexUpdated(context)
        val index = appIndex ?: return "Error: App index not available"

        val apps = index.values
            .sortedBy { it.displayName }
            .take(100)

        val builder = StringBuilder()
        builder.appendLine()
        builder.appendLine("Installed Apps (${index.size} total, showing first 100):")
        builder.appendLine("─".repeat(50))

        apps.forEach { app ->
            builder.appendLine("  ${app.displayName}")
        }

        if (index.size > 100) {
            builder.appendLine("  ... and ${index.size - 100} more")
        }

        return builder.toString()
    }

    /**
     * Searches for apps matching a pattern
     */
    private fun searchApps(context: Context, terms: List<String>): String {
        ensureIndexUpdated(context)
        val index = appIndex ?: return "Error: App index not available"

        val query = terms.joinToString(" ").lowercase()
        val results = index.values.filter { app ->
            app.displayName.lowercase().contains(query) ||
            app.packageName.lowercase().contains(query)
        }.sortedBy { it.displayName }.take(20)

        val builder = StringBuilder()
        builder.appendLine()
        builder.appendLine("Search results for \"$query\" (${results.size} found):")
        builder.appendLine("─".repeat(50))

        results.forEach { app ->
            builder.appendLine("  ${app.displayName}")
            builder.appendLine("    ${app.packageName}")
        }

        return builder.toString()
    }

    /**
     * Opens a URL or deep link
     */
    private fun openUrl(context: Context, url: String): String {
        if (url.isBlank()) return "Usage: launch -u <url>"

        val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
            "https://$url"
        } else {
            url
        }

        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            "Opened: $formattedUrl"
        } catch (e: Exception) {
            "Failed to open URL: ${e.message}"
        }
    }

    /**
     * Shows information about a specific app
     */
    private fun showAppInfo(context: Context, terms: List<String>): String {
        ensureIndexUpdated(context)
        val index = appIndex ?: return "Error: App index not available"

        val query = terms.joinToString(" ").lowercase()
        val app = index.values.find {
            it.displayName.lowercase().contains(query) ||
            it.packageName.lowercase().contains(query)
        }

        return if (app != null) {
            buildString {
                appendLine()
                appendLine("App Information:")
                appendLine("═".repeat(50))
                appendLine("  Name: ${app.displayName}")
                appendLine("  Package: ${app.packageName}")
                appendLine("  Launch Intent: ${app.launchIntent.component?.className ?: "Unknown"}")
            }
        } else {
            "App not found: $query"
        }
    }

    /**
     * Gets suggestions for autocomplete
     */
    fun getSuggestions(partial: String): List<String> {
        val index = appIndex ?: return emptyList()
        val normalizedPartial = normalizeString(partial)

        return index.values
            .filter { it.displayName.lowercase().startsWith(normalizedPartial) }
            .map { it.displayName }
            .take(5)
    }

    /**
     * Normalizes string for comparison
     */
    private fun normalizeString(input: String): String {
        return input.lowercase()
            .replace(Regex("[^a-z0-9]"), "")
    }
}
