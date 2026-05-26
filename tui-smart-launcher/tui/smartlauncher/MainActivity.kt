package tui.smartlauncher

import android.os.Bundle
import android.text.Editable
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.PopupWindow
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tui.smartlauncher.ai.AIEvent
import tui.smartlauncher.ai.AIUsageStats
import tui.smartlauncher.ai.MiniMaxService
import tui.smartlauncher.automation.AutomationCommand
import tui.smartlauncher.core.AliasCommand
import tui.smartlauncher.core.AliasManager
import tui.smartlauncher.core.AppLauncherCommand
import tui.smartlauncher.core.CommandHistory
import tui.smartlauncher.core.CommandProcessor
import tui.smartlauncher.core.HelpCommand
import tui.smartlauncher.core.ThemeCommand
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

    // Command history navigation
    private var historyIndex = -1
    private var currentInput = ""

    // Autocomplete suggestions popup
    private var suggestionsPopup: PopupWindow? = null
    private var lastSuggestions = listOf<String>()

    // Session persistence
    private val PREFS_SESSION = "tui_session_prefs"
    private val KEY_SESSION_OUTPUT = "session_output"

    // ── Smart AI Fields ────────────────────────────────────────────────────────

    private var aiService: MiniMaxService? = null
    private var currentAiResponse = StringBuilder()

    // ── Quick AI action keywords ───────────────────────────────────────────────

    private val quickAiActions = listOf("summarize", "translate", "fix", "explain", "debug")

    // ═══════════════════════════════════════════════════════════════════════════
    //  Lifecycle
    // ═══════════════════════════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()

        // Load saved theme
        ThemeCommand.currentTheme = ThemeCommand.loadTheme(this)
        terminalAdapter.setTheme(ThemeCommand.currentTheme)

        initializeCommandProcessor()
        registerCommands()
        showWelcome()
        restoreSession()
    }

    override fun onResume() {
        super.onResume()
        scrollToBottom()
    }

    override fun onPause() {
        super.onPause()
        saveSession()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  View Initialization
    // ═══════════════════════════════════════════════════════════════════════════

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

        // Command history navigation (Up/Down arrows)
        inputField.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    val history = commandHistory
                    if (history.isEmpty()) return@setOnKeyListener true
                    if (historyIndex == -1) currentInput = inputField.text.toString()
                    historyIndex = (historyIndex + 1).coerceAtMost(history.size - 1)
                    inputField.setText(history[history.size - 1 - historyIndex])
                    inputField.setSelection(inputField.text?.length ?: 0)
                    true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    val history = commandHistory
                    if (historyIndex > 0) {
                        historyIndex--
                        inputField.setText(history[history.size - 1 - historyIndex])
                        inputField.setSelection(inputField.text?.length ?: 0)
                    } else {
                        historyIndex = -1
                        inputField.setText(currentInput)
                        inputField.setSelection(inputField.text?.length ?: 0)
                    }
                    true
                }
                else -> false
            }
        }

        // Autocomplete suggestions as user types
        inputField.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString()?.trim() ?: ""
                if (text.length >= 1) {
                    val suggestions = commandProcessor.getSuggestions(text)
                    showSuggestions(suggestions)
                } else {
                    dismissSuggestions()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Clear button
        findViewById<View>(R.id.clear_button)?.setOnClickListener {
            terminalAdapter.clearHistory()
            showWelcome()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Command Processor
    // ═══════════════════════════════════════════════════════════════════════════

    private fun initializeCommandProcessor() {
        // Create command processor with history tracking
        commandProcessor = object : CommandProcessor(this) {
            override fun processInput(input: String): ProcessingResult {
                val result = super.processInput(input)
                if (result !is ProcessingResult.Empty) {
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

        // Management Commands
        commandProcessor.registerCommand("alias", AliasCommand())
        commandProcessor.registerCommand("theme", ThemeCommand())

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

    // ═══════════════════════════════════════════════════════════════════════════
    //  Welcome Screen
    // ═══════════════════════════════════════════════════════════════════════════

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
            appendLine("AI Commands:")
            appendLine("  ?? !!                - Repeat last query")
            appendLine("  ?? !                 - Show cached response")
            appendLine("  ?? --clear           - Reset conversation memory")
            appendLine("  ?? --persona <name>  - Switch AI persona")
            appendLine("  ?? --personas        - List personas")
            appendLine("  ?? --provider <name> - Switch AI provider")
            appendLine("  ?? --providers       - List providers")
            appendLine("  ?? --usage           - Show usage stats")
            appendLine("  ?? --key <prov> <key> - Set API key for a provider")
            appendLine("  ?? --keys            - Show configured API keys")
            appendLine("  summarize <text>     - Summarize with AI")
            appendLine("  translate <text>     - Translate with AI")
            appendLine("  fix <code>           - Debug with AI")
            appendLine("  explain <topic>      - Explain with AI")
            appendLine()
            appendLine()
            appendLine("Type 'help' for full command reference.")
            appendLine()
        }
        terminalAdapter.addOutput(welcome)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Command Execution
    // ═══════════════════════════════════════════════════════════════════════════

    private fun executeCommand() {
        val input = inputField.text.toString().trim()
        if (input.isEmpty()) return

        // Show input in terminal
        terminalAdapter.addInput(input)
        inputField.text?.clear()

        // ── Quick AI Action Commands ──────────────────────────────────────────
        // Intercept before the command processor so that bare keywords like
        // "summarize <text>" get routed directly to the AI.
        val firstWord = input.split(" ").firstOrNull()?.lowercase() ?: ""
        if (firstWord in quickAiActions) {
            handleAIQuery(input)
            return
        }

        // Process command on background thread using coroutines
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    commandProcessor.processInput(input)
                } catch (e: Exception) {
                    ProcessingResult.Error("Error: ${e.message}")
                }
            }

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
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  AI Query Handling  (NEW smart version)
    // ═══════════════════════════════════════════════════════════════════════════

    private fun handleAIQuery(query: String) {
        terminalAdapter.addOutput("\uD83E\uDD16 AI: $query")

        val normalized = query.trim().lowercase()

        // ── Handle special AI control commands ───────────────────────────────
        when {
            normalized == "!!" || normalized == "--repeat" -> {
                repeatLastQuery()
                return
            }
            normalized == "!" || normalized == "--last" || normalized == "--cache" -> {
                showLastResponse()
                return
            }
            normalized in listOf("--clear", "--reset", "clear", "reset") -> {
                aiService?.clearMemory()
                terminalAdapter.addOutput("Conversation memory cleared.")
                scrollToBottom()
                return
            }
            normalized.startsWith("--persona ") || normalized.startsWith("persona ") -> {
                val personaName = normalized
                    .removePrefix("--persona ")
                    .removePrefix("persona ")
                    .trim()
                handlePersonaSwitch(personaName)
                return
            }
            normalized in listOf("--personas", "--persona", "personas", "persona") -> {
                showPersonas()
                return
            }
            normalized.startsWith("--provider ") || normalized.startsWith("provider ") -> {
                val providerName = normalized
                    .removePrefix("--provider ")
                    .removePrefix("provider ")
                    .trim()
                handleProviderSwitch(providerName)
                return
            }
            normalized in listOf("--providers", "--provider", "providers") -> {
                showProviders()
                return
            }
            normalized in listOf("--usage", "--stats", "usage", "stats") -> {
                showUsageStats()
                return
            }
            normalized.startsWith("--key ") || normalized.startsWith("key ") -> {
                val parts = normalized.split("\\s+".toRegex(), 3)
                if (parts.size < 3) {
                    terminalAdapter.addError("Usage: ?? --key <provider> <api_key>")
                } else {
                    val provider = parts[1]
                    val key = query.trim().split("\\s+".toRegex(), 3)[2] // preserve original casing
                    val ai = aiService ?: MiniMaxService(this@MainActivity).also { aiService = it }
                    ai.setApiKey(provider, key)
                    terminalAdapter.addOutput("API key set for provider: $provider")
                }
                scrollToBottom(); return
            }
            normalized in listOf("--keys", "--key", "keys") -> {
                showApiKeys()
                return
            }
        }

        // ── Normal AI query with streaming ───────────────────────────────────
        currentAiResponse = StringBuilder()

        lifecycleScope.launch {
            val ai = aiService ?: MiniMaxService(this@MainActivity).also { aiService = it }

            terminalAdapter.addOutput("\u23F3 Thinking...")
            scrollToBottom()

            ai.smartQuery(query, stream = true)
                .catch { e ->
                    terminalAdapter.addError("AI error: ${e.message}")
                    scrollToBottom()
                }
                .onCompletion {
                    // Ensure any trailing state is resolved
                }
                .collect { event ->
                    when (event) {
                        is AIEvent.Thinking -> {
                            // The "Thinking..." indicator is already shown above.
                            // Could update it in-place for a more polished UX.
                        }
                        is AIEvent.Chunk -> {
                            currentAiResponse.append(event.text)
                            // For a live preview you could update the last line,
                            // but for simplicity we accumulate and show on Complete.
                        }
                        is AIEvent.Complete -> {
                            // Remove the "Thinking..." line and show the full response
                            // Since we can't easily remove previous lines from the
                            // RecyclerView, we replace by adding an output.
                            terminalAdapter.addOutput(event.fullResponse)
                            scrollToBottom()
                        }
                        is AIEvent.Error -> {
                            terminalAdapter.addError(event.message)
                            scrollToBottom()
                        }
                        is AIEvent.CommandSuggestion -> {
                            terminalAdapter.addOutput("")
                            terminalAdapter.addOutput("\uD83D\uDCBB Command suggestion:")
                            terminalAdapter.addOutput("  ${event.command}")
                            terminalAdapter.addOutput("  ${event.explanation}")
                            terminalAdapter.addOutput("  Type the command to execute it.")
                            scrollToBottom()
                        }
                        is AIEvent.ContextInfo -> {
                            terminalAdapter.addInfo(event.info)
                        }
                    }
                }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  AI Helper Methods
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Re-sends the last AI query (if any).
     */
    private fun repeatLastQuery() {
        val lastQuery = aiService?.getLastQuery() ?: run {
            terminalAdapter.addError("No previous query to repeat.")
            scrollToBottom()
            return
        }
        handleAIQuery(lastQuery)
    }

    /**
     * Displays the last cached AI response.
     */
    private fun showLastResponse() {
        val lastResponse = aiService?.getLastResponse() ?: run {
            terminalAdapter.addError("No cached response available.")
            scrollToBottom()
            return
        }
        terminalAdapter.addOutput("\uD83D\uDDA5\uFE0F Cached response:")
        terminalAdapter.addOutput(lastResponse)
        scrollToBottom()
    }

    /**
     * Switches the AI to the named persona.
     */
    private fun handlePersonaSwitch(name: String) {
        val ai = aiService ?: MiniMaxService(this@MainActivity).also { aiService = it }
        if (ai.setPersona(name)) {
            terminalAdapter.addOutput("Switched to persona: $name")
        } else {
            val personas = ai.getPersonas().joinToString(", ") { it.name }
            terminalAdapter.addError("Persona not found: $name\nAvailable: $personas")
        }
        scrollToBottom()
    }

    /**
     * Lists all available AI personas with a selection marker on the current one.
     */
    private fun showPersonas() {
        val ai = aiService ?: MiniMaxService(this@MainActivity).also { aiService = it }
        val current = ai.getCurrentPersona().name
        val personas = ai.getPersonas()
        val output = buildString {
            appendLine()
            appendLine("╔══════════════════════════════════════════════╗")
            appendLine("║          AI PERSONAS                        ║")
            appendLine("╠══════════════════════════════════════════════╣")
            personas.forEach { p ->
                val marker = if (p.name == current) " \u25C9" else " \u25CB"
                val desc = p.description.take(28)
                appendLine("║$marker ${p.name.padEnd(20)} $desc║")
            }
            appendLine("╚══════════════════════════════════════════════╝")
            appendLine()
            appendLine("Usage: ?? --persona <name>")
            appendLine("       ?? --personas (this list)")
        }
        terminalAdapter.addOutput(output)
        scrollToBottom()
    }

    /**
     * Switches the AI provider.
     */
    private fun handleProviderSwitch(name: String) {
        val ai = aiService ?: MiniMaxService(this@MainActivity).also { aiService = it }
        val providers = ai.getProviders()
        if (name.lowercase() in providers.map { it.lowercase() }) {
            ai.setProvider(name)
            terminalAdapter.addOutput("Switched to AI provider: $name")
        } else {
            terminalAdapter.addError("Provider not found: $name\nAvailable: ${providers.joinToString(", ")}")
        }
        scrollToBottom()
    }

    /**
     * Lists all available AI providers with a selection marker on the current one.
     */
    private fun showProviders() {
        val ai = aiService ?: MiniMaxService(this@MainActivity).also { aiService = it }
        val current = ai.getCurrentProvider()
        val providers = ai.getProviders()
        val output = buildString {
            appendLine()
            appendLine("╔══════════════════════════════════════════════╗")
            appendLine("║          AI PROVIDERS                       ║")
            appendLine("╠══════════════════════════════════════════════╣")
            providers.forEach { p ->
                val marker = if (p == current) " \u25C9" else " \u25CB"
                appendLine("║$marker $p${" ".repeat(30 - p.length)}║")
            }
            appendLine("╚══════════════════════════════════════════════╝")
            appendLine()
            appendLine("Usage: ?? --provider <name>")
            appendLine("       ?? --providers (this list)")
        }
        terminalAdapter.addOutput(output)
        scrollToBottom()
    }

    /**
     * Displays AI usage statistics and estimated cost.
     */
    private fun showUsageStats() {
        val ai = aiService ?: MiniMaxService(this@MainActivity).also { aiService = it }
        val stats = ai.getUsageStats()
        val output = buildString {
            appendLine()
            appendLine("╔══════════════════════════════════════════════╗")
            appendLine("║          AI USAGE STATS                     ║")
            appendLine("╠══════════════════════════════════════════════╣")
            appendLine("║  Queries:    ${stats.totalQueries.toString().padEnd(28)}║")
            appendLine("║  Input Tok:  ${stats.totalInputTokens.toString().padEnd(28)}║")
            appendLine("║  Output Tok: ${stats.totalOutputTokens.toString().padEnd(28)}║")
            appendLine("║  Est. Cost:  \$${"%.6f".format(stats.totalCost).padEnd(26)}║")
            appendLine("╚══════════════════════════════════════════════╝")
        }
        terminalAdapter.addOutput(output)
        scrollToBottom()
    }

    /**
     * Shows which providers have API keys configured (with masked values).
     */
    private fun showApiKeys() {
        val ai = aiService ?: MiniMaxService(this@MainActivity).also { aiService = it }
        val output = buildString {
            appendLine()
            appendLine("╔══════════════════════════════════════════════╗")
            appendLine("║          AI API KEYS                        ║")
            appendLine("╠══════════════════════════════════════════════╣")
            ai.getProviders().forEach { p ->
                val k = ai.getApiKey(p)
                val masked = if (k.isNotBlank()) {
                    if (k.length > 8) "${k.take(4)}...${k.takeLast(4)}"
                    else "****${k.takeLast(2)}"
                } else "— not set —"
                appendLine("║  ${p.padEnd(14)} $masked║")
            }
            appendLine("╚══════════════════════════════════════════════╝")
            appendLine()
            appendLine("Usage: ?? --key <provider> <api_key>")
        }
        terminalAdapter.addOutput(output)
        scrollToBottom()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  App Launch
    // ═══════════════════════════════════════════════════════════════════════════

    private fun handleAppLaunch(appName: String) {
        terminalAdapter.addOutput("App launch: $appName")
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                val handler = commandProcessor.getHandler("launch")
                if (handler is AppLauncherCommand) {
                    try {
                        val output = handler.execute(this@MainActivity, listOf(appName))
                        ProcessingResult.Success(output)
                    } catch (e: Exception) {
                        ProcessingResult.Error("App not found: $appName — ${e.message}")
                    }
                } else {
                    ProcessingResult.Error("App launcher not available")
                }
            }
            when (result) {
                is ProcessingResult.Success -> terminalAdapter.addOutput(result.output)
                is ProcessingResult.Error -> terminalAdapter.addError(result.message)
                else -> {}
            }
            scrollToBottom()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Utilities
    // ═══════════════════════════════════════════════════════════════════════════

    private fun scrollToBottom() {
        recyclerView.post {
            recyclerView.scrollToPosition(terminalAdapter.itemCount - 1)
        }
    }

    // ── Autocomplete Suggestions ──────────────────────────────────────────────

    private fun showSuggestions(suggestions: List<String>) {
        dismissSuggestions()
        if (suggestions.isEmpty()) return
        lastSuggestions = suggestions

        val listView = ListView(this)
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, suggestions)
        listView.setOnItemClickListener { _, _, position, _ ->
            inputField.setText(suggestions[position])
            inputField.setSelection(inputField.text?.length ?: 0)
            dismissSuggestions()
            // Trigger execution if unambiguous
            if (suggestions.size == 1) {
                executeCommand()
            }
        }

        suggestionsPopup = PopupWindow(
            listView,
            inputField.width,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            showAsDropDown(inputField, 0, 0)
        }
    }

    private fun dismissSuggestions() {
        suggestionsPopup?.dismiss()
        suggestionsPopup = null
    }

    // ── Session Persistence ───────────────────────────────────────────────────

    private fun saveSession() {
        val json = TerminalAdapter.itemsToJson(terminalAdapter.getItems())
        getSharedPreferences(PREFS_SESSION, MODE_PRIVATE)
            .edit()
            .putString(KEY_SESSION_OUTPUT, json)
            .apply()
    }

    private fun restoreSession() {
        val json = getSharedPreferences(PREFS_SESSION, MODE_PRIVATE)
            .getString(KEY_SESSION_OUTPUT, null) ?: return
        TerminalAdapter.itemsFromJson(json)?.forEach { terminalAdapter.addItem(it) }
    }
}
