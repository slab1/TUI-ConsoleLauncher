package ohi.andre.consolelauncher.commands.smartlauncher.developer.settings.base;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;

import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Abstract base class for all settings modules.
 * Provides common functionality for settings storage, validation, and lifecycle management.
 */
public abstract class BaseSettingsModule<T> implements ISettingsModule {
    private static final String TAG = "BaseSettingsModule";

    protected WeakReference<Context> contextRef;
    protected SharedPreferences sharedPreferences;
    protected SharedPreferences secureSharedPreferences;

    protected final String moduleId;
    protected final String moduleName;
    protected final String moduleCategory;

    protected final Map<String, Object> currentSettings;
    protected final Set<String> sensitiveKeys;
    protected final ExecutorService executor;

    protected boolean initialized = false;

    protected BaseSettingsModule(String moduleId, String moduleName, String moduleCategory) {
        this.moduleId = moduleId;
        this.moduleName = moduleName;
        this.moduleCategory = moduleCategory;
        this.currentSettings = new ConcurrentHashMap<>();
        this.sensitiveKeys = new HashSet<>();
    }

    @Override
    public String getModuleId() {
        return moduleId;
    }

    @Override
    public String getModuleName() {
        return moduleName;
    }

    @Override
    public String getModuleCategory() {
        return moduleCategory;
    }

    @Override
    public boolean isInitialized() {
        return initialized && contextRef.get() != null;
    }

    @Override
    public void initialize(Context context) {
        if (context == null) {
            Log.e(TAG, "Cannot initialize " + moduleId + " with null context");
            return;
        }

        contextRef = new WeakReference<>(context.getApplicationContext());
        String prefsName = "settings_" + moduleId;

        try {
            // Initialize regular SharedPreferences
            sharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);

            // Initialize encrypted SharedPreferences for sensitive data
            File securePrefsFile = new File(context.getFilesDir(), "shared_prefs/" + prefsName + "_secure.xml");
            if (securePrefsFile.getParentFile() != null) {
                securePrefsFile.getParentFile().mkdirs();
            }

            secureSharedPreferences = EncryptedSharedPreferences.create(
                context,
                prefsName + "_secure",
                securePrefsFile,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            // Load settings from storage
            loadSettingsSync();

            initialized = true;
            Log.d(TAG, "Module " + moduleId + " initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing module " + moduleId, e);
        }
    }

    /**
     * Load settings from storage synchronously
     */
    protected void loadSettingsSync() {
        if (sharedPreferences == null) return;

        // Start with defaults
        Map<String, Object> defaults = getDefaults();
        currentSettings.clear();
        currentSettings.putAll(defaults);

        // Load from SharedPreferences
        Map<String, ?> allPrefs = sharedPreferences.getAll();
        for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
            currentSettings.put(entry.getKey(), entry.getValue());
        }

        // Load sensitive data from encrypted storage
        if (secureSharedPreferences != null) {
            for (String key : sensitiveKeys) {
                String value = secureSharedPreferences.getString(key, null);
                if (value != null) {
                    currentSettings.put(key, value);
                }
            }
        }
    }

    /**
     * Get a setting value
     */
    protected Object getSetting(String key) {
        return currentSettings.get(key);
    }

    /**
     * Set a setting value with validation
     */
    protected boolean setSetting(String key, Object value) {
        ValidationResult result = validate(key, value);
        if (!result.isValid()) {
            Log.w(TAG, "Validation failed for " + key + ": " + result.getErrorMessage());
            return false;
        }

        Object finalValue = result.hasCorrection() ? result.getCorrectedValue() : value;
        currentSettings.put(key, finalValue);

        // Save to appropriate storage
        saveSettingToStorage(key, finalValue);

        // Notify of change
        onSettingChanged(key, finalValue);

        return true;
    }

    /**
     * Save setting to appropriate storage (regular or encrypted)
     */
    private void saveSettingToStorage(String key, Object value) {
        boolean isSensitive = sensitiveKeys.contains(key);

        if (isSensitive && secureSharedPreferences != null) {
            SharedPreferences.Editor editor = secureSharedPreferences.edit();
            if (value == null) {
                editor.remove(key);
            } else {
                editor.putString(key, String.valueOf(value));
            }
            editor.apply();
        } else if (sharedPreferences != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();

            if (value instanceof Boolean) {
                editor.putBoolean(key, (Boolean) value);
            } else if (value instanceof Integer) {
                editor.putInt(key, (Integer) value);
            } else if (value instanceof Long) {
                editor.putLong(key, (Long) value);
            } else if (value instanceof Float) {
                editor.putFloat(key, (Float) value);
            } else if (value instanceof String) {
                editor.putString(key, (String) value);
            } else {
                editor.putString(key, String.valueOf(value));
            }
            editor.apply();
        }
    }

    @Override
    public Map<String, Object> getCurrentSettings() {
        return new HashMap<>(currentSettings);
    }

    @Override
    public void resetToDefaults() {
        currentSettings.clear();
        currentSettings.putAll(getDefaults());

        // Clear storage
        if (sharedPreferences != null) {
            sharedPreferences.edit().clear().apply();
        }
        if (secureSharedPreferences != null) {
            secureSharedPreferences.edit().clear().apply();
        }

        Log.d(TAG, "Module " + moduleId + " reset to defaults");
    }

    @Override
    public void onImport(JSONObject data) {
        if (data == null) return;

        Map<String, Object> defaults = getDefaults();

        for (String key : data.keySet()) {
            try {
                Object value = data.get(key);

                // Skip sensitive keys during import (user must enter them manually)
                if (sensitiveKeys.contains(key)) {
                    Log.d(TAG, "Skipping sensitive key during import: " + key);
                    continue;
                }

                // Validate before setting
                ValidationResult result = validate(key, value);
                if (result.isValid()) {
                    Object finalValue = result.hasCorrection() ? result.getCorrectedValue() : value;
                    currentSettings.put(key, finalValue);
                    saveSettingToStorage(key, finalValue);
                } else {
                    Log.w(TAG, "Invalid value for " + key + " during import: " + result.getErrorMessage());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error importing setting: " + key, e);
            }
        }
    }

    @Override
    public JSONObject onExport(boolean includeSensitive) {
        JSONObject json = new JSONObject();

        for (Map.Entry<String, Object> entry : currentSettings.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Skip sensitive data unless explicitly requested
            if (!includeSensitive && sensitiveKeys.contains(key)) {
                continue;
            }

            try {
                json.put(key, value);
            } catch (Exception e) {
                Log.w(TAG, "Error exporting setting: " + key, e);
            }
        }

        return json;
    }

    @Override
    public void cleanup() {
        contextRef.clear();
        currentSettings.clear();
        sensitiveKeys.clear();
        Log.d(TAG, "Module " + moduleId + " cleanup completed");
    }

    /**
     * Get a setting value with type safety
     */
    @SuppressWarnings("unchecked")
    protected T getValue(String key, T defaultValue) {
        Object value = currentSettings.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    /**
     * Get integer setting with validation
     */
    protected int getInt(String key, int defaultValue) {
        Object value = currentSettings.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    /**
     * Get boolean setting with validation
     */
    protected boolean getBoolean(String key, boolean defaultValue) {
        Object value = currentSettings.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    /**
     * Get string setting with validation
     */
    protected String getString(String key, String defaultValue) {
        Object value = currentSettings.get(key);
        if (value != null) {
            return String.valueOf(value);
        }
        return defaultValue;
    }
}
