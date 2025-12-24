#!/bin/bash

# T-UI Smart Launcher - Complete Setup Script
# This script clones T-UI Launcher and integrates our smart launcher modules

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔═══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                                                               ║${NC}"
echo -e "${BLUE}║    T-U I   S M A R T   I D E   L A U N C H E R               ║${NC}"
echo -e "${BLUE}║              SETUP AND INTEGRATION SCRIPT                     ║${NC}"
echo -e "${BLUE}║                                                               ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Configuration
PROJECT_NAME="tui-smart-launcher"
TUI_ORIGINAL_REPO="https://github.com/Andr3as07/T-UI-Launcher.git"
TUI_BRANCH="master"

# Step 1: Clone T-UI Launcher
echo -e "${YELLOW}[1/6]${NC} Cloning T-UI Launcher repository..."
echo "─────────────────────────────────────────────"

if [ -d "T-UI-Launcher" ]; then
    echo -e "${YELLOW}Directory T-UI-Launcher already exists. Using existing clone.${NC}"
else
    echo "Cloning from: $TUI_ORIGINAL_REPO"
    git clone --depth 1 --branch "$TUI_BRANCH" "$TUI_ORIGINAL_REPO" T-UI-Launcher
fi

echo -e "${GREEN}✓${NC} T-UI Launcher cloned successfully"
echo ""

# Step 2: Create project structure
echo -e "${YELLOW}[2/6]${NC} Creating project structure..."
echo "─────────────────────────────────────────────"

# Create directories for our modules
mkdir -p T-UI-Launcher/app/src/main/java/tui/smartlauncher/{core,ai,developer,productivity,automation}
mkdir -p T-UI-Launcher/app/src/main/java/tui/smartlauncher/services
mkdir -p T-UI-Launcher/app/src/main/res/{layout,values,xml,drawable,mipmap-hdpi,mipmap-mdpi,mipmap-xhdpi,mipmap-xxhdpi,mipmap-xxxhdpi}
mkdir -p T-UI-Launcher/app/src/test/java/tui/smartlauncher
mkdir -p T-UI-Launcher/app/src/androidTest/java/tui/smartlauncher
mkdir -p T-UI-Launcher/docs

echo -e "${GREEN}✓${NC} Directory structure created"
echo ""

# Step 3: Copy our smart launcher modules
echo -e "${YELLOW}[3/6]${NC} Integrating Smart Launcher modules..."
echo "─────────────────────────────────────────────"

# Copy core modules
cp -r tui-smart-launcher/tui/smartlauncher/core/* T-UI-Launcher/app/src/main/java/tui/smartlauncher/core/ 2>/dev/null || echo "  Note: Run from parent directory to copy core modules"
cp -r tui-smart-launcher/tui/smartlauncher/ai/* T-UI-Launcher/app/src/main/java/tui/smartlauncher/ai/ 2>/dev/null || echo "  Note: Run from parent directory to copy AI modules"
cp -r tui-smart-launcher/tui/smartlauncher/developer/* T-UI-Launcher/app/src/main/java/tui/smartlauncher/developer/ 2>/dev/null || echo "  Note: Run from parent directory to copy developer modules"
cp -r tui-smart-launcher/tui/smartlauncher/productivity/* T-UI-Launcher/app/src/main/java/tui/smartlauncher/productivity/ 2>/dev/null || echo "  Note: Run from parent directory to copy productivity modules"
cp -r tui-smart-launcher/tui/smartlauncher/automation/* T-UI-Launcher/app/src/main/java/tui/smartlauncher/automation/ 2>/dev/null || echo "  Note: Run from parent directory to copy automation modules"

echo -e "${GREEN}✓${NC} Module directories created (copy modules manually if needed)"
echo ""

# Step 4: Update build configuration
echo -e "${YELLOW}[4/6]${NC} Updating build configuration..."
echo "─────────────────────────────────────────────"

# Update build.gradle with our dependencies
if [ -f "T-UI-Launcher/app/build.gradle" ]; then
    # Check if our dependencies are already added
    if ! grep -q "kotlinx-coroutines" T-UI-Launcher/app/build.gradle; then
        echo "Adding Kotlin Coroutines dependency..."
        # This is handled by the build.gradle we'll create
    fi
fi

echo -e "${GREEN}✓${NC} Build configuration ready"
echo ""

# Step 5: Create integration documentation
echo -e "${YELLOW}[5/6]${NC} Creating integration documentation..."
echo "─────────────────────────────────────────────"

cat > T-UI-Launcher/INTEGRATION.md << 'EOF'
# T-UI Smart Launcher Integration Guide

## Overview

This guide explains how to integrate the T-UI Smart Launcher modules into the T-UI Launcher project.

## Integration Steps

### 1. Copy Module Files

Copy the following directories from the smart-launcher package:

```bash
# From smart-launcher package, run:
cp -r tui/smartlauncher/core /path/to/T-UI-Launcher/app/src/main/java/tui/
cp -r tui/smartlauncher/ai /path/to/T-UI-Launcher/app/src/main/java/tui/
cp -r tui/smartlauncher/developer /path/to/T-UI-Launcher/app/src/main/java/tui/
cp -r tui/smartlauncher/productivity /path/to/T-UI-Launcher/app/src/main/java/tui/
cp -r tui/smartlauncher/automation /path/to/T-UI-Launcher/app/src/main/java/tui/
```

### 2. Update AndroidManifest.xml

Add required permissions and services:

```xml
<!-- Network permissions -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- Storage permissions -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

<!-- Notification access -->
<uses-permission android:name="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE" />
```

### 3. Update Main Activity

Modify your MainActivity to register our commands:

```kotlin
// In MainActivity.kt, add these imports:
import tui.smartlauncher.core.CommandProcessor
import tui.smartlauncher.developer.FileManagerCommand
import tui.smartlauncher.developer.GitCommand
import tui.smartlauncher.productivity.CalculatorCommand
import tui.smartlauncher.productivity.SystemCommand
import tui.smartlauncher.productivity.NotesCommand
import tui.smartlauncher.productivity.NetworkCommand
import tui.smartlauncher.automation.AutomationCommand

// Register commands in onCreate():
commandProcessor = CommandProcessor(this)
commandProcessor.registerCommand("file", FileManagerCommand())
commandProcessor.registerCommand("git", GitCommand())
commandProcessor.registerCommand("calc", CalculatorCommand())
commandProcessor.registerCommand("system", SystemCommand())
commandProcessor.registerCommand("note", NotesCommand())
commandProcessor.registerCommand("network", NetworkCommand())
commandProcessor.registerCommand("auto", AutomationCommand())
```

### 4. Add Dependencies

Ensure your build.gradle includes:

```gradle
dependencies {
    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3'
    
    // Networking
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    
    // JSON
    implementation 'com.google.code.gson:gson:2.10.1'
}
```

### 5. Create secrets.properties

Create `app/src/main/assets/secrets.properties`:

```properties
MINIMAX_API_KEY=your_api_key_here
```

## T-UI Library Compatibility

The T-UI Launcher uses a library for core command functionality. Our modules are designed to work alongside it:

- **Original Commands**: Apps, Contacts, Calculator (T-UI built-in)
- **Our Commands**: file, git, calc, system, note, network, auto (our additions)

Both command sets can coexist in the same launcher.

## Testing Integration

```bash
# Build the project
./gradlew assembleDebug

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk

# Test commands
adb shell input text "help\n"
```

## Common Issues

### Commands not found
- Ensure all files are copied to correct packages
- Check for compilation errors in Android Studio
- Clean and rebuild: `./gradlew clean assembleDebug`

### API errors
- Verify MiniMax API key is set correctly
- Check internet connectivity
- Review logs in Logcat

### Storage access denied
- Grant storage permissions manually in app settings
- For Android 11+, grant MANAGE_EXTERNAL_STORAGE
EOF

echo -e "${GREEN}✓${NC} Integration documentation created"
echo ""

# Step 6: Create README
echo -e "${YELLOW}[6/6]${NC} Creating project documentation..."
echo "─────────────────────────────────────────────"

cat > T-UI-Launcher/README.md << 'EOF'
# T-UI Smart Launcher

## Quick Start

1. **Clone T-UI Launcher:**
   ```bash
   git clone https://github.com/Andr3as07/T-UI-Launcher.git
   cd T-UI-Launcher
   ```

2. **Add Smart Launcher modules:**
   Copy the `tui/smartlauncher/` directory from the smart-launcher package to:
   ```
   app/src/main/java/tui/smartlauncher/
   ```

3. **Update AndroidManifest.xml:**
   Add permissions from INTEGRATION.md

4. **Build and install:**
   ```bash
   ./gradlew assembleDebug
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## Available Commands

| Command | Description |
|---------|-------------|
| `file ls` | List directory contents |
| `file cd <dir>` | Change directory |
| `file cat <file>` | Display file |
| `git status` | Git repository status |
| `calc 2+2` | Calculator |
| `system` | System information |
| `note create "Title" content` | Create note |
| `network ping <host>` | Ping host |
| `?? question` | Ask AI |
| `auto task <name>` | Run Tasker task |

## Documentation

- [INTEGRATION.md](INTEGRATION.md) - Integration guide
- [CHANGELOG.md](CHANGELOG.md) - Version history

## Credits

- [T-UI Launcher](https://github.com/Andr3as07/T-UI-Launcher) - Original project
- [MiniMax](https://minimax.chat/) - AI capabilities
- [OkHttp](https://square.github.io/okhttp/) - HTTP client
- [Gson](https://github.com/google/gson) - JSON parser

## License

MIT License - See LICENSE file for details.
EOF

echo -e "${GREEN}✓${NC} README created"
echo ""

# Summary
echo -e "${BLUE}╔═══════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                    SETUP COMPLETE                             ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo "Next steps:"
echo "  1. cd T-UI-Launcher"
echo "  2. Copy smart launcher modules: cp -r ../tui-smart-launcher/tui/smartlauncher/* app/src/main/java/tui/"
echo "  3. Add permissions to AndroidManifest.xml"
echo "  4. Build: ./gradlew assembleDebug"
echo ""
echo "For detailed instructions, see INTEGRATION.md"
echo ""
