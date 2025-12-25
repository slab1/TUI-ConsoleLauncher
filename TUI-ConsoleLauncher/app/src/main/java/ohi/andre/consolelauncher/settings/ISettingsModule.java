package ohi.andre.consolelauncher.settings;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.List;

/**
 * Interface defining the contract for all settings modules in the T-UI ConsoleLauncher.
 * Each module manages a specific category of configuration and must implement these
 * lifecycle and data management methods.
 */
public interface ISettingsModule {
    
    /**
     * Returns a unique identifier for this settings module.
     * This ID is used for registration in the GlobalSettingsManager and for
     * identifying the module in change notifications and exports.
     * 
     * @return Unique module identifier (e.g., "ai_settings", "theme_settings")
     */
    String getModuleId();
    
    /**
     * Loads settings from persistent storage into memory.
     * This method is called during application startup and when settings are reset.
     * Implementations should handle missing or corrupted data gracefully.
     */
    void loadSettings();
    
    /**
     * Saves current settings from memory to persistent storage.
     * This method should be called after any settings modification.
     * Implementations should ensure atomic writes to prevent data corruption.
     */
    void saveSettings();
    
    /**
     * Resets all settings in this module to their default values.
     * After calling this method, loadSettings() should restore defaults.
     * This method should also clear any cached data and notify listeners.
     */
    void resetToDefaults();
    
    /**
     * Exports all settings as a list of key-value pairs for backup purposes.
     * Sensitive values should be masked or excluded in the export.
     * 
     * @return List of setting entries suitable for serialization
     */
    List<SettingEntry> exportSettings();
    
    /**
     * Imports settings from a list of key-value pairs.
     * This is used for restoring settings from backup.
     * Implementations should validate imported values before applying them.
     * 
     * @param entries List of setting entries to import
     * @return true if import was successful, false otherwise
     */
    boolean importSettings(List<SettingEntry> entries);
    
    /**
     * Registers a listener to receive notifications when settings change.
     * 
     * @param listener The SettingsChangeListener to register
     */
    void registerChangeListener(SettingsChangeListener listener);
    
    /**
     * Unregisters a previously registered change listener.
     * 
     * @param listener The SettingsChangeListener to unregister
     */
    void unregisterChangeListener(SettingsChangeListener listener);
    
    /**
     * Checks if this module has any unsaved changes.
     * This is used to determine if saveSettings() needs to be called.
     * 
     * @return true if there are unsaved changes, false otherwise
     */
    boolean hasUnsavedChanges();
    
    /**
     * Validates the current settings for correctness.
     * This method should check all settings against valid ranges and formats.
     * 
     * @return true if all settings are valid, false otherwise
     */
    boolean validateSettings();
    
    /**
     * Represents a single setting entry for import/export operations.
     */
    class SettingEntry {
        private final String key;
        private final Object value;
        private final SettingType type;
        
        public SettingEntry(String key, Object value, SettingType type) {
            this.key = key;
            this.value = value;
            this.type = type;
        }
        
        public String getKey() {
            return key;
        }
        
        public Object getValue() {
            return value;
        }
        
        public SettingType getType() {
            return type;
        }
        
        public String getStringValue() {
            return value != null ? value.toString() : "";
        }
        
        public int getIntValue() {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return 0;
        }
        
        public boolean getBooleanValue() {
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            return false;
        }
        
        public float getFloatValue() {
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            }
            return 0f;
        }
    }
    
    /**
     * Enum representing the data type of a setting value.
     */
    enum SettingType {
        STRING,
        INTEGER,
        BOOLEAN,
        FLOAT,
        LONG,
        DOUBLE,
        STRING_ARRAY
    }
}
