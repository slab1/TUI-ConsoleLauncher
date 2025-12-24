package tui.feature.news

import android.content.Context
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import tui.feature.news.models.ChatMessage
import tui.feature.news.models.MiniMaxRequest
import tui.feature.news.models.MiniMaxResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Service layer for MiniMax API communication
 * Handles all network requests to the MiniMax AI platform
 */
class MiniMaxService(private val context: Context) {

    companion object {
        private const val TAG = "MiniMaxService"
        private const val BASE_URL = "https://api.minimax.chat/v1"
        private const val TIMEOUT_SECONDS = 30L
    }

    private val gson = Gson()
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    private val apiKey: String by lazy {
        NewsConfig.getApiKey(context)
    }

    /**
     * Fetches news summary from MiniMax AI
     * @param topic The news topic to query
     * @param detailed Whether to provide detailed summaries
     * @return Result containing the news text or an error
     */
    suspend fun getNewsSummary(topic: String, detailed: Boolean = false): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val systemPrompt = buildSystemPrompt(detailed)
                val userPrompt = buildUserPrompt(topic, detailed)

                val messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = userPrompt)
                )

                val request = MiniMaxRequest(messages = messages)
                val jsonBody = gson.toJson(request)

                val mediaType = "application/json".toMediaType()
                val requestBody = jsonBody.toRequestBody(mediaType)

                val httpRequest = Request.Builder()
                    .url("$BASE_URL/text/chatcompletion_v2")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                val response = client.newCall(httpRequest).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val miniMaxResponse = gson.fromJson(responseBody, MiniMaxResponse::class.java)
                        val content = miniMaxResponse.choices?.firstOrNull()?.message?.content
                        if (content != null) {
                            Result.success(content)
                        } else {
                            Result.failure(IOException("Empty response from MiniMax API"))
                        }
                    } else {
                        Result.failure(IOException("Empty response body"))
                    }
                } else {
                    val errorMsg = "API Error: ${response.code} - ${response.message}"
                    Log.e(TAG, errorMsg)
                    Result.failure(IOException(errorMsg))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Builds the system prompt that defines the AI's behavior as a news aggregator
     */
    private fun buildSystemPrompt(detailed: Boolean): String {
        return if (detailed) {
            """
            You are a professional news analyst for a terminal-based interface. 
            Your output must be plain text optimized for CLI display.
            
            Rules:
            - NO markdown formatting (no **, ##, >, etc.)
            - Use simple ASCII characters for formatting
            - Maximum 60 characters per line
            - Provide 3-5 in-depth news stories with context
            - Each story should have: headline, key details, and significance
            - Separate stories with blank lines
            - End with source attribution if available
            """.trimIndent()
        } else {
            """
            You are a concise news aggregator for a terminal-based interface.
            Your output must be plain text optimized for CLI display.
            
            Rules:
            - NO markdown formatting (no **, ##, >, etc.)
            - Use simple bullet points (•) or hyphens (-)
            - Maximum 50 characters per line
            - Provide 5-8 brief news headlines with one-line summaries
            - Keep each summary under 20 words
            - Group related stories under topic headers
            """.trimIndent()
        }
    }

    /**
     * Builds the user prompt based on the requested news topic
     */
    private fun buildUserPrompt(topic: String, detailed: Boolean): String {
        val depth = if (detailed) "Provide detailed analysis" else "Brief summaries"
        return "Get the latest news about '$topic'. $depth. Focus on recent developments from the last 24-48 hours."
    }

    /**
     * Validates the API key configuration
     */
    fun validateApiKey(): Boolean {
        return apiKey.isNotBlank() && apiKey.length > 10
    }
}
