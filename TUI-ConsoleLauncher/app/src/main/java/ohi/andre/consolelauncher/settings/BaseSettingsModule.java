package ohi.andre.consolelauncher.settings;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Abstract base class providing common implementations for ISettingsModule.
 * This class handles the boilerplate code required for all settings modules
 * while allowing subclasses to focus on module-specific logic.
 */
public abstract class BaseSettingsModule implements ISettingsModule {
    
    protected final Context context;
    protected final String moduleId;
    protected final SharedPreferences preferences;
    protected final List<SettingsChangeListener> listeners;
    protected boolean unsavedChanges;
    
    /**
     * Creates a new BaseSettingsModule with the specified context and module ID.
     * 
     * @param context Application context
     * @param moduleId Unique identifier for this settings module
     */
    protected BaseSettingsModule(Context context, String moduleId) {
        this.context = context.getApplicationContext();
        this.moduleId = moduleId;
        this.preferences = createSharedPreferences();
        this.listeners = new CopyOnWriteArrayList<>();
        this.unsavedChanges = false;
    }
    
    /**
     * Creates the SharedPreferences instance for this module.
     * Subclasses can override to use encrypted preferences for sensitive data.
     * 
     * @return SharedPreferences instance for this module
     */
    protected SharedPreferences createSharedPreferences() {
        return context.getSharedPreferences("tui_settings_" + moduleId, Context.MODE_PRIVATE);
    }
    
    @Override
    public String getModuleId() {
        return moduleId;
    }
    
    @Override
    public void registerChangeListener(SettingsChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    @Override
    public void unregisterChangeListener(SettingsChangeListener listener) {
        listeners.remove(listener);
    }
    
    @Override
    public boolean hasUnsavedChanges() {
        return unsavedChanges;
    }
    
    /**
     * Marks that this module has unsaved changes.
     * This should be called whenever a setting value is modified.
     */
    protected void markAsChanged() {
        this.unsavedChanges = true;
    }
    
    /**
     * Clears the unsaved changes flag.
     * This should be called after saveSettings() completes successfully.
     */
    protected void clearChangedFlag() {
        this.unsavedChanges = false;
    }
    
    /**
     * Notifies all registered listeners that a setting has changed.
     * 
     * @param key The key of the setting that changed (can be null for bulk changes)
     */
    protected void notifyChange(String key) {
        for (SettingsChangeListener listener : listeners) {
            listener.onSettingsChanged(moduleId, key);
        }
    }
    
    /**
     * Notifies all registered listeners that multiple settings have changed.
     * 
     * @param keys Array of keys that changed (can contain null for bulk changes)
     */
    protected void notifyChanges(String[] keys) {
        for (SettingsChangeListener listener : listeners) {
            for (String key : keys) {
                listener.onSettingsChanged(moduleId, key);
            }
        }
    }
    
    /**
     * Notifies all registered listeners that all settings have been reset.
     */
    protected void notifyReset() {
        for (SettingsChangeListener listener : listeners) {
            listener.onSettingsReset(moduleId);
        }
    }
    
    /**
     * Gets a string preference value with a default fallback.
     * 
     * @param key The preference key
     * @param defaultValue The value to return if the key doesn't exist
     * @return The preference value or default
     */
    protected String getString(String key, String defaultValue) {
        return preferences.getString(key, defaultValue);
    }
    
    /**
     * Sets a string preference value and marks the module as changed.
     * 
     * @param key The preference key
     * @param value The value to set
     */
    protected void setString(String key, String value) {
        preferences.edit().putString(key, value).apply();
        markAsChanged();
        notifyChange(key);
    }
    
    /**
     * Gets an integer preference value with a default fallback.
     * 
     * @param key The preference key
     * @param defaultValue The value to return if the key doesn't exist
     * @return The preference value or default
     */
    protected int getInt(String key, int defaultValue) {
        return preferences.getInt(key, defaultValue);
    }
    
    /**
     * Sets an integer preference value and marks the module as changed.
     * 
     * @param key The preference key
     * @param value The value to set
     */
    protected void setInt(String key, int value) {
        preferences.edit().putInt(key, value).apply();
        markAsChanged();
        notifyChange(key);
    }
    
    /**
     * Gets a boolean preference value with a default fallback.
     * 
     * @param key The preference key
     * @param defaultValue The value to return if the key doesn't exist
     * @return The preference value or default
     */
    protected boolean getBoolean(String key, boolean defaultValue) {
        return preferences.getBoolean(key, defaultValue);
    }
    
    /**
     * Sets a boolean preference value and marks the module as changed.
     * 
     * @param key The preference key
     * @param value The value to set
     */
    protected void setBoolean(String key, boolean value) {
        preferences.edit().putBoolean(key, value).apply();
        markAsChanged();
        notifyChange(key);
    }
    
    /**
     * Gets a float preference value with a default fallback.
     * 
     * @param key The preference key
     * @param defaultValue The value to return if the key doesn't exist
     * @return The preference value or default
     */
    protected float getFloat(String key, float defaultValue) {
        return preferences.getFloat(key, defaultValue);
    }
    
    /**
     * Sets a float preference value and marks the module as changed.
     * 
     * @param key The preference key
     * @param value The value to set
     */
    protected void setFloat(String key, float value) {
        preferences.edit().putFloat(key, value).apply();
        markAsChanged();
        notifyChange(key);
    }
    
    /**
     * Gets a long preference value with a default fallback.
     * 
     * @param key The preference key
     * @param defaultValue The value to return if the key doesn't exist
     * @return The preference value or default
     */
    protected long getLong(String key, long defaultValue) {
        return preferences.getLong(key, defaultValue);
    }
    
    /**
     * Sets a long preference value and marks the module as changed.
     * 
     * @param key The preference key
     * @param value The value to set
     */
    protected void setLong(String key, long value) {
        preferences.edit().putLong(key, value).apply();
        markAsChanged();
        notifyChange(key);
    }
    
    /**
     * Removes a preference value and marks the module as changed.
     * 
     * @param key The preference key to remove
     */
    protected void remove(String key) {
        preferences.edit().remove(key).apply();
        markAsChanged();
        notifyChange(key);
    }
    
    /**
     * Clears all preferences in this module and marks as changed.
     */
    protected void clearAll() {
        preferences.edit().clear().apply();
        markAsChanged();
        notifyChange(null);
    }
    
    /**
     * Checks if a preference key exists.
     * 
     * @param key The preference key to check
     * @return true if the key exists, false otherwise
     */
    protected boolean contains(String key) {
        return preferences.contains(key);
    }
    
    @Override
    public List<SettingEntry> exportSettings() {
        List<SettingEntry> entries = new ArrayList<>();
        
        // Export all non-sensitive preferences
        for (String key : preferences.getAll().keySet()) {
            Object value = preferences.getAll().get(key);
            if (value != null) {
                SettingType type = getTypeForValue(value);
                entries.add(new SettingEntry(key, value, type));
            }
        }
        
        return entries;
    }
    
    /**
     * Determines the SettingType for a given value.
     * 
     * @param value The value to check
     * @return The corresponding SettingType
     */
    private SettingType getTypeForValue(Object value) {
        if (value instanceof String) {
            return SettingType.STRING;
        } else if (value instanceof Integer) {
            return SettingType.INTEGER;
        } else if (value instanceof Boolean) {
            return SettingType.BOOLEAN;
        } else if (value instanceof Float) {
            return SettingType.FLOAT;
        } else if (value instanceof Long) {
            return SettingType.LONG;
        } else if (value instanceof Double) {
            return SettingType.DOUBLE;
        }
        return SettingType.STRING;
    }
    
    @Override
    public boolean importSettings(List<SettingEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return false;
        }
        
        SharedPreferences.Editor editor = preferences.edit();
        
        for (SettingEntry entry : entries) {
            try {
                switch (entry.getType()) {
                    case STRING:
                        editor.putString(entry.getKey(), entry.getStringValue());
                        break;
                    case INTEGER:
                        editor.putInt(entry.getKey(), entry.getIntValue());
                        break;
                    case BOOLEAN:
                        editor.putBoolean(entry.getKey(), entry.getBooleanValue());
                        break;
                    case FLOAT:
                        editor.putFloat(entry.getKey(), entry.getFloatValue());
                        break;
                    case LONG:
                        Object longVal = entry.getValue();
                        if (longVal instanceof Long) {
                            editor.putLong(entry.getKey(), (Long) longVal);
                        } else if (longVal instanceof Integer) {
                            editor.putLong(entry.getKey(), ((Integer) longVal).longValue());
                        } else if (longVal instanceof String) {
                            try {
                                editor.putLong(entry.getKey(), Long.parseLong((String) longVal));
                            } catch (NumberFormatException e) {
                                editor.putString(entry.getKey(), (String) longVal);
                            }
                        }
                        break;
                    default:
                        editor.putString(entry.getKey(), entry.getStringValue());
                }
            } catch (Exception e) {
                // Skip invalid entries but continue with others
            }
        }
        
        editor.apply();
        loadSettings();
        markAsChanged();
        
        return true;
    }
    
    @Override
    public boolean validateSettings() {
        // Default implementation always returns true
        // Subclasses should override to provide specific validation
        return true;
    }
    
    /**
     * Returns the SharedPreferences for this module.
     * Use with caution - direct modifications bypass change tracking.
     * 
     * @return The SharedPreferences instance
     */
    protected SharedPreferences getPreferences() {
        return preferences;
    }
}
