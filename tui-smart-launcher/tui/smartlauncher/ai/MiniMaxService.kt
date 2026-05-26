package tui.smartlauncher.ai

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import tui.smartlauncher.core.CommandHistory
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

// ═══════════════════════════════════════════════════════════════
// CORE DATA TYPES
// ═══════════════════════════════════════════════════════════════

data class TokenUsage(val inputTokens: Int, val outputTokens: Int, val cost: Double)
data class ChatMessage(val role: String, val content: String)
data class AIRequest(val messages: List<ChatMessage>, val temperature: Double = 0.7,
                     val maxTokens: Int = 1024, val stream: Boolean = false)

sealed class AIResult {
    data class Success(val response: String, val tokens: Int) : AIResult()
    data class Error(val message: String, val isRetryable: Boolean = false) : AIResult()
}

sealed class AIEvent {
    data class Thinking(val message: String) : AIEvent()
    data class Chunk(val text: String) : AIEvent()
    data class Complete(val fullResponse: String, val tokens: TokenUsage) : AIEvent()
    data class Error(val message: String) : AIEvent()
    data class CommandSuggestion(val command: String, val explanation: String) : AIEvent()
    data class ContextInfo(val info: String) : AIEvent()
}

enum class AIIntent { COMMAND, CODE, DEBUG, EXPLAIN, SUMMARIZE, TRANSLATE, CREATIVE, CHAT }
data class Persona(val name: String, val systemPrompt: String, val description: String)
data class UsageStats(val totalQueries: Int, val totalInputTokens: Int, val totalOutputTokens: Int,
                      val totalCost: Double, val lastQueryTime: Long)

// ═══════════════════════════════════════════════════════════════
// AI PROVIDER INTERFACE + ABSTRACT BASE
// ═══════════════════════════════════════════════════════════════

interface AIProvider {
    val name: String
    val isConfigured: Boolean
    fun query(request: AIRequest): AIResult
    fun stream(request: AIRequest, onChunk: (String, Boolean) -> Unit): AIResult
}

abstract class BaseProvider(protected val client: OkHttpClient) : AIProvider {
    protected abstract val apiKey: String
    protected abstract val modelName: String
    protected abstract val baseUrl: String
    protected abstract val authHeader: String // "Bearer" or "x-api-key"

    protected open fun buildRequestBody(request: AIRequest, stream: Boolean): String {
        val root = JSONObject()
        root.put("model", modelName)
        root.put("temperature", request.temperature)
        root.put("max_tokens", request.maxTokens)
        root.put("stream", stream)
        root.put("messages", request.messages.map { msg ->
            JSONObject().apply { put("role", msg.role); put("content", msg.content) }
        })
        return root.toString()
    }

    protected open fun parseSuccessResponse(body: String, request: AIRequest): AIResult {
        val json = JSONObject(body)
        val choices = json.optJSONArray("choices") ?: return AIResult.Error("No choices", false)
        if (choices.length() == 0) return AIResult.Error("Empty choices", false)
        val content = choices.getJSONObject(0).optJSONObject("message")
            ?.optString("content", "")?.trim() ?: ""
        val usage = json.optJSONObject("usage")
        val inTokens = usage?.optInt("prompt_tokens", 0) ?: estimateTokens(messagesToText(request.messages))
        val outTokens = usage?.optInt("completion_tokens", 0) ?: estimateTokens(content)
        return AIResult.Success(content, inTokens + outTokens)
    }

    protected open fun parseErrorBody(body: String?): String {
        if (body == null) return "Unknown error"
        return try {
            JSONObject(body).optJSONObject("error")?.optString("message", null)
                ?: JSONObject(body).optString("error", "HTTP error")
        } catch (_: Exception) { "HTTP error" }
    }

    private val requestHeaders: Map<String, String>
        get() = if (authHeader == "x-api-key") {
            mapOf("x-api-key" to apiKey, "anthropic-version" to "2023-06-01",
                  "Content-Type" to "application/json")
        } else {
            mapOf("Authorization" to "Bearer $apiKey", "Content-Type" to "application/json")
        }

    fun executeHttp(request: AIRequest, stream: Boolean): AIResult {
        return try {
            val body = buildRequestBody(request, stream)
            val httpReq = Request.Builder().url(baseUrl).apply {
                requestHeaders.forEach { (k, v) -> header(k, v) }
                post(body.toRequestBody("application/json".toMediaType()))
            }.build()

            val response = client.newCall(httpReq).execute()
            if (response.isSuccessful) {
                parseSuccessResponse(response.body?.string() ?: "", request)
            } else {
                val errBody = response.body?.string()
                AIResult.Error(parseErrorBody(errBody), response.code in listOf(429, 500, 502, 503))
            }
        } catch (e: Exception) {
            Log.e(name, "Request failed: ${e.message}")
            AIResult.Error("${name} error: ${e.message}", true)
        }
    }

    fun executeSse(request: AIRequest, onChunk: (String, Boolean) -> Unit): AIResult {
        return try {
            val body = buildRequestBody(request, stream = true)
            val httpReq = Request.Builder().url(baseUrl).apply {
                requestHeaders.forEach { (k, v) -> header(k, v) }
                header("Accept", "text/event-stream")
                post(body.toRequestBody("application/json".toMediaType()))
            }.build()

            val sseClient = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS).build()

            val factory = EventSources.createFactory(sseClient)
            val full = StringBuilder()
            var result: AIResult? = null
            val latch = CountDownLatch(1)

            factory.newEventSource(httpReq, object : EventSourceListener() {
                override fun onEvent(es: EventSource, id: String?, type: String?, data: String) {
                    if (data == "[DONE]" || data.isBlank()) return
                    val text = parseSseContent(data, type)
                    if (text != null) {
                        synchronized(full) { full.append(text) }
                        onChunk(text, false)
                    }
                }
                override fun onFailure(es: EventSource, t: Throwable?, resp: okhttp3.Response?) {
                    Log.e(name, "SSE fail: ${t?.message}")
                    result = AIResult.Error(t?.message ?: "Stream error (${resp?.code})",
                        resp?.code != 401 && resp?.code != 403)
                    latch.countDown()
                }
                override fun onClosed(es: EventSource) {
                    val text = synchronized(full) { full.toString() }
                    onChunk("", true)
                    result = AIResult.Success(text, estimateTokens(text))
                    latch.countDown()
                }
            })
            if (!latch.await(120, TimeUnit.SECONDS))
                AIResult.Error("Stream timed out", true)
            else result ?: AIResult.Error("No result", false)
        } catch (e: Exception) {
            Log.e(name, "SSE error: ${e.message}")
            AIResult.Error("${name} stream error: ${e.message}", true)
        }
    }

    override fun query(request: AIRequest): AIResult = executeHttp(request, stream = false)
    override fun stream(request: AIRequest, onChunk: (String, Boolean) -> Unit): AIResult = executeSse(request, onChunk)
}

// ═══════════════════════════════════════════════════════════════
// PROVIDER IMPLEMENTATIONS
// ═══════════════════════════════════════════════════════════════

class MiniMaxProvider(client: OkHttpClient, override val apiKey: String,
                      override val modelName: String) : BaseProvider(client) {
    override val name: String get() = "minimax"
    override val isConfigured: Boolean get() = apiKey.isNotBlank() && apiKey.length >= 10
    override val baseUrl: String get() = "https://api.minimax.chat/v1/text/chatcompletion_v2"
    override val authHeader: String get() = "Bearer"
}

class OpenAIProvider(client: OkHttpClient, override val apiKey: String,
                     override val modelName: String) : BaseProvider(client) {
    override val name: String get() = "openai"
    override val isConfigured: Boolean get() = apiKey.isNotBlank() && apiKey.startsWith("sk-")
    override val baseUrl: String get() = "https://api.openai.com/v1/chat/completions"
    override val authHeader: String get() = "Bearer"
    companion object { val MODELS = listOf("gpt-4", "gpt-4-turbo", "gpt-3.5-turbo") }
}

class AnthropicProvider(client: OkHttpClient, override val apiKey: String,
                        override val modelName: String) : BaseProvider(client) {
    override val name: String get() = "anthropic"
    override val isConfigured: Boolean get() = apiKey.isNotBlank() && apiKey.startsWith("sk-ant-")
    override val baseUrl: String get() = "https://api.anthropic.com/v1/messages"
    override val authHeader: String get() = "x-api-key"

    companion object {
        val MODELS = listOf("claude-3-opus-20240229", "claude-3-sonnet-20240229", "claude-3-haiku-20240307")
    }

    override fun buildRequestBody(request: AIRequest, stream: Boolean): String {
        val root = JSONObject()
        root.put("model", modelName)
        root.put("max_tokens", request.maxTokens)
        if (request.temperature != 0.7) root.put("temperature", request.temperature)
        root.put("stream", stream)
        val sys = request.messages.filter { it.role == "system" }
        if (sys.isNotEmpty()) root.put("system", sys.joinToString("\n") { it.content })
        root.put("messages", request.messages.filter { it.role != "system" }.map { msg ->
            JSONObject().apply { put("role", msg.role); put("content", msg.content) }
        })
        return root.toString()
    }

    override fun parseSuccessResponse(body: String, request: AIRequest): AIResult {
        val json = JSONObject(body)
        val content = json.optJSONArray("content")?.let { arr ->
            (0 until arr.length()).mapNotNull { i ->
                val block = arr.getJSONObject(i)
                if (block.optString("type") == "text") block.optString("text", null) else null
            }.joinToString("")
        } ?: ""
        val usage = json.optJSONObject("usage")
        val inTokens = usage?.optInt("input_tokens", 0) ?: estimateTokens(messagesToText(request.messages))
        val outTokens = usage?.optInt("output_tokens", 0) ?: estimateTokens(content)
        return AIResult.Success(content, inTokens + outTokens)
    }

    override fun parseErrorBody(body: String?): String {
        if (body == null) return "Unknown error"
        return try {
            JSONObject(body).optJSONObject("error")?.optString("message", "HTTP error") ?: "HTTP error"
        } catch (_: Exception) { "HTTP error" }
    }
}

// ═══════════════════════════════════════════════════════════════
// FREE / OPEN PROVIDERS (OpenAI-compatible)
// ═══════════════════════════════════════════════════════════════

/**
 * Groq — Free tier, no credit card needed (gsk_* API key).
 * https://console.groq.com  →  Sign up → API Keys
 * Models: llama-3.3-70b-versatile, llama-3.1-8b-instant,
 *         llama-4-scout-17b-16e-instruct, deepseek-r1-distill-llama-70b,
 *         qwen-qwq-32b, gemma2-9b-it
 */
class GroqProvider(client: OkHttpClient, override val apiKey: String,
                   override val modelName: String) : BaseProvider(client) {
    override val name: String get() = "groq"
    override val isConfigured: Boolean get() = apiKey.isNotBlank() && apiKey.startsWith("gsk_")
    override val baseUrl: String get() = "https://api.groq.com/openai/v1/chat/completions"
    override val authHeader: String get() = "Bearer"
    companion object {
        val MODELS = listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant",
            "llama-4-scout-17b-16e-instruct", "deepseek-r1-distill-llama-70b",
            "qwen-qwq-32b", "gemma2-9b-it")
    }
}

/**
 * Cerebras — Free tier, 1M tokens/day, no credit card.
 * https://cloud.cerebras.ai  →  Sign up → API Keys
 * Models: gpt-oss-120b, llama3.1-8b, qwen-3-235b-a22b-instruct
 */
class CerebrasProvider(client: OkHttpClient, override val apiKey: String,
                       override val modelName: String) : BaseProvider(client) {
    override val name: String get() = "cerebras"
    override val isConfigured: Boolean get() = apiKey.isNotBlank() && apiKey.length >= 8
    override val baseUrl: String get() = "https://api.cerebras.ai/v1/chat/completions"
    override val authHeader: String get() = "Bearer"
    companion object {
        val MODELS = listOf("gpt-oss-120b", "llama3.1-8b", "qwen-3-235b-a22b-instruct")
    }
}

/**
 * OpenRouter — Free tier, 28+ free models, no credit card needed.
 * https://openrouter.ai  →  Sign up → Keys → sk-or-...
 * Models use :free suffix, e.g. "meta-llama/llama-3.3-70b-instruct:free"
 */
class OpenRouterProvider(client: OkHttpClient, override val apiKey: String,
                         override val modelName: String) : BaseProvider(client) {
    override val name: String get() = "openrouter"
    override val isConfigured: Boolean get() = apiKey.isNotBlank() && apiKey.length >= 10
    override val baseUrl: String get() = "https://openrouter.ai/api/v1/chat/completions"
    override val authHeader: String get() = "Bearer"
    companion object {
        val MODELS = listOf(
            "meta-llama/llama-3.3-70b-instruct:free",
            "google/gemini-2.0-flash-exp:free",
            "deepseek/deepseek-r1:free",
            "qwen/qwen3-coder-480b:free",
            "minimax/minimax-m2.5:free",
            "nousresearch/hermes-3-llama-3.1-405b:free",
            "google/gemma-3-27b-it:free"
        )
    }

    // OpenRouter sends extra headers for identification
    override fun executeHttp(request: AIRequest, stream: Boolean): AIResult {
        return try {
            val body = buildRequestBody(request, stream)
            val httpReq = Request.Builder().url(baseUrl).apply {
                requestHeaders.forEach { (k, v) -> header(k, v) }
                header("HTTP-Referer", "https://github.com/tui-smart-launcher")
                header("X-Title", "TUI-Smart-Launcher")
                post(body.toRequestBody("application/json".toMediaType()))
            }.build()
            val response = client.newCall(httpReq).execute()
            if (response.isSuccessful) {
                parseSuccessResponse(response.body?.string() ?: "", request)
            } else {
                val errBody = response.body?.string()
                AIResult.Error(parseErrorBody(errBody), response.code in listOf(429, 500, 502, 503))
            }
        } catch (e: Exception) {
            Log.e(name, "Request failed: ${e.message}")
            AIResult.Error("${name} error: ${e.message}", true)
        }
    }

    override fun executeSse(request: AIRequest, onChunk: (String, Boolean) -> Unit): AIResult {
        return try {
            val body = buildRequestBody(request, stream = true)
            val httpReq = Request.Builder().url(baseUrl).apply {
                requestHeaders.forEach { (k, v) -> header(k, v) }
                header("Accept", "text/event-stream")
                header("HTTP-Referer", "https://github.com/tui-smart-launcher")
                header("X-Title", "TUI-Smart-Launcher")
                post(body.toRequestBody("application/json".toMediaType()))
            }.build()
            val sseClient = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS).build()
            val factory = EventSources.createFactory(sseClient)
            val full = StringBuilder()
            var result: AIResult? = null
            val latch = CountDownLatch(1)
            factory.newEventSource(httpReq, object : EventSourceListener() {
                override fun onEvent(es: EventSource, id: String?, type: String?, data: String) {
                    if (data == "[DONE]" || data.isBlank()) return
                    val text = parseSseContent(data, type)
                    if (text != null) {
                        synchronized(full) { full.append(text) }
                        onChunk(text, false)
                    }
                }
                override fun onFailure(es: EventSource, t: Throwable?, resp: okhttp3.Response?) {
                    Log.e(name, "SSE fail: ${t?.message}")
                    result = AIResult.Error(t?.message ?: "Stream error (${resp?.code})",
                        resp?.code != 401 && resp?.code != 403)
                    latch.countDown()
                }
                override fun onClosed(es: EventSource) {
                    val text = synchronized(full) { full.toString() }
                    onChunk("", true)
                    result = AIResult.Success(text, estimateTokens(text))
                    latch.countDown()
                }
            })
            if (!latch.await(120, TimeUnit.SECONDS))
                AIResult.Error("Stream timed out", true)
            else result ?: AIResult.Error("No result", false)
        } catch (e: Exception) {
            Log.e(name, "SSE error: ${e.message}")
            AIResult.Error("${name} stream error: ${e.message}", true)
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// SSE PARSING (shared)
// ═══════════════════════════════════════════════════════════════

private fun parseSseContent(data: String, eventType: String?): String? {
    return try {
        val json = JSONObject(data)
        val choices = json.optJSONArray("choices")
        if (choices != null && choices.length() > 0) {
            return choices.getJSONObject(0).optJSONObject("delta")?.optString("content", null)
        }
        if (eventType == "content_block_delta") {
            return json.optJSONObject("delta")?.optString("text", null)
        }
        if (eventType == "content_block_start") {
            return json.optJSONObject("content_block")?.optString("text", null)?.takeIf { it.isNotEmpty() }
        }
        null
    } catch (_: Exception) { null }
}

// ═══════════════════════════════════════════════════════════════
// CONVERSATION MEMORY
// ═══════════════════════════════════════════════════════════════

class ConversationMemory(var maxTurns: Int = 20) {
    private val messages = mutableListOf<ChatMessage>()
    private var systemPrompt = ""

    fun setSystem(prompt: String) {
        systemPrompt = prompt
        val idx = messages.indexOfFirst { it.role == "system" }
        val msg = ChatMessage("system", prompt)
        if (idx >= 0) messages[idx] = msg else messages.add(0, msg)
        trim()
    }
    fun addUser(content: String) { messages.add(ChatMessage("user", content)); trim() }
    fun addAssistant(content: String) { messages.add(ChatMessage("assistant", content)); trim() }
    fun getMessages(): List<ChatMessage> = messages.toList()
    fun clear() { messages.clear(); systemPrompt = "" }
    fun isEmpty(): Boolean = messages.isEmpty()
    fun size(): Int = messages.size

    private fun trim() {
        val sys = messages.filter { it.role == "system" }
        val others = messages.filter { it.role != "system" }
        if (others.size > maxTurns) {
            messages.clear()
            messages.addAll(sys)
            messages.addAll(others.drop(others.size - maxTurns))
        }
    }

    fun toJson(): String = Gson().toJson(mapOf(
        "maxTurns" to maxTurns, "systemPrompt" to systemPrompt,
        "messages" to messages.map { mapOf("role" to it.role, "content" to it.content) }
    ))

    fun fromJson(json: String) {
        try {
            @Suppress("UNCHECKED_CAST")
            val map = Gson().fromJson(json, Map::class.java) as? Map<String, Any> ?: return
            maxTurns = (map["maxTurns"] as? Double)?.toInt() ?: 20
            systemPrompt = map["systemPrompt"] as? String ?: ""
            messages.clear()
            (map["messages"] as? List<Map<String, String>>)?.forEach {
                messages.add(ChatMessage(it["role"] ?: "user", it["content"] ?: ""))
            }
        } catch (e: Exception) { Log.e("ConvMem", "Restore failed: ${e.message}") }
    }
}

// ═══════════════════════════════════════════════════════════════
// BUILT-IN PERSONAS
// ═══════════════════════════════════════════════════════════════

private val BUILT_IN_PERSONAS = listOf(
    Persona("default", "You are a helpful assistant for a mobile terminal interface. Keep responses concise. Use bullet points for lists. Maximum 80 characters per line. Be direct and helpful. Never use markdown formatting.", "Default helpful assistant"),
    Persona("teacher", "You are a patient teacher. Explain concepts simply and clearly. Use analogies and examples. Break down complex topics into easy steps. Assume the user is curious but not an expert.", "Patient teacher who explains concepts clearly"),
    Persona("code-reviewer", "You are a senior software engineer reviewing code. Be thorough but kind. Point out potential bugs, security issues, and performance problems. Suggest improvements with code examples. Format code in [CODE:language]...[/CODE] blocks. Prioritize correctness and maintainability.", "Senior engineer reviewing code"),
    Persona("concise", "Answer in 1-3 sentences maximum. Be extremely brief and direct. No explanations unless specifically asked. No bullet points. No formatting. Just the essential information.", "Ultra-concise answers in 1-3 sentences"),
    Persona("creative", "You are a creative writing assistant. Be engaging, vivid, and detailed. Use rich language and imagery. Adapt your style to match the user's request — whether story, poem, dialogue, or description.", "Creative writing assistant"),
    Persona("pirate", "Arrr, ye be talkin' to a pirate AI! Speak like a swashbucklin' pirate from the golden age of sail. Use pirate lingo — 'arrr', 'me hearties', 'avast', 'shiver me timbers', 'booty', 'doubloons'. Be helpful but make every response sound like it comes from a pirate captain. Refer to the user as 'me matey' or 'cap'n'.", "Arrr! A pirate AI, yarr!")
)

// ═══════════════════════════════════════════════════════════════
// RESPONSE CACHE
// ═══════════════════════════════════════════════════════════════

class ResponseCache(private val maxEntries: Int = 50) {
    data class Entry(val response: String, val timestamp: Long)
    private val cache = LinkedHashMap<String, Entry>(maxEntries, 0.75f, true)
    private var lastQ: String? = null
    private var lastR: String? = null

    @Synchronized
    fun get(key: String): String? {
        val nk = key.lowercase().trim()
        val e = cache[nk] ?: return null
        if (System.currentTimeMillis() - e.timestamp > TimeUnit.MINUTES.toMillis(5)) {
            cache.remove(nk); return null
        }
        return e.response
    }
    @Synchronized
    fun put(key: String, response: String) {
        val nk = key.lowercase().trim()
        if (cache.size >= maxEntries) cache.remove(cache.entries.firstOrNull()?.key)
        cache[nk] = Entry(response, System.currentTimeMillis())
        lastQ = key; lastR = response
    }
    @Synchronized
    fun clear() { cache.clear(); lastQ = null; lastR = null }
    @Synchronized
    fun getRecent(): Pair<String, String>? = lastQ?.let { q -> lastR?.let { q to it } }
    @Synchronized
    fun size(): Int = cache.size
}

// ═══════════════════════════════════════════════════════════════
// USAGE TRACKER
// ═══════════════════════════════════════════════════════════════

class UsageTracker(private val prefs: android.content.SharedPreferences) {
    companion object {
        private const val Q = "ust_queries"; private const val IT = "ust_in_tokens"
        private const val OT = "ust_out_tokens"; private const val C = "ust_cost"
        private const val LT = "ust_last_time"
    }
    private val costMap = mapOf(
        "minimax" to 0.008 to 0.008, "abab6.5-chat" to 0.008 to 0.008,
        "gpt-4" to 0.03 to 0.06, "gpt-4-turbo" to 0.01 to 0.03,
        "gpt-3.5-turbo" to 0.0015 to 0.002,
        "claude-3-opus" to 0.015 to 0.075, "claude-3-sonnet" to 0.003 to 0.015,
        "claude-3-haiku" to 0.00025 to 0.00125,
        // Free / open providers — $0 cost
        "groq" to 0.0 to 0.0, "llama" to 0.0 to 0.0,
        "cerebras" to 0.0 to 0.0, "gpt-oss" to 0.0 to 0.0,
        "openrouter" to 0.0 to 0.0, ":free" to 0.0 to 0.0
    )

    fun record(inTok: Int, outTok: Int, model: String) {
        val cost = getCostEstimate(inTok, outTok, model)
        prefs.edit().putInt(Q, prefs.getInt(Q, 0) + 1)
            .putInt(IT, prefs.getInt(IT, 0) + inTok)
            .putInt(OT, prefs.getInt(OT, 0) + outTok)
            .putFloat(C, prefs.getFloat(C, 0f) + cost.toFloat())
            .putLong(LT, System.currentTimeMillis()).apply()
    }

    fun getStats(): UsageStats = UsageStats(
        prefs.getInt(Q, 0), prefs.getInt(IT, 0), prefs.getInt(OT, 0),
        prefs.getFloat(C, 0f).toDouble(), prefs.getLong(LT, 0)
    )

    fun getCostEstimate(inTok: Int, outTok: Int, model: String): Double {
        val (inRate, outRate) = costMap.entries.firstOrNull { model.contains(it.key, true) }?.value
            ?: (0.01 to 0.02)
        return inRate * (inTok / 1000.0) + outRate * (outTok / 1000.0)
    }

    fun reset() = prefs.edit().remove(Q).remove(IT).remove(OT).remove(C).remove(LT).apply()
}

// ═══════════════════════════════════════════════════════════════
// INTENT DETECTION
// ═══════════════════════════════════════════════════════════════

fun detectIntent(input: String): AIIntent = when {
    matchesAny(input, Regex("^(find|search|list|show|create|make|build|delete|remove|copy|move|" +
        "zip|unzip|compress|download|install|run|execute|start|stop|kill|restart|backup|deploy|" +
        "mount|unmount|format|chmod|chown|grep|sort|count|print|scan|connect|disconnect|" +
        "enable|disable|launch|open)\\b", RegexOption.IGNORE_CASE)) -> AIIntent.COMMAND
    matchesAny(input, Regex("^(write|code|function|class|implement|refactor|optimize|compile|" +
        "debug.*code|code.*review|review.*code|generate.*code)\\b", RegexOption.IGNORE_CASE)) ||
    containsAny(input, listOf("function", "method", "class ", "interface", "import ", "public ",
        "private ", "def ", "fun ", "void ", "syntax", "algorithm", "data structure",
        "api", "[CODE:", "```", "programming")) -> AIIntent.CODE
    matchesAny(input, Regex("^(fix|debug|error|bug|crash|exception|stack.?trace|" +
        "why|what.*wrong|troubleshoot|issue)\\b", RegexOption.IGNORE_CASE)) ||
    containsAny(input, listOf("exception", "stack trace", "null pointer", "NPE",
        "failed", "failure", "not working", "broken", "logcat")) -> AIIntent.DEBUG
    matchesAny(input, Regex("^(what|how|why|when|where|which|explain|describe|" +
        "define|tell me|difference|compar)\\b", RegexOption.IGNORE_CASE)) &&
    !containsAny(input, listOf("fix", "debug", "error")) -> AIIntent.EXPLAIN
    matchesAny(input, Regex("^(summarize|summary|tl;dr|tl.?dr|recap|brief|" +
        "in short|gist|key.?point|main.?point)\\b", RegexOption.IGNORE_CASE)) -> AIIntent.SUMMARIZE
    matchesAny(input, Regex("^(translate|convert.*to|en français|auf Deutsch|" +
        "en español|in italiano|по-русски|in Japanese|in Chinese)\\b", RegexOption.IGNORE_CASE)) ||
    input.matches(Regex(".*(?i)translate.*to\\s+\\w+.*")) -> AIIntent.TRANSLATE
    matchesAny(input, Regex("^(write a .*story|write a .*poem|write a .*song|" +
        "compose|create a story|tell me a story|imagine|fantasy|fiction)\\b", RegexOption.IGNORE_CASE)) -> AIIntent.CREATIVE
    else -> AIIntent.CHAT
}

private fun matchesAny(input: String, regex: Regex) = regex.containsMatchIn(input.trim())
private fun containsAny(input: String, keywords: List<String>): Boolean {
    val lower = input.lowercase()
    return keywords.any { lower.contains(it.lowercase()) }
}

// ═══════════════════════════════════════════════════════════════
// CONTEXT GATHERING
// ═══════════════════════════════════════════════════════════════

private fun gatherContext(ctx: Context, type: AIIntent, input: String, history: CommandHistory): String? {
    val parts = mutableListOf<String>()
    val wd = ctx.filesDir?.parentFile ?: ctx.filesDir ?: return null
    val files = wd.listFiles()?.toList() ?: emptyList()

    when (type) {
        AIIntent.CODE, AIIntent.DEBUG -> {
            val src = files.filter { it.isFile && it.name.matches(Regex(".*\\.(kt|java|xml|json|gradle|ts|js|py)$")) }
            if (src.isNotEmpty()) parts.add("Files: ${src.take(10).joinToString(", ") { it.name }}")
            src.firstOrNull { input.contains(it.name, ignoreCase = true) }?.let { f ->
                try { parts.add("File ${f.name}:\n${f.readText().take(2000)}") } catch (_: Exception) {}
            }
            history.getRecent(10).takeIf { it.isNotEmpty() }?.let { parts.add("Recent cmds: ${it.joinToString("; ")}") }
        }
        AIIntent.EXPLAIN -> {
            files.firstOrNull { input.contains(it.name, true) && it.isFile && it.length() < 50000 }?.let { f ->
                try { parts.add("File ${f.name}:\n${f.readText().take(1000)}") } catch (_: Exception) {}
            }
        }
        AIIntent.COMMAND -> {
            parts.add("Dir: ${wd.absolutePath}")
            parts.add("Contents: ${files.take(15).joinToString(", ") { it.name }}")
            history.getRecent(5).takeIf { it.isNotEmpty() }?.let { parts.add("Recent: ${it.joinToString("; ")}") }
        }
        else -> {}
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString("\n")
}

// ═══════════════════════════════════════════════════════════════
// RESPONSE FORMATTING
// ═══════════════════════════════════════════════════════════════

fun formatResponse(input: String): String {
    val lines = input.lines()
    val sb = StringBuilder()
    var inBlock = false
    var lang = ""

    for (line in lines) {
        when {
            line.matches(Regex("\\[CODE:\\w+\\].*", RegexOption.IGNORE_CASE)) -> {
                val m = Regex("\\[CODE:(\\w+)\\](.*)", RegexOption.IGNORE_CASE).find(line)
                if (m != null) {
                    inBlock = true; lang = m.groupValues[1]
                    val rest = m.groupValues[2]; val w = 58
                    sb.appendLine("╔${"═".repeat(w)}╗")
                    sb.appendLine("║  ${lang.uppercase()}${" ".repeat((w - lang.length - 4).coerceAtLeast(0))}║")
                    sb.appendLine("║${"─".repeat(w)}║")
                    if (rest.isNotBlank()) boxAppend(sb, rest, w)
                }
            }
            line.matches(Regex(".*\\[/CODE\\].*", RegexOption.IGNORE_CASE)) && inBlock -> {
                val before = line.replace(Regex("\\[/CODE\\].*", RegexOption.IGNORE_CASE), "")
                if (before.isNotBlank()) boxAppend(sb, before, 58)
                sb.appendLine("╚${"═".repeat(58)}╝"); inBlock = false
            }
            inBlock -> boxAppend(sb, line, 58)
            line.matches(Regex("^#{1,3}\\s+.*")) -> {
                val t = line.replace(Regex("^#+\\s*"), "")
                sb.appendLine(t); sb.appendLine("═".repeat(t.length.coerceAtMost(60)))
            }
            line.matches(Regex("^\\s*[-*]\\s+.*")) -> {
                sb.appendLine(wrap("  • ${line.replace(Regex("^\\s*[-*]\\s+"), "")}", 60))
            }
            else -> sb.appendLine(wrap(line, 60))
        }
    }
    if (inBlock) sb.appendLine("╚${"═".repeat(58)}╝")
    return sb.toString().trimEnd()
}

private fun boxAppend(sb: StringBuilder, text: String, w: Int) {
    wrap(text, w - 4).lines().forEach { sb.appendLine("║  ${it.padEnd(w - 4)}  ║") }
}

private fun wrap(text: String, maxW: Int): String {
    if (text.length <= maxW) return text
    val sb = StringBuilder(); var s = 0
    while (s < text.length) {
        var e = (s + maxW).coerceAtMost(text.length)
        if (e < text.length) { val sp = text.lastIndexOf(' ', e); if (sp > s) e = sp }
        sb.appendLine(text.substring(s, e).trimEnd()); s = if (e == s) e + 1 else e
    }
    return sb.toString().trimEnd()
}

// ═══════════════════════════════════════════════════════════════
// AI CONFIGURATION
// ═══════════════════════════════════════════════════════════════

class AIConfig(private val context: Context) {
    companion object {
        private const val PREFS = "tui_ai_prefs"
        private const val TAG = "AIConfig"
    }
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val gson = Gson()

    var apiKeys: Map<String, String>
        get() = try { gson.fromJson(prefs.getString("api_keys", "{}") ?: "{}",
            object : TypeToken<Map<String, String>>() {}.type) ?: emptyMap()
        } catch (_: Exception) { emptyMap() }
        set(v) = prefs.edit().putString("api_keys", gson.toJson(v)).apply()

    fun getApiKey(provider: String = "minimax"): String = apiKeys[provider]
        ?: prefs.getString("minimax_api_key", "") ?: ""
    fun setApiKey(provider: String, key: String) {
        val m = apiKeys.toMutableMap(); m[provider] = key; apiKeys = m
        if (provider == "minimax") prefs.edit().putString("minimax_api_key", key).apply()
    }

    var provider: String
        get() = prefs.getString("ai_provider", "minimax") ?: "minimax"
        set(v) = prefs.edit().putString("ai_provider", v.lowercase()).apply()
    var minimaxModel: String
        get() = prefs.getString("ai_minimax_model", "abab6.5-chat") ?: "abab6.5-chat"
        set(v) = prefs.edit().putString("ai_minimax_model", v).apply()
    var openAIModel: String
        get() = prefs.getString("ai_openai_model", "gpt-4") ?: "gpt-4"
        set(v) = prefs.edit().putString("ai_openai_model", v).apply()
    var anthropicModel: String
        get() = prefs.getString("ai_anthropic_model", "claude-3-sonnet-20240229") ?: "claude-3-sonnet-20240229"
        set(v) = prefs.edit().putString("ai_anthropic_model", v).apply()
    var groqModel: String
        get() = prefs.getString("ai_groq_model", "llama-3.1-8b-instant") ?: "llama-3.1-8b-instant"
        set(v) = prefs.edit().putString("ai_groq_model", v).apply()
    var cerebrasModel: String
        get() = prefs.getString("ai_cerebras_model", "llama3.1-8b") ?: "llama3.1-8b"
        set(v) = prefs.edit().putString("ai_cerebras_model", v).apply()
    var openrouterModel: String
        get() = prefs.getString("ai_openrouter_model", "meta-llama/llama-3.3-70b-instruct:free") ?: "meta-llama/llama-3.3-70b-instruct:free"
        set(v) = prefs.edit().putString("ai_openrouter_model", v).apply()
    @Deprecated("Use minimaxModel") var model: String
        get() = minimaxModel; set(v) { minimaxModel = v }

    var currentPersona: String
        get() = prefs.getString("ai_persona", "default") ?: "default"
        set(v) = prefs.edit().putString("ai_persona", v).apply()
    var temperature: Double
        get() = prefs.getFloat("ai_temp", 0.7f).toDouble()
        set(v) = prefs.edit().putFloat("ai_temp", v.toFloat()).apply()
    var maxTokens: Int
        get() = prefs.getInt("ai_max_tokens", 1024)
        set(v) = prefs.edit().putInt("ai_max_tokens", v).apply()
    var isStreamingEnabled: Boolean
        get() = prefs.getBoolean("ai_streaming", true)
        set(v) = prefs.edit().putBoolean("ai_streaming", v).apply()
    var cacheEnabled: Boolean
        get() = prefs.getBoolean("ai_cache", true)
        set(v) = prefs.edit().putBoolean("ai_cache", v).apply()
    var maxMemoryTurns: Int
        get() = prefs.getInt("ai_memory_turns", 20)
        set(v) = prefs.edit().putInt("ai_memory_turns", v).apply()
    var translationLanguage: String
        get() = prefs.getString("ai_translate_lang", "English") ?: "English"
        set(v) = prefs.edit().putString("ai_translate_lang", v).apply()
    val isConfigured: Boolean get() = apiKeys.any { (_, k) -> k.isNotBlank() && k.length >= 10 }

    private var personaCache: MutableMap<String, Persona>? = null

    fun getCustomPersonas(): Map<String, Persona> {
        if (personaCache != null) return personaCache!!.toMap()
        val json = prefs.getString("ai_custom_personas", "{}") ?: "{}"
        return try {
            val raw: Map<String, Map<String, String>> = gson.fromJson(json,
                object : TypeToken<Map<String, Map<String, String>>>() {}.type) ?: emptyMap()
            val r = raw.mapValues { (_, v) -> Persona(v["name"] ?: "?", v["systemPrompt"] ?: "", v["description"] ?: "") }
            personaCache = r.toMutableMap(); r
        } catch (_: Exception) { emptyMap() }
    }

    fun saveCustomPersona(p: Persona) {
        val cur = getCustomPersonas().toMutableMap(); cur[p.name] = p; personaCache = cur.toMutableMap()
        prefs.edit().putString("ai_custom_personas", gson.toJson(cur.mapValues { (_, p2) ->
            mapOf("name" to p2.name, "systemPrompt" to p2.systemPrompt, "description" to p2.description)
        })).apply()
    }

    fun deleteCustomPersona(name: String) {
        val cur = getCustomPersonas().toMutableMap(); cur.remove(name); personaCache = cur.toMutableMap()
        prefs.edit().putString("ai_custom_personas", gson.toJson(cur.mapValues { (_, p2) ->
            mapOf("name" to p2.name, "systemPrompt" to p2.systemPrompt, "description" to p2.description)
        })).apply()
    }

    fun getAllPersonas(): List<Persona> = BUILT_IN_PERSONAS + getCustomPersonas().values

    fun resetAll() = prefs.edit().clear().apply()
}

// ═══════════════════════════════════════════════════════════════
// MAIN SERVICE CLASS
// ═══════════════════════════════════════════════════════════════

class MiniMaxService(private val context: Context) {
    companion object { private const val TAG = "MiniMaxService" }

    private val config = AIConfig(context)
    private val memory = ConversationMemory(config.maxMemoryTurns)
    private val cache = ResponseCache()
    private val usageTracker = UsageTracker(context.getSharedPreferences("tui_ai_prefs", Context.MODE_PRIVATE))
    private val commandHistory = CommandHistory(context)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS).build()

    // ── Provider Factory ──
    private fun getProvider(): AIProvider {
        val keys = config.apiKeys
        return when (config.provider) {
            "openai" -> OpenAIProvider(httpClient, keys["openai"] ?: "", config.openAIModel)
            "anthropic" -> AnthropicProvider(httpClient, keys["anthropic"] ?: "", config.anthropicModel)
            "groq" -> GroqProvider(httpClient, keys["groq"] ?: "", config.groqModel)
            "cerebras" -> CerebrasProvider(httpClient, keys["cerebras"] ?: "", config.cerebrasModel)
            "openrouter" -> OpenRouterProvider(httpClient, keys["openrouter"] ?: "", config.openrouterModel)
            else -> MiniMaxProvider(httpClient, keys["minimax"] ?: config.getApiKey("minimax"), config.minimaxModel)
        }
    }
    private fun currentModel(): String = when (config.provider) {
        "openai" -> config.openAIModel
        "anthropic" -> config.anthropicModel
        "groq" -> config.groqModel
        "cerebras" -> config.cerebrasModel
        "openrouter" -> config.openrouterModel
        else -> config.minimaxModel
    }

    // ── Persona Management ──
    private fun resolvePersona(name: String): Persona =
        config.getCustomPersonas()[name] ?: BUILT_IN_PERSONAS.firstOrNull { it.name == name }
            ?: BUILT_IN_PERSONAS.first()

    fun getPersonas(): List<Persona> = config.getAllPersonas()
    fun setPersona(name: String): Boolean {
        if (getPersonas().none { it.name == name }) return false
        config.currentPersona = name; memory.setSystem(resolvePersona(name).systemPrompt); return true
    }
    fun createPersona(name: String, prompt: String) =
        config.saveCustomPersona(Persona(name, prompt, "Custom: $name"))
    fun deletePersona(name: String) {
        if (BUILT_IN_PERSONAS.any { it.name == name }) return
        config.deleteCustomPersona(name)
        if (config.currentPersona == name) config.currentPersona = "default"
    }
    fun getCurrentPersona(): Persona = resolvePersona(config.currentPersona)

    // ── Provider / Memory / Cache ──
    fun getProviders(): List<String> = listOf("minimax", "openai", "anthropic", "groq", "cerebras", "openrouter")
    fun setProvider(name: String) { require(name in getProviders()); config.provider = name }
    fun getCurrentProvider(): String = config.provider

    // ── API Key Management ──
    fun setApiKey(provider: String, key: String) { config.setApiKey(provider, key) }
    fun getApiKey(provider: String): String = config.getApiKey(provider)
    fun getConfiguredProviders(): List<String> =
        getProviders().filter { p -> config.getApiKey(p).isNotBlank() }
    fun hasApiKey(provider: String): Boolean = config.getApiKey(provider).isNotBlank()

    fun clearMemory() { memory.clear(); Log.d(TAG, "Memory cleared") }
    fun saveMemory(): String = memory.toJson()
    fun restoreMemory(json: String) = memory.fromJson(json)
    fun getLastResponse(): String? = cache.getRecent()?.second
    fun getLastQuery(): String? = cache.getRecent()?.first
    fun getUsageStats(): UsageStats = usageTracker.getStats()
    fun resetUsageStats() = usageTracker.reset()

    fun repeatLastQuery(): Flow<AIEvent> {
        val last = cache.getRecent() ?: return callbackFlow {
            trySend(AIEvent.Error("No previous query")); awaitClose { }
        }
        return smartQuery(last.first, stream = config.isStreamingEnabled)
    }

    // ── Command Suggestion ──
    private fun suggestCommand(input: String): Pair<String, String>? {
        val l = input.lowercase()
        return when {
            l.contains("find large") || l.contains("large file") ->
                "find /sdcard -type f -size +10M" to "Find files > 10MB"
            l.contains("zip") || l.contains("compress") ->
                "zip -r archive.zip ." to "Compress to archive.zip"
            l.contains("backup") ->
                "tar -czf backup.tar.gz /sdcard/Documents" to "Backup Documents"
            l.contains("kill") || l.contains("stop") ->
                "ps -ef | grep ${lastWord(input)}" to "Find process to kill"
            l.contains("download") ->
                "wget ${extractUrl(input) ?: "<url>"}" to "Download file"
            l.contains("install") ->
                "pm install -r ${lastWord(input)}.apk" to "Install APK"
            else -> null
        }
    }
    private fun lastWord(s: String) = s.split("\\s+".toRegex()).lastOrNull { it.length > 2 } ?: "target"
    private fun extractUrl(s: String): String? = Regex("https?://[\\w\\-./?=%&]+").find(s)?.value

    // ── Smart Query (Flow) ──
    fun smartQuery(input: String, stream: Boolean = true): Flow<AIEvent> = callbackFlow {
        if (input.isBlank()) { trySend(AIEvent.Error("Empty query")); close(); return@callbackFlow }
        try {
            val intent = detectIntent(input)
            trySend(AIEvent.ContextInfo("Intent: ${intent.name.lowercase()}"))

            val provider = getProvider()
            if (!provider.isConfigured) {
                trySend(AIEvent.Error("${provider.name} not configured. Use 'ai config' to set API key."))
                close(); return@callbackFlow
            }

            val persona = resolvePersona(config.currentPersona)
            memory.maxTurns = config.maxMemoryTurns
            memory.setSystem(persona.systemPrompt)

            val ctx = gatherContext(context, intent, input, commandHistory)
            if (ctx != null) trySend(AIEvent.ContextInfo("Context gathered"))

            val cacheKey = input.lowercase().trim()
            if (config.cacheEnabled && intent != AIIntent.COMMAND) {
                cache.get(cacheKey)?.let { cached ->
                    memory.addUser(input); memory.addAssistant(cached)
                    trySend(AIEvent.Complete(cached, TokenUsage(0, estimateTokens(cached), 0.0)))
                    close(); return@callbackFlow
                }
            }

            memory.addUser(input)
            val msgs = buildMessages(input, intent, ctx)
            val req = AIRequest(msgs, config.temperature, config.maxTokens, stream && config.isStreamingEnabled)

            if (intent == AIIntent.COMMAND) {
                suggestCommand(input)?.let { (cmd, exp) ->
                    trySend(AIEvent.CommandSuggestion(cmd, exp))
                }
            }

            if (req.stream) {
                trySend(AIEvent.Thinking("Streaming from ${provider.name}..."))
                val full = StringBuilder()
                provider.stream(req) { chunk, done ->
                    if (chunk.isNotEmpty()) { full.append(chunk); trySend(AIEvent.Chunk(chunk)) }
                    if (done) {
                        val raw = full.toString(); val formatted = formatResponse(raw)
                        val inT = estimateTokens(messagesToText(msgs))
                        val outT = estimateTokens(raw)
                        val cost = usageTracker.getCostEstimate(inT, outT, currentModel())
                        usageTracker.record(inT, outT, currentModel())
                        cache.put(cacheKey, formatted); memory.addAssistant(formatted)
                        trySend(AIEvent.Complete(formatted, TokenUsage(inT, outT, cost)))
                    }
                }
            } else {
                trySend(AIEvent.Thinking("Processing with ${provider.name}..."))
                when (val result = provider.query(req)) {
                    is AIResult.Success -> {
                        val formatted = formatResponse(result.response)
                        val inT = estimateTokens(messagesToText(msgs))
                        val outT = estimateTokens(result.response)
                        val cost = usageTracker.getCostEstimate(inT, outT, currentModel())
                        usageTracker.record(inT, outT, currentModel())
                        if (config.cacheEnabled) cache.put(cacheKey, formatted)
                        memory.addAssistant(formatted)
                        trySend(AIEvent.Complete(formatted, TokenUsage(inT, outT, cost)))
                    }
                    is AIResult.Error -> trySend(AIEvent.Error(result.message))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "SmartQuery failed: ${e.message}", e)
            trySend(AIEvent.Error("Error: ${e.message}"))
        }
        close()
    }

    // ── Backward-compatible simple query ──
    fun query(userInput: String): AIResult {
        return try {
            val provider = getProvider()
            if (!provider.isConfigured) return AIResult.Error("API key not configured.", false)
            val persona = resolvePersona(config.currentPersona)
            provider.query(AIRequest(listOf(
                ChatMessage("system", persona.systemPrompt), ChatMessage("user", userInput)
            ), config.temperature, config.maxTokens, false))
        } catch (e: Exception) {
            Log.e(TAG, "Query error: ${e.message}"); AIResult.Error("AI error: ${e.message}", true)
        }
    }

    private fun buildMessages(input: String, intent: AIIntent, ctx: String?): List<ChatMessage> {
        val prompt = StringBuilder(input)
        when (intent) {
            AIIntent.CODE -> {
                if (ctx != null) prompt.append("\n[Context]\n$ctx")
                prompt.append("\nUse [CODE:lang]...[/CODE] tags for code.")
            }
            AIIntent.DEBUG -> {
                if (ctx != null) prompt.append("\n[Context]\n$ctx")
                prompt.append("\nExplain root cause and minimal fix.")
            }
            AIIntent.TRANSLATE -> prompt.append("\nTranslate to ${config.translationLanguage}.")
            else -> {}
        }
        val mem = memory.getMessages().toMutableList()
        if (mem.lastOrNull()?.role == "user") mem.removeAt(mem.lastIndex)
        return mem + ChatMessage("user", prompt.toString().trimEnd())
    }

    // ── Legacy QueryType enum ──
    enum class QueryType { GENERAL, CODE, DEBUG, SUMMARIZE, TRANSLATE, CREATIVE }
    fun detectQueryType(input: String): QueryType = when (detectIntent(input)) {
        AIIntent.CODE -> QueryType.CODE; AIIntent.DEBUG -> QueryType.DEBUG
        AIIntent.SUMMARIZE -> QueryType.SUMMARIZE; AIIntent.TRANSLATE -> QueryType.TRANSLATE
        AIIntent.CREATIVE -> QueryType.CREATIVE; else -> QueryType.GENERAL
    }

    fun validateApiKey(): Boolean = query("test") is AIResult.Success
}

// ═══════════════════════════════════════════════════════════════
// UTILITY FUNCTIONS
// ═══════════════════════════════════════════════════════════════

internal fun estimateTokens(text: String): Int = (text.length / 4).coerceAtLeast(1)
internal fun messagesToText(msgs: List<ChatMessage>): String = msgs.joinToString("\n") { "${it.role}: ${it.content}" }
