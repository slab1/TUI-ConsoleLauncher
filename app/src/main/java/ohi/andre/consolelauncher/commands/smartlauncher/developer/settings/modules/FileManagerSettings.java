package ohi.andre.consolelauncher.commands.smartlauncher.developer.settings.modules;

import android.content.Context;

import ohi.andre.consolelauncher.commands.smartlauncher.developer.settings.base.BaseSettingsModule;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Settings module for FileManager operations.
 * Manages file explorer preferences, display options, and navigation behavior.
 */
public class FileManagerSettings extends BaseSettingsModule<FileManagerSettings> {

    public static final String MODULE_ID = "file_manager";
    public static final String MODULE_NAME = "File Manager";
    public static final String MODULE_CATEGORY = "File Operations";

    // View modes
    public static final int VIEW_MODE_LIST = 0;
    public static final int VIEW_MODE_GRID = 1;
    public static final int VIEW_MODE_TREE = 2;

    // Sort orders
    public static final int SORT_NAME_ASC = 0;
    public static final int SORT_NAME_DESC = 1;
    public static final int SORT_DATE_ASC = 2;
    public static final int SORT_DATE_DESC = 3;
    public static final int SORT_SIZE_ASC = 4;
    public static final int SORT_SIZE_DESC = 5;
    public static final int SORT_TYPE_ASC = 6;
    public static final int SORT_TYPE_DESC = 7;

    // Setting keys
    public static final String KEY_DEFAULT_VIEW_MODE = "default_view_mode";
    public static final String KEY_SORT_ORDER = "sort_order";
    public static final String KEY_SHOW_HIDDEN_FILES = "show_hidden_files";
    public static final String KEY_SHOW_FILE_PERMISSIONS = "show_file_permissions";
    public static final String KEY_SHOW_FILE_SIZE = "show_file_size";
    public static final String KEY_SHOW_FILE_DATE = "show_file_date";
    public static final String KEY_ROOT_ACCESS_ENABLED = "root_access_enabled";
    public static final String KEY_CONFIRM_DELETE = "confirm_delete";
    public static final String KEY_CONFIRM_OVERWRITE = "confirm_overwrite";
    public static final String KEY_DEFAULT_PERMISSION = "default_permission";
    public static final String KEY_NAVIGATE_TO_LAST_DIR = "navigate_to_last_dir";
    public static final String KEY_SHOW_THUMBNAILS = "show_thumbnails";
    public static final String KEY_PREVIEW_FILE_SIZE = "preview_file_size";
    public static final String KEY_MAX_RECENT_FOLDERS = "max_recent_folders";
    public static final String KEY_DOUBLE_TAP_ACTION = "double_tap_action";
    public static final String KEY_GRID_COLUMN_COUNT = "grid_column_count";

    public FileManagerSettings() {
        super(MODULE_ID, MODULE_NAME, MODULE_CATEGORY);
    }

    @Override
    public Map<String, Object> getDefaults() {
        Map<String, Object> defaults = new HashMap<>();

        // View settings
        defaults.put(KEY_DEFAULT_VIEW_MODE, VIEW_MODE_LIST);
        defaults.put(KEY_SORT_ORDER, SORT_NAME_ASC);
        defaults.put(KEY_SHOW_HIDDEN_FILES, false);
        defaults.put(KEY_SHOW_FILE_PERMISSIONS, true);
        defaults.put(KEY_SHOW_FILE_SIZE, true);
        defaults.put(KEY_SHOW_FILE_DATE, true);
        defaults.put(KEY_SHOW_THUMBNAILS, true);

        // Behavior settings
        defaults.put(KEY_ROOT_ACCESS_ENABLED, false);
        defaults.put(KEY_CONFIRM_DELETE, true);
        defaults.put(KEY_CONFIRM_OVERWRITE, true);
        defaults.put(KEY_DEFAULT_PERMISSION, "644");
        defaults.put(KEY_NAVIGATE_TO_LAST_DIR, true);
        defaults.put(KEY_MAX_RECENT_FOLDERS, 10);
        defaults.put(KEY_DOUBLE_TAP_ACTION, "open");

        // Grid settings
        defaults.put(KEY_GRID_COLUMN_COUNT, 3);
        defaults.put(KEY_PREVIEW_FILE_SIZE, 10485760); // 10MB

        return defaults;
    }

    @Override
    public Set<String> getSensitiveKeys() {
        return new java.util.HashSet<>(); // No sensitive data in file manager
    }

    @Override
    public ValidationResult validate(String key, Object value) {
        switch (key) {
            case KEY_DEFAULT_VIEW_MODE:
                int viewMode = parseInt(value, VIEW_MODE_LIST);
                if (viewMode >= VIEW_MODE_LIST && viewMode <= VIEW_MODE_TREE) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Invalid view mode");

            case KEY_SORT_ORDER:
                int sortOrder = parseInt(value, SORT_NAME_ASC);
                if (sortOrder >= SORT_NAME_ASC && sortOrder <= SORT_TYPE_DESC) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Invalid sort order");

            case KEY_SHOW_HIDDEN_FILES:
            case KEY_SHOW_FILE_PERMISSIONS:
            case KEY_SHOW_FILE_SIZE:
            case KEY_SHOW_FILE_DATE:
            case KEY_SHOW_THUMBNAILS:
            case KEY_ROOT_ACCESS_ENABLED:
            case KEY_CONFIRM_DELETE:
            case KEY_CONFIRM_OVERWRITE:
            case KEY_NAVIGATE_TO_LAST_DIR:
                return ValidationResult.success();

            case KEY_DEFAULT_PERMISSION:
                String permission = String.valueOf(value);
                if (permission.matches("^[0-7]{3}$")) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Invalid permission format (must be 3 digits)");

            case KEY_MAX_RECENT_FOLDERS:
                int maxRecent = parseInt(value, 10);
                if (maxRecent >= 1 && maxRecent <= 100) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Max recent folders must be 1-100");

            case KEY_DOUBLE_TAP_ACTION:
                String action = String.valueOf(value);
                if (action.matches("^(open|select|menu)$")) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Invalid double tap action");

            case KEY_GRID_COLUMN_COUNT:
                int columns = parseInt(value, 3);
                if (columns >= 1 && columns <= 6) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Grid columns must be 1-6");

            case KEY_PREVIEW_FILE_SIZE:
                long previewSize = parseLong(value, 10485760L);
                if (previewSize >= 0 && previewSize <= 104857600) { // 0-100MB
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Preview size must be 0-100MB");

            default:
                return ValidationResult.success();
        }
    }

    @Override
    public void onSettingChanged(String key, Object value) {
        Log.d(MODULE_ID, "Setting changed: " + key + " = " + value);
        // Notify file explorer to refresh if needed
    }

    // View mode getters/setters

    public int getDefaultViewMode() {
        return getInt(KEY_DEFAULT_VIEW_MODE, VIEW_MODE_LIST);
    }

    public void setDefaultViewMode(int mode) {
        setSetting(KEY_DEFAULT_VIEW_MODE, mode);
    }

    public String getDefaultViewModeName() {
        switch (getDefaultViewMode()) {
            case VIEW_MODE_GRID: return "Grid";
            case VIEW_MODE_TREE: return "Tree";
            default: return "List";
        }
    }

    // Sort order getters/setters

    public int getSortOrder() {
        return getInt(KEY_SORT_ORDER, SORT_NAME_ASC);
    }

    public void setSortOrder(int order) {
        setSetting(KEY_SORT_ORDER, order);
    }

    public String getSortOrderName() {
        switch (getSortOrder()) {
            case SORT_NAME_DESC: return "Name (Z-A)";
            case SORT_DATE_ASC: return "Date (Oldest)";
            case SORT_DATE_DESC: return "Date (Newest)";
            case SORT_SIZE_ASC: return "Size (Smallest)";
            case SORT_SIZE_DESC: return "Size (Largest)";
            case SORT_TYPE_ASC: return "Type (A-Z)";
            case SORT_TYPE_DESC: return "Type (Z-A)";
            default: return "Name (A-Z)";
        }
    }

    // Display options

    public boolean isShowHiddenFiles() {
        return getBoolean(KEY_SHOW_HIDDEN_FILES, false);
    }

    public void setShowHiddenFiles(boolean show) {
        setSetting(KEY_SHOW_HIDDEN_FILES, show);
    }

    public boolean isShowFilePermissions() {
        return getBoolean(KEY_SHOW_FILE_PERMISSIONS, true);
    }

    public void setShowFilePermissions(boolean show) {
        setSetting(KEY_SHOW_FILE_PERMISSIONS, show);
    }

    public boolean isShowFileSize() {
        return getBoolean(KEY_SHOW_FILE_SIZE, true);
    }

    public void setShowFileSize(boolean show) {
        setSetting(KEY_SHOW_FILE_SIZE, show);
    }

    public boolean isShowFileDate() {
        return getBoolean(KEY_SHOW_FILE_DATE, true);
    }

    public void setShowFileDate(boolean show) {
        setSetting(KEY_SHOW_FILE_DATE, show);
    }

    public boolean isShowThumbnails() {
        return getBoolean(KEY_SHOW_THUMBNAILS, true);
    }

    public void setShowThumbnails(boolean show) {
        setSetting(KEY_SHOW_THUMBNAILS, show);
    }

    // Behavior options

    public boolean isRootAccessEnabled() {
        return getBoolean(KEY_ROOT_ACCESS_ENABLED, false);
    }

    public void setRootAccessEnabled(boolean enabled) {
        setSetting(KEY_ROOT_ACCESS_ENABLED, enabled);
    }

    public boolean isConfirmDelete() {
        return getBoolean(KEY_CONFIRM_DELETE, true);
    }

    public void setConfirmDelete(boolean confirm) {
        setSetting(KEY_CONFIRM_DELETE, confirm);
    }

    public boolean isConfirmOverwrite() {
        return getBoolean(KEY_CONFIRM_OVERWRITE, true);
    }

    public void setConfirmOverwrite(boolean confirm) {
        setSetting(KEY_CONFIRM_OVERWRITE, confirm);
    }

    public String getDefaultPermission() {
        return getString(KEY_DEFAULT_PERMISSION, "644");
    }

    public void setDefaultPermission(String permission) {
        setSetting(KEY_DEFAULT_PERMISSION, permission);
    }

    public boolean isNavigateToLastDir() {
        return getBoolean(KEY_NAVIGATE_TO_LAST_DIR, true);
    }

    public void setNavigateToLastDir(boolean navigate) {
        setSetting(KEY_NAVIGATE_TO_LAST_DIR, navigate);
    }

    public int getMaxRecentFolders() {
        return getInt(KEY_MAX_RECENT_FOLDERS, 10);
    }

    public void setMaxRecentFolders(int max) {
        setSetting(KEY_MAX_RECENT_FOLDERS, max);
    }

    public String getDoubleTapAction() {
        return getString(KEY_DOUBLE_TAP_ACTION, "open");
    }

    public void setDoubleTapAction(String action) {
        setSetting(KEY_DOUBLE_TAP_ACTION, action);
    }

    public int getGridColumnCount() {
        return getInt(KEY_GRID_COLUMN_COUNT, 3);
    }

    public void setGridColumnCount(int count) {
        setSetting(KEY_GRID_COLUMN_COUNT, count);
    }

    public long getPreviewFileSize() {
        return getInt(KEY_PREVIEW_FILE_SIZE, 10485760);
    }

    public void setPreviewFileSize(long size) {
        setSetting(KEY_PREVIEW_FILE_SIZE, size);
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

    private long parseLong(Object value, long defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
