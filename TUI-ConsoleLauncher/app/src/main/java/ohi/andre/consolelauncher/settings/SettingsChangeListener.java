package ohi.andre.consolelauncher.settings;

/**
 * Interface for receiving notifications when settings change.
 * Implementations can register with any ISettingsModule to receive
 * callbacks when that module's settings are modified.
 */
public interface SettingsChangeListener {
    
    /**
     * Called when a setting value changes in a registered module.
     * 
     * @param moduleId The ID of the module that changed
     * @param key The key of the setting that changed (null for bulk changes)
     */
    void onSettingsChanged(String moduleId, String key);
    
    /**
     * Called when all settings in a module are reset to defaults.
     * 
     * @param moduleId The ID of the module that was reset
     */
    void onSettingsReset(String moduleId);
    
    /**
     * Called when settings are loaded from persistent storage.
     * This is called after initial load and after explicit reload operations.
     * 
     * @param moduleId The ID of the module that was loaded
     */
    void onSettingsLoaded(String moduleId);
    
    /**
     * Called when settings are saved to persistent storage.
     * 
     * @param moduleId The ID of the module that was saved
     */
    void onSettingsSaved(String moduleId);
}
