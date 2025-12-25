package ohi.andre.consolelauncher.commands.smartlauncher.ai;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Map;

import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;

/**
 * AI Command - MiniMax AI integration for T-UI
 * Provides chat, help, and AI-powered assistance
 */
public class AICommand implements CommandAbstraction {

    private static final String TAG = "AICommand";
    private MiniMaxService miniMaxService;
    private Context context;

    @Override
    public String exec(ExecutePack pack) throws Exception {
        context = pack.context;
        
        ArrayList<String> argsList = new ArrayList<>();
        for (Object arg : pack.args) {
            if (arg != null) {
                argsList.add(arg.toString());
            }
        }

        // Initialize service if needed
        initializeService();

        if (argsList.isEmpty() || "--help".equals(argsList.get(0)) || "-h".equals(argsList.get(0))) {
            return getUsage();
        }

        String command = argsList.get(0).toLowerCase();
        ArrayList<String> parameters = new ArrayList<>(argsList.subList(1, argsList.size()));

        switch (command) {
            case "chat":
            case "ask":
            case "ai":
                return chat(parameters);
            case "config":
            case "setup":
                return configure(parameters);
            case "test":
                return testConnection();
            case "status":
                return status();
            case "models":
                return listModels();
            case "clear":
                return clearConfig();
            default:
                // Treat as direct chat if no specific command
                return chat(argsList);
        }
    }

    @Override
    public int[] argType() {
        return new int[]{CommandAbstraction.COMMAND};
    }

    @Override
    public int priority() {
        return 3; // Higher priority for AI responses
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
               "║                   AI COMMANDS                         ║\n" +
               "╠══════════════════════════════════════════════════════╣\n" +
               "║  ai chat <message>           - Chat with AI          ║\n" +
               "║  ai config <key> <group>     - Configure API         ║\n" +
               "║  ai test                     - Test connection       ║\n" +
               "║  ai status                   - Show status           ║\n" +
               "║  ai models                   - List available models ║\n" +
               "║  ai clear                    - Clear configuration   ║\n" +
               "║                                                      ║\n" +
               "║  Direct usage: ai <your question>                   ║\n" +
               "╚══════════════════════════════════════════════════════╝\n";
    }

    /**
     * Initialize MiniMax service with saved configuration
     */
    private void initializeService() {
        if (miniMaxService != null) return;

        Map<String, Object> config = MiniMaxService.Config.loadConfig(context);
        String apiKey = (String) config.get("apiKey");
        String groupId = (String) config.get("groupId");
        boolean enabled = (Boolean) config.get("enabled");

        if (enabled && apiKey != null && groupId != null) {
            miniMaxService = new MiniMaxService(context, apiKey, groupId);
        }
    }

    /**
     * Chat with AI
     */
    private String chat(ArrayList<String> args) {
        if (miniMaxService == null) {
            return "AI service not configured.\nUse 'ai config <api_key> <group_id>' to set up.\n" +
                   "Get your credentials from: https://api.minimax.chat";
        }

        if (args.isEmpty()) {
            return "Please provide a message to chat.\nUsage: ai chat <your message>";
        }

        String message = listToString(args);
        
        // Show typing indicator
        StringBuilder result = new StringBuilder();
        result.append("\n");
        result.append("🤖 AI Assistant\n");
        result.append("─".repeat(50)).append("\n");
        result.append("Thinking...\n");

        try {
            MiniMaxService.MiniMaxResponse response = miniMaxService.sendChatRequest(message, null, 500, 0.7);
            
            if (response.success) {
                result.append("\n");
                result.append(response.message);
                result.append("\n");
                result.append("─".repeat(50)).append("\n");
                result.append("💬 Ask another question or use 'ai help' for commands");
            } else {
                result.append("\n");
                result.append("❌ Error: ").append(response.message);
                result.append("\n");
                result.append("Use 'ai test' to check connection or 'ai config' to reconfigure");
            }
        } catch (Exception e) {
            Log.e(TAG, "Chat error", e);
            result.append("\n");
            result.append("❌ Chat failed: ").append(e.getMessage());
        }

        return result.toString();
    }

    /**
     * Configure AI service
     */
    private String configure(ArrayList<String> args) {
        if (args.size() < 2) {
            return "Usage: ai config <api_key> <group_id> [model]\n\n" +
                   "Get your credentials from: https://api.minimax.chat\n" +
                   "Example: ai config sk-abc123 def456";
        }

        String apiKey = args.get(0);
        String groupId = args.get(1);
        String model = args.size() > 2 ? args.get(2) : "abab6.5-chat";

        // Validate basic format
        if (apiKey.length() < 20) {
            return "❌ Invalid API key format. API keys are typically longer.\n" +
                   "Get your key from: https://api.minimax.chat";
        }

        if (groupId.length() < 10) {
            return "❌ Invalid group ID format.\n" +
                   "Get your group ID from: https://api.minimax.chat";
        }

        // Test the configuration
        MiniMaxService testService = new MiniMaxService(context, apiKey, groupId);
        MiniMaxService.MiniMaxResponse testResponse = testService.testConnection();

        if (testResponse.success) {
            // Save configuration
            MiniMaxService.Config.saveConfig(context, apiKey, groupId, model, true);
            miniMaxService = testService; // Update current service
            
            StringBuilder result = new StringBuilder();
            result.append("\n");
            result.append("✅ AI Configuration Saved\n");
            result.append("─".repeat(50)).append("\n");
            result.append("API Key: ").append(maskApiKey(apiKey)).append("\n");
            result.append("Group ID: ").append(maskGroupId(groupId)).append("\n");
            result.append("Model: ").append(model).append("\n");
            result.append("\n");
            result.append("Test message sent successfully!\n");
            result.append("You can now chat with AI using 'ai <message>'");

            return result.toString();
        } else {
            return "❌ Configuration test failed:\n" + testResponse.message + "\n\n" +
                   "Please check your credentials and try again.\n" +
                   "Get valid credentials from: https://api.minimax.chat";
        }
    }

    /**
     * Test AI connection
     */
    private String testConnection() {
        if (miniMaxService == null) {
            return "AI service not configured. Use 'ai config' to set up first.";
        }

        StringBuilder result = new StringBuilder();
        result.append("\n");
        result.append("🔍 Testing AI Connection\n");
        result.append("─".repeat(50)).append("\n");

        try {
            MiniMaxService.MiniMaxResponse response = miniMaxService.testConnection();
            
            if (response.success) {
                result.append("✅ Connection successful!\n");
                result.append("Response: ").append(response.message).append("\n");
                result.append("\n");
                result.append("AI service is ready for use.");
            } else {
                result.append("❌ Connection failed:\n");
                result.append(response.message).append("\n");
                result.append("\n");
                result.append("Check your configuration with 'ai config'");
            }
        } catch (Exception e) {
            Log.e(TAG, "Test connection error", e);
            result.append("❌ Test failed: ").append(e.getMessage());
        }

        return result.toString();
    }

    /**
     * Show AI status
     */
    private String status() {
        Map<String, Object> config = MiniMaxService.Config.loadConfig(context);
        boolean enabled = (Boolean) config.get("enabled");
        
        StringBuilder result = new StringBuilder();
        result.append("\n");
        result.append("🤖 AI Service Status\n");
        result.append("═".repeat(50)).append("\n");
        
        if (enabled && miniMaxService != null) {
            result.append("Status: ✅ Enabled\n");
            result.append("Model: ").append(config.get("model")).append("\n");
            result.append("API Key: ").append(maskApiKey((String) config.get("apiKey"))).append("\n");
            result.append("Group ID: ").append(maskGroupId((String) config.get("groupId"))).append("\n");
            result.append("\n");
            result.append("Usage: ai <your message>\n");
            result.append("Commands: ai help");
        } else {
            result.append("Status: ❌ Disabled\n");
            result.append("Configuration: Not set\n");
            result.append("\n");
            result.append("Setup: ai config <api_key> <group_id>");
        }

        return result.toString();
    }

    /**
     * List available models
     */
    private String listModels() {
        if (miniMaxService == null) {
            initializeService();
        }

        StringBuilder result = new StringBuilder();
        result.append("\n");
        result.append("🧠 Available AI Models\n");
        result.append("─".repeat(50)).append("\n");

        if (miniMaxService != null) {
            for (String model : miniMaxService.getAvailableModels()) {
                result.append("  • ").append(model).append("\n");
            }
            result.append("\n");
            result.append("Default: abab6.5-chat\n");
            result.append("Fast: abab6.5s-chat\n");
            result.append("Legacy: abab5.5-chat");
        } else {
            result.append("Available models:\n");
            result.append("  • abab6.5-chat (default)\n");
            result.append("  • abab6.5s-chat (fast)\n");
            result.append("  • abab5.5-chat (legacy)");
        }

        return result.toString();
    }

    /**
     * Clear configuration
     */
    private String clearConfig() {
        MiniMaxService.Config.clearConfig(context);
        miniMaxService = null;
        
        return "\n✅ AI configuration cleared.\n" +
               "Use 'ai config' to set up again.";
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

    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) return "****";
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    private String maskGroupId(String groupId) {
        if (groupId == null || groupId.length() < 8) return "****";
        return groupId.substring(0, 3) + "****" + groupId.substring(groupId.length() - 3);
    }
}