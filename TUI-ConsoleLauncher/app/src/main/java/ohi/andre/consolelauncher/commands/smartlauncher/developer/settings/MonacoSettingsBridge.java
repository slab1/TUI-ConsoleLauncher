package ohi.andre.consolelauncher.commands.smartlauncher.developer.settings;

import android.util.Log;
import android.webkit.JavascriptInterface;

/**
 * MonacoSettingsBridge - JavaScript interface for settings management
 * Provides methods for the WebView to read and write editor settings
 */
public class MonacoSettingsBridge {
    private static final String TAG = "MonacoSettingsBridge";

    private final EditorSettingsManager settingsManager;
    private final SettingsChangeCallback callback;

    /**
     * Callback interface for JavaScript notifications
     */
    public interface SettingsChangeCallback {
        void onSettingsChanged(String settingsJson);
        void onError(String error);
        void onSettingsSaved(String key, boolean success);
    }

    public MonacoSettingsBridge(EditorSettingsManager settingsManager, SettingsChangeCallback callback) {
        this.settingsManager = settingsManager;
        this.callback = callback;
    }

    /**
     * Get all settings as JSON string
     * Called from JavaScript: AndroidEditorSettings.getSettings()
     */
    @JavascriptInterface
    public String getSettings() {
        try {
            if (settingsManager == null || !settingsManager.isInitialized()) {
                return "{}";
            }
            return settingsManager.getSettingsAsJson().toString();
        } catch (Exception e) {
            Log.e(TAG, "Error getting settings", e);
            return "{}";
        }
    }

    /**
     * Get Monaco Editor compatible options
     * Called from JavaScript: AndroidEditorSettings.getMonacoOptions()
     */
    @JavascriptInterface
    public String getMonacoOptions() {
        try {
            if (settingsManager == null || !settingsManager.isInitialized()) {
                return "{}";
            }
            return settingsManager.getMonacoOptions().toString();
        } catch (Exception e) {
            Log.e(TAG, "Error getting Monaco options", e);
            return "{}";
        }
    }

    /**
     * Save a setting value
     * Called from JavaScript: AndroidEditorSettings.saveSetting(key, value, type)
     */
    @JavascriptInterface
    public void saveSetting(String key, String value, String type) {
        Log.d(TAG, "saveSetting called: key=" + key + ", value=" + value + ", type=" + type);

        if (settingsManager == null || !settingsManager.isInitialized()) {
            if (callback != null) {
                callback.onError("Settings manager not initialized");
            }
            return;
        }

        try {
            Object parsedValue = parseValue(value, type);
            settingsManager.saveSetting(key, parsedValue, new EditorSettingsManager.SettingsCallback() {
                @Override
                public void onSettingsLoaded(EditorSettings settings) {
                    // Not used for single setting save
                }

                @Override
                public void onSettingsSaved(String savedKey, boolean success) {
                    if (callback != null) {
                        callback.onSettingsSaved(key, success);
                    }
                }

                @Override
                public void onError(String error) {
                    if (callback != null) {
                        callback.onError(error);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error saving setting", e);
            if (callback != null) {
                callback.onError("Error saving setting: " + e.getMessage());
            }
        }
    }

    /**
     * Save multiple settings at once
     * Called from JavaScript: AndroidEditorSettings.saveSettings(settingsJson)
     */
    @JavascriptInterface
    public void saveSettings(String settingsJson) {
        Log.d(TAG, "saveSettings called with: " + settingsJson);

        if (settingsManager == null || !settingsManager.isInitialized()) {
            if (callback != null) {
                callback.onError("Settings manager not initialized");
            }
            return;
        }

        try {
            org.json.JSONObject json = new org.json.JSONObject(settingsJson);
            java.util.Map<String, Object> settingsMap = new java.util.HashMap<>();

            for (String key : json.keySet()) {
                Object value = json.get(key);
                settingsMap.put(key, value);
            }

            settingsManager.saveSettings(settingsMap, new EditorSettingsManager.SettingsCallback() {
                @Override
                public void onSettingsLoaded(EditorSettings settings) {
                    if (callback != null) {
                        callback.onSettingsChanged(settings.toJson().toString());
                    }
                }

                @Override
                public void onSettingsSaved(String key, boolean success) {
                    // Not used for bulk save
                }

                @Override
                public void onError(String error) {
                    if (callback != null) {
                        callback.onError(error);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error saving settings", e);
            if (callback != null) {
                callback.onError("Error saving settings: " + e.getMessage());
            }
        }
    }

    /**
     * Get a specific setting value
     * Called from JavaScript: AndroidEditorSettings.getSetting(key)
     */
    @JavascriptInterface
    public String getSetting(String key) {
        try {
            if (settingsManager == null || !settingsManager.isInitialized()) {
                return "null";
            }
            Object value = settingsManager.getSetting(key);
            if (value == null) {
                return "null";
            }
            return String.valueOf(value);
        } catch (Exception e) {
            Log.e(TAG, "Error getting setting: " + key, e);
            return "null";
        }
    }

    /**
     * Reset all settings to defaults
     * Called from JavaScript: AndroidEditorSettings.resetToDefaults()
     */
    @JavascriptInterface
    public void resetToDefaults() {
        Log.d(TAG, "resetToDefaults called");

        if (settingsManager == null || !settingsManager.isInitialized()) {
            if (callback != null) {
                callback.onError("Settings manager not initialized");
            }
            return;
        }

        settingsManager.resetToDefaults(new EditorSettingsManager.SettingsCallback() {
            @Override
            public void onSettingsLoaded(EditorSettings settings) {
                if (callback != null) {
                    callback.onSettingsChanged(settings.toJson().toString());
                }
            }

            @Override
            public void onSettingsSaved(String key, boolean success) {
                // Not used for reset
            }

            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }

    /**
     * Export settings as JSON string
     * Called from JavaScript: AndroidEditorSettings.exportSettings()
     */
    @JavascriptInterface
    public String exportSettings() {
        try {
            if (settingsManager == null || !settingsManager.isInitialized()) {
                return "{}";
            }
            return settingsManager.exportSettings();
        } catch (Exception e) {
            Log.e(TAG, "Error exporting settings", e);
            return "{}";
        }
    }

    /**
     * Import settings from JSON string
     * Called from JavaScript: AndroidEditorSettings.importSettings(json, merge)
     */
    @JavascriptInterface
    public void importSettings(String json, boolean merge) {
        Log.d(TAG, "importSettings called: " + json.substring(0, Math.min(100, json.length())));

        if (settingsManager == null || !settingsManager.isInitialized()) {
            if (callback != null) {
                callback.onError("Settings manager not initialized");
            }
            return;
        }

        settingsManager.importSettings(json, merge, new EditorSettingsManager.SettingsCallback() {
            @Override
            public void onSettingsLoaded(EditorSettings settings) {
                if (callback != null) {
                    callback.onSettingsChanged(settings.toJson().toString());
                }
            }

            @Override
            public void onSettingsSaved(String key, boolean success) {
                // Not used for import
            }

            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }

    /**
     * Check if a setting exists
     * Called from JavaScript: AndroidEditorSettings.hasSetting(key)
     */
    @JavascriptInterface
    public boolean hasSetting(String key) {
        if (settingsManager == null || !settingsManager.isInitialized()) {
            return false;
        }
        return settingsManager.hasSetting(key);
    }

    /**
     * Remove a specific setting
     * Called from JavaScript: AndroidEditorSettings.removeSetting(key)
     */
    @JavascriptInterface
    public void removeSetting(String key) {
        Log.d(TAG, "removeSetting called: " + key);

        if (settingsManager == null || !settingsManager.isInitialized()) {
            if (callback != null) {
                callback.onError("Settings manager not initialized");
            }
            return;
        }

        settingsManager.removeSetting(key, new EditorSettingsManager.SettingsCallback() {
            @Override
            public void onSettingsLoaded(EditorSettings settings) {
                // Not used
            }

            @Override
            public void onSettingsSaved(String removedKey, boolean success) {
                if (callback != null) {
                    callback.onSettingsSaved(key, success);
                }
            }

            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }

    /**
     * Apply settings to Monaco Editor
     * Called from JavaScript to get formatted Monaco options
     */
    @JavascriptInterface
    public void applyMonacoSettings() {
        if (settingsManager == null || !settingsManager.isInitialized()) {
            return;
        }

        String options = settingsManager.getMonacoOptions().toString();
        Log.d(TAG, "Applying Monaco settings: " + options);

        // The actual application happens in JavaScript via callback
        if (callback != null) {
            callback.onSettingsChanged(options);
        }
    }

    /**
     * Parse value from string based on type
     */
    private Object parseValue(String value, String type) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        switch (type != null ? type.toLowerCase() : "string") {
            case "boolean":
            case "bool":
                return Boolean.parseBoolean(value);

            case "integer":
            case "int":
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    return 0;
                }

            case "long":
                try {
                    return Long.parseLong(value);
                } catch (NumberFormatException e) {
                    return 0L;
                }

            case "float":
            case "double":
                try {
                    return Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    return 0.0;
                }

            case "string":
            case "text":
            default:
                // Unescape common JSON escape sequences
                return value.replace("\\n", "\n")
                           .replace("\\t", "\t")
                           .replace("\\r", "\r")
                           .replace("\\\"", "\"")
                           .replace("\\\\", "\\");
        }
    }
}
