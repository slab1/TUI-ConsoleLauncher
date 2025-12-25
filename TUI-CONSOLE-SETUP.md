# T-UI Smart Launcher - Complete Setup Guide

## Overview

T-UI Smart Launcher extends T-UI ConsoleLauncher with intelligent AI-powered commands, developer tools, and productivity features. This guide shows you how to clone the correct T-UI repository and integrate our smart launcher modules.

## Repository Information

- **T-UI ConsoleLauncher**: https://github.com/slab1/TUI-ConsoleLauncher.git
- **Original Creator**: @slab1
- **Type**: Terminal-based Android launcher
- **Features**: Command-line interface, terminal emulation, app launching

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Clone T-UI ConsoleLauncher](#clone-t-ui-consolelauncher)
3. [Project Structure](#project-structure)
4. [Integrate Smart Modules](#integrate-smart-modules)
5. [Configure Permissions](#configure-permissions)
6. [Build and Run](#build-and-run)
7. [Testing](#testing)
8. [Available Commands](#available-commands)

---

## Prerequisites

Before you begin, ensure you have:

| Software | Version | Purpose |
|----------|---------|---------|
| **Java JDK** | 17+ | Android development |
| **Android Studio** | Hedgehog (2023.1.1) | IDE and build tools |
| **Git** | 2.0+ | Version control |
| **Gradle** | 8.0+ | Build automation |
| **Android SDK** | 34 | Target platform |

### Verify Your Setup

```bash
# Check Java
java -version

# Check Git
git --version

# Check Android SDK
echo $ANDROID_HOME
```

---

## Clone T-UI ConsoleLauncher

### Step 1: Open Terminal

```bash
# Navigate to your projects folder
cd ~/Projects  # or your preferred directory
```

### Step 2: Clone T-UI ConsoleLauncher

```bash
# Clone the correct T-UI ConsoleLauncher repository
git clone https://github.com/slab1/TUI-ConsoleLauncher.git tui-console

# Navigate into the directory
cd tui-console

# Verify the clone
ls -la
# You should see: app/, build.gradle, settings.gradle, etc.
```

### Step 3: Explore the Original Project

```bash
# Check the project structure
find . -name "*.kt" -type f | head -10

# Look at the main activity
cat app/src/main/java/MainActivity.kt
```

---

## Project Structure

After cloning, your project structure:

```
tui-console/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── MainActivity.kt      # ← Original T-UI main activity
│   │       └── res/                     # ← Resources
│   └── build.gradle                     # ← Build configuration
├── build.gradle                         # ← Root build file
├── settings.gradle
└── README.md                            # ← Original project README
```

### Adding Smart Modules

```
tui-console/
├── app/src/main/java/
│   ├── MainActivity.kt                  # ← Original T-UI
│   └── tui/
│       └── smartlauncher/               # ← Our smart modules
│           ├── core/
│           │   ├── CommandProcessor.kt
│           │   ├── CommandHandler.kt
│           │   ├── AliasManager.kt
│           │   └── AppLauncherCommand.kt
│           ├── ai/
│           │   └── MiniMaxService.kt
│           ├── developer/
│           │   ├── FileManagerCommand.kt
│           │   └── GitCommand.kt
│           ├── productivity/
│           │   ├── CalculatorCommand.kt
│           │   ├── SystemCommand.kt
│           │   ├── NotesCommand.kt
│           │   └── NetworkCommand.kt
│           └── automation/
│               └── AutomationCommand.kt
```

---

## Integrate Smart Modules

### Step 1: Copy Smart Launcher Files

If you have our smart launcher package:

```bash
# Copy all smart modules to T-UI ConsoleLauncher
cp -r /path/to/smart-launcher/tui/smartlauncher/* app/src/main/java/tui/
```

### Step 2: Update MainActivity

Edit `app/src/main/java/MainActivity.kt` and add smart command registration:

```kotlin
package com.slab1.consolelauncher

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import tui.smartlauncher.core.CommandProcessor
import tui.smartlauncher.developer.FileManagerCommand
import tui.smartlauncher.developer.GitCommand
import tui.smartlauncher.productivity.CalculatorCommand
import tui.smartlauncher.productivity.SystemCommand
import tui.smartlauncher.productivity.NotesCommand
import tui.smartlauncher.productivity.NetworkCommand
import tui.smartlauncher.automation.AutomationCommand

class MainActivity : AppCompatActivity() {

    private lateinit var commandProcessor: CommandProcessor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Original T-UI initialization...
        
        // Add smart launcher integration
        initializeSmartLauncher()
    }

    private fun initializeSmartLauncher() {
        commandProcessor = CommandProcessor(this)
        
        // Register smart commands alongside original T-UI commands
        registerSmartCommands()
    }

    private fun registerSmartCommands() {
        // Developer Commands
        commandProcessor.registerCommand("file", FileManagerCommand())
        commandProcessor.registerCommand("git", GitCommand())
        
        // Productivity Commands
        commandProcessor.registerCommand("calc", CalculatorCommand())
        commandProcessor.registerCommand("system", SystemCommand())
        commandProcessor.registerCommand("note", NotesCommand())
        commandProcessor.registerCommand("network", NetworkCommand())
        
        // Automation Commands
        commandProcessor.registerCommand("auto", AutomationCommand())
        
        // Add aliases for common operations
        registerAliases()
    }

    private fun registerAliases() {
        val aliasManager = tui.smartlauncher.core.AliasManager(this)
        
        // Common aliases
        aliasManager.addAlias("ls", "file ls")
        aliasManager.addAlias("cd", "file cd")
        aliasManager.addAlias("pwd", "file pwd")
        aliasManager.addAlias("cat", "file cat")
        aliasManager.addAlias("git", "git")
        aliasManager.addAlias("=", "calc")
        aliasManager.addAlias("sys", "system")
    }
}
```

### Step 3: Add Dependencies

Update `app/build.gradle` with required dependencies:

```gradle
dependencies {
    // Original T-UI dependencies...
    
    // Add smart launcher dependencies
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.google.code.gson:gson:2.10.1'
}
```

### Step 4: Create Configuration

Create `app/src/main/assets/secrets.properties`:

```properties
# MiniMax API Key for AI Features
# Get your API key from: https://minimax.chat/
MINIMAX_API_KEY=your_api_key_here
```

---

## Configure Permissions

### Update AndroidManifest.xml

Add required permissions to `app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Original T-UI permissions... -->

    <!-- Smart launcher permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

    <application
        android:allowBackup="true"
        android:label="@string/app_name"
        android:supportsRtl="true"
        tools:targetApi="31">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTask">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.HOME" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </activity>

        <!-- Original T-UI services... -->

    </application>

</manifest>
```

---

## Build and Run

### Build APK

```bash
# Navigate to project
cd tui-console

# Build debug APK
./gradlew assembleDebug

# APK location: app/build/outputs/apk/debug/app-debug.apk
```

### Install on Device

```bash
# Install via ADB
adb install app/build/outputs/apk/debug/app-debug.apk

# Or transfer APK manually and install
```

### Set as Default Launcher

1. Open the app after installation
2. When prompted, select **T-UI ConsoleLauncher** as default home app
3. Press home button to test

---

## Testing

### Test Original T-UI Commands

```bash
# Original T-UI commands should still work
help
clear
apps
calculator
settings
```

### Test Smart Launcher Commands

```bash
# Smart launcher commands
system                    # System info
file ls                   # List files
calc 2+2                  # Calculator
git status                # Git status
network ping google.com   # Network tools
note create "Test" Hello  # Quick notes
?? what is Android        # AI assistance
auto task Morning         # Tasker integration
```

### Command Examples

```bash
# File operations
file ls
file cd Documents
file cat notes.txt
file mkdir new_folder

# Git workflow
git status
git add .
git commit -m "Update"
git push

# System monitoring
system
system --cpu
system --battery
network status

# Quick calculations
calc 15 * 24 + 100
calc sqrt(144)
calc 100 C to F

# AI assistance
?? write a Python function
?? explain this error: [paste error]
?? what is machine learning

# Network diagnostics
network ping github.com
network localip
network publicip
```

---

## Available Commands

### Original T-UI Commands

| Command | Description |
|---------|-------------|
| `help` | Show T-UI help |
| `clear` | Clear terminal |
| `apps` | List applications |
| `calculator` | Open calculator |
| `contacts` | Show contacts |
| `settings` | Open settings |
| `restart` | Restart launcher |

### Smart Launcher Commands

| Category | Command | Description |
|----------|---------|-------------|
| **System** | `system` | System information |
| **Files** | `file ls` | List directory |
| **Files** | `file cd <dir>` | Change directory |
| **Files** | `file cat <file>` | View file |
| **Git** | `git status` | Repository status |
| **Git** | `git commit -m "<msg>"` | Commit changes |
| **Calculator** | `calc <expr>` | Calculate expression |
| **Notes** | `note create "Title" content` | Create note |
| **Network** | `network ping <host>` | Ping host |
| **AI** | `?? <question>` | Ask AI anything |
| **Automation** | `auto task <name>` | Run Tasker task |

### Aliases

| Alias | Expands To |
|-------|------------|
| `ls` | `file ls` |
| `cd` | `file cd` |
| `pwd` | `file pwd` |
| `cat` | `file cat` |
| `git` | `git` |
| `=` | `calc` |
| `sys` | `system` |

---

## Troubleshooting

### Build Fails

```bash
# Clean and rebuild
./gradlew clean
./gradlew assembleDebug

# Check Android Studio
# File → Invalidate Caches → Invalidate and Restart
```

### Commands Not Found

1. Ensure smart modules are copied to correct location
2. Check for compilation errors in Android Studio
3. Verify imports are correct
4. Clean and rebuild project

### Original T-UI Broken

- Don't modify original T-UI files
- Only add new files and modify MainActivity
- Keep original T-UI functionality intact
- Test original commands first

### AI Not Working

- Check API key in `secrets.properties`
- Verify internet connectivity
- Check Logcat for error messages
- Test with simple queries first

### Storage Access Issues

- Grant storage permissions on Android 10 and below
- For Android 11+, grant MANAGE_EXTERNAL_STORAGE
- Check app permissions in Settings

---

## Next Steps

1. **Test Integration**: Verify both original and smart commands work
2. **Configure AI**: Set up MiniMax API key for AI features
3. **Customize Commands**: Add or modify commands as needed
4. **Create Aliases**: Set up custom shortcuts for frequent operations
5. **Explore Features**: Try all available commands to understand capabilities

---

## Repository Information

- **T-UI ConsoleLauncher**: https://github.com/slab1/TUI-ConsoleLauncher.git
- **Original Creator**: @slab1
- **License**: See original repository for license details
- **Type**: Terminal-based Android launcher with extensible architecture

---

## Support

- **Original T-UI Issues**: Report on https://github.com/slab1/TUI-ConsoleLauncher/issues
- **Smart Launcher Issues**: Report in smart launcher repository
- **Documentation**: Check docs/ directory for detailed guides

---

## Credits

- **T-UI ConsoleLauncher**: Original project by [@slab1](https://github.com/slab1)
- **T-UI Smart Launcher**: Extension with AI and developer tools
- **MiniMax**: AI capabilities
- **Contributors**: All who contributed to both projects

---

## License

This integration inherits the license from T-UI ConsoleLauncher. See the original repository for specific license terms.
