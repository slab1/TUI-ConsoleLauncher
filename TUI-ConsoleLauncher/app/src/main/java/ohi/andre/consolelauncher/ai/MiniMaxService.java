package ohi.andre.consolelauncher.ai;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * MiniMax Service - API client for MiniMax AI
 * Handles HTTP communication with MiniMax Chat API
 */
public class MiniMaxService {

    private static final String TAG = "MiniMaxService";
    private static final String BASE_URL = "https://api.minimax.chat";
    private static final String CHAT_ENDPOINT = "/v1/chat/completions";
    private static final int TIMEOUT_SECONDS = 30;

    private final String apiKey;
    private final String groupId;
    private final String model;
    private final OkHttpClient httpClient;

    /**
     * Create service with default config
     */
    public MiniMaxService(Context context) {
        this.apiKey = "";
        this.groupId = "";
        this.model = "abab6.5-chat";
        this.httpClient = createHttpClient();
    }

    /**
     * Create service with configuration
     */
    public MiniMaxService(Context context, String apiKey, String groupId) {
        this.apiKey = apiKey;
        this.groupId = groupId;
        this.model = "abab6.5-chat";
        this.httpClient = createHttpClient();
    }

    private OkHttpClient createHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request.Builder builder = original.newBuilder()
                            .header("Content-Type", "application/json")
                            .header("Authorization", "Bearer " + apiKey)
                            .header("X-GroupId", groupId);
                    return chain.proceed(builder.build());
                })
                .build();
    }

    /**
     * Test connection to API
     */
    public MiniMaxResponse testConnection() {
        MiniMaxResponse response = new MiniMaxResponse();

        try {
            RequestBody body = RequestBody.create(
                    "{\"model\":\"" + model + "\",\"messages\":[{\"role\":\"user\",\"content\":\"test\"}],\"max_tokens\":1}",
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(BASE_URL + CHAT_ENDPOINT)
                    .post(body)
                    .build();

            Response httpResponse = httpClient.newCall(request).execute();
            response.success = httpResponse.isSuccessful();
            response.message = httpResponse.isSuccessful() ? "Connection successful" : "Error: " + httpResponse.code();
            httpResponse.close();

        } catch (Exception e) {
            Log.e(TAG, "Connection test failed", e);
            response.success = false;
            response.message = e.getMessage();
        }

        return response;
    }

    /**
     * Send chat request
     */
    public MiniMaxResponse sendChatRequest(String message, String systemPrompt, int maxTokens, float temperature) {
        MiniMaxResponse response = new MiniMaxResponse();

        try {
            StringBuilder json = new StringBuilder();
            json.append("{\"model\":\"").append(model).append("\",");
            json.append("\"messages\":[");

            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                json.append("{\"role\":\"system\",\"content\":\"").append(escapeJson(systemPrompt)).append("\"},");
            }

            json.append("{\"role\":\"user\",\"content\":\"").append(escapeJson(message)).append("\"}]");
            json.append(",\"max_tokens\":").append(maxTokens);
            json.append(",\"temperature\":").append(temperature);
            json.append("}");

            RequestBody body = RequestBody.create(json.toString(), MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(BASE_URL + CHAT_ENDPOINT)
                    .post(body)
                    .build();

            Response httpResponse = httpClient.newCall(request).execute();

            if (httpResponse.isSuccessful() && httpResponse.body() != null) {
                String responseBody = httpResponse.body().string();
                response.success = true;
                response.message = extractContent(responseBody);
            } else {
                response.success = false;
                response.message = "API Error: " + httpResponse.code();
            }

            httpResponse.close();

        } catch (Exception e) {
            Log.e(TAG, "Chat request failed", e);
            response.success = false;
            response.message = e.getMessage();
        }

        return response;
    }

    /**
     * Get available models
     */
    public String[] getAvailableModels() {
        return new String[]{
                "abab6.5-chat",
                "abab6.5s-chat",
                "abab5.5-chat"
        };
    }

    /**
     * Extract content from API response
     */
    private String extractContent(String responseBody) {
        try {
            int contentStart = responseBody.indexOf("\"content\":\"");
            if (contentStart == -1) return responseBody;

            contentStart += 11;
            int contentEnd = responseBody.indexOf("\"", contentStart);
            if (contentEnd == -1) return responseBody;

            String content = responseBody.substring(contentStart, contentEnd);
            return content.replace("\\n", "\n").replace("\\\"", "\"");
        } catch (Exception e) {
            return responseBody;
        }
    }

    /**
     * Escape special characters for JSON
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    /**
     * Response container
     */
    public static class MiniMaxResponse {
        public boolean success;
        public String message;
    }
}
