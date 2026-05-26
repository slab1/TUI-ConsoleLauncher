package tui.smartlauncher.automation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import tui.smartlauncher.core.CommandHandler
import java.io.File

/**
 * Automation Command - Integration with Tasker and Termux
 * Enables executing automation tasks and scripts from T-UI
 */
class AutomationCommand : CommandHandler {

    companion object {
        private const val TAG = "AutomationCommand"
        private const val TASKER_PACKAGE = "net.dinglisch.android.taskerm"
        private const val TERMUX_PACKAGE = "com.termux"
        private const val TERMUX_TASKER_PACKAGE = "com.termux.tasker"
    }

    override fun getName(): String = "auto"

    override fun getAliases(): List<String> = listOf("automation", "task", "run", "exec")

    override fun getDescription(): String = "Automation integration with Tasker and Termux"

    override fun getUsage(): String = """
        ╔══════════════════════════════════════════════════════╗
        ║                 AUTOMATION COMMANDS                   ║
        ╠══════════════════════════════════════════════════════╣
        ║  auto tasker                 - List Tasker tasks     ║
        ║  auto task <name>            - Run Tasker task       ║
        ║  auto termux <command>       - Run Termux command    ║
        ║  auto script                 - List saved scripts    ║
        ║  auto script <name>          - Run saved script      ║
        ║  auto script create <name>   - Create a script       ║
        ║  auto script edit <name>     - Edit a script         ║
        ║  auto script rm <name>       - Delete a script       ║
        ║  auto script cat <name>      - Show script contents  ║
        ║  auto apps                   - List automation apps  ║
        ║  auto broadcast <action>     - Send broadcast        ║
        ║  auto intent <action>        - Send intent           ║
        ║  auto settings <setting>     - Change setting        ║
        ╚══════════════════════════════════════════════════════╝
    """.trimIndent()

    override fun execute(context: Context, args: List<String>): String {
        if (args.isEmpty() || args[0] == "--help" || args[0] == "-h") {
            return getUsage()
        }

        val command = args[0].lowercase()
        val parameters = args.drop(1)

        return when (command) {
            "tasker", "task", "tasks" -> handleTasker(context, parameters)
            "termux" -> handleTermux(context, parameters)
            "script", "scripts" -> handleScript(context, parameters)
            "apps", "list" -> listAutomationApps(context)
            "broadcast" -> sendBroadcast(context, parameters)
            "intent" -> sendIntent(context, parameters)
            "settings" -> handleSettings(context, parameters)
            "wifi" -> toggleWifi(context, parameters)
            "bluetooth" -> toggleBluetooth(context, parameters)
            else -> "Unknown automation command: $command\n${getUsage()}"
        }
    }

    /**
     * Handles Tasker integration
     */
    private fun handleTasker(context: Context, args: List<String>): String {
        if (!isPackageInstalled(context, TASKER_PACKAGE)) {
            return "Tasker is not installed.\nInstall Tasker from Play Store to use this feature."
        }

        if (args.isEmpty()) {
            return listTaskerTasks(context)
        }

        val taskName = args.joinToString(" ")
        return executeTaskerTask(context, taskName)
    }

    /**
     * Lists available Tasker tasks
     */
    private fun listTaskerTasks(context: Context): String {
        // Try to get tasks via Tasker broadcast
        val intent = Intent("net.dinglisch.android.taskerm.GET_TASKS")
        intent.setPackage(TASKER_PACKAGE)

        return try {
            context.sendOrderedBroadcast(intent, null, object : android.content.BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    // Tasker will respond via the result bundle
                }
            }, null, android.app.Activity.RESULT_OK, null, null)

            // For now, show manual approach
            buildString {
                appendLine()
                appendLine("Tasker Tasks")
                appendLine("─".repeat(50))
                appendLine("To list tasks, use Tasker's built-in HTTP server.")
                appendLine()
                appendLine("Usage: auto task <task_name>")
                appendLine()
                appendLine("Example: auto task Morning Routine")
                appendLine()
                appendLine("Note: Tasker must have 'Allow External Access' enabled")
                appendLine("      in: Tasker > Menu > Preferences > Misc")
            }
        } catch (e: Exception) {
            "Error listing tasks: ${e.message}"
        }
    }

    /**
     * Executes a Tasker task
     */
    private fun executeTaskerTask(context: Context, taskName: String): String {
        return try {
            val intent = Intent("net.dinglisch.android.taskerm.TASK")
            intent.setPackage(TASKER_PACKAGE)
            intent.putExtra("task_name", taskName)

            context.sendBroadcast(intent)

            buildString {
                appendLine()
                appendLine("Tasker Task Execution")
                appendLine("─".repeat(50))
                appendLine("Task: $taskName")
                appendLine()
                appendLine("✓ Task execution signal sent")
                appendLine()
                appendLine("Note: Check Tasker for actual execution status")
            }
        } catch (e: Exception) {
            "Failed to execute task: ${e.message}"
        }
    }

    /**
     * Handles Termux execution
     */
    private fun handleTermux(context: Context, args: List<String>): String {
        if (!isPackageInstalled(context, TERMUX_PACKAGE)) {
            return buildString {
                appendLine("Termux is not installed.")
                appendLine()
                appendLine("Termux provides a Linux environment on Android.")
                appendLine("Install it for full command execution support.")
                appendLine()
                appendLine("Install URL:")
                appendLine("  https://f-droid.org/packages/com.termux/")
            }
        }

        if (args.isEmpty()) {
            return showTermuxHelp()
        }

        return executeTermuxCommand(context, args.joinToString(" "))
    }

    /**
     * Shows Termux help
     */
    private fun showTermuxHelp(): String {
        return buildString {
            appendLine()
            appendLine("Termux Integration")
            appendLine("─".repeat(50))
            appendLine("Usage: auto termux <command>")
            appendLine()
            appendLine("Examples:")
            appendLine("  auto termux pkg update")
            appendLine("  auto termux python script.py")
            appendLine("  auto termux ls -la")
            appendLine()
            appendLine("For complex scripts, use 'auto script' instead.")
        }
    }

    /**
     * Executes a Termux command
     */
    private fun executeTermuxCommand(context: Context, command: String): String {
        return try {
            // Try to use Termux-Tasker if available
            if (isPackageInstalled(context, TERMUX_TASKER_PACKAGE)) {
                val intent = Intent("com.termux.tasker.EXECUTE")
                intent.setPackage(TERMUX_TASKER_PACKAGE)
                intent.putExtra("com.termux.tasker.COMMAND", command)
                context.sendBroadcast(intent)

                return "Command sent to Termux: $command"
            }

            // Fallback: open Termux with command
            val intent = context.packageManager.getLaunchIntentForPackage(TERMUX_PACKAGE)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)

                buildString {
                    appendLine("Opened Termux.")
                    appendLine("Run this command manually:")
                    appendLine()
                    appendLine("  $command")
                }
            } else {
                "Could not open Termux."
            }
        } catch (e: Exception) {
            "Failed to execute Termux command: ${e.message}"
        }
    }

    // ──────────────────────────────────────────────
    //  Script Management
    // ──────────────────────────────────────────────

    /**
     * Returns (and creates if needed) the scripts directory.
     */
    private fun getScriptsDir(context: Context): File {
        val dir = File(context.filesDir, "scripts")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Handles saved scripts — CRUD + execution.
     *
     * Subcommands: create, edit, rm, cat
     * Otherwise the first argument is treated as a script name to run.
     */
    private fun handleScript(context: Context, args: List<String>): String {
        if (args.isEmpty()) {
            return listScripts(context)
        }

        val command = args[0].lowercase()
        val scriptsDir = getScriptsDir(context)

        return when (command) {
            "create" -> {
                if (args.size < 2) return "Usage: auto script create <name>"
                createScript(scriptsDir, args[1])
            }
            "edit" -> {
                if (args.size < 2) return "Usage: auto script edit <name>"
                editScript(context, scriptsDir, args[1])
            }
            "rm", "delete" -> {
                if (args.size < 2) return "Usage: auto script rm <name>"
                deleteScript(scriptsDir, args[1])
            }
            "cat", "show" -> {
                if (args.size < 2) return "Usage: auto script cat <name>"
                showScript(scriptsDir, args[1])
            }
            else -> runScript(context, scriptsDir, command, args.drop(1))
        }
    }

    /**
     * Lists saved scripts in a formatted box.
     */
    private fun listScripts(context: Context): String {
        val scriptsDir = getScriptsDir(context)
        val scriptFiles = scriptsDir.listFiles { file -> file.extension == "sh" }
            ?.sortedBy { it.name } ?: emptyList()

        val boxWidth = 50
        val innerWidth = boxWidth - 4 // space inside the ║  ...  ║ borders

        return buildString {
            appendLine()
            val title = " SCRIPTS (${scriptFiles.size}) "
            val paddedTitle = title.padStart((innerWidth + title.length) / 2).padEnd(innerWidth)

            appendLine("╔${"═".repeat(boxWidth - 2)}╗")
            appendLine("║$paddedTitle║")
            appendLine("╠${"═".repeat(boxWidth - 2)}╣")

            if (scriptFiles.isEmpty()) {
                appendLine("║  No scripts found.                                    ║")
                appendLine("║  Use: auto script create <name>                       ║")
            } else {
                scriptFiles.forEach { file ->
                    val name = file.nameWithoutExtension
                    val desc = getScriptDescription(file)
                    val line = if (desc.isNotEmpty()) "  $name  -> $desc" else "  $name"
                    val display = if (line.length <= innerWidth) {
                        line.padEnd(innerWidth)
                    } else {
                        line.take(innerWidth - 3) + "..."
                    }
                    appendLine("║ $display ║")
                }
            }
            appendLine("╚${"═".repeat(boxWidth - 2)}╝")
        }
    }

    /**
     * Extracts a human-readable description from the script's first line
     * if it starts with "# ".
     */
    private fun getScriptDescription(scriptFile: File): String {
        return try {
            val firstLine = scriptFile.useLines { it.firstOrNull() } ?: return ""
            if (firstLine.startsWith("# ")) {
                firstLine.removePrefix("# ").trim()
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Creates an empty script file with a shebang template.
     */
    private fun createScript(scriptsDir: File, name: String): String {
        val scriptFile = File(scriptsDir, "$name.sh")
        if (scriptFile.exists()) {
            return "Script '$name' already exists.\nUse 'auto script edit $name' to modify it."
        }

        return try {
            scriptFile.writeText(
                """#!/system/bin/sh
# ${name.replaceFirstChar { it.uppercaseChar() }} script
# Add your commands below:

"""
            )
            buildString {
                appendLine()
                appendLine("✓ Script '$name' created.")
                appendLine()
                appendLine("Edit it with:   auto script edit $name")
                appendLine("Run it with:    auto script $name")
                appendLine("View it with:   auto script cat $name")
            }
        } catch (e: Exception) {
            "Failed to create script: ${e.message}"
        }
    }

    /**
     * Opens a script for editing — prefers Termux with nano, otherwise shows
     * the file path and current contents.
     */
    private fun editScript(context: Context, scriptsDir: File, name: String): String {
        val scriptFile = File(scriptsDir, "$name.sh")
        if (!scriptFile.exists()) {
            return buildString {
                appendLine("Script '$name' doesn't exist.")
                appendLine("Create it first: auto script create $name")
            }
        }

        return try {
            // Prefer Termux with nano if available
            if (isPackageInstalled(context, TERMUX_PACKAGE)) {
                val intent = context.packageManager.getLaunchIntentForPackage(TERMUX_PACKAGE)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.putExtra("com.termux.execute_cmd", "nano '${scriptFile.absolutePath}'")
                    context.startActivity(intent)
                    return buildString {
                        appendLine("Opened Termux with nano for editing '$name'.")
                        appendLine()
                        appendLine("File: ${scriptFile.absolutePath}")
                        appendLine("After saving, run: auto script $name")
                    }
                }
            }

            // Fallback: show the current content and file path
            val content = scriptFile.readText()
            buildString {
                appendLine()
                appendLine("Script: $name")
                appendLine("─".repeat(50))
                appendLine(content.trimEnd())
                appendLine("─".repeat(50))
                appendLine("Edit the file at:")
                appendLine("  ${scriptFile.absolutePath}")
                appendLine()
                appendLine("Or open in Termux:")
                appendLine("  auto termux nano ${scriptFile.absolutePath}")
            }
        } catch (e: Exception) {
            "Failed to edit script: ${e.message}"
        }
    }

    /**
     * Deletes a saved script.
     */
    private fun deleteScript(scriptsDir: File, name: String): String {
        val scriptFile = File(scriptsDir, "$name.sh")
        if (!scriptFile.exists()) {
            return "Script '$name' not found."
        }

        return try {
            scriptFile.delete()
            buildString {
                appendLine()
                appendLine("✓ Script '$name' deleted.")
            }
        } catch (e: Exception) {
            "Failed to delete script: ${e.message}"
        }
    }

    /**
     * Displays the contents of a saved script.
     */
    private fun showScript(scriptsDir: File, name: String): String {
        val scriptFile = File(scriptsDir, "$name.sh")
        if (!scriptFile.exists()) {
            return "Script '$name' not found."
        }

        return try {
            val content = scriptFile.readText()
            buildString {
                appendLine()
                appendLine("Script: $name.sh")
                appendLine("Location: ${scriptFile.absolutePath}")
                appendLine("─".repeat(50))
                appendLine(content.trimEnd())
            }
        } catch (e: Exception) {
            "Failed to read script: ${e.message}"
        }
    }

    /**
     * Runs a saved script.
     *
     * Prefers executing via Termux (if installed); otherwise falls back to a
     * direct `Runtime.exec()` call.
     */
    private fun runScript(
        context: Context,
        scriptsDir: File,
        scriptName: String,
        scriptArgs: List<String>
    ): String {
        val scriptFile = File(scriptsDir, "$scriptName.sh")

        if (!scriptFile.exists()) {
            return buildString {
                appendLine("Script '$scriptName' not found.")
                appendLine("Use 'auto script' to list available scripts.")
            }
        }

        // Prefer Termux execution if available
        if (isPackageInstalled(context, TERMUX_PACKAGE)) {
            val termuxResult = executeScriptViaTermux(context, scriptFile, scriptArgs)
            // If Termux execution didn't fall back, return its result
            if (!termuxResult.startsWith("Falling back")) {
                return termuxResult
            }
        }

        return executeScriptDirect(scriptFile, scriptArgs)
    }

    /**
     * Executes a script file directly using `Runtime.getRuntime().exec()`.
     * The script is run via `sh` and both stdout and stderr are captured.
     */
    private fun executeScriptDirect(scriptFile: File, args: List<String>): String {
        val output = StringBuilder()
        try {
            val cmd = mutableListOf("sh", scriptFile.absolutePath)
            cmd.addAll(args)

            val process = Runtime.getRuntime().exec(cmd.toTypedArray())

            // Read stdout
            process.inputStream.bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.appendLine(line)
                }
            }

            // Read stderr
            process.errorStream.bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.appendLine(line)
                }
            }

            val exitCode = process.waitFor()
            if (output.isEmpty() && exitCode == 0) {
                output.appendLine("Script executed successfully (exit code: $exitCode)")
            } else if (exitCode != 0) {
                output.appendLine("(exit code: $exitCode)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Script execution error", e)
            output.appendLine("Error executing script: ${e.message}")
        }
        return output.toString().trimEnd()
    }

    /**
     * Executes a script via the Termux bridge.
     *
     * Tries Termux-Tasker first (fire-and-forget), then falls back to opening
     * Termux with the script command.  Returns a string that starts with
     * "Falling back" when direct execution should be attempted instead.
     */
    private fun executeScriptViaTermux(
        context: Context,
        scriptFile: File,
        args: List<String>
    ): String {
        return try {
            // Prefer Termux-Tasker bridge (broadcast-based execution)
            if (isPackageInstalled(context, TERMUX_TASKER_PACKAGE)) {
                val command = buildString {
                    append("sh '${scriptFile.absolutePath}'")
                    if (args.isNotEmpty()) {
                        append(" ")
                        append(args.joinToString(" "))
                    }
                }
                val intent = Intent("com.termux.tasker.EXECUTE")
                intent.setPackage(TERMUX_TASKER_PACKAGE)
                intent.putExtra("com.termux.tasker.COMMAND", command)
                context.sendBroadcast(intent)

                return buildString {
                    appendLine("Script sent to Termux for execution: ${scriptFile.name}")
                    appendLine()
                    appendLine("Command: $command")
                    appendLine()
                    appendLine("Check Termux for output.")
                }
            }

            // Fallback: open Termux with the script command
            val intent = context.packageManager.getLaunchIntentForPackage(TERMUX_PACKAGE)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra(
                    "com.termux.execute_cmd",
                    "sh '${scriptFile.absolutePath}'"
                )
                context.startActivity(intent)

                buildString {
                    appendLine("Opened Termux to run script: ${scriptFile.name}")
                    appendLine()
                    appendLine("Script path: ${scriptFile.absolutePath}")
                    appendLine("Args: ${args.joinToString(" ")}")
                }
            } else {
                "Falling back to direct execution — could not open Termux."
            }
        } catch (e: Exception) {
            "Falling back to direct execution — Termux error: ${e.message}"
        }
    }

    // ──────────────────────────────────────────────
    //  App Listing & Utilities
    // ──────────────────────────────────────────────

    /**
     * Lists installed automation apps
     */
    private fun listAutomationApps(context: Context): String {
        val automationApps = listOf(
            TASKER_PACKAGE to "Tasker",
            TERMUX_PACKAGE to "Termux",
            TERMUX_TASKER_PACKAGE to "Termux-Tasker",
            "com.ifttt" to "IFTTT",
            "com.llamalab.automate" to "Automate",
            "com.buzzzapp.autoinput" to "AutoInput"
        )

        val builder = StringBuilder()
        builder.appendLine()
        builder.appendLine("Automation Apps")
        builder.appendLine("═".repeat(50))

        automationApps.forEach { (packageName, appName) ->
            val installed = isPackageInstalled(context, packageName)
            val status = if (installed) "✓ Installed" else "✗ Not installed"
            builder.appendLine(String.format("  %-20s %s", appName, status))

            if (installed && packageName == TASKER_PACKAGE) {
                builder.appendLine("     → Use 'auto task <name>' to run tasks")
            }
            if (installed && packageName == TERMUX_PACKAGE) {
                builder.appendLine("     → Use 'auto termux <command>' to run commands")
            }
        }

        return builder.toString()
    }

    /**
     * Sends a custom broadcast
     */
    private fun sendBroadcast(context: Context, args: List<String>): String {
        if (args.isEmpty()) {
            return "Usage: auto broadcast <action> [extras]"
        }

        val action = args[0]

        return try {
            val intent = Intent(action)
            args.drop(1).forEach { extra ->
                val parts = extra.split("=", limit = 2)
                if (parts.size == 2) {
                    intent.putExtra(parts[0], parts[1])
                }
            }

            context.sendBroadcast(intent)

            buildString {
                appendLine()
                appendLine("Broadcast Sent")
                appendLine("─".repeat(50))
                appendLine("Action: $action")
                appendLine("Extras: ${args.drop(1).joinToString(", ")}")
            }
        } catch (e: Exception) {
            "Failed to send broadcast: ${e.message}"
        }
    }

    /**
     * Sends an intent to launch an activity
     */
    private fun sendIntent(context: Context, args: List<String>): String {
        if (args.isEmpty()) {
            return "Usage: auto intent <action> [uri]"
        }

        val action = args[0]
        val uri = args.getOrNull(1)

        return try {
            val intent = if (uri != null) {
                Intent(action, Uri.parse(uri))
            } else {
                Intent(action)
            }

            context.startActivity(intent)

            buildString {
                appendLine()
                appendLine("Intent Sent")
                appendLine("─".repeat(50))
                appendLine("Action: $action")
                if (uri != null) appendLine("URI: $uri")
            }
        } catch (e: Exception) {
            "Failed to send intent: ${e.message}"
        }
    }

    /**
     * Handles quick settings changes
     */
    private fun handleSettings(context: Context, args: List<String>): String {
        if (args.isEmpty()) {
            return buildString {
                appendLine("Quick Settings")
                appendLine("─".repeat(50))
                appendLine("Usage: auto settings <setting>")
                appendLine()
                appendLine("Available settings:")
                appendLine("  wifi on/off")
                appendLine("  bluetooth on/off")
                appendLine("  sync on/off")
                appendLine("  airplane on/off")
            }
        }

        val setting = args[0].lowercase()
        val action = args.getOrNull(1)?.lowercase() ?: "toggle"

        return when {
            setting.startsWith("wifi") -> toggleWifi(context, listOf(action))
            setting.startsWith("bluetooth") -> toggleBluetooth(context, listOf(action))
            setting.startsWith("sync") -> toggleSync(context, action)
            setting.startsWith("airplane") -> toggleAirplane(context, action)
            else -> "Unknown setting: $setting"
        }
    }

    private fun toggleWifi(context: Context, args: List<String>): String {
        val action = when (args.firstOrNull()?.lowercase()) {
            "on", "enable", "true" -> "enable"
            "off", "disable", "false" -> "disable"
            else -> null // Toggle
        }

        // On Android 10+ WiFi cannot be toggled programmatically without system permissions
        // Open WiFi settings for manual control
        return try {
            if (action == null) {
                // Toggle: open WiFi settings
                context.startActivity(
                    Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
                buildString {
                    appendLine()
                    appendLine("WiFi Settings")
                    appendLine("─".repeat(50))
                    appendLine("Opened WiFi settings panel for manual toggle.")
                }
            } else {
                buildString {
                    appendLine()
                    appendLine("WiFi: $action")
                    appendLine("─".repeat(50))
                    appendLine("To ${action}WiFi on Android 10+, please use system quick settings.")
                    appendLine("Or open settings: auto wifi toggle")
                }
            }
        } catch (e: Exception) {
            "Failed to open WiFi settings: ${e.message}"
        }
    }

    private fun toggleBluetooth(context: Context, args: List<String>): String {
        val action = when (args.firstOrNull()?.lowercase()) {
            "on", "enable" -> "enable"
            "off", "disable" -> "disable"
            else -> null
        }

        // On modern Android, Bluetooth cannot be toggled programmatically without system permissions
        return try {
            if (action == null) {
                context.startActivity(
                    Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
                buildString {
                    appendLine()
                    appendLine("Bluetooth Settings")
                    appendLine("─".repeat(50))
                    appendLine("Opened Bluetooth settings panel for manual toggle.")
                }
            } else {
                buildString {
                    appendLine()
                    appendLine("Bluetooth: $action")
                    appendLine("─".repeat(50))
                    appendLine("To ${action}Bluetooth on Android 10+, please use system quick settings.")
                    appendLine("Or open settings: auto bluetooth toggle")
                }
            }
        } catch (e: Exception) {
            "Failed to open Bluetooth settings: ${e.message}"
        }
    }

    private fun toggleSync(context: Context, action: String): String {
        return "Sync toggle requires Settings Secure access.\nOpening settings..."
    }

    private fun toggleAirplane(context: Context, action: String): String {
        return "Airplane mode cannot be toggled programmatically.\nUse system quick settings."
    }

    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
