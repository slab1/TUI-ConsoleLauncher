# Monaco Editor Settings Management System

## Overview

This document describes the comprehensive settings management system implemented for the Monaco Editor Android integration. The system provides a robust, secure, and thread-safe way to manage editor preferences with bi-directional synchronization between native Android code and the WebView-based editor.

## Architecture

### Component Structure

```
developer/
├── settings/
│   ├── EditorSettings.java          # Data model with all settings fields
│   ├── EditorSettingsManager.java   # Singleton manager for CRUD operations
│   └── MonacoSettingsBridge.java    # JavaScript interface for WebView
├── MonacoEditorController.java      # Updated with settings integration
└── test/
    └── settings/
        └── EditorSettingsTest.java  # Unit tests

assets/
├── css/
│   └── settings.css                 # Settings panel styles
└── js/
    └── settings.js                  # Settings UI JavaScript module
```

### Settings Categories

1. **Editor Preferences**: Font size, theme, word wrap, minimap, tab size, cursor styling
2. **Editor Behavior**: Auto-save, format on save, trim trailing whitespace
3. **LSP Settings**: Language server enablement, server paths, language-specific paths
4. **Debug Settings**: Debugger enablement, breakpoint behavior, variable display limits
5. **UI Preferences**: Sidebar visibility, status bar, smooth scrolling
6. **File Settings**: Default encoding, confirm on close, recent files limit

## Configuration

### Default Values

| Setting | Default | Range |
|---------|---------|-------|
| fontSize | 14 | 8-36 |
| theme | vs-dark | vs, vs-dark, hc-black |
| wordWrap | off | off, on, wordWrapColumn, bounded |
| minimapEnabled | true | boolean |
| tabSize | 4 | 1-16 |
| autoSave | false | boolean |
| lspEnabled | false | boolean |
| debugEnabled | false | boolean |
| sidebarVisible | true | boolean |

### Sensitive Settings

The following settings are stored encrypted using Android's EncryptedSharedPreferences:

- **lsp.token**: Authentication token for LSP servers

## Usage

### Android Native Code

#### Initialization

```java
// In your Activity or Application class
EditorSettingsManager.getInstance().initialize(context);
```

#### Getting Settings

```java
EditorSettingsManager manager = EditorSettingsManager.getInstance();
EditorSettings settings = manager.getSettings();

// Get specific values
int fontSize = settings.getFontSize();
String theme = settings.getTheme();
boolean autoSave = settings.isAutoSave();
```

#### Updating Settings

```java
// Update single setting
manager.saveSetting("fontSize", 16, new SettingsCallback() {
    @Override
    public void onSettingsLoaded(EditorSettings settings) {}
    
    @Override
    public void onSettingsSaved(String key, boolean success) {
        if (success) {
            Log.d(TAG, "Setting saved: " + key);
        }
    }
    
    @Override
    public void onError(String error) {
        Log.e(TAG, "Error: " + error);
    }
});

// Update multiple settings
Map<String, Object> settingsMap = new HashMap<>();
settingsMap.put("fontSize", 18);
settingsMap.put("theme", "vs");
manager.saveSettings(settingsMap, callback);
```

#### Controller Integration

```java
// In MonacoEditorController
public class MonacoEditorController {
    private final EditorSettingsManager settingsManager;
    
    public MonacoEditorController(Activity activity, WebView webView, ExecutorService executor) {
        this.settingsManager = EditorSettingsManager.getInstance();
        settingsManager.initialize(activity);
        
        // Load settings on startup
        loadInitialSettings();
    }
    
    private void loadInitialSettings() {
        settingsManager.loadSettings(new SettingsCallback() {
            @Override
            public void onSettingsLoaded(EditorSettings settings) {
                // Send to WebView
                String settingsJson = settings.toJson().toString();
                sendToWebView("window.initSettings", settingsJson);
            }
        });
    }
}
```

### WebView JavaScript

#### Initialize Settings

```javascript
// Called from native code
window.initSettings = function(settingsJson) {
    const settings = JSON.parse(settingsJson);
    SettingsManager.init(settings);
};
```

#### Using Settings Manager

```javascript
// Get current settings
const settings = SettingsManager.getSettings();
console.log('Font size:', settings.fontSize);

// Update a setting
SettingsManager.setSetting('fontSize', 16);
SettingsManager.setSetting('theme', 'vs-dark');
SettingsManager.setSetting('autoSave', true);

// Open settings panel
SettingsManager.openPanel();

// Reset to defaults
SettingsManager.resetToDefaults();

// Export settings
const json = SettingsManager.exportSettings();

// Import settings
SettingsManager.importSettings(json);
```

#### Adding Settings Button to Toolbar

```html
<button class="settings-toolbar-btn" onclick="SettingsManager.togglePanel()">
    <span>⚙</span> Settings
</button>
```

## Security Features

### Encrypted Storage

Sensitive settings are stored using AndroidX Security library with:

- **Key Encryption**: AES-256-SIV (Deterministic encryption)
- **Value Encryption**: AES-256-GCM (Authenticated encryption)

### Sensitive Keys Management

```java
public static final Set<String> SENSITIVE_KEYS = java.util.Set.of(
    "lsp.token"
);
```

The SettingsManager automatically routes sensitive keys to encrypted storage while non-sensitive keys go to regular SharedPreferences.

## Thread Safety

The SettingsManager uses several techniques for thread safety:

1. **AtomicReference**: For the current settings object
2. **AtomicBoolean**: For initialization state
3. **ConcurrentHashMap**: For listener management
4. **Single-threaded Executor**: For all disk I/O operations
5. **Main Handler**: For UI thread callbacks

## Testing

### Unit Tests

Run the settings tests:

```bash
./gradlew test --tests "*EditorSettingsTest*"
```

### Test Coverage

- Default value validation
- Input range validation (font size, tab size, etc.)
- Builder pattern functionality
- JSON serialization/deserialization
- Settings map conversion
- Sensitive keys verification

## Integration with Monaco Editor

### Applying Settings

```javascript
// In settings.js
function applySetting(key, value) {
    if (window.monaco && window.editor) {
        const options = getMonacoOptions();
        options[key] = value;
        window.editor.updateOptions(options);
    }
}

function getMonacoOptions() {
    return {
        fontSize: currentSettings.fontSize,
        theme: currentSettings.theme,
        wordWrap: currentSettings.wordWrap,
        minimap: { enabled: currentSettings.minimapEnabled },
        tabSize: currentSettings.tabSize,
        // ... more options
    };
}
```

### Theme Application

```javascript
window.applyTheme = function(theme) {
    if (window.monaco) {
        monaco.editor.setTheme(theme);
    }
};
```

## Migration Guide

### From Existing Settings

If migrating from a simple SharedPreferences implementation:

```java
// Old code
SharedPreferences prefs = getSharedPreferences("editor", MODE_PRIVATE);
int fontSize = prefs.getInt("fontSize", 14);

// New code
EditorSettingsManager manager = EditorSettingsManager.getInstance();
EditorSettings settings = manager.getSettings();
int fontSize = settings.getFontSize();
```

### Export/Import

```java
// Export settings
String json = manager.exportSettings();
// json: {"fontSize":14,"theme":"vs-dark",...}

// Import settings
manager.importSettings(json, false, callback); // false = replace all
```

## Performance Considerations

1. **Lazy Initialization**: Settings are loaded on demand
2. **Background I/O**: All disk operations happen on background thread
3. **Cached Settings**: Current settings are cached in memory
4. **Batch Updates**: Multiple settings can be updated together
5. **Listener Pattern**: UI updates only when settings change

## Future Enhancements

Potential improvements for future versions:

1. **Settings Sync**: Cloud sync across devices
2. **Settings Profiles**: Multiple named configuration profiles
3. **Settings Import/Export**: JSON file import/export
4. **Search**: Quick settings search functionality
5. **Keyboard Shortcuts**: Navigate settings with keyboard
6. **Reset Confirmation**: Dialog before resetting to defaults
7. **Settings Backup**: Automatic backup of settings

## Troubleshooting

### Common Issues

**Settings not persisting:**
- Ensure `initialize()` is called during app startup
- Check that `SharedPreferences` is not cleared elsewhere

**Settings not applying to editor:**
- Verify `loadInitialSettings()` is called after WebView is ready
- Check JavaScript console for errors

**Encrypted settings not working:**
- Ensure AndroidX Security library is included
- Check device supports hardware-backed keystore

### Debug Logging

Enable debug logging:

```java
Log.d(TAG, "Loading settings...");
Log.d(TAG, "Saving setting: " + key + " = " + value);
Log.e(TAG, "Error: " + error);
```
