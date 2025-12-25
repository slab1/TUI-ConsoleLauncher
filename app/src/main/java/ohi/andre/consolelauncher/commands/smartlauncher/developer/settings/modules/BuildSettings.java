package ohi.andre.consolelauncher.commands.smartlauncher.developer.settings.modules;

import android.content.Context;

import ohi.andre.consolelauncher.commands.smartlauncher.developer.settings.base.BaseSettingsModule;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Settings module for Build/Compiler operations.
 * Manages compilation settings, toolchain paths, and build preferences.
 */
public class BuildSettings extends BaseSettingsModule<BuildSettings> {

    public static final String MODULE_ID = "build";
    public static final String MODULE_NAME = "Build Configuration";
    public static final String MODULE_CATEGORY = "Development";

    // Build types
    public static final int BUILD_TYPE_DEBUG = 0;
    public static final int BUILD_TYPE_RELEASE = 1;
    public static final int BUILD_TYPE_CUSTOM = 2;

    // Setting keys
    public static final String KEY_DEFAULT_OUTPUT_DIR = "default_output_dir";
    public static final String KEY_COMPILER_FLAGS_GLOBAL = "compiler_flags_global";
    public static final String KEY_PARALLEL_COMPILATION = "parallel_compilation";
    public static final String KEY_BUILD_TYPE = "build_type";
    public static final String KEY_JAVA_HOME = "java_home";
    public static final String KEY_ANDROID_HOME = "android_home";
    public static final String KEY_NDK_PATH = "ndk_path";
    public static final String KEY_GRADLE_HOME = "gradle_home";
    public static final String KEY_AUTO_BUILD_ON_SAVE = "auto_build_on_save";
    public static final String KEY_SHOW_BUILD_OUTPUT = "show_build_output";
    public static final String KEY_WARNINGS_AS_ERRORS = "warnings_as_errors";
    public static final String KEY_DEBUG_SYMBOLS = "debug_symbols";
    public static final String KEY_OPTIMIZATION_LEVEL = "optimization_level";
    public static final String KEY_CLEAN_BEFORE_BUILD = "clean_before_build";
    public static final String KEY_CACHE_BUILD_RESULTS = "cache_build_results";
    public static final String KEY_BUILD_TIMEOUT = "build_timeout";
    public static final String KEY_SIGNING_KEYSTORE = "signing_keystore";
    public static final String KEY_SIGNING_ALIAS = "signing_alias";
    public static final String KEY_KOTLIN_DAEMON = "kotlin_daemon";
    public static final String KEY_INCREMENTAL_COMPILATION = "incremental_compilation";

    // Optimization levels
    public static final int OPT_NONE = 0;
    public static final int OPT_BASIC = 1;
    public static final int OPT_STANDARD = 2;
    public static final int OPT_AGGRESSIVE = 3;

    public BuildSettings() {
        super(MODULE_ID, MODULE_NAME, MODULE_CATEGORY);
    }

    @Override
    public Map<String, Object> getDefaults() {
        Map<String, Object> defaults = new HashMap<>();

        // Paths
        defaults.put(KEY_DEFAULT_OUTPUT_DIR, "");
        defaults.put(KEY_JAVA_HOME, "");
        defaults.put(KEY_ANDROID_HOME, "");
        defaults.put(KEY_NDK_PATH, "");
        defaults.put(KEY_GRADLE_HOME, "");

        // Compiler settings
        defaults.put(KEY_COMPILER_FLAGS_GLOBAL, "");
        defaults.put(KEY_PARALLEL_COMPILATION, true);
        defaults.put(KEY_BUILD_TYPE, BUILD_TYPE_DEBUG);
        defaults.put(KEY_INCREMENTAL_COMPILATION, true);

        // Output settings
        defaults.put(KEY_AUTO_BUILD_ON_SAVE, false);
        defaults.put(KEY_SHOW_BUILD_OUTPUT, true);
        defaults.put(KEY_WARNINGS_AS_ERRORS, false);
        defaults.put(KEY_DEBUG_SYMBOLS, true);
        defaults.put(KEY_OPTIMIZATION_LEVEL, OPT_STANDARD);
        defaults.put(KEY_CLEAN_BEFORE_BUILD, false);
        defaults.put(KEY_CACHE_BUILD_RESULTS, true);

        // Build behavior
        defaults.put(KEY_BUILD_TIMEOUT, 300); // 5 minutes
        defaults.put(KEY_KOTLIN_DAEMON, true);

        // Signing (paths only, credentials stored securely)
        defaults.put(KEY_SIGNING_KEYSTORE, "");
        defaults.put(KEY_SIGNING_ALIAS, "");

        return defaults;
    }

    @Override
    public Set<String> getSensitiveKeys() {
        Set<String> sensitive = new java.util.HashSet<>();
        // Add sensitive signing credentials
        sensitive.add("signing_keystore_password");
        sensitive.add("signing_key_password");
        return sensitive;
    }

    @Override
    public ValidationResult validate(String key, Object value) {
        switch (key) {
            case KEY_DEFAULT_OUTPUT_DIR:
            case KEY_JAVA_HOME:
            case KEY_ANDROID_HOME:
            case KEY_NDK_PATH:
            case KEY_GRADLE_HOME:
                String path = String.valueOf(value);
                if (path.isEmpty() || path.startsWith("/")) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Invalid path");

            case KEY_COMPILER_FLAGS_GLOBAL:
                String flags = String.valueOf(value);
                if (flags.length() <= 2000) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Compiler flags too long");

            case KEY_PARALLEL_COMPILATION:
            case KEY_AUTO_BUILD_ON_SAVE:
            case KEY_SHOW_BUILD_OUTPUT:
            case KEY_WARNINGS_AS_ERRORS:
            case KEY_DEBUG_SYMBOLS:
            case KEY_CLEAN_BEFORE_BUILD:
            case KEY_KOTLIN_DAEMON:
            case KEY_INCREMENTAL_COMPILATION:
                return ValidationResult.success();

            case KEY_BUILD_TYPE:
                int buildType = parseInt(value, BUILD_TYPE_DEBUG);
                if (buildType >= BUILD_TYPE_DEBUG && buildType <= BUILD_TYPE_CUSTOM) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Invalid build type");

            case KEY_OPTIMIZATION_LEVEL:
                int optLevel = parseInt(value, OPT_STANDARD);
                if (optLevel >= OPT_NONE && optLevel <= OPT_AGGRESSIVE) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Invalid optimization level");

            case KEY_BUILD_TIMEOUT:
                int timeout = parseInt(value, 300);
                if (timeout >= 30 && timeout <= 3600) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Build timeout must be 30-3600 seconds");

            case KEY_SIGNING_KEYSTORE:
                String keystore = String.valueOf(value);
                if (keystore.isEmpty() || keystore.endsWith(".jks") || keystore.endsWith(".keystore")) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Keystore must be .jks or .keystore file");

            case KEY_SIGNING_ALIAS:
                String alias = String.valueOf(value);
                if (alias.isEmpty() || alias.length() <= 100) {
                    return ValidationResult.success();
                }
                return ValidationResult.failure("Invalid alias name");

            default:
                return ValidationResult.success();
        }
    }

    @Override
    public void onSettingChanged(String key, Object value) {
        Log.d(MODULE_ID, "Setting changed: " + key + " = " + value);

        // Notify build system of path changes
        if (key.equals(KEY_JAVA_HOME) || key.equals(KEY_ANDROID_HOME) ||
            key.equals(KEY_NDK_PATH) || key.equals(KEY_GRADLE_HOME)) {
            // Refresh build environment
        }
    }

    // Path configuration

    public String getDefaultOutputDir() {
        return getString(KEY_DEFAULT_OUTPUT_DIR, "");
    }

    public void setDefaultOutputDir(String path) {
        setSetting(KEY_DEFAULT_OUTPUT_DIR, path);
    }

    public String getJavaHome() {
        return getString(KEY_JAVA_HOME, "");
    }

    public void setJavaHome(String path) {
        setSetting(KEY_JAVA_HOME, path);
    }

    public String getAndroidHome() {
        return getString(KEY_ANDROID_HOME, "");
    }

    public void setAndroidHome(String path) {
        setSetting(KEY_ANDROID_HOME, path);
    }

    public String getNdkPath() {
        return getString(KEY_NDK_PATH, "");
    }

    public void setNdkPath(String path) {
        setSetting(KEY_NDK_PATH, path);
    }

    public String getGradleHome() {
        return getString(KEY_GRADLE_HOME, "");
    }

    public void setGradleHome(String path) {
        setSetting(KEY_GRADLE_HOME, path);
    }

    // Compiler settings

    public String getCompilerFlagsGlobal() {
        return getString(KEY_COMPILER_FLAGS_GLOBAL, "");
    }

    public void setCompilerFlagsGlobal(String flags) {
        setSetting(KEY_COMPILER_FLAGS_GLOBAL, flags);
    }

    public boolean isParallelCompilation() {
        return getBoolean(KEY_PARALLEL_COMPILATION, true);
    }

    public void setParallelCompilation(boolean enabled) {
        setSetting(KEY_PARALLEL_COMPILATION, enabled);
    }

    public int getBuildType() {
        return getInt(KEY_BUILD_TYPE, BUILD_TYPE_DEBUG);
    }

    public void setBuildType(int type) {
        setSetting(KEY_BUILD_TYPE, type);
    }

    public String getBuildTypeName() {
        switch (getBuildType()) {
            case BUILD_TYPE_RELEASE: return "Release";
            case BUILD_TYPE_CUSTOM: return "Custom";
            default: return "Debug";
        }
    }

    public boolean isIncrementalCompilation() {
        return getBoolean(KEY_INCREMENTAL_COMPILATION, true);
    }

    public void setIncrementalCompilation(boolean enabled) {
        setSetting(KEY_INCREMENTAL_COMPILATION, enabled);
    }

    // Output settings

    public boolean isAutoBuildOnSave() {
        return getBoolean(KEY_AUTO_BUILD_ON_SAVE, false);
    }

    public void setAutoBuildOnSave(boolean auto) {
        setSetting(KEY_AUTO_BUILD_ON_SAVE, auto);
    }

    public boolean isShowBuildOutput() {
        return getBoolean(KEY_SHOW_BUILD_OUTPUT, true);
    }

    public void setShowBuildOutput(boolean show) {
        setSetting(KEY_SHOW_BUILD_OUTPUT, show);
    }

    public boolean isWarningsAsErrors() {
        return getBoolean(KEY_WARNINGS_AS_ERRORS, false);
    }

    public void setWarningsAsErrors(boolean errors) {
        setSetting(KEY_WARNINGS_AS_ERRORS, errors);
    }

    public boolean isDebugSymbols() {
        return getBoolean(KEY_DEBUG_SYMBOLS, true);
    }

    public void setDebugSymbols(boolean symbols) {
        setSetting(KEY_DEBUG_SYMBOLS, symbols);
    }

    public int getOptimizationLevel() {
        return getInt(KEY_OPTIMIZATION_LEVEL, OPT_STANDARD);
    }

    public void setOptimizationLevel(int level) {
        setSetting(KEY_OPTIMIZATION_LEVEL, level);
    }

    public String getOptimizationLevelName() {
        switch (getOptimizationLevel()) {
            case OPT_NONE: return "None";
            case OPT_BASIC: return "Basic";
            case OPT_AGGRESSIVE: return "Aggressive";
            default: return "Standard";
        }
    }

    // Build behavior

    public boolean isCleanBeforeBuild() {
        return getBoolean(KEY_CLEAN_BEFORE_BUILD, false);
    }

    public void setCleanBeforeBuild(boolean clean) {
        setSetting(KEY_CLEAN_BEFORE_BUILD, clean);
    }

    public boolean isCacheBuildResults() {
        return getBoolean(KEY_CACHE_BUILD_RESULTS, true);
    }

    public void setCacheBuildResults(boolean cache) {
        setSetting(KEY_CACHE_BUILD_RESULTS, cache);
    }

    public int getBuildTimeout() {
        return getInt(KEY_BUILD_TIMEOUT, 300);
    }

    public void setBuildTimeout(int seconds) {
        setSetting(KEY_BUILD_TIMEOUT, seconds);
    }

    public boolean isKotlinDaemonEnabled() {
        return getBoolean(KEY_KOTLIN_DAEMON, true);
    }

    public void setKotlinDaemon(boolean enabled) {
        setSetting(KEY_KOTLIN_DAEMON, enabled);
    }

    // Signing configuration

    public String getSigningKeystore() {
        return getString(KEY_SIGNING_KEYSTORE, "");
    }

    public void setSigningKeystore(String path) {
        setSetting(KEY_SIGNING_KEYSTORE, path);
    }

    public String getSigningAlias() {
        return getString(KEY_SIGNING_ALIAS, "");
    }

    public void setSigningAlias(String alias) {
        setSetting(KEY_SIGNING_ALIAS, alias);
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
}
