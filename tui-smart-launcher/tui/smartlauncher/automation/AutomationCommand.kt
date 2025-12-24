package tui.smartlauncher.automation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import tui.smartlauncher.core.CommandHandler

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
        ║  auto script <name>          - Run saved script      ║
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

    /**
     * Handles saved scripts
     */
    private fun handleScript(context: Context, args: List<String>): String {
        if (args.isEmpty()) {
            return listScripts()
        }

        val scriptName = args[0]
        return runScript(context, scriptName, args.drop(1))
    }

    /**
     * Lists available scripts
     */
    private fun listScripts(): String {
        return buildString {
            appendLine()
            appendLine("Saved Scripts")
            appendLine("─".repeat(50))
            appendLine("No scripts configured yet.")
            appendLine()
            appendLine("To add a script:")
            appendLine("  1. Create a script file in scripts/")
            appendLine("  2. Register it in automation config")
            appendLine()
            appendLine("Script execution: auto script <name>")
        }
    }

    /**
     * Runs a saved script
     */
    private fun runScript(context: Context, scriptName: String, args: List<String>): String {
        // Script execution would be implemented here
        // For now, return placeholder
        return buildString {
            appendLine()
            appendLine("Script: $scriptName")
            appendLine("─".repeat(50))
            appendLine("Args: ${args.joinToString(" ")}")
            appendLine()
            appendLine("Script execution not yet configured.")
        }
    }

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
        val enable = when (args.firstOrNull()?.lowercase()) {
            "on", "enable", "true" -> true
            "off", "disable", "false" -> false
            else -> null // Toggle
        }

        return try {
            val intent = Intent("android.net.wifi.WIFI_SETTINGS")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)

            buildString {
                appendLine()
                appendLine("WiFi Settings")
                appendLine("─".repeat(50))
                appendLine("Opened WiFi settings panel.")
            }
        } catch (e: Exception) {
            "Failed to open WiFi settings: ${e.message}"
        }
    }

    private fun toggleBluetooth(context: Context, args: List<String>): String {
        val enable = when (args.firstOrNull()?.lowercase()) {
            "on", "enable" -> true
            "off", "disable" -> false
            else -> null
        }

        return try {
            val intent = Intent("android.bluetooth.settings")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)

            buildString {
                appendLine()
                appendLine("Bluetooth Settings")
                appendLine("─".repeat(50))
                appendLine("Opened Bluetooth settings panel.")
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
