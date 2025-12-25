package ohi.andre.consolelauncher.ai;

import android.content.Context;
import android.util.Log;

import ohi.andre.consolelauncher.BuildConfig;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.settings.GlobalSettingsManager;
import ohi.andre.consolelauncher.settings.modules.AISettingsModule;
import ohi.andre.consolelauncher.settings.modules.VoiceSettingsModule;
import ohi.andre.consolelauncher.tuils.StoppableThread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI command implementation for T-UI ConsoleLauncher.
 * This command provides access to MiniMax AI capabilities through the console interface.
 * 
 * Usage:
 * - ai <message> - Send a message to the AI
 * - ai config -k <api_key> - Set API key
 * - ai config -g <group_id> - Set Group ID
 * - ai config -k <api_key> -g <group_id> - Set both credentials
 * - ai test - Test API connection
 * - ai status - Show AI configuration status
 * - ai models - List available models
 * - ai clear - Clear conversation context
 * - ai model <model_name> - Switch AI model
 */
public class AICommand implements CommandAbstraction {
    
    private static final String TAG = "AICommand";
    
    private final Context context;
    private final MiniMaxService aiService;
    private final AISettingsModule aiSettings;
    private final VoiceSettingsModule voiceSettings;
    private final ExecutorService executorService;
    private final List<MiniMaxService.Message> conversationHistory;
    private StoppableThread currentThread;
    
    public AICommand(Context context) {
        this.context = context.getApplicationContext();
        this.aiService = new MiniMaxService(context);
        this.aiSettings = GlobalSettingsManager.getInstance(context).getModule(AISettingsModule.class);
        this.voiceSettings = GlobalSettingsManager.getInstance(context).getModule(VoiceSettingsModule.class);
        this.executorService = Executors.newSingleThreadExecutor();
        this.conversationHistory = new ArrayList<>();
    }
    
    @Override
    public String exec(ExecutePack pack) throws Exception {
        String[] args = pack.args;
        
        if (args == null || args.length == 0 || args[0].isEmpty()) {
            return getHelpText();
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "config":
                return handleConfig(args);
            case "test":
                return handleTest();
            case "status":
                return handleStatus();
            case "models":
                return handleModels();
            case "clear":
                return handleClear();
            case "model":
                return handleModel(args);
            case "help":
                return getHelpText();
            default:
                // Treat as direct message
                return handleMessage(String.join(" ", args));
        }
    }
    
    /**
     * Handles the config subcommand for setting API credentials.
     */
    private String handleConfig(String[] args) {
        String apiKey = null;
        String groupId = null;
        
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "-k":
                case "--key":
                    if (i + 1 < args.length) {
                        apiKey = args[++i];
                    }
                    break;
                case "-g":
                case "--group":
                    if (i + 1 < args.length) {
                        groupId = args[++i];
                    }
                    break;
                case "-h":
                case "--help":
                    return getConfigHelpText();
            }
        }
        
        if (apiKey == null && groupId == null) {
            return "Error: No credentials provided. Use: ai config -k <api_key> -g <group_id>";
        }
        
        if (apiKey != null) {
            aiSettings.setApiKey(apiKey);
            Log.d(TAG, "API key set");
        }
        
        if (groupId != null) {
            aiSettings.setGroupId(groupId);
            Log.d(TAG, "Group ID set");
        }
        
        return "Credentials saved successfully. Use 'ai test' to verify connection.";
    }
    
    /**
     * Handles the test subcommand for verifying API connection.
     */
    private String handleTest() {
        CompletableFuture<Boolean> future = aiService.testConnection();
        
        try {
            Boolean result = future.get(10, java.util.concurrent.TimeUnit.SECONDS);
            if (result) {
                String message = "✓ API connection successful!\n";
                message += "Model: " + aiSettings.getModel() + "\n";
                message += "Temperature: " + aiSettings.getTemperature() + "\n";
                message += "Max Tokens: " + aiSettings.getMaxTokens();
                
                // Speak success message if enabled
                if (voiceSettings.isTtsEnabled() && voiceSettings.isTtsSpeakSuccessEnabled()) {
                    voiceSettings.speak("AI connection successful", "ai-test-success");
                }
                
                return message;
            } else {
                return "✗ API connection failed. Check your credentials.";
            }
        } catch (Exception e) {
            Log.e(TAG, "Connection test failed", e);
            return "✗ API connection failed: " + e.getMessage();
        }
    }
    
    /**
     * Handles the status subcommand for showing current configuration.
     */
    private String handleStatus() {
        StringBuilder status = new StringBuilder();
        status.append("=== AI Status ===\n\n");
        status.append("Configuration: ").append(aiSettings.getStatusDescription()).append("\n\n");
        status.append("Settings:\n");
        status.append("  Model: ").append(aiSettings.getModel()).append("\n");
        status.append("  Temperature: ").append(aiSettings.getTemperature()).append("\n");
        status.append("  Max Tokens: ").append(aiSettings.getMaxTokens()).append("\n");
        status.append("  Context Messages: ").append(aiSettings.getContextMessages()).append("\n\n");
        
        status.append("Voice:\n");
        status.append("  Enabled: ").append(aiSettings.isVoiceEnabled() ? "Yes" : "No").append("\n");
        status.append("  Auto-read: ").append(aiSettings.isVoiceAutoReadEnabled() ? "Yes" : "No").append("\n");
        
        status.append("\nStreaming:\n");
        status.append("  Enabled: ").append(aiSettings.isStreamingEnabled() ? "Yes" : "No").append("\n");
        status.append("  Show Thinking: ").append(aiSettings.isShowThinkingEnabled() ? "Yes" : "No");
        
        return status.toString();
    }
    
    /**
     * Handles the models subcommand for listing available models.
     */
    private String handleModels() {
        String[] models = aiService.getAvailableModels();
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== Available Models ===\n\n");
        
        for (String model : models) {
            sb.append("• ").append(model);
            if (model.equals(aiSettings.getModel())) {
                sb.append(" (current)");
            }
            sb.append("\n");
        }
        
        sb.append("\nUse 'ai model <name>' to switch models.");
        
        return sb.toString();
    }
    
    /**
     * Handles the clear subcommand for clearing conversation history.
     */
    private String handleClear() {
        conversationHistory.clear();
        return "Conversation context cleared.";
    }
    
    /**
     * Handles the model subcommand for switching models.
     */
    private String handleModel(String[] args) {
        if (args.length < 2) {
            return "Error: No model specified. Use: ai model <model_name>";
        }
        
        String modelName = args[1];
        aiSettings.setModel(modelName);
        return "Model switched to: " + modelName;
    }
    
    /**
     * Handles direct message sending to the AI.
     */
    private String handleMessage(String message) {
        if (!aiSettings.hasApiKey() || !aiSettings.hasGroupId()) {
            return "Error: AI not configured.\n" +
                   aiSettings.getStatusDescription() + "\n\n" +
                   "Use 'ai config -k <api_key> -g <group_id>' to configure.";
        }
        
        if (aiSettings.isShowThinkingEnabled()) {
            pack.apply("Thinking...");
        }
        
        try {
            if (aiSettings.isStreamingEnabled()) {
                return handleStreamingMessage(message);
            } else {
                return handleBlockingMessage(message);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to process message", e);
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * Handles message with streaming response.
     */
    private String handleStreamingMessage(String message) throws InterruptedException {
        final StringBuilder fullResponse = new StringBuilder();
        final boolean[] errorOccurred = {false};
        final String[] errorMessage = {null};
        
        MiniMaxService.StreamingCallback callback = new MiniMaxService.StreamingCallback() {
            @Override
            public void onChunk(String chunk, String currentContent) {
                fullResponse.setLength(0);
                fullResponse.append(currentContent);
            }
            
            @Override
            public void onComplete(String content) {
                fullResponse.setLength(0);
                fullResponse.append(content);
            }
            
            @Override
            public void onError(Exception e) {
                errorOccurred[0] = true;
                errorMessage[0] = e.getMessage();
            }
        };
        
        // Add user message to history
        conversationHistory.add(new MiniMaxService.Message("user", message));
        
        // Start streaming request
        currentThread = new StoppableThread(() -> {
            try {
                aiService.sendMessageStreaming(message, conversationHistory, callback);
            } catch (Exception e) {
                errorOccurred[0] = true;
                errorMessage[0] = e.getMessage();
            }
        });
        
        currentThread.start();
        
        // Wait for completion or timeout
        try {
            synchronized (this) {
                int waitCount = 0;
                int maxWait = 60; // 60 seconds timeout
                
                while (fullResponse.length() == 0 && !errorOccurred[0] && waitCount < maxWait) {
                    wait(1000);
                    waitCount++;
                }
            }
        } catch (InterruptedException e) {
            Log.d(TAG, "Streaming interrupted");
        }
        
        if (errorOccurred[0]) {
            return "Error: " + errorMessage[0];
        }
        
        String response = fullResponse.toString();
        
        // Add AI response to history
        conversationHistory.add(new MiniMaxService.Message("assistant", response));
        
        // Prune history if needed
        while (conversationHistory.size() > aiSettings.getContextMessages() * 2) {
            conversationHistory.remove(0);
        }
        
        // Speak response if voice is enabled
        if (aiSettings.isVoiceEnabled() && aiSettings.isVoiceAutoReadEnabled()) {
            voiceSettings.speak(response, "ai-response");
        }
        
        return response;
    }
    
    /**
     * Handles message with blocking response.
     */
    private String handleBlockingMessage(String message) {
        // Add user message to history
        conversationHistory.add(new MiniMaxService.Message("user", message));
        
        CompletableFuture<AIResponse> future = aiService.sendMessage(message, conversationHistory);
        
        try {
            AIResponse response = future.get(30, java.util.concurrent.TimeUnit.SECONDS);
            String content = response.getContent();
            
            // Add AI response to history
            conversationHistory.add(new MiniMaxService.Message("assistant", content));
            
            // Prune history if needed
            while (conversationHistory.size() > aiSettings.getContextMessages() * 2) {
                conversationHistory.remove(0);
            }
            
            // Speak response if voice is enabled
            if (aiSettings.isVoiceEnabled() && aiSettings.isVoiceAutoReadEnabled()) {
                voiceSettings.speak(content, "ai-response");
            }
            
            return content;
            
        } catch (Exception e) {
            Log.e(TAG, "AI request failed", e);
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * Gets the help text for the AI command.
     */
    private String getHelpText() {
        return "=== AI Command Help ===\n\n" +
               "Usage: ai [subcommand] [arguments]\n\n" +
               "Subcommands:\n" +
               "  <message>      - Send a message to the AI\n" +
               "  config -k <key> - Set API key\n" +
               "  config -g <id>  - Set Group ID\n" +
               "  test           - Test API connection\n" +
               "  status         - Show current configuration\n" +
               "  models         - List available models\n" +
               "  model <name>   - Switch AI model\n" +
               "  clear          - Clear conversation history\n" +
               "  help           - Show this help message\n\n" +
               "Examples:\n" +
               "  ai What is Android development?\n" +
               "  ai config -k my_api_key -g my_group_id\n" +
               "  ai test";
    }
    
    /**
     * Gets the help text for the config subcommand.
     */
    private String getConfigHelpText() {
        return "=== AI Config Help ===\n\n" +
               "Usage: ai config [options]\n\n" +
               "Options:\n" +
               "  -k, --key <api_key>   - Set the MiniMax API key\n" +
               "  -g, --group <group_id> - Set the MiniMax Group ID\n" +
               "  -h, --help            - Show this help message\n\n" +
               "To get your API credentials:\n" +
               "1. Visit https://api.minimax.chat\n" +
               "2. Create an account and get API access\n" +
               "3. Copy your API key and Group ID\n\n" +
               "Example:\n" +
               "  ai config -k sk-1234567890 -g 12345";
    }
    
    /**
     * Stops any ongoing AI request.
     */
    public void stopCurrentRequest() {
        if (currentThread != null && currentThread.isAlive()) {
            currentThread.interrupt();
            currentThread = null;
        }
    }
    
    /**
     * Gets the current conversation history size.
     */
    public int getHistorySize() {
        return conversationHistory.size();
    }
    
    /**
     * Clears the conversation history.
     */
    public void clearHistory() {
        conversationHistory.clear();
    }
    
    @Override
    public int[] argType() {
        return new int[]{CommandAbstraction.OPTIONAL};
    }
    
    @Override
    public int priority() {
        return 2; // Same priority as other smart launcher commands
    }
    
    @Override
    public int helpRes() {
        return 0; // Using inline help
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
