package ohi.andre.consolelauncher.settings.modules;

import android.content.Context;

import ohi.andre.consolelauncher.settings.BaseSettingsModule;
import ohi.andre.consolelauncher.settings.ISettingsModule;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings module for general application preferences.
 * This module manages application-wide settings that don't fit into other categories.
 */
public class GeneralSettingsModule extends BaseSettingsModule {
    
    private static final String TAG = "GeneralSettings";
    public static final String MODULE_ID = "general_settings";
    
    // Preference keys
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_ANIMATIONS_ENABLED = "animations_enabled";
    private static final String KEY_HAPTIC_FEEDBACK = "haptic_feedback";
    private static final String KEY_SOUND_FEEDBACK = "sound_feedback";
    private static final String KEY_AUTO_UPDATE_CHECK = "auto_update_check";
    private static final String KEY_CRASH_REPORTING = "crash_reporting";
    private static final String KEY_ANALYTICS_ENABLED = "analytics_enabled";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_LOG_LEVEL = "log_level";
    private static final String KEY_SUGGESTIONS_ENABLED = "suggestions_enabled";
    private static final String KEY_SUGGESTION_COUNT = "suggestion_count";
    private static final String KEY_HISTORY_ENABLED = "history_enabled";
    private static final String KEY_HISTORY_SIZE = "history_size";
    private static final String KEY_QUICK_COMMANDS = "quick_commands";
    private static final String KEY_CONFIRM_EXIT = "confirm_exit";
    private static final String KEY_STARTUP_COMMAND = "startup_command";
    
    // Default values
    private static final boolean DEFAULT_ANIMATIONS_ENABLED = true;
    private static final boolean DEFAULT_HAPTIC_FEEDBACK = true;
    private static final boolean DEFAULT_SOUND_FEEDBACK = false;
    private static final boolean DEFAULT_AUTO_UPDATE_CHECK = true;
    private static final boolean DEFAULT_CRASH_REPORTING = true;
    private static final boolean DEFAULT_ANALYTICS_ENABLED = true;
    private static final String DEFAULT_LANGUAGE = "";
    private static final int DEFAULT_LOG_LEVEL = android.util.Log.WARN;
    private static final boolean DEFAULT_SUGGESTIONS_ENABLED = true;
    private static final int DEFAULT_SUGGESTION_COUNT = 5;
    private static final boolean DEFAULT_HISTORY_ENABLED = true;
    private static final int DEFAULT_HISTORY_SIZE = 100;
    private static final boolean DEFAULT_CONFIRM_EXIT = false;
    private static final String DEFAULT_STARTUP_COMMAND = "";
    
    public GeneralSettingsModule(Context context) {
        super(context, MODULE_ID);
    }
    
    @Override
    public void loadSettings() {
        Log.d(TAG, "General settings loaded");
    }
    
    @Override
    public void saveSettings() {
        clearChangedFlag();
        Log.d(TAG, "General settings saved");
    }
    
    @Override
    public void resetToDefaults() {
        clearAll();
        // Keep first_launch as true to show welcome on next load
        setBoolean(KEY_FIRST_LAUNCH, true);
        markAsChanged();
        notifyReset();
        Log.d(TAG, "General settings reset to defaults");
    }
    
    // ==================== First Launch ====================
    
    public boolean isFirstLaunch() {
        return getBoolean(KEY_FIRST_LAUNCH, true);
    }
    
    public void setFirstLaunch(boolean firstLaunch) {
        setBoolean(KEY_FIRST_LAUNCH, firstLaunch);
    }
    
    /**
     * Marks the first launch as complete.
     */
    public void completeFirstLaunch() {
        setBoolean(KEY_FIRST_LAUNCH, false);
    }
    
    // ==================== UI Feedback ====================
    
    public boolean isAnimationsEnabled() {
        return getBoolean(KEY_ANIMATIONS_ENABLED, DEFAULT_ANIMATIONS_ENABLED);
    }
    
    public void setAnimationsEnabled(boolean enabled) {
        setBoolean(KEY_ANIMATIONS_ENABLED, enabled);
    }
    
    public boolean isHapticFeedbackEnabled() {
        return getBoolean(KEY_HAPTIC_FEEDBACK, DEFAULT_HAPTIC_FEEDBACK);
    }
    
    public void setHapticFeedbackEnabled(boolean enabled) {
        setBoolean(KEY_HAPTIC_FEEDBACK, enabled);
    }
    
    public boolean isSoundFeedbackEnabled() {
        return getBoolean(KEY_SOUND_FEEDBACK, DEFAULT_SOUND_FEEDBACK);
    }
    
    public void setSoundFeedbackEnabled(boolean enabled) {
        setBoolean(KEY_SOUND_FEEDBACK, enabled);
    }
    
    // ==================== Privacy & Analytics ====================
    
    public boolean isAutoUpdateCheckEnabled() {
        return getBoolean(KEY_AUTO_UPDATE_CHECK, DEFAULT_AUTO_UPDATE_CHECK);
    }
    
    public void setAutoUpdateCheckEnabled(boolean enabled) {
        setBoolean(KEY_AUTO_UPDATE_CHECK, enabled);
    }
    
    public boolean isCrashReportingEnabled() {
        return getBoolean(KEY_CRASH_REPORTING, DEFAULT_CRASH_REPORTING);
    }
    
    public void setCrashReportingEnabled(boolean enabled) {
        setBoolean(KEY_CRASH_REPORTING, enabled);
    }
    
    public boolean isAnalyticsEnabled() {
        return getBoolean(KEY_ANALYTICS_ENABLED, DEFAULT_ANALYTICS_ENABLED);
    }
    
    public void setAnalyticsEnabled(boolean enabled) {
        setBoolean(KEY_ANALYTICS_ENABLED, enabled);
    }
    
    // ==================== Language ====================
    
    public String getLanguage() {
        return getString(KEY_LANGUAGE, DEFAULT_LANGUAGE);
    }
    
    public void setLanguage(String languageCode) {
        setString(KEY_LANGUAGE, languageCode != null ? languageCode : DEFAULT_LANGUAGE);
    }
    
    /**
     * Checks if a custom language is set.
     * 
     * @return true if custom language is set
     */
    public boolean hasCustomLanguage() {
        String language = getLanguage();
        return language != null && !language.isEmpty();
    }
    
    // ==================== Logging ====================
    
    public int getLogLevel() {
        return getInt(KEY_LOG_LEVEL, DEFAULT_LOG_LEVEL);
    }
    
    public void setLogLevel(int level) {
        // Clamp to valid log levels
        int clamped = Math.max(android.util.Log.VERBOSE, Math.min(android.util.Log.ERROR, level));
        setInt(KEY_LOG_LEVEL, clamped);
    }
    
    /**
     * Gets the log level as a string.
     * 
     * @return Log level name
     */
    public String getLogLevelName() {
        int level = getLogLevel();
        switch (level) {
            case android.util.Log.VERBOSE:
                return "VERBOSE";
            case android.util.Log.DEBUG:
                return "DEBUG";
            case android.util.Log.INFO:
                return "INFO";
            case android.util.Log.WARN:
                return "WARN";
            case android.util.Log.ERROR:
                return "ERROR";
            default:
                return "UNKNOWN";
        }
    }
    
    // ==================== Suggestions ====================
    
    public boolean isSuggestionsEnabled() {
        return getBoolean(KEY_SUGGESTIONS_ENABLED, DEFAULT_SUGGESTIONS_ENABLED);
    }
    
    public void setSuggestionsEnabled(boolean enabled) {
        setBoolean(KEY_SUGGESTIONS_ENABLED, enabled);
    }
    
    public int getSuggestionCount() {
        return getInt(KEY_SUGGESTION_COUNT, DEFAULT_SUGGESTION_COUNT);
    }
    
    public void setSuggestionCount(int count) {
        int clamped = Math.max(1, Math.min(20, count));
        setInt(KEY_SUGGESTION_COUNT, clamped);
    }
    
    // ==================== History ====================
    
    public boolean isHistoryEnabled() {
        return getBoolean(KEY_HISTORY_ENABLED, DEFAULT_HISTORY_ENABLED);
    }
    
    public void setHistoryEnabled(boolean enabled) {
        setBoolean(KEY_HISTORY_ENABLED, enabled);
    }
    
    public int getHistorySize() {
        return getInt(KEY_HISTORY_SIZE, DEFAULT_HISTORY_SIZE);
    }
    
    public void setHistorySize(int size) {
        int clamped = Math.max(10, Math.min(1000, size));
        setInt(KEY_HISTORY_SIZE, clamped);
    }
    
    // ==================== Exit & Startup ====================
    
    public boolean isConfirmExitEnabled() {
        return getBoolean(KEY_CONFIRM_EXIT, DEFAULT_CONFIRM_EXIT);
    }
    
    public void setConfirmExitEnabled(boolean enabled) {
        setBoolean(KEY_CONFIRM_EXIT, enabled);
    }
    
    public String getStartupCommand() {
        return getString(KEY_STARTUP_COMMAND, DEFAULT_STARTUP_COMMAND);
    }
    
    public void setStartupCommand(String command) {
        setString(KEY_STARTUP_COMMAND, command != null ? command : DEFAULT_STARTUP_COMMAND);
    }
    
    /**
     * Checks if a startup command is configured.
     * 
     * @return true if startup command is set
     */
    public boolean hasStartupCommand() {
        String command = getStartupCommand();
        return command != null && !command.isEmpty();
    }
    
    // ==================== Export/Import ====================
    
    @Override
    public List<SettingEntry> exportSettings() {
        List<SettingEntry> entries = new ArrayList<>();
        
        entries.add(new SettingEntry(KEY_FIRST_LAUNCH, isFirstLaunch(), SettingType.BOOLEAN));
        entries.add(new SettingEntry(KEY_ANIMATIONS_ENABLED, isAnimationsEnabled(), SettingType.BOOLEAN));
        entries.add(new SettingEntry(KEY_HAPTIC_FEEDBACK, isHapticFeedbackEnabled(), SettingType.BOOLEAN));
        entries.add(new SettingEntry(KEY_SOUND_FEEDBACK, isSoundFeedbackEnabled(), SettingType.BOOLEAN));
        entries.add(new SettingEntry(KEY_AUTO_UPDATE_CHECK, isAutoUpdateCheckEnabled(), SettingType.BOOLEAN));
        entries.add(new SettingEntry(KEY_CRASH_REPORTING, isCrashReportingEnabled(), SettingType.BOOLEAN));
        entries.add(new SettingEntry(KEY_ANALYTICS_ENABLED, isAnalyticsEnabled(), SettingType.BOOLEAN));
        entries.add(new SettingEntry(KEY_LANGUAGE, getLanguage(), SettingType.STRING));
        entries.add(new SettingEntry(KEY_LOG_LEVEL, getLogLevel(), SettingType.INTEGER));
        entries.add(new SettingEntry(KEY_SUGGESTIONS_ENABLED, isSuggestionsEnabled(), SettingType.BOOLEAN));
        entries.add(new SettingEntry(KEY_SUGGESTION_COUNT, getSuggestionCount(), SettingType.INTEGER));
        entries.add(new SettingEntry(KEY_HISTORY_ENABLED, isHistoryEnabled(), SettingType.BOOLEAN));
        entries.add(new SettingEntry(KEY_HISTORY_SIZE, getHistorySize(), SettingType.INTEGER));
        entries.add(new SettingEntry(KEY_CONFIRM_EXIT, isConfirmExitEnabled(), SettingType.BOOLEAN));
        entries.add(new SettingEntry(KEY_STARTUP_COMMAND, getStartupCommand(), SettingType.STRING));
        
        return entries;
    }
    
    @Override
    public boolean importSettings(List<SettingEntry> entries) {
        for (SettingEntry entry : entries) {
            switch (entry.getKey()) {
                case KEY_FIRST_LAUNCH:
                    setBoolean(KEY_FIRST_LAUNCH, entry.getBooleanValue());
                    break;
                case KEY_ANIMATIONS_ENABLED:
                    setBoolean(KEY_ANIMATIONS_ENABLED, entry.getBooleanValue());
                    break;
                case KEY_HAPTIC_FEEDBACK:
                    setBoolean(KEY_HAPTIC_FEEDBACK, entry.getBooleanValue());
                    break;
                case KEY_SOUND_FEEDBACK:
                    setBoolean(KEY_SOUND_FEEDBACK, entry.getBooleanValue());
                    break;
                case KEY_AUTO_UPDATE_CHECK:
                    setBoolean(KEY_AUTO_UPDATE_CHECK, entry.getBooleanValue());
                    break;
                case KEY_CRASH_REPORTING:
                    setBoolean(KEY_CRASH_REPORTING, entry.getBooleanValue());
                    break;
                case KEY_ANALYTICS_ENABLED:
                    setBoolean(KEY_ANALYTICS_ENABLED, entry.getBooleanValue());
                    break;
                case KEY_LANGUAGE:
                    setLanguage(entry.getStringValue());
                    break;
                case KEY_LOG_LEVEL:
                    setLogLevel(entry.getIntValue());
                    break;
                case KEY_SUGGESTIONS_ENABLED:
                    setSuggestionsEnabled(entry.getBooleanValue());
                    break;
                case KEY_SUGGESTION_COUNT:
                    setSuggestionCount(entry.getIntValue());
                    break;
                case KEY_HISTORY_ENABLED:
                    setHistoryEnabled(entry.getBooleanValue());
                    break;
                case KEY_HISTORY_SIZE:
                    setHistorySize(entry.getIntValue());
                    break;
                case KEY_CONFIRM_EXIT:
                    setConfirmExitEnabled(entry.getBooleanValue());
                    break;
                case KEY_STARTUP_COMMAND:
                    setStartupCommand(entry.getStringValue());
                    break;
            }
        }
        return true;
    }
    
    @Override
    public boolean validateSettings() {
        // Validate suggestion count
        if (getSuggestionCount() < 1 || getSuggestionCount() > 20) {
            return false;
        }
        
        // Validate history size
        if (getHistorySize() < 10 || getHistorySize() > 1000) {
            return false;
        }
        
        return true;
    }
}
