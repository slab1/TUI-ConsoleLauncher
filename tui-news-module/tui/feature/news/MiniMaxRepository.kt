package tui.feature.news

import android.content.Context
import android.util.Log

/**
 * Repository layer for news data management
 * Acts as a single source of truth for news-related operations
 */
class MiniMaxRepository(context: Context) {

    companion object {
        private const val TAG = "MiniMaxRepository"
        private const val CACHE_SIZE = 10
        private const val CACHE_TIMEOUT_MS = 30 * 60 * 1000L // 30 minutes
    }

    private val service = MiniMaxService(context)
    private val config = NewsConfig(context)
    
    // Simple in-memory cache
    private data class CacheEntry(
        val topic: String,
        val response: String,
        val timestamp: Long
    )
    
    private val cache = mutableMapOf<String, CacheEntry>()

    /**
     * Fetches news for a given topic
     * @param topic The news topic (e.g., "technology", "sports")
     * @param forceRefresh Whether to bypass cache
     * @return Result containing formatted news output or error
     */
    fun getNews(topic: String, forceRefresh: Boolean = false): Result<String> {
        val normalizedTopic = topic.lowercase().trim()
        val cacheKey = if (config.isDetailedMode) "detailed:$normalizedTopic" else normalTopic

        // Check cache if not forcing refresh
        if (!forceRefresh) {
            val cached = cache[cacheKey]
            if (cached != null && isCacheValid(cached.timestamp)) {
                Log.d(TAG, "Returning cached news for topic: $topic")
                return Result.success(formatCachedResponse(cached))
            }
        }

        // Validate API key before making request
        if (!service.validateApiKey()) {
            return Result.failure(SecurityException(
                "Invalid MiniMax API key. Please configure in settings."
            ))
        }

        // Fetch from API
        val result = service.getNewsSummary(
            topic = normalizedTopic,
            detailed = config.isDetailedMode
        )

        // Cache successful responses
        result.onSuccess { response ->
            val entry = CacheEntry(
                topic = normalizedTopic,
                response = response,
                timestamp = System.currentTimeMillis()
            )
            cache[cacheKey] = entry
            pruneCacheIfNeeded()
        }

        return result.map { response ->
            formatNewsResponse(response, normalizedTopic)
        }
    }

    /**
     * Checks if cached entry is still valid
     */
    private fun isCacheValid(timestamp: Long): Boolean {
        return System.currentTimeMillis() - timestamp < CACHE_TIMEOUT_MS
    }

    /**
     * Prunes cache to maintain maximum size
     */
    private fun pruneCacheIfNeeded() {
        if (cache.size > CACHE_SIZE) {
            // Remove oldest entries
            val sortedEntries = cache.entries
                .sortedBy { it.value.timestamp }
                .take(cache.size - CACHE_SIZE)
            
            sortedEntries.forEach { cache.remove(it.key) }
        }
    }

    /**
     * Formats the news response for terminal display
     */
    private fun formatNewsResponse(rawResponse: String, topic: String): String {
        val builder = StringBuilder()
        
        // Add header
        builder.appendLine("═" .repeat(50))
        builder.appendLine("  📰 NEWS: ${topic.uppercase()}")
        builder.appendLine("═" .repeat(50))
        builder.appendLine()

        // Clean and format the response
        val cleanedResponse = cleanMarkdown(rawResponse)
        builder.appendLine(cleanedResponse)
        
        builder.appendLine()
        builder.appendLine("─" .repeat(50))
        builder.appendLine("  [Powered by MiniMax AI • ${getTimestamp()}]")
        
        return builder.toString()
    }

    /**
     * Formats cached response with fresh timestamp
     */
    private fun formatCachedResponse(entry: CacheEntry): String {
        return entry.response.replace(
            Regex("\\[Powered by MiniMax AI.*?\\]"),
            "[Powered by MiniMax AI • Cached • ${getTimestamp()}]"
        )
    }

    /**
     * Removes markdown formatting from AI response
     */
    private fun cleanMarkdown(text: String): String {
        return text
            .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
            .replace(Regex("\\*(.+?)\\*"), "$1")
            .replace(Regex("`(.+?)`"), "$1")
            .replace(Regex("###\\s*"), "")
            .replace(Regex("##\\s*"), "")
            .replace(Regex("#\\s*"), "")
            .replace(Regex(">\\s*"), "  ")
            .replace(Regex("^-{3,}$"), "")
            .replace(Regex("^\\s*[-*]\\s+", RegexOption.MULTILINE), "  • ")
            .trim()
    }

    /**
     * Gets current timestamp in readable format
     */
    private fun getTimestamp(): String {
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    /**
     * Clears the news cache
     */
    fun clearCache() {
        cache.clear()
        Log.d(TAG, "News cache cleared")
    }

    /**
     * Gets cache statistics
     */
    fun getCacheStats(): Pair<Int, Long> {
        val oldestEntry = cache.values.minOfOrNull { it.timestamp } ?: 0L
        return Pair(cache.size, oldestEntry)
    }
}
