package ohi.andre.consolelauncher.commands.smartlauncher.developer.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;

import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import ohi.andre.consolelauncher.commands.smartlauncher.developer.security.SecureStorageManager;

/**
 * EditorSettingsManager - Singleton manager for all editor settings
 * Provides thread-safe CRUD operations with support for both regular and encrypted storage
 */
public class EditorSettingsManager {
    private static final String TAG = "EditorSettingsManager";
    private static final String PREFS_NAME = "monaco_editor_settings";
    private static final String SECURE_PREFS_NAME = "monaco_editor_secure_settings";

    // Singleton instance
    private static volatile EditorSettingsManager instance;

    // Dependencies
    private WeakReference<Context> contextRef;
    private SharedPreferences sharedPreferences;
    private SharedPreferences secureSharedPreferences;
    private SecureStorageManager secureStorageManager;

    // Settings state
    private final AtomicReference<EditorSettings> currentSettings;
    private final AtomicBoolean initialized;
    private final ConcurrentHashMap<String, SettingsChangeListener> listeners;

    // Threading
    private final ExecutorService executor;
    private final Handler mainHandler;

    // Callbacks
    public interface SettingsChangeListener {
        void onSettingsChanged(EditorSettings settings, String key);
        void onSettingsReset();
    }

    public interface SettingsCallback {
        void onSettingsLoaded(EditorSettings settings);
        void onSettingsSaved(String key, boolean success);
        void onError(String error);
    }

    // ======= Singleton Pattern =======

    private EditorSettingsManager() {
        this.currentSettings = new AtomicReference<>(EditorSettings.getDefaults());
        this.initialized = new AtomicBoolean(false);
        this.listeners = new ConcurrentHashMap<>();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static EditorSettingsManager getInstance() {
        if (instance == null) {
            synchronized (EditorSettingsManager.class) {
                if (instance == null) {
                    instance = new EditorSettingsManager();
                }
            }
        }
        return instance;
    }

    // ======= Initialization =======

    /**
     * Initialize the settings manager with application context
     * Must be called once during app startup
     */
    public void initialize(Context context) {
        if (context == null) {
            Log.e(TAG, "Cannot initialize with null context");
            return;
        }

        contextRef = new WeakReference<>(context.getApplicationContext());

        try {
            // Initialize SharedPreferences
            sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

            // Initialize Encrypted SharedPreferences for sensitive data
            File securePrefsFile = new File(context.getFilesDir(), "shared_prefs/" + SECURE_PREFS_NAME + ".xml");
            if (securePrefsFile.getParentFile() != null) {
                securePrefsFile.getParentFile().mkdirs();
            }

            secureSharedPreferences = EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_NAME,
                securePrefsFile,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            // Initialize SecureStorageManager for additional encryption needs
            secureStorageManager = new SecureStorageManager();
            secureStorageManager.initialize(context);

            // Load settings from storage
            loadSettingsSync();

            initialized.set(true);
            Log.i(TAG, "EditorSettingsManager initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error initializing EditorSettingsManager", e);
        }
    }

    /**
     * Check if manager is initialized
     */
    public boolean isInitialized() {
        return initialized.get() && contextRef.get() != null;
    }

    // ======= Settings Loading =======

    /**
     * Load settings asynchronously
     */
    public void loadSettings(SettingsCallback callback) {
        executor.execute(() -> {
            try {
                EditorSettings settings = loadSettingsSync();
                if (callback != null) {
                    mainHandler.post(() -> callback.onSettingsLoaded(settings));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading settings", e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    /**
     * Load settings synchronously (must be called from background thread)
     */
    private EditorSettings loadSettingsSync() {
        if (sharedPreferences == null) {
            return EditorSettings.getDefaults();
        }

        Map<String, Object> settingsMap = new ConcurrentHashMap<>();

        // Load all keys from SharedPreferences
        Map<String, ?> allPrefs = sharedPreferences.getAll();
        for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
            settingsMap.put(entry.getKey(), entry.getValue());
        }

        // Create builder and apply loaded values
        EditorSettings.Builder builder = new EditorSettings.Builder();
        builder.applyMap(settingsMap);

        // Load sensitive data from secure storage
        if (secureSharedPreferences != null) {
            String lspToken = secureSharedPreferences.getString("lsp.token", null);
            if (lspToken != null) {
                settingsMap.put("lsp.token", lspToken);
            }
        }

        // Update current settings
        EditorSettings newSettings = builder.build();
        currentSettings.set(newSettings);

        return newSettings;
    }

    /**
     * Get current settings (synchronous, returns cached value)
     */
    public EditorSettings getSettings() {
        return currentSettings.get();
    }

    /**
     * Get settings as JSON for JavaScript consumption
     */
    public JSONObject getSettingsAsJson() {
        EditorSettings settings = currentSettings.get();
        if (settings == null) {
            return EditorSettings.getDefaults().toJson();
        }
        return settings.toJson();
    }

    /**
     * Get Monaco-compatible options JSON
     */
    public JSONObject getMonacoOptions() {
        EditorSettings settings = currentSettings.get();
        if (settings == null) {
            return EditorSettings.getDefaults().toMonacoOptions();
        }
        return settings.toMonacoOptions();
    }

    // ======= Settings Saving =======

    /**
     * Save a setting value
     */
    public void saveSetting(String key, Object value, SettingsCallback callback) {
        if (!initialized.get()) {
            if (callback != null) {
                mainHandler.post(() -> callback.onError("SettingsManager not initialized"));
            }
            return;
        }

        executor.execute(() -> {
            try {
                boolean success = saveSettingSync(key, value);

                // Notify listeners
                EditorSettings settings = currentSettings.get();
                if (success && settings != null) {
                    notifyListeners(settings, key);
                }

                if (callback != null) {
                    final boolean finalSuccess = success;
                    mainHandler.post(() -> {
                        if (finalSuccess) {
                            callback.onSettingsSaved(key, true);
                        } else {
                            callback.onError("Failed to save setting: " + key);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving setting: " + key, e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    /**
     * Save setting synchronously
     */
    private boolean saveSettingSync(String key, Object value) {
        if (sharedPreferences == null) return false;

        // Check if key is sensitive
        boolean isSensitive = EditorSettings.SENSITIVE_KEYS.contains(key);

        if (isSensitive && secureSharedPreferences != null) {
            // Save to encrypted storage
            try {
                if (value == null) {
                    secureSharedPreferences.edit().remove(key).apply();
                } else {
                    secureSharedPreferences.edit().putString(key, String.valueOf(value)).apply();
                }
                Log.d(TAG, "Saved sensitive setting: " + key);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error saving sensitive setting", e);
                return false;
            }
        } else {
            // Save to regular SharedPreferences
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
            Log.d(TAG, "Saved setting: " + key);

            // Update cached settings
            updateCachedSettings(key, value);

            return true;
        }
    }

    /**
     * Update cached settings object
     */
    private void updateCachedSettings(String key, Object value) {
        EditorSettings current = currentSettings.get();
        if (current == null) return;

        EditorSettings.Builder builder = new EditorSettings.Builder();

        // Copy current settings
        copySettingsToBuilder(current, builder);

        // Apply the change
        if (value instanceof Boolean) {
            applyBooleanSetting(builder, key, (Boolean) value);
        } else if (value instanceof Integer) {
            applyIntegerSetting(builder, key, (Integer) value);
        } else if (value instanceof String) {
            applyStringSetting(builder, key, (String) value);
        }

        currentSettings.set(builder.build());
    }

    private void copySettingsToBuilder(EditorSettings settings, EditorSettings.Builder builder) {
        builder.setFontSize(settings.getFontSize());
        builder.setTheme(settings.getTheme());
        builder.setWordWrap(settings.getWordWrap());
        builder.setMinimapEnabled(settings.isMinimapEnabled());
        builder.setTabSize(settings.getTabSize());
        builder.setInsertSpaces(settings.isInsertSpaces());
        builder.setLineNumbers(settings.getLineNumbers());
        builder.setRenderWhitespace(settings.isRenderWhitespace());
        builder.setAutoClosingBrackets(settings.isAutoClosingBrackets());
        builder.setCursorBlinking(settings.getCursorBlinking());
        builder.setFoldingEnabled(settings.isFoldingEnabled());
        builder.setAutoSave(settings.isAutoSave());
        builder.setAutoSaveDelay(settings.getAutoSaveDelay());
        builder.setFormatOnSave(settings.isFormatOnSave());
        builder.setLspEnabled(settings.isLspEnabled());
        builder.setLspServerPath(settings.getLspServerPath());
        builder.setDebugEnabled(settings.isDebugEnabled());
        builder.setSidebarVisible(settings.isSidebarVisible());
        builder.setDefaultEncoding(settings.getDefaultEncoding());
    }

    private void applyBooleanSetting(EditorSettings.Builder builder, String key, boolean value) {
        switch (key) {
            case "minimapEnabled": builder.setMinimapEnabled(value); break;
            case "autoSave": builder.setAutoSave(value); break;
            case "formatOnSave": builder.setFormatOnSave(value); break;
            case "lspEnabled": builder.setLspEnabled(value); break;
            case "debugEnabled": builder.setDebugEnabled(value); break;
            case "sidebarVisible": builder.setSidebarVisible(value); break;
            case "renderWhitespace": builder.setRenderWhitespace(value); break;
            case "autoClosingBrackets": builder.setAutoClosingBrackets(value); break;
            case "foldingEnabled": builder.setFoldingEnabled(value); break;
        }
    }

    private void applyIntegerSetting(EditorSettings.Builder builder, String key, int value) {
        switch (key) {
            case "fontSize": builder.setFontSize(value); break;
            case "tabSize": builder.setTabSize(value); break;
            case "autoSaveDelay": builder.setAutoSaveDelay(value); break;
        }
    }

    private void applyStringSetting(EditorSettings.Builder builder, String key, String value) {
        switch (key) {
            case "theme": builder.setTheme(value); break;
            case "wordWrap": builder.setWordWrap(value); break;
            case "lineNumbers": builder.setLineNumbers(value); break;
            case "cursorBlinking": builder.setCursorBlinking(value); break;
            case "lspServerPath": builder.setLspServerPath(value); break;
            case "sidebarWidth": builder.setSidebarWidth(value); break;
            case "defaultEncoding": builder.setDefaultEncoding(value); break;
        }
    }

    /**
     * Save multiple settings at once
     */
    public void saveSettings(Map<String, Object> settingsMap, SettingsCallback callback) {
        if (!initialized.get()) {
            if (callback != null) {
                mainHandler.post(() -> callback.onError("SettingsManager not initialized"));
            }
            return;
        }

        executor.execute(() -> {
            try {
                for (Map.Entry<String, Object> entry : settingsMap.entrySet()) {
                    saveSettingSync(entry.getKey(), entry.getValue());
                }

                // Reload settings to ensure consistency
                loadSettingsSync();

                if (callback != null) {
                    mainHandler.post(() -> callback.onSettingsLoaded(currentSettings.get()));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving settings", e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    // ======= Settings Reset =======

    /**
     * Reset all settings to defaults
     */
    public void resetToDefaults(SettingsCallback callback) {
        executor.execute(() -> {
            try {
                // Clear SharedPreferences
                if (sharedPreferences != null) {
                    sharedPreferences.edit().clear().apply();
                }

                // Clear secure preferences
                if (secureSharedPreferences != null) {
                    secureSharedPreferences.edit().clear().apply();
                }

                // Reset cached settings
                currentSettings.set(EditorSettings.getDefaults());

                // Notify listeners
                notifyListeners(EditorSettings.getDefaults(), null);

                Log.i(TAG, "Settings reset to defaults");

                if (callback != null) {
                    mainHandler.post(() -> callback.onSettingsLoaded(EditorSettings.getDefaults()));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error resetting settings", e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    // ======= Settings Export/Import =======

    /**
     * Export settings as JSON string
     */
    public String exportSettings() {
        try {
            JSONObject json = currentSettings.get().toJson();
            return json.toString(2);
        } catch (Exception e) {
            Log.e(TAG, "Error exporting settings", e);
            return "{}";
        }
    }

    /**
     * Import settings from JSON string
     */
    public void importSettings(String jsonString, boolean merge, SettingsCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject json = new JSONObject(jsonString);
                Map<String, Object> settingsMap = new ConcurrentHashMap<>();

                // Parse JSON to map
                for (String key : json.keySet()) {
                    Object value = json.get(key);
                    settingsMap.put(key, value);
                }

                if (merge) {
                    // Merge with existing settings
                    saveSettings(settingsMap, callback);
                } else {
                    // Replace all settings
                    EditorSettings.Builder builder = new EditorSettings.Builder();
                    builder.applyMap(settingsMap);
                    EditorSettings newSettings = builder.build();

                    // Save to storage
                    for (Map.Entry<String, Object> entry : settingsMap.entrySet()) {
                        saveSettingSync(entry.getKey(), entry.getValue());
                    }

                    currentSettings.set(newSettings);
                    notifyListeners(newSettings, null);

                    if (callback != null) {
                        mainHandler.post(() -> callback.onSettingsLoaded(newSettings));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error importing settings", e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    // ======= Listeners =======

    /**
     * Add a settings change listener
     */
    public void addListener(String id, SettingsChangeListener listener) {
        if (listener != null) {
            listeners.put(id, listener);
        }
    }

    /**
     * Remove a settings change listener
     */
    public void removeListener(String id) {
        listeners.remove(id);
    }

    /**
     * Notify all listeners of settings change
     */
    private void notifyListeners(EditorSettings settings, String key) {
        for (SettingsChangeListener listener : listeners.values()) {
            if (listener != null) {
                if (key == null) {
                    listener.onSettingsReset();
                } else {
                    listener.onSettingsChanged(settings, key);
                }
            }
        }
    }

    // ======= Utility Methods =======

    /**
     * Get a specific setting value
     */
    public Object getSetting(String key) {
        if (sharedPreferences == null) return null;

        // Check sensitive keys first
        if (EditorSettings.SENSITIVE_KEYS.contains(key) && secureSharedPreferences != null) {
            String value = secureSharedPreferences.getString(key, null);
            return value;
        }

        // Check SharedPreferences
        Object value = sharedPreferences.getAll().get(key);
        if (value != null) {
            return value;
        }

        // Return default from current settings
        EditorSettings settings = currentSettings.get();
        if (settings != null) {
            return getSettingFromSettings(settings, key);
        }

        return null;
    }

    private Object getSettingFromSettings(EditorSettings settings, String key) {
        switch (key) {
            case "fontSize": return settings.getFontSize();
            case "theme": return settings.getTheme();
            case "wordWrap": return settings.getWordWrap();
            case "minimapEnabled": return settings.isMinimapEnabled();
            case "autoSave": return settings.isAutoSave();
            case "lspEnabled": return settings.isLspEnabled();
            case "debugEnabled": return settings.isDebugEnabled();
            case "sidebarVisible": return settings.isSidebarVisible();
            default: return null;
        }
    }

    /**
     * Check if a setting exists
     */
    public boolean hasSetting(String key) {
        if (sharedPreferences == null) return false;
        return sharedPreferences.contains(key) ||
               (EditorSettings.SENSITIVE_KEYS.contains(key) &&
                secureSharedPreferences != null &&
                secureSharedPreferences.contains(key));
    }

    /**
     * Remove a specific setting
     */
    public void removeSetting(String key, SettingsCallback callback) {
        executor.execute(() -> {
            try {
                if (EditorSettings.SENSITIVE_KEYS.contains(key) && secureSharedPreferences != null) {
                    secureSharedPreferences.edit().remove(key).apply();
                } else if (sharedPreferences != null) {
                    sharedPreferences.edit().remove(key).apply();
                }

                if (callback != null) {
                    mainHandler.post(() -> callback.onSettingsSaved(key, true));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error removing setting", e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (executor != null) {
            executor.shutdownNow();
        }
        listeners.clear();
        contextRef.clear();
        Log.i(TAG, "EditorSettingsManager cleanup completed");
    }
}
