package tui.feature.news

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Configuration management for the news feature
 * Handles API key storage and user preferences
 */
class NewsConfig(context: Context) {

    companion object {
        private const val TAG = "NewsConfig"
        private const val PREFS_NAME = "tui_news_prefs"
        
        // Preference keys
        private const val KEY_API_KEY = "minimax_api_key"
        private const val KEY_DETAILED_MODE = "detailed_mode"
        private const val KEY_DEFAULT_TOPIC = "default_topic"
        private const val KEY_SHOW_SOURCES = "show_sources"
        private const val KEY_CACHE_ENABLED = "cache_enabled"
        private const val KEY_MAX_ITEMS = "max_items"
        
        // Default values
        private const val DEFAULT_TOPIC = "general"
        private const val DEFAULT_MAX_ITEMS = 5
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, 
        Context.MODE_PRIVATE
    )

    /**
     * Gets the MiniMax API key
     */
    fun getApiKey(context: Context): String {
        // First check SharedPreferences
        val prefsKey = prefs.getString(KEY_API_KEY, null)
        if (!prefsKey.isNullOrBlank()) {
            return prefsKey
        }

        // Fall back to secrets.properties
        return try {
            val inputStream = context.assets.open("secrets.properties")
            val properties = java.util.Properties()
            properties.load(inputStream)
            inputStream.close()
            
            properties.getProperty("MINIMAX_API_KEY", "").also { key ->
                if (key.isNotBlank()) {
                    Log.d(TAG, "API key loaded from secrets.properties")
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "No secrets.properties found")
            ""
        }
    }

    /**
     * Sets the MiniMax API key
     */
    fun setApiKey(apiKey: String) {
        prefs.edit().putString(KEY_API_KEY, apiKey.trim()).apply()
        Log.d(TAG, "API key saved successfully")
    }

    /**
     * Checks if detailed mode is enabled
     * Detailed mode provides longer, more informative summaries
     */
    var isDetailedMode: Boolean
        get() = prefs.getBoolean(KEY_DETAILED_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DETAILED_MODE, value).apply()

    /**
     * Gets the default news topic
     */
    var defaultTopic: String
        get() = prefs.getString(KEY_DEFAULT_TOPIC, DEFAULT_TOPIC) ?: DEFAULT_TOPIC
        set(value) = prefs.edit().putString(KEY_DEFAULT_TOPIC, value.lowercase()).apply()

    /**
     * Whether to show source attribution in responses
     */
    var showSources: Boolean
        get() = prefs.getBoolean(KEY_SHOW_SOURCES, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_SOURCES, value).apply()

    /**
     * Whether caching is enabled
     */
    var isCacheEnabled: Boolean
        get() = prefs.getBoolean(KEY_CACHE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_CACHE_ENABLED, value).apply()

    /**
     * Maximum number of news items to display
     */
    var maxItems: Int
        get() = prefs.getInt(KEY_MAX_ITEMS, DEFAULT_MAX_ITEMS)
        set(value) = prefs.edit().putInt(KEY_MAX_ITEMS, value.coerceIn(3, 10)).apply()

    /**
     * Validates the API key format
     */
    fun isApiKeyValid(apiKey: String): Boolean {
        return apiKey.isNotBlank() && apiKey.length >= 10
    }

    /**
     * Gets all current settings as a formatted string
     */
    fun getSettingsSummary(): String {
        return buildString {
            appendLine("News Module Settings:")
            appendLine("  API Key: ${if (getApiKey(context).isNotBlank()) "✓ Configured" else "✗ Not Set"}")
            appendLine("  Detailed Mode: ${if (isDetailedMode) "Enabled" else "Disabled"}")
            appendLine("  Default Topic: $defaultTopic")
            appendLine("  Show Sources: ${if (showSources) "Yes" else "No"}")
            appendLine("  Caching: ${if (isCacheEnabled) "Enabled" else "Disabled"}")
            appendLine("  Max Items: $maxItems")
        }
    }

    /**
     * Resets all settings to defaults
     */
    fun resetToDefaults() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Settings reset to defaults")
    }
}
