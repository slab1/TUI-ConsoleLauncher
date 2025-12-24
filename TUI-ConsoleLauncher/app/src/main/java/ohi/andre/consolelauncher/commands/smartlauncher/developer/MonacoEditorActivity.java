package ohi.andre.consolelauncher.commands.smartlauncher.developer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MonacoEditorActivityEnhanced - Full-featured IDE using Monaco Editor
 * Phase 2 Enhanced Version with File Explorer, Git, Terminal, AI Assistant, and Plugin System
 */
public class MonacoEditorActivity extends Activity {
    
    private static final String TAG = "MonacoEditorActivityEnhanced";
    private static final String PREFS_NAME = "monaco_editor_enhanced_prefs";
    private static final String PREF_THEME = "editor_theme";
    private static final String PREF_FONT_SIZE = "font_size";
    private static final String PREF_WORD_WRAP = "word_wrap";
    private static final String PREF_SIDEBAR_WIDTH = "sidebar_width";
    private static final String PREF_TERMINAL_HEIGHT = "terminal_height";
    
    // Intent extras
    public static final String EXTRA_FILE_PATH = "file_path";
    public static final String EXTRA_IS_NEW_FILE = "is_new_file";
    public static final String EXTRA_INITIAL_CONTENT = "initial_content";
    public static final String EXTRA_DIRECTORY_PATH = "directory_path";
    public static final String EXTRA_PROJECT_PATH = "project_path";
    
    // AI Configuration
    private static final String AI_API_ENDPOINT = "https://api.openai.com/v1/completions";
    private static final String AI_API_KEY = "YOUR_AI_API_KEY"; // Should be configured in settings
    private static final boolean AI_ENABLED = true;
    
    private WebView webView;
    private Toolbar toolbar;
    private String currentFilePath;
    private String currentProjectPath;
    private boolean isNewFile = false;
    private boolean isDirty = false;
    private String initialContent = "";
    
    // File system and project management
    private Map<String, FileInfo> fileSystem = new HashMap<>();
    private List<String> openTabs = new ArrayList<>();
    private String currentTab = null;
    
    // Git integration
    private GitManager gitManager;
    
    // Terminal management
    private TerminalManager terminalManager;
    
    // AI Assistant
    private AIAssistant aiAssistant;
    
    // Plugin system
    private PluginManager pluginManager;
    
    // File system management
    private FileSystemManager fileSystemManager;
    
    // Background execution
    private ExecutorService executor = Executors.newCachedThreadPool();
    
    /**
     * File information data class
     */
    private static class FileInfo {
        public String path;
        public String content;
        public String language;
        public boolean modified;
        public String gitStatus; // "added", "modified", "deleted", null
        public long lastModified;
        
        public FileInfo(String path, String content, String language) {
            this.path = path;
            this.content = content;
            this.language = language;
            this.modified = false;
            this.gitStatus = null;
            this.lastModified = System.currentTimeMillis();
        }
    }
    
    /**
     * JavaScript interface for enhanced Monaco Editor communication
     */
    public class EnhancedMonacoJavaScriptInterface {
        @JavascriptInterface
        public void onContentChanged(String content) {
            if (currentTab != null) {
                FileInfo fileInfo = fileSystem.get(currentTab);
                if (fileInfo != null) {
                    fileInfo.content = content;
                    fileInfo.modified = true;
                    fileInfo.lastModified = System.currentTimeMillis();
                    isDirty = true;
                    updateTitle();
                    
                    // Update file system
                    webView.evaluateJavascript("window.markTabDirty('" + currentTab + "', true);", null);
                }
            }
        }
        
        @JavascriptInterface
        public void onSaveRequested() {
            saveCurrentFile();
        }
        
        @JavascriptInterface
        public void onOpenFile(String filePath) {
            openFile(filePath);
        }
        
        @JavascriptInterface
        public void onGitCommand(String command) {
            executeGitCommand(command);
        }
        
        @JavascriptInterface
        public void onTerminalCommand(String command) {
            executeTerminalCommand(command);
        }
        
        @JavascriptInterface
        public void onFileOperation(String operation, String path, String newName) {
            handleFileOperation(operation, path, newName);
        }
        
        @JavascriptInterface
        public void onAIRequest(String requestType, String context) {
            handleAIRequest(requestType, context);
        }
        
        @JavascriptInterface
        public void onShowInExplorer() {
            showInFileManager();
        }
        
        @JavascriptInterface
        public void onPluginCommand(String command, String args) {
            executePluginCommand(command, args);
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Setup full screen enhanced editor
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_monaco_editor);
        
        // Initialize managers
        initializeManagers();
        
        // Setup toolbar
        toolbar = findViewById(R.id.toolbar);
        setActionBar(toolbar);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        // Setup WebView
        setupEnhancedWebView();
        
        // Handle intent data
        handleIntentData();
        
        // Load enhanced editor
        loadEnhancedEditor();
    }
    
    private void initializeManagers() {
        gitManager = new GitManager(this);
        terminalManager = new TerminalManager(this);
        aiAssistant = new AIAssistant(this);
        pluginManager = new PluginManager(this);
        fileSystemManager = new FileSystemManager(this);
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private void setupEnhancedWebView() {
        webView = findViewById(R.id.monaco_webview);
        
        // Enable JavaScript and enhanced features
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setGeolocationEnabled(false);
        webView.getSettings().setAllowContentUrlAccess(true);
        
        // Enable WebView debugging
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        
        // WebViewClient for enhanced page handling
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "Enhanced Monaco Editor loaded");
                
                // Initialize enhanced editor
                initializeEnhancedEditor();
            }
        });
        
        // Enhanced WebChromeClient for console and progress
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onConsoleMessage(String message, int lineNumber, String sourceID) {
                Log.d(TAG, "Monaco Console [" + sourceID + ":" + lineNumber + "]: " + message);
            }
        });
        
        // Add enhanced JavaScript interface
        webView.addJavascriptInterface(new EnhancedMonacoJavaScriptInterface(), "Android");
    }
    
    private void handleIntentData() {
        Intent intent = getIntent();
        
        if (intent.hasExtra(EXTRA_PROJECT_PATH)) {
            currentProjectPath = intent.getStringExtra(EXTRA_PROJECT_PATH);
            loadProject(currentProjectPath);
        } else if (intent.hasExtra(EXTRA_FILE_PATH)) {
            currentFilePath = intent.getStringExtra(EXTRA_FILE_PATH);
            isNewFile = intent.getBooleanExtra(EXTRA_IS_NEW_FILE, false);
            initialContent = intent.getStringExtra(EXTRA_INITIAL_CONTENT);
        } else if (intent.hasExtra(EXTRA_DIRECTORY_PATH)) {
            currentProjectPath = intent.getStringExtra(EXTRA_DIRECTORY_PATH);
            loadProject(currentProjectPath);
        }
    }
    
    private void loadEnhancedEditor() {
        // Load enhanced Monaco Editor HTML
        webView.loadUrl("file:///android_asset/monaco_editor.html");
    }
    
    private void initializeEnhancedEditor() {
        // Load welcome content and initialize enhanced features
        String welcomeContent = getWelcomeContent();
        
        webView.evaluateJavascript(
            "window.initializeEnhancedEditor && window.initializeEnhancedEditor();", null);
        
        // Initialize project if available
        if (currentProjectPath != null) {
            webView.evaluateJavascript("window.loadProject && window.loadProject('" + 
                escapeJavaScript(currentProjectPath) + "');", null);
        } else if (currentFilePath != null) {
            if (isNewFile) {
                loadNewFile(currentFilePath, initialContent);
            } else {
                loadFile(currentFilePath);
            }
        } else {
            // Show welcome screen
            loadWelcomeScreen();
        }
    }
    
    private String getWelcomeContent() {
        return "# Welcome to T-UI Enhanced Monaco Editor\n\n" +
               "## 🚀 Phase 2 Features\n\n" +
               "### 📁 File Explorer\n" +
               "- Browse project files in the left sidebar\n" +
               "- Right-click for context menu operations\n" +
               "- Git status indicators on files\n\n" +
               "### 🌿 Git Integration\n" +
               "- Branch information in status bar\n" +
               "- Modified files highlighted in explorer\n" +
               "- Git status panel with staging area\n\n" +
               "### 🤖 AI Assistant\n" +
               "- Intelligent code completion\n" +
               "- Ghost text suggestions\n" +
               "- Code explanation and refactoring\n\n" +
               "### 💻 Integrated Terminal\n" +
               "- Full terminal emulation\n" +
               "- Command execution and output\n" +
               "- Terminal resizing support\n\n" +
               "### 🔌 Plugin System\n" +
               "- Extensible architecture\n" +
               "- Custom commands and UI\n" +
               "- Community extensions ready\n\n" +
               "## 🎯 Quick Start\n\n" +
               "1. **Open a project**: Use File > Open Project\n" +
               "2. **Terminal**: Press Ctrl+` or click Terminal in status bar\n" +
               "3. **AI Help**: Type code and wait for suggestions\n" +
               "4. **Git**: Check Git panel for version control status\n\n" +
               "**Ready to code? Start by creating a new file or opening an existing project!**";
    }
    
    // File System Operations
    private void loadProject(String projectPath) {
        // Background thread execution for file system operations
        executor.execute(() -> {
            try {
                File projectDir = new File(projectPath);
                if (!projectDir.exists() || !projectDir.isDirectory()) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Invalid project directory: " + projectPath, Toast.LENGTH_LONG).show();
                        loadWelcomeScreen();
                    });
                    return;
                }
                
                scanDirectory(projectDir, "");
                loadWelcomeScreen();
                
                // Update file tree in UI
                runOnUiThread(() -> {
                    updateFileTreeUI();
                    updateGitStatus();
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error loading project", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading project: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    loadWelcomeScreen();
                });
            }
        });
    }
    
    private void scanDirectory(File dir, String relativePath) {
        fileSystemManager.scanDirectory(dir, relativePath, fileSystem);
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
            
            webView.evaluateJavascript(
                "window.updateFileSystem && window.updateFileSystem(" + fileSystemJson.toString() + ");", null);
        } catch (JSONException e) {
            Log.e(TAG, "Error updating file system UI", e);
        }
    }
    
    private void loadWelcomeScreen() {
        webView.evaluateJavascript(
            "window.switchToTab && window.switchToTab('welcome');", null);
        currentTab = "welcome";
    }
    
    private void loadNewFile(String fileName, String content) {
        String language = getLanguageFromPath(fileName);
        FileInfo fileInfo = new FileInfo(fileName, content, language);
        fileSystem.put(fileName, fileInfo);
        
        webView.evaluateJavascript(
            "window.createTab && window.createTab('" + escapeJavaScript(fileName) + "', " +
            "{content: '" + escapeJavaScript(content) + "', language: '" + language + "', modified: false});", null);
        
        webView.evaluateJavascript(
            "window.switchToTab && window.switchToTab('" + escapeJavaScript(fileName) + "');", null);
        
        currentTab = fileName;
        updateTitle();
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
            webView.evaluateJavascript(
                "window.createTab && window.createTab('" + escapeJavaScript(filePath) + "', " +
                "{content: '" + escapeJavaScript(fileInfo.content) + "', language: '" + 
                fileInfo.language + "', modified: " + fileInfo.modified + "});", null);
            
            webView.evaluateJavascript(
                "window.switchToTab && window.switchToTab('" + escapeJavaScript(filePath) + "');", null);
            
            currentTab = filePath;
            updateTitle();
        }
    }
    
    private void openFile(String filePath) {
        if (!fileSystem.containsKey(filePath)) {
            // Try to load file
            loadFile(filePath);
        } else {
            loadFile(filePath);
        }
    }
    
    private void saveCurrentFile() {
        if (currentTab == null || currentTab.equals("welcome")) return;
        
        FileInfo fileInfo = fileSystem.get(currentTab);
        if (fileInfo == null) return;
        
        // Background thread execution for file saving operations
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
                
                runOnUiThread(() -> {
                    isDirty = false;
                    updateTitle();
                    webView.evaluateJavascript("window.markTabDirty('" + currentTab + "', false);", null);
                    Toast.makeText(this, "File saved successfully", Toast.LENGTH_SHORT).show();
                    
                    // Update git status
                    updateGitStatus();
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error saving file", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error saving file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void handleFileOperation(String operation, String path, String newName) {
        // Background thread execution for file operations
        executor.execute(() -> {
            try {
                switch (operation) {
                    case "create_file":
                        createFile(path);
                        break;
                    case "create_folder":
                        createFolder(path);
                        break;
                    case "rename":
                        renameFile(path, newName);
                        break;
                    case "delete":
                        deleteFile(path);
                        break;
                    default:
                        Log.w(TAG, "Unknown file operation: " + operation);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error handling file operation", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "File operation error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void createFile(String fileName) {
        String language = getLanguageFromPath(fileName);
        FileInfo fileInfo = new FileInfo(fileName, "", language);
        fileInfo.modified = true;
        fileSystem.put(fileName, fileInfo);
        
        runOnUiThread(() -> {
            updateFileTreeUI();
            Toast.makeText(this, "File created: " + fileName, Toast.LENGTH_SHORT).show();
        });
    }
    
    private void createFolder(String folderName) {
        FileInfo folderInfo = new FileInfo(folderName + "/", "", "folder");
        fileSystem.put(folderName + "/", folderInfo);
        
        runOnUiThread(() -> {
            updateFileTreeUI();
            Toast.makeText(this, "Folder created: " + folderName, Toast.LENGTH_SHORT).show();
        });
    }
    
    private void renameFile(String oldPath, String newName) {
        FileInfo fileInfo = fileSystem.get(oldPath);
        if (fileInfo == null) return;
        
        String newPath = oldPath.substring(0, oldPath.lastIndexOf('/') + 1) + newName;
        fileInfo.path = newPath;
        fileSystem.remove(oldPath);
        fileSystem.put(newPath, fileInfo);
        
        runOnUiThread(() -> {
            updateFileTreeUI();
            Toast.makeText(this, "File renamed to: " + newName, Toast.LENGTH_SHORT).show();
        });
    }
    
    private void deleteFile(String path) {
        fileSystem.remove(path);
        
        runOnUiThread(() -> {
            updateFileTreeUI();
            Toast.makeText(this, "File deleted: " + path, Toast.LENGTH_SHORT).show();
        });
    }
    
    // Git Integration
    private void executeGitCommand(String command) {
        if (currentProjectPath == null) {
            runOnUiThread(() -> {
                Toast.makeText(this, "No project loaded for Git operations", Toast.LENGTH_LONG).show();
            });
            return;
        }
        
        executor.execute(() -> {
            try {
                String result = gitManager.executeCommand(command, currentProjectPath);
                runOnUiThread(() -> {
                    if (result != null && !result.isEmpty()) {
                        // Display git output in terminal
                        webView.evaluateJavascript(
                            "window.executeTerminalCommand && window.executeTerminalCommand('git output');", null);
                    }
                    updateGitStatus();
                    Toast.makeText(this, "Git command executed", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error executing Git command", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Git command failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void updateGitStatus() {
        if (currentProjectPath == null) return;
        
        executor.execute(() -> {
            try {
                GitStatus gitStatus = gitManager.getStatus(currentProjectPath);
                
                // Update file system with git status
                for (Map.Entry<String, FileInfo> entry : fileSystem.entrySet()) {
                    String path = entry.getKey();
                    if (!path.endsWith("/")) { // Skip folders
                        String gitFilePath = currentProjectPath + "/" + path;
                        String status = gitStatus.getFileStatus(gitFilePath);
                        entry.getValue().gitStatus = status;
                    }
                }
                
                runOnUiThread(() -> {
                    // Update UI with git status
                    webView.evaluateJavascript(
                        "window.updateGitStatus && window.updateGitStatus('" + 
                        gitStatus.getCurrentBranch() + "', " + gitStatus.toJSONArray() + ");", null);
                    updateFileTreeUI();
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error updating git status", e);
            }
        });
    }
    
    // Terminal Integration
    private void executeTerminalCommand(String command) {
        if (currentProjectPath == null) {
            runOnUiThread(() -> {
                webView.evaluateJavascript(
                    "window.terminal && window.terminal.writeln('No project directory set');", null);
            });
            return;
        }
        
        executor.execute(() -> {
            try {
                String result = terminalManager.executeCommand(command, currentProjectPath);
                
                runOnUiThread(() -> {
                    // Send output to terminal
                    webView.evaluateJavascript(
                        "window.terminal && window.terminal.writeln('" + 
                        escapeJavaScript(result) + "');", null);
                    
                    // Refresh file system if directory changed
                    if (command.trim().startsWith("cd ")) {
                        loadProject(currentProjectPath);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error executing terminal command", e);
                runOnUiThread(() -> {
                    webView.evaluateJavascript(
                        "window.terminal && window.terminal.writeln('Error: " + 
                        escapeJavaScript(e.getMessage()) + "');", null);
                });
            }
        });
    }
    
    // AI Assistant
    private void handleAIRequest(String requestType, String context) {
        if (!AI_ENABLED) return;
        
        executor.execute(() -> {
            try {
                String response = aiAssistant.processRequest(requestType, context);
                
                runOnUiThread(() -> {
                    switch (requestType) {
                        case "suggestion":
                            webView.evaluateJavascript(
                                "window.showAISuggestion && window.showAISuggestion('" + 
                                escapeJavaScript(response) + "', {x: 200, y: 50});", null);
                            break;
                        case "completion":
                            webView.evaluateJavascript(
                                "window.acceptAICompletion && window.acceptAICompletion('" + 
                                escapeJavaScript(response) + "');", null);
                            break;
                        case "explanation":
                            showAIExplanation(response);
                            break;
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error processing AI request", e);
            }
        });
    }
    
    private void showAIExplanation(String explanation) {
        runOnUiThread(() -> {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("AI Code Explanation");
            builder.setMessage(explanation);
            builder.setPositiveButton("OK", null);
            builder.show();
        });
    }
    
    // Plugin System
    private void executePluginCommand(String command, String args) {
        try {
            String result = pluginManager.executeCommand(command, args);
            if (result != null) {
                webView.evaluateJavascript(
                    "window.showStatusMessage && window.showStatusMessage('" + 
                    escapeJavaScript(result) + "');", null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error executing plugin command", e);
        }
    }
    
    // Utility Methods
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
    
    private String escapeJavaScript(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t")
                  .replace("'", "\\'");
    }
    
    private void updateTitle() {
        if (currentTab != null && !currentTab.equals("welcome")) {
            String title = new File(currentTab).getName();
            if (isDirty) {
                title += " *";
            }
            getActionBar().setTitle(title);
        } else {
            getActionBar().setTitle("T-UI Enhanced Monaco Editor");
        }
    }
    
    private void showInFileManager() {
        if (currentTab != null && !currentTab.equals("welcome")) {
            File file = new File(currentProjectPath != null ? 
                currentProjectPath + "/" + currentTab : currentTab);
            if (file.exists()) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(file), "resource/folder");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        }
    }
    
    // Menu and Dialog Methods
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.monaco_editor_enhanced_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            if (isDirty) {
                showSaveDialog();
            } else {
                finish();
            }
            return true;
        } else if (id == R.id.action_save) {
            saveCurrentFile();
            return true;
        } else if (id == R.id.action_new_file) {
            createNewFile();
            return true;
        } else if (id == R.id.action_open_project) {
            openProject();
            return true;
        } else if (id == R.id.action_find) {
            webView.evaluateJavascript("window.toggleFindWidget && window.toggleFindWidget();", null);
            return true;
        } else if (id == R.id.action_git_status) {
            executeGitCommand("git status");
            return true;
        } else if (id == R.id.action_terminal) {
            webView.evaluateJavascript("window.toggleTerminal && window.toggleTerminal();", null);
            return true;
        } else if (id == R.id.action_ai_toggle) {
            toggleAIAssistant();
            return true;
        } else if (id == R.id.action_settings) {
            showSettingsDialog();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void createNewFile() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Create New File");
        
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("filename.ext");
        builder.setView(input);
        
        builder.setPositiveButton("Create", (dialog, which) -> {
            String fileName = input.getText().toString().trim();
            if (!fileName.isEmpty()) {
                createFile(fileName);
                openFile(fileName);
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void openProject() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Open Project");
        
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("/path/to/project");
        builder.setView(input);
        
        builder.setPositiveButton("Open", (dialog, which) -> {
            String projectPath = input.getText().toString().trim();
            if (!projectPath.isEmpty()) {
                currentProjectPath = projectPath;
                loadProject(projectPath);
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void toggleAIAssistant() {
        // Toggle AI assistant in JavaScript
        webView.evaluateJavascript("window.aiEnabled = !window.aiEnabled;", null);
        Toast.makeText(this, "AI Assistant " + 
            (true ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
    }
    
    private void showSaveDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Save Changes");
        builder.setMessage("You have unsaved changes. Do you want to save before closing?");
        builder.setPositiveButton("Save", (dialog, which) -> {
            saveCurrentFile();
            finish();
        });
        builder.setNegativeButton("Don't Save", (dialog, which) -> finish());
        builder.setNeutralButton("Cancel", null);
        builder.show();
    }
    
    private void showSettingsDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Editor Settings");
        
        // Create settings layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        
        // Theme selection
        TextView themeLabel = new TextView(this);
        themeLabel.setText("Theme:");
        layout.addView(themeLabel);
        
        String[] themes = {"Light", "Dark", "High Contrast"};
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int selectedTheme = prefs.getInt(PREF_THEME, 1); // Default to dark
        
        builder.setSingleChoiceItems(themes, selectedTheme, (dialog, which) -> {
            String theme = which == 0 ? "vs" : (which == 1 ? "vs-dark" : "hc-black");
            webView.evaluateJavascript("window.setTheme && window.setTheme('" + theme + "');", null);
            prefs.edit().putInt(PREF_THEME, which).apply();
        });
        
        builder.setPositiveButton("OK", null);
        builder.show();
    }
    
    @Override
    public void onBackPressed() {
        if (isDirty) {
            showSaveDialog();
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
    
    // Manager Classes
    private static class GitManager {
        private Activity activity;
        
        public GitManager(Activity activity) {
            this.activity = activity;
        }
        
        public String executeCommand(String command, String workingDirectory) throws Exception {
            Process process = Runtime.getRuntime().exec(command, null, new File(workingDirectory));
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new Exception("Git command failed with exit code: " + exitCode);
            }
            
            return result.toString();
        }
        
        public GitStatus getStatus(String workingDirectory) throws Exception {
            String branchOutput = executeCommand("git rev-parse --abbrev-ref HEAD", workingDirectory);
            String currentBranch = branchOutput.trim();
            
            // Parse git status output
            String statusOutput = executeCommand("git status --porcelain", workingDirectory);
            Map<String, String> fileStatus = new HashMap<>();
            
            String[] lines = statusOutput.split("\n");
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                
                String status = line.substring(0, 2).trim();
                String filePath = line.substring(3);
                
                if (status.startsWith("A")) {
                    fileStatus.put(filePath, "added");
                } else if (status.startsWith("M") || status.endsWith("M")) {
                    fileStatus.put(filePath, "modified");
                } else if (status.startsWith("D")) {
                    fileStatus.put(filePath, "deleted");
                }
            }
            
            return new GitStatus(currentBranch, fileStatus);
        }
    }
    
    private static class GitStatus {
        private String currentBranch;
        private Map<String, String> fileStatus;
        
        public GitStatus(String currentBranch, Map<String, String> fileStatus) {
            this.currentBranch = currentBranch;
            this.fileStatus = fileStatus;
        }
        
        public String getCurrentBranch() {
            return currentBranch;
        }
        
        public String getFileStatus(String filePath) {
            return fileStatus.get(filePath);
        }
        
        public String toJSONArray() {
            // Convert to JSON array format for JavaScript
            return "[]"; // Simplified for now
        }
    }
    
    private static class TerminalManager {
        private Activity activity;
        
        public TerminalManager(Activity activity) {
            this.activity = activity;
        }
        
        public String executeCommand(String command, String workingDirectory) throws Exception {
            Process process = Runtime.getRuntime().exec(command, null, new File(workingDirectory));
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String errorLine;
                StringBuilder errorResult = new StringBuilder();
                while ((errorLine = errorReader.readLine()) != null) {
                    errorResult.append(errorLine).append("\n");
                }
                throw new Exception(errorResult.toString().trim());
            }
            
            return result.toString().trim();
        }
    }
    
    private static class AIAssistant {
        private Activity activity;
        
        public AIAssistant(Activity activity) {
            this.activity = activity;
        }
        
        public String processRequest(String requestType, String context) {
            // Mock AI responses for demonstration
            // In a real implementation, this would connect to an AI service
            switch (requestType) {
                case "suggestion":
                    return "Consider adding error handling for edge cases.";
                case "completion":
                    return "console.log('Debug output');";
                case "explanation":
                    return "This code implements a basic authentication function that validates user credentials.";
                default:
                    return "AI Assistant is processing your request...";
            }
        }
    }
    
    private static class PluginManager {
        private Activity activity;
        
        public PluginManager(Activity activity) {
            this.activity = activity;
        }
        
        public String executeCommand(String command, String args) {
            // Mock plugin system
            if (command.equals("hello")) {
                return "Hello from plugin system!";
            }
            return null;
        }
    }
    
    private static class FileSystemManager {
        private Activity activity;
        
        public FileSystemManager(Activity activity) {
            this.activity = activity;
        }
        
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