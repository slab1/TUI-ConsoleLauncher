# T-UI Smart IDE Launcher

A comprehensive, intelligent Android launcher that combines terminal efficiency with IDE capabilities and AI-powered assistance. Transform your phone into a powerful development environment while maintaining the quick, keyboard-driven workflow that power users love.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Installation](#installation)
- [Command Reference](#command-reference)
- [Configuration](#configuration)
- [Development](#development)
- [AI Integration](#ai-integration)
- [Automation](#automation)
- [Troubleshooting](#troubleshooting)

## Overview

T-UI Smart IDE Launcher reimagines what a mobile launcher can be. Instead of just launching apps with icons, this launcher provides a complete development environment accessible directly from your home screen. Built as an extension of the popular T-UI Launcher, it maintains the terminal-first philosophy while adding powerful new capabilities for developers, DevOps engineers, and power users.

The launcher operates through a command-based interface where everything from launching applications to managing files, running code, and controlling your phone can be accomplished through text commands. This approach is particularly powerful on mobile devices where touch input can be imprecise and menus can be deeply nested. With T-UI Smart IDE Launcher, you type what you want to do, and the launcher makes it happen.

The intelligence layer powered by MiniMax AI takes this further by understanding natural language queries. Instead of memorizing exact command syntax, you can ask questions in plain English and receive intelligent responses formatted specifically for the terminal environment. The AI can help with code explanations, debugging assistance, general knowledge questions, and much more.

## Features

### Core Launcher Features

The foundation of T-UI Smart IDE Launcher provides all the functionality you'd expect from a modern launcher while maintaining the terminal-based interface that distinguishes it from other launchers. The app launching system uses fuzzy matching to find applications even when you misspell names or remember only parts of an app's name. Command history remembers your previous commands and allows quick retrieval through keyboard shortcuts. The alias system lets you create custom shortcuts for frequently used commands, dramatically reducing typing for complex operations.

The command processor handles input parsing, argument validation, and output formatting. It supports quoted arguments for multi-word parameters, flag-based options, and context-aware suggestions as you type. The system maintains a persistent session state including current directory, command history, and user preferences across app restarts.

### Developer Tools

For developers, the launcher includes a comprehensive set of tools that turn your phone into a capable development environment. The file manager provides familiar commands like ls, cd, cat, mkdir, rm, cp, and mv, working within Android's storage system. You can navigate your phone's file system, view and edit files, and manage your project directories directly from the terminal.

Git integration allows common version control operations without leaving the launcher. Clone repositories with git clone, check status with git status, stage and commit changes, push and pull from remotes, and review commit history. The git commands integrate with the file manager, so you can work with your repositories in the same environment as your other file operations.

The system monitor provides real-time visibility into device resources. The system command displays CPU usage, memory consumption, battery status, storage availability, and network connectivity. This is particularly useful for developers testing resource-intensive applications or monitoring device health during development sessions.

### Network Tools

Network diagnostic tools help you understand and troubleshoot connectivity issues. Ping hosts to verify connectivity, perform DNS lookups to resolve domain names, check specific ports for accessibility, and scan port ranges to discover services. The network localip command shows your device's local IP addresses across all network interfaces, while network publicip retrieves your external IP address through an API call.

A simple speed test measures download speed by retrieving a test file and calculating throughput. The network status command provides a quick overview of your current network connection, showing connection type, connectivity status, and available transports.

### Productivity Tools

Beyond development, the launcher includes general productivity tools that are useful in daily workflows. The calculator supports arithmetic expressions, scientific functions, and unit conversions. You can calculate expressions directly, convert between units like kilograms and pounds or Celsius and Fahrenheit, and perform trigonometric calculations.

Quick notes allow rapid capture and retrieval of text snippets, code fragments, and ideas. Create notes with titles and content, search through notes by keyword, tag notes for organization, and export notes when needed. Notes persist across sessions and sync with your workflow.

### Automation Integration

The automation system connects T-UI Smart IDE Launcher with external automation tools. Tasker integration allows you to execute Tasker tasks directly from the terminal, triggering complex automation workflows with simple commands. Termux integration provides access to the full Linux environment that Termux provides, letting you run shell commands and scripts.

You can send custom broadcasts and intents from the terminal, useful for triggering other apps or system behaviors. Quick settings commands provide shortcuts to common system settings, opening the appropriate settings panels with single commands.

### AI Intelligence

The MiniMax AI integration provides intelligent assistance throughout the launcher. Prefix any query with ?? to send it to the AI for processing. The AI understands context from your command history and can provide contextual assistance based on what you're working on.

Code-related queries receive specialized handling with code block formatting and syntax-appropriate responses. Debugging assistance helps you understand error messages and identify solutions. The AI learns from your usage patterns over time, improving suggestions and responses based on how you use the launcher.

## Architecture

The launcher uses a layered architecture that separates concerns and enables modular development. Understanding this architecture helps when extending the launcher with new commands or modifying existing behavior.

### Presentation Layer

The presentation layer handles all user interface concerns. At its core is the command surface, which extends T-UI's existing RecyclerView-based terminal display with enhanced capabilities for colored output, code formatting, and embedded elements. This surface maintains scrollable history while keeping the input line accessible.

The overlay system provides slide-up panels for specialized interactions without disrupting the command flow. When you need to edit a file or view detailed information, overlays present this content while maintaining your command context. The overlay system supports the file editor, help viewers, and any future specialized interfaces.

### Logic Layer

The command processor is the central nervous system of the launcher. It receives user input, parses it into commands and arguments, validates the structure, and routes requests to appropriate handlers. The processor implements a flexible parsing system that handles quoted arguments, flags, and nested command structures.

The alias manager maintains user-defined shortcuts for commands. Aliases can reference other aliases, support parameter substitution with $1, $2 notation, and provide a powerful way to customize your workflow. The alias system includes a library of default aliases for common operations while allowing complete user customization.

Command history management persists your command history across sessions, enabling quick retrieval and search. The history system tracks not just the commands but also their execution status, allowing you to identify successful and failed operations. History search finds matching commands, and statistics show your most frequently used commands.

### Service Layer

Background services provide capabilities that persist across command sessions. The notification listener intercepts system notifications and can route them to the command stream based on configurable rules. This enables notification monitoring and filtering through command-based queries.

### Data Layer

The data layer manages persistent storage using Android's SharedPreferences for configuration and small data, with file storage for larger assets. The data layer implements caching strategies to balance performance with storage efficiency, and provides clean interfaces for data access that the upper layers consume.

### Module Organization

Commands are organized into modules based on their function. The core module contains the command processor, alias manager, and command history. The developer module contains file management and git commands. The productivity module contains the calculator, system monitor, network tools, and notes. The automation module handles Tasker and Termux integration. The AI module manages MiniMax integration.

Each command implements the CommandHandler interface, which defines the contract for command registration, execution, and cleanup. This interface-based design makes it easy to add new commands by implementing the interface and registering with the command processor.

## Installation

### Prerequisites

Before installing T-UI Smart IDE Launcher, ensure you have the following:

- Android device running Android 8.0 (Oreo) or higher
- At least 100MB of free storage space
- Internet connection for initial setup and AI features
- Android Studio if you plan to modify the source code

### Obtaining the Launcher

You can obtain T-UI Smart IDE Launcher through one of these methods:

**From Source (Recommended for Development):**

Clone the repository and build the application:

```bash
git clone https://github.com/yourusername/tui-smart-launcher.git
cd tui-smart-launcher
./gradlew build
```

The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

**Pre-built APK:**

Download the latest pre-built APK from the releases page on GitHub. Enable "Install from unknown sources" in your device settings, then tap the APK to install.

### Initial Setup

After installation, the launcher will guide you through initial setup:

1. **Set as Home Launcher:** When prompted, select T-UI Smart IDE Launcher as your default home launcher. This allows it to appear when you press the home button.

2. **Grant Permissions:** The launcher needs certain permissions for full functionality. Storage permission enables file management features. Notification access enables notification monitoring. These permissions are optional and the launcher will function with reduced capabilities if denied.

3. **Configure AI:** To enable AI features, you'll need a MiniMax API key. The configuration section explains how to obtain and configure your API key.

4. **Configure Storage Access:** For comprehensive file management, grant storage access permission. This enables access to external storage and improves file operations throughout the system.

### Verifying Installation

Run a few commands to verify your installation is working correctly:

```bash
# Check system status
system

# List installed apps
launch -l

# Test AI (if configured)
?? hello

# View help
help
```

## Command Reference

### Core Commands

**launch [appname]**
Launches an application by name with fuzzy matching. If multiple apps match, shows a selection list.

```bash
# Launch exact app
launch Spotify

# Fuzzy search
launch chrome

# List all apps
launch -l

# Search for app
launch -s telegram

# Open URL
launch -u https://example.com
```

**help**
Displays help information for all commands or specific command details.

```bash
# Show all commands
help

# Show specific command help
help launch
help file
```

**clear**
Clears the terminal screen.

```bash
clear
```

### Developer Commands

**file [operation] [arguments]**
Comprehensive file management commands.

```bash
# Navigation
file ls                    # List directory
file cd <dir>             # Change directory
file pwd                  # Show current directory

# File operations
file cat <filename>        # Display file contents
file mkdir <name>         # Create directory
file touch <name>         # Create empty file
file rm <path>            # Delete file
file cp <src> <dst>       # Copy file
file mv <src> <dst>       # Move/rename file

# Information
file wc <filename>        # Word count
file find <pattern>       # Search files
file info <path>          # File information
file tree                 # Directory tree
```

**git [operation] [arguments]**
Git version control operations.

```bash
# Repository management
git clone <url> [dir]     # Clone repository
git init                  # Initialize repository

# Basic operations
git status                # Show status
git add <file>            # Stage file
git add .                 # Stage all
git commit -m "<msg>"     # Commit changes
git push                  # Push to remote
git pull                  # Pull from remote

# Information
git log                   # Show commit log
git diff                  # Show changes
git branch                # List branches
git checkout <branch>     # Switch branch
git remote                # Show remotes
git open                  # Open in browser
```

### Productivity Commands

**calc [expression]**
Calculator with expression evaluation.

```bash
# Basic math
calc 15 * 24 + 100

# Functions
calc sqrt(144)
calc pow(2, 8)
calc sin(45)
calc log(100)
calc fact(5)

# Conversions
calc 100 C to F
calc 10 kg to lbs
calc 5 mi to km
```

**system [options]**
System resource monitoring.

```bash
# All information
system
system --all

# Specific information
system --cpu              # CPU info
system --memory           # Memory usage
system --battery          # Battery status
system --storage          # Storage info
system --process          # Running processes
system --network          # Network status
```

**note [operation] [arguments]**
Quick notes management.

```bash
# Create note
note create "Meeting Notes" Discussion points here

# List notes
note list

# View note
note show <id or title>

# Edit note
note edit <id> <new content>

# Search notes
note search <query>

# Delete note
note delete <id>

# Add tag
note tag <id> <tag>
```

**network [operation] [arguments]**
Network diagnostic tools.

```bash
# Connectivity
network ping <host>       # Ping host
network dns <domain>      # DNS lookup
network status            # Connection status

# Port operations
network port <host> <port>        # Check port
network scan <host> <ports>       # Scan ports

# Information
network localip           # Local IP addresses
network publicip          # Public IP address
network curl <url>        # HTTP request
network speed             # Speed test
network whois <domain>    # WHOIS lookup
```

### Automation Commands

**auto [operation] [arguments]**
Automation with Tasker and Termux.

```bash
# Tasker integration
auto tasker               # List Tasker tasks
auto task <name>          # Run Tasker task

# Termux integration
auto termux <command>     # Run Termux command

# Script execution
auto script <name>        # Run saved script

# System operations
auto apps                 # List automation apps
auto broadcast <action>   # Send broadcast
auto intent <action>      # Send intent
auto settings wifi on     # Open settings
```

### AI Commands

**?? [query]**
Send query to MiniMax AI.

```bash
# General questions
?? what is quantum computing

# Code assistance
?? write a Python function to sort a list
?? explain this code: [paste code]

# Debugging help
?? debug this error: [paste error]

# Creative writing
?? write a haiku about coding
```

## Configuration

### API Key Setup

For AI features, you need a MiniMax API key:

1. Sign up at the MiniMax developer portal
2. Create a new API key in your dashboard
3. Configure the key in the launcher:

```bash
# Set API key
ai --config set <your-api-key>

# Verify configuration
ai --config show
```

### Storage Permissions

For comprehensive file management, grant storage access:

```bash
# Request storage permission
file --request-storage
```

The launcher will prompt you to grant the MANAGE_EXTERNAL_STORAGE permission, which enables full file system access.

### Customization

**Aliases:** Create custom command shortcuts:

```bash
alias add gp git push
alias add ll file ls -la
alias add .. file cd ..
```

**Command History:** View and manage command history:

```bash
history          # Show command history
history clear    # Clear history
history search   # Search history
```

**Theme:** Configure terminal appearance:

```bash
theme set dark   # Dark theme
theme set light  # Light theme
theme set custom # Custom colors
```

## Development

### Adding New Commands

To add a new command to the launcher:

1. Create a new Kotlin file in the appropriate module directory
2. Implement the CommandHandler interface:

```kotlin
class MyCommand : CommandHandler {
    override fun getName(): String = "mycommand"
    
    override fun getAliases(): List<String> = listOf("mc", "mycmd")
    
    override fun getDescription(): String = "Description of my command"
    
    override fun getUsage(): String = "Usage information"
    
    override fun execute(context: Context, args: List<String>): String {
        // Command implementation
        return "Result output"
    }
}
```

3. Register the command in the main launcher activity:

```kotlin
commandProcessor.registerCommand("mycommand", MyCommand())
```

### Project Structure

```
tui-smart-launcher/
├── app/src/main/java/tui/smartlauncher/
│   ├── core/                    # Core command system
│   │   ├── CommandProcessor.kt
│   │   ├── CommandHandler.kt
│   │   ├── AliasManager.kt
│   │   ├── CommandHistory.kt
│   │   └── AppLauncherCommand.kt
│   ├── ai/                      # AI integration
│   │   ├── MiniMaxService.kt
│   │   └── AIConfig.kt
│   ├── developer/               # Developer tools
│   │   ├── FileManagerCommand.kt
│   │   └── GitCommand.kt
│   ├── productivity/            # Productivity tools
│   │   ├── CalculatorCommand.kt
│   │   ├── SystemCommand.kt
│   │   ├── NotesCommand.kt
│   │   └── NetworkCommand.kt
│   ├── automation/              # Automation integration
│   │   └── AutomationCommand.kt
│   └── MainActivity.kt          # Main launcher activity
├── res/                         # Resources
└── build.gradle                 # Build configuration
```

### Building from Source

```bash
# Clone the repository
git clone https://github.com/yourusername/tui-smart-launcher.git

# Open in Android Studio
# Or build from command line:
./gradlew assembleDebug

# Run tests
./gradlew test

# Generate APK
./gradlew build
```

## AI Integration

### MiniMax API

The launcher uses MiniMax's language models for AI capabilities. The integration supports streaming responses for real-time output, context injection from command history, and specialized handling for different query types.

### Query Types

The AI system automatically detects query types for specialized processing:

- **Code queries:** Code generation, explanation, and completion
- **Debug queries:** Error analysis and debugging assistance
- **General queries:** General knowledge and assistance
- **Creative queries:** Creative writing and brainstorming

### Context Engine

The context engine maintains awareness of your current session and injects relevant context into AI requests. This includes recently executed commands, current directory, clipboard content, and system state. When you ask a question related to recent work, the AI can reference this context for more accurate, helpful responses.

### Prompt Engineering

The system uses carefully crafted prompts optimized for terminal display:

- No markdown formatting in output
- Maximum 80 characters per line
- Clean ASCII formatting for structure
- Clear section separation

## Automation

### Tasker Integration

To use Tasker integration:

1. Install Tasker from the Play Store
2. Enable "Allow External Access" in Tasker preferences
3. Create named tasks in Tasker
4. Execute tasks from T-UI:

```bash
auto task Morning Routine
```

### Termux Integration

For Termux integration:

1. Install Termux from F-Droid (recommended) or Play Store
2. Install Termux-Tasker plugin for full integration
3. Execute Termux commands:

```bash
auto termux pkg update
auto termux python script.py
```

### Custom Automations

Create custom automation workflows by combining commands:

```bash
# Example: Automated backup script
alias backup="file cp /sdcard/Documents/*.md /storage/backups/"

# Example: Morning setup
alias morning="auto task Morning Routine && launch -u https://news.ycombinator.com"
```

## Troubleshooting

### Common Issues

**Command not found:**
- Verify the command is installed and registered
- Check spelling and syntax
- Use `help` to see available commands

**AI not responding:**
- Check API key configuration
- Verify internet connectivity
- Check MiniMax service status

**File operations failing:**
- Grant storage permissions
- Check file paths are correct
- Verify file exists

**Git commands failing:**
- Verify git is installed on device
- Check you're in a git repository
- Verify network connectivity for remote operations

### Getting Help

If you encounter issues not covered here:

```bash
# Check command help
help <command>

# View system information
system --all

# Check network status
network status
```

### Reporting Issues

When reporting issues, include:

- Device model and Android version
- Steps to reproduce the issue
- Command that triggered the issue
- Error messages received
- Any relevant logs (accessible through developer options)

## License

This project is licensed under the MIT License. See the LICENSE file for details.

## Credits

- T-UI Launcher for the original project
- MiniMax for AI capabilities
- The Android development community
- All contributors and users

---

For the latest updates, documentation, and community resources, visit the project repository.
