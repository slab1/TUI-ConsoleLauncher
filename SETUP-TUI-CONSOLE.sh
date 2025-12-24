#!/bin/bash

# =============================================================================
# T-UI Smart Launcher - Automated Setup Script
# =============================================================================
# This script clones T-UI ConsoleLauncher and integrates the Smart Launcher modules
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
TUI_REPO="https://github.com/slab1/TUI-ConsoleLauncher.git"
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
# STEP 2: Clone T-UI ConsoleLauncher
# =============================================================================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${YELLOW}[ STEP 2/7 ]${NC}  ${WHITE}Cloning T-UI ConsoleLauncher Repository...${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

# Choose project location
DEFAULT_DIR="$HOME/Projects"
read -p "Enter project directory (default: $DEFAULT_DIR): " PROJECT_DIR
PROJECT_DIR=${PROJECT_DIR:-$DEFAULT_DIR}

mkdir -p "$PROJECT_DIR"
cd "$PROJECT_DIR"

# Clone T-UI ConsoleLauncher
if [ -d "TUI-ConsoleLauncher" ]; then
    echo -e "${YELLOW}TUI-ConsoleLauncher directory already exists.${NC}"
    read -p "Use existing directory? (y/n): " USE_EXISTING
    if [ "$USE_EXISTING" != "y" ] && [ "$USE_EXISTING" != "Y" ]; then
        echo "Aborting."
        exit 1
    fi
else
    echo "Cloning T-UI ConsoleLauncher from GitHub..."
    echo "Repository: $TUI_REPO"
    git clone --depth 1 "$TUI_REPO" TUI-ConsoleLauncher
fi

cd TUI-ConsoleLauncher
echo -e "${GREEN}✓ T-UI ConsoleLauncher cloned successfully${NC}"
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
    echo -e "${YELLOW}Creating sample modules structure...${NC}"
    
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
    
    # Create a simple example module
    mkdir -p app/src/main/java/tui/smartlauncher/core
    cat > app/src/main/java/tui/smartlauncher/core/ExampleCommand.kt << 'EOF'
package tui.smartlauncher.core

import android.content.Context
import tui.smartlauncher.core.CommandHandler

/**
 * Example smart command for T-UI ConsoleLauncher
 */
class ExampleCommand : CommandHandler {
    override fun getName(): String = "example"
    
    override fun getAliases(): List<String> = listOf("ex", "sample")
    
    override fun getDescription(): String = "Example smart launcher command"
    
    override fun getUsage(): String = """
        Example command usage:
        example - Basic example
        ex      - Short alias
    """.trimIndent()
    
    override fun execute(context: Context, args: List<String>): String {
        return """
        ┌─────────────────────────────────────────────────────────────────────┐
        │                    T-UI SMART LAUNCHER                             │
        ├─────────────────────────────────────────────────────────────────────┤
        │  Welcome to T-UI Smart Launcher!                                   │
        │                                                                     │
        │  This is an example command. Replace this with your actual         │
        │  smart launcher modules for full functionality.                    │
        │                                                                     │
        │  Available modules to copy:                                        │
        │    - Core: CommandProcessor, AliasManager, History                 │
        │    - AI: MiniMax integration for intelligent assistance            │
        │    - Developer: File manager, Git commands                         │
        │    - Productivity: Calculator, System monitor, Notes               │
        │    - Automation: Tasker and Termux integration                     │
        │                                                                     │
        └─────────────────────────────────────────────────────────────────────┘
        """.trimIndent()
    }
}
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

# Update AndroidManifest.xml if needed
echo "Checking AndroidManifest.xml..."
if [ -f "app/src/main/AndroidManifest.xml" ]; then
    echo -e "${GREEN}✓ AndroidManifest.xml already exists${NC}"
else
    echo "Creating AndroidManifest.xml..."
    mkdir -p app/src/main
    cat > app/src/main/AndroidManifest.xml << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Network permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <!-- Storage permissions -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

    <!-- WiFi state -->
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

    <!-- Notification access -->
    <uses-permission android:name="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
        tools:ignore="ProtectedPermissions" />

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

    </application>

</manifest>
EOF
    echo -e "${GREEN}✓ AndroidManifest.xml created${NC}"
fi

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

## Overview

This project integrates smart launcher capabilities into T-UI ConsoleLauncher.

## Quick Start

```bash
# Build the project
./gradlew assembleDebug

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Available Commands

### Original T-UI Commands
- `help` - Show T-UI help
- `clear` - Clear screen
- `apps` - List apps
- `calculator` - Open calculator

### Smart Launcher Commands (after integration)
- `system` - Display system info
- `file ls` - List files
- `git status` - Git status
- `calc 2+2` - Calculator
- `note create "Title" content` - Create note
- `network ping <host>` - Ping host
- `?? question` - Ask AI
- `auto task <name>` - Run Tasker task

## Integration

1. Copy smart launcher modules to `app/src/main/java/tui/smartlauncher/`
2. Update MainActivity to register commands
3. Add dependencies to build.gradle
4. Configure MiniMax API key in secrets.properties

## Documentation

See docs/ directory for detailed guides.
EOF

# Create integration guide
cat > docs/INTEGRATION.md << 'EOF'
# Integration Guide

## T-UI ConsoleLauncher + Smart Launcher Integration

### Repository Information
- **T-UI ConsoleLauncher**: https://github.com/slab1/TUI-ConsoleLauncher
- **Original Creator**: @slab1
- **Type**: Terminal-based Android launcher

### Integration Steps

1. **Clone T-UI ConsoleLauncher**
   ```bash
   git clone https://github.com/slab1/TUI-ConsoleLauncher.git
   cd TUI-ConsoleLauncher
   ```

2. **Copy Smart Modules**
   ```bash
   cp -r /path/to/smart-launcher/tui/smartlauncher/* app/src/main/java/tui/
   ```

3. **Update MainActivity**
   Add smart command registration in the main activity.

4. **Add Dependencies**
   Update build.gradle with required libraries.

5. **Configure API**
   Set MiniMax API key in secrets.properties.

### Command Categories

**Original T-UI Commands:**
- Built-in launcher commands
- Apps, contacts, settings
- Terminal emulation

**Smart Launcher Commands:**
- File management (file ls, file cd)
- Developer tools (git, system monitor)
- Productivity (calculator, notes, network)
- AI assistance (?? queries)
- Automation (Tasker, Termux)

Both command sets coexist seamlessly.
EOF

# Create command reference
cat > docs/COMMANDS.md << 'EOF'
# Command Reference

## Original T-UI Commands

| Command | Description |
|---------|-------------|
| `help` | Show T-UI help |
| `clear` | Clear screen |
| `apps` | List applications |
| `calculator` | Open calculator |
| `contacts` | Show contacts |
| `settings` | Open settings |
| `restart` | Restart launcher |

## Smart Launcher Commands

### Core Commands
| Command | Description |
|---------|-------------|
| `system` | Display system information |
| `calc <expr>` | Calculate expression |
| `?? <question>` | Ask AI anything |

### File Management
| Command | Description |
|---------|-------------|
| `file ls` | List directory contents |
| `file cd <dir>` | Change directory |
| `file cat <file>` | Display file |
| `file mkdir <name>` | Create directory |
| `file rm <path>` | Delete file |

### Git Commands
| Command | Description |
|---------|-------------|
| `git status` | Show repository status |
| `git commit -m "<msg>"` | Commit changes |
| `git push` | Push to remote |
| `git pull` | Pull from remote |

### Network Tools
| Command | Description |
|---------|-------------|
| `network ping <host>` | Ping host |
| `network localip` | Show local IP |
| `network publicip` | Show public IP |

### Notes
| Command | Description |
|---------|-------------|
| `note create "Title" content` | Create note |
| `note list` | List all notes |
| `note show <id>` | Display note |

### Automation
| Command | Description |
|---------|-------------|
| `auto task <name>` | Run Tasker task |
| `auto termux <cmd>` | Run Termux command |

## Examples

```bash
# Quick calculations
calc 2 + 2 * 3
calc sqrt(144)
calc 100 USD to EUR

# File operations
file ls
file cd Documents
file cat notes.txt

# Git workflow
git status
git add .
git commit -m "Update documentation"
git push

# System info
system
system --cpu
system --battery

# AI assistance
?? what is machine learning
?? write a Python hello world
?? debug this error: [paste error]

# Network diagnostics
network ping google.com
network dns github.com
network localip

# Quick notes
note create "Meeting Notes" Discussion points here
note list
```

## Command Combinations

```bash
# Create and edit a file
file touch script.py
file cat script.py

# Git workflow with file operations
file ls
git status
git add .
git commit -m "Add new feature"

# System monitoring
system
network status
note create "System Report" $(system)

# AI with context
?? explain this code: $(file cat script.py)
```
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
   - File → Open → Select TUI-ConsoleLauncher directory
   - Wait for Gradle sync to complete

2. BUILD FROM COMMAND LINE
   cd TUI-ConsoleLauncher
   ./gradlew assembleDebug

3. INSTALL ON DEVICE
   adb install app/build/outputs/apk/debug/app-debug.apk

4. SET AS DEFAULT LAUNCHER
   - Open the app
   - When prompted, set as default home app
   - Press home button to test

5. TEST COMMANDS
   - Type 'help' to see available commands
   - Type 'example' to see the smart launcher
   - Add smart modules for full functionality

└─────────────────────────────────────────────────────────────────────────────┘

EOF

# =============================================================================
# Summary
# =============================================================================
echo -e "${CYAN}"
echo "╔══════════════════════════════════════════════════════════════════════╗"
echo "║                    SETUP COMPLETE!                                    ║"
echo "╠══════════════════════════════════════════════════════════════════════╣"
echo "║  Project location: $PROJECT_DIR/TUI-ConsoleLauncher                 ║"
echo "║  Repository: https://github.com/slab1/TUI-ConsoleLauncher           ║"
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
echo "║  Repository Information:                                             ║"
echo "║    - Original: T-UI ConsoleLauncher by @slab1                       ║"
echo "║    - GitHub: https://github.com/slab1/TUI-ConsoleLauncher          ║"
echo "║    - Type: Terminal-based Android launcher                          ║"
echo "╚══════════════════════════════════════════════════════════════════════╝"
echo -e "${NC}"
