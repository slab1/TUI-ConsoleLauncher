package ohi.andre.consolelauncher.settings.legacy;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import ohi.andre.consolelauncher.settings.BaseSettingsModule;
import ohi.andre.consolelauncher.settings.ISettingsModule;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Legacy adapter for alias settings.
 * This adapter wraps the existing T-UI alias configuration system
 * and exposes it through the unified settings architecture.
 */
public class AliasSettingsAdapter extends BaseSettingsModule {
    
    private static final String TAG = "AliasSettingsAdapter";
    public static final String MODULE_ID = "alias_settings";
    
    private static final String PREFS_NAME = "tui_alias";
    private static final String LEGACY_FILE = "cmd_alias.txt";
    
    // Preference keys
    private static final String KEY_ALIAS_COUNT = "alias_count";
    private static final String KEY_ENABLED = "enabled";
    
    private SharedPreferences legacyPreferences;
    private Map<String, String> aliasCache;
    private File legacyFile;
    
    public AliasSettingsAdapter(Context context) {
        super(context, MODULE_ID);
        this.legacyPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.aliasCache = new ConcurrentHashMap<>();
        this.legacyFile = new File(context.getFilesDir(), LEGACY_FILE);
    }
    
    @Override
    public void loadSettings() {
        // First try to migrate from legacy file
        migrateFromLegacyIfNeeded();
        
        // Load aliases from modern preferences
        loadFromPreferences();
        
        Log.d(TAG, "Alias settings loaded, " + aliasCache.size() + " aliases");
    }
    
    /**
     * Migrates aliases from legacy cmd_alias.txt file if it exists.
     */
    private void migrateFromLegacyIfNeeded() {
        if (!legacyFile.exists()) {
            return;
        }
        
        // Check if already migrated
        if (legacyPreferences.getBoolean("migrated", false)) {
            return;
        }
        
        Log.d(TAG, "Migrating from legacy cmd_alias.txt");
        
        Map<String, String> legacyAliases = parseLegacyAliasFile(legacyFile);
        
        if (!legacyAliases.isEmpty()) {
            SharedPreferences.Editor editor = legacyPreferences.edit();
            
            for (Map.Entry<String, String> entry : legacyAliases.entrySet()) {
                editor.putString("alias_" + entry.getKey(), entry.getValue());
            }
            
            editor.putInt(KEY_ALIAS_COUNT, legacyAliases.size());
            editor.putBoolean("migrated", true);
            editor.apply();
            
            Log.d(TAG, "Migrated " + legacyAliases.size() + " aliases");
        }
    }
    
    /**
     * Parses the legacy alias file format.
     * Format: alias=command
     * Lines starting with # are comments
     * 
     * @param file Legacy alias file
     * @return Map of alias to command
     */
    private Map<String, String> parseLegacyAliasFile(File file) {
        Map<String, String> aliases = new HashMap<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            
            while ((line = reader.readLine()) != null) {
                // Skip comments and empty lines
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // Parse alias=command format
                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String alias = line.substring(0, equalsIndex).trim();
                    String command = line.substring(equalsIndex + 1).trim();
                    
                    if (!alias.isEmpty() && !command.isEmpty()) {
                        aliases.put(alias, command);
                    }
                }
            }
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to parse legacy alias file", e);
        }
        
        return aliases;
    }
    
    /**
     * Loads aliases from SharedPreferences.
     */
    private void loadFromPreferences() {
        aliasCache.clear();
        
        // Get all alias keys
        Map<String, ?> allPrefs = legacyPreferences.getAll();
        
        for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
            if (entry.getKey().startsWith("alias_")) {
                String alias = entry.getKey().substring(6); // Remove "alias_" prefix
                Object value = entry.getValue();
                
                if (value instanceof String) {
                    aliasCache.put(alias, (String) value);
                }
            }
        }
    }
    
    @Override
    public void saveSettings() {
        clearChangedFlag();
        Log.d(TAG, "Alias settings saved");
    }
    
    @Override
    public void resetToDefaults() {
        aliasCache.clear();
        
        SharedPreferences.Editor editor = legacyPreferences.edit();
        editor.clear();
        editor.putBoolean("migrated", true);
        editor.apply();
        
        markAsChanged();
        notifyReset();
        Log.d(TAG, "Alias settings reset to defaults");
    }
    
    // ==================== Alias Management ====================
    
    /**
     * Gets all registered aliases.
     * 
     * @return Map of alias to command
     */
    public Map<String, String> getAllAliases() {
        return new HashMap<>(aliasCache);
    }
    
    /**
     * Gets all alias names.
     * 
     * @return Set of alias names
     */
    public Set<String> getAliasNames() {
        return aliasCache.keySet();
    }
    
    /**
     * Gets the command for an alias.
     * 
     * @param alias Alias name
     * @return Command string, or null if not found
     */
    public String getAlias(String alias) {
        return aliasCache.get(alias);
    }
    
    /**
     * Sets an alias.
     * 
     * @param alias Alias name
     * @param command Command to execute
     */
    public void setAlias(String alias, String command) {
        if (alias == null || alias.isEmpty() || command == null) {
            return;
        }
        
        aliasCache.put(alias, command);
        legacyPreferences.edit()
            .putString("alias_" + alias, command)
            .putInt(KEY_ALIAS_COUNT, aliasCache.size())
            .apply();
        
        markAsChanged();
        notifyChange("alias_" + alias);
        
        // Update legacy file for compatibility
        updateLegacyFile();
    }
    
    /**
     * Removes an alias.
     * 
     * @param alias Alias name to remove
     * @return true if alias was removed
     */
    public boolean removeAlias(String alias) {
        if (!aliasCache.containsKey(alias)) {
            return false;
        }
        
        aliasCache.remove(alias);
        legacyPreferences.edit()
            .remove("alias_" + alias)
            .putInt(KEY_ALIAS_COUNT, aliasCache.size())
            .apply();
        
        markAsChanged();
        notifyChange("alias_" + alias);
        
        // Update legacy file for compatibility
        updateLegacyFile();
        
        return true;
    }
    
    /**
     * Checks if an alias exists.
     * 
     * @param alias Alias name
     * @return true if alias exists
     */
    public boolean hasAlias(String alias) {
        return aliasCache.containsKey(alias);
    }
    
    /**
     * Gets the alias count.
     * 
     * @return Number of aliases
     */
    public int getAliasCount() {
        return aliasCache.size();
    }
    
    /**
     * Checks if aliases are enabled.
     * 
     * @return true if aliases are enabled
     */
    public boolean isEnabled() {
        return legacyPreferences.getBoolean(KEY_ENABLED, true);
    }
    
    /**
     * Sets whether aliases are enabled.
     * 
     * @param enabled True to enable aliases
     */
    public void setEnabled(boolean enabled) {
        legacyPreferences.edit().putBoolean(KEY_ENABLED, enabled).apply();
        markAsChanged();
        notifyChange(KEY_ENABLED);
    }
    
    /**
     * Exports aliases to legacy format for backup.
     * 
     * @return Export string in legacy format
     */
    public String exportToLegacyFormat() {
        StringBuilder sb = new StringBuilder();
        sb.append("# T-UI Aliases\n");
        sb.append("# Format: alias=command\n\n");
        
        for (Map.Entry<String, String> entry : aliasCache.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Updates the legacy alias file for compatibility.
     */
    private void updateLegacyFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(legacyFile))) {
            writer.write("# T-UI Aliases\n");
            writer.write("# This file is auto-generated. Edit via 'alias' command.\n\n");
            
            for (Map.Entry<String, String> entry : aliasCache.entrySet()) {
                writer.write(entry.getKey());
                writer.write("=");
                writer.write(entry.getValue());
                writer.newLine();
            }
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to update legacy alias file", e);
        }
    }
    
    /**
     * Imports aliases from legacy format string.
     * 
     * @param content Legacy format content
     * @return Number of aliases imported
     */
    public int importFromLegacyFormat(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        
        String[] lines = content.split("\n");
        int count = 0;
        
        for (String line : lines) {
            line = line.trim();
            
            // Skip comments and empty lines
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            
            // Parse alias=command format
            int equalsIndex = line.indexOf('=');
            if (equalsIndex > 0) {
                String alias = line.substring(0, equalsIndex).trim();
                String command = line.substring(equalsIndex + 1).trim();
                
                if (!alias.isEmpty() && !command.isEmpty()) {
                    setAlias(alias, command);
                    count++;
                }
            }
        }
        
        return count;
    }
    
    /**
     * Gets the legacy file path.
     * 
     * @return Legacy file path
     */
    public String getLegacyFilePath() {
        return legacyFile.getAbsolutePath();
    }
    
    // ==================== Export/Import ====================
    
    @Override
    public List<SettingEntry> exportSettings() {
        List<SettingEntry> entries = new ArrayList<>();
        
        entries.add(new SettingEntry(KEY_ENABLED, isEnabled(), SettingType.BOOLEAN));
        entries.add(new SettingEntry(KEY_ALIAS_COUNT, getAliasCount(), SettingType.INTEGER));
        
        // Export each alias as individual entry
        for (Map.Entry<String, String> entry : aliasCache.entrySet()) {
            entries.add(new SettingEntry("alias_" + entry.getKey(), entry.getValue(), SettingType.STRING));
        }
        
        return entries;
    }
    
    @Override
    public boolean importSettings(List<SettingEntry> entries) {
        SharedPreferences.Editor editor = legacyPreferences.edit();
        int aliasCount = 0;
        
        for (SettingEntry entry : entries) {
            if (entry.getKey().startsWith("alias_")) {
                String alias = entry.getKey().substring(6);
                aliasCache.put(alias, entry.getStringValue());
                editor.putString("alias_" + alias, entry.getStringValue());
                aliasCount++;
            } else if (KEY_ENABLED.equals(entry.getKey())) {
                editor.putBoolean(KEY_ENABLED, entry.getBooleanValue());
            } else if (KEY_ALIAS_COUNT.equals(entry.getKey())) {
                editor.putInt(KEY_ALIAS_COUNT, entry.getIntValue());
            }
        }
        
        editor.putInt(KEY_ALIAS_COUNT, aliasCount);
        editor.apply();
        
        markAsChanged();
        
        return true;
    }
}
