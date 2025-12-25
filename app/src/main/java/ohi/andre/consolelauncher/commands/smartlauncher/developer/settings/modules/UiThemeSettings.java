package ohi.andre.consolelauncher.commands.smartlauncher.developer.settings.modules;

import android.content.Context;
import android.content.res.Configuration;

import ohi.andre.consolelauncher.commands.smartlauncher.developer.settings.base.BaseSettingsModule;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Settings module for UI and Theme configuration.
 * Manages visual appearance, accent colors, and layout preferences.
 */
public class UiThemeSettings extends BaseSettingsModule<UiThemeSettings> {

    public static final String MODULE_ID = "ui_theme";
    public static final String MODULE_NAME = "Appearance";
    public static final String MODULE_CATEGORY = "Interface";

    // Theme modes
    public static final int THEME_MODE_SYSTEM = 0;
    public static final int THEME_MODE_LIGHT = 1;
    public static final int THEME_MODE_DARK = 2;
    public static final int THEME_MODE_BLACK = 3;

    // Accent colors
    public static final String ACCENT_BLUE = "#007ACC";
    public static final String ACCENT_GREEN = "#4EC9B0";
    public static final String ACCENT_PURPLE = "#C586C0";
    public static final String ACCENT_ORANGE = "#CE9178";
    public static final String ACCENT_RED = "#F44747";
    public static final String ACCENT_YELLOW = "#DCDCAA";

    // Setting keys
    public static final String KEY_THEME_MODE = "theme_mode";
    public static final String KEY_PRIMARY_ACCENT_COLOR = "primary_accent_color";
    public static final String KEY_SYNTAX_THEME = "syntax_theme";
    public static final String KEY_FONT_FAMILY = "font_family";
    public static final String KEY_UI_SCALE = "ui_scale";
    public static final String KEY_DENSITY = "density";
    public static final String KEY_NAVIGATION_STYLE = "navigation_style";
    public static final String KEY_ANIMATIONS_ENABLED = "animations_enabled";
    public static final String KEY_HAPTIC_FEEDBACK = "haptic_feedback";
    public static final String KEY_STATUS_BAR_STYLE = "status_bar_style";
    public static final String KEY_TOOLBAR_POSITION = "toolbar_position";
    public static final String KEY_COMPACT_MODE = "compact_mode";
    public static final String KEY_SHOW_BREADCRUMBS = "show_breadcrumbs";
    public static final String KEY_QUICK_ACTIONS_ENABLED = "quick_actions_enabled";
    public static final String KEY_BACKGROUND_IMAGE = "background_image";
    public static final String KEY_CUSTOM_CSS = "custom_css";
    public static final String KEY_ICON_PACK = "icon_pack";
    public static final String KEY_RTL_SUPPORT = "rtl_support";

    public UiThemeSettings() {
        super(MODULE_ID, MODULE_NAME, MODULE_CATEGORY);
    }

    @Override
    public Map<String, Object> getDefaults() {
        Map<String, Object> defaults = new HashMap<>();

        // Theme settings
        defaults.put(KEY_THEME_MODE, THEME_MODE_DARK);
        defaults.put(KEY_PRIMARY_ACCENT_COLOR, ACCENT_BLUE);
        defaults.put(KEY_SYNTAX_THEME, "vs-dark");

        // Typography
        defaults.put(KEY_FONT_FAMILY, "system-ui");
        defaults.put(KEY_UI_SCALE, 1.0f);

        // Display
        defaults.put(KEY_DENSITY, -1); // -1 = default
        defaults.put(KEY_NAVIGATION_STYLE, "bottom");
        defaults.put(KEY_ANIMATIONS_ENABLED, true);
        defaults.put(KEY_HAPTIC_FEEDBACK, true);
        defaults.put(KEY_STATUS_BAR_STYLE, "dark");

        // Layout
        defaults.put(KEY_TOOLBAR_POSITION, "top");
        defaults.put(KEY_COMPACT_MODE, false);
        defaults.put(KEY_SHOW_BREADCRUMBS, true);
        defaults.put(KEY_QUICK_ACTIONS_ENABLED, true);

        // Customization
        defaults.put(KEY_BACKGROUND_IMAGE, "");
        defaults.put(KEY_CUSTOM_CSS, "");
        defaults.put(KEY_ICON_PACK, "default");
        defaults.put(KEY_RTL_SUPPORT, false);

        return defaults;
    }

    @Override
    public Set<String> getSensitiveKeys() {
        return new java.util.HashSet<>(); // No sensitive data
    }

    @Override
    public ValidationResult validate(String key, Object value) {
        switch (key) {
            case KEY_THEME_MODE:
                int mode = parseInt(value, THEME_MODE_DARK);
                if (mode >= THEME_MODE_SYSTEM && mode <= THEME_MODE_BLACK) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Invalid theme mode");

            case KEY_PRIMARY_ACCENT_COLOR:
                String color = String.valueOf(value);
                if (color.matches("^#[0-9A-Fa-f]{6}$")) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Invalid color format (use #RRGGBB)");

            case KEY_SYNTAX_THEME:
                String syntaxTheme = String.valueOf(value);
                if (syntaxTheme.matches("^(vs|vs-dark|hc-black)$")) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Invalid syntax theme");

            case KEY_FONT_FAMILY:
                String font = String.valueOf(value);
                if (!font.isEmpty() && font.length() <= 50) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Invalid font family");

            case KEY_UI_SCALE:
                float scale = parseFloat(value, 1.0f);
                if (scale >= 0.75f && scale <= 1.5f) {
                    return ValidationResult.success(scale);
                }
                return ValidationResult.failure("UI scale must be 0.75-1.5");

            case KEY_DENSITY:
                int density = parseInt(value, -1);
                if (density == -1 || (density >= 120 && density <= 640)) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Invalid density value");

            case KEY_NAVIGATION_STYLE:
                String navStyle = String.valueOf(value);
                if (navStyle.matches("^(bottom|side|top|none)$")) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Invalid navigation style");

            case KEY_STATUS_BAR_STYLE:
                String statusStyle = String.valueOf(value);
                if (statusStyle.matches("^(light|dark|transparent)$")) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Invalid status bar style");

            case KEY_TOOLBAR_POSITION:
                String toolbarPos = String.valueOf(value);
                if (toolbarPos.matches("^(top|bottom|hidden)$")) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Invalid toolbar position");

            case KEY_ANIMATIONS_ENABLED:
            case KEY_HAPTIC_FEEDBACK:
            case KEY_COMPACT_MODE:
            case KEY_SHOW_BREADCRUMBS:
            case KEY_QUICK_ACTIONS_ENABLED:
            case KEY_RTL_SUPPORT:
                return ValidationResult.success();

            case KEY_BACKGROUND_IMAGE:
                String bg = String.valueOf(value);
                if (bg.isEmpty() || bg.matches("^(/|file://).*\\.(png|jpg|jpeg|gif|webp)$")) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Invalid image path");

            case KEY_CUSTOM_CSS:
                String css = String.valueOf(value);
                if (css.length() <= 10000) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Custom CSS too long (max 10000 chars)");

            case KEY_ICON_PACK:
                String iconPack = String.valueOf(value);
                if (iconPack.matches("^[a-zA-Z0-9_-]+$")) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Invalid icon pack name");

            default:
                return ValidationResult.success();
        }
    }

    @Override
    public void onSettingChanged(String key, Object value) {
        Log.d(MODULE_ID, "Setting changed: " + key + " = " + value);

        // Apply theme changes immediately
        if (key.equals(KEY_THEME_MODE) || key.equals(KEY_PRIMARY_ACCENT_COLOR) ||
            key.equals(KEY_UI_SCALE) || key.equals(KEY_FONT_FAMILY)) {
            // Notify all modules to update their appearance
        }
    }

    // Theme settings

    public int getThemeMode() {
        return getInt(KEY_THEME_MODE, THEME_MODE_DARK);
    }

    public void setThemeMode(int mode) {
        setSetting(KEY_THEME_MODE, mode);
    }

    public String getThemeModeName() {
        switch (getThemeMode()) {
            case THEME_MODE_SYSTEM: return "System";
            case THEME_MODE_LIGHT: return "Light";
            case THEME_MODE_BLACK: return "Black (OLED)";
            default: return "Dark";
        }
    }

    public String getPrimaryAccentColor() {
        return getString(KEY_PRIMARY_ACCENT_COLOR, ACCENT_BLUE);
    }

    public void setPrimaryAccentColor(String color) {
        setSetting(KEY_PRIMARY_ACCENT_COLOR, color);
    }

    public String getSyntaxTheme() {
        return getString(KEY_SYNTAX_THEME, "vs-dark");
    }

    public void setSyntaxTheme(String theme) {
        setSetting(KEY_SYNTAX_THEME, theme);
    }

    // Typography

    public String getFontFamily() {
        return getString(KEY_FONT_FAMILY, "system-ui");
    }

    public void setFontFamily(String family) {
        setSetting(KEY_FONT_FAMILY, family);
    }

    public float getUiScale() {
        Object value = getSetting(KEY_UI_SCALE);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return 1.0f;
    }

    public void setUiScale(float scale) {
        setSetting(KEY_UI_SCALE, scale);
    }

    // Display settings

    public int getDensity() {
        return getInt(KEY_DENSITY, -1);
    }

    public void setDensity(int dpi) {
        setSetting(KEY_DENSITY, dpi);
    }

    public String getNavigationStyle() {
        return getString(KEY_NAVIGATION_STYLE, "bottom");
    }

    public void setNavigationStyle(String style) {
        setSetting(KEY_NAVIGATION_STYLE, style);
    }

    public boolean isAnimationsEnabled() {
        return getBoolean(KEY_ANIMATIONS_ENABLED, true);
    }

    public void setAnimationsEnabled(boolean enabled) {
        setSetting(KEY_ANIMATIONS_ENABLED, enabled);
    }

    public boolean isHapticFeedback() {
        return getBoolean(KEY_HAPTIC_FEEDBACK, true);
    }

    public void setHapticFeedback(boolean enabled) {
        setSetting(KEY_HAPTIC_FEEDBACK, enabled);
    }

    public String getStatusBarStyle() {
        return getString(KEY_STATUS_BAR_STYLE, "dark");
    }

    public void setStatusBarStyle(String style) {
        setSetting(KEY_STATUS_BAR_STYLE, style);
    }

    // Layout settings

    public String getToolbarPosition() {
        return getString(KEY_TOOLBAR_POSITION, "top");
    }

    public void setToolbarPosition(String position) {
        setSetting(KEY_TOOLBAR_POSITION, position);
    }

    public boolean isCompactMode() {
        return getBoolean(KEY_COMPACT_MODE, false);
    }

    public void setCompactMode(boolean compact) {
        setSetting(KEY_COMPACT_MODE, compact);
    }

    public boolean isShowBreadcrumbs() {
        return getBoolean(KEY_SHOW_BREADCRUMBS, true);
    }

    public void setShowBreadcrumbs(boolean show) {
        setSetting(KEY_SHOW_BREADCRUMBS, show);
    }

    public boolean isQuickActionsEnabled() {
        return getBoolean(KEY_QUICK_ACTIONS_ENABLED, true);
    }

    public void setQuickActionsEnabled(boolean enabled) {
        setSetting(KEY_QUICK_ACTIONS_ENABLED, enabled);
    }

    // Customization

    public String getBackgroundImage() {
        return getString(KEY_BACKGROUND_IMAGE, "");
    }

    public void setBackgroundImage(String path) {
        setSetting(KEY_BACKGROUND_IMAGE, path);
    }

    public String getCustomCss() {
        return getString(KEY_CUSTOM_CSS, "");
    }

    public void setCustomCss(String css) {
        setSetting(KEY_CUSTOM_CSS, css);
    }

    public String getIconPack() {
        return getString(KEY_ICON_PACK, "default");
    }

    public void setIconPack(String iconPack) {
        setSetting(KEY_ICON_PACK, iconPack);
    }

    public boolean isRtlSupport() {
        return getBoolean(KEY_RTL_SUPPORT, false);
    }

    public void setRtlSupport(boolean enabled) {
        setSetting(KEY_RTL_SUPPORT, enabled);
    }

    // Utility methods

    public boolean isDarkTheme() {
        int mode = getThemeMode();
        return mode == THEME_MODE_DARK || mode == THEME_MODE_BLACK ||
               (mode == THEME_MODE_SYSTEM && (Configuration.UI_MODE_NIGHT_YES ==
                   (contextRef.get() != null ? contextRef.get().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK : 0)));
    }

    public boolean isBlackTheme() {
        return getThemeMode() == THEME_MODE_BLACK;
    }

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
