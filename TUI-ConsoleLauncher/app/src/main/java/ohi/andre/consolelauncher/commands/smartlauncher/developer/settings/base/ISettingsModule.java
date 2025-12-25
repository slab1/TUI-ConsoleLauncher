package ohi.andre.consolelauncher.commands.smartlauncher.developer.settings.base;

import android.content.Context;

import org.json.JSONObject;

import java.util.Map;
import java.util.Set;

/**
 * Base interface for all settings modules in the application.
 * Every module that manages settings should implement this interface.
 */
public interface ISettingsModule {

    /**
     * Get the unique module identifier
     */
    String getModuleId();

    /**
     * Get the human-readable module name
     */
    String getModuleName();

    /**
     * Get the category for UI organization
     */
    String getModuleCategory();

    /**
     * Get all default values for this module
     */
    Map<String, Object> getDefaults();

    /**
     * Get the set of sensitive keys that require encrypted storage
     */
    Set<String> getSensitiveKeys();

    /**
     * Validate a setting value before saving
     * @param key The setting key
     * @param value The value to validate
     * @return ValidationResult indicating success/failure and any error message
     */
    ValidationResult validate(String key, Object value);

    /**
     * Called when settings are imported from JSON
     */
    void onImport(JSONObject data);

    /**
     * Called when settings are exported to JSON
     * @param includeSensitive Whether to include sensitive data (should be false for export)
     */
    JSONObject onExport(boolean includeSensitive);

    /**
     * Called when the module should apply settings changes
     * @param key The setting key that changed
     * @param value The new value
     */
    void onSettingChanged(String key, Object value);

    /**
     * Reset all settings in this module to defaults
     */
    void resetToDefaults();

    /**
     * Get current settings as a map
     */
    Map<String, Object> getCurrentSettings();

    /**
     * Check if the module is initialized
     */
    boolean isInitialized();

    /**
     * Initialize the module with context
     */
    void initialize(Context context);

    /**
     * Cleanup resources when module is destroyed
     */
    void cleanup();

    /**
     * Validation result container
     */
    class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final Object correctedValue;

        public ValidationResult(boolean valid) {
            this(valid, null, null);
        }

        public ValidationResult(boolean valid, String errorMessage) {
            this(valid, errorMessage, null);
        }

        public ValidationResult(boolean valid, String errorMessage, Object correctedValue) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.correctedValue = correctedValue;
        }

        public static ValidationResult success() {
            return new ValidationResult(true);
        }

        public static ValidationResult success(Object correctedValue) {
            return new ValidationResult(true, null, correctedValue);
        }

        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public Object getCorrectedValue() {
            return correctedValue;
        }

        public boolean hasCorrection() {
            return correctedValue != null;
        }
    }
}
