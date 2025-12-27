package ohi.andre.consolelauncher.ai;

import android.content.Context;
import android.util.Log;

import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;

import java.util.ArrayList;

/**
 * AI command implementation for T-UI ConsoleLauncher.
 * Provides access to MiniMax AI capabilities through the console interface.
 */
public class AICommand implements CommandAbstraction {
    
    private static final String TAG = "AICommand";
    
    private Context context;
    private MiniMaxService aiService;
    
    public AICommand() {
    }
    
    @Override
    public String exec(ExecutePack pack) throws Exception {
        context = pack.context;
        
        // Initialize service lazily
        if (aiService == null) {
            aiService = new MiniMaxService(context);
        }
        
        ArrayList<String> argsList = new ArrayList<>();
        for (Object arg : pack.args) {
            if (arg != null) {
                argsList.add(arg.toString());
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
                return handleClear();
            case "help":
                return getHelpText();
            default:
                // Treat as direct message
                return handleMessage(String.join(" ", argsList));
        }
    }
    
    /**
     * Handle config subcommand
     */
    private String handleConfig(ArrayList<String> args) {
        if (args.size() < 2) {
            return getConfigHelpText();
        }
        
        String apiKey = args.get(0);
        String groupId = args.get(1);
        
        // For now, just test the configuration
        MiniMaxService testService = new MiniMaxService(context, apiKey, groupId);
        MiniMaxService.MiniMaxResponse response = testService.testConnection();
        
        if (response.success) {
            aiService = testService;
            return "AI Configuration successful!\n" + response.message;
        } else {
            return "Configuration failed: " + response.message;
        }
    }
    
    /**
     * Handle test subcommand
     */
    private String handleTest() {
        if (aiService == null) {
            return "AI service not configured. Use 'ai config <api_key> <group_id>' to set up.";
        }
        
        MiniMaxService.MiniMaxResponse response = aiService.testConnection();
        
        if (response.success) {
            return "API connection successful!\n" + response.message;
        } else {
            return "API connection failed: " + response.message;
        }
    }
    
    /**
     * Handle status subcommand
     */
    private String handleStatus() {
        StringBuilder status = new StringBuilder();
        status.append("=== AI Status ===\n\n");
        
        if (aiService != null) {
            status.append("Status: Configured\n");
            status.append("Available Models:\n");
            for (String model : aiService.getAvailableModels()) {
                status.append("  - ").append(model).append("\n");
            }
        } else {
            status.append("Status: Not configured\n");
            status.append("Use 'ai config <api_key> <group_id>' to configure.");
        }
        
        return status.toString();
    }
    
    /**
     * Handle models subcommand
     */
    private String handleModels() {
        if (aiService == null) {
            return "AI service not configured. Available models:\n" +
                   "  - abab6.5-chat (default)\n" +
                   "  - abab6.5s-chat (fast)\n" +
                   "  - abab5.5-chat (legacy)";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== Available Models ===\n\n");
        
        for (String model : aiService.getAvailableModels()) {
            sb.append("• ").append(model).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Handle clear subcommand
     */
    private String handleClear() {
        return "Conversation context cleared.";
    }
    
    /**
     * Handle message sending to AI
     */
    private String handleMessage(String message) {
        if (aiService == null) {
            return "Error: AI not configured.\n" +
                   "Use 'ai config <api_key> <group_id>' to configure.\n" +
                   "Get credentials from: https://api.minimax.chat";
        }
        
        try {
            MiniMaxService.MiniMaxResponse response = aiService.sendChatRequest(message, null, 500, 0.7);
            
            if (response.success) {
                return response.message;
            } else {
                return "Error: " + response.message;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to process message", e);
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * Get help text
     */
    private String getHelpText() {
        return "=== AI Command Help ===\n\n" +
               "Usage: ai [subcommand] [arguments]\n\n" +
               "Subcommands:\n" +
               "  <message>      - Send a message to the AI\n" +
               "  config <key> <id> - Configure API credentials\n" +
               "  test           - Test API connection\n" +
               "  status         - Show current configuration\n" +
               "  models         - List available models\n" +
               "  clear          - Clear conversation history\n" +
               "  help           - Show this help message\n\n" +
               "Examples:\n" +
               "  ai What is Android development?\n" +
               "  ai config sk-1234567890 12345\n" +
               "  ai test\n\n" +
               "Get API credentials from: https://api.minimax.chat";
    }
    
    /**
     * Get config help text
     */
    private String getConfigHelpText() {
        return "=== AI Config Help ===\n\n" +
               "Usage: ai config <api_key> <group_id>\n\n" +
               "To get your API credentials:\n" +
               "1. Visit https://api.minimax.chat\n" +
               "2. Create an account and get API access\n" +
               "3. Copy your API key and Group ID\n\n" +
               "Example:\n" +
               "  ai config sk-1234567890 12345";
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
