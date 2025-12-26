package ohi.andre.consolelauncher.managers;

import android.content.Context;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import ohi.andre.consolelauncher.BuildConfig;
import ohi.andre.consolelauncher.ai.AICommand;
import ohi.andre.consolelauncher.commands.Command;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.CommandsPreferences;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.smartlauncher.ai.AICommand as SmartAICommand;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Behavior;
import ohi.andre.consolelauncher.settings.SettingsInitializer;
import ohi.andre.consolelauncher.settings.modules.AISettingsModule;
import ohi.andre.consolelauncher.settings.modules.VoiceSettingsModule;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Manager for registering and accessing commands in T-UI.
 * This class provides centralized command registration and management.
 */
public class CommandManager {
    
    private static final String TAG = "CommandManager";
    private static volatile CommandManager instance;
    
    private final Context context;
    private final Map<String, CommandAbstraction> commands;
    private final Map<Class<?>, CommandAbstraction> commandClasses;
    private boolean isInitialized;
    
    private CommandManager(Context context) {
        this.context = context.getApplicationContext();
        this.commands = new HashMap<>();
        this.commandClasses = new HashMap<>();
        this.isInitialized = false;
    }
    
    /**
     * Gets the singleton instance of CommandManager.
     */
    public static CommandManager getInstance(Context context) {
        if (instance == null) {
            synchronized (CommandManager.class) {
                if (instance == null) {
                    instance = new CommandManager(context);
                }
            }
        }
        return instance;
    }
    
    /**
     * Initializes all commands including AI integration.
     * Should be called during application startup.
     * 
     * @return true if initialization was successful
     */
    public boolean initialize() {
        if (isInitialized) {
            Log.w(TAG, "CommandManager already initialized");
            return true;
        }
        
        try {
            Log.i(TAG, "Initializing CommandManager...");
            
            // First initialize settings
            SettingsInitializer.initialize(context);
            
            // Register built-in commands
            registerBuiltInCommands();
            
            // Register AI command
            registerAICommand();
            
            isInitialized = true;
            Log.i(TAG, "CommandManager initialized with " + commands.size() + " commands");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize CommandManager", e);
            return false;
        }
    }
    
    /**
     * Registers the built-in T-UI commands.
     */
    private void registerBuiltInCommands() {
        // These commands are registered through the existing T-UI system
        // This method is for documentation purposes
        Log.d(TAG, "Built-in commands are registered through the existing system");
    }
    
    /**
     * Registers the AI command.
     */
    private void registerAICommand() {
        try {
            // Create AI command instance
            AICommand aiCommand = new AICommand(context);
            
            // Register with command system
            // Note: The actual registration depends on T-UI's command system
            // This creates a reference that can be used for command execution
            
            commandClasses.put(AICommand.class, aiCommand);
            commands.put("ai", aiCommand);
            
            Log.d(TAG, "Registered AI command");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to register AI command", e);
        }
    }
    
    /**
     * Registers a command with the command system.
     * 
     * @param commandName The command name
     * @param command The command implementation
     */
    public void registerCommand(String commandName, CommandAbstraction command) {
        if (commandName == null || command == null) {
            Log.w(TAG, "Cannot register null command");
            return;
        }
        
        commands.put(commandName.toLowerCase(), command);
        commandClasses.put(command.getClass(), command);
        
        Log.d(TAG, "Registered command: " + commandName);
    }
    
    /**
     * Gets a command by name.
     * 
     * @param commandName The command name
     * @return The command implementation, or null if not found
     */
    public CommandAbstraction getCommand(String commandName) {
        if (commandName == null) {
            return null;
        }
        return commands.get(commandName.toLowerCase());
    }
    
    /**
     * Gets a command by class.
     * 
     * @param commandClass The command class
     * @return The command implementation, or null if not found
     */
    public CommandAbstraction getCommand(Class<? extends CommandAbstraction> commandClass) {
        return commandClasses.get(commandClass);
    }
    
    /**
     * Gets the AI command instance.
     * 
     * @return The AI command, or null if not registered
     */
    public AICommand getAICommand() {
        return (AICommand) commands.get("ai");
    }
    
    /**
     * Checks if a command is registered.
     * 
     * @param commandName The command name
     * @return true if the command is registered
     */
    public boolean hasCommand(String commandName) {
        return commandName != null && commands.containsKey(commandName.toLowerCase());
    }
    
    /**
     * Gets all registered command names.
     * 
     * @return Set of command names
     */
    public java.util.Set<String> getCommandNames() {
        return commands.keySet();
    }
    
    /**
     * Gets the number of registered commands.
     * 
     * @return Number of commands
     */
    public int getCommandCount() {
        return commands.size();
    }
    
    /**
     * Executes a command.
     * 
     * @param commandName The command name
     * @param args Command arguments
     * @param pack Main pack with execution context
     * @return Command output, or error message
     */
    public String executeCommand(String commandName, String[] args, MainPack pack) {
        CommandAbstraction command = getCommand(commandName);
        
        if (command == null) {
            return "Command not found: " + commandName;
        }
        
        try {
            return command.exec(pack);
        } catch (Exception e) {
            Log.e(TAG, "Command execution failed: " + commandName, e);
            return "Error executing " + commandName + ": " + e.getMessage();
        }
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
     * Gets the AI settings module.
     * 
     * @return AISettingsModule instance
     */
    public AISettingsModule getAISettings() {
        return SettingsInitializer.getSettingsManager(context).getModule(AISettingsModule.class);
    }
    
    /**
     * Gets the voice settings module.
     * 
     * @return VoiceSettingsModule instance
     */
    public VoiceSettingsModule getVoiceSettings() {
        return SettingsInitializer.getSettingsManager(context).getModule(VoiceSettingsModule.class);
    }
    
    /**
     * Resets the manager state.
     * This is primarily useful for testing.
     */
    public void reset() {
        commands.clear();
        commandClasses.clear();
        isInitialized = false;
        SettingsInitializer.reset();
        Log.d(TAG, "CommandManager reset");
    }
}
