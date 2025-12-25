/**
 * Global Settings Manager JavaScript Module
 * Unified settings interface for all T-UI ConsoleLauncher modules
 */

const GlobalSettingsManager = (function() {
    'use strict';

    // Settings state
    let settings = {};
    let isInitialized = false;
    let settingsVersion = '1.0.0';

    // Module definitions
    const modules = {
        editor: {
            name: 'Monaco Editor',
            category: 'Editor',
            settings: {}
        },
        git: {
            name: 'Git Configuration',
            category: 'Version Control',
            settings: {}
        },
        file_manager: {
            name: 'File Manager',
            category: 'File Operations',
            settings: {}
        },
        terminal: {
            name: 'Terminal',
            category: 'System',
            settings: {}
        },
        build: {
            name: 'Build Configuration',
            category: 'Development',
            settings: {}
        },
        ui_theme: {
            name: 'Appearance',
            category: 'Interface',
            settings: {}
        },
        lsp: {
            name: 'Language Server',
            category: 'Development',
            settings: {}
        },
        debugger: {
            name: 'Debugger',
            category: 'Development',
            settings: {}
        },
        security: {
            name: 'Security',
            category: 'System',
            settings: {}
        },
        validator: {
            name: 'Input Validator',
            category: 'Security',
            settings: {}
        }
    };

    // Category order for UI
    const categoryOrder = [
        'Interface',
        'Editor',
        'File Operations',
        'Version Control',
        'Development',
        'System',
        'Security'
    ];

    // ======= Public API =======

    /**
     * Initialize settings manager with data from native code
     */
    function init(settingsData, version) {
        if (settingsData && typeof settingsData === 'object') {
            settings = settingsData;
        } else {
            initializeDefaults();
        }

        if (version) {
            settingsVersion = version;
        }

        // Build settings map for each module
        buildModuleSettings();

        isInitialized = true;
        console.log('GlobalSettingsManager initialized:', Object.keys(settings).length, 'settings loaded');
    }

    /**
     * Initialize default settings for all modules
     */
    function initializeDefaults() {
        settings = {};

        // Editor defaults
        Object.assign(settings, getEditorDefaults());

        // Git defaults
        Object.assign(settings, getGitDefaults());

        // File Manager defaults
        Object.assign(settings, getFileManagerDefaults());

        // Terminal defaults
        Object.assign(settings, getTerminalDefaults());

        // Build defaults
        Object.assign(settings, getBuildDefaults());

        // UI Theme defaults
        Object.assign(settings, getUiThemeDefaults());
    }

    /**
     * Get all current settings
     */
    function getAll() {
        return { ...settings };
    }

    /**
     * Get settings for a specific module
     */
    function getModule(moduleId) {
        const moduleSettings = {};
        const module = modules[moduleId];

        if (!module) return null;

        for (const key in settings) {
            if (key.startsWith(moduleId + '.') || key.startsWith(moduleId + '_')) {
                const shortKey = key.replace(/^(editor|git|file_manager|terminal|build|ui_theme|lsp|debugger|security|validator)[._]/, '');
                moduleSettings[shortKey] = settings[key];
            }
        }

        return {
            id: moduleId,
            name: module.name,
            category: module.category,
            settings: moduleSettings
        };
    }

    /**
     * Get all modules grouped by category
     */
    function getModulesByCategory() {
        const grouped = {};

        for (const category of categoryOrder) {
            grouped[category] = [];
        }

        for (const [id, module] of Object.entries(modules)) {
            if (!grouped[module.category]) {
                grouped[module.category] = [];
            }
            grouped[module.category].push({
                id: id,
                name: module.name
            });
        }

        return grouped;
    }

    /**
     * Get a setting value
     */
    function get(key, defaultValue) {
        if (settings.hasOwnProperty(key)) {
            return settings[key];
        }
        return defaultValue !== undefined ? defaultValue : null;
    }

    /**
     * Set a setting value
     */
    function set(key, value) {
        // Validate value before setting
        const validated = validateSetting(key, value);
        if (!validated.valid) {
            console.warn('Invalid setting value for', key + ':', validated.error);
            return false;
        }

        const finalValue = validated.corrected !== undefined ? validated.corrected : value;
        settings[key] = finalValue;

        // Notify native code
        notifyNativeChange(key, finalValue);

        return true;
    }

    /**
     * Get multiple settings at once
     */
    function getMultiple(keys) {
        const result = {};
        for (const key of keys) {
            result[key] = get(key);
        }
        return result;
    }

    /**
     * Set multiple settings at once
     */
    function setMultiple(updates) {
        const success = [];
        const failures = [];

        for (const [key, value] of Object.entries(updates)) {
            if (set(key, value)) {
                success.push(key);
            } else {
                failures.push(key);
            }
        }

        return { success, failures };
    }

    /**
     * Reset a module to defaults
     */
    function resetModule(moduleId) {
        const defaults = getModuleDefaults(moduleId);
        if (defaults) {
            for (const [key, value] of Object.entries(defaults)) {
                settings[key] = value;
            }
            notifyNativeReset(moduleId);
            return true;
        }
        return false;
    }

    /**
     * Reset all settings to defaults
     */
    function resetAll() {
        initializeDefaults();
        notifyNativeReset('all');
    }

    /**
     * Export settings as JSON string
     */
    function exportSettings() {
        const exportData = {
            version: settingsVersion,
            timestamp: Date.now(),
            settings: settings
        };
        return JSON.stringify(exportData, null, 2);
    }

    /**
     * Import settings from JSON string
     */
    function importSettings(jsonString) {
        try {
            const importData = JSON.parse(jsonString);

            // Validate version
            if (importData.version && !isVersionCompatible(importData.version)) {
                console.warn('Incompatible settings version:', importData.version);
                return { success: false, error: 'Incompatible version' };
            }

            // Apply settings
            if (importData.settings) {
                for (const [key, value] of Object.entries(importData.settings)) {
                    set(key, value);
                }
            }

            return { success: true };
        } catch (e) {
            console.error('Error importing settings:', e);
            return { success: false, error: e.message };
        }
    }

    /**
     * Open settings panel
     */
    function openPanel() {
        const panel = document.getElementById('global-settings-panel');
        if (panel) {
            panel.classList.add('visible');
            renderSettingsPanel();
        }
    }

    /**
     * Close settings panel
     */
    function closePanel() {
        const panel = document.getElementById('global-settings-panel');
        if (panel) {
            panel.classList.remove('visible');
        }
    }

    /**
     * Toggle settings panel
     */
    function togglePanel() {
        const panel = document.getElementById('global-settings-panel');
        if (panel) {
            panel.classList.toggle('visible');
            if (panel.classList.contains('visible')) {
                renderSettingsPanel();
            }
        }
    }

    /**
     * Search settings
     */
    function searchSettings(query) {
        const results = [];
        const lowerQuery = query.toLowerCase();

        for (const [key, value] of Object.entries(settings)) {
            const module = getModuleForKey(key);
            const label = getSettingLabel(key);

            if (key.toLowerCase().includes(lowerQuery) ||
                label.toLowerCase().includes(lowerQuery) ||
                module.toLowerCase().includes(lowerQuery)) {
                results.push({
                    key: key,
                    value: value,
                    module: module,
                    label: label
                });
            }
        }

        return results;
    }

    // ======= Private Methods =======

    /**
     * Build module settings map from flat settings
     */
    function buildModuleSettings() {
        // Settings are stored flat, but we organize them by module for UI
    }

    /**
     * Get module name for a setting key
     */
    function getModuleForKey(key) {
        const parts = key.split(/[._]/);
        const moduleId = parts[0];

        if (modules[moduleId]) {
            return modules[moduleId].name;
        }

        return 'General';
    }

    /**
     * Get human-readable label for a setting
     */
    function getSettingLabel(key) {
        const labels = {
            // Editor
            fontSize: 'Font Size',
            theme: 'Theme',
            wordWrap: 'Word Wrap',
            minimapEnabled: 'Show Minimap',
            tabSize: 'Tab Size',
            autoSave: 'Auto Save',
            lspEnabled: 'Enable Language Server',

            // Git
            author_name: 'Author Name',
            author_email: 'Author Email',
            default_branch: 'Default Branch',
            auto_fetch_interval: 'Auto Fetch Interval',

            // File Manager
            default_view_mode: 'View Mode',
            show_hidden_files: 'Show Hidden Files',
            sort_order: 'Sort Order',

            // Terminal
            shell_path: 'Shell Path',
            prompt_format: 'Prompt Format',
            history_size: 'History Size',

            // Build
            default_output_dir: 'Output Directory',
            parallel_compilation: 'Parallel Compilation',

            // UI Theme
            theme_mode: 'Theme',
            primary_accent_color: 'Accent Color',
            ui_scale: 'UI Scale'
        };

        return labels[key] || key.split('.').pop().replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
    }

    /**
     * Validate a setting value
     */
    function validateSetting(key, value) {
        // Type validation
        if (value === null || value === undefined) {
            return { valid: false, error: 'Value cannot be null' };
        }

        // Range validation for numbers
        if (typeof value === 'number') {
            if (key.includes('fontSize') && (value < 8 || value > 36)) {
                return { valid: false, error: 'Font size must be 8-36', corrected: Math.max(8, Math.min(36, value)) };
            }
            if (key.includes('tabSize') && (value < 1 || value > 16)) {
                return { valid: false, error: 'Tab size must be 1-16', corrected: Math.max(1, Math.min(16, value)) };
            }
            if (key.includes('interval') && (value < 0 || value > 1440)) {
                return { valid: false, error: 'Interval must be 0-1440 minutes' };
            }
        }

        // Boolean validation
        if (typeof value === 'boolean') {
            return { valid: true };
        }

        // String validation
        if (typeof value === 'string') {
            if (key.includes('email') && !value.includes('@')) {
                return { valid: false, error: 'Invalid email format' };
            }
            if (key.includes('color') && !value.match(/^#[0-9A-Fa-f]{6}$/)) {
                return { valid: false, error: 'Invalid color format (use #RRGGBB)' };
            }
        }

        return { valid: true };
    }

    /**
     * Notify native code of setting change
     */
    function notifyNativeChange(key, value) {
        if (window.AndroidEditorSettings) {
            try {
                AndroidEditorSettings.saveSetting(key, String(value), typeof value);
            } catch (e) {
                console.log('Native bridge not available:', e);
            }
        }
    }

    /**
     * Notify native code of reset
     */
    function notifyNativeReset(moduleId) {
        if (window.AndroidEditorSettings) {
            try {
                AndroidEditorSettings.resetModule(moduleId);
            } catch (e) {
                console.log('Native bridge not available:', e);
            }
        }
    }

    /**
     * Check version compatibility
     */
    function isVersionCompatible(version) {
        const current = settingsVersion.split('.').map(Number);
        const incoming = version.split('.').map(Number);

        return current[0] === incoming[0]; // Major version must match
    }

    /**
     * Render settings panel UI
     */
    function renderSettingsPanel() {
        // This would be implemented in the settings-panel.js module
        if (typeof renderSettingsPanelUI === 'function') {
            renderSettingsPanelUI();
        }
    }

    // ======= Default Values =======

    function getEditorDefaults() {
        return {
            'editor.fontSize': 14,
            'editor.theme': 'vs-dark',
            'editor.wordWrap': 'off',
            'editor.minimapEnabled': true,
            'editor.tabSize': 4,
            'editor.insertSpaces': true,
            'editor.lineNumbers': 'on',
            'editor.renderWhitespace': false,
            'editor.autoSave': false,
            'editor.autoSaveDelay': 1000,
            'editor.formatOnSave': true,
            'editor.lspEnabled': false,
            'editor.lspServerPath': '',
            'editor.debugEnabled': false,
            'editor.sidebarVisible': true,
            'editor.smoothScrolling': false,
            'editor.mouseWheelZoom': true
        };
    }

    function getGitDefaults() {
        return {
            'git.repo_root_path': '',
            'git.author_name': '',
            'git.author_email': '',
            'git.default_branch': 'main',
            'git.auto_fetch_interval': 0,
            'git.gpg_signing_enabled': false,
            'git.sign_off_commit': true,
            'git.rebase_on_pull': false,
            'git.push_default_behavior': 'simple',
            'git.fetch_prune': true
        };
    }

    function getFileManagerDefaults() {
        return {
            'file_manager.default_view_mode': 0,
            'file_manager.sort_order': 0,
            'file_manager.show_hidden_files': false,
            'file_manager.show_file_permissions': true,
            'file_manager.show_file_size': true,
            'file_manager.show_file_date': true,
            'file_manager.root_access_enabled': false,
            'file_manager.confirm_delete': true,
            'file_manager.confirm_overwrite': true,
            'file_manager.default_permission': '644',
            'file_manager.navigate_to_last_dir': true,
            'file_manager.show_thumbnails': true,
            'file_manager.max_recent_folders': 10,
            'file_manager.grid_column_count': 3
        };
    }

    function getTerminalDefaults() {
        return {
            'terminal.shell_path': '/system/bin/sh',
            'terminal.prompt_format': '$P$G',
            'terminal.history_size': 1000,
            'terminal.cursor_style': 0,
            'terminal.cursor_blink': true,
            'terminal.font_size': 14,
            'terminal.font_family': 'Monospace',
            'terminal.text_color': '#CCCCCC',
            'terminal.background_color': '#000000',
            'terminal.line_spacing': 1.0,
            'terminal.scroll_on_output': true,
            'terminal.bell_enabled': true,
            'terminal.copy_on_select': true,
            'terminal.paste_on_long_press': true,
            'terminal.close_on_exit': false,
            'terminal.show_touch_keyboard': true
        };
    }

    function getBuildDefaults() {
        return {
            'build.default_output_dir': '',
            'build.compiler_flags_global': '',
            'build.parallel_compilation': true,
            'build.build_type': 0,
            'build.java_home': '',
            'build.android_home': '',
            'build.ndk_path': '',
            'build.gradle_home': '',
            'build.auto_build_on_save': false,
            'build.show_build_output': true,
            'build.warnings_as_errors': false,
            'build.debug_symbols': true,
            'build.optimization_level': 2,
            'build.clean_before_build': false,
            'build.cache_build_results': true,
            'build.build_timeout': 300,
            'build.incremental_compilation': true
        };
    }

    function getUiThemeDefaults() {
        return {
            'ui_theme.theme_mode': 2,
            'ui_theme.primary_accent_color': '#007ACC',
            'ui_theme.syntax_theme': 'vs-dark',
            'ui_theme.font_family': 'system-ui',
            'ui_theme.ui_scale': 1.0,
            'ui_theme.density': -1,
            'ui_theme.navigation_style': 'bottom',
            'ui_theme.animations_enabled': true,
            'ui_theme.haptic_feedback': true,
            'ui_theme.status_bar_style': 'dark',
            'ui_theme.toolbar_position': 'top',
            'ui_theme.compact_mode': false,
            'ui_theme.show_breadcrumbs': true,
            'ui_theme.quick_actions_enabled': true,
            'ui_theme.icon_pack': 'default',
            'ui_theme.rtl_support': false
        };
    }

    function getModuleDefaults(moduleId) {
        const defaultsMap = {
            'editor': getEditorDefaults(),
            'git': getGitDefaults(),
            'file_manager': getFileManagerDefaults(),
            'terminal': getTerminalDefaults(),
            'build': getBuildDefaults(),
            'ui_theme': getUiThemeDefaults()
        };

        return defaultsMap[moduleId];
    }

    // ======= Export Public API =======

    return {
        init,
        getAll,
        getModule,
        getModulesByCategory,
        get,
        set,
        getMultiple,
        setMultiple,
        resetModule,
        resetAll,
        exportSettings,
        importSettings,
        openPanel,
        closePanel,
        togglePanel,
        searchSettings,
        isInitialized: () => isInitialized,
        getVersion: () => settingsVersion
    };
})();

// ======= Native Bridge Callbacks =======

/**
 * Called from native code to update settings
 */
window.onNativeSettingsChanged = function(settingsJson) {
    try {
        const data = JSON.parse(settingsJson);
        if (data.settings) {
            GlobalSettingsManager.init(data.settings, data.version);
        }
    } catch (e) {
        console.error('Error parsing native settings:', e);
    }
};

/**
 * Called from native code when a module is reset
 */
window.onModuleReset = function(moduleId) {
    console.log('Module reset:', moduleId);
    GlobalSettingsManager.resetModule(moduleId);
};

/**
 * Called from native code when settings are exported
 */
window.onSettingsExported = function(jsonString) {
    console.log('Settings exported:', jsonString);
    // Handle exported settings (e.g., save to file)
};

/**
 * Initialize global settings from native code
 */
window.initGlobalSettings = function(settingsJson, version) {
    try {
        const settings = JSON.parse(settingsJson);
        GlobalSettingsManager.init(settings, version);
    } catch (e) {
        console.error('Error initializing global settings:', e);
        GlobalSettingsManager.init({}, version);
    }
};

/**
 * Apply theme settings
 */
window.applyThemeSettings = function(themeMode, accentColor) {
    document.body.setAttribute('data-theme', themeMode === 2 ? 'vs-dark' : themeMode === 1 ? 'vs' : 'system');
    if (accentColor) {
        document.documentElement.style.setProperty('--accent-color', accentColor);
    }
};

/**
 * Apply editor settings
 */
window.applyEditorSettings = function(settingsJson) {
    if (window.monaco && window.editor) {
        try {
            const settings = JSON.parse(settingsJson);
            window.editor.updateOptions(settings);
        } catch (e) {
            console.error('Error applying editor settings:', e);
        }
    }
};
