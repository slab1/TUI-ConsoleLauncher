/**
 * Settings Manager Module for Monaco Editor
 * Handles settings UI, persistence, and synchronization with Android native settings
 */

const SettingsManager = (function() {
    'use strict';

    // Settings state
    let currentSettings = {};
    let isInitialized = false;

    // Default settings
    const defaults = {
        fontSize: 14,
        theme: 'vs-dark',
        wordWrap: 'off',
        minimapEnabled: true,
        tabSize: 4,
        insertSpaces: true,
        lineNumbers: 'on',
        renderWhitespace: false,
        autoSave: false,
        autoSaveDelay: 1000,
        formatOnSave: true,
        lspEnabled: false,
        debugEnabled: false,
        sidebarVisible: true,
        smoothScrolling: false,
        mouseWheelZoom: true,
        defaultEncoding: 'UTF-8'
    };

    // Category definitions
    const categories = [
        { id: 'editor', name: 'Editor', icon: 'edit' },
        { id: 'editorBehavior', name: 'Behavior', icon: 'settings' },
        { id: 'lsp', name: 'Language Server', icon: 'code' },
        { id: 'debug', name: 'Debug', icon: 'bug_report' },
        { id: 'ui', name: 'UI & Layout', icon: 'dashboard' },
        { id: 'files', name: 'Files', icon: 'folder' }
    ];

    // Setting definitions with metadata
    const settingDefs = {
        // Editor preferences
        fontSize: { category: 'editor', type: 'number', min: 8, max: 36, default: 14, label: 'Font Size' },
        theme: { category: 'editor', type: 'select', options: ['vs', 'vs-dark', 'hc-black'], default: 'vs-dark', label: 'Theme' },
        wordWrap: { category: 'editor', type: 'select', options: ['off', 'on', 'wordWrapColumn', 'bounded'], default: 'off', label: 'Word Wrap' },
        minimapEnabled: { category: 'editor', type: 'boolean', default: true, label: 'Enable Minimap' },
        tabSize: { category: 'editor', type: 'number', min: 1, max: 16, default: 4, label: 'Tab Size' },
        insertSpaces: { category: 'editor', type: 'boolean', default: true, label: 'Insert Spaces' },
        lineNumbers: { category: 'editor', type: 'select', options: ['on', 'off', 'relative'], default: 'on', label: 'Line Numbers' },
        renderWhitespace: { category: 'editor', type: 'boolean', default: false, label: 'Render Whitespace' },
        smoothScrolling: { category: 'editor', type: 'boolean', default: false, label: 'Smooth Scrolling' },
        mouseWheelZoom: { category: 'editor', type: 'boolean', default: true, label: 'Mouse Wheel Zoom' },

        // Editor behavior
        autoSave: { category: 'editorBehavior', type: 'boolean', default: false, label: 'Auto Save' },
        autoSaveDelay: { category: 'editorBehavior', type: 'number', min: 100, max: 60000, default: 1000, label: 'Auto Save Delay (ms)' },
        formatOnSave: { category: 'editorBehavior', type: 'boolean', default: true, label: 'Format On Save' },
        trimTrailingWhitespace: { category: 'editorBehavior', type: 'boolean', default: false, label: 'Trim Trailing Whitespace' },

        // LSP settings
        lspEnabled: { category: 'lsp', type: 'boolean', default: false, label: 'Enable LSP' },
        lspServerPath: { category: 'lsp', type: 'string', default: '', label: 'LSP Server Path' },
        lspPythonPath: { category: 'lsp', type: 'string', default: '', label: 'Python Path' },
        lspAutoComplete: { category: 'lsp', type: 'boolean', default: true, label: 'Auto Complete' },
        lspDiagnostics: { category: 'lsp', type: 'boolean', default: true, label: 'Diagnostics' },
        lspHover: { category: 'lsp', type: 'boolean', default: true, label: 'Hover Tooltips' },
        lspDefinition: { category: 'lsp', type: 'boolean', default: true, label: 'Go to Definition' },

        // Debug settings
        debugEnabled: { category: 'debug', type: 'boolean', default: false, label: 'Enable Debugger' },
        breakOnException: { category: 'debug', type: 'boolean', default: true, label: 'Break on Exception' },
        showDebugToolbar: { category: 'debug', type: 'boolean', default: true, label: 'Show Debug Toolbar' },
        maxVariablesPerScope: { category: 'debug', type: 'number', min: 10, max: 500, default: 100, label: 'Max Variables' },

        // UI preferences
        sidebarVisible: { category: 'ui', type: 'boolean', default: true, label: 'Show Sidebar' },
        statusBarVisible: { category: 'ui', type: 'boolean', default: true, label: 'Show Status Bar' },

        // File settings
        defaultEncoding: { category: 'files', type: 'select', options: ['UTF-8', 'UTF-16', 'UTF-32', 'ISO-8859-1'], default: 'UTF-8', label: 'Default Encoding' },
        confirmOnClose: { category: 'files', type: 'boolean', default: true, label: 'Confirm on Close' }
    };

    // ======= Public API =======

    /**
     * Initialize settings manager
     */
    function init(initialSettings) {
        if (initialSettings && typeof initialSettings === 'object') {
            currentSettings = { ...defaults, ...initialSettings };
        } else {
            currentSettings = { ...defaults };
        }

        createSettingsPanel();
        bindEvents();
        isInitialized = true;

        console.log('SettingsManager initialized:', currentSettings);
    }

    /**
     * Get current settings
     */
    function getSettings() {
        return { ...currentSettings };
    }

    /**
     * Get a specific setting value
     */
    function getSetting(key) {
        return currentSettings[key];
    }

    /**
     * Update a setting value
     */
    function setSetting(key, value) {
        if (!settingDefs[key]) {
            console.warn('Unknown setting:', key);
            return false;
        }

        // Validate value based on type
        const def = settingDefs[key];
        value = validateValue(value, def);

        currentSettings[key] = value;

        // Apply setting to editor
        applySetting(key, value);

        // Save to native storage
        saveToNative(key, value);

        // Update UI
        updateSettingUI(key, value);

        return true;
    }

    /**
     * Apply multiple settings at once
     */
    function applySettings(settings) {
        Object.keys(settings).forEach(key => {
            if (settingDefs[key]) {
                setSetting(key, settings[key]);
            }
        });
    }

    /**
     * Reset all settings to defaults
     */
    function resetToDefaults() {
        currentSettings = { ...defaults };

        // Apply defaults to editor
        Object.keys(defaults).forEach(key => {
            applySetting(key, defaults[key]);
        });

        // Save reset to native
        if (window.AndroidEditorSettings) {
            try {
                AndroidEditorSettings.resetToDefaults();
            } catch (e) {
                console.log('Native reset not available');
            }
        }

        // Update all UI controls
        updateAllSettingUI();

        console.log('Settings reset to defaults');
    }

    /**
     * Export settings as JSON string
     */
    function exportSettings() {
        return JSON.stringify(currentSettings, null, 2);
    }

    /**
     * Import settings from JSON string
     */
    function importSettings(jsonString) {
        try {
            const settings = JSON.parse(jsonString);
            applySettings(settings);
            return true;
        } catch (e) {
            console.error('Error importing settings:', e);
            return false;
        }
    }

    /**
     * Open settings panel
     */
    function openPanel() {
        const panel = document.getElementById('settings-panel');
        if (panel) {
            panel.classList.add('visible');
            updateAllSettingUI();
        }
    }

    /**
     * Close settings panel
     */
    function closePanel() {
        const panel = document.getElementById('settings-panel');
        if (panel) {
            panel.classList.remove('visible');
        }
    }

    /**
     * Toggle settings panel
     */
    function togglePanel() {
        const panel = document.getElementById('settings-panel');
        if (panel) {
            panel.classList.toggle('visible');
            if (panel.classList.contains('visible')) {
                updateAllSettingUI();
            }
        }
    }

    // ======= Private Methods =======

    /**
     * Create the settings panel UI
     */
    function createSettingsPanel() {
        // Check if panel already exists
        if (document.getElementById('settings-panel')) return;

        const panel = document.createElement('div');
        panel.id = 'settings-panel';
        panel.className = 'settings-panel';

        panel.innerHTML = `
            <div class="settings-header">
                <h2>Settings</h2>
                <button class="settings-close" onclick="SettingsManager.closePanel()">×</button>
            </div>
            <div class="settings-body">
                <div class="settings-sidebar">
                    ${categories.map(cat => `
                        <button class="settings-category-btn" data-category="${cat.id}">
                            <span class="category-icon">${cat.icon}</span>
                            <span>${cat.name}</span>
                        </button>
                    `).join('')}
                </div>
                <div class="settings-content">
                    ${categories.map(cat => `
                        <div class="settings-category" data-category="${cat.id}">
                            <h3>${cat.name}</h3>
                            <div class="settings-list">
                                ${getSettingsByCategory(cat.id)}
                            </div>
                        </div>
                    `).join('')}
                </div>
            </div>
            <div class="settings-footer">
                <button class="btn btn-secondary" onclick="SettingsManager.resetToDefaults()">Reset to Defaults</button>
                <button class="btn btn-primary" onclick="SettingsManager.closePanel()">Done</button>
            </div>
        `;

        document.body.appendChild(panel);
    }

    /**
     * Get HTML for settings controls by category
     */
    function getSettingsByCategory(categoryId) {
        const settings = Object.entries(settingDefs)
            .filter(([key, def]) => def.category === categoryId);

        return settings.map(([key, def]) => `
            <div class="setting-item" data-setting="${key}">
                <label for="setting-${key}">${def.label}</label>
                ${createSettingControl(key, def, currentSettings[key])}
            </div>
        `).join('');
    }

    /**
     * Create appropriate control HTML for a setting
     */
    function createSettingControl(key, def, value) {
        switch (def.type) {
            case 'boolean':
                return `
                    <label class="toggle-switch">
                        <input type="checkbox" id="setting-${key}" ${value ? 'checked' : ''}
                               onchange="SettingsManager.setSetting('${key}', this.checked)">
                        <span class="toggle-slider"></span>
                    </label>
                `;

            case 'select':
                const options = def.options.map(opt =>
                    `<option value="${opt}" ${value === opt ? 'selected' : ''}>${opt}</option>`
                ).join('');
                return `
                    <select id="setting-${key}" onchange="SettingsManager.setSetting('${key}', this.value)">
                        ${options}
                    </select>
                `;

            case 'number':
                return `
                    <input type="number" id="setting-${key}" value="${value}"
                           min="${def.min}" max="${def.max}"
                           onchange="SettingsManager.setSetting('${key}', parseInt(this.value))">
                `;

            case 'string':
            default:
                return `
                    <input type="text" id="setting-${key}" value="${escapeHtml(String(value))}"
                           onchange="SettingsManager.setSetting('${key}', this.value)">
                `;
        }
    }

    /**
     * Validate value based on setting definition
     */
    function validateValue(value, def) {
        switch (def.type) {
            case 'boolean':
                return Boolean(value);

            case 'number':
                const num = parseFloat(value);
                if (isNaN(num)) return def.default;
                return Math.max(def.min, Math.min(def.max, num));

            case 'select':
                return def.options.includes(value) ? value : def.default;

            default:
                return String(value);
        }
    }

    /**
     * Apply a setting to the editor
     */
    function applySetting(key, value) {
        // Handle editor-specific settings
        if (window.monaco && window.editor) {
            const monacoOptions = getMonacoOptions();
            monacoOptions[key] = value;
            window.editor.updateOptions(monacoOptions);
        }

        // Handle UI settings
        switch (key) {
            case 'sidebarVisible':
                const sidebar = document.querySelector('.sidebar');
                if (sidebar) {
                    sidebar.classList.toggle('collapsed', !value);
                }
                break;

            case 'theme':
                if (window.monaco) {
                    monaco.editor.setTheme(value);
                }
                break;
        }

        // Notify native code
        notifyNativeChange(key, value);
    }

    /**
     * Get Monaco Editor options from current settings
     */
    function getMonacoOptions() {
        return {
            fontSize: currentSettings.fontSize,
            theme: currentSettings.theme,
            wordWrap: currentSettings.wordWrap,
            minimap: { enabled: currentSettings.minimapEnabled },
            tabSize: currentSettings.tabSize,
            insertSpaces: currentSettings.insertSpaces,
            lineNumbers: currentSettings.lineNumbers,
            renderWhitespace: currentSettings.renderWhitespace ? 'selection' : 'none',
            smoothScrolling: currentSettings.smoothScrolling,
            mouseWheelZoom: currentSettings.mouseWheelZoom
        };
    }

    /**
     * Save setting to native Android storage
     */
    function saveToNative(key, value) {
        if (window.AndroidEditorSettings) {
            try {
                const type = settingDefs[key].type;
                const stringValue = String(value);
                AndroidEditorSettings.saveSetting(key, stringValue, type);
            } catch (e) {
                console.log('Could not save to native:', e);
            }
        }
    }

    /**
     * Notify native code of setting change
     */
    function notifyNativeChange(key, value) {
        if (window.AndroidEditorSettings) {
            try {
                AndroidEditorSettings.saveSetting(key, String(value), settingDefs[key].type);
            } catch (e) {
                // Native bridge not available
            }
        }
    }

    /**
     * Update a single setting UI control
     */
    function updateSettingUI(key, value) {
        const input = document.getElementById(`setting-${key}`);
        if (input) {
            if (input.type === 'checkbox') {
                input.checked = Boolean(value);
            } else {
                input.value = value;
            }
        }
    }

    /**
     * Update all setting UI controls
     */
    function updateAllSettingUI() {
        Object.keys(currentSettings).forEach(key => {
            updateSettingUI(key, currentSettings[key]);
        });
    }

    /**
     * Bind event listeners
     */
    function bindEvents() {
        // Category navigation
        document.querySelectorAll('.settings-category-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const category = btn.dataset.category;

                // Update active button
                document.querySelectorAll('.settings-category-btn').forEach(b =>
                    b.classList.remove('active'));
                btn.classList.add('active');

                // Show corresponding category
                document.querySelectorAll('.settings-category').forEach(cat => {
                    cat.classList.toggle('active', cat.dataset.category === category);
                });
            });
        });

        // Set first category as active
        const firstCategory = document.querySelector('.settings-category-btn');
        if (firstCategory) {
            firstCategory.classList.add('active');
            const firstCatEl = document.querySelector('.settings-category');
            if (firstCatEl) firstCatEl.classList.add('active');
        }
    }

    /**
     * Escape HTML for safe rendering
     */
    function escapeHtml(str) {
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    // ======= Android Native Bridge =======

    /**
     * Called from native code to update settings
     */
    function onNativeSettingsChanged(settingsJson) {
        try {
            const settings = JSON.parse(settingsJson);
            applySettings(settings);
        } catch (e) {
            console.error('Error parsing native settings:', e);
        }
    }

    /**
     * Called when settings are saved
     */
    function onSettingSaved(key, success) {
        if (!success) {
            console.warn('Failed to save setting:', key);
        }
    }

    /**
     * Called when error occurs
     */
    function onSettingsError(error) {
        console.error('Settings error:', error);
    }

    // ======= Export Public API =======

    return {
        init,
        getSettings,
        getSetting,
        setSetting,
        applySettings,
        resetToDefaults,
        exportSettings,
        importSettings,
        openPanel,
        closePanel,
        togglePanel,
        // Native bridge callbacks
        onNativeSettingsChanged,
        onSettingSaved,
        onSettingsError
    };
})();

// ======= Global Window Functions =======

/**
 * Initialize settings from native code
 */
window.initSettings = function(settingsJson) {
    try {
        const settings = JSON.parse(settingsJson);
        SettingsManager.init(settings);
    } catch (e) {
        console.error('Error initializing settings:', e);
        SettingsManager.init({});
    }
};

/**
 * Apply Monaco editor options
 */
window.applyMonacoOptions = function(optionsJson) {
    try {
        const options = JSON.parse(optionsJson);
        if (window.editor && window.monaco) {
            window.editor.updateOptions(options);
        }
    } catch (e) {
        console.error('Error applying Monaco options:', e);
    }
};

/**
 * Apply individual settings
 */
window.applyTheme = function(theme) {
    if (window.monaco) {
        monaco.editor.setTheme(theme);
    }
};

window.applyFontSize = function(size) {
    if (window.editor) {
        window.editor.updateOptions({ fontSize: parseInt(size) });
    }
};

window.applyWordWrap = function(mode) {
    if (window.editor) {
        window.editor.updateOptions({ wordWrap: mode });
    }
};

window.applyMinimap = function(enabled) {
    if (window.editor) {
        window.editor.updateOptions({ minimap: { enabled: Boolean(enabled) } });
    }
};

window.toggleSidebar = function(visible) {
    const sidebar = document.querySelector('.sidebar');
    if (sidebar) {
        sidebar.classList.toggle('collapsed', !visible);
    }
};

window.applyAutoSave = function(enabled) {
    // Auto-save implementation
    console.log('Auto-save:', enabled ? 'enabled' : 'disabled');
};

window.applyLspEnabled = function(enabled) {
    // LSP enable/disable implementation
    console.log('LSP:', enabled ? 'enabled' : 'disabled');
};

window.applyDebugEnabled = function(enabled) {
    // Debug enable/disable implementation
    console.log('Debug:', enabled ? 'enabled' : 'disabled');
};

window.applyAllSettings = function(settingsJson) {
    SettingsManager.onNativeSettingsChanged(settingsJson);
};
