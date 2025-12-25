package ohi.andre.consolelauncher.commands.smartlauncher.developer.settings;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * EditorSettings - Data model holding all Monaco Editor configuration settings
 * Implements thread-safe immutable pattern for settings data
 */
public class EditorSettings {

    // ======= Editor Preferences =======
    private final int fontSize;
    private final String theme;
    private final String wordWrap;
    private final boolean minimapEnabled;
    private final int tabSize;
    private final boolean insertSpaces;
    private final String lineNumbers;
    private final boolean renderWhitespace;
    private final boolean bracketPairGuides;
    private final boolean autoClosingBrackets;
    private final String cursorBlinking;
    private final boolean cursorSmoothCaretAnimation;
    private final int renderLineHighlight;
    private final boolean foldingEnabled;
    private final int indentSize;

    // ======= Editor Behavior =======
    private final boolean autoSave;
    private final int autoSaveDelay;
    private final boolean formatOnSave;
    private final boolean formatOnPaste;
    private final boolean trimTrailingWhitespace;
    private final boolean insertFinalNewline;
    private final boolean autoClosingQuotes;
    private final boolean autoSurround;
    private final boolean wordBasedSuggestions;
    private final int quickSuggestionsDelay;
    private final boolean formatOnType;

    // ======= LSP (Language Server Protocol) Settings =======
    private final boolean lspEnabled;
    private final String lspServerPath;
    private final String lspServerArgs;
    private final int lspTimeout;
    private final boolean lspAutoComplete;
    private final boolean lspDiagnostics;
    private final boolean lspHover;
    private final boolean lspDefinition;
    private final String lspPythonPath;
    private final String lspJavaHome;

    // ======= Debug Settings =======
    private final boolean debugEnabled;
    private final boolean breakOnException;
    private final boolean breakOnEntry;
    private final int maxVariablesPerScope;
    private final int maxStringLength;
    private final boolean showDebugToolbar;
    private final String debugConsolePosition;
    private final boolean asyncDebugStepping;

    // ======= UI Preferences =======
    private final boolean sidebarVisible;
    private final String sidebarWidth;
    private final boolean statusBarVisible;
    private final boolean minimapSideRight;
    private final boolean smoothScrolling;
    private final boolean mouseWheelZoom;
    private final int suggestionsFontSize;
    private final boolean suggestOnTriggerCharacters;
    private final boolean acceptSuggestionOnEnter;

    // ======= File Settings =======
    private final String defaultEncoding;
    private final boolean autoDetectEncoding;
    private final boolean confirmOnClose;
    private final int recentFilesLimit;
    private final boolean preserveUndoStack;

    // ======= Security Settings =======
    private final boolean allowInsecureConnections;
    private final boolean saveSensitiveDataEncrypted;

    // ======= Sensitive Keys (for secure storage) =======
    public static final Set<String> SENSITIVE_KEYS = java.util.Set.of(
        "lsp.token"
    );

    // ======= Default Values =======
    public static final int DEFAULT_FONT_SIZE = 14;
    public static final String DEFAULT_THEME = "vs-dark";
    public static final String DEFAULT_WORD_WRAP = "off";
    public static final boolean DEFAULT_MINIMAP_ENABLED = true;
    public static final int DEFAULT_TAB_SIZE = 4;
    public static final boolean DEFAULT_AUTO_SAVE = false;
    public static final int DEFAULT_AUTO_SAVE_DELAY = 1000;
    public static final boolean DEFAULT_LSP_ENABLED = false;
    public static final boolean DEFAULT_DEBUG_ENABLED = false;
    public static final boolean DEFAULT_SIDEBAR_VISIBLE = true;
    public static final String DEFAULT_ENCODING = "UTF-8";

    // Private constructor for Builder pattern
    private EditorSettings(Builder builder) {
        // Editor Preferences
        this.fontSize = builder.fontSize;
        this.theme = builder.theme;
        this.wordWrap = builder.wordWrap;
        this.minimapEnabled = builder.minimapEnabled;
        this.tabSize = builder.tabSize;
        this.insertSpaces = builder.insertSpaces;
        this.lineNumbers = builder.lineNumbers;
        this.renderWhitespace = builder.renderWhitespace;
        this.bracketPairGuides = builder.bracketPairGuides;
        this.autoClosingBrackets = builder.autoClosingBrackets;
        this.cursorBlinking = builder.cursorBlinking;
        this.cursorSmoothCaretAnimation = builder.cursorSmoothCaretAnimation;
        this.renderLineHighlight = builder.renderLineHighlight;
        this.foldingEnabled = builder.foldingEnabled;
        this.indentSize = builder.indentSize;

        // Editor Behavior
        this.autoSave = builder.autoSave;
        this.autoSaveDelay = builder.autoSaveDelay;
        this.formatOnSave = builder.formatOnSave;
        this.formatOnPaste = builder.formatOnPaste;
        this.trimTrailingWhitespace = builder.trimTrailingWhitespace;
        this.insertFinalNewline = builder.insertFinalNewline;
        this.autoClosingQuotes = builder.autoClosingQuotes;
        this.autoSurround = builder.autoSurround;
        this.wordBasedSuggestions = builder.wordBasedSuggestions;
        this.quickSuggestionsDelay = builder.quickSuggestionsDelay;
        this.formatOnType = builder.formatOnType;

        // LSP Settings
        this.lspEnabled = builder.lspEnabled;
        this.lspServerPath = builder.lspServerPath;
        this.lspServerArgs = builder.lspServerArgs;
        this.lspTimeout = builder.lspTimeout;
        this.lspAutoComplete = builder.lspAutoComplete;
        this.lspDiagnostics = builder.lspDiagnostics;
        this.lspHover = builder.lspHover;
        this.lspDefinition = builder.lspDefinition;
        this.lspPythonPath = builder.lspPythonPath;
        this.lspJavaHome = builder.lspJavaHome;

        // Debug Settings
        this.debugEnabled = builder.debugEnabled;
        this.breakOnException = builder.breakOnException;
        this.breakOnEntry = builder.breakOnEntry;
        this.maxVariablesPerScope = builder.maxVariablesPerScope;
        this.maxStringLength = builder.maxStringLength;
        this.showDebugToolbar = builder.showDebugToolbar;
        this.debugConsolePosition = builder.debugConsolePosition;
        this.asyncDebugStepping = builder.asyncDebugStepping;

        // UI Preferences
        this.sidebarVisible = builder.sidebarVisible;
        this.sidebarWidth = builder.sidebarWidth;
        this.statusBarVisible = builder.statusBarVisible;
        this.minimapSideRight = builder.minimapSideRight;
        this.smoothScrolling = builder.smoothScrolling;
        this.mouseWheelZoom = builder.mouseWheelZoom;
        this.suggestionsFontSize = builder.suggestionsFontSize;
        this.suggestOnTriggerCharacters = builder.suggestOnTriggerCharacters;
        this.acceptSuggestionOnEnter = builder.acceptSuggestionOnEnter;

        // File Settings
        this.defaultEncoding = builder.defaultEncoding;
        this.autoDetectEncoding = builder.autoDetectEncoding;
        this.confirmOnClose = builder.confirmOnClose;
        this.recentFilesLimit = builder.recentFilesLimit;
        this.preserveUndoStack = builder.preserveUndoStack;

        // Security Settings
        this.allowInsecureConnections = builder.allowInsecureConnections;
        this.saveSensitiveDataEncrypted = builder.saveSensitiveDataEncrypted;
    }

    // ======= Getters =======

    // Editor Preferences
    public int getFontSize() { return fontSize; }
    public String getTheme() { return theme; }
    public String getWordWrap() { return wordWrap; }
    public boolean isMinimapEnabled() { return minimapEnabled; }
    public int getTabSize() { return tabSize; }
    public boolean isInsertSpaces() { return insertSpaces; }
    public String getLineNumbers() { return lineNumbers; }
    public boolean isRenderWhitespace() { return renderWhitespace; }
    public boolean isBracketPairGuides() { return bracketPairGuides; }
    public boolean isAutoClosingBrackets() { return autoClosingBrackets; }
    public String getCursorBlinking() { return cursorBlinking; }
    public boolean isCursorSmoothCaretAnimation() { return cursorSmoothCaretAnimation; }
    public int getRenderLineHighlight() { return renderLineHighlight; }
    public boolean isFoldingEnabled() { return foldingEnabled; }
    public int getIndentSize() { return indentSize; }

    // Editor Behavior
    public boolean isAutoSave() { return autoSave; }
    public int getAutoSaveDelay() { return autoSaveDelay; }
    public boolean isFormatOnSave() { return formatOnSave; }
    public boolean isFormatOnPaste() { return formatOnPaste; }
    public boolean isTrimTrailingWhitespace() { return trimTrailingWhitespace; }
    public boolean isInsertFinalNewline() { return insertFinalNewline; }
    public boolean isAutoClosingQuotes() { return autoClosingQuotes; }
    public boolean isAutoSurround() { return autoSurround; }
    public boolean isWordBasedSuggestions() { return wordBasedSuggestions; }
    public int getQuickSuggestionsDelay() { return quickSuggestionsDelay; }
    public boolean isFormatOnType() { return formatOnType; }

    // LSP Settings
    public boolean isLspEnabled() { return lspEnabled; }
    public String getLspServerPath() { return lspServerPath; }
    public String getLspServerArgs() { return lspServerArgs; }
    public int getLspTimeout() { return lspTimeout; }
    public boolean isLspAutoComplete() { return lspAutoComplete; }
    public boolean isLspDiagnostics() { return lspDiagnostics; }
    public boolean isLspHover() { return lspHover; }
    public boolean isLspDefinition() { return lspDefinition; }
    public String getLspPythonPath() { return lspPythonPath; }
    public String getLspJavaHome() { return lspJavaHome; }

    // Debug Settings
    public boolean isDebugEnabled() { return debugEnabled; }
    public boolean isBreakOnException() { return breakOnException; }
    public boolean isBreakOnEntry() { return breakOnEntry; }
    public int getMaxVariablesPerScope() { return maxVariablesPerScope; }
    public int getMaxStringLength() { return maxStringLength; }
    public boolean isShowDebugToolbar() { return showDebugToolbar; }
    public String getDebugConsolePosition() { return debugConsolePosition; }
    public boolean isAsyncDebugStepping() { return asyncDebugStepping; }

    // UI Preferences
    public boolean isSidebarVisible() { return sidebarVisible; }
    public String getSidebarWidth() { return sidebarWidth; }
    public boolean isStatusBarVisible() { return statusBarVisible; }
    public boolean isMinimapSideRight() { return minimapSideRight; }
    public boolean isSmoothScrolling() { return smoothScrolling; }
    public boolean isMouseWheelZoom() { return mouseWheelZoom; }
    public int getSuggestionsFontSize() { return suggestionsFontSize; }
    public boolean isSuggestOnTriggerCharacters() { return suggestOnTriggerCharacters; }
    public boolean isAcceptSuggestionOnEnter() { return acceptSuggestionOnEnter; }

    // File Settings
    public String getDefaultEncoding() { return defaultEncoding; }
    public boolean isAutoDetectEncoding() { return autoDetectEncoding; }
    public boolean isConfirmOnClose() { return confirmOnClose; }
    public int getRecentFilesLimit() { return recentFilesLimit; }
    public boolean isPreserveUndoStack() { return preserveUndoStack; }

    // Security Settings
    public boolean isAllowInsecureConnections() { return allowInsecureConnections; }
    public boolean isSaveSensitiveDataEncrypted() { return saveSensitiveDataEncrypted; }

    // ======= Conversion Methods =======

    /**
     * Convert settings to Monaco Editor JSON format
     */
    public JSONObject toMonacoOptions() {
        JSONObject options = new JSONObject();
        try {
            // Monaco Editor compatible options
            options.put("fontSize", fontSize);
            options.put("theme", theme);
            options.put("wordWrap", wordWrap);
            options.put("minimap", new JSONObject().put("enabled", minimapEnabled));
            options.put("tabSize", tabSize);
            options.put("insertSpaces", insertSpaces);
            options.put("lineNumbers", lineNumbers);
            options.put("renderWhitespace", renderWhitespace ? "selection" : "none");
            options.put("autoClosingBrackets", autoClosingBrackets ? "always" : "never");
            options.put("cursorBlinking", cursorBlinking);
            options.put("cursorSmoothCaretAnimation", cursorSmoothCaretAnimation ? "on" : "off");
            options.put("renderLineHighlight", renderLineHighlight == 1 ? "all" : "line");
            options.put("folding", foldingEnabled);
            options.put("scrollBeyondLastLine", false);
            options.put("smoothScrolling", smoothScrolling);
            options.put("mouseWheelZoom", mouseWheelZoom);
            options.put("suggestFontSize", suggestionsFontSize);
            options.put("acceptSuggestionOnEnter", acceptSuggestionOnEnter ? "on" : "off");
            options.put("formatOnType", formatOnType);
            options.put("formatOnPaste", formatOnPaste);
        } catch (Exception e) {
            android.util.Log.e("EditorSettings", "Error converting to Monaco options", e);
        }
        return options;
    }

    /**
     * Convert settings to JSON for JavaScript consumption
     */
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            // Editor Preferences
            json.put("fontSize", fontSize);
            json.put("theme", theme);
            json.put("wordWrap", wordWrap);
            json.put("minimapEnabled", minimapEnabled);
            json.put("tabSize", tabSize);
            json.put("insertSpaces", insertSpaces);
            json.put("lineNumbers", lineNumbers);
            json.put("renderWhitespace", renderWhitespace);
            json.put("bracketPairGuides", bracketPairGuides);
            json.put("autoClosingBrackets", autoClosingBrackets);
            json.put("cursorBlinking", cursorBlinking);
            json.put("cursorSmoothCaretAnimation", cursorSmoothCaretAnimation);
            json.put("renderLineHighlight", renderLineHighlight);
            json.put("foldingEnabled", foldingEnabled);
            json.put("indentSize", indentSize);

            // Editor Behavior
            json.put("autoSave", autoSave);
            json.put("autoSaveDelay", autoSaveDelay);
            json.put("formatOnSave", formatOnSave);
            json.put("formatOnPaste", formatOnPaste);
            json.put("trimTrailingWhitespace", trimTrailingWhitespace);
            json.put("insertFinalNewline", insertFinalNewline);
            json.put("autoClosingQuotes", autoClosingQuotes);
            json.put("autoSurround", autoSurround);
            json.put("wordBasedSuggestions", wordBasedSuggestions);
            json.put("quickSuggestionsDelay", quickSuggestionsDelay);
            json.put("formatOnType", formatOnType);

            // LSP Settings
            json.put("lspEnabled", lspEnabled);
            json.put("lspServerPath", lspServerPath);
            json.put("lspServerArgs", lspServerArgs);
            json.put("lspTimeout", lspTimeout);
            json.put("lspAutoComplete", lspAutoComplete);
            json.put("lspDiagnostics", lspDiagnostics);
            json.put("lspHover", lspHover);
            json.put("lspDefinition", lspDefinition);
            json.put("lspPythonPath", lspPythonPath);
            json.put("lspJavaHome", lspJavaHome);

            // Debug Settings
            json.put("debugEnabled", debugEnabled);
            json.put("breakOnException", breakOnException);
            json.put("breakOnEntry", breakOnEntry);
            json.put("maxVariablesPerScope", maxVariablesPerScope);
            json.put("maxStringLength", maxStringLength);
            json.put("showDebugToolbar", showDebugToolbar);
            json.put("debugConsolePosition", debugConsolePosition);
            json.put("asyncDebugStepping", asyncDebugStepping);

            // UI Preferences
            json.put("sidebarVisible", sidebarVisible);
            json.put("sidebarWidth", sidebarWidth);
            json.put("statusBarVisible", statusBarVisible);
            json.put("minimapSideRight", minimapSideRight);
            json.put("smoothScrolling", smoothScrolling);
            json.put("mouseWheelZoom", mouseWheelZoom);
            json.put("suggestionsFontSize", suggestionsFontSize);
            json.put("suggestOnTriggerCharacters", suggestOnTriggerCharacters);
            json.put("acceptSuggestionOnEnter", acceptSuggestionOnEnter);

            // File Settings
            json.put("defaultEncoding", defaultEncoding);
            json.put("autoDetectEncoding", autoDetectEncoding);
            json.put("confirmOnClose", confirmOnClose);
            json.put("recentFilesLimit", recentFilesLimit);
            json.put("preserveUndoStack", preserveUndoStack);

            // Security Settings
            json.put("allowInsecureConnections", allowInsecureConnections);
            json.put("saveSensitiveDataEncrypted", saveSensitiveDataEncrypted);

        } catch (Exception e) {
            android.util.Log.e("EditorSettings", "Error converting to JSON", e);
        }
        return json;
    }

    /**
     * Convert to Map for SharedPreferences storage
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        // Editor Preferences
        map.put("fontSize", fontSize);
        map.put("theme", theme);
        map.put("wordWrap", wordWrap);
        map.put("minimapEnabled", minimapEnabled);
        map.put("tabSize", tabSize);
        map.put("insertSpaces", insertSpaces);
        map.put("lineNumbers", lineNumbers);
        map.put("renderWhitespace", renderWhitespace);
        map.put("bracketPairGuides", bracketPairGuides);
        map.put("autoClosingBrackets", autoClosingBrackets);
        map.put("cursorBlinking", cursorBlinking);
        map.put("cursorSmoothCaretAnimation", cursorSmoothCaretAnimation);
        map.put("renderLineHighlight", renderLineHighlight);
        map.put("foldingEnabled", foldingEnabled);
        map.put("indentSize", indentSize);

        // Editor Behavior
        map.put("autoSave", autoSave);
        map.put("autoSaveDelay", autoSaveDelay);
        map.put("formatOnSave", formatOnSave);
        map.put("formatOnPaste", formatOnPaste);
        map.put("trimTrailingWhitespace", trimTrailingWhitespace);
        map.put("insertFinalNewline", insertFinalNewline);
        map.put("autoClosingQuotes", autoClosingQuotes);
        map.put("autoSurround", autoSurround);
        map.put("wordBasedSuggestions", wordBasedSuggestions);
        map.put("quickSuggestionsDelay", quickSuggestionsDelay);
        map.put("formatOnType", formatOnType);

        // LSP Settings
        map.put("lspEnabled", lspEnabled);
        map.put("lspServerPath", lspServerPath);
        map.put("lspServerArgs", lspServerArgs);
        map.put("lspTimeout", lspTimeout);
        map.put("lspAutoComplete", lspAutoComplete);
        map.put("lspDiagnostics", lspDiagnostics);
        map.put("lspHover", lspHover);
        map.put("lspDefinition", lspDefinition);
        map.put("lspPythonPath", lspPythonPath);
        map.put("lspJavaHome", lspJavaHome);

        // Debug Settings
        map.put("debugEnabled", debugEnabled);
        map.put("breakOnException", breakOnException);
        map.put("breakOnEntry", breakOnEntry);
        map.put("maxVariablesPerScope", maxVariablesPerScope);
        map.put("maxStringLength", maxStringLength);
        map.put("showDebugToolbar", showDebugToolbar);
        map.put("debugConsolePosition", debugConsolePosition);
        map.put("asyncDebugStepping", asyncDebugStepping);

        // UI Preferences
        map.put("sidebarVisible", sidebarVisible);
        map.put("sidebarWidth", sidebarWidth);
        map.put("statusBarVisible", statusBarVisible);
        map.put("minimapSideRight", minimapSideRight);
        map.put("smoothScrolling", smoothScrolling);
        map.put("mouseWheelZoom", mouseWheelZoom);
        map.put("suggestionsFontSize", suggestionsFontSize);
        map.put("suggestOnTriggerCharacters", suggestOnTriggerCharacters);
        map.put("acceptSuggestionOnEnter", acceptSuggestionOnEnter);

        // File Settings
        map.put("defaultEncoding", defaultEncoding);
        map.put("autoDetectEncoding", autoDetectEncoding);
        map.put("confirmOnClose", confirmOnClose);
        map.put("recentFilesLimit", recentFilesLimit);
        map.put("preserveUndoStack", preserveUndoStack);

        // Security Settings
        map.put("allowInsecureConnections", allowInsecureConnections);
        map.put("saveSensitiveDataEncrypted", saveSensitiveDataEncrypted);

        return map;
    }

    // ======= Builder Pattern =======

    public static class Builder {
        // Editor Preferences
        private int fontSize = DEFAULT_FONT_SIZE;
        private String theme = DEFAULT_THEME;
        private String wordWrap = DEFAULT_WORD_WRAP;
        private boolean minimapEnabled = DEFAULT_MINIMAP_ENABLED;
        private int tabSize = DEFAULT_TAB_SIZE;
        private boolean insertSpaces = true;
        private String lineNumbers = "on";
        private boolean renderWhitespace = false;
        private boolean bracketPairGuides = false;
        private boolean autoClosingBrackets = true;
        private String cursorBlinking = "blink";
        private boolean cursorSmoothCaretAnimation = false;
        private int renderLineHighlight = 1;
        private boolean foldingEnabled = true;
        private int indentSize = 4;

        // Editor Behavior
        private boolean autoSave = DEFAULT_AUTO_SAVE;
        private int autoSaveDelay = DEFAULT_AUTO_SAVE_DELAY;
        private boolean formatOnSave = true;
        private boolean formatOnPaste = false;
        private boolean trimTrailingWhitespace = false;
        private boolean insertFinalNewline = false;
        private boolean autoClosingQuotes = false;
        private boolean autoSurround = true;
        private boolean wordBasedSuggestions = true;
        private int quickSuggestionsDelay = 10;
        private boolean formatOnType = false;

        // LSP Settings
        private boolean lspEnabled = DEFAULT_LSP_ENABLED;
        private String lspServerPath = "";
        private String lspServerArgs = "";
        private int lspTimeout = 30;
        private boolean lspAutoComplete = true;
        private boolean lspDiagnostics = true;
        private boolean lspHover = true;
        private boolean lspDefinition = true;
        private String lspPythonPath = "";
        private String lspJavaHome = "";

        // Debug Settings
        private boolean debugEnabled = DEFAULT_DEBUG_ENABLED;
        private boolean breakOnException = true;
        private boolean breakOnEntry = false;
        private int maxVariablesPerScope = 100;
        private int maxStringLength = 1000;
        private boolean showDebugToolbar = true;
        private String debugConsolePosition = "bottom";
        private boolean asyncDebugStepping = true;

        // UI Preferences
        private boolean sidebarVisible = DEFAULT_SIDEBAR_VISIBLE;
        private String sidebarWidth = "250px";
        private boolean statusBarVisible = true;
        private boolean minimapSideRight = true;
        private boolean smoothScrolling = false;
        private boolean mouseWheelZoom = true;
        private int suggestionsFontSize = 14;
        private boolean suggestOnTriggerCharacters = true;
        private boolean acceptSuggestionOnEnter = true;

        // File Settings
        private String defaultEncoding = DEFAULT_ENCODING;
        private boolean autoDetectEncoding = true;
        private boolean confirmOnClose = true;
        private int recentFilesLimit = 10;
        private boolean preserveUndoStack = true;

        // Security Settings
        private boolean allowInsecureConnections = false;
        private boolean saveSensitiveDataEncrypted = true;

        // ======= Builder Methods =======

        public Builder setFontSize(int fontSize) {
            this.fontSize = Math.max(8, Math.min(36, fontSize));
            return this;
        }

        public Builder setTheme(String theme) {
            this.theme = theme != null ? theme : DEFAULT_THEME;
            return this;
        }

        public Builder setWordWrap(String wordWrap) {
            this.wordWrap = wordWrap != null ? wordWrap : DEFAULT_WORD_WRAP;
            return this;
        }

        public Builder setMinimapEnabled(boolean minimapEnabled) {
            this.minimapEnabled = minimapEnabled;
            return this;
        }

        public Builder setTabSize(int tabSize) {
            this.tabSize = Math.max(1, Math.min(16, tabSize));
            return this;
        }

        public Builder setInsertSpaces(boolean insertSpaces) {
            this.insertSpaces = insertSpaces;
            return this;
        }

        public Builder setLineNumbers(String lineNumbers) {
            this.lineNumbers = lineNumbers != null ? lineNumbers : "on";
            return this;
        }

        public Builder setRenderWhitespace(boolean renderWhitespace) {
            this.renderWhitespace = renderWhitespace;
            return this;
        }

        public Builder setAutoSave(boolean autoSave) {
            this.autoSave = autoSave;
            return this;
        }

        public Builder setAutoSaveDelay(int autoSaveDelay) {
            this.autoSaveDelay = Math.max(100, Math.min(60000, autoSaveDelay));
            return this;
        }

        public Builder setFormatOnSave(boolean formatOnSave) {
            this.formatOnSave = formatOnSave;
            return this;
        }

        public Builder setLspEnabled(boolean lspEnabled) {
            this.lspEnabled = lspEnabled;
            return this;
        }

        public Builder setLspServerPath(String lspServerPath) {
            this.lspServerPath = lspServerPath != null ? lspServerPath : "";
            return this;
        }

        public Builder setDebugEnabled(boolean debugEnabled) {
            this.debugEnabled = debugEnabled;
            return this;
        }

        public Builder setSidebarVisible(boolean sidebarVisible) {
            this.sidebarVisible = sidebarVisible;
            return this;
        }

        public Builder setDefaultEncoding(String defaultEncoding) {
            this.defaultEncoding = defaultEncoding != null ? defaultEncoding : DEFAULT_ENCODING;
            return this;
        }

        public Builder applyMap(Map<String, Object> map) {
            if (map == null || map.isEmpty()) return this;

            // Editor Preferences
            if (map.containsKey("fontSize")) this.fontSize = (Integer) map.get("fontSize");
            if (map.containsKey("theme")) this.theme = (String) map.get("theme");
            if (map.containsKey("wordWrap")) this.wordWrap = (String) map.get("wordWrap");
            if (map.containsKey("minimapEnabled")) this.minimapEnabled = (Boolean) map.get("minimapEnabled");
            if (map.containsKey("tabSize")) this.tabSize = (Integer) map.get("tabSize");
            if (map.containsKey("insertSpaces")) this.insertSpaces = (Boolean) map.get("insertSpaces");
            if (map.containsKey("lineNumbers")) this.lineNumbers = (String) map.get("lineNumbers");
            if (map.containsKey("renderWhitespace")) this.renderWhitespace = (Boolean) map.get("renderWhitespace");
            if (map.containsKey("autoClosingBrackets")) this.autoClosingBrackets = (Boolean) map.get("autoClosingBrackets");
            if (map.containsKey("cursorBlinking")) this.cursorBlinking = (String) map.get("cursorBlinking");
            if (map.containsKey("foldingEnabled")) this.foldingEnabled = (Boolean) map.get("foldingEnabled");

            // Editor Behavior
            if (map.containsKey("autoSave")) this.autoSave = (Boolean) map.get("autoSave");
            if (map.containsKey("autoSaveDelay")) this.autoSaveDelay = (Integer) map.get("autoSaveDelay");
            if (map.containsKey("formatOnSave")) this.formatOnSave = (Boolean) map.get("formatOnSave");
            if (map.containsKey("formatOnPaste")) this.formatOnPaste = (Boolean) map.get("formatOnPaste");
            if (map.containsKey("trimTrailingWhitespace")) this.trimTrailingWhitespace = (Boolean) map.get("trimTrailingWhitespace");

            // LSP Settings
            if (map.containsKey("lspEnabled")) this.lspEnabled = (Boolean) map.get("lspEnabled");
            if (map.containsKey("lspServerPath")) this.lspServerPath = (String) map.get("lspServerPath");
            if (map.containsKey("lspServerArgs")) this.lspServerArgs = (String) map.get("lspServerArgs");
            if (map.containsKey("lspPythonPath")) this.lspPythonPath = (String) map.get("lspPythonPath");
            if (map.containsKey("lspJavaHome")) this.lspJavaHome = (String) map.get("lspJavaHome");

            // Debug Settings
            if (map.containsKey("debugEnabled")) this.debugEnabled = (Boolean) map.get("debugEnabled");
            if (map.containsKey("breakOnException")) this.breakOnException = (Boolean) map.get("breakOnException");
            if (map.containsKey("maxVariablesPerScope")) this.maxVariablesPerScope = (Integer) map.get("maxVariablesPerScope");

            // UI Preferences
            if (map.containsKey("sidebarVisible")) this.sidebarVisible = (Boolean) map.get("sidebarVisible");
            if (map.containsKey("sidebarWidth")) this.sidebarWidth = (String) map.get("sidebarWidth");
            if (map.containsKey("smoothScrolling")) this.smoothScrolling = (Boolean) map.get("smoothScrolling");
            if (map.containsKey("mouseWheelZoom")) this.mouseWheelZoom = (Boolean) map.get("mouseWheelZoom");

            // File Settings
            if (map.containsKey("defaultEncoding")) this.defaultEncoding = (String) map.get("defaultEncoding");
            if (map.containsKey("autoDetectEncoding")) this.autoDetectEncoding = (Boolean) map.get("autoDetectEncoding");
            if (map.containsKey("recentFilesLimit")) this.recentFilesLimit = (Integer) map.get("recentFilesLimit");

            return this;
        }

        public EditorSettings build() {
            return new EditorSettings(this);
        }
    }

    /**
     * Create default settings
     */
    public static EditorSettings getDefaults() {
        return new Builder().build();
    }
}
