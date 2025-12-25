package ohi.andre.consolelauncher.settings.modules;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import ohi.andre.consolelauncher.settings.SecurityHelper;

/**
 * Service class for communicating with the MiniMax API.
 * This class handles all HTTP communication with the AI backend,
 * including request formatting, response parsing, and error handling.
 */
public class MiniMaxService {
    
    private static final String TAG = "MiniMaxService";
    private static final String BASE_URL = "https://api.minimax.chat";
    private static final String CHAT_ENDPOINT = "/v1/chat/completions";
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    
    private final Context context;
    private final Gson gson;
    private final OkHttpClient httpClient;
    private final AISettingsModule settingsModule;
    
    public MiniMaxService(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new Gson();
        this.settingsModule = GlobalSettingsManager.getInstance(context).getModule(AISettingsModule.class);
        this.httpClient = createHttpClient();
    }
    
    /**
     * Creates the HTTP client with appropriate configuration.
     * 
     * @return Configured OkHttpClient instance
     */
    private OkHttpClient createHttpClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> {
            Log.d(TAG, message);
        });
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        return new OkHttpClient.Builder()
            .connectTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(chain -> {
                Request original = chain.request();
                Request.Builder requestBuilder = original.newBuilder()
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + getApiKey())
                    .header("X-GroupId", getGroupId());
                
                return chain.proceed(requestBuilder.build());
            })
            .build();
    }
    
    /**
     * Gets the API key from secure storage.
     * 
     * @return API key or empty string
     */
    private String getApiKey() {
        return SecurityHelper.getSecureString(context, "api_key", "");
    }
    
    /**
     * Gets the Group ID from secure storage.
     * 
     * @return Group ID or empty string
     */
    private String getGroupId() {
        return SecurityHelper.getSecureString(context, "group_id", "");
    }
    
    /**
     * Checks if the service is configured with required credentials.
     * 
     * @return true if API key and Group ID are set
     */
    public boolean isConfigured() {
        String apiKey = getApiKey();
        String groupId = getGroupId();
        return apiKey != null && !apiKey.isEmpty() && 
               groupId != null && !groupId.isEmpty();
    }
    
    /**
     * Tests the API connection without sending a message.
     * 
     * @return CompletableFuture containing true if connection successful
     */
    public CompletableFuture<Boolean> testConnection() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        if (!isConfigured()) {
            future.complete(false);
            return future;
        }
        
        // Create a simple test request
        ChatRequest testRequest = new ChatRequest();
        testRequest.setModel(settingsModule.getModel());
        testRequest.setTemperature(0.0f);
        testRequest.setMaxTokens(1);
        
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("user", "test"));
        testRequest.setMessages(messages);
        
        String jsonBody = gson.toJson(testRequest);
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
        
        Request request = new Request.Builder()
            .url(BASE_URL + CHAT_ENDPOINT)
            .post(body)
            .build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Connection test failed", e);
                future.complete(false);
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                boolean success = response.isSuccessful() && response.body() != null;
                future.complete(success);
            }
        });
        
        return future;
    }
    
    /**
     * Sends a message to the AI and returns the response.
     * 
     * @param message The user's message
     * @param conversationHistory Previous messages for context
     * @return CompletableFuture containing the AI response
     */
    public CompletableFuture<AIResponse> sendMessage(String message, List<Message> conversationHistory) {
        CompletableFuture<AIResponse> future = new CompletableFuture<>();
        
        if (!isConfigured()) {
            future.completeExceptionally(new AIException("API not configured. Use: ai config -k <api_key> -g <group_id>"));
            return future;
        }
        
        ChatRequest request = buildRequest(message, conversationHistory);
        String jsonBody = gson.toJson(request);
        
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
        
        Request httpRequest = new Request.Builder()
            .url(BASE_URL + CHAT_ENDPOINT)
            .post(body)
            .build();
        
        httpClient.newCall(httpRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "API request failed", e);
                future.completeExceptionally(new AIException("Network error: " + e.getMessage()));
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        String errorBody = responseBody != null ? responseBody.string() : "Unknown error";
                        Log.e(TAG, "API error: " + response.code() + " - " + errorBody);
                        future.completeExceptionally(new AIException("API error: " + response.code()));
                        return;
                    }
                    
                    if (responseBody == null) {
                        future.completeExceptionally(new AIException("Empty response from API"));
                        return;
                    }
                    
                    String responseString = responseBody.string();
                    Log.d(TAG, "API Response: " + responseString);
                    
                    AIResponse aiResponse = gson.fromJson(responseString, AIResponse.class);
                    future.complete(aiResponse);
                }
            }
        });
        
        return future;
    }
    
    /**
     * Sends a message with streaming response.
     * The response is delivered through the callback as chunks arrive.
     * 
     * @param message The user's message
     * @param conversationHistory Previous messages for context
     * @param callback Callback for streaming response chunks
     * @return Call object for cancellation
     */
    public Call sendMessageStreaming(String message, List<Message> conversationHistory, 
                                     StreamingCallback callback) {
        if (!isConfigured()) {
            callback.onError(new AIException("API not configured"));
            return null;
        }
        
        ChatRequest request = buildRequest(message, conversationHistory);
        String jsonBody = gson.toJson(request);
        
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));
        
        Request httpRequest = new Request.Builder()
            .url(BASE_URL + CHAT_ENDPOINT)
            .post(body)
            .build();
        
        Call call = httpClient.newCall(httpRequest);
        
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(new AIException("Network error: " + e.getMessage()));
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful()) {
                        callback.onError(new AIException("API error: " + response.code()));
                        return;
                    }
                    
                    if (responseBody == null) {
                        callback.onError(new AIException("Empty response"));
                        return;
                    }
                    
                    // Process streaming response
                    String rawResponse = responseBody.string();
                    processStreamingResponse(rawResponse, callback);
                }
            }
        });
        
        return call;
    }
    
    /**
     * Builds a chat request from the message and history.
     * 
     * @param message Current user message
     * @param conversationHistory Previous messages
     * @return ChatRequest ready for serialization
     */
    private ChatRequest buildRequest(String message, List<Message> conversationHistory) {
        ChatRequest request = new ChatRequest();
        request.setModel(settingsModule.getModel());
        request.setTemperature(settingsModule.getTemperature());
        request.setMaxTokens(settingsModule.getMaxTokens());
        
        List<Message> messages = new ArrayList<>();
        
        // Add system prompt if configured
        if (settingsModule.hasCustomSystemPrompt()) {
            Message systemMessage = new Message("system", settingsModule.getSystemPrompt());
            messages.add(systemMessage);
        }
        
        // Add conversation history
        if (conversationHistory != null) {
            int contextMessages = settingsModule.getContextMessages();
            int startIndex = Math.max(0, conversationHistory.size() - contextMessages);
            for (int i = startIndex; i < conversationHistory.size(); i++) {
                messages.add(conversationHistory.get(i));
            }
        }
        
        // Add current message
        messages.add(new Message("user", message));
        
        request.setMessages(messages);
        
        return request;
    }
    
    /**
     * Processes a streaming response and delivers chunks via callback.
     * 
     * @param rawResponse Raw response string
     * @param callback Streaming callback
     */
    private void processStreamingResponse(String rawResponse, StreamingCallback callback) {
        try {
            // Split by newlines and parse each chunk
            String[] lines = rawResponse.split("\n");
            StringBuilder fullContent = new StringBuilder();
            
            for (String line : lines) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6);
                    
                    if (data.equals("[DONE]")) {
                        break;
                    }
                    
                    // Parse chunk
                    AIResponse chunk = gson.fromJson(data, AIResponse.class);
                    if (chunk != null && chunk.getChoices() != null && !chunk.getChoices().isEmpty()) {
                        String content = chunk.getChoices().get(0).getDelta().getContent();
                        if (content != null && !content.isEmpty()) {
                            fullContent.append(content);
                            callback.onChunk(content, fullContent.toString());
                        }
                    }
                }
            }
            
            callback.onComplete(fullContent.toString());
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to process streaming response", e);
            callback.onError(new AIException("Failed to parse response: " + e.getMessage()));
        }
    }
    
    /**
     * Gets available models from settings.
     * 
     * @return Array of model identifiers
     */
    public String[] getAvailableModels() {
        return settingsModule.getAvailableModels();
    }
    
    /**
     * Shuts down the service and releases resources.
     */
    public void shutdown() {
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
    }
    
    // ==================== Inner Classes ====================
    
    /**
     * Represents a chat message.
     */
    public static class Message {
        private String role;
        private String content;
        
        public Message() {}
        
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
    }
    
    /**
     * Chat request payload.
     */
    public static class ChatRequest {
        private String model;
        private List<Message> messages;
        private float temperature;
        private int maxTokens;
        private boolean stream;
        
        public ChatRequest() {
            this.stream = false;
        }
        
        public String getModel() {
            return model;
        }
        
        public void setModel(String model) {
            this.model = model;
        }
        
        public List<Message> getMessages() {
            return messages;
        }
        
        public void setMessages(List<Message> messages) {
            this.messages = messages;
        }
        
        public float getTemperature() {
            return temperature;
        }
        
        public void setTemperature(float temperature) {
            this.temperature = temperature;
        }
        
        public int getMaxTokens() {
            return maxTokens;
        }
        
        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }
        
        public boolean isStream() {
            return stream;
        }
        
        public void setStream(boolean stream) {
            this.stream = stream;
        }
    }
    
    /**
     * Callback interface for streaming responses.
     */
    public interface StreamingCallback {
        void onChunk(String chunk, String fullContent);
        void onComplete(String fullContent);
        void onError(Exception e);
    }
    
    /**
     * Exception class for AI-related errors.
     */
    public static class AIException extends RuntimeException {
        public AIException(String message) {
            super(message);
        }
        
        public AIException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
