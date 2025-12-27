package ohi.andre.consolelauncher.settings.modules;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import ohi.andre.consolelauncher.settings.BaseSettingsModule;
import ohi.andre.consolelauncher.settings.ISettingsModule;
import ohi.andre.consolelauncher.settings.SecurityHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings module for AI configuration including API credentials, model selection,
 * conversation settings, and voice-related preferences for AI interactions.
 * This module uses encrypted storage for sensitive values like API keys.
 */
public class AISettingsModule extends BaseSettingsModule {
    
    private static final String TAG = "AISettingsModule";
    
    public static final String MODULE_ID = "ai_settings";
    
    // Preference keys
    private static final String KEY_API_KEY = "api_key";
    private static final String KEY_GROUP_ID = "group_id";
    private static final String KEY_MODEL = "model";
    private static final String KEY_TEMPERATURE = "temperature";
    private static final String KEY_MAX_TOKENS = "max_tokens";
    private static final String KEY_CONTEXT_MESSAGES = "context_messages";
    private static final String KEY_VOICE_ENABLED = "voice_enabled";
    private static final String KEY_VOICE_AUTO_READ = "voice_auto_read";
    private static final String KEY_VOICE_PITCH = "voice_pitch";
    private static final String KEY_VOICE_SPEED = "voice_speed";
    private static final String KEY_SYSTEM_PROMPT = "system_prompt";
    private static final String KEY_STREAMING_ENABLED = "streaming_enabled";
    private static final String KEY_SHOW_THINKING = "show_thinking";
    
    // Default values
    private static final String DEFAULT_MODEL = "abab5.5-chat";
    private static final float DEFAULT_TEMPERATURE = 0.7f;
    private static final int DEFAULT_MAX_TOKENS = 1024;
    private static final int DEFAULT_CONTEXT_MESSAGES = 10;
    private static final float DEFAULT_VOICE_PITCH = 1.0f;
    private static final float DEFAULT_VOICE_SPEED = 1.0f;
    private static final boolean DEFAULT_STREAMING_ENABLED = true;
    private static final boolean DEFAULT_SHOW_THINKING = true;
    
    private final SharedPreferences securePreferences;
    private final SharedPreferences standardPreferences;
    
    public AISettingsModule(Context context) {
        super(context, MODULE_ID);
        
        // Create separate preference files for secure and standard values
        this.standardPreferences = context.getSharedPreferences(
            "tui_settings_ai_standard", Context.MODE_PRIVATE);
        this.securePreferences = SecurityHelper.getEncryptedPreferences(context);
    }
    
    @Override
    protected SharedPreferences createSharedPreferences() {
        // Return standard preferences for base class operations
        // Secure values are handled separately
        return standardPreferences;
    }
    
    @Override
    public void loadSettings() {
        // Load standard preferences through base class
        // No explicit action needed - base class handles standard prefs
        
        // Log secure status without exposing values
        boolean hasApiKey = securePreferences.contains(KEY_API_KEY);
        Log.d(TAG, "AI Settings loaded - API Key present: " + hasApiKey);
    }
    
    @Override
    public void saveSettings() {
        // All values are already saved through individual set methods
        clearChangedFlag();
        Log.d(TAG, "AI Settings saved");
    }
    
    @Override
    public void resetToDefaults() {
        // Clear secure values
        securePreferences.edit()
            .remove(KEY_API_KEY)
            .remove(KEY_GROUP_ID)
            .remove(KEY_SYSTEM_PROMPT)
            .apply();
        
        // Clear standard values
        standardPreferences.edit()
            .remove(KEY_MODEL)
            .remove(KEY_TEMPERATURE)
            .remove(KEY_MAX_TOKENS)
            .remove(KEY_CONTEXT_MESSAGES)
            .remove(KEY_VOICE_ENABLED)
            .remove(KEY_VOICE_AUTO_READ)
            .remove(KEY_VOICE_PITCH)
            .remove(KEY_VOICE_SPEED)
            .remove(KEY_STREAMING_ENABLED)
            .remove(KEY_SHOW_THINKING)
            .apply();
        
        markAsChanged();
        notifyReset();
        Log.d(TAG, "AI Settings reset to defaults");
    }
    
    // ==================== API Credentials (Secure Storage) ====================
    
    /**
     * Gets the API key from secure storage.
     * 
     * @return The API key, or empty string if not set
     */
    public String getApiKey() {
        return securePreferences.getString(KEY_API_KEY, "");
    }
    
    /**
     * Sets the API key in secure storage.
     * 
     * @param apiKey The API key to store
     */
    public void setApiKey(String apiKey) {
        securePreferences.edit().putString(KEY_API_KEY, apiKey).apply();
        markAsChanged();
        notifyChange(KEY_API_KEY);
    }
    
    /**
     * Checks if API key is configured.
     * 
     * @return true if API key is set
     */
    public boolean hasApiKey() {
        String key = getApiKey();
        return key != null && !key.isEmpty();
    }
    
    /**
     * Gets the API key masked for display.
     * 
     * @return Masked API key (e.g., "sk-****1234")
     */
    public String getApiKeyMasked() {
        String key = getApiKey();
        if (key == null || key.length() <= 4) {
            return "****";
        }
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }
    
    /**
     * Sets the Group ID for API authentication.
     * 
     * @param groupId The group ID
     */
    public void setGroupId(String groupId) {
        securePreferences.edit().putString(KEY_GROUP_ID, groupId).apply();
        markAsChanged();
        notifyChange(KEY_GROUP_ID);
    }
    
    /**
     * Gets the Group ID.
     * 
     * @return The Group ID, or empty string if not set
     */
    public String getGroupId() {
        return securePreferences.getString(KEY_GROUP_ID, "");
    }
    
    /**
     * Checks if Group ID is configured.
     * 
     * @return true if Group ID is set
     */
    public boolean hasGroupId() {
        String groupId = getGroupId();
        return groupId != null && !groupId.isEmpty();
    }
    
    /**
     * Removes API credentials from secure storage.
     */
    public void clearCredentials() {
        securePreferences.edit()
            .remove(KEY_API_KEY)
            .remove(KEY_GROUP_ID)
            .apply();
        markAsChanged();
    }
    
    // ==================== Model Settings ====================
    
    /**
     * Gets the currently selected AI model.
     * 
     * @return The model identifier
     */
    public String getModel() {
        return standardPreferences.getString(KEY_MODEL, DEFAULT_MODEL);
    }
    
    /**
     * Sets the AI model to use.
     * 
     * @param model The model identifier
     */
    public void setModel(String model) {
        standardPreferences.edit().putString(KEY_MODEL, model).apply();
        markAsChanged();
        notifyChange(KEY_MODEL);
    }
    
    /**
     * Gets the available models list.
     * 
     * @return Array of available model identifiers
     */
    public String[] getAvailableModels() {
        return new String[]{
            "abab5.5-chat",
            "abab6.5-chat",
            "abab6.5s-chat"
        };
    }
    
    // ==================== Generation Settings ====================
    
    /**
     * Gets the temperature setting for generation.
     * Higher values produce more creative but less focused responses.
     * 
     * @return Temperature value between 0.0 and 1.0
     */
    public float getTemperature() {
        return standardPreferences.getFloat(KEY_TEMPERATURE, DEFAULT_TEMPERATURE);
    }
    
    /**
     * Sets the temperature setting.
     * 
     * @param temperature Value between 0.0 and 1.0
     */
    public void setTemperature(float temperature) {
        float clamped = Math.max(0.0f, Math.min(1.0f, temperature));
        standardPreferences.edit().putFloat(KEY_TEMPERATURE, clamped).apply();
        markAsChanged();
        notifyChange(KEY_TEMPERATURE);
    }
    
    /**
     * Gets the maximum tokens for response generation.
     * 
     * @return Maximum token count
     */
    public int getMaxTokens() {
        return standardPreferences.getInt(KEY_MAX_TOKENS, DEFAULT_MAX_TOKENS);
    }
    
    /**
     * Sets the maximum tokens for response generation.
     * 
     * @param maxTokens Maximum token count
     */
    public void setMaxTokens(int maxTokens) {
        int clamped = Math.max(256, Math.min(4096, maxTokens));
        standardPreferences.edit().putInt(KEY_MAX_TOKENS, clamped).apply();
        markAsChanged();
        notifyChange(KEY_MAX_TOKENS);
    }
    
    /**
     * Gets the number of context messages to maintain.
     * 
     * @return Number of messages
     */
    public int getContextMessages() {
        return standardPreferences.getInt(KEY_CONTEXT_MESSAGES, DEFAULT_CONTEXT_MESSAGES);
    }
    
    /**
     * Sets the number of context messages to maintain.
     * 
     * @param count Number of messages to keep in context
     */
    public void setContextMessages(int count) {
        int clamped = Math.max(0, Math.min(50, count));
        standardPreferences.edit().putInt(KEY_CONTEXT_MESSAGES, clamped).apply();
        markAsChanged();
        notifyChange(KEY_CONTEXT_MESSAGES);
    }
    
    // ==================== Voice Settings ====================
    
    /**
     * Checks if voice output is enabled.
     * 
     * @return true if voice is enabled
     */
    public boolean isVoiceEnabled() {
        return standardPreferences.getBoolean(KEY_VOICE_ENABLED, false);
    }
    
    /**
     * Sets whether voice output is enabled.
     * 
     * @param enabled True to enable voice
     */
    public void setVoiceEnabled(boolean enabled) {
        standardPreferences.edit().putBoolean(KEY_VOICE_ENABLED, enabled).apply();
        markAsChanged();
        notifyChange(KEY_VOICE_ENABLED);
    }
    
    /**
     * Checks if AI responses should be auto-read aloud.
     * 
     * @return true if auto-read is enabled
     */
    public boolean isVoiceAutoReadEnabled() {
        return standardPreferences.getBoolean(KEY_VOICE_AUTO_READ, false);
    }
    
    /**
     * Sets whether AI responses should be auto-read aloud.
     * 
     * @param enabled True to enable auto-read
     */
    public void setVoiceAutoReadEnabled(boolean enabled) {
        standardPreferences.edit().putBoolean(KEY_VOICE_AUTO_READ, enabled).apply();
        markAsChanged();
        notifyChange(KEY_VOICE_AUTO_READ);
    }
    
    /**
     * Gets the voice pitch for TTS.
     * 
     * @return Pitch multiplier (0.5 to 2.0)
     */
    public float getVoicePitch() {
        return standardPreferences.getFloat(KEY_VOICE_PITCH, DEFAULT_VOICE_PITCH);
    }
    
    /**
     * Sets the voice pitch for TTS.
     * 
     * @param pitch Pitch multiplier (0.5 to 2.0)
     */
    public void setVoicePitch(float pitch) {
        float clamped = Math.max(0.5f, Math.min(2.0f, pitch));
        standardPreferences.edit().putFloat(KEY_VOICE_PITCH, clamped).apply();
        markAsChanged();
        notifyChange(KEY_VOICE_PITCH);
    }
    
    /**
     * Gets the voice speed for TTS.
     * 
     * @return Speed multiplier (0.5 to 2.0)
     */
    public float getVoiceSpeed() {
        return standardPreferences.getFloat(KEY_VOICE_SPEED, DEFAULT_VOICE_SPEED);
    }
    
    /**
     * Sets the voice speed for TTS.
     * 
     * @param speed Speed multiplier (0.5 to 2.0)
     */
    public void setVoiceSpeed(float speed) {
        float clamped = Math.max(0.5f, Math.min(2.0f, speed));
        standardPreferences.edit().putFloat(KEY_VOICE_SPEED, clamped).apply();
        markAsChanged();
        notifyChange(KEY_VOICE_SPEED);
    }
    
    // ==================== System Prompt ====================
    
    /**
     * Gets the custom system prompt.
     * 
     * @return System prompt, or empty string if default
     */
    public String getSystemPrompt() {
        return securePreferences.getString(KEY_SYSTEM_PROMPT, "");
    }
    
    /**
     * Sets the custom system prompt.
     * 
     * @param prompt System prompt text
     */
    public void setSystemPrompt(String prompt) {
        securePreferences.edit().putString(KEY_SYSTEM_PROMPT, prompt).apply();
        markAsChanged();
        notifyChange(KEY_SYSTEM_PROMPT);
    }
    
    /**
     * Checks if a custom system prompt is set.
     * 
     * @return true if custom prompt is set
     */
    public boolean hasCustomSystemPrompt() {
        String prompt = getSystemPrompt();
        return prompt != null && !prompt.isEmpty();
    }
    
    // ==================== Display Settings ====================
    
    /**
     * Checks if streaming response is enabled.
     * When enabled, responses appear token by token.
     * 
     * @return true if streaming is enabled
     */
    public boolean isStreamingEnabled() {
        return standardPreferences.getBoolean(KEY_STREAMING_ENABLED, DEFAULT_STREAMING_ENABLED);
    }
    
    /**
     * Sets whether streaming response is enabled.
     * 
     * @param enabled True to enable streaming
     */
    public void setStreamingEnabled(boolean enabled) {
        standardPreferences.edit().putBoolean(KEY_STREAMING_ENABLED, enabled).apply();
        markAsChanged();
        notifyChange(KEY_STREAMING_ENABLED);
    }
    
    /**
     * Checks if thinking indicator is shown.
     * 
     * @return true if thinking indicator is shown
     */
    public boolean isShowThinkingEnabled() {
        return standardPreferences.getBoolean(KEY_SHOW_THINKING, DEFAULT_SHOW_THINKING);
    }
    
    /**
     * Sets whether thinking indicator is shown.
     * 
     * @param enabled True to show thinking indicator
     */
    public void setShowThinkingEnabled(boolean enabled) {
        standardPreferences.edit().putBoolean(KEY_SHOW_THINKING, enabled).apply();
        markAsChanged();
        notifyChange(KEY_SHOW_THINKING);
    }
    
    // ==================== Status and Validation ====================
    
    /**
     * Gets the configuration status.
     * 
     * @return ConfigStatus indicating what is configured
     */
    public ConfigStatus getConfigStatus() {
        boolean hasKey = hasApiKey();
        boolean hasGroupId = hasGroupId();
        
        if (hasKey && hasGroupId) {
            return ConfigStatus.FULLY_CONFIGURED;
        } else if (hasKey) {
            return ConfigStatus.MISSING_GROUP_ID;
        } else if (hasGroupId) {
            return ConfigStatus.MISSING_API_KEY;
        } else {
            return ConfigStatus.NOT_CONFIGURED;
        }
    }
    
    /**
     * Gets a human-readable status description.
     * 
     * @return Status description
     */
    public String getStatusDescription() {
        switch (getConfigStatus()) {
            case FULLY_CONFIGURED:
                return "AI is configured and ready. Model: " + getModel();
            case MISSING_GROUP_ID:
                return "API Key set but Group ID missing. Use: ai config -g <group_id>";
            case MISSING_API_KEY:
                return "Group ID set but API Key missing. Use: ai config -k <api_key>";
            case NOT_CONFIGURED:
            default:
                return "AI not configured. Use: ai config -k <api_key> -g <group_id>";
        }
    }
    
    /**
     * Tests if the current configuration is valid.
     * 
     * @return true if all required settings are present
     */
    @Override
    public boolean validateSettings() {
        return hasApiKey() && hasGroupId();
    }
    
    @Override
    public List<SettingEntry> exportSettings() {
        List<SettingEntry> entries = new ArrayList<>();
        
        // Export standard preferences (non-sensitive)
        entries.add(new SettingEntry(KEY_MODEL, getModel(), SettingType.STRING));
        entries.add(new SettingEntry(KEY_TEMPERATURE, getTemperature(), SettingType.FLOAT));
        entries.add(new SettingEntry(KEY_MAX_TOKENS, getMaxTokens(), SettingType.INTEGER));
        entries.add(new SettingEntry(KEY_CONTEXT_MESSAGES, getContextMessages(), SettingType.INTEGER));
        entries.add(new SettingEntry(KEY_VOICE_ENABLED, isVoiceEnabled(), SettingType.BOOLEAN));
        entries.add(new SettingEntry(KEY_VOICE_AUTO_READ, isVoiceAutoReadEnabled(), SettingType.BOOLEAN));
        entries.add(new SettingEntry(KEY_VOICE_PITCH, getVoicePitch(), SettingType.FLOAT));
        entries.add(new SettingEntry(KEY_VOICE_SPEED, getVoiceSpeed(), SettingType.FLOAT));
        entries.add(new SettingEntry(KEY_STREAMING_ENABLED, isStreamingEnabled(), SettingType.BOOLEAN));
        entries.add(new SettingEntry(KEY_SHOW_THINKING, isShowThinkingEnabled(), SettingType.BOOLEAN));
        
        // Note: API key and Group ID are NOT exported for security
        // They must be re-entered by the user after import
        
        return entries;
    }
    
    @Override
    public boolean importSettings(List<SettingEntry> entries) {
        for (SettingEntry entry : entries) {
            switch (entry.getKey()) {
                case KEY_MODEL:
                    setModel(entry.getStringValue());
                    break;
                case KEY_TEMPERATURE:
                    setTemperature(entry.getFloatValue());
                    break;
                case KEY_MAX_TOKENS:
                    setMaxTokens(entry.getIntValue());
                    break;
                case KEY_CONTEXT_MESSAGES:
                    setContextMessages(entry.getIntValue());
                    break;
                case KEY_VOICE_ENABLED:
                    setBoolean(KEY_VOICE_ENABLED, entry.getBooleanValue());
                    break;
                case KEY_VOICE_AUTO_READ:
                    setBoolean(KEY_VOICE_AUTO_READ, entry.getBooleanValue());
                    break;
                case KEY_VOICE_PITCH:
                    setVoicePitch(entry.getFloatValue());
                    break;
                case KEY_VOICE_SPEED:
                    setVoiceSpeed(entry.getFloatValue());
                    break;
                case KEY_STREAMING_ENABLED:
                    setStreamingEnabled(entry.getBooleanValue());
                    break;
                case KEY_SHOW_THINKING:
                    setShowThinkingEnabled(entry.getBooleanValue());
                    break;
            }
        }
        return true;
    }
    
    /**
     * Enum representing configuration status.
     */
    public enum ConfigStatus {
        NOT_CONFIGURED,
        MISSING_API_KEY,
        MISSING_GROUP_ID,
        FULLY_CONFIGURED
    }
}
