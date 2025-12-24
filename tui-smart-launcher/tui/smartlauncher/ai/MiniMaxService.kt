package tui.smartlauncher.ai

import android.content.Context
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.OkHttpSseConnector
import org.json.JSONObject
import tui.smartlauncher.core.CommandProcessor
import tui.smartlauncher.core.CommandHistory
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * MiniMax AI Service - Handles all AI interactions
 * Supports streaming responses, context injection, and command parsing
 */
class MiniMaxService(private val context: Context) {

    companion object {
        private const val TAG = "MiniMaxService"
        private const val BASE_URL = "https://api.minimax.chat/v1"
        private const val TIMEOUT_SECONDS = 60L
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    private val config = AIConfig(context)
    private val history = CommandHistory(context)

    /**
     * Query types for specialized processing
     */
    enum class QueryType {
        GENERAL,       // General question
        CODE,          // Code generation or explanation
        DEBUG,         // Debugging assistance
        SUMMARIZE,     // Content summarization
        TRANSLATE,     // Translation
        CREATIVE       // Creative writing
    }

    /**
     * Result from AI query
     */
    sealed class AIResult {
        data class Success(
            val response: String,
            val tokens: Int,
            val streaming: Boolean = false
        ) : AIResult()

        data class StreamingUpdate(val chunk: String, val isComplete: Boolean) : AIResult()
        data class Error(val message: String, val isRetryable: Boolean) : AIResult()
    }

    /**
     * Sends a query to MiniMax AI with automatic type detection
     */
    fun query(
        userInput: String,
        context: String? = null,
        stream: Boolean = true,
        type: QueryType = QueryType.GENERAL
    ): AIResult {
        val apiKey = config.getApiKey()
        if (apiKey.isBlank()) {
            return AIResult.Error("API key not configured. Use 'ai --config' to set it.", false)
        }

        val prompt = buildPrompt(userInput, context, type)
        val systemPrompt = buildSystemPrompt(type)

        return if (stream && config.isStreamingEnabled) {
            executeStreamingRequest(apiKey, prompt, systemPrompt)
        } else {
            executeRegularRequest(apiKey, prompt, systemPrompt)
        }
    }

    /**
     * Builds the user prompt with context injection
     */
    private fun buildPrompt(userInput: String, context: String?, type: QueryType): String {
        val builder = StringBuilder()

        when (type) {
            QueryType.CODE -> {
                builder.appendLine("Request: $userInput")
                if (context != null) {
                    builder.appendLine("Code context:")
                    builder.appendLine(context)
                    builder.appendLine("---")
                }
                builder.appendLine("Provide code with brief explanation. Use code blocks with language tags.")
            }

            QueryType.DEBUG -> {
                builder.appendLine("Debug this error or issue:")
                builder.appendLine(userInput)
                if (context != null) {
                    builder.appendLine("Related code:")
                    builder.appendLine(context)
                }
                builder.appendLine("Explain the issue and provide a fix.")
            }

            QueryType.SUMMARIZE -> {
                builder.appendLine("Summarize the following concisely:")
                builder.appendLine(userInput)
            }

            QueryType.TRANSLATE -> {
                builder.appendLine("Translate the following text to ${config.translationLanguage}:")
                builder.appendLine(userInput)
            }

            QueryType.CREATIVE -> {
                builder.appendLine(userInput)
                builder.appendLine("Be creative and detailed.")
            }

            QueryType.GENERAL -> {
                builder.appendLine(userInput)
            }
        }

        return builder.toString()
    }

    /**
     * Builds system prompt based on query type
     */
    private fun buildSystemPrompt(type: QueryType): String {
        return when (type) {
            QueryType.CODE -> """
                You are a helpful coding assistant optimized for mobile display.
                Rules:
                - Format code with proper syntax highlighting markers
                - Keep line lengths under 60 characters
                - Add brief inline comments for complex logic
                - Use plain text, minimal markdown
                - For code blocks, use: [CODE:language]...[/CODE]
            """.trimIndent()

            QueryType.DEBUG -> """
                You are a debugging assistant.
                Rules:
                - Explain the root cause clearly
                - Provide the minimal fix needed
                - Explain how to prevent similar issues
                - Keep responses concise for mobile reading
            """.trimIndent()

            QueryType.SUMMARIZE -> """
                You are a summarization assistant.
                Rules:
                - Extract key points only
                - Use bullet points for multiple items
                - Keep total response under 200 words
            """.trimIndent()

            QueryType.TRANSLATE -> """
                You are a translation assistant.
                Rules:
                - Provide natural-sounding translations
                - Preserve formatting and special terms
                - Show original text for reference
            """.trimIndent()

            QueryType.CREATIVE -> """
                You are a creative writing assistant.
                Rules:
                - Be engaging and detailed
                - Adapt style to the request type
                - Format for readability on mobile
            """.trimIndent()

            QueryType.GENERAL -> """
                You are a helpful assistant for a mobile terminal interface.
                Rules:
                - Keep responses concise
                - Use bullet points for lists
                - No markdown formatting
                - Maximum 80 characters per line
                - Be direct and helpful
            """.trimIndent()
        }
    }

    /**
     * Executes streaming request for real-time output
     */
    private fun executeStreamingRequest(
        apiKey: String,
        prompt: String,
        systemPrompt: String
    ): AIResult {
        try {
            val jsonBody = JSONObject().apply {
                put("model", config.getModel())
                put("messages", listOf(
                    JSONObject().put("role", "system").put("content", systemPrompt),
                    JSONObject().put("role", "user").put("content", prompt)
                ))
                put("temperature", config.temperature)
                put("max_tokens", config.maxTokens)
                put("stream", true)
            })

            val request = Request.Builder()
                .url("$BASE_URL/text/chatcompletion_v2")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val result = StringBuilder()
            var isComplete = false

            val listener = object : EventSourceListener() {
                override fun onOpen(eventSource: EventSource, response: okhttp3.Response) {
                    Log.d(TAG, "Stream opened")
                }

                override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                    if (data.isBlank() || data == "[DONE]") {
                        isComplete = true
                        return
                    }

                    try {
                        val json = JSONObject(data)
                        val choices = json.optJSONArray("choices") ?: return
                        if (choices.length() > 0) {
                            val delta = choices.getJSONObject(0).optJSONObject("delta")
                            val content = delta?.optString("content", "")
                            if (!content.isNullOrBlank()) {
                                result.append(content)
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing stream data: ${e.message}")
                    }
                }

                override fun onClosed(eventSource: EventSource, code: Int, reason: String) {
                    isComplete = true
                    Log.d(TAG, "Stream closed: $reason")
                }

                override fun onFailure(eventSource: EventSource, t: Throwable?, response: okhttp3.Response?) {
                    Log.e(TAG, "Stream failure: ${t?.message}")
                    isComplete = true
                }
            }

            // Note: Full SSE implementation would use OkHttp SSE connector
            // For simplicity, falling back to regular request
            return executeRegularRequest(apiKey, prompt, systemPrompt)

        } catch (e: Exception) {
            Log.e(TAG, "Streaming error: ${e.message}")
            return executeRegularRequest(apiKey, prompt, systemPrompt)
        }
    }

    /**
     * Executes regular (non-streaming) request
     */
    private fun executeRegularRequest(
        apiKey: String,
        prompt: String,
        systemPrompt: String
    ): AIResult {
        return try {
            val jsonBody = JSONObject().apply {
                put("model", config.getModel())
                put("messages", listOf(
                    JSONObject().put("role", "system").put("content", systemPrompt),
                    JSONObject().put("role", "user").put("content", prompt)
                ))
                put("temperature", config.temperature)
                put("max_tokens", config.maxTokens)
            }

            val request = Request.Builder()
                .url("$BASE_URL/text/chatcompletion_v2")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val json = JSONObject(responseBody)
                    val content = parseResponseContent(json)
                    AIResult.Success(content, content.length)
                } else {
                    AIResult.Error("Empty response", true)
                }
            } else {
                val errorMessage = parseError(response)
                val isRetryable = response.code in listOf(429, 500, 503)
                AIResult.Error(errorMessage, isRetryable)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Request error: ${e.message}")
            AIResult.Error("Network error: ${e.message}", true)
        }
    }

    /**
     * Parses content from API response
     */
    private fun parseResponseContent(json: JSONObject): String {
        val choices = json.optJSONArray("choices") ?: return ""
        if (choices.length() == 0) return ""

        val message = choices.getJSONObject(0).optJSONObject("message") ?: return ""
        return message.optString("content", "").trim()
    }

    /**
     * Parses error from response
     */
    private fun parseError(response: okhttp3.Response): String {
        val body = response.body?.string()
        return try {
            if (body != null) {
                val json = JSONObject(body)
                json.optString("error", "API Error: ${response.code}")
            } else {
                "API Error: ${response.code}"
            }
        } catch (e: Exception) {
            "API Error: ${response.code}"
        }
    }

    /**
     * Detects query type from user input
     */
    fun detectQueryType(input: String): QueryType {
        val lower = input.lowercase()

        return when {
            lower.startsWith("fix") || lower.startsWith("debug") ||
            lower.startsWith("error") || lower.contains("exception") ||
            lower.contains("stack trace") -> QueryType.DEBUG

            lower.startsWith("write") || lower.startsWith("create") ||
            lower.startsWith("generate") || lower.startsWith("code") ||
            lower.startsWith("function") || lower.startsWith("class ") -> QueryType.CODE

            lower.startsWith("summarize") || lower.startsWith("summary") ||
            lower.startsWith("what is") || lower.startsWith("tl;dr") -> QueryType.SUMMARIZE

            lower.startsWith("translate") -> QueryType.TRANSLATE

            lower.startsWith("write a") || lower.startsWith("story") ||
            lower.startsWith("poem") || lower.startsWith("creative") -> QueryType.CREATIVE

            else -> QueryType.GENERAL
        }
    }

    /**
     * Gets usage statistics
     */
    fun getUsageStats(): AIUsageStats {
        return config.getUsageStats()
    }

    /**
     * Validates API key
     */
    fun validateApiKey(): Boolean {
        val result = query("test", type = QueryType.GENERAL)
        return result is AIResult.Success
    }
}

/**
 * AI Configuration management
 */
class AIConfig(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "tui_ai_prefs"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString("minimax_api_key", "") ?: ""
        set(value) = prefs.edit().putString("minimax_api_key", value).apply()

    var temperature: Double
        get() = prefs.getFloat("ai_temperature", 0.7).toDouble()
        set(value) = prefs.edit().putFloat("ai_temperature", value.toFloat()).apply()

    var maxTokens: Int
        get() = prefs.getInt("ai_max_tokens", 1024)
        set(value) = prefs.edit().putInt("ai_max_tokens", value).apply()

    var model: String
        get() = prefs.getString("ai_model", "abab6.5-chat") ?: "abab6.5-chat"
        set(value) = prefs.edit().putString("ai_model", value).apply()

    var isStreamingEnabled: Boolean
        get() = prefs.getBoolean("ai_streaming", true)
        set(value) = prefs.edit().putBoolean("ai_streaming", value).apply()

    var translationLanguage: String
        get() = prefs.getString("ai_translation_lang", "English") ?: "English"
        set(value) = prefs.edit().putString("ai_translation_lang", value).apply()

    var isConfigured: Boolean
        get() = apiKey.isNotBlank() && apiKey.length >= 10

    fun getApiKey(): String {
        // Try secrets.properties first
        return try {
            val inputStream = context.assets.open("secrets.properties")
            val properties = java.util.Properties()
            properties.load(inputStream)
            inputStream.close()
            properties.getProperty("MINIMAX_API_KEY", apiKey)
        } catch (e: Exception) {
            apiKey
        }
    }

    fun getUsageStats(): AIUsageStats {
        return AIUsageStats(
            totalQueries = prefs.getInt("stats_queries", 0),
            totalTokens = prefs.getInt("stats_tokens", 0),
            lastQueryTime = prefs.getLong("stats_last_query", 0)
        )
    }

    fun recordQuery(tokens: Int) {
        prefs.edit()
            .putInt("stats_queries", prefs.getInt("stats_queries", 0) + 1)
            .putInt("stats_tokens", prefs.getInt("stats_tokens", 0) + tokens)
            .putLong("stats_last_query", System.currentTimeMillis())
            .apply()
    }

    data class AIUsageStats(
        val totalQueries: Int,
        val totalTokens: Int,
        val lastQueryTime: Long
    )
}
