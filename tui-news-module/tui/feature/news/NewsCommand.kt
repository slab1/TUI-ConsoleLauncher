package tui.feature.news

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import launcher.tui.Andr3as07.tuilibrary.src.main.kotlin.Command
import launcher.tui.Andr3as07.tuilibrary.src.main.kotlin.CommandResult
import launcher.tui.Andr3as07.tuilibrary.src.main.kotlin.tools.Tools

/**
 * T-UI Launcher Command for fetching news via MiniMax AI
 * 
 * Usage:
 *   news                    - Get general news
 *   news <topic>            - Get news for specific topic
 *   news -d                 - Get detailed news
 *   news <topic> -d         - Get detailed news for topic
 *   news --config           - Show current configuration
 *   news --help             - Show help message
 */
class NewsCommand : Command {

    companion object {
        private const val TAG = "NewsCommand"
        
        // Command aliases
        const val COMMAND_NAME = "news"
        const val ALIAS_NEWS = "n"
        const val ALIAS_GETNEWS = "getnews"
        
        // Animation frames for loading state
        private val LOADING_FRAMES = listOf("│", "╱", "─", "╲")
        private const val LOADING_DELAY_MS = 200L
    }

    private val repository = MiniMaxRepository(null!!) // Will be initialized in execute
    private val config: NewsConfig? = null // Will be initialized in execute
    private var loadingHandler: Handler? = null
    private var loadingRunnable: Runnable? = null
    private var isLoading = false

    override fun getCommandName(): String = COMMAND_NAME

    override fun getCommandAliases(): Array<String> {
        return arrayOf(ALIAS_NEWS, ALIAS_GETNEWS)
    }

    override fun getCommandDescription(): String {
        return "Fetch AI-powered news summaries from MiniMax\n" +
               "Usage: news [topic] [-d|--detailed]"
    }

    override fun getCommandUsage(): String {
        return """
        ╔══════════════════════════════════════════════════════╗
        ║                    NEWS COMMAND                       ║
        ╠══════════════════════════════════════════════════════╣
        ║  news              - General top headlines            ║
        ║  news tech         - Technology news                  ║
        ║  news sports       - Sports news                      ║
        ║  news finance      - Financial news                   ║
        ║  news -d           - Detailed mode (fewer, deeper)    ║
        ║  news --config     - Show configuration               ║
        ║  news --help       - Show this help                   ║
        ╚══════════════════════════════════════════════════════╝
        """.trimIndent()
    }

    override fun onExecute(context: Context, args: ArrayList<String>): CommandResult {
        if (args.isEmpty()) {
            return executeNewsQuery(context, "general", false)
        }

        val arguments = parseArguments(args)
        val topic = arguments.topic
        val detailed = arguments.detailed

        // Handle special flags
        return when {
            arguments.showHelp -> showHelp(context)
            arguments.showConfig -> showConfig(context)
            else -> executeNewsQuery(context, topic, detailed)
        }
    }

    /**
     * Executes the news query and displays results
     */
    private fun executeNewsQuery(context: Context, topic: String, detailed: Boolean): CommandResult {
        loadingHandler = Handler(Looper.getMainLooper())
        
        // Show loading animation
        showLoading(context, "Fetching news about '$topic'...")
        
        // Execute in background
        loadingHandler?.post {
            try {
                val tempConfig = NewsConfig(context)
                val tempRepo = MiniMaxRepository(context)
                tempConfig.isDetailedMode = detailed
                
                val result = tempRepo.getNews(topic, forceRefresh = true)
                
                hideLoading()
                
                result.fold(
                    onSuccess = { newsText ->
                        val formattedText = formatForTerminal(newsText, detailed)
                        CommandResult.success(formattedText)
                    },
                    onFailure = { error ->
                        val errorMsg = formatError(error.message ?: "Unknown error")
                        CommandResult.error(errorMsg)
                    }
                )
            } catch (e: Exception) {
                hideLoading()
                Log.e(TAG, "Error executing news command", e)
                CommandResult.error(formatError(e.message ?: "Unexpected error"))
            }
        }
        
        // Return pending result while loading
        return CommandResult.pending()
    }

    /**
     * Shows help information
     */
    private fun showHelp(context: Context): CommandResult {
        return CommandResult.success(getCommandUsage())
    }

    /**
     * Shows current configuration
     */
    private fun showConfig(context: Context): CommandResult {
        val config = NewsConfig(context)
        return CommandResult.success(config.getSettingsSummary())
    }

    /**
     * Parses command arguments
     */
    private data class ParsedArguments(
        val topic: String = "general",
        val detailed: Boolean = false,
        val showHelp: Boolean = false,
        val showConfig: Boolean = false
    )

    private fun parseArguments(args: ArrayList<String>): ParsedArguments {
        var topic = "general"
        var detailed = false
        var showHelp = false
        var showConfig = false

        val iterator = args.iterator()
        while (iterator.hasNext()) {
            val arg = iterator.next()
            
            when {
                arg.startsWith("-") -> {
                    when (arg.lowercase()) {
                        "-d", "--detailed" -> detailed = true
                        "-h", "--help" -> showHelp = true
                        "--config" -> showConfig = true
                        else -> Log.w(TAG, "Unknown flag: $arg")
                    }
                }
                !arg.startsWith("-") && topic == "general" -> topic = arg
                else -> Log.w(TAG, "Unexpected argument: $arg")
            }
        }

        return ParsedArguments(topic, detailed, showHelp, showConfig)
    }

    /**
     * Shows loading animation in terminal
     */
    private fun showLoading(context: Context, message: String) {
        isLoading = true
        var frameIndex = 0
        
        loadingRunnable = object : Runnable {
            override fun run() {
                if (!isLoading) return
                
                val frame = LOADING_FRAMES[frameIndex % LOADING_FRAMES.size]
                val loadingText = "[$frame] $message"
                Tools.addToLog(context, loadingText, false)
                
                frameIndex++
                loadingHandler?.postDelayed(this, LOADING_DELAY_MS)
            }
        }
        
        loadingHandler?.post(loadingRunnable!!)
    }

    /**
     * Hides loading animation
     */
    private fun hideLoading() {
        isLoading = false
        loadingRunnable?.let { loadingHandler?.removeCallbacks(it) }
        loadingRunnable = null
    }

    /**
     * Formats news text for terminal display
     */
    private fun formatForTerminal(text: String, detailed: Boolean): String {
        val builder = StringBuilder()
        
        // Clean up the response
        val cleaned = text
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
        
        builder.appendLine()
        builder.append(cleaned)
        builder.appendLine()
        
        return builder.toString()
    }

    /**
     * Formats error messages for terminal display
     */
    private fun formatError(message: String): String {
        return buildString {
            appendLine()
            appendLine("┌" + "─".repeat(48) + "┐")
            appendLine("│" + " ".repeat(15) + "ERROR" + " ".repeat(22) + "│")
            appendLine("├" + "─".repeat(48) + "┤")
            appendLine("│  $message")
            appendLine("│")
            appendLine("│  Tips:")
            appendLine("│  • Check your internet connection")
            appendLine("│  • Verify MiniMax API key in settings")
            appendLine("│  • Try 'news --config' to check configuration")
            appendLine("└" + "─".repeat(48) + "┘")
        }
    }

    override fun onStop() {
        hideLoading()
    }
}
