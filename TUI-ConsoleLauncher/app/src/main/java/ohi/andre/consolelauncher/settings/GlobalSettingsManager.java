package ohi.andre.consolelauncher.settings;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Central registry for all settings modules in the T-UI ConsoleLauncher.
 * This singleton manages the lifecycle of all settings modules, coordinates
 * save and load operations, and provides centralized access to configuration.
 * 
 * The GlobalSettingsManager ensures consistent behavior across all modules
 * while maintaining separation of concerns through the module-based design.
 */
public class GlobalSettingsManager {
    
    private static final String TAG = "GlobalSettingsManager";
    private static volatile GlobalSettingsManager instance;
    private final Context applicationContext;
    private final Map<String, ISettingsModule> modules;
    private final Map<Class<?>, ISettingsModule> modulesByClass;
    private final List<GlobalSettingsChangeListener> globalListeners;
    private final ExecutorService executorService;
    private boolean isInitialized;
    
    /**
     * Private constructor for singleton pattern.
     * 
     * @param context Application context
     */
    private GlobalSettingsManager(Context context) {
        this.applicationContext = context.getApplicationContext();
        this.modules = new ConcurrentHashMap<>();
        this.modulesByClass = new ConcurrentHashMap<>();
        this.globalListeners = new CopyOnWriteArrayList<>();
        this.executorService = Executors.newSingleThreadExecutor();
        this.isInitialized = false;
    }
    
    /**
     * Gets the singleton instance of GlobalSettingsManager.
     * Thread-safe with double-checked locking.
     * 
     * @param context Application context (only used for first initialization)
     * @return The singleton instance
     */
    public static GlobalSettingsManager getInstance(Context context) {
        if (instance == null) {
            synchronized (GlobalSettingsManager.class) {
                if (instance == null) {
                    instance = new GlobalSettingsManager(context);
                }
            }
        }
        return instance;
    }
    
    /**
     * Gets the singleton instance without requiring context.
     * This should only be called after the instance has been initialized.
     * 
     * @return The singleton instance
     * @throws IllegalStateException if the instance has not been initialized
     */
    public static GlobalSettingsManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("GlobalSettingsManager has not been initialized. Call getInstance(Context) first.");
        }
        return instance;
    }
    
    /**
     * Initializes all registered settings modules.
     * This method should be called during application startup.
     * 
     * @return true if initialization completed successfully
     */
    public boolean initialize() {
        if (isInitialized) {
            Log.w(TAG, "GlobalSettingsManager is already initialized");
            return true;
        }
        
        try {
            Log.i(TAG, "Initializing GlobalSettingsManager with " + modules.size() + " modules");
            
            // Load all registered modules
            for (ISettingsModule module : modules.values()) {
                try {
                    module.loadSettings();
                    Log.d(TAG, "Loaded module: " + module.getModuleId());
                } catch (Exception e) {
                    Log.e(TAG, "Failed to load module: " + module.getModuleId(), e);
                }
            }
            
            isInitialized = true;
            Log.i(TAG, "GlobalSettingsManager initialization complete");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize GlobalSettingsManager", e);
            return false;
        }
    }
    
    /**
     * Registers a settings module with the manager.
     * The module will be loaded immediately if the manager is already initialized.
     * 
     * @param module The settings module to register
     */
    public void registerModule(ISettingsModule module) {
        if (module == null) {
            Log.w(TAG, "Attempted to register null module");
            return;
        }
        
        String moduleId = module.getModuleId();
        
        if (modules.containsKey(moduleId)) {
            Log.w(TAG, "Module with ID " + moduleId + " is already registered. Replacing.");
            modules.remove(moduleId);
        }
        
        modules.put(moduleId, module);
        modulesByClass.put(module.getClass(), module);
        
        // Register this manager as a listener for the module
        module.registerChangeListener(moduleChangeListener);
        
        // Load if already initialized
        if (isInitialized) {
            try {
                module.loadSettings();
            } catch (Exception e) {
                Log.e(TAG, "Failed to load registered module: " + moduleId, e);
            }
        }
        
        Log.d(TAG, "Registered module: " + moduleId);
    }
    
    /**
     * Unregisters a settings module from the manager.
     * 
     * @param moduleId The ID of the module to unregister
     */
    public void unregisterModule(String moduleId) {
        ISettingsModule module = modules.remove(moduleId);
        if (module != null) {
            modulesByClass.remove(module.getClass());
            Log.d(TAG, "Unregistered module: " + moduleId);
        }
    }
    
    /**
     * Unregisters a settings module from the manager.
     * 
     * @param moduleClass The class of the module to unregister
     */
    public void unregisterModule(Class<? extends ISettingsModule> moduleClass) {
        ISettingsModule module = modulesByClass.remove(moduleClass);
        if (module != null) {
            modules.values().removeIf(m -> m.getClass().equals(moduleClass));
            Log.d(TAG, "Unregistered module class: " + moduleClass.getSimpleName());
        }
    }
    
    /**
     * Gets a registered settings module by its ID.
     * 
     * @param moduleId The module ID
     * @param <T> The expected module type
     * @return The settings module, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T extends ISettingsModule> T getModule(String moduleId) {
        return (T) modules.get(moduleId);
    }
    
    /**
     * Gets a registered settings module by its class.
     * 
     * @param moduleClass The module class
     * @param <T> The expected module type
     * @return The settings module, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T extends ISettingsModule> T getModule(Class<T> moduleClass) {
        return (T) modulesByClass.get(moduleClass);
    }
    
    /**
     * Checks if a module is registered.
     * 
     * @param moduleId The module ID to check
     * @return true if the module is registered
     */
    public boolean hasModule(String moduleId) {
        return modules.containsKey(moduleId);
    }
    
    /**
     * Checks if a module is registered.
     * 
     * @param moduleClass The module class to check
     * @return true if the module is registered
     */
    public boolean hasModule(Class<? extends ISettingsModule> moduleClass) {
        return modulesByClass.containsKey(moduleClass);
    }
    
    /**
     * Gets all registered module IDs.
     * 
     * @return List of module IDs
     */
    public List<String> getRegisteredModuleIds() {
        return new ArrayList<>(modules.keySet());
    }
    
    /**
     * Gets the count of registered modules.
     * 
     * @return Number of registered modules
     */
    public int getModuleCount() {
        return modules.size();
    }
    
    /**
     * Saves all registered settings modules.
     * This method iterates through all modules and calls saveSettings() on each.
     * 
     * @param async If true, saves are performed in the background
     * @return true if all saves completed successfully
     */
    public boolean saveAll(boolean async) {
        if (async) {
            executorService.execute(() -> saveAllSync());
            return true;
        } else {
            return saveAllSync();
        }
    }
    
    /**
     * Synchronous implementation of saveAll.
     * 
     * @return true if all saves completed successfully
     */
    private boolean saveAllSync() {
        boolean allSuccessful = true;
        
        for (ISettingsModule module : modules.values()) {
            try {
                if (module.hasUnsavedChanges()) {
                    module.saveSettings();
                    Log.d(TAG, "Saved module: " + module.getModuleId());
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to save module: " + module.getModuleId(), e);
                allSuccessful = false;
            }
        }
        
        return allSuccessful;
    }
    
    /**
     * Reloads all registered settings modules.
     * This discards any unsaved changes and reloads from persistent storage.
     * 
     * @param async If true, reloads are performed in the background
     */
    public void reloadAll(boolean async) {
        if (async) {
            executorService.execute(this::reloadAllSync);
        } else {
            reloadAllSync();
        }
    }
    
    /**
     * Synchronous implementation of reloadAll.
     */
    private void reloadAllSync() {
        for (ISettingsModule module : modules.values()) {
            try {
                module.loadSettings();
            } catch (Exception e) {
                Log.e(TAG, "Failed to reload module: " + module.getModuleId(), e);
            }
        }
    }
    
    /**
     * Resets all registered settings modules to their defaults.
     * 
     * @param async If true, resets are performed in the background
     */
    public void resetAll(boolean async) {
        if (async) {
            executorService.execute(this::resetAllSync);
        } else {
            resetAllSync();
        }
    }
    
    /**
     * Synchronous implementation of resetAll.
     */
    private void resetAllSync() {
        for (ISettingsModule module : modules.values()) {
            try {
                module.resetToDefaults();
            } catch (Exception e) {
                Log.e(TAG, "Failed to reset module: " + module.getModuleId(), e);
            }
        }
    }
    
    /**
     * Exports all settings as a JSON object.
     * This can be used for backup purposes.
     * 
     * @return JSON representation of all settings
     */
    public JSONObject exportAllSettings() {
        JSONObject export = new JSONObject();
        
        try {
            export.put("version", 1);
            export.put("timestamp", System.currentTimeMillis());
            
            JSONObject modulesExport = new JSONObject();
            
            for (ISettingsModule module : modules.values()) {
                try {
                    List<ISettingsModule.SettingEntry> entries = module.exportSettings();
                    JSONArray entriesArray = new JSONArray();
                    
                    for (ISettingsModule.SettingEntry entry : entries) {
                        JSONObject entryObj = new JSONObject();
                        entryObj.put("key", entry.getKey());
                        entryObj.put("type", entry.getType().name());
                        entryObj.put("value", entry.getStringValue());
                        entriesArray.put(entryObj);
                    }
                    
                    modulesExport.put(module.getModuleId(), entriesArray);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to export module: " + module.getModuleId(), e);
                }
            }
            
            export.put("modules", modulesExport);
            
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create export JSON", e);
        }
        
        return export;
    }
    
    /**
     * Imports settings from a JSON export.
     * 
     * @param export The JSON export to import from
     * @return true if import was successful
     */
    public boolean importSettings(JSONObject export) {
        try {
            if (!export.has("modules")) {
                Log.w(TAG, "Import file has no modules section");
                return false;
            }
            
            JSONObject modulesExport = export.getJSONObject("modules");
            boolean allSuccessful = true;
            
            for (String moduleId : modules.keySet()) {
                if (modulesExport.has(moduleId)) {
                    ISettingsModule module = modules.get(moduleId);
                    JSONArray entriesArray = modulesExport.getJSONArray(moduleId);
                    List<ISettingsModule.SettingEntry> entries = new ArrayList<>();
                    
                    for (int i = 0; i < entriesArray.length(); i++) {
                        JSONObject entryObj = entriesArray.getJSONObject(i);
                        String key = entryObj.getString("key");
                        String typeStr = entryObj.getString("type");
                        String value = entryObj.getString("value");
                        
                        ISettingsModule.SettingType type = 
                            ISettingsModule.SettingType.valueOf(typeStr);
                        entries.add(new ISettingsModule.SettingEntry(key, value, type));
                    }
                    
                    if (!module.importSettings(entries)) {
                        allSuccessful = false;
                    }
                }
            }
            
            return allSuccessful;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to import settings", e);
            return false;
        }
    }
    
    /**
     * Registers a global listener that receives notifications for all modules.
     * 
     * @param listener The listener to register
     */
    public void registerGlobalListener(GlobalSettingsChangeListener listener) {
        if (listener != null && !globalListeners.contains(listener)) {
            globalListeners.add(listener);
        }
    }
    
    /**
     * Unregisters a global listener.
     * 
     * @param listener The listener to unregister
     */
    public void unregisterGlobalListener(GlobalSettingsChangeListener listener) {
        globalListeners.remove(listener);
    }
    
    /**
     * Gets the application context.
     * 
     * @return Application context
     */
    public Context getApplicationContext() {
        return applicationContext;
    }
    
    /**
     * Checks if the manager is initialized.
     * 
     * @return true if initialized
     */
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * Clears all registered modules and resets the manager.
     * This is primarily useful for testing.
     */
    public void clear() {
        modules.clear();
        modulesByClass.clear();
        globalListeners.clear();
        isInitialized = false;
        Log.d(TAG, "GlobalSettingsManager cleared");
    }
    
    /**
     * Internal listener for module-level changes.
     */
    private final SettingsChangeListener moduleChangeListener = new SettingsChangeListener() {
        @Override
        public void onSettingsChanged(String moduleId, String key) {
            for (GlobalSettingsChangeListener listener : globalListeners) {
                listener.onGlobalSettingsChanged(moduleId, key);
            }
        }
        
        @Override
        public void onSettingsReset(String moduleId) {
            for (GlobalSettingsChangeListener listener : globalListeners) {
                listener.onGlobalSettingsReset(moduleId);
            }
        }
        
        @Override
        public void onSettingsLoaded(String moduleId) {
            for (GlobalSettingsChangeListener listener : globalListeners) {
                listener.onGlobalSettingsLoaded(moduleId);
            }
        }
        
        @Override
        public void onSettingsSaved(String moduleId) {
            for (GlobalSettingsChangeListener listener : globalListeners) {
                listener.onGlobalSettingsSaved(moduleId);
            }
        }
    };
    
    /**
     * Interface for receiving global notifications across all settings modules.
     */
    public interface GlobalSettingsChangeListener {
        void onGlobalSettingsChanged(String moduleId, String key);
        void onGlobalSettingsReset(String moduleId);
        void onGlobalSettingsLoaded(String moduleId);
        void onGlobalSettingsSaved(String moduleId);
    }
}
