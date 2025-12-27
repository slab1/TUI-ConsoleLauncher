package ohi.andre.consolelauncher.commands.smartlauncher.automation;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;

import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;

/**
 * Automation Command - Integration with Tasker and Termux
 * Enables executing automation tasks and scripts from T-UI
 */
public class AutomationCommand implements CommandAbstraction {

    private static final String TAG = "AutomationCommand";
    private static final String TASKER_PACKAGE = "net.dinglisch.android.taskerm";
    private static final String TERMUX_PACKAGE = "com.termux";
    private static final String TERMUX_TASKER_PACKAGE = "com.termux.tasker";

    @Override
    public String exec(ExecutePack pack) throws Exception {
        Context context = pack.context;
        
        ArrayList<String> argsList = new ArrayList<>();
        for (Object arg : pack.args) {
            if (arg != null) {
                argsList.add(arg.toString());
            }
        }

        if (argsList.isEmpty() || "--help".equals(argsList.get(0)) || "-h".equals(argsList.get(0))) {
            return getUsage();
        }

        String command = argsList.get(0).toLowerCase();
        ArrayList<String> parameters = new ArrayList<>(argsList.subList(1, argsList.size()));

        switch (command) {
            case "tasker":
            case "task":
            case "tasks":
                return handleTasker(context, parameters);
            case "termux":
                return handleTermux(context, parameters);
            case "script":
            case "scripts":
                return handleScript(context, parameters);
            case "apps":
            case "list":
                return listAutomationApps(context);
            case "broadcast":
                return sendBroadcast(context, parameters);
            case "intent":
                return sendIntent(context, parameters);
            case "settings":
                return handleSettings(context, parameters);
            case "wifi":
                return toggleWifi(context, parameters);
            case "bluetooth":
                return toggleBluetooth(context, parameters);
            default:
                return "Unknown automation command: " + command + "\n" + getUsage();
        }
    }

    @Override
    public int[] argType() {
        return new int[]{CommandAbstraction.COMMAND};
    }

    @Override
    public int priority() {
        return 5;
    }

    @Override
    public int helpRes() {
        return 0;
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int indexNotFound) {
        return "Missing argument at position " + indexNotFound + "\n" + getUsage();
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        return "Not enough arguments (" + nArgs + " required)\n" + getUsage();
    }

    private String getUsage() {
        return "\n╔══════════════════════════════════════════════════════╗\n" +
               "║                 AUTOMATION COMMANDS                   ║\n" +
               "╠══════════════════════════════════════════════════════╣\n" +
               "║  auto tasker                 - List Tasker tasks     ║\n" +
               "║  auto task <name>            - Run Tasker task       ║\n" +
               "║  auto termux <command>       - Run Termux command    ║\n" +
               "║  auto script <name>          - Run saved script      ║\n" +
               "║  auto apps                   - List automation apps  ║\n" +
               "║  auto broadcast <action>     - Send broadcast        ║\n" +
               "║  auto intent <action>        - Send intent           ║\n" +
               "║  auto settings <setting>     - Change setting        ║\n" +
               "╚══════════════════════════════════════════════════════╝\n";
    }

    /**
     * Handles Tasker integration
     */
    private String handleTasker(Context context, ArrayList<String> args) {
        if (!isPackageInstalled(context, TASKER_PACKAGE)) {
            return "Tasker is not installed.\nInstall Tasker from Play Store to use this feature.";
        }

        if (args.isEmpty()) {
            return listTaskerTasks(context);
        }

        String taskName = listToString(args);
        return executeTaskerTask(context, taskName);
    }

    /**
     * Lists available Tasker tasks
     */
    private String listTaskerTasks(Context context) {
        // Try to get tasks via Tasker broadcast
        Intent intent = new Intent("net.dinglisch.android.taskerm.GET_TASKS");
        intent.setPackage(TASKER_PACKAGE);

        try {
            context.sendOrderedBroadcast(intent, null, null, null, android.app.Activity.RESULT_OK, null, null);

            // For now, show manual approach
            StringBuilder result = new StringBuilder();
            result.append("\n");
            result.append("Tasker Tasks\n");
            result.append("─".repeat(50)).append("\n");
            result.append("To list tasks, use Tasker's built-in HTTP server.\n");
            result.append("\n");
            result.append("Usage: auto task <task_name>\n");
            result.append("\n");
            result.append("Example: auto task Morning Routine\n");
            result.append("\n");
            result.append("Note: Tasker must have 'Allow External Access' enabled\n");
            result.append("      in: Tasker > Menu > Preferences > Misc\n");

            return result.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error listing tasks", e);
            return "Error listing tasks: " + e.getMessage();
        }
    }

    /**
     * Executes a Tasker task
     */
    private String executeTaskerTask(Context context, String taskName) {
        try {
            Intent intent = new Intent("net.dinglisch.android.taskerm.TASK");
            intent.setPackage(TASKER_PACKAGE);
            intent.putExtra("task_name", taskName);

            context.sendBroadcast(intent);

            StringBuilder result = new StringBuilder();
            result.append("\n");
            result.append("Tasker Task Execution\n");
            result.append("─".repeat(50)).append("\n");
            result.append("Task: ").append(taskName).append("\n");
            result.append("\n");
            result.append("✓ Task execution signal sent\n");
            result.append("\n");
            result.append("Note: Check Tasker for actual execution status\n");

            return result.toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to execute task", e);
            return "Failed to execute task: " + e.getMessage();
        }
    }

    /**
     * Handles Termux execution
     */
    private String handleTermux(Context context, ArrayList<String> args) {
        if (!isPackageInstalled(context, TERMUX_PACKAGE)) {
            StringBuilder result = new StringBuilder();
            result.append("Termux is not installed.\n");
            result.append("\n");
            result.append("Termux provides a Linux environment on Android.\n");
            result.append("Install it for full command execution support.\n");
            result.append("\n");
            result.append("Install URL:\n");
            result.append("  https://f-droid.org/packages/com.termux/\n");
            return result.toString();
        }

        if (args.isEmpty()) {
            return showTermuxHelp();
        }

        String command = listToString(args);
        return executeTermuxCommand(context, command);
    }

    /**
     * Shows Termux help
     */
    private String showTermuxHelp() {
        StringBuilder result = new StringBuilder();
        result.append("\n");
        result.append("Termux Integration\n");
        result.append("─".repeat(50)).append("\n");
        result.append("Usage: auto termux <command>\n");
        result.append("\n");
        result.append("Examples:\n");
        result.append("  auto termux pkg update\n");
        result.append("  auto termux python script.py\n");
        result.append("  auto termux ls -la\n");
        result.append("\n");
        result.append("For complex scripts, use 'auto script' instead.\n");

        return result.toString();
    }

    /**
     * Executes a Termux command
     */
    private String executeTermuxCommand(Context context, String command) {
        try {
            // Try to use Termux-Tasker if available
            if (isPackageInstalled(context, TERMUX_TASKER_PACKAGE)) {
                Intent intent = new Intent("com.termux.tasker.EXECUTE");
                intent.setPackage(TERMUX_TASKER_PACKAGE);
                intent.putExtra("com.termux.tasker.COMMAND", command);
                context.sendBroadcast(intent);

                return "Command sent to Termux: " + command;
            }

            // Fallback: open Termux with command
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(TERMUX_PACKAGE);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);

                StringBuilder result = new StringBuilder();
                result.append("Opened Termux.\n");
                result.append("Run this command manually:\n");
                result.append("\n");
                result.append("  ").append(command).append("\n");

                return result.toString();
            } else {
                return "Could not open Termux.";
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to execute Termux command", e);
            return "Failed to execute Termux command: " + e.getMessage();
        }
    }

    /**
     * Handles saved scripts
     */
    private String handleScript(Context context, ArrayList<String> args) {
        if (args.isEmpty()) {
            return listScripts();
        }

        String scriptName = args.get(0);
        ArrayList<String> scriptArgs = new ArrayList<>(args.subList(1, args.size()));
        return runScript(context, scriptName, scriptArgs);
    }

    /**
     * Lists available scripts
     */
    private String listScripts() {
        StringBuilder result = new StringBuilder();
        result.append("\n");
        result.append("Saved Scripts\n");
        result.append("─".repeat(50)).append("\n");
        result.append("No scripts configured yet.\n");
        result.append("\n");
        result.append("To add a script:\n");
        result.append("  1. Create a script file in scripts/\n");
        result.append("  2. Register it in automation config\n");
        result.append("\n");
        result.append("Script execution: auto script <name>\n");

        return result.toString();
    }

    /**
     * Runs a saved script
     */
    private String runScript(Context context, String scriptName, ArrayList<String> args) {
        StringBuilder result = new StringBuilder();
        result.append("\n");
        result.append("Script: ").append(scriptName).append("\n");
        result.append("─".repeat(50)).append("\n");
        result.append("Args: ").append(listToString(args)).append("\n");
        result.append("\n");
        result.append("Script execution not yet configured.\n");

        return result.toString();
    }

    /**
     * Lists installed automation apps
     */
    private String listAutomationApps(Context context) {
        String[][] automationApps = {
            {TASKER_PACKAGE, "Tasker"},
            {TERMUX_PACKAGE, "Termux"},
            {TERMUX_TASKER_PACKAGE, "Termux-Tasker"},
            {"com.ifttt", "IFTTT"},
            {"com.llamalab.automate", "Automate"},
            {"com.buzzzapp.autoinput", "AutoInput"}
        };

        StringBuilder builder = new StringBuilder();
        builder.append("\n");
        builder.append("Automation Apps\n");
        builder.append("═".repeat(50)).append("\n");

        for (String[] app : automationApps) {
            String packageName = app[0];
            String appName = app[1];
            boolean installed = isPackageInstalled(context, packageName);
            String status = installed ? "✓ Installed" : "✗ Not installed";
            builder.append(String.format("  %-20s %s\n", appName, status));

            if (installed && TASKER_PACKAGE.equals(packageName)) {
                builder.append("     → Use 'auto task <name>' to run tasks\n");
            }
            if (installed && TERMUX_PACKAGE.equals(packageName)) {
                builder.append("     → Use 'auto termux <command>' to run commands\n");
            }
        }

        return builder.toString();
    }

    /**
     * Sends a custom broadcast
     */
    private String sendBroadcast(Context context, ArrayList<String> args) {
        if (args.isEmpty()) {
            return "Usage: auto broadcast <action> [extras]";
        }

        String action = args.get(0);

        try {
            Intent intent = new Intent(action);
            
            for (int i = 1; i < args.size(); i++) {
                String extra = args.get(i);
                String[] parts = extra.split("=", 2);
                if (parts.length == 2) {
                    intent.putExtra(parts[0], parts[1]);
                }
            }

            context.sendBroadcast(intent);

            StringBuilder result = new StringBuilder();
            result.append("\n");
            result.append("Broadcast Sent\n");
            result.append("─".repeat(50)).append("\n");
            result.append("Action: ").append(action).append("\n");
            ArrayList<String> extras = new ArrayList<>(args.subList(1, args.size()));
            result.append("Extras: ").append(listToString(extras)).append("\n");

            return result.toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to send broadcast", e);
            return "Failed to send broadcast: " + e.getMessage();
        }
    }

    /**
     * Sends an intent to launch an activity
     */
    private String sendIntent(Context context, ArrayList<String> args) {
        if (args.isEmpty()) {
            return "Usage: auto intent <action> [uri]";
        }

        String action = args.get(0);
        String uri = args.size() > 1 ? args.get(1) : null;

        try {
            Intent intent;
            if (uri != null) {
                intent = new Intent(action, Uri.parse(uri));
            } else {
                intent = new Intent(action);
            }

            context.startActivity(intent);

            StringBuilder result = new StringBuilder();
            result.append("\n");
            result.append("Intent Sent\n");
            result.append("─".repeat(50)).append("\n");
            result.append("Action: ").append(action).append("\n");
            if (uri != null) {
                result.append("URI: ").append(uri).append("\n");
            }

            return result.toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to send intent", e);
            return "Failed to send intent: " + e.getMessage();
        }
    }

    /**
     * Handles quick settings changes
     */
    private String handleSettings(Context context, ArrayList<String> args) {
        if (args.isEmpty()) {
            StringBuilder result = new StringBuilder();
            result.append("Quick Settings\n");
            result.append("─".repeat(50)).append("\n");
            result.append("Usage: auto settings <setting>\n");
            result.append("\n");
            result.append("Available settings:\n");
            result.append("  wifi on/off\n");
            result.append("  bluetooth on/off\n");
            result.append("  sync on/off\n");
            result.append("  airplane on/off\n");
            return result.toString();
        }

        String setting = args.get(0).toLowerCase();
        String action = args.size() > 1 ? args.get(1).toLowerCase() : "toggle";

        switch (setting) {
            case "wifi":
                return toggleWifi(context, new ArrayList<>(args.subList(1, args.size())));
            case "bluetooth":
                return toggleBluetooth(context, new ArrayList<>(args.subList(1, args.size())));
            case "sync":
                return toggleSync(context, action);
            case "airplane":
                return toggleAirplane(context, action);
            default:
                return "Unknown setting: " + setting;
        }
    }

    private String toggleWifi(Context context, ArrayList<String> args) {
        String firstArg = !args.isEmpty() ? args.get(0).toLowerCase() : "";
        Boolean enable = null;
        
        if ("on".equals(firstArg) || "enable".equals(firstArg) || "true".equals(firstArg)) {
            enable = true;
        } else if ("off".equals(firstArg) || "disable".equals(firstArg) || "false".equals(firstArg)) {
            enable = false;
        }

        try {
            Intent intent = new Intent("android.net.wifi.WIFI_SETTINGS");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);

            StringBuilder result = new StringBuilder();
            result.append("\n");
            result.append("WiFi Settings\n");
            result.append("─".repeat(50)).append("\n");
            result.append("Opened WiFi settings panel.\n");

            return result.toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to open WiFi settings", e);
            return "Failed to open WiFi settings: " + e.getMessage();
        }
    }

    private String toggleBluetooth(Context context, ArrayList<String> args) {
        String firstArg = !args.isEmpty() ? args.get(0).toLowerCase() : "";
        Boolean enable = null;
        
        if ("on".equals(firstArg) || "enable".equals(firstArg)) {
            enable = true;
        } else if ("off".equals(firstArg) || "disable".equals(firstArg)) {
            enable = false;
        }

        try {
            Intent intent = new Intent("android.bluetooth.settings");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);

            StringBuilder result = new StringBuilder();
            result.append("\n");
            result.append("Bluetooth Settings\n");
            result.append("─".repeat(50)).append("\n");
            result.append("Opened Bluetooth settings panel.\n");

            return result.toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to open Bluetooth settings", e);
            return "Failed to open Bluetooth settings: " + e.getMessage();
        }
    }

    private String toggleSync(Context context, String action) {
        return "Sync toggle requires Settings Secure access.\nOpening settings...";
    }

    private String toggleAirplane(Context context, String action) {
        return "Airplane mode cannot be toggled programmatically.\nUse system quick settings.";
    }

    private boolean isPackageInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private String listToString(ArrayList<String> list) {
        if (list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(" ");
            sb.append(list.get(i));
        }
        return sb.toString();
    }
}