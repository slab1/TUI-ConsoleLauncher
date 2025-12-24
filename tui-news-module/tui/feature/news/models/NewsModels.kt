package tui.feature.news.models

import com.google.gson.annotations.SerializedName

/**
 * Request model for MiniMax API chat completion
 */
data class MiniMaxRequest(
    @SerializedName("model")
    val model: String = "abab6.5-chat",
    
    @SerializedName("messages")
    val messages: List<ChatMessage>,
    
    @SerializedName("temperature")
    val temperature: Double = 0.7,
    
    @SerializedName("max_tokens")
    val maxTokens: Int = 1024
)

/**
 * Chat message structure for API requests
 */
data class ChatMessage(
    @SerializedName("role")
    val role: String,
    
    @SerializedName("content")
    val content: String
)

/**
 * Response model from MiniMax API
 */
data class MiniMaxResponse(
    @SerializedName("id")
    val id: String?,
    
    @SerializedName("object")
    val objectType: String?,
    
    @SerializedName("created")
    val created: Long?,
    
    @SerializedName("model")
    val model: String?,
    
    @SerializedName("choices")
    val choices: List<Choice>?,
    
    @SerializedName("usage")
    val usage: Usage?
)

/**
 * Choice inside MiniMax response
 */
data class Choice(
    @SerializedName("index")
    val index: Int?,
    
    @SerializedName("message")
    val message: ChatMessage?,
    
    @SerializedName("finish_reason")
    val finishReason: String?
)

/**
 * Token usage statistics
 */
data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int?,
    
    @SerializedName("completion_tokens")
    val completionTokens: Int?,
    
    @SerializedName("total_tokens")
    val totalTokens: Int?
)
