# T-UI Smart IDE Launcher - Quick Start Guide

## Project Overview

T-UI Smart IDE Launcher is a comprehensive Android launcher that combines terminal efficiency with IDE capabilities and AI-powered assistance. This project extends the popular T-UI Launcher with developer tools, productivity features, and MiniMax AI integration.

## What's Been Built

### Core Architecture
- **CommandProcessor**: Central command parsing and execution engine
- **CommandHandler Interface**: Standard interface for all commands
- **AliasManager**: User-defined command shortcuts
- **CommandHistory**: Persistent command history with search
- **AppLauncherCommand**: Intelligent app launching with fuzzy search

### AI Integration
- **MiniMaxService**: Full API integration with streaming support
- **AIConfig**: Configuration management for API keys and settings
- **Query type detection**: Auto-detects code, debug, general queries

### Developer Tools
- **FileManagerCommand**: Complete file operations (ls, cd, cat, mkdir, rm, cp, mv)
- **GitCommand**: Git integration (clone, status, commit, push, pull)

### Productivity Tools
- **CalculatorCommand**: Expression evaluation, scientific functions, unit conversions
- **SystemCommand**: Real-time CPU, memory, battery, storage monitoring
- **NotesCommand**: Quick note capture and retrieval
- **NetworkCommand**: Ping, DNS, port scan, IP lookup, speed test

### Automation
- **AutomationCommand**: Tasker and Termux integration, broadcast intents

## Project Structure

```
tui-smart-launcher/
├── app/src/main/
│   ├── java/tui/smartlauncher/
│   │   ├── MainActivity.kt          # Main launcher activity
│   │   ├── TerminalAdapter.kt       # Terminal display adapter
│   │   ├── core/
│   │   │   ├── CommandProcessor.kt
│   │   │   ├── CommandHandler.kt
│   │   │   ├── AliasManager.kt
│   │   │   ├── CommandHistory.kt
│   │   │   └── AppLauncherCommand.kt
│   │   ├── ai/
│   │   │   └── MiniMaxService.kt
│   │   ├── developer/
│   │   │   ├── FileManagerCommand.kt
│   │   │   └── GitCommand.kt
│   │   ├── productivity/
│   │   │   ├── CalculatorCommand.kt
│   │   │   ├── SystemCommand.kt
│   │   │   ├── NotesCommand.kt
│   │   │   └── NetworkCommand.kt
│   │   └── automation/
│   │       └── AutomationCommand.kt
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml
│   │   │   └── item_terminal_line.xml
│   │   ├── values/
│   │   │   ├── strings.xml
│   │   │   ├── colors.xml
│   │   │   └── themes.xml
│   │   └── xml/
│   │       └── file_paths.xml
│   └── AndroidManifest.xml
├── build.gradle                      # Root build file
├── settings.gradle
├── gradle.properties
└── README.md                         # Full documentation
```

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 or higher
- Gradle 8.0+
- Android SDK 34

### Building the Project

1. **Clone and Open**
   ```bash
   git clone <repository-url>
   cd tui-smart-launcher
   open in Android Studio
   ```

2. **Sync Gradle**
   - File → Sync Project with Gradle Files
   - Wait for dependencies to download

3. **Build Debug APK**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Install on Device**
   - Transfer APK to your Android device
   - Enable "Install from unknown sources"
   - Tap to install

### Running in Emulator

1. Create an Android Virtual Device (API 26+)
2. Run the app from Android Studio (Shift + F10)
3. Set as default home launcher when prompted

## Key Commands Quick Reference

| Command | Description |
|---------|-------------|
| `help` | Show all commands |
| `launch <app>` | Launch apps with fuzzy search |
| `file ls` | List files |
| `file cd <dir>` | Change directory |
| `git status` | Show git status |
| `calc 2+2` | Calculate expression |
| `system` | Show system info |
| `note create "Title" content` | Create note |
| `network ping google.com` | Ping host |
| `?? question` | Ask AI anything |
| `auto task <name>` | Run Tasker task |

## Configuration

### AI Setup (MiniMax API)

1. Get API key from MiniMax developer portal
2. Configure in code or create `secrets.properties`:
   ```properties
   MINIMAX_API_KEY=your_api_key_here
   ```

### Storage Permissions

Grant MANAGE_EXTERNAL_STORAGE for full file system access (Android 11+).

## Testing

```bash
# Run unit tests
./gradlew test

# Run lint checks
./gradlew lint

# Run instrumented tests
./gradlew connectedAndroidTest
```

## Development

### Adding New Commands

1. Create new command class implementing `CommandHandler`:
   ```kotlin
   class MyCommand : CommandHandler {
       override fun getName() = "mycommand"
       override fun getDescription() = "Does something useful"
       override fun execute(context: Context, args: List<String>): String {
           return "Result"
       }
   }
   ```

2. Register in `MainActivity`:
   ```kotlin
   commandProcessor.registerCommand("mycommand", MyCommand())
   ```

### Modifying AI Prompts

Edit prompts in `MiniMaxService.kt`:
- `buildSystemPrompt()` - System prompt configuration
- `buildPrompt()` - Query-specific prompts

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Build fails | Update Android Studio and SDK tools |
| Commands not found | Check command registration in MainActivity |
| AI not working | Verify API key in secrets.properties |
| File ops fail | Grant storage permissions |
| App crashes | Check logcat for error details |

## Next Steps

1. Complete AI integration with streaming responses
2. Add code editor component (Monaco/WebView)
3. Implement persistent terminal sessions
4. Add script execution system
5. Expand Tasker integration

## Resources

- [Full Documentation](README.md)
- [GitHub Repository](https://github.com/yourusername/tui-smart-launcher)
- [Issue Tracker](https://github.com/yourusername/tui-smart-launcher/issues)

## License

MIT License - See LICENSE file for details.
