package ohi.andre.consolelauncher.commands;

import android.content.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * MultiCommandGroup - Extends CommandGroup to support multiple command packages
 * Enables T-UI to load commands from multiple packages simultaneously
 */
public class MultiCommandGroup extends CommandGroup {
    
    private final List<CommandGroup> commandGroups;
    private CommandAbstraction[] combinedCommands;
    private String[] combinedCommandNames;
    
    public MultiCommandGroup(Context context, String... packageNames) {
        super(context, packageNames.length > 0 ? packageNames[0] : "");
        this.commandGroups = new ArrayList<>();
        
        // Create CommandGroup for each package
        for (String packageName : packageNames) {
            if (packageName != null && !packageName.trim().isEmpty()) {
                CommandGroup group = new CommandGroup(context, packageName);
                commandGroups.add(group);
            }
        }
        
        // Combine all commands
        combineCommands();
    }
    
    /**
     * Combine commands from all CommandGroups into a single array
     */
    private void combineCommands() {
        List<CommandAbstraction> allCommands = new ArrayList<>();
        List<String> allNames = new ArrayList<>();
        
        for (CommandGroup group : commandGroups) {
            CommandAbstraction[] commands = group.getCommands();
            String[] names = group.getCommandNames();
            
            if (commands != null) {
                allCommands.addAll(Arrays.asList(commands));
            }
            if (names != null) {
                allNames.addAll(Arrays.asList(names));
            }
        }
        
        // Sort commands by priority (highest first)
        Collections.sort(allCommands, (o1, o2) -> o2.priority() - o1.priority());
        
        // Convert to arrays
        combinedCommands = allCommands.toArray(new CommandAbstraction[0]);
        combinedCommandNames = allNames.toArray(new String[0]);
    }
    
    @Override
    public CommandAbstraction getCommandByName(String name) {
        // Search through all command groups
        for (CommandGroup group : commandGroups) {
            CommandAbstraction cmd = group.getCommandByName(name);
            if (cmd != null) {
                return cmd;
            }
        }
        return null;
    }
    
    @Override
    public CommandAbstraction[] getCommands() {
        return combinedCommands;
    }
    
    @Override
    public String[] getCommandNames() {
        return combinedCommandNames;
    }
    
    /**
     * Get the underlying command groups
     */
    public List<CommandGroup> getCommandGroups() {
        return new ArrayList<>(commandGroups);
    }
    
    /**
     * Check if any command group contains the specified command
     */
    public boolean hasCommand(String commandName) {
        return getCommandByName(commandName) != null;
    }
    
    /**
     * Get command by name with priority ordering
     */
    public CommandAbstraction getCommandWithPriority(String name) {
        CommandAbstraction command = getCommandByName(name);
        if (command != null) {
            return command;
        }
        
        // Fallback to parent implementation
        return super.getCommandByName(name);
    }
}