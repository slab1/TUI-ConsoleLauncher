package ohi.andre.consolelauncher.commands.main.raw;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.ai.MiniMaxService;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * AI command — interface to MiniMax AI capabilities.
 * Usage: ai <subcommand> [args]
 * Subcommands:
 *   config <api_key> <group_id>   — Configure AI API credentials
 *   ask|chat <message>            — Send a message to the AI
 *   test                          — Test the API connection
 *   status                        — Show current configuration status
 *   models                        — List available AI models
 *   clear                         — Clear conversation history
 *   help                          — Show this help
 */
public class ai implements CommandAbstraction {

    private static final String TAG = "ai";

    private MiniMaxService aiService;
    private Context context;

    @Override
    public String exec(ExecutePack pack) throws Exception {
        MainPack mainPack = (MainPack) pack;
        context = mainPack.context;

        ArrayList<String> argsList = new ArrayList<>();
        if (pack.args != null) {
            for (Object arg : pack.args) {
                if (arg != null) {
                    argsList.add(arg.toString());
                }
            }
        }

        if (argsList.isEmpty() || "--help".equals(argsList.get(0)) || "-h".equals(argsList.get(0))) {
            return getHelpText();
        }

        String subCommand = argsList.get(0).toLowerCase();
        ArrayList<String> parameters = new ArrayList<>(argsList.subList(1, argsList.size()));

        switch (subCommand) {
            case "config":
            case "setup":
                return handleConfig(parameters);
            case "test":
                return handleTest();
            case "status":
                return handleStatus();
            case "models":
                return handleModels();
            case "clear":
                aiService = null;
                return "Conversation context cleared.";
            case "ask":
            case "chat":
                return handleChat(parameters, true);
            case "help":
                return getHelpText();
            default:
                // Treat as a direct chat message
                return handleChat(argsList, false);
        }
    }

    private String handleConfig(ArrayList<String> args) {
        if (args.size() < 2) {
            return getConfigHelpText();
        }

        String apiKey = args.get(0);
        String groupId = args.get(1);

        try {
            MiniMaxService testService = new MiniMaxService(context, apiKey, groupId);
            MiniMaxService.MiniMaxResponse response = testService.testConnection();

            if (response.success) {
                aiService = testService;
                return "AI configuration successful!\n" + response.message;
            } else {
                return "Configuration failed: " + response.message + "\n\n" +
                       "Check your API key and Group ID.\n" +
                       "Get credentials from: https://api.minimax.chat";
            }
        } catch (Exception e) {
            Log.e(TAG, "Config failed", e);
            return "Configuration error: " + e.getMessage();
        }
    }

    private String handleTest() {
        if (aiService == null) {
            return "AI not configured. Use 'ai config <api_key> <group_id>' first.";
        }

        try {
            MiniMaxService.MiniMaxResponse response = aiService.testConnection();
            if (response.success) {
                return "Connection OK!\n" + response.message;
            } else {
                return "Connection failed: " + response.message;
            }
        } catch (Exception e) {
            Log.e(TAG, "Test failed", e);
            return "Test error: " + e.getMessage();
        }
    }

    private String handleStatus() {
        if (aiService != null) {
            return "AI: Configured and ready.\n" +
                   "Use 'ai ask <message>' to chat, or 'ai test' to verify connection.";
        } else {
            return "AI: Not configured.\n" +
                   "Use 'ai config <api_key> <group_id>' to set up.\n" +
                   "Get credentials from: https://api.minimax.chat";
        }
    }

    private String handleModels() {
        if (aiService != null) {
            StringBuilder sb = new StringBuilder("Available models:\n");
            for (String model : aiService.getAvailableModels()) {
                sb.append("  - ").append(model).append("\n");
            }
            return sb.toString().trim();
        } else {
            return "Default models:\n" +
                   "  - abab6.5-chat (default)\n" +
                   "  - abab6.5s-chat (fast)\n" +
                   "  - abab5.5-chat (legacy)\n\n" +
                   "Configure AI first: ai config <key> <group_id>";
        }
    }

    private String handleChat(ArrayList<String> args, boolean explicit) {
        if (args.isEmpty()) {
            return "Usage: ai ask <message>\nExample: ai ask What is Android?";
        }

        if (aiService == null) {
            return "AI not configured.\nUse 'ai config <api_key> <group_id>' first.\n" +
                   "Get credentials from: https://api.minimax.chat";
        }

        String message = Tuils.toPlanString(args, Tuils.SPACE);

        try {
            MiniMaxService.MiniMaxResponse response = aiService.sendChatRequest(message, null, 500, 0.7f);
            if (response.success) {
                return response.message;
            } else {
                return "AI error: " + response.message;
            }
        } catch (Exception e) {
            Log.e(TAG, "Chat failed", e);
            return "Error: " + e.getMessage();
        }
    }

    private String getHelpText() {
        return "=== AI Command ===\n\n" +
               "Usage: ai <subcommand> [args]\n\n" +
               "Subcommands:\n" +
               "  ask|chat <message>            — Chat with the AI\n" +
               "  config <key> <group_id>        — Configure API credentials\n" +
               "  test                           — Test API connection\n" +
               "  status                         — Show configuration status\n" +
               "  models                         — List available models\n" +
               "  clear                          — Clear conversation history\n" +
               "  help                           — Show this help\n\n" +
               "Examples:\n" +
               "  ai What is Android development?\n" +
               "  ai config sk-xxxxx 12345\n" +
               "  ai test\n\n" +
               "Get API credentials at: https://api.minimax.chat";
    }

    private String getConfigHelpText() {
        return "Usage: ai config <api_key> <group_id>\n\n" +
               "Example:\n" +
               "  ai config sk-abc123def456 123456\n\n" +
               "Get credentials at: https://api.minimax.chat";
    }

    @Override
    public int[] argType() {
        return new int[]{CommandAbstraction.OPTIONAL};
    }

    @Override
    public int priority() {
        return 2;
    }

    @Override
    public int helpRes() {
        return 0;
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int indexNotFound) {
        return getHelpText();
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        return getHelpText();
    }
}
