package ohi.andre.consolelauncher.commands.smartlauncher.developer.settings.modules;

import android.content.Context;

import ohi.andre.consolelauncher.commands.smartlauncher.developer.settings.base.BaseSettingsModule;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Settings module for Terminal/Console operations.
 * Manages shell configuration, prompt styling, and command history.
 */
public class TerminalSettings extends BaseSettingsModule<TerminalSettings> {

    public static final String MODULE_ID = "terminal";
    public static final String MODULE_NAME = "Terminal";
    public static final String MODULE_CATEGORY = "System";

    // Cursor styles
    public static final int CURSOR_BLOCK = 0;
    public static final int CURSOR_BAR = 1;
    public static final int CURSOR_UNDERLINE = 2;

    // Setting keys
    public static final String KEY_SHELL_PATH = "shell_path";
    public static final String KEY_PROMPT_FORMAT = "prompt_format";
    public static final String KEY_HISTORY_SIZE = "history_size";
    public static final String KEY_MAX_HISTORY_LINES = "max_history_lines";
    public static final String KEY_CURSOR_STYLE = "cursor_style";
    public static final String KEY_CURSOR_BLINK = "cursor_blink";
    public static final String KEY_FONT_SIZE = "font_size";
    public static final String KEY_FONT_FAMILY = "font_family";
    public static final String KEY_TEXT_COLOR = "text_color";
    public static final String KEY_BACKGROUND_COLOR = "background_color";
    public static final String KEY_LINE_SPACING = "line_spacing";
    public static final String KEY_SCROLL_ON_OUTPUT = "scroll_on_output";
    public static final String KEY_BELL_ENABLED = "bell_enabled";
    public static final String KEY_COPY_ON_SELECT = "copy_on_select";
    public static final String KEY_PASTE_ON_LONG_PRESS = "paste_on_long_press";
    public static final String KEY_ENV_VARIABLES = "env_variables";
    public static final String KEY_INITIAL_COMMAND = "initial_command";
    public static final String KEY_CLOSE_ON_EXIT = "close_on_exit";
    public static final String KEY_SHOW_TOUCH_KEYBOARD = "show_touch_keyboard";
    public static final String KEY_KEYBOARD_HEIGHT = "keyboard_height";

    // Default prompt format templates
    public static final String PROMPT_DEFAULT = "$P$G";
    public static final String PROMPT_SIMPLE = "> ";
    public static final String PROMPT_DETAILED = "[\\u@\\h \\W]\\$ ";
    public static final String PROMPT_MINIMAL = "➜ ";

    // Default shell paths
    public static final String SHELL_SYSTEM = "/system/bin/sh";
    public static final String SHELL_BASH = "/system/bin/bash";
    public static final String SHELL_DASH = "/system/bin/dash";

    public TerminalSettings() {
        super(MODULE_ID, MODULE_NAME, MODULE_CATEGORY);
    }

    @Override
    public Map<String, Object> getDefaults() {
        Map<String, Object> defaults = new HashMap<>();

        // Shell settings
        defaults.put(KEY_SHELL_PATH, SHELL_SYSTEM);
        defaults.put(KEY_PROMPT_FORMAT, PROMPT_DEFAULT);
        defaults.put(KEY_INITIAL_COMMAND, "");

        // History settings
        defaults.put(KEY_HISTORY_SIZE, 1000);
        defaults.put(KEY_MAX_HISTORY_LINES, 5000);

        // Cursor settings
        defaults.put(KEY_CURSOR_STYLE, CURSOR_BLOCK);
        defaults.put(KEY_CURSOR_BLINK, true);

        // Appearance settings
        defaults.put(KEY_FONT_SIZE, 14);
        defaults.put(KEY_FONT_FAMILY, "Monospace");
        defaults.put(KEY_TEXT_COLOR, "#CCCCCC");
        defaults.put(KEY_BACKGROUND_COLOR, "#000000");
        defaults.put(KEY_LINE_SPACING, 1.0f);

        // Behavior settings
        defaults.put(KEY_SCROLL_ON_OUTPUT, true);
        defaults.put(KEY_BELL_ENABLED, true);
        defaults.put(KEY_COPY_ON_SELECT, true);
        defaults.put(KEY_PASTE_ON_LONG_PRESS, true);
        defaults.put(KEY_CLOSE_ON_EXIT, false);

        // Keyboard settings
        defaults.put(KEY_SHOW_TOUCH_KEYBOARD, true);
        defaults.put(KEY_KEYBOARD_HEIGHT, 200);

        // Environment variables (empty by default)
        defaults.put(KEY_ENV_VARIABLES, new HashMap<String, String>());

        return defaults;
    }

    @Override
    public Set<String> getSensitiveKeys() {
        return new java.util.HashSet<>(); // No typically sensitive data
    }

    @Override
    public ValidationResult validate(String key, Object value) {
        switch (key) {
            case KEY_SHELL_PATH:
                String path = String.valueOf(value);
                if (path.isEmpty() || path.startsWith("/")) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Invalid shell path");

            case KEY_PROMPT_FORMAT:
                // Allow any non-empty string
                String format = String.valueOf(value);
                if (!format.isEmpty()) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Prompt format cannot be empty");

            case KEY_HISTORY_SIZE:
            case KEY_MAX_HISTORY_LINES:
                int size = parseInt(value, 1000);
                if (size >= 100 && size <= 100000) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("History size must be 100-100000");

            case KEY_CURSOR_STYLE:
                int style = parseInt(value, CURSOR_BLOCK);
                if (style >= CURSOR_BLOCK && style <= CURSOR_UNDERLINE) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Invalid cursor style");

            case KEY_CURSOR_BLINK:
            case KEY_SCROLL_ON_OUTPUT:
            case KEY_BELL_ENABLED:
            case KEY_COPY_ON_SELECT:
            case KEY_PASTE_ON_LONG_PRESS:
            case KEY_CLOSE_ON_EXIT:
            case KEY_SHOW_TOUCH_KEYBOARD:
                return ValidationResult.success();

            case KEY_FONT_SIZE:
                int fontSize = parseInt(value, 14);
                if (fontSize >= 8 && fontSize <= 32) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Font size must be 8-32");

            case KEY_FONT_FAMILY:
                String font = String.valueOf(value);
                if (!font.isEmpty() && font.length() <= 50) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Invalid font family");

            case KEY_TEXT_COLOR:
            case KEY_BACKGROUND_COLOR:
                String color = String.valueOf(value);
                if (color.matches("^#[0-9A-Fa-f]{6}$")) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Invalid color format (use #RRGGBB)");

            case KEY_LINE_SPACING:
                float spacing = parseFloat(value, 1.0f);
                if (spacing >= 0.8f && spacing <= 2.0f) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Line spacing must be 0.8-2.0");

            case KEY_KEYBOARD_HEIGHT:
                int height = parseInt(value, 200);
                if (height >= 100 && height <= 500) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Keyboard height must be 100-500");

            case KEY_INITIAL_COMMAND:
                String cmd = String.valueOf(value);
                if (cmd.length() <= 500) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Initial command too long");

            case KEY_ENV_VARIABLES:
                // Validate map structure if present
                if (value instanceof Map) {
                    return ValidationResult.success();
                }
                return ValidationResult.success(new HashMap<String, String>());

            default:
                return ValidationResult.success();
        }
    }

    @Override
    public void onSettingChanged(String key, Object value) {
        Log.d(MODULE_ID, "Setting changed: " + key + " = " + value);

        // Apply visual changes immediately
        if (key.equals(KEY_TEXT_COLOR) || key.equals(KEY_BACKGROUND_COLOR) ||
            key.equals(KEY_FONT_SIZE) || key.equals(KEY_FONT_FAMILY)) {
            // Notify terminal view to update
        }
    }

    // Shell configuration

    public String getShellPath() {
        return getString(KEY_SHELL_PATH, SHELL_SYSTEM);
    }

    public void setShellPath(String path) {
        setSetting(KEY_SHELL_PATH, path);
    }

    public String getPromptFormat() {
        return getString(KEY_PROMPT_FORMAT, PROMPT_DEFAULT);
    }

    public void setPromptFormat(String format) {
        setSetting(KEY_PROMPT_FORMAT, format);
    }

    public String getInitialCommand() {
        return getString(KEY_INITIAL_COMMAND, "");
    }

    public void setInitialCommand(String command) {
        setSetting(KEY_INITIAL_COMMAND, command);
    }

    // History settings

    public int getHistorySize() {
        return getInt(KEY_HISTORY_SIZE, 1000);
    }

    public void setHistorySize(int size) {
        setSetting(KEY_HISTORY_SIZE, size);
    }

    public int getMaxHistoryLines() {
        return getInt(KEY_MAX_HISTORY_LINES, 5000);
    }

    public void setMaxHistoryLines(int max) {
        setSetting(KEY_MAX_HISTORY_LINES, max);
    }

    // Cursor settings

    public int getCursorStyle() {
        return getInt(KEY_CURSOR_STYLE, CURSOR_BLOCK);
    }

    public void setCursorStyle(int style) {
        setSetting(KEY_CURSOR_STYLE, style);
    }

    public String getCursorStyleName() {
        switch (getCursorStyle()) {
            case CURSOR_BAR: return "Bar";
            case CURSOR_UNDERLINE: return "Underline";
            default: return "Block";
        }
    }

    public boolean isCursorBlink() {
        return getBoolean(KEY_CURSOR_BLINK, true);
    }

    public void setCursorBlink(boolean blink) {
        setSetting(KEY_CURSOR_BLINK, blink);
    }

    // Appearance settings

    public int getFontSize() {
        return getInt(KEY_FONT_SIZE, 14);
    }

    public void setFontSize(int size) {
        setSetting(KEY_FONT_SIZE, size);
    }

    public String getFontFamily() {
        return getString(KEY_FONT_FAMILY, "Monospace");
    }

    public void setFontFamily(String family) {
        setSetting(KEY_FONT_FAMILY, family);
    }

    public String getTextColor() {
        return getString(KEY_TEXT_COLOR, "#CCCCCC");
    }

    public void setTextColor(String color) {
        setSetting(KEY_TEXT_COLOR, color);
    }

    public String getBackgroundColor() {
        return getString(KEY_BACKGROUND_COLOR, "#000000");
    }

    public void setBackgroundColor(String color) {
        setSetting(KEY_BACKGROUND_COLOR, color);
    }

    public float getLineSpacing() {
        Object value = getSetting(KEY_LINE_SPACING);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return 1.0f;
    }

    public void setLineSpacing(float spacing) {
        setSetting(KEY_LINE_SPACING, spacing);
    }

    // Behavior settings

    public boolean isScrollOnOutput() {
        return getBoolean(KEY_SCROLL_ON_OUTPUT, true);
    }

    public void setScrollOnOutput(boolean scroll) {
        setSetting(KEY_SCROLL_ON_OUTPUT, scroll);
    }

    public boolean isBellEnabled() {
        return getBoolean(KEY_BELL_ENABLED, true);
    }

    public void setBellEnabled(boolean enabled) {
        setSetting(KEY_BELL_ENABLED, enabled);
    }

    public boolean isCopyOnSelect() {
        return getBoolean(KEY_COPY_ON_SELECT, true);
    }

    public void setCopyOnSelect(boolean copy) {
        setSetting(KEY_COPY_ON_SELECT, copy);
    }

    public boolean isPasteOnLongPress() {
        return getBoolean(KEY_PASTE_ON_LONG_PRESS, true);
    }

    public void setPasteOnLongPress(boolean paste) {
        setSetting(KEY_PASTE_ON_LONG_PRESS, paste);
    }

    public boolean isCloseOnExit() {
        return getBoolean(KEY_CLOSE_ON_EXIT, false);
    }

    public void setCloseOnExit(boolean close) {
        setSetting(KEY_CLOSE_ON_EXIT, close);
    }

    // Keyboard settings

    public boolean isShowTouchKeyboard() {
        return getBoolean(KEY_SHOW_TOUCH_KEYBOARD, true);
    }

    public void setShowTouchKeyboard(boolean show) {
        setSetting(KEY_SHOW_TOUCH_KEYBOARD, show);
    }

    public int getKeyboardHeight() {
        return getInt(KEY_KEYBOARD_HEIGHT, 200);
    }

    public void setKeyboardHeight(int height) {
        setSetting(KEY_KEYBOARD_HEIGHT, height);
    }

    // Environment variables

    @SuppressWarnings("unchecked")
    public Map<String, String> getEnvVariables() {
        Object value = getSetting(KEY_ENV_VARIABLES);
        if (value instanceof Map) {
            return new HashMap<>((Map<String, String>) value);
        }
        return new HashMap<>();
    }

    public void setEnvVariables(Map<String, String> env) {
        setSetting(KEY_ENV_VARIABLES, env);
    }

    public void setEnvVariable(String name, String value) {
        Map<String, String> env = getEnvVariables();
        env.put(name, value);
        setEnvVariables(env);
    }

    public String getEnvVariable(String name) {
        return getEnvVariables().get(name);
    }

    // Helper methods

    private int parseInt(Object value, int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private float parseFloat(Object value, float defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        try {
            return Float.parseFloat(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
