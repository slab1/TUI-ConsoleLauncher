package ohi.andre.consolelauncher.commands.smartlauncher.developer.settings.modules;

import android.content.Context;
import android.util.Patterns;

import ohi.andre.consolelauncher.commands.smartlauncher.developer.settings.base.BaseSettingsModule;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Settings module for GitCommand operations.
 * Manages repository configuration, author information, and authentication.
 */
public class GitSettings extends BaseSettingsModule<GitSettings> {

    public static final String MODULE_ID = "git";
    public static final String MODULE_NAME = "Git Configuration";
    public static final String MODULE_CATEGORY = "Version Control";

    // Setting keys
    public static final String KEY_REPO_ROOT_PATH = "repo_root_path";
    public static final String KEY_AUTHOR_NAME = "author_name";
    public static final String KEY_AUTHOR_EMAIL = "author_email";
    public static final String KEY_DEFAULT_BRANCH = "default_branch";
    public static final String KEY_AUTO_FETCH_INTERVAL = "auto_fetch_interval";
    public static final String KEY_GPG_SIGNING_ENABLED = "gpg_signing_enabled";
    public static final String KEY_GPG_KEY_ID = "gpg_key_id";
    public static final String KEY_AUTH_TOKEN = "auth_token";
    public static final String KEY_SIGN_OFF_COMMIT = "sign_off_commit";
    public static final String KEY_PUSH_DEFAULT_BEHAVIOR = "push_default_behavior";
    public static final String KEY_REBASE_ON_PULL = "rebase_on_pull";
    public static final String KEY_FETCH_PRUNE = "fetch_prune";
    public static final String KEY_LOG_FORMAT = "log_format";

    public GitSettings() {
        super(MODULE_ID, MODULE_NAME, MODULE_CATEGORY);
        initializeSensitiveKeys();
    }

    private void initializeSensitiveKeys() {
        sensitiveKeys.add(KEY_AUTH_TOKEN);
        sensitiveKeys.add(KEY_GPG_KEY_ID);
    }

    @Override
    public Map<String, Object> getDefaults() {
        Map<String, Object> defaults = new HashMap<>();

        // Repository settings
        defaults.put(KEY_REPO_ROOT_PATH, "");
        defaults.put(KEY_DEFAULT_BRANCH, "main");
        defaults.put(KEY_AUTO_FETCH_INTERVAL, 0); // 0 = disabled
        defaults.put(KEY_FETCH_PRUNE, true);

        // Author settings
        defaults.put(KEY_AUTHOR_NAME, "");
        defaults.put(KEY_AUTHOR_EMAIL, "");

        // Commit settings
        defaults.put(KEY_GPG_SIGNING_ENABLED, false);
        defaults.put(KEY_SIGN_OFF_COMMIT, true);
        defaults.put(KEY_REBASE_ON_PULL, false);
        defaults.put(KEY_PUSH_DEFAULT_BEHAVIOR, "simple");
        defaults.put(KEY_LOG_FORMAT, "oneline");

        return defaults;
    }

    @Override
    public Set<String> getSensitiveKeys() {
        return sensitiveKeys;
    }

    @Override
    public ValidationResult validate(String key, Object value) {
        switch (key) {
            case KEY_REPO_ROOT_PATH:
                String path = String.valueOf(value);
                if (path.isEmpty() || path.startsWith("/")) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Invalid repository path");

            case KEY_AUTHOR_NAME:
                String name = String.valueOf(value);
                if (name.isEmpty() || name.length() <= 100) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Author name too long");

            case KEY_AUTHOR_EMAIL:
                String email = String.valueOf(value);
                if (email.isEmpty() || Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Invalid email format");

            case KEY_DEFAULT_BRANCH:
                String branch = String.valueOf(value);
                if (branch.matches("^[a-zA-Z0-9/_-]+$")) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Invalid branch name");

            case KEY_AUTO_FETCH_INTERVAL:
                int interval = parseInt(value, 0);
                if (interval >= 0 && interval <= 1440) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Interval must be 0-1440 minutes");

            case KEY_GPG_SIGNING_ENABLED:
            case KEY_SIGN_OFF_COMMIT:
            case KEY_FETCH_PRUNE:
            case KEY_REBASE_ON_PULL:
                return ValidationResult.success();

            case KEY_PUSH_DEFAULT_BEHAVIOR:
                String behavior = String.valueOf(value);
                if (behavior.matches("^(simple|upstream|current|nothing)$")) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Invalid push behavior");

            case KEY_LOG_FORMAT:
                String format = String.valueOf(value);
                if (format.matches("^(oneline|short|medium|full|fancier)$")) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Invalid log format");

            default:
                return ValidationResult.success();
        }
    }

    @Override
    public void onSettingChanged(String key, Object value) {
        // Notify listeners or perform module-specific actions
        Log.d(MODULE_ID, "Setting changed: " + key + " = " + value);
    }

    // Convenience methods for common operations

    public String getRepoRootPath() {
        return getString(KEY_REPO_ROOT_PATH, "");
    }

    public void setRepoRootPath(String path) {
        setSetting(KEY_REPO_ROOT_PATH, path);
    }

    public String getAuthorName() {
        return getString(KEY_AUTHOR_NAME, "");
    }

    public void setAuthorName(String name) {
        setSetting(KEY_AUTHOR_NAME, name);
    }

    public String getAuthorEmail() {
        return getString(KEY_AUTHOR_EMAIL, "");
    }

    public void setAuthorEmail(String email) {
        setSetting(KEY_AUTHOR_EMAIL, email);
    }

    public String getDefaultBranch() {
        return getString(KEY_DEFAULT_BRANCH, "main");
    }

    public void setDefaultBranch(String branch) {
        setSetting(KEY_DEFAULT_BRANCH, branch);
    }

    public int getAutoFetchInterval() {
        return getInt(KEY_AUTO_FETCH_INTERVAL, 0);
    }

    public void setAutoFetchInterval(int minutes) {
        setSetting(KEY_AUTO_FETCH_INTERVAL, minutes);
    }

    public boolean isGpgSigningEnabled() {
        return getBoolean(KEY_GPG_SIGNING_ENABLED, false);
    }

    public void setGpgSigningEnabled(boolean enabled) {
        setSetting(KEY_GPG_SIGNING_ENABLED, enabled);
    }

    public boolean isSignOffCommit() {
        return getBoolean(KEY_SIGN_OFF_COMMIT, true);
    }

    public void setSignOffCommit(boolean enabled) {
        setSetting(KEY_SIGN_OFF_COMMIT, enabled);
    }

    public boolean isRebaseOnPull() {
        return getBoolean(KEY_REBASE_ON_PULL, false);
    }

    public void setRebaseOnPull(boolean enabled) {
        setSetting(KEY_REBASE_ON_PULL, enabled);
    }

    public String getPushDefaultBehavior() {
        return getString(KEY_PUSH_DEFAULT_BEHAVIOR, "simple");
    }

    public void setPushDefaultBehavior(String behavior) {
        setSetting(KEY_PUSH_DEFAULT_BEHAVIOR, behavior);
    }

    public boolean isFetchPruneEnabled() {
        return getBoolean(KEY_FETCH_PRUNE, true);
    }

    public void setFetchPruneEnabled(boolean enabled) {
        setSetting(KEY_FETCH_PRUNE, enabled);
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
}
