package ohi.andre.consolelauncher.commands.smartlauncher.ai;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * MiniMaxService - Service for communicating with MiniMax AI API
 * Handles authentication, request formatting, and response parsing
 */
public class MiniMaxService {
    private static final String TAG = "MiniMaxService";
    private static final String BASE_URL = "https://api.minimax.chat/v1/text/chatcompletion_v2";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private final OkHttpClient client;
    private final Gson gson;
    private final String apiKey;
    private final String groupId;
    
    private Context context;

    public MiniMaxService(Context context, String apiKey, String groupId) {
        this.context = context;
        this.apiKey = apiKey;
        this.groupId = groupId;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Sends a chat completion request to MiniMax API
     */
    public MiniMaxResponse sendChatRequest(String message, String model, int maxTokens, double temperature) {
        try {
            ChatRequest request = createChatRequest(message, model, maxTokens, temperature);
            String jsonRequest = gson.toJson(request);
            
            Log.d(TAG, "Sending request: " + jsonRequest);

            Request httpRequest = new Request.Builder()
                    .url(BASE_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonRequest, JSON))
                    .build();

            try (Response response = client.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    Log.e(TAG, "API request failed: " + response.code() + " - " + errorBody);
                    return new MiniMaxResponse(false, "API request failed: " + response.code() + " - " + errorBody);
                }

                String responseBody = response.body().string();
                Log.d(TAG, "API response: " + responseBody);

                MiniMaxResponse chatResponse = gson.fromJson(responseBody, MiniMaxResponse.class);
                
                if (chatResponse != null && chatResponse.choices != null && !chatResponse.choices.isEmpty()) {
                    chatResponse.success = true;
                    chatResponse.message = chatResponse.choices.get(0).message.content;
                } else {
                    chatResponse = new MiniMaxResponse(false, "Invalid response format");
                }

                return chatResponse;
            }
        } catch (IOException e) {
            Log.e(TAG, "Network error", e);
            return new MiniMaxResponse(false, "Network error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error", e);
            return new MiniMaxResponse(false, "Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Creates a chat request object
     */
    private ChatRequest createChatRequest(String message, String model, int maxTokens, double temperature) {
        List<Message> messages = new ArrayList<>();
        
        // Add system prompt for T-UI assistant
        String systemPrompt = "You are a helpful AI assistant integrated into the T-UI Android launcher. " +
                "Provide concise, useful responses that are appropriate for mobile CLI usage. " +
                "Keep responses brief and actionable. If asked about system features, " +
                "suggest using the available T-UI commands when relevant.";
        
        messages.add(new Message("system", systemPrompt));
        messages.add(new Message("user", message));

        return new ChatRequest(
            model != null ? model : "abab6.5-chat",
            messages,
            maxTokens,
            temperature,
            groupId
        );
    }

    /**
     * Gets available models
     */
    public List<String> getAvailableModels() {
        List<String> models = new ArrayList<>();
        models.add("abab6.5-chat");
        models.add("abab6.5s-chat");
        models.add("abab5.5-chat");
        return models;
    }

    /**
     * Validates API key format
     */
    public boolean isValidApiKey() {
        return apiKey != null && !apiKey.trim().isEmpty() && apiKey.length() > 20;
    }

    /**
     * Validates group ID format
     */
    public boolean isValidGroupId() {
        return groupId != null && !groupId.trim().isEmpty() && groupId.length() > 10;
    }

    /**
     * Test API connectivity
     */
    public MiniMaxResponse testConnection() {
        return sendChatRequest("Hello, this is a connection test.", null, 50, 0.1);
    }

    // Request/Response classes
    public static class ChatRequest {
        public String model;
        public List<Message> messages;
        public int max_tokens;
        public double temperature;
        public String group_id;

        public ChatRequest(String model, List<Message> messages, int max_tokens, double temperature, String group_id) {
            this.model = model;
            this.messages = messages;
            this.max_tokens = max_tokens;
            this.temperature = temperature;
            this.group_id = group_id;
        }
    }

    public static class Message {
        public String role;
        public String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public static class MiniMaxResponse {
        public boolean success;
        public String message;
        public String error;
        public List<Choice> choices;
        public String model;
        public String object;
        public long created;
        public String id;

        public MiniMaxResponse() {}

        public MiniMaxResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public MiniMaxResponse(boolean success, String message, String error) {
            this.success = success;
            this.message = message;
            this.error = error;
        }
    }

    public static class Choice {
        public int index;
        public Message message;
        public String finish_reason;
    }

    public static class Usage {
        public int prompt_tokens;
        public int completion_tokens;
        public int total_tokens;
    }

    /**
     * Configuration helper class
     */
    public static class Config {
        private static final String PREFS_NAME = "minimax_config";
        private static final String KEY_API_KEY = "api_key";
        private static final String KEY_GROUP_ID = "group_id";
        private static final String KEY_MODEL = "model";
        private static final String KEY_ENABLED = "enabled";

        public static void saveConfig(Context context, String apiKey, String groupId, String model, boolean enabled) {
            android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                .putString(KEY_API_KEY, apiKey)
                .putString(KEY_GROUP_ID, groupId)
                .putString(KEY_MODEL, model)
                .putBoolean(KEY_ENABLED, enabled)
                .apply();
        }

        public static Map<String, Object> loadConfig(Context context) {
            android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            Map<String, Object> config = new HashMap<>();
            config.put("apiKey", prefs.getString(KEY_API_KEY, ""));
            config.put("groupId", prefs.getString(KEY_GROUP_ID, ""));
            config.put("model", prefs.getString(KEY_MODEL, "abab6.5-chat"));
            config.put("enabled", prefs.getBoolean(KEY_ENABLED, false));
            return config;
        }

        public static boolean isEnabled(Context context) {
            android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            return prefs.getBoolean(KEY_ENABLED, false);
        }

        public static void clearConfig(Context context) {
            android.content.SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().clear().apply();
        }
    }
}