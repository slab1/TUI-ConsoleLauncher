# T-UI Launcher MiniMax News Module

This module adds AI-powered news capabilities to T-UI Launcher using MiniMax's language models.

## Features

- **Natural Language Queries**: Get news on any topic using simple commands
- **AI Summarization**: Concise, AI-generated news summaries optimized for terminal display
- **Multiple Topics**: Technology, sports, finance, politics, entertainment, and more
- **Detailed Mode**: Option for in-depth analysis with fewer stories
- **Caching**: Reduces API calls and improves response times
- **CLI-Optimized**: Output formatted specifically for terminal interfaces

## Installation

### Step 1: Clone and Setup

```bash
# Clone T-UI Launcher repository
git clone https://github.com/Andr3as07/T-UI-Launcher.git
cd T-UI-Launcher

# Open in Android Studio
# File → Open → Select T-UI-Launcher folder
```

### Step 2: Add Dependencies

In `app/build.gradle`, add:

```gradle
dependencies {
    // Existing dependencies...
    
    // OkHttp for network requests
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    
    // Gson for JSON parsing
    implementation 'com.google.code.gson:gson:2.10.1'
    
    // Kotlin Coroutines for async operations
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
}
```

### Step 3: Add Module Files

Copy the following files to your project:

```
tui/
└── feature/
    └── news/
        ├── models/
        │   └── NewsModels.kt
        ├── MiniMaxService.kt
        ├── MiniMaxRepository.kt
        ├── NewsConfig.kt
        └── NewsCommand.kt
```

### Step 4: Configure API Key

Create `app/src/main/assets/secrets.properties`:

```properties
MINIMAX_API_KEY=your_api_key_here
```

Or set it programmatically through SharedPreferences.

### Step 5: Register the Command

In your main activity or command loader, register the news command:

```kotlin
import tui.feature.news.NewsCommand

// In your command registration logic:
commandRegistry.register(NewsCommand())
```

## Usage

Once installed, use the following commands in T-UI:

| Command | Description |
|---------|-------------|
| `news` | Get general top headlines |
| `news tech` | Technology news |
| `news sports` | Sports news |
| `news finance` | Financial news |
| `news -d` | Detailed mode (fewer, deeper stories) |
| `news crypto -d` | Detailed cryptocurrency news |
| `news --config` | Show current configuration |
| `news --help` | Show help message |

## Configuration

### Programmatic Configuration

```kotlin
val config = NewsConfig(context)

// Set API key
config.setApiKey("your_api_key")

// Enable detailed mode
config.isDetailedMode = true

// Set default topic
config.defaultTopic = "technology"

// Toggle caching
config.isCacheEnabled = true
```

### Settings Summary

Use `news --config` to view current settings:

```
News Module Settings:
  API Key: ✓ Configured
  Detailed Mode: Disabled
  Default Topic: general
  Show Sources: Yes
  Caching: Enabled
  Max Items: 5
```

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                    T-UI Launcher                     │
├─────────────────────────────────────────────────────┤
│  NewsCommand.kt                                     │
│  └─ Parses user input                               │
│     └─ Delegates to Repository                       │
├─────────────────────────────────────────────────────┤
│  MiniMaxRepository.kt                               │
│  └─ Manages caching                                 │
│     └─ Calls Service                                │
├─────────────────────────────────────────────────────┤
│  MiniMaxService.kt                                  │
│  └─ HTTP requests to MiniMax API                    │
│     └─ JSON serialization                           │
├─────────────────────────────────────────────────────┤
│  NewsConfig.kt                                      │
│  └─ API key & preferences management                │
└─────────────────────────────────────────────────────┘
```

## API Integration

The module uses MiniMax's Chat Completion API:

- **Endpoint**: `https://api.minimax.chat/v1/text/chatcompletion_v2`
- **Model**: `abab6.5-chat` (or latest available)
- **Response Format**: Plain text optimized for CLI

## Prompt Engineering

The module uses carefully crafted prompts to ensure terminal-friendly output:

- No markdown formatting
- Maximum 50-60 characters per line
- Clean ASCII formatting
- Source attribution

## Error Handling

| Error | Solution |
|-------|----------|
| "Invalid API Key" | Check `secrets.properties` or use `setApiKey()` |
| "Network Error" | Check internet connection |
| "API Quota Exceeded" | Wait or upgrade MiniMax plan |

## Troubleshooting

### Command not recognized
Ensure `NewsCommand` is properly registered in your command registry.

### No output displayed
Check that API key is configured and network is available.

### Text formatting issues
The module automatically strips markdown, but some edge cases may need manual adjustment.

## License

This module is open source and follows T-UI Launcher's license terms.

## Credits

- **T-UI Launcher**: Original project by Andr3as07
- **MiniMax**: AI API provider
- **OkHttp**: HTTP client
- **Gson**: JSON parser
