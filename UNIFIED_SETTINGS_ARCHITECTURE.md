# Unified Settings Management System

## Overview

This document describes the comprehensive settings management architecture implemented for the T-UI ConsoleLauncher Android application. The system provides a unified, modular, and secure approach to managing settings across all application modules with cross-module coordination and dependency handling.

## Architecture

### Component Structure

```
settings/
├── base/
│   ├── ISettingsModule.java         # Base interface for all modules
│   └── BaseSettingsModule.java      # Abstract base implementation
├── modules/
│   ├── EditorSettings.java          # Monaco Editor settings
│   ├── GitSettings.java             # Git configuration
│   ├── FileManagerSettings.java     # File explorer preferences
│   ├── TerminalSettings.java        # Terminal/Shell settings
│   ├── BuildSettings.java           # Build/Compiler settings
│   └── UiThemeSettings.java         # UI/Theme preferences
├── GlobalSettingsManager.java       # Central coordinator
└── MonacoSettingsBridge.java        # JavaScript interface

assets/js/
├── global-settings.js               # Web UI settings manager
└── settings.js                      # Settings panel UI
```

### Design Principles

The settings architecture follows several key principles that ensure consistency, security, and maintainability across all application modules.

**Modularity** ensures that each module manages its own settings independently while still participating in the global settings ecosystem. This separation of concerns allows teams to work on individual modules without affecting others.

**Security** is built into the foundation with encrypted storage for sensitive data such as authentication tokens, passwords, and signing keys. The system automatically routes sensitive settings to encrypted SharedPreferences while keeping non-sensitive settings in regular storage.

**Thread Safety** is achieved through careful use of atomic references, concurrent data structures, and executor services. All disk I/O operations occur on background threads while callbacks are delivered on the main thread.

**Observability** allows modules to subscribe to settings changes and react accordingly. This enables cross-module coordination where a change in one module can trigger updates in dependent modules.

## Module Specifications

### Editor Settings

The EditorSettings module manages all Monaco Editor configuration including visual appearance, editing behavior, and integration settings.

| Setting | Type | Default | Range | Secure |
|---------|------|---------|-------|--------|
| fontSize | int | 14 | 8-36 | No |
| theme | string | vs-dark | vs, vs-dark, hc-black | No |
| wordWrap | string | off | off, on, wordWrapColumn | No |
| minimapEnabled | boolean | true | - | No |
| tabSize | int | 4 | 1-16 | No |
| autoSave | boolean | false | - | No |
| autoSaveDelay | int | 1000 | 100-60000ms | No |
| lspEnabled | boolean | false | - | No |
| lspServerPath | string | | | No |
| debugEnabled | boolean | false | - | No |
| sidebarVisible | boolean | true | - | No |

### Git Settings

The GitSettings module handles version control configuration including repository settings, author information, and authentication credentials.

| Setting | Type | Default | Validation | Secure |
|---------|------|---------|------------|--------|
| repo_root_path | string | | Valid path | No |
| author_name | string | | Max 100 chars | No |
| author_email | string | | Email format | No |
| default_branch | string | main | Alphanumeric, /_- | No |
| auto_fetch_interval | int | 0 | 0-1440 min | No |
| gpg_signing_enabled | boolean | false | - | No |
| gpg_key_id | string | | | **Yes** |
| auth_token | string | | | **Yes** |
| sign_off_commit | boolean | true | - | No |
| push_default_behavior | string | simple | simple, upstream, current, nothing | No |
| rebase_on_pull | boolean | false | - | No |
| fetch_prune | boolean | true | - | No |

### File Manager Settings

The FileManagerSettings module controls file explorer behavior, display options, and navigation preferences.

| Setting | Type | Default | Range | Description |
|---------|------|---------|-------|-------------|
| default_view_mode | int | 0 (List) | 0-2 | LIST, GRID, TREE |
| sort_order | int | 0 (Name) | 0-7 | Name, Date, Size, Type variants |
| show_hidden_files | boolean | false | - | Show dotfiles |
| show_file_permissions | boolean | true | - | Show chmod-style permissions |
| show_file_size | boolean | true | - | Display file sizes |
| show_file_date | boolean | true | - | Display modification dates |
| root_access_enabled | boolean | false | - | Require root for sensitive operations |
| confirm_delete | boolean | true | - | Confirm before deletion |
| confirm_overwrite | boolean | true | - | Confirm file replacement |
| default_permission | string | 644 | 3 digits | Octal file permissions |
| navigate_to_last_dir | boolean | true | - | Remember last directory |
| show_thumbnails | boolean | true | - | Generate image previews |
| max_recent_folders | int | 10 | 1-100 | Recent folders list size |
| grid_column_count | int | 3 | 1-6 | Columns in grid view |
| double_tap_action | string | open | open, select, menu | Double-tap behavior |

### Terminal Settings

The TerminalSettings module manages shell configuration, appearance, and command history.

| Setting | Type | Default | Range | Description |
|---------|------|---------|-------|-------------|
| shell_path | string | /system/bin/sh | Valid path | Default shell |
| prompt_format | string | $P$G | Any string | Prompt template |
| history_size | int | 1000 | 100-100000 | Lines in history |
| max_history_lines | int | 5000 | 100-100000 | Maximum history entries |
| cursor_style | int | 0 (Block) | 0-2 | Block, Bar, Underline |
| cursor_blink | boolean | true | - | Blinking cursor |
| font_size | int | 14 | 8-32 | Font size in points |
| font_family | string | Monospace | Any font | Font family name |
| text_color | string | #CCCCCC | #RRGGBB | Text color |
| background_color | string | #000000 | #RRGGBB | Background color |
| line_spacing | float | 1.0 | 0.8-2.0 | Line height multiplier |
| scroll_on_output | boolean | true | - | Auto-scroll on output |
| bell_enabled | boolean | true | - | Terminal bell |
| copy_on_select | boolean | true | - | Auto-copy selection |
| paste_on_long_press | boolean | true | - | Long-press paste |
| close_on_exit | boolean | false | - | Close on shell exit |
| show_touch_keyboard | boolean | true | - | Show virtual keyboard |
| keyboard_height | int | 200 | 100-500 | Keyboard height in px |
| initial_command | string | | | Command to run on start |
| env_variables | map | {} | Key-value pairs | Environment variables |

### Build Settings

The BuildSettings module handles compilation preferences, toolchain paths, and build behavior.

| Setting | Type | Default | Validation | Description |
|---------|------|---------|------------|-------------|
| default_output_dir | string | | Valid path | Build output directory |
| compiler_flags_global | string | | Max 2000 chars | Global compiler flags |
| parallel_compilation | boolean | true | - | Parallel make jobs |
| build_type | int | 0 (Debug) | 0-2 | Debug, Release, Custom |
| java_home | string | | Valid path | JDK location |
| android_home | string | | Valid path | Android SDK location |
| ndk_path | string | | Valid path | NDK location |
| gradle_home | string | | Valid path | Gradle installation |
| auto_build_on_save | boolean | false | - | Build on file save |
| show_build_output | boolean | true | - | Display build logs |
| warnings_as_errors | boolean | false | - | Treat warnings as errors |
| debug_symbols | boolean | true | - | Include debug info |
| optimization_level | int | 2 (Standard) | 0-3 | Optimization level |
| clean_before_build | boolean | false | - | Clean before build |
| cache_build_results | boolean | true | - | Use build cache |
| build_timeout | int | 300 | 30-3600s | Build timeout seconds |
| kotlin_daemon | boolean | true | - | Use Kotlin daemon |
| incremental_compilation | boolean | true | - | Incremental builds |
| signing_keystore | string | | .jks/.keystore | Signing keystore path |
| signing_alias | string | | | Key alias name |

### UI Theme Settings

The UiThemeSettings module controls visual appearance, theming, and layout preferences.

| Setting | Type | Default | Range | Description |
|---------|------|---------|-------|-------------|
| theme_mode | int | 2 (Dark) | 0-3 | System, Light, Dark, Black |
| primary_accent_color | string | #007ACC | #RRGGBB | Accent color |
| syntax_theme | string | vs-dark | vs, vs-dark, hc-black | Code editor theme |
| font_family | string | system-ui | Any font | UI font family |
| ui_scale | float | 1.0 | 0.75-1.5 | Scale factor |
| density | int | -1 | 120-640 | Display density (DPI) |
| navigation_style | string | bottom | bottom, side, top, none | Navigation bar style |
| animations_enabled | boolean | true | - | Enable animations |
| haptic_feedback | boolean | true | - | Vibrate on interaction |
| status_bar_style | string | dark | light, dark, transparent | Status bar appearance |
| toolbar_position | string | top | top, bottom, hidden | Toolbar location |
| compact_mode | boolean | false | - | Dense UI layout |
| show_breadcrumbs | boolean | true | - | Show navigation breadcrumb |
| quick_actions_enabled | boolean | true | - | Show quick action buttons |
| background_image | string | | Image path | Custom background |
| custom_css | string | | Max 10000 chars | Custom stylesheet |
| icon_pack | string | default | Any string | Icon theme name |
| rtl_support | boolean | false | - | Right-to-left layout |

## Global Settings Manager

The GlobalSettingsManager serves as the central coordinator for all settings modules, providing unified access and managing cross-module dependencies.

### Key Responsibilities

The manager handles initialization of all registered modules, loading their settings from persistent storage and preparing them for operation. It maintains a registry of all modules and their categories for UI organization.

Cross-module coordination is a critical function where the manager tracks dependencies between modules. When a setting changes in one module, the manager identifies and notifies all dependent modules that may need to react.

The import and export functionality aggregates settings from all modules into a unified JSON structure with versioning information. This enables users to backup and restore their configuration across devices or application reinstalls.

### Usage Example

```java
// Initialize all settings modules
GlobalSettingsManager manager = GlobalSettingsManager.getInstance();
manager.initialize(context);

// Get a specific module
GitSettings gitSettings = manager.getModule(GitSettings.MODULE_ID);

// Update a setting
gitSettings.setAuthorName("John Doe");

// Export all settings
JSONObject export = manager.exportAllSettings();

// Import settings
manager.importSettings(importData, new SettingsImportCallback() {
    @Override
    public void onComplete(boolean success, String error) {
        if (success) {
            // Settings imported successfully
        }
    }
});

// Add global change listener
manager.addGlobalListener(new SettingsChangeListener() {
    @Override
    public void onGlobalSettingsChanged(String moduleId, String key, Object value) {
        Log.d("Settings", "Changed: " + moduleId + "." + key);
    }

    @Override
    public void onSettingsReset() {
        Log.d("Settings", "All settings reset");
    }

    @Override
    public void onSettingsImported(String version) {
        Log.d("Settings", "Imported version: " + version);
    }
});
```

## Cross-Module Dependencies

The settings system automatically coordinates changes across modules based on defined dependencies.

When the UI theme changes, all visual modules including the editor, terminal, and file manager receive notifications to update their appearance accordingly. This ensures consistent theming across the entire application.

Language server configuration changes trigger updates in both the editor and debugger modules, as these depend on LSP functionality for features like autocompletion and symbol resolution.

Terminal settings affect build output display, ensuring that build logs and compiler messages are rendered according to the user's terminal preferences.

Git settings influence file manager behavior, particularly for displaying git status indicators and handling repository-specific operations.

## Security Features

### Encrypted Storage

Sensitive settings are stored using AndroidX Security library with AES-256 encryption for both keys and values. This ensures that authentication tokens, passwords, and signing keys are never stored in plain text.

The system automatically routes settings marked as sensitive to encrypted SharedPreferences while keeping non-sensitive settings in regular storage for performance.

### Secure Export

When exporting settings, sensitive data is automatically excluded from the output JSON. This prevents accidental exposure of credentials in backups or configuration files.

### Validation

All settings undergo validation before storage, preventing injection attacks and ensuring data integrity. The validation rules include type checking, range limits, and format verification.

## Web Integration

The JavaScript settings manager provides equivalent functionality for the WebView-based UI components, maintaining consistency between native and web settings interfaces.

### Native Bridge

The MonacoSettingsBridge class exposes native settings operations to JavaScript, enabling the WebView to read and write settings through a well-defined interface.

```javascript
// Get all settings
const settings = AndroidEditorSettings.getSettings();

// Save a setting
AndroidEditorSettings.saveSetting('fontSize', '16', 'integer');

// Reset to defaults
AndroidEditorSettings.resetToDefaults();

// Export settings
const json = AndroidEditorSettings.exportSettings();
```

### Global Settings API

The global-settings.js module provides a unified interface for all settings operations from JavaScript code.

```javascript
// Initialize with data from native
GlobalSettingsManager.init(settings, version);

// Get a module's settings
const gitSettings = GlobalSettingsManager.getModule('git');

// Update a setting
GlobalSettingsManager.set('editor.fontSize', 18);

// Search settings
const results = GlobalSettingsManager.searchSettings('font');

// Export for backup
const backup = GlobalSettingsManager.exportSettings();
```

## Testing

### Unit Tests

Each settings module includes comprehensive unit tests covering validation, default values, and serialization.

```java
@Test
public void testFontSizeValidation() {
    EditorSettings settings = new EditorSettings.Builder()
        .setFontSize(4)
        .build();
    assertEquals(8, settings.getFontSize()); // Clamped to minimum
}

@Test
public void testSensitiveKeyRouting() {
    // Token should be stored in encrypted storage
    assertTrue(EditorSettings.SENSITIVE_KEYS.contains("lsp.token"));
}
```

### Integration Tests

The GlobalSettingsManager integration tests verify cross-module coordination and dependency handling.

```java
@Test
public void testCrossModuleDependency() {
    // Theme change should notify editor
    uiThemeSettings.setThemeMode(THEME_MODE_LIGHT);
    verify(editorSettings).onSettingChanged(eq("theme"), any());
}
```

## Migration

The settings system supports versioning for smooth migrations between application versions.

### Version Format

Settings versions follow semantic versioning principles with major version breaking changes and minor version introducing new settings.

### Migration Process

On application startup, the GlobalSettingsManager checks the stored settings version against the current version. If versions differ, migration handlers are invoked to transform old settings to the new format.

```java
private void migrateSettings(String oldVersion, String newVersion) {
    if (oldVersion.compareTo("1.1.0") < 0) {
        // Migrate from pre-1.1 format
        migrateFromV1_0();
    }
    if (oldVersion.compareTo("1.2.0") < 0) {
        // Migrate from pre-1.2 format
        migrateFromV1_1();
    }
}
```

## Performance Considerations

### Initialization

Settings are loaded asynchronously on a background thread to avoid blocking the main thread during application startup.

### Caching

Current settings are cached in memory for fast access. The cache is updated only when settings change.

### Batch Operations

Multiple settings can be updated in a single batch operation, minimizing disk I/O and listener notifications.

## Future Enhancements

### Cloud Sync

Future versions may include optional cloud synchronization of settings across devices using the user's Google account.

### Settings Profiles

Users could create and switch between multiple named settings profiles for different use cases or projects.

### Advanced Import/Export

Enhanced import/export with selective inclusion of specific modules or categories, and support for import from various formats.

### Settings Search

A comprehensive search interface allowing users to quickly find and modify specific settings across all modules.
