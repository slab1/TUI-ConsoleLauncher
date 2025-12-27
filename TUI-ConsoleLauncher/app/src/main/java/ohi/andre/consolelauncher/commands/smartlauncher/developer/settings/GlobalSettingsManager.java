package ohi.andre.consolelauncher.commands.smartlauncher.developer.settings;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import ohi.andre.consolelauncher.commands.smartlauncher.developer.settings.base.ISettingsModule;
import ohi.andre.consolelauncher.commands.smartlauncher.developer.settings.modules.BuildSettings;
import ohi.andre.consolelauncher.commands.smartlauncher.developer.settings.modules.FileManagerSettings;
import ohi.andre.consolelauncher.commands.smartlauncher.developer.settings.modules.GitSettings;
import ohi.andre.consolelauncher.commands.smartlauncher.developer.settings.modules.TerminalSettings;
import ohi.andre.consolelauncher.commands.smartlauncher.developer.settings.modules.UiThemeSettings;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * GlobalSettingsManager - Central coordinator for all settings modules.
 * Provides unified access to all settings, handles cross-module dependencies,
 * and manages import/export functionality.
 */
public class GlobalSettingsManager {
    private static final String TAG = "GlobalSettingsManager";
    private static final String SETTINGS_VERSION = "1.0.0";

    // Singleton instance
    private static volatile GlobalSettingsManager instance;

    // Dependencies
    private WeakReference<Context> contextRef;
    private final ExecutorService executor;
    private final Handler mainHandler;

    // State
    private final AtomicBoolean initialized;
    private final ConcurrentHashMap<String, ISettingsModule> modules;
    private final Set<SettingsChangeListener> globalListeners;

    // Current settings version
    private String settingsVersion;

    /**
     * Settings change listener for global notifications
     */
    public interface SettingsChangeListener {
        void onGlobalSettingsChanged(String moduleId, String key, Object value);
        void onSettingsReset();
        void onSettingsImported(String version);
    }

    private GlobalSettingsManager() {
        this.initialized = new AtomicBoolean(false);
        this.modules = new ConcurrentHashMap<>();
        this.globalListeners = new HashSet<>();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static GlobalSettingsManager getInstance() {
        if (instance == null) {
            synchronized (GlobalSettingsManager.class) {
                if (instance == null) {
                    instance = new GlobalSettingsManager();
                }
            }
        }
        return instance;
    }

    // ======= Initialization =======

    /**
     * Initialize all settings modules
     */
    public void initialize(Context context) {
        if (context == null) {
            Log.e(TAG, "Cannot initialize with null context");
            return;
        }

        contextRef = new WeakReference<>(context.getApplicationContext());

        // Register all modules
        registerModules();

        // Initialize each module
        for (ISettingsModule module : modules.values()) {
            module.initialize(context);
        }

        // Load settings version
        loadSettingsVersion();

        initialized.set(true);
        Log.i(TAG, "GlobalSettingsManager initialized with " + modules.size() + " modules");
    }

    /**
     * Register all settings modules
     */
    private void registerModules() {
        // Core modules
        modules.put(EditorSettings.MODULE_ID, new EditorSettings());
        modules.put(GitSettings.MODULE_ID, new GitSettings());
        modules.put(FileManagerSettings.MODULE_ID, new FileManagerSettings());
        modules.put(TerminalSettings.MODULE_ID, new TerminalSettings());
        modules.put(BuildSettings.MODULE_ID, new BuildSettings());
        modules.put(UiThemeSettings.MODULE_ID, new UiThemeSettings());

        // Security modules
        modules.put("security", new SecuritySettings());
        modules.put("validator", new ValidatorSettings());
        modules.put("lsp", new LspSettings());
        modules.put("debugger", new DebugSettings());
    }

    private void loadSettingsVersion() {
        if (modules.containsKey("editor")) {
            EditorSettings editor = (EditorSettings) modules.get("editor");
            Map<String, Object> settings = editor.getCurrentSettings();
            Object version = settings.get("settings_version");
            settingsVersion = version != null ? String.valueOf(version) : SETTINGS_VERSION;
        } else {
            settingsVersion = SETTINGS_VERSION;
        }
    }

    public boolean isInitialized() {
        return initialized.get() && contextRef.get() != null;
    }

    // ======= Module Access =======

    /**
     * Get a specific module by ID
     */
    @SuppressWarnings("unchecked")
    public <T extends ISettingsModule> T getModule(String moduleId) {
        return (T) modules.get(moduleId);
    }

    /**
     * Get all registered module IDs
     */
    public Set<String> getRegisteredModules() {
        return new HashSet<>(modules.keySet());
    }

    /**
     * Get modules by category
     */
    public Map<String, Set<String>> getModulesByCategory() {
        Map<String, Set<String>> byCategory = new HashMap<>();

        for (ISettingsModule module : modules.values()) {
            String category = module.getModuleCategory();
            if (!byCategory.containsKey(category)) {
                byCategory.put(category, new HashSet<>());
            }
            byCategory.get(category).add(module.getModuleId());
        }

        return byCategory;
    }

    // ======= Settings Operations =======

    /**
     * Get all settings as JSON
     */
    public JSONObject exportAllSettings() {
        return exportAllSettings(false);
    }

    /**
     * Export all settings to JSON
     * @param includeSensitive Whether to include sensitive data (should be false for export)
     */
    public JSONObject exportAllSettings(boolean includeSensitive) {
        JSONObject export = new JSONObject();

        try {
            export.put("version", SETTINGS_VERSION);
            export.put("export_timestamp", System.currentTimeMillis());

            JSONObject moduleExports = new JSONObject();
            for (ISettingsModule module : modules.values()) {
                JSONObject moduleData = module.onExport(includeSensitive);
                moduleExports.put(module.getModuleId(), moduleData);
            }
            export.put("modules", moduleExports);

        } catch (Exception e) {
            Log.e(TAG, "Error exporting settings", e);
        }

        return export;
    }

    /**
     * Import settings from JSON
     */
    public void importSettings(JSONObject importData, SettingsImportCallback callback) {
        if (!initialized.get()) {
            if (callback != null) {
                mainHandler.post(() -> callback.onComplete(false, "Settings manager not initialized"));
            }
            return;
        }

        executor.execute(() -> {
            try {
                // Validate version
                String importVersion = importData.optString("version", "0.0.0");
                if (!isVersionCompatible(importVersion)) {
                    if (callback != null) {
                        final String error = "Incompatible settings version: " + importVersion;
                        mainHandler.post(() -> callback.onComplete(false, error));
                    }
                    return;
                }

                // Import each module
                JSONObject moduleImports = importData.getJSONObject("modules");
                for (String moduleId : moduleImports.keySet()) {
                    ISettingsModule module = modules.get(moduleId);
                    if (module != null) {
                        JSONObject moduleData = moduleImports.getJSONObject(moduleId);
                        module.onImport(moduleData);
                    }
                }

                // Notify global listeners
                notifyImport(importVersion);

                if (callback != null) {
                    mainHandler.post(() -> callback.onComplete(true, null));
                }

            } catch (Exception e) {
                Log.e(TAG, "Error importing settings", e);
                if (callback != null) {
                    mainHandler.post(() -> callback.onComplete(false, e.getMessage()));
                }
            }
        });
    }

    /**
     * Reset all modules to defaults
     */
    public void resetAllToDefaults() {
        for (ISettingsModule module : modules.values()) {
            module.resetToDefaults();
        }

        // Notify listeners
        mainHandler.post(() -> {
            for (SettingsChangeListener listener : globalListeners) {
                listener.onSettingsReset();
            }
        });

        Log.i(TAG, "All settings reset to defaults");
    }

    /**
     * Reset a specific module to defaults
     */
    public void resetModuleToDefaults(String moduleId) {
        ISettingsModule module = modules.get(moduleId);
        if (module != null) {
            module.resetToDefaults();
            notifyChange(moduleId, null, null);
        }
    }

    // ======= Settings Change Handling =======

    /**
     * Add a global settings change listener
     */
    public void addGlobalListener(SettingsChangeListener listener) {
        if (listener != null) {
            globalListeners.add(listener);
        }
    }

    /**
     * Remove a global settings change listener
     */
    public void removeGlobalListener(SettingsChangeListener listener) {
        globalListeners.remove(listener);
    }

    /**
     * Notify listeners of a settings change
     */
    private void notifyChange(String moduleId, String key, Object value) {
        mainHandler.post(() -> {
            for (SettingsChangeListener listener : globalListeners) {
                listener.onGlobalSettingsChanged(moduleId, key, value);
            }
        });
    }

    /**
     * Notify listeners of import
     */
    private void notifyImport(String version) {
        mainHandler.post(() -> {
            for (SettingsChangeListener listener : globalListeners) {
                listener.onSettingsImported(version);
            }
        });
    }

    /**
     * Called by modules when settings change
     */
    public void onModuleSettingChanged(ISettingsModule module, String key, Object value) {
        notifyChange(module.getModuleId(), key, value);
    }

    // ======= Cross-Module Coordination =======

    /**
     * Get dependent modules for a given module ID
     */
    public Set<String> getDependentModules(String moduleId) {
        Set<String> dependents = new HashSet<>();

        // Define cross-module dependencies
        switch (moduleId) {
            case "ui_theme":
                // All modules may need to update their appearance
                dependents.add("editor");
                dependents.add("terminal");
                dependents.add("file_manager");
                break;

            case "lsp":
                // LSP changes affect editor and debugger
                dependents.add("editor");
                dependents.add("debugger");
                break;

            case "debugger":
                // Debug changes may affect LSP
                dependents.add("lsp");
                break;

            case "terminal":
                // Terminal changes may affect build output display
                dependents.add("build");
                break;

            case "build":
                // Build changes may affect terminal output
                dependents.add("terminal");
                break;

            case "git":
                // Git changes may affect file manager display
                dependents.add("file_manager");
                break;
        }

        return dependents;
    }

    /**
     * Check if a setting change requires other modules to update
     */
    public boolean hasDependencies(String moduleId) {
        return !getDependentModules(moduleId).isEmpty();
    }

    // ======= Settings Version Management =======

    /**
     * Check if imported settings version is compatible
     */
    private boolean isVersionCompatible(String version) {
        // Simple version check - can be enhanced with proper semver comparison
        try {
            String[] currentParts = SETTINGS_VERSION.split("\\.");
            String[] importParts = version.split("\\.");

            int currentMajor = Integer.parseInt(currentParts[0]);
            int importMajor = Integer.parseInt(importParts[0]);

            // Major version must match
            return currentMajor == importMajor;
        } catch (Exception e) {
            return false;
        }
    }

    public String getSettingsVersion() {
        return settingsVersion;
    }

    // ======= Utility Methods =======

    /**
     * Get a setting value from any module
     */
    public Object getSetting(String moduleId, String key) {
        ISettingsModule module = modules.get(moduleId);
        if (module != null) {
            Map<String, Object> settings = module.getCurrentSettings();
            return settings.get(key);
        }
        return null;
    }

    /**
     * Check if a module is registered
     */
    public boolean hasModule(String moduleId) {
        return modules.containsKey(moduleId);
    }

    /**
     * Get the number of registered modules
     */
    public int getModuleCount() {
        return modules.size();
    }

    // ======= Cleanup =======

    /**
     * Cleanup all resources
     */
    public void cleanup() {
        for (ISettingsModule module : modules.values()) {
            module.cleanup();
        }
        modules.clear();
        globalListeners.clear();
        contextRef.clear();
        executor.shutdownNow();

        if (instance == this) {
            instance = null;
        }

        Log.i(TAG, "GlobalSettingsManager cleanup completed");
    }

    /**
     * Callback for import operations
     */
    public interface SettingsImportCallback {
        void onComplete(boolean success, String errorMessage);
    }

    /**
     * Security settings module placeholder
     */
    private static class SecuritySettings implements ISettingsModule {
        SecuritySettings() {}
        @Override public String getModuleId() { return "security"; }
        @Override public String getModuleName() { return "Security"; }
        @Override public String getModuleCategory() { return "System"; }
        @Override public Map<String, Object> getDefaults() { return new HashMap<>(); }
        @Override public Set<String> getSensitiveKeys() { return new HashSet<>(); }
        @Override public ValidationResult validate(String key, Object value) { return ValidationResult.success(); }
        @Override public void onImport(JSONObject data) {}
        @Override public JSONObject onExport(boolean includeSensitive) { return new JSONObject(); }
        @Override public void onSettingChanged(String key, Object value) {}
        @Override public void resetToDefaults() {}
        @Override public Map<String, Object> getCurrentSettings() { return new HashMap<>(); }
        @Override public boolean isInitialized() { return true; }
        @Override public void initialize(Context context) {}
        @Override public void cleanup() {}
        @Override public void addListener(String listenerId, SettingsModuleListener listener) {}
        @Override public void removeListener(String listenerId) {}
    }

    /**
     * Validator settings module placeholder
     */
    private static class ValidatorSettings implements ISettingsModule {
        ValidatorSettings() {}
        @Override public String getModuleId() { return "validator"; }
        @Override public String getModuleName() { return "Input Validator"; }
        @Override public String getModuleCategory() { return "Security"; }
        @Override public Map<String, Object> getDefaults() { return new HashMap<>(); }
        @Override public Set<String> getSensitiveKeys() { return new HashSet<>(); }
        @Override public ValidationResult validate(String key, Object value) { return ValidationResult.success(); }
        @Override public void onImport(JSONObject data) {}
        @Override public JSONObject onExport(boolean includeSensitive) { return new JSONObject(); }
        @Override public void onSettingChanged(String key, Object value) {}
        @Override public void resetToDefaults() {}
        @Override public Map<String, Object> getCurrentSettings() { return new HashMap<>(); }
        @Override public boolean isInitialized() { return true; }
        @Override public void initialize(Context context) {}
        @Override public void cleanup() {}
        @Override public void addListener(String listenerId, SettingsModuleListener listener) {}
        @Override public void removeListener(String listenerId) {}
    }

    /**
     * LSP settings module placeholder
     */
    private static class LspSettings implements ISettingsModule {
        LspSettings() {}
        @Override public String getModuleId() { return "lsp"; }
        @Override public String getModuleName() { return "Language Server"; }
        @Override public String getModuleCategory() { return "Development"; }
        @Override public Map<String, Object> getDefaults() { return new HashMap<>(); }
        @Override public Set<String> getSensitiveKeys() { return new HashSet<>(); }
        @Override public ValidationResult validate(String key, Object value) { return ValidationResult.success(); }
        @Override public void onImport(JSONObject data) {}
        @Override public JSONObject onExport(boolean includeSensitive) { return new JSONObject(); }
        @Override public void onSettingChanged(String key, Object value) {}
        @Override public void resetToDefaults() {}
        @Override public Map<String, Object> getCurrentSettings() { return new HashMap<>(); }
        @Override public boolean isInitialized() { return true; }
        @Override public void initialize(Context context) {}
        @Override public void cleanup() {}
        @Override public void addListener(String listenerId, SettingsModuleListener listener) {}
        @Override public void removeListener(String listenerId) {}
    }

    /**
     * Debug settings module placeholder
     */
    private static class DebugSettings implements ISettingsModule {
        DebugSettings() {}
        @Override public String getModuleId() { return "debugger"; }
        @Override public String getModuleName() { return "Debugger"; }
        @Override public String getModuleCategory() { return "Development"; }
        @Override public Map<String, Object> getDefaults() { return new HashMap<>(); }
        @Override public Set<String> getSensitiveKeys() { return new HashSet<>(); }
        @Override public ValidationResult validate(String key, Object value) { return ValidationResult.success(); }
        @Override public void onImport(JSONObject data) {}
        @Override public JSONObject onExport(boolean includeSensitive) { return new JSONObject(); }
        @Override public void onSettingChanged(String key, Object value) {}
        @Override public void resetToDefaults() {}
        @Override public Map<String, Object> getCurrentSettings() { return new HashMap<>(); }
        @Override public boolean isInitialized() { return true; }
        @Override public void initialize(Context context) {}
        @Override public void cleanup() {}
        @Override public void addListener(String listenerId, SettingsModuleListener listener) {}
        @Override public void removeListener(String listenerId) {}
    }
}
