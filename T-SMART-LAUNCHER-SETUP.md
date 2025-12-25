# T-UI Smart Launcher - Complete Setup Guide

## Overview

This guide provides step-by-step instructions to set up the T-UI Smart Launcher by cloning the original T-UI Launcher repository and integrating our smart launcher modules.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Clone T-UI Launcher](#clone-t-ui-launcher)
3. [Project Structure](#project-structure)
4. [Integrate Smart Modules](#integrate-smart-modules)
5. [Configure Permissions](#configure-permissions)
6. [Build and Run](#build-and-run)
7. [Testing](#testing)

---

## Prerequisites

Before you begin, ensure you have the following installed on your computer:

### Required Software

| Software | Minimum Version | Purpose |
|----------|-----------------|---------|
| **Java JDK** | 17 or higher | Android development |
| **Android Studio** | Hedgehog (2023.1.1) | IDE and build tools |
| **Gradle** | 8.0+ | Build automation |
| **Android SDK** | 34 (API Level) | Target platform |
| **Git** | 2.0+ | Version control |

### Verify Your Setup

```bash
# Check Java version
java -version
# Should show: openjdk version "17" or higher

# Check Gradle version
gradle --version
# Should show: Gradle 8.0 or higher

# Check Android SDK
echo $ANDROID_HOME
# Should point to your Android SDK directory
```

---

## Clone T-UI Launcher

### Step 1: Open Terminal

Open a terminal or command prompt on your computer.

### Step 2: Navigate to Working Directory

```bash
# Choose where you want to store the project
cd ~/Projects  # or your preferred directory
```

### Step 3: Clone T-UI Launcher

```bash
# Clone the original T-UI Launcher repository
git clone https://github.com/Andr3as07/T-UI-Launcher.git tui-launcher

# Navigate into the cloned directory
cd tui-launcher

# Verify the clone
ls -la
# You should see: app/, build.gradle, settings.gradle, etc.
```

### Step 4: Clone Smart Launcher Modules

If you have the smart launcher modules in a separate location:

```bash
# From your smart launcher package location
cd /path/to/tui-smart-launcher

# Copy all module files
cp -r tui/smartlauncher ~/Projects/tui-launcher/app/src/main/java/tui/
```

---

## Project Structure

After cloning and copying modules, your project structure should look like this:

```
tui-launcher/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── tui/
│   │       │       ├── smartlauncher/          # ← Our modules
│   │       │       │   ├── core/
│   │       │       │   │   ├── CommandProcessor.kt
│   │       │       │   │   ├── CommandHandler.kt
│   │       │       │   │   ├── AliasManager.kt
│   │       │       │   │   ├── CommandHistory.kt
│   │       │       │   │   └── AppLauncherCommand.kt
│   │       │       │   ├── ai/
│   │       │       │   │   ├── MiniMaxService.kt
│   │       │       │   │   └── AIConfig.kt
│   │       │       │   ├── developer/
│   │       │       │   │   ├── FileManagerCommand.kt
│   │       │       │   │   └── GitCommand.kt
│   │       │       │   ├── productivity/
│   │       │       │   │   ├── CalculatorCommand.kt
│   │       │       │   │   ├── SystemCommand.kt
│   │       │       │   │   ├── NotesCommand.kt
│   │       │       │   │   └── NetworkCommand.kt
│   │       │       │   ├── automation/
│   │       │       │   │   └── AutomationCommand.kt
│   │       │       │   ├── MainActivity.kt
│   │       │       │   └── TerminalAdapter.kt
│   │       │       └── Andr3as07/              # ← Original T-UI
│   │       │           tuilibrary/
│   │       │           MainActivity.kt
│   │       │           └── ...
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   ├── values/
│   │       │   └── xml/
│   │       └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── gradle.properties
```

---

## Integrate Smart Modules

### Step 1: Update AndroidManifest.xml

Edit `app/src/main/AndroidManifest.xml` and add these permissions:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Network permissions for AI and network tools -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <!-- Storage permissions for file management -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="29" />

    <!-- WiFi state for network tools -->
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

    <!-- Notification access for notification features -->
    <uses-permission android:name="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
        tools:ignore="ProtectedPermissions" />

    <!-- Phone call permissions for automation -->
    <uses-permission android:name="android.permission.CALL_PHONE" />

    <!-- Foreground service for background operations -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.TUISmartLauncher"
        tools:targetApi="31">

        <!-- Main Launcher Activity -->
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTask"
            android:theme="@style/Theme.TUISmartLauncher">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.HOME" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </activity>

        <!-- Notification Listener Service -->
        <service
            android:name=".services.NotificationListener"
            android:exported="true"
            android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
            <intent-filter>
                <action android:name="android.service.notification.NotificationListenerService" />
            </intent-filter>
        </service>

        <!-- Foreground Service -->
        <service
            android:name=".services.BackgroundService"
            android:exported="false"
            android:foregroundServiceType="dataSync" />

        <!-- File Provider -->
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>

    </application>

</manifest>
```

### Step 2: Update build.gradle

Edit `app/build.gradle` and add the required dependencies:

```gradle
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}

android {
    namespace 'tui.smartlauncher'
    compileSdk 34

    defaultConfig {
        applicationId "tui.smartlauncher"
        minSdk 26
        targetSdk 34
        versionCode 1
        versionName "1.0.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
        debug {
            debuggable true
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = '17'
    }

    buildFeatures {
        viewBinding true
    }
}

dependencies {
    // Core Android
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.recyclerview:recyclerview:1.3.2'

    // Lifecycle
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'

    // Activity
    implementation 'androidx.activity:activity-ktx:1.8.2'
    implementation 'androidx.fragment:fragment-ktx:1.6.2'

    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3'

    // Networking
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'

    // JSON
    implementation 'com.google.code.gson:gson:2.10.1'

    // Testing
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.mockito:mockito-core:5.8.0'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}
```

### Step 3: Create secrets.properties

Create `app/src/main/assets/secrets.properties`:

```properties
# MiniMax API Key for AI features
MINIMAX_API_KEY=your_api_key_here

# Get your API key from: https://minimax.chat/
```

### Step 4: Update MainActivity

Modify your MainActivity to register our smart commands:

```kotlin
package tui.smartlauncher

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import tui.smartlauncher.automation.AutomationCommand
import tui.smartlauncher.core.CommandProcessor
import tui.smartlauncher.core.AppLauncherCommand
import tui.smartlauncher.developer.FileManagerCommand
import tui.smartlauncher.developer.GitCommand
import tui.smartlauncher.productivity.CalculatorCommand
import tui.smartlauncher.productivity.NotesCommand
import tui.smartlauncher.productivity.NetworkCommand
import tui.smartlauncher.productivity.SystemCommand

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var inputField: EditText
    private lateinit var commandProcessor: CommandProcessor
    private lateinit var terminalAdapter: TerminalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        initializeCommandProcessor()
        registerCommands()
        showWelcome()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.terminal_recycler_view)
        inputField = findViewById(R.id.command_input)

        terminalAdapter = TerminalAdapter()
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true
            }
            adapter = terminalAdapter
        }
    }

    private fun initializeCommandProcessor() {
        commandProcessor = CommandProcessor(this)
    }

    private fun registerCommands() {
        // Core Commands
        commandProcessor.registerCommand("launch", AppLauncherCommand())
        
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
    }

    private fun showWelcome() {
        val welcome = """
        ╔═══════════════════════════════════════════════════════╗
        ║    T-UI Smart IDE Launcher v1.0.0                    ║
        ║    Your intelligent terminal-based launcher           ║
        ╠═══════════════════════════════════════════════════════╣
        ║  Quick Start:                                         ║
        ║    help     - Show all commands                      ║
        ║    launch   - Launch applications                    ║
        ║    ??       - Ask AI anything                        ║
        ║    file     - File management                        ║
        ║    calc     - Calculator                             ║
        ╚═══════════════════════════════════════════════════════╝
        """.trimIndent()
        terminalAdapter.addOutput(welcome)
    }

    // ... rest of the activity code
}
```

---

## Configure Permissions

### Android 10 and Below

1. Open the app on your device
2. Grant storage permission when prompted
3. Grant notification access in system settings

### Android 11 and Above

For Android 11+, you'll need to grant MANAGE_EXTERNAL_STORAGE for full file system access:

1. Open **Settings** → **Apps** → **T-UI Smart Launcher**
2. Tap **Permissions**
3. Select **Files and media** (or **Storage**)
4. Select **Allow all files access**

### Notification Access

1. Open **Settings** → **Apps** → **T-UI Smart Launcher**
2. Tap **Permissions**
3. Enable **Notification access**

---

## Build and Run

### Build Debug APK

```bash
# Navigate to project directory
cd tui-launcher

# Sync Gradle
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# APK will be at: app/build/outputs/apk/debug/app-debug.apk
```

### Install on Device

```bash
# Method 1: Using ADB
adb install app/build/outputs/apk/debug/app-debug.apk

# Method 2: Manual
# Transfer APK to your device and tap to install
```

### Set as Default Launcher

1. Open the app after installation
2. When prompted, select **T-UI Smart Launcher** as default home app
3. Press home button to test

---

## Testing

### Test Basic Commands

Open the launcher and type these commands:

```bash
# Check system info
system

# List files
file ls

# Calculate something
calc 2 + 2

# Test app launcher
launch settings

# Get help
help
```

### Test AI Features

```bash
# Ask a question
?? what is Android

# Generate code
?? write a Python hello world
```

### Test Network Tools

```bash
# Ping Google
network ping google.com

# Check IP
network localip
```

---

## Troubleshooting

### Build Fails

```bash
# Clean and rebuild
./gradlew clean
./gradlew assembleDebug

# Invalidate caches (in Android Studio)
# File → Invalidate Caches → Invalidate and Restart
```

### Commands Not Found

- Ensure all `.kt` files are in correct directories
- Check for compilation errors in Android Studio
- Verify imports are correct

### AI Not Working

- Check API key in `secrets.properties`
- Verify internet connectivity
- Check Logcat for error messages

### Storage Access Issues

- Grant MANAGE_EXTERNAL_STORAGE on Android 11+
- Check app permissions in Settings

---

## Available Commands

### Core Commands

| Command | Description |
|---------|-------------|
| `help` | Show help information |
| `clear` | Clear terminal screen |
| `launch <app>` | Launch applications |

### Developer Commands

| Command | Description |
|---------|-------------|
| `file ls` | List directory contents |
| `file cd <dir>` | Change directory |
| `file cat <file>` | Display file contents |
| `file mkdir <name>` | Create directory |
| `file rm <path>` | Delete file |
| `git status` | Show git status |
| `git commit -m "<msg>"` | Commit changes |
| `git push` | Push to remote |

### Productivity Commands

| Command | Description |
|---------|-------------|
| `calc <expr>` | Calculate expression |
| `system` | Show system info |
| `note create "Title" content` | Create note |
| `network ping <host>` | Ping host |
| `network localip` | Show local IP |

### Automation Commands

| Command | Description |
|---------|-------------|
| `auto task <name>` | Run Tasker task |
| `auto termux <cmd>` | Run Termux command |

### AI Commands

| Command | Description |
|---------|-------------|
| `?? <question>` | Ask AI anything |
| `?? code <request>` | Generate code |
| `?? debug <error>` | Debug assistance |

---

## Next Steps

1. **Explore Commands**: Try different commands to understand capabilities
2. **Configure AI**: Set up your MiniMax API key for AI features
3. **Create Aliases**: Set up custom aliases for frequently used commands
4. **Customize Theme**: Modify colors and styling in resources
5. **Add Commands**: Implement custom commands following CommandHandler interface

---

## Support

- **Issues**: Report bugs on GitHub Issues
- **Documentation**: See README.md for full documentation
- **Wiki**: Check T-UI Launcher wiki for original project info

---

## Credits

- **T-UI Launcher**: Original project by [Andr3as07](https://github.com/Andr3as07)
- **MiniMax**: AI capabilities
- **OkHttp**: HTTP client
- **Gson**: JSON parser
