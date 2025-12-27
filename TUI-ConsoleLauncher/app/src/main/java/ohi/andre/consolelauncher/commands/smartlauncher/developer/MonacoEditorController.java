package ohi.andre.consolelauncher.commands.smartlauncher.developer;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * MonacoEditorController - Separates business logic from Activity
 * Handles all Monaco Editor operations and communication
 */
public class MonacoEditorController {
    private static final String TAG = "MonacoEditorController";
    
    private final Activity activity;
    private final WebView webView;
    private final ExecutorService executor;
    
    // File system management
    private Map<String, FileInfo> fileSystem = new HashMap<>();
    private List<String> openTabs = new ArrayList<>();
    private String currentTab = null;
    
    // Editor state
    private boolean isDirty = false;
    private String currentFilePath;
    private String currentProjectPath;
    private boolean isNewFile = false;
    
    // Managers
    private final LanguageServerManager lspManager;
    private final DebugManager debugManager;
    private final FileSystemManager fileSystemManager;
    private final EditorSettingsManager settingsManager;
    
    public MonacoEditorController(Activity activity, WebView webView, ExecutorService executor) {
        this.activity = activity;
        this.webView = webView;
        this.executor = executor;
        
        // Initialize managers
        this.lspManager = new LanguageServerManager();
        this.debugManager = new DebugManager();
        this.fileSystemManager = new FileSystemManager();
        this.settingsManager = EditorSettingsManager.getInstance();
        
        // Initialize settings manager
        initializeSettingsManager();
    }
    
    // ======= Settings Management =======
    
    /**
     * Initialize the settings manager with context
     */
    private void initializeSettingsManager() {
        if (activity != null) {
            settingsManager.initialize(activity);
            
            // Add settings change listener
            settingsManager.addListener("MonacoEditorController", new EditorSettingsManager.SettingsChangeListener() {
                @Override
                public void onSettingsChanged(EditorSettings settings, String key) {
                    onEditorSettingsChanged(settings, key);
                }
                
                @Override
                public void onSettingsReset() {
                    onEditorSettingsReset();
                }
            });
        }
    }
    
    /**
     * Handle settings change from manager
     */
    private void onEditorSettingsChanged(EditorSettings settings, String key) {
        activity.runOnUiThread(() -> {
            // Apply editor preference changes to WebView
            if (key != null) {
                switch (key) {
                    case "theme":
                        sendToWebView("window.applyTheme", "'" + settings.getTheme() + "'");
                        break;
                    case "fontSize":
                        sendToWebView("window.applyFontSize", String.valueOf(settings.getFontSize()));
                        break;
                    case "wordWrap":
                        sendToWebView("window.applyWordWrap", "'" + settings.getWordWrap() + "'");
                        break;
                    case "minimapEnabled":
                        sendToWebView("window.applyMinimap", String.valueOf(settings.isMinimapEnabled()));
                        break;
                    case "sidebarVisible":
                        sendToWebView("window.toggleSidebar", String.valueOf(settings.isSidebarVisible()));
                        break;
                    case "autoSave":
                        sendToWebView("window.applyAutoSave", String.valueOf(settings.isAutoSave()));
                        break;
                    case "lspEnabled":
                        sendToWebView("window.applyLspEnabled", String.valueOf(settings.isLspEnabled()));
                        break;
                    case "debugEnabled":
                        sendToWebView("window.applyDebugEnabled", String.valueOf(settings.isDebugEnabled()));
                        break;
                }
            }
        });
    }
    
    /**
     * Handle settings reset
     */
    private void onEditorSettingsReset() {
        activity.runOnUiThread(() -> {
            EditorSettings defaults = EditorSettings.getDefaults();
            sendToWebView("window.applyAllSettings", defaults.toJson().toString());
            Toast.makeText(activity, "Settings reset to defaults", Toast.LENGTH_SHORT).show();
        });
    }
    
    /**
     * Get the settings manager for external access
     */
    public EditorSettingsManager getSettingsManager() {
        return settingsManager;
    }
    
    /**
     * Get current editor settings
     */
    public EditorSettings getEditorSettings() {
        return settingsManager.getSettings();
    }
    
    /**
     * Initialize settings bridge for WebView
     */
    public void initializeSettingsBridge(MonacoSettingsBridge.SettingsChangeCallback callback) {
        if (webView != null) {
            MonacoSettingsBridge bridge = new MonacoSettingsBridge(settingsManager, callback);
            webView.addJavascriptInterface(bridge, "AndroidEditorSettings");
        }
    }
    
    /**
     * Load and apply initial settings to editor
     */
    public void loadInitialSettings() {
        settingsManager.loadSettings(new EditorSettingsManager.SettingsCallback() {
            @Override
            public void onSettingsLoaded(EditorSettings settings) {
                activity.runOnUiThread(() -> {
                    // Send settings to WebView
                    String settingsJson = settings.toJson().toString();
                    sendToWebView("window.initSettings", settingsJson);
                    
                    // Send Monaco-specific options
                    String monacoOptions = settings.toMonacoOptions().toString();
                    sendToWebView("window.applyMonacoOptions", monacoOptions);
                });
            }
            
            @Override
            public void onSettingsSaved(String key, boolean success) {
                // Not used for initial load
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading settings: " + error);
            }
        });
    }
    
    /**
     * Update a setting from WebView
     */
    public void updateSetting(String key, Object value) {
        settingsManager.saveSetting(key, value, null);
    }
    
    /**
     * Apply editor settings to current session
     */
    public void applyEditorSettings(EditorSettings settings) {
        if (settings == null) return;
        
        activity.runOnUiThread(() -> {
            JSONObject options = settings.toMonacoOptions();
            sendToWebView("window.applyEditorSettings", options.toString());
        });
    }
    
    // ======= File Operations =======
    public void handleContentChanged(String content) {
        if (currentTab != null) {
            FileInfo fileInfo = fileSystem.get(currentTab);
            if (fileInfo != null) {
                fileInfo.content = content;
                fileInfo.modified = true;
                fileInfo.lastModified = System.currentTimeMillis();
                isDirty = true;
                updateTitle();
                
                // Send update to JavaScript
                sendToWebView("window.markTabDirty", "'" + currentTab + "', true");
            }
        }
    }
    
    public void handleSaveRequested() {
        saveCurrentFile();
    }
    
    public void handleOpenFile(String filePath) {
        openFile(filePath);
    }
    
    private void openFile(String filePath) {
        if (!fileSystem.containsKey(filePath)) {
            loadFile(filePath);
        } else {
            loadFile(filePath);
        }
    }
    
    private void loadFile(String filePath) {
        FileInfo fileInfo = fileSystem.get(filePath);
        if (fileInfo == null) {
            // Try to load from disk
            try {
                File file = new File(filePath);
                if (file.exists()) {
                    String content = readFile(file);
                    String language = getLanguageFromPath(filePath);
                    fileInfo = new FileInfo(filePath, content, language);
                    fileSystem.put(filePath, fileInfo);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error loading file from disk", e);
            }
        }
        
        if (fileInfo != null) {
            String escapedContent = escapeJavaScript(fileInfo.content);
            String escapedPath = escapeJavaScript(filePath);
            
            sendToWebView("window.createTab", "'" + escapedPath + "', " +
                "{content: '" + escapedContent + "', language: '" + 
                fileInfo.language + "', modified: " + fileInfo.modified + "}");
            
            sendToWebView("window.switchToTab", "'" + escapedPath + "'");
            
            currentTab = filePath;
            updateTitle();
        }
    }
    
    private void saveCurrentFile() {
        if (currentTab == null || currentTab.equals("welcome")) return;
        
        FileInfo fileInfo = fileSystem.get(currentTab);
        if (fileInfo == null) return;
        
        executor.execute(() -> {
            try {
                File file = new File(currentProjectPath != null ? 
                    currentProjectPath + "/" + currentTab : currentTab);
                
                // Ensure parent directory exists
                File parentDir = file.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                
                // Write content to file
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(fileInfo.content.getBytes("UTF-8"));
                }
                
                fileInfo.modified = false;
                fileInfo.lastModified = System.currentTimeMillis();
                
                activity.runOnUiThread(() -> {
                    isDirty = false;
                    updateTitle();
                    sendToWebView("window.markTabDirty", "'" + currentTab + "', false");
                    Toast.makeText(activity, "File saved successfully", Toast.LENGTH_SHORT).show();
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error saving file", e);
                activity.runOnUiThread(() -> {
                    Toast.makeText(activity, "Error saving file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    // ======= LSP (Language Server Protocol) =======
    public void handleLspRequest(String requestId, String method, String params) {
        Log.d(TAG, "Handling LSP request: " + method);
        
        executor.execute(() -> {
            try {
                JSONObject jsonParams = new JSONObject(params);
                JSONObject response = null;
                
                switch (method) {
                    case "textDocument/completion":
                        response = lspManager.handleCompletion(requestId, jsonParams);
                        break;
                    case "textDocument/definition":
                        response = lspManager.handleDefinition(requestId, jsonParams);
                        break;
                    case "textDocument/hover":
                        response = lspManager.handleHover(requestId, jsonParams);
                        break;
                    case "textDocument/diagnostics":
                        response = lspManager.handleDiagnostics(requestId, jsonParams);
                        break;
                    case "initialize":
                        response = lspManager.handleInitialize(requestId, jsonParams);
                        break;
                    case "shutdown":
                        response = lspManager.handleShutdown(requestId, jsonParams);
                        break;
                    default:
                        Log.w(TAG, "Unknown LSP method: " + method);
                        response = createErrorResponse(requestId, "Method not supported: " + method);
                }
                
                // Send response back to JavaScript
                if (response != null) {
                    String responseScript = "window.onLspResponse && window.onLspResponse('" + 
                        requestId + "', " + response.toString() + ");";
                    activity.runOnUiThread(() -> webView.evaluateJavascript(responseScript, null));
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error handling LSP request", e);
                JSONObject errorResponse = createErrorResponse(requestId, e.getMessage());
                sendErrorToWebView(requestId, errorResponse);
            }
        });
    }
    
    public void handleCompletionRequest(String documentUri, int line, int column, String triggerCharacter) {
        try {
            JSONObject completionItem = new JSONObject();
            completionItem.put("label", "console.log");
            completionItem.put("kind", 14); // Function
            completionItem.put("detail", "console.log(message: any): void");
            completionItem.put("insertText", "console.log(${1:message});");
            completionItem.put("insertTextFormat", 2); // Snippet
            
            JSONArray completions = new JSONArray();
            completions.put(completionItem);
            
            JSONObject response = new JSONObject();
            response.put("items", completions);
            
            sendToWebView("window.onLspCompletionResponse", response.toString());
            
        } catch (JSONException e) {
            Log.e(TAG, "Error handling completion request", e);
        }
    }
    
    public void handleDefinitionRequest(String documentUri, int line, int column) {
        try {
            JSONArray definitions = new JSONArray();
            
            JSONObject definition = new JSONObject();
            definition.put("uri", documentUri);
            definition.put("range", createRange(line - 1, column - 1, line - 1, column + 10));
            
            definitions.put(definition);
            
            sendToWebView("window.onLspDefinitionResponse", definitions.toString());
            
        } catch (JSONException e) {
            Log.e(TAG, "Error handling definition request", e);
        }
    }
    
    public void handleHoverRequest(String documentUri, int line, int column) {
        try {
            JSONObject hover = new JSONObject();
            hover.put("contents", "Console.log method\n\nPrints a message to the console.");
            hover.put("range", createRange(line - 1, column - 1, line - 1, column + 1));
            
            sendToWebView("window.onLspHoverResponse", hover.toString());
            
        } catch (JSONException e) {
            Log.e(TAG, "Error handling hover request", e);
        }
    }
    
    public void handleDiagnosticsRequest(String documentUri) {
        try {
            JSONArray diagnostics = new JSONArray();
            
            // Simulate a warning diagnostic
            JSONObject diagnostic = new JSONObject();
            diagnostic.put("range", createRange(5, 0, 5, 15));
            diagnostic.put("severity", 2); // Warning
            diagnostic.put("message", "Unused variable 'unusedVar'");
            diagnostic.put("source", "JavaScript");
            
            diagnostics.put(diagnostic);
            
            sendToWebView("window.onLspDiagnosticsResponse", diagnostics.toString());
            
        } catch (JSONException e) {
            Log.e(TAG, "Error handling diagnostics request", e);
        }
    }
    
    // ======= Debug Integration =======
    public void handleDebugCommand(String command, String params) {
        Log.d(TAG, "Handling debug command: " + command);
        
        executor.execute(() -> {
            try {
                JSONObject jsonParams = new JSONObject(params);
                
                switch (command) {
                    case "start":
                        debugManager.start();
                        sendDebugStatus("running");
                        break;
                    case "continue":
                        debugManager.continueExecution();
                        sendDebugStatus("running");
                        break;
                    case "stepOver":
                        debugManager.stepOver();
                        sendDebugStep("stepOver");
                        break;
                    case "stepInto":
                        debugManager.stepInto();
                        sendDebugStep("stepInto");
                        break;
                    case "stepOut":
                        debugManager.stepOut();
                        sendDebugStep("stepOut");
                        break;
                    case "stop":
                        debugManager.stop();
                        sendDebugStatus("stopped");
                        break;
                    default:
                        Log.w(TAG, "Unknown debug command: " + command);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error handling debug command: " + command, e);
            }
        });
    }
    
    public void handleBreakpointToggle(String filePath, int lineNumber, boolean enabled) {
        try {
            debugManager.toggleBreakpoint(filePath, lineNumber, enabled);
            
            JSONObject breakpoint = new JSONObject();
            breakpoint.put("filePath", filePath);
            breakpoint.put("lineNumber", lineNumber);
            breakpoint.put("enabled", enabled);
            
            sendToWebView("window.onBreakpointToggle", breakpoint.toString());
            
            Log.i(TAG, "Breakpoint " + (enabled ? "set" : "removed") + " at " + filePath + ":" + lineNumber);
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling breakpoint toggle", e);
        }
    }
    
    public void handleVariableWatch(String variableName, String expression) {
        try {
            JSONObject watch = new JSONObject();
            watch.put("variableName", variableName);
            watch.put("expression", expression);
            watch.put("value", "undefined"); // Would be populated by actual debugger
            
            sendToWebView("window.onVariableWatchUpdate", watch.toString());
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling variable watch", e);
        }
    }
    
    // ======= Utility Methods =======
    private void sendToWebView(String method, String params) {
        String script = method + "(" + params + ");";
        activity.runOnUiThread(() -> webView.evaluateJavascript(script, null));
    }
    
    private void sendErrorToWebView(String requestId, JSONObject errorResponse) {
        String script = "window.onLspError && window.onLspError('" + requestId + "', " + errorResponse.toString() + ");";
        activity.runOnUiThread(() -> webView.evaluateJavascript(script, null));
    }
    
    private JSONObject createErrorResponse(String requestId, String errorMessage) throws JSONException {
        JSONObject response = new JSONObject();
        response.put("id", requestId);
        response.put("error", errorMessage);
        return response;
    }
    
    private JSONObject createRange(int startLine, int startCol, int endLine, int endCol) throws JSONException {
        JSONObject range = new JSONObject();
        range.put("start", createPosition(startLine, startCol));
        range.put("end", createPosition(endLine, endCol));
        return range;
    }
    
    private JSONObject createPosition(int line, int column) throws JSONException {
        JSONObject position = new JSONObject();
        position.put("line", line);
        position.put("character", column);
        return position;
    }
    
    private String escapeJavaScript(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t")
                  .replace("'", "\\'");
    }
    
    private String readFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
    
    private String getLanguageFromPath(String filePath) {
        String lowerPath = filePath.toLowerCase();
        
        if (lowerPath.endsWith(".java") || lowerPath.endsWith(".kt")) {
            return "java";
        } else if (lowerPath.endsWith(".py")) {
            return "python";
        } else if (lowerPath.endsWith(".js") || lowerPath.endsWith(".ts")) {
            return lowerPath.endsWith(".ts") ? "typescript" : "javascript";
        } else if (lowerPath.endsWith(".html")) {
            return "html";
        } else if (lowerPath.endsWith(".css")) {
            return "css";
        } else if (lowerPath.endsWith(".json")) {
            return "json";
        } else if (lowerPath.endsWith(".xml")) {
            return "xml";
        } else if (lowerPath.endsWith(".md")) {
            return "markdown";
        } else if (lowerPath.endsWith(".yml") || lowerPath.endsWith(".yaml")) {
            return "yaml";
        } else if (lowerPath.endsWith(".sh")) {
            return "shell";
        } else if (lowerPath.endsWith(".cpp") || lowerPath.endsWith(".cxx")) {
            return "cpp";
        } else if (lowerPath.endsWith(".c")) {
            return "c";
        } else {
            return "plaintext";
        }
    }
    
    private void updateTitle() {
        if (currentTab != null && !currentTab.equals("welcome")) {
            String title = new File(currentTab).getName();
            if (isDirty) {
                title += " *";
            }
            if (activity.getActionBar() != null) {
                activity.getActionBar().setTitle(title);
            }
        } else {
            if (activity.getActionBar() != null) {
                activity.getActionBar().setTitle("Monaco Editor");
            }
        }
    }
    
    private void sendDebugStatus(String status) {
        sendToWebView("window.onDebugStatusChanged", "{\"status\": \"" + status + "\"}");
    }
    
    private void sendDebugStep(String action) {
        sendToWebView("window.onDebugStep", "{\"action\": \"" + action + "\"}");
    }
    
    // ======= File Path and Project Management =======
    public void handleFilePath(String filePath, boolean isNewFile, String initialContent) {
        this.currentFilePath = filePath;
        this.isNewFile = isNewFile;
        
        if (isNewFile && initialContent != null) {
            String language = getLanguageFromPath(filePath);
            FileInfo fileInfo = new FileInfo(filePath, initialContent, language);
            fileSystem.put(filePath, fileInfo);
        }
    }
    
    public void handleProjectPath(String projectPath) {
        this.currentProjectPath = projectPath;
        
        executor.execute(() -> {
            try {
                File projectDir = new File(projectPath);
                if (projectDir.exists() && projectDir.isDirectory()) {
                    fileSystem.clear();
                    fileSystemManager.scanDirectory(projectDir, "", fileSystem);
                    
                    activity.runOnUiThread(() -> {
                        updateFileTreeUI();
                        loadWelcomeScreen();
                    });
                } else {
                    activity.runOnUiThread(() -> {
                        android.widget.Toast.makeText(activity, "Invalid project directory: " + projectPath, android.widget.Toast.LENGTH_LONG).show();
                        loadWelcomeScreen();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading project", e);
                activity.runOnUiThread(() -> {
                    android.widget.Toast.makeText(activity, "Error loading project: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                    loadWelcomeScreen();
                });
            }
        });
    }
    
    public void handleCreateFile(String fileName) {
        String language = getLanguageFromPath(fileName);
        FileInfo fileInfo = new FileInfo(fileName, "", language);
        fileInfo.modified = true;
        fileSystem.put(fileName, fileInfo);
        
        activity.runOnUiThread(() -> {
            updateFileTreeUI();
            openFile(fileName);
            android.widget.Toast.makeText(activity, "File created: " + fileName, android.widget.Toast.LENGTH_SHORT).show();
        });
    }
    
    private void updateFileTreeUI() {
        // Send file system data to JavaScript
        try {
            JSONObject fileSystemJson = new JSONObject();
            for (Map.Entry<String, FileInfo> entry : fileSystem.entrySet()) {
                JSONObject fileJson = new JSONObject();
                fileJson.put("path", entry.getValue().path);
                fileJson.put("content", entry.getValue().content);
                fileJson.put("language", entry.getValue().language);
                fileJson.put("modified", entry.getValue().modified);
                fileJson.put("gitStatus", entry.getValue().gitStatus);
                fileSystemJson.put(entry.getKey(), fileJson);
            }
            
            sendToWebView("window.updateFileSystem", fileSystemJson.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error updating file system UI", e);
        }
    }
    
    private void loadWelcomeScreen() {
        sendToWebView("window.switchToTab", "'welcome'");
        currentTab = "welcome";
    }
    
    // ======= Utility Methods for State =======
    public boolean hasUnsavedChanges() {
        return isDirty;
    }
    
    // ======= State Management =======
    public Bundle saveState() {
        Bundle state = new Bundle();
        state.putString("currentTab", currentTab);
        state.putBoolean("isDirty", isDirty);
        state.putString("currentFilePath", currentFilePath);
        state.putString("currentProjectPath", currentProjectPath);
        state.putBoolean("isNewFile", isNewFile);
        
        // Save file system state
        Bundle fileSystemState = new Bundle();
        for (Map.Entry<String, FileInfo> entry : fileSystem.entrySet()) {
            Bundle fileInfoState = new Bundle();
            fileInfoState.putString("content", entry.getValue().content);
            fileInfoState.putString("language", entry.getValue().language);
            fileInfoState.putBoolean("modified", entry.getValue().modified);
            fileInfoState.putLong("lastModified", entry.getValue().lastModified);
            fileSystemState.putBundle(entry.getKey(), fileInfoState);
        }
        state.putBundle("fileSystem", fileSystemState);
        
        return state;
    }
    
    public void restoreState(Bundle state) {
        if (state == null) return;
        
        currentTab = state.getString("currentTab");
        isDirty = state.getBoolean("isDirty");
        currentFilePath = state.getString("currentFilePath");
        currentProjectPath = state.getString("currentProjectPath");
        isNewFile = state.getBoolean("isNewFile");
        
        // Restore file system
        Bundle fileSystemState = state.getBundle("fileSystem");
        if (fileSystemState != null) {
            fileSystem.clear();
            for (String key : fileSystemState.keySet()) {
                Bundle fileInfoState = fileSystemState.getBundle(key);
                if (fileInfoState != null) {
                    FileInfo fileInfo = new FileInfo(key, "", "");
                    fileInfo.content = fileInfoState.getString("content", "");
                    fileInfo.language = fileInfoState.getString("language", "");
                    fileInfo.modified = fileInfoState.getBoolean("modified", false);
                    fileInfo.lastModified = fileInfoState.getLong("lastModified", System.currentTimeMillis());
                    fileSystem.put(key, fileInfo);
                }
            }
        }
        
        updateTitle();
    }
    
    // ======= File System Manager =======
    private static class FileSystemManager {
        public void scanDirectory(File dir, String relativePath, Map<String, FileInfo> fileSystem) {
            File[] files = dir.listFiles();
            if (files == null) return;
            
            for (File file : files) {
                String fileRelativePath = relativePath.isEmpty() ? file.getName() : relativePath + "/" + file.getName();
                
                if (file.isDirectory()) {
                    // Add directory to file system
                    fileSystem.put(fileRelativePath + "/", new FileInfo(fileRelativePath + "/", "", "folder"));
                    scanDirectory(file, fileRelativePath, fileSystem);
                } else {
                    // Add file to file system
                    try {
                        String content = readFile(file);
                        String language = getLanguageFromPath(file.getAbsolutePath());
                        fileSystem.put(fileRelativePath, new FileInfo(fileRelativePath, content, language));
                    } catch (IOException e) {
                        Log.w("FileSystemManager", "Could not read file: " + file.getAbsolutePath(), e);
                    }
                }
            }
        }
        
        private static String readFile(File file) throws IOException {
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            return content.toString();
        }
        
        private static String getLanguageFromPath(String filePath) {
            String lowerPath = filePath.toLowerCase();
            
            if (lowerPath.endsWith(".java") || lowerPath.endsWith(".kt")) {
                return "java";
            } else if (lowerPath.endsWith(".py")) {
                return "python";
            } else if (lowerPath.endsWith(".js") || lowerPath.endsWith(".ts")) {
                return lowerPath.endsWith(".ts") ? "typescript" : "javascript";
            } else if (lowerPath.endsWith(".html")) {
                return "html";
            } else if (lowerPath.endsWith(".css")) {
                return "css";
            } else if (lowerPath.endsWith(".json")) {
                return "json";
            } else if (lowerPath.endsWith(".xml")) {
                return "xml";
            } else if (lowerPath.endsWith(".md")) {
                return "markdown";
            } else if (lowerPath.endsWith(".yml") || lowerPath.endsWith(".yaml")) {
                return "yaml";
            } else if (lowerPath.endsWith(".sh")) {
                return "shell";
            } else if (lowerPath.endsWith(".cpp") || lowerPath.endsWith(".cxx")) {
                return "cpp";
            } else if (lowerPath.endsWith(".c")) {
                return "c";
            } else {
                return "plaintext";
            }
        }
    }
}