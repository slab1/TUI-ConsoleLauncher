package ohi.andre.consolelauncher.settings;

import android.content.Context;
import android.util.Log;

import ohi.andre.consolelauncher.settings.legacy.AliasSettingsAdapter;
import ohi.andre.consolelauncher.settings.legacy.ThemeSettingsAdapter;
import ohi.andre.consolelauncher.settings.modules.AISettingsModule;
import ohi.andre.consolelauncher.settings.modules.GeneralSettingsModule;
import ohi.andre.consolelauncher.settings.modules.TerminalSettingsModule;
import ohi.andre.consolelauncher.settings.modules.VoiceSettingsModule;

/**
 * Initializes the unified settings architecture for T-UI ConsoleLauncher.
 * This class registers all settings modules during application startup.
 */
public class SettingsInitializer {
    
    private static final String TAG = "SettingsInitializer";
    private static boolean isInitialized = false;
    
    /**
     * Initializes all settings modules.
     * Should be called from Application.onCreate() or MainActivity.onCreate().
     * 
     * @param context Application context
     * @return true if initialization was successful
     */
    public static boolean initialize(Context context) {
        if (isInitialized) {
            Log.w(TAG, "Settings already initialized");
            return true;
        }
        
        try {
            Log.i(TAG, "Initializing settings architecture...");
            
            // Initialize the GlobalSettingsManager
            GlobalSettingsManager manager = GlobalSettingsManager.getInstance(context);
            
            // Register settings modules
            registerModules(context, manager);
            
            // Initialize all modules
            boolean success = manager.initialize();
            
            if (success) {
                isInitialized = true;
                Log.i(TAG, "Settings architecture initialized with " + 
                      manager.getModuleCount() + " modules");
            } else {
                Log.e(TAG, "Failed to initialize settings modules");
            }
            
            return success;
            
        } catch (Exception e) {
            Log.e(TAG, "Settings initialization failed", e);
            return false;
        }
    }
    
    /**
     * Registers all settings modules with the manager.
     * 
     * @param context Application context
     * @param manager GlobalSettingsManager instance
     */
    private static void registerModules(Context context, GlobalSettingsManager manager) {
        Log.d(TAG, "Registering settings modules...");
        
        // Core modules (must be registered first)
        registerCoreModules(context, manager);
        
        // AI modules
        registerAIModules(context, manager);
        
        // Legacy adapter modules
        registerLegacyModules(context, manager);
        
        Log.d(TAG, "Registered " + manager.getModuleCount() + " modules");
    }
    
    /**
     * Registers core settings modules.
     */
    private static void registerCoreModules(Context context, GlobalSettingsManager manager) {
        // General settings module
        GeneralSettingsModule generalSettings = new GeneralSettingsModule(context);
        manager.registerModule(generalSettings);
        Log.d(TAG, "Registered GeneralSettingsModule");
        
        // Terminal settings module
        TerminalSettingsModule terminalSettings = new TerminalSettingsModule(context);
        manager.registerModule(terminalSettings);
        Log.d(TAG, "Registered TerminalSettingsModule");
        
        // Voice settings module
        VoiceSettingsModule voiceSettings = new VoiceSettingsModule(context);
        manager.registerModule(voiceSettings);
        Log.d(TAG, "Registered VoiceSettingsModule");
    }
    
    /**
     * Registers AI-related settings modules.
     */
    private static void registerAIModules(Context context, GlobalSettingsManager manager) {
        // AI settings module
        AISettingsModule aiSettings = new AISettingsModule(context);
        manager.registerModule(aiSettings);
        Log.d(TAG, "Registered AISettingsModule");
    }
    
    /**
     * Registers legacy adapter modules for backward compatibility.
     */
    private static void registerLegacyModules(Context context, GlobalSettingsManager manager) {
        // Theme settings adapter
        ThemeSettingsAdapter themeSettings = new ThemeSettingsAdapter(context);
        manager.registerModule(themeSettings);
        Log.d(TAG, "Registered ThemeSettingsAdapter");
        
        // Alias settings adapter
        AliasSettingsAdapter aliasSettings = new AliasSettingsAdapter(context);
        manager.registerModule(aliasSettings);
        Log.d(TAG, "Registered AliasSettingsAdapter");
    }
    
    /**
     * Checks if the settings architecture has been initialized.
     * 
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * Gets the GlobalSettingsManager instance.
     * This will initialize the settings if not already done.
     * 
     * @param context Application context
     * @return GlobalSettingsManager instance
     */
    public static GlobalSettingsManager getSettingsManager(Context context) {
        if (!isInitialized) {
            initialize(context);
        }
        return GlobalSettingsManager.getInstance(context);
    }
    
    /**
     * Resets the initialization state.
     * This is primarily useful for testing.
     */
    public static void reset() {
        isInitialized = false;
        Log.d(TAG, "Settings initialization state reset");
    }
}
