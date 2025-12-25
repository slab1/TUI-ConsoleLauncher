package ohi.andre.consolelauncher.settings.legacy;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import ohi.andre.consolelauncher.settings.BaseSettingsModule;
import ohi.andre.consolelauncher.settings.ISettingsModule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Legacy adapter for theme settings.
 * This adapter wraps the existing T-UI theme configuration system
 * and exposes it through the unified settings architecture.
 */
public class ThemeSettingsAdapter extends BaseSettingsModule {
    
    private static final String TAG = "ThemeSettingsAdapter";
    public static final String MODULE_ID = "theme_settings";
    
    private static final String PREFS_NAME = "tui_theme";
    private static final String LEGACY_FILE = "theme.xml";
    
    // Theme preferences
    private static final String KEY_BACKGROUND_COLOR = "background_color";
    private static final String KEY_TEXT_COLOR = "text_color";
    private static final String KEY_ERROR_COLOR = "error_color";
    private static final String KEY_INFO_COLOR = "info_color";
    private static final String KEY_SUCCESS_COLOR = "success_color";
    private static final String KEY_ACCENT_COLOR = "accent_color";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_FONT_NAME = "font_name";
    private static final String KEY_BG_IMAGE = "bg_image";
    private static final String KEY_HIGHLIGHT_COLOR = "highlight_color";
    private static final String KEY_CMD_INPUT_COLOR = "cmd_input_color";
    private static final String KEY_SUGGESTION_COLOR = "suggestion_color";
    private static final String KEY_SUGGESTION_HIGHLIGHT = "suggestion_highlight";
    
    // Default theme values
    private static final String DEFAULT_BACKGROUND = "#000000";
    private static final String DEFAULT_TEXT = "#00FF00";
    private static final String DEFAULT_ERROR = "#FF0000";
    private static final String DEFAULT_INFO = "#FFFF00";
    private static final String DEFAULT_SUCCESS = "#00FFFF";
    private static final String DEFAULT_ACCENT = "#0088FF";
    private static final int DEFAULT_FONT_SIZE = 14;
    
    private SharedPreferences legacyPreferences;
    
    public ThemeSettingsAdapter(Context context) {
        super(context, MODULE_ID);
        this.legacyPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    @Override
    public void loadSettings() {
        // First try to migrate from legacy XML if exists
        migrateFromLegacyIfNeeded();
        
        Log.d(TAG, "Theme settings loaded");
    }
    
    /**
     * Migrates settings from legacy theme.xml file if it exists
     * and no modern preferences are set.
     */
    private void migrateFromLegacyIfNeeded() {
        File legacyFile = new File(context.getFilesDir(), LEGACY_FILE);
        
        if (legacyFile.exists()) {
            // Check if we already migrated
            if (legacyPreferences.getBoolean("migrated", false)) {
                return;
            }
            
            // Parse legacy theme file
            Map<String, String> legacySettings = parseLegacyThemeFile(legacyFile);
            
            if (!legacySettings.isEmpty()) {
                Log.d(TAG, "Migrating from legacy theme.xml");
                
                // Apply legacy values to modern preferences
                SharedPreferences.Editor editor = legacyPreferences.edit();
                
                if (legacySettings.containsKey("background")) {
                    editor.putString(KEY_BACKGROUND_COLOR, legacySettings.get("background"));
                }
                if (legacySettings.containsKey("text")) {
                    editor.putString(KEY_TEXT_COLOR, legacySettings.get("text"));
                }
                if (legacySettings.containsKey("error")) {
                    editor.putString(KEY_ERROR_COLOR, legacySettings.get("error"));
                }
                if (legacySettings.containsKey("info")) {
                    editor.putString(KEY_INFO_COLOR, legacySettings.get("info"));
                }
                if (legacySettings.containsKey("success")) {
                    editor.putString(KEY_SUCCESS_COLOR, legacySettings.get("success"));
                }
                if (legacySettings.containsKey("accent")) {
                    editor.putString(KEY_ACCENT_COLOR, legacySettings.get("accent"));
                }
                if (legacySettings.containsKey("highlight")) {
                    editor.putString(KEY_HIGHLIGHT_COLOR, legacySettings.get("highlight"));
                }
                if (legacySettings.containsKey("input")) {
                    editor.putString(KEY_CMD_INPUT_COLOR, legacySettings.get("input"));
                }
                if (legacySettings.containsKey("suggestion")) {
                    editor.putString(KEY_SUGGESTION_COLOR, legacySettings.get("suggestion"));
                }
                if (legacySettings.containsKey("suggestionHigh")) {
                    editor.putString(KEY_SUGGESTION_HIGHLIGHT, legacySettings.get("suggestionHigh"));
                }
                if (legacySettings.containsKey("bg")) {
                    editor.putString(KEY_BG_IMAGE, legacySettings.get("bg"));
                }
                
                // Mark as migrated
                editor.putBoolean("migrated", true);
                editor.apply();
                
                Log.d(TAG, "Theme migration complete");
            }
        }
    }
    
    /**
     * Parses the legacy theme.xml file format.
     * 
     * @param file Legacy theme file
     * @return Map of theme properties
     */
    private Map<String, String> parseLegacyThemeFile(File file) {
        Map<String, String> settings = new HashMap<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            Pattern pattern = Pattern.compile("<(\\w+)>(#[0-9A-Fa-f]+)</\\1>");
            
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String key = matcher.group(1);
                    String value = matcher.group(2);
                    settings.put(key, value);
                }
            }
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to parse legacy theme file", e);
        }
        
        return settings;
    }
    
    @Override
    public void saveSettings() {
        clearChangedFlag();
        Log.d(TAG, "Theme settings saved");
    }
    
    @Override
    public void resetToDefaults() {
        legacyPreferences.edit()
            .clear()
            .putBoolean("migrated", true)
            .apply();
        
        markAsChanged();
        notifyReset();
        Log.d(TAG, "Theme settings reset to defaults");
    }
    
    // ==================== Color Getters/Setters ====================
    
    public String getBackgroundColor() {
        return legacyPreferences.getString(KEY_BACKGROUND_COLOR, DEFAULT_BACKGROUND);
    }
    
    public void setBackgroundColor(String color) {
        legacyPreferences.edit().putString(KEY_BACKGROUND_COLOR, color).apply();
        markAsChanged();
        notifyChange(KEY_BACKGROUND_COLOR);
    }
    
    public String getTextColor() {
        return legacyPreferences.getString(KEY_TEXT_COLOR, DEFAULT_TEXT);
    }
    
    public void setTextColor(String color) {
        legacyPreferences.edit().putString(KEY_TEXT_COLOR, color).apply();
        markAsChanged();
        notifyChange(KEY_TEXT_COLOR);
    }
    
    public String getErrorColor() {
        return legacyPreferences.getString(KEY_ERROR_COLOR, DEFAULT_ERROR);
    }
    
    public void setErrorColor(String color) {
        legacyPreferences.edit().putString(KEY_ERROR_COLOR, color).apply();
        markAsChanged();
        notifyChange(KEY_ERROR_COLOR);
    }
    
    public String getInfoColor() {
        return legacyPreferences.getString(KEY_INFO_COLOR, DEFAULT_INFO);
    }
    
    public void setInfoColor(String color) {
        legacyPreferences.edit().putString(KEY_INFO_COLOR, color).apply();
        markAsChanged();
        notifyChange(KEY_INFO_COLOR);
    }
    
    public String getSuccessColor() {
        return legacyPreferences.getString(KEY_SUCCESS_COLOR, DEFAULT_SUCCESS);
    }
    
    public void setSuccessColor(String color) {
        legacyPreferences.edit().putString(KEY_SUCCESS_COLOR, color).apply();
        markAsChanged();
        notifyChange(KEY_SUCCESS_COLOR);
    }
    
    public String getAccentColor() {
        return legacyPreferences.getString(KEY_ACCENT_COLOR, DEFAULT_ACCENT);
    }
    
    public void setAccentColor(String color) {
        legacyPreferences.edit().putString(KEY_ACCENT_COLOR, color).apply();
        markAsChanged();
        notifyChange(KEY_ACCENT_COLOR);
    }
    
    public String getHighlightColor() {
        return legacyPreferences.getString(KEY_HIGHLIGHT_COLOR, DEFAULT_ACCENT);
    }
    
    public void setHighlightColor(String color) {
        legacyPreferences.edit().putString(KEY_HIGHLIGHT_COLOR, color).apply();
        markAsChanged();
        notifyChange(KEY_HIGHLIGHT_COLOR);
    }
    
    public String getCmdInputColor() {
        return legacyPreferences.getString(KEY_CMD_INPUT_COLOR, DEFAULT_TEXT);
    }
    
    public void setCmdInputColor(String color) {
        legacyPreferences.edit().putString(KEY_CMD_INPUT_COLOR, color).apply();
        markAsChanged();
        notifyChange(KEY_CMD_INPUT_COLOR);
    }
    
    public String getSuggestionColor() {
        return legacyPreferences.getString(KEY_SUGGESTION_COLOR, "#888888");
    }
    
    public void setSuggestionColor(String color) {
        legacyPreferences.edit().putString(KEY_SUGGESTION_COLOR, color).apply();
        markAsChanged();
        notifyChange(KEY_SUGGESTION_COLOR);
    }
    
    public String getSuggestionHighlight() {
        return legacyPreferences.getString(KEY_SUGGESTION_HIGHLIGHT, DEFAULT_ACCENT);
    }
    
    public void setSuggestionHighlight(String color) {
        legacyPreferences.edit().putString(KEY_SUGGESTION_HIGHLIGHT, color).apply();
        markAsChanged();
        notifyChange(KEY_SUGGESTION_HIGHLIGHT);
    }
    
    // ==================== Font Settings ====================
    
    public int getFontSize() {
        return legacyPreferences.getInt(KEY_FONT_SIZE, DEFAULT_FONT_SIZE);
    }
    
    public void setFontSize(int size) {
        legacyPreferences.edit().putInt(KEY_FONT_SIZE, size).apply();
        markAsChanged();
        notifyChange(KEY_FONT_SIZE);
    }
    
    public String getFontName() {
        return legacyPreferences.getString(KEY_FONT_NAME, "monospace");
    }
    
    public void setFontName(String fontName) {
        legacyPreferences.edit().putString(KEY_FONT_NAME, fontName).apply();
        markAsChanged();
        notifyChange(KEY_FONT_NAME);
    }
    
    // ==================== Background Settings ====================
    
    public String getBackgroundImage() {
        return legacyPreferences.getString(KEY_BG_IMAGE, "");
    }
    
    public void setBackgroundImage(String imagePath) {
        legacyPreferences.edit().putString(KEY_BG_IMAGE, imagePath).apply();
        markAsChanged();
        notifyChange(KEY_BG_IMAGE);
    }
    
    // ==================== Export/Import ====================
    
    @Override
    public List<SettingEntry> exportSettings() {
        List<SettingEntry> entries = new ArrayList<>();
        
        entries.add(new SettingEntry(KEY_BACKGROUND_COLOR, getBackgroundColor(), SettingType.STRING));
        entries.add(new SettingEntry(KEY_TEXT_COLOR, getTextColor(), SettingType.STRING));
        entries.add(new SettingEntry(KEY_ERROR_COLOR, getErrorColor(), SettingType.STRING));
        entries.add(new SettingEntry(KEY_INFO_COLOR, getInfoColor(), SettingType.STRING));
        entries.add(new SettingEntry(KEY_SUCCESS_COLOR, getSuccessColor(), SettingType.STRING));
        entries.add(new SettingEntry(KEY_ACCENT_COLOR, getAccentColor(), SettingType.STRING));
        entries.add(new SettingEntry(KEY_HIGHLIGHT_COLOR, getHighlightColor(), SettingType.STRING));
        entries.add(new SettingEntry(KEY_CMD_INPUT_COLOR, getCmdInputColor(), SettingType.STRING));
        entries.add(new SettingEntry(KEY_SUGGESTION_COLOR, getSuggestionColor(), SettingType.STRING));
        entries.add(new SettingEntry(KEY_SUGGESTION_HIGHLIGHT, getSuggestionHighlight(), SettingType.STRING));
        entries.add(new SettingEntry(KEY_FONT_SIZE, getFontSize(), SettingType.INTEGER));
        entries.add(new SettingEntry(KEY_FONT_NAME, getFontName(), SettingType.STRING));
        entries.add(new SettingEntry(KEY_BG_IMAGE, getBackgroundImage(), SettingType.STRING));
        
        return entries;
    }
    
    @Override
    public boolean importSettings(List<SettingEntry> entries) {
        SharedPreferences.Editor editor = legacyPreferences.edit();
        
        for (SettingEntry entry : entries) {
            switch (entry.getKey()) {
                case KEY_BACKGROUND_COLOR:
                    editor.putString(KEY_BACKGROUND_COLOR, entry.getStringValue());
                    break;
                case KEY_TEXT_COLOR:
                    editor.putString(KEY_TEXT_COLOR, entry.getStringValue());
                    break;
                case KEY_ERROR_COLOR:
                    editor.putString(KEY_ERROR_COLOR, entry.getStringValue());
                    break;
                case KEY_INFO_COLOR:
                    editor.putString(KEY_INFO_COLOR, entry.getStringValue());
                    break;
                case KEY_SUCCESS_COLOR:
                    editor.putString(KEY_SUCCESS_COLOR, entry.getStringValue());
                    break;
                case KEY_ACCENT_COLOR:
                    editor.putString(KEY_ACCENT_COLOR, entry.getStringValue());
                    break;
                case KEY_HIGHLIGHT_COLOR:
                    editor.putString(KEY_HIGHLIGHT_COLOR, entry.getStringValue());
                    break;
                case KEY_CMD_INPUT_COLOR:
                    editor.putString(KEY_CMD_INPUT_COLOR, entry.getStringValue());
                    break;
                case KEY_SUGGESTION_COLOR:
                    editor.putString(KEY_SUGGESTION_COLOR, entry.getStringValue());
                    break;
                case KEY_SUGGESTION_HIGHLIGHT:
                    editor.putString(KEY_SUGGESTION_HIGHLIGHT, entry.getStringValue());
                    break;
                case KEY_FONT_SIZE:
                    editor.putInt(KEY_FONT_SIZE, entry.getIntValue());
                    break;
                case KEY_FONT_NAME:
                    editor.putString(KEY_FONT_NAME, entry.getStringValue());
                    break;
                case KEY_BG_IMAGE:
                    editor.putString(KEY_BG_IMAGE, entry.getStringValue());
                    break;
            }
        }
        
        editor.apply();
        loadSettings();
        markAsChanged();
        
        return true;
    }
}
