#!/bin/bash

# =============================================================================
# T-UI Smart Launcher - Automated Setup Script
# =============================================================================
# This script clones T-UI Launcher and integrates the Smart Launcher modules
# Run this on your local machine with Git and Android Studio installed
# =============================================================================

set -e  # Exit on any error

# Colors for beautiful output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m' # No Color

# Configuration
TUI_REPO="https://github.com/Andr3as07/T-UI-Launcher.git"
PROJECT_NAME="tui-smart-launcher"
BRANCH="master"

echo -e "${CYAN}"
echo "╔══════════════════════════════════════════════════════════════════════╗"
echo "║                                                                      ║"
echo "║       T-U I   S M A R T   L A U N C H E R   S E T U P               ║"
echo "║                                                                      ║"
echo "║              Automated Setup and Integration Script                  ║"
echo "║                                                                      ║"
echo "╚══════════════════════════════════════════════════════════════════════╝"
echo -e "${NC}"
echo ""

# =============================================================================
# STEP 1: Check Prerequisites
# =============================================================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}[ STEP 1/7 ]${NC}  ${WHITE}Checking Prerequisites...${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

check_command() {
    if command -v "$1" &> /dev/null; then
        echo -e "  ${GREEN}✓${NC} $1: $($1 --version 2>&1 | head -n1)"
        return 0
    else
        echo -e "  ${RED}✗${NC} $1: Not found"
        return 1
    fi
}

PREREQS_OK=true
check_command git || PREREQS_OK=false
check_command java || PREREQS_OK=false
check_command gradle || PREREQS_OK=false

if [ "$PREREQS_OK" = false ]; then
    echo ""
    echo -e "${RED}Error: Missing required tools. Please install them first.${NC}"
    echo "  - Git: https://git-scm.com/downloads"
    echo "  - Java: https://adoptium.net/"
    echo "  - Gradle: https://gradle.org/install/"
    exit 1
fi

echo -e "${GREEN}All prerequisites satisfied!${NC}"
echo ""

# =============================================================================
# STEP 2: Clone T-UI Launcher
# =============================================================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}[ STEP 2/7 ]${NC}  ${WHITE}Cloning T-UI Launcher Repository...${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# Choose project location
DEFAULT_DIR="$HOME/Projects"
read -p "Enter project directory (default: $DEFAULT_DIR): " PROJECT_DIR
PROJECT_DIR=${PROJECT_DIR:-$DEFAULT_DIR}

mkdir -p "$PROJECT_DIR"
cd "$PROJECT_DIR"

# Clone T-UI Launcher
if [ -d "T-UI-Launcher" ]; then
    echo -e "${YELLOW}T-UI-Launcher directory already exists.${NC}"
    read -p "Use existing directory? (y/n): " USE_EXISTING
    if [ "$USE_EXISTING" != "y" ] && [ "$USE_EXISTING" != "Y" ]; then
        echo "Aborting."
        exit 1
    fi
else
    echo "Cloning T-UI Launcher from GitHub..."
    git clone --depth 1 "$TUI_REPO" T-UI-Launcher
fi

cd T-UI-Launcher
echo -e "${GREEN}✓ T-UI Launcher cloned successfully${NC}"
echo ""

# =============================================================================
# STEP 3: Clone Smart Launcher Modules
# =============================================================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}[ STEP 3/7 ]${NC}  ${WHITE}Setting Up Project Structure...${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# Create directory structure for smart modules
echo "Creating module directories..."
mkdir -p app/src/main/java/tui/smartlauncher/{core,ai,developer,productivity,automation}
mkdir -p app/src/main/java/tui/smartlauncher/services
mkdir -p app/src/main/res/{layout,values,xml}
mkdir -p app/src/test/java/tui/smartlauncher
mkdir -p docs
echo -e "${GREEN}✓ Directories created${NC}"
echo ""

# =============================================================================
# STEP 4: Copy Smart Launcher Modules
# =============================================================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}[ STEP 4/7 ]${NC}  ${WHITE}Copying Smart Launcher Modules...${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# Ask where smart launcher modules are located
echo "Where is your smart launcher modules directory?"
echo "  (This contains the tui/smartlauncher/ directory with all the .kt files)"
read -p "Path (or press Enter to create sample modules): " SMART_MODULES_PATH

if [ -d "$SMART_MODULES_PATH/tui/smartlauncher" ]; then
    echo "Copying modules..."
    cp -r "$SMART_MODULES_PATH/tui/smartlauncher/"* app/src/main/java/tui/smartlauncher/
    echo -e "${GREEN}✓ Modules copied successfully${NC}"
else
    echo -e "${YELLOW}Creating sample modules...${NC}"
    
    # Create a sample module structure for reference
    cat > app/src/main/java/tui/smartlauncher/README.md << 'EOF'
# T-UI Smart Launcher Modules

Copy your smart launcher modules here:
- core/ - Core command processing
- ai/ - AI integration (MiniMax)
- developer/ - Developer tools
- productivity/ - Productivity tools
- automation/ - Automation integration

For full modules, see the smart-launcher package.
EOF
    echo -e "${GREEN}✓ Sample structure created${NC}"
fi
echo ""

# =============================================================================
# STEP 5: Configure Project Files
# =============================================================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}[ STEP 5/7 ]${NC}  ${WHITE}Configuring Project Files...${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# Create secrets.properties
echo "Creating secrets.properties..."
mkdir -p app/src/main/assets
cat > app/src/main/assets/secrets.properties << 'EOF'
# MiniMax API Key for AI Features
# Get your API key from: https://minimax.chat/
MINIMAX_API_KEY=your_api_key_here
EOF
echo -e "${GREEN}✓ secrets.properties created${NC}"

# Update AndroidManifest.xml
echo "Updating AndroidManifest.xml..."
cat > app/src/main/AndroidManifest.xml << 'MANIFEST_EOF'
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

    <!-- Notification access -->
    <uses-permission android:name="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
        tools:ignore="ProtectedPermissions" />

    <!-- Phone call permissions for automation -->
    <uses-permission android:name="android.permission.CALL_PHONE" />

    <!-- Foreground service -->
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

        <service
            android:name=".services.NotificationListener"
            android:exported="true"
            android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
            <intent-filter>
                <action android:name="android.service.notification.NotificationListenerService" />
            </intent-filter>
        </service>

        <service
            android:name=".services.BackgroundService"
            android:exported="false"
            android:foregroundServiceType="dataSync" />

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
MANIFEST_EOF
echo -e "${GREEN}✓ AndroidManifest.xml updated${NC}"

# Create file_paths.xml
echo "Creating file_paths.xml..."
mkdir -p app/src/main/res/xml
cat > app/src/main/res/xml/file_paths.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-path name="external_files" path="." />
    <files-path name="internal_files" path="." />
    <cache-path name="cache" path="." />
    <external-files-path name="external_app_files" path="." />
</paths>
EOF
echo -e "${GREEN}✓ file_paths.xml created${NC}"
echo ""

# =============================================================================
# STEP 6: Create Documentation
# =============================================================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}[ STEP 6/7 ]${NC}  ${WHITE}Creating Documentation...${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# Create quick start guide
cat > QUICKSTART.md << 'EOF'
# T-UI Smart Launcher - Quick Start Guide

## What's Included

This is the T-UI Smart Launcher project, integrating AI-powered commands into T-UI Launcher.

## Quick Start

```bash
# Build the project
./gradlew assembleDebug

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Available Commands

| Command | Description |
|---------|-------------|
| `help` | Show all commands |
| `system` | Display system info |
| `file ls` | List files |
| `git status` | Git status |
| `calc 2+2` | Calculator |
| `note create "Title" content` | Create note |
| `network ping <host>` | Ping host |
| `?? question` | Ask AI |
| `auto task <name>` | Run Tasker task |

## Configuration

1. Set MiniMax API key in `app/src/main/assets/secrets.properties`
2. Grant storage permissions on Android 11+
3. Set as default home launcher

## Documentation

See docs/SETUP.md for detailed setup instructions.
EOF

# Create detailed setup guide
cat > docs/SETUP.md << 'EOF'
# Detailed Setup Guide

See the full setup guide in T-SMART-LAUNCHER-SETUP.md in the parent directory.
EOF

# Create command reference
cat > docs/COMMANDS.md << 'EOF'
# Command Reference

## Core Commands

- `help` - Show help
- `clear` - Clear screen
- `launch <app>` - Launch apps

## Developer Commands

- `file ls` - List directory
- `file cd <dir>` - Change directory
- `file cat <file>` - View file
- `file mkdir <name>` - Create directory
- `file rm <path>` - Delete file
- `git status` - Git status
- `git commit -m "<msg>"` - Commit
- `git push` - Push changes

## Productivity

- `calc <expr>` - Calculate
- `system` - System info
- `note create "Title" content` - Create note
- `note list` - List notes
- `network ping <host>` - Ping
- `network localip` - Local IP

## Automation

- `auto task <name>` - Run Tasker task
- `auto termux <cmd>` - Run Termux command

## AI

- `?? <question>` - Ask AI
- `?? code <request>` - Generate code
- `?? debug <error>` - Debug help
EOF

echo -e "${GREEN}✓ Documentation created${NC}"
echo ""

# =============================================================================
# STEP 7: Build Instructions
# =============================================================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}[ STEP 7/7 ]${NC}  ${WHITE}Build Instructions...${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

cat << 'EOF'

┌─────────────────────────────────────────────────────────────────────────────┐
│                          BUILD INSTRUCTIONS                                  │
└─────────────────────────────────────────────────────────────────────────────┘

1. OPEN IN ANDROID STUDIO
   - File → Open → Select T-UI-Launcher directory
   - Wait for Gradle sync to complete

2. BUILD FROM COMMAND LINE
   cd T-UI-Launcher
   ./gradlew assembleDebug

3. INSTALL ON DEVICE
   adb install app/build/outputs/apk/debug/app-debug.apk

4. SET AS DEFAULT LAUNCHER
   - Open the app
   - When prompted, set as default home app
   - Press home button to test

└─────────────────────────────────────────────────────────────────────────────┘

EOF

# =============================================================================
# Summary
# =============================================================================
echo -e "${CYAN}"
echo "╔══════════════════════════════════════════════════════════════════════╗"
echo "║                    SETUP COMPLETE!                                    ║"
echo "╠══════════════════════════════════════════════════════════════════════╣"
echo "║  Project location: $PROJECT_DIR/T-UI-Launcher                       ║"
echo "║                                                                      ║"
echo "║  Next steps:                                                         ║"
echo "║    1. Open project in Android Studio                                ║"
echo "║    2. Copy smart launcher modules to:                               ║"
echo "║       app/src/main/java/tui/smartlauncher/                          ║"
echo "║    3. Add your MiniMax API key to:                                  ║"
echo "║       app/src/main/assets/secrets.properties                        ║"
echo "║    4. Build: ./gradlew assembleDebug                                ║"
echo "║    5. Install and enjoy!                                            ║"
echo "║                                                                      ║"
echo "╚══════════════════════════════════════════════════════════════════════╝"
echo -e "${NC}"
