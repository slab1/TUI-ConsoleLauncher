package tui.smartlauncher

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import tui.smartlauncher.automation.AutomationCommand
import tui.smartlauncher.core.AliasManager
import tui.smartlauncher.core.AppLauncherCommand
import tui.smartlauncher.core.CommandHistory
import tui.smartlauncher.core.CommandProcessor
import tui.smartlauncher.core.CommandHandler
import tui.smartlauncher.developer.FileManagerCommand
import tui.smartlauncher.developer.GitCommand
import tui.smartlauncher.productivity.CalculatorCommand
import tui.smartlauncher.productivity.NotesCommand
import tui.smartlauncher.productivity.NetworkCommand
import tui.smartlauncher.productivity.SystemCommand

/**
 * Main Activity for T-UI Smart IDE Launcher
 * Integrates all command modules and provides the terminal interface
 */
class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var inputField: EditText
    private lateinit var commandProcessor: CommandProcessor
    private lateinit var terminalAdapter: TerminalAdapter
    private val commandHistory = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        initializeCommandProcessor()
        registerCommands()
        showWelcome()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.terminal_recycler_view)
        inputField = findViewById(R.id.command_input)

        // Setup RecyclerView
        terminalAdapter = TerminalAdapter()
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true
            }
            adapter = terminalAdapter
        }

        // Setup input field
        inputField.setOnEditorActionListener { _, _, _ ->
            executeCommand()
            true
        }

        // Clear button
        findViewById<View>(R.id.clear_button)?.setOnClickListener {
            terminalAdapter.clearHistory()
            showWelcome()
        }
    }

    private fun initializeCommandProcessor() {
        commandProcessor = CommandProcessor(this)

        // Register alias manager and history callbacks
        commandProcessor = object : CommandProcessor(this) {
            override fun processInput(input: String): ProcessingResult {
                val result = super.processInput(input)
                if (result !is ProcessingResult.Empty && result !is ProcessingResult.Pending) {
                    commandHistory.add(input)
                }
                return result
            }
        }
    }

    private fun registerCommands() {
        // Core Commands
        commandProcessor.registerCommand("launch", AppLauncherCommand())
        commandProcessor.registerCommand("help", HelpCommand())

        // Developer Commands
        commandProcessor.registerCommand("file", FileManagerCommand())
        commandProcessor.registerCommand("git", GitCommand())

        // Productivity Commands
        commandProcessor.registerCommand("calc", CalculatorCommand())
        commandProcessor.registerCommand("system", SystemCommand())
        commandProcessor.registerCommand("note", NotesCommand())
        commandProcessor.registerCommand("network", NetworkCommand())

        // Automation Commands
        commandProcessor.registerCommand("auto", AutomationCommand())

        // Register with aliases
        registerAdditionalAliases()
    }

    private fun registerAdditionalAliases() {
        val aliasManager = AliasManager(this)

        // Add default aliases
        val defaults = mapOf(
            "ls" to "file ls",
            "cd" to "file cd",
            "pwd" to "file pwd",
            "cat" to "file cat",
            "mkdir" to "file mkdir",
            "rm" to "file rm",
            "cp" to "file cp",
            "mv" to "file mv",
            "g" to "git",
            "cls" to "clear",
            "?" to "help",
            "=" to "calc",
            "sys" to "system",
            "net" to "network"
        )

        defaults.forEach { (alias, expansion) ->
            aliasManager.addAlias(alias, expansion)
        }
    }

    private fun showWelcome() {
        val welcome = buildString {
            appendLine("╔═══════════════════════════════════════════════════════════════╗")
            appendLine("║                                                               ║")
            appendLine("║    T-U I   S M A R T   I D E   L A U N C H E R              ║")
            appendLine("║                                                               ║")
            appendLine("║    Version 1.0.0                                             ║")
            appendLine("║                                                               ║")
            appendLine("╠═══════════════════════════════════════════════════════════════╣")
            appendLine("║  Your intelligent terminal-based mobile launcher             ║")
            appendLine("║  - Developer tools, AI assistance, automation & more         ║")
            appendLine("║                                                               ║")
            appendLine("╚═══════════════════════════════════════════════════════════════╝")
            appendLine()
            appendLine("Quick Start:")
            appendLine("  help              - Show all commands")
            appendLine("  launch <app>      - Launch applications")
            appendLine("  ?? <question>     - Ask AI anything")
            appendLine("  file ls           - List files")
            appendLine("  calc 2+2          - Quick calculation")
            appendLine()
            appendLine("Type 'help' for full command reference.")
            appendLine()
        }
        terminalAdapter.addOutput(welcome)
    }

    private fun executeCommand() {
        val input = inputField.text.toString().trim()
        if (input.isEmpty()) return

        // Show input in terminal
        terminalAdapter.addInput(input)
        inputField.text?.clear()

        // Process command
        Thread {
            try {
                val result = commandProcessor.processInput(input)

                runOnUiThread {
                    when (result) {
                        is ProcessingResult.Success -> {
                            terminalAdapter.addOutput(result.output)
                            scrollToBottom()
                        }
                        is ProcessingResult.Error -> {
                            terminalAdapter.addError(result.message)
                            scrollToBottom()
                        }
                        is ProcessingResult.AIRequest -> {
                            handleAIQuery(result.query)
                        }
                        is ProcessingResult.AppLaunch -> {
                            handleAppLaunch(result.appName)
                        }
                        is ProcessingResult.Empty -> {
                            // No output needed
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    terminalAdapter.addError("Error: ${e.message}")
                    scrollToBottom()
                }
            }
        }.start()
    }

    private fun handleAIQuery(query: String) {
        terminalAdapter.addOutput("AI Query: $query\nProcessing...")

        // TODO: Integrate with MiniMaxService
        // For now, show placeholder
        terminalAdapter.addOutput("""
            ╔══════════════════════════════════════════════════════════════════╗
            ║  AI Assistant - Configure MiniMax API key to enable             ║
            ║                                                                 ║
            ║  Set API key: ai --config set <your-api-key>                    ║
            ╚══════════════════════════════════════════════════════════════════╝
        """.trimIndent())
        scrollToBottom()
    }

    private fun handleAppLaunch(appName: String) {
        terminalAdapter.addOutput("App launch: $appName")
        // Let AppLauncherCommand handle the actual launch
        val result = commandProcessor.processInput("launch $appName")
        if (result is ProcessingResult.Error) {
            terminalAdapter.addError("App not found: $appName")
        }
        scrollToBottom()
    }

    private fun scrollToBottom() {
        recyclerView.post {
            recyclerView.scrollToPosition(terminalAdapter.itemCount - 1)
        }
    }

    override fun onResume() {
        super.onResume()
        scrollToBottom()
    }
}

/**
 * Simple help command implementation
 */
class HelpCommand : CommandHandler {
    override fun getName(): String = "help"

    override fun getAliases(): List<String> = listOf("?", "commands")

    override fun getDescription(): String = "Show help information"

    override fun getUsage(): String = """
        ╔══════════════════════════════════════════════════════╗
        ║                      HELP                             ║
        ╠══════════════════════════════════════════════════════╣
        ║  help                  - Show this help              ║
        ║  help <command>        - Show specific command help  ║
        ╚══════════════════════════════════════════════════════╝
    """.trimIndent()

    override fun execute(context: Context, args: List<String>): String {
        return buildString {
            appendLine()
            appendLine("╔═══════════════════════════════════════════════════════════════════╗")
            appendLine("║                    T-UI SMART LAUNCHER                           ║")
            appendLine("║                      Command Reference                           ║")
            appendLine("╠═══════════════════════════════════════════════════════════════════╣")
            appendLine()
            appendLine("CORE COMMANDS")
            appendLine("─".repeat(70))
            appendLine("  launch <name>     Launch apps with fuzzy search")
            appendLine("  help              Show this help message")
            appendLine("  clear             Clear terminal screen")
            appendLine()
            appendLine("DEVELOPER TOOLS")
            appendLine("─".repeat(70))
            appendLine("  file <cmd>        File management (ls, cd, cat, etc.)")
            appendLine("  git <cmd>         Git version control")
            appendLine()
            appendLine("PRODUCTIVITY")
            appendLine("─".repeat(70))
            appendLine("  calc <expr>       Calculator with expression eval")
            appendLine("  system            System resource monitoring")
            appendLine("  note              Quick notes management")
            appendLine("  network           Network diagnostic tools")
            appendLine()
            appendLine("AUTOMATION")
            appendLine("─".repeat(70))
            appendLine("  auto              Tasker and Termux integration")
            appendLine()
            appendLine("AI ASSISTANCE")
            appendLine("─".repeat(70))
            appendLine("  ?? <question>     Ask AI anything")
            appendLine()
            appendLine("For detailed help on a command, type: help <command>")
            appendLine()
            appendLine("╚═══════════════════════════════════════════════════════════════════╝")
        }
    }
}
