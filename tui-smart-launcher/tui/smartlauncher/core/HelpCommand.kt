package tui.smartlauncher.core

import android.content.Context

/**
 * Help Command - Shows comprehensive command reference for the smart launcher
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
            appendLine("  ?? !!             Repeat last AI query")
            appendLine("  ?? !              Show cached AI response")
            appendLine("  ?? --clear        Reset conversation memory")
            appendLine("  ?? --persona <n>  Switch AI persona")
            appendLine("  ?? --personas     List personas")
            appendLine("  ?? --provider <p> Switch AI provider")
            appendLine("  ?? --providers    List providers")
            appendLine("  ?? --key <p> <k>  Set API key for a provider")
            appendLine("  ?? --keys         Show configured API keys")
            appendLine("  ?? --usage        Show AI usage stats")
            appendLine("  summarize <text>  Summarize with AI")
            appendLine("  translate <text>  Translate with AI")
            appendLine("  fix <code>        Debug with AI")
            appendLine("  explain <topic>   Explain with AI")
            appendLine()
            appendLine("For detailed help on a command, type: help <command>")
            appendLine()
            appendLine("╚═══════════════════════════════════════════════════════════════════╝")
        }
    }
}
