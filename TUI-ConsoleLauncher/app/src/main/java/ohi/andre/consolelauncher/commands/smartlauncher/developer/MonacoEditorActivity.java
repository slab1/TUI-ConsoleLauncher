package ohi.andre.consolelauncher.commands.smartlauncher.developer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.widget.Toolbar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;

/**
 * MonacoEditorActivity - Full-featured code editor using Monaco Editor
 * Provides professional code editing with syntax highlighting, autocomplete, and integrations
 */
public class MonacoEditorActivity extends Activity {
    
    private static final String TAG = "MonacoEditorActivity";
    private static final String PREFS_NAME = "monaco_editor_prefs";
    private static final String PREF_THEME = "editor_theme";
    private static final String PREF_FONT_SIZE = "font_size";
    private static final String PREF_WORD_WRAP = "word_wrap";
    
    // Intent extras
    public static final String EXTRA_FILE_PATH = "file_path";
    public static final String EXTRA_IS_NEW_FILE = "is_new_file";
    public static final String EXTRA_INITIAL_CONTENT = "initial_content";
    public static final String EXTRA_DIRECTORY_PATH = "directory_path";
    
    private WebView webView;
    private Toolbar toolbar;
    private String currentFilePath;
    private boolean isNewFile = false;
    private boolean isDirty = false;
    private String initialContent = "";
    
    // JavaScript interface for communication with Monaco
    public class MonacoJavaScriptInterface {
        @JavascriptInterface
        public void onContentChanged(String content) {
            isDirty = true;
            updateTitle();
        }
        
        @JavascriptInterface
        public void onSaveRequested() {
            saveFile();
        }
        
        @JavascriptInterface
        public void onOpenFile(String filePath) {
            openFile(filePath);
        }
        
        @JavascriptInterface
        public void onGitCommand(String command) {
            executeGitCommand(command);
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Setup full screen editor
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_monaco_editor);
        
        // Setup toolbar
        toolbar = findViewById(R.id.toolbar);
        setActionBar(toolbar);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
        // Setup WebView
        setupWebView();
        
        // Handle intent data
        handleIntentData();
        
        // Load editor
        loadEditor();
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        webView = findViewById(R.id.monaco_webview);
        
        // Enable JavaScript
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        
        // WebViewClient to handle page navigation
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "Monaco Editor loaded");
                
                // Initialize editor after page load
                initializeEditor();
            }
        });
        
        // WebChromeClient for console messages and progress
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onConsoleMessage(String message, int lineNumber, String sourceID) {
                Log.d(TAG, "Monaco Console: " + message + " at line " + lineNumber);
            }
        });
        
        // Add JavaScript interface
        webView.addJavascriptInterface(new MonacoJavaScriptInterface(), "Android");
    }
    
    private void handleIntentData() {
        Intent intent = getIntent();
        
        if (intent.hasExtra(EXTRA_FILE_PATH)) {
            currentFilePath = intent.getStringExtra(EXTRA_FILE_PATH);
            isNewFile = intent.getBooleanExtra(EXTRA_IS_NEW_FILE, false);
            initialContent = intent.getStringExtra(EXTRA_INITIAL_CONTENT);
        } else if (intent.hasExtra(EXTRA_DIRECTORY_PATH)) {
            // Directory mode - show file explorer
            currentFilePath = intent.getStringExtra(EXTRA_DIRECTORY_PATH);
        }
    }
    
    private void loadEditor() {
        // Load Monaco Editor HTML
        webView.loadUrl("file:///android_asset/monaco_editor.html");
    }
    
    private void initializeEditor() {
        if (currentFilePath != null) {
            if (isNewFile) {
                // Load initial content for new file
                webView.evaluateJavascript(
                    "window.initializeEditor('" + escapeJavaScript(initialContent) + "', '" + 
                    escapeJavaScript(getLanguageFromPath(currentFilePath)) + "', " +
                    "true" + ");", null);
            } else {
                // Load existing file
                loadFileContent(currentFilePath);
            }
        } else {
            // Blank editor
            webView.evaluateJavascript(
                "window.initializeEditor('', 'plaintext', false);", null);
        }
    }
    
    private void loadFileContent(String filePath) {
        new Thread(() -> {
            try {
                File file = new File(filePath);
                if (!file.exists()) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "File not found: " + filePath, Toast.LENGTH_LONG).show();
                        finish();
                    });
                    return;
                }
                
                String content = readFile(file);
                String language = getLanguageFromPath(filePath);
                
                runOnUiThread(() -> {
                    webView.evaluateJavascript(
                        "window.initializeEditor('" + escapeJavaScript(content) + "', '" + 
                        escapeJavaScript(language) + "', false);", null);
                    
                    updateTitle();
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error loading file", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
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
    
    private void saveFile() {
        if (currentFilePath == null) {
            // New file - ask for name
            saveAsFile();
            return;
        }
        
        new Thread(() -> {
            try {
                webView.evaluateJavascript("window.getEditorContent();", value -> {
                    // Remove quotes from the returned string
                    String content = value != null ? value.substring(1, value.length() - 1) : "";
                    content = unescapeJavaScript(content);
                    
                    saveFileContent(currentFilePath, content);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error saving file", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error saving file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    
    private void saveFileContent(String filePath, String content) {
        try {
            File file = new File(filePath);
            
            // Ensure parent directory exists
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            // Write content to file
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(content.getBytes("UTF-8"));
            }
            
            runOnUiThread(() -> {
                isDirty = false;
                updateTitle();
                Toast.makeText(this, "File saved successfully", Toast.LENGTH_SHORT).show();
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error writing file", e);
            runOnUiThread(() -> {
                Toast.makeText(this, "Error saving file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        }
    }
    
    private void saveAsFile() {
        // This would show a dialog to choose filename
        // For now, show a simple input dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Save As");
        builder.setMessage("Save as functionality will be implemented\nCurrent file will be saved as: " + currentFilePath);
        builder.setPositiveButton("OK", (dialog, which) -> saveFile());
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void openFile(String filePath) {
        Intent intent = new Intent(this, MonacoEditorActivity.class);
        intent.putExtra(EXTRA_FILE_PATH, filePath);
        intent.putExtra(EXTRA_IS_NEW_FILE, false);
        startActivity(intent);
        finish();
    }
    
    private void executeGitCommand(String command) {
        // Execute Git command and update editor
        new Thread(() -> {
            try {
                Process process = Runtime.getRuntime().exec(command);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line).append("\n");
                }
                
                int exitCode = process.waitFor();
                final String output = result.toString();
                
                runOnUiThread(() -> {
                    if (exitCode == 0) {
                        Toast.makeText(this, "Git command executed successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Git command failed: " + output, Toast.LENGTH_LONG).show();
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error executing Git command", e);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Git command error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    
    private void updateTitle() {
        if (currentFilePath != null) {
            File file = new File(currentFilePath);
            String title = file.getName();
            if (isDirty) {
                title += " *";
            }
            getActionBar().setTitle(title);
        } else {
            getActionBar().setTitle("Monaco Editor");
        }
    }
    
    private String getLanguageFromPath(String filePath) {
        String lowerPath = filePath.toLowerCase();
        
        if (lowerPath.endsWith(".java") || lowerPath.endsWith(".kt")) {
            return "java";
        } else if (lowerPath.endsWith(".py")) {
            return "python";
        } else if (lowerPath.endsWith(".js") || lowerPath.endsWith(".ts")) {
            return "javascript";
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
                  .replace("\t", "\\t");
    }
    
    private String unescapeJavaScript(String str) {
        if (str == null) return "";
        return str.replace("\\n", "\n")
                  .replace("\\r", "\r")
                  .replace("\\t", "\t")
                  .replace("\\\"", "\"")
                  .replace("\\\\", "\\");
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.monaco_editor_menu, menu);
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
            saveFile();
            return true;
        } else if (id == R.id.action_save_as) {
            saveAsFile();
            return true;
        } else if (id == R.id.action_find) {
            webView.evaluateJavascript("window.toggleFindWidget();", null);
            return true;
        } else if (id == R.id.action_git_status) {
            executeGitCommand("git status");
            return true;
        } else if (id == R.id.action_git_commit) {
            showCommitDialog();
            return true;
        } else if (id == R.id.action_settings) {
            showSettingsDialog();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void showSaveDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Save Changes");
        builder.setMessage("You have unsaved changes. Do you want to save before closing?");
        builder.setPositiveButton("Save", (dialog, which) -> {
            saveFile();
            finish();
        });
        builder.setNegativeButton("Don't Save", (dialog, which) -> finish());
        builder.setNeutralButton("Cancel", null);
        builder.show();
    }
    
    private void showCommitDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Git Commit");
        builder.setMessage("This would open a commit dialog\nFor now, execute: git add . && git commit -m \"Update\"");
        builder.setPositiveButton("OK", (dialog, which) -> {
            executeGitCommand("git add . && git commit -m \"Update from Monaco Editor\"");
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void showSettingsDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Editor Settings");
        
        // Simple settings for now
        String[] themes = {"Light", "Dark", "High Contrast"};
        int selectedTheme = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getInt(PREF_THEME, 0);
        
        builder.setSingleChoiceItems(themes, selectedTheme, (dialog, which) -> {
            // Apply theme
            String theme = which == 0 ? "vs" : (which == 1 ? "vs-dark" : "hc-black");
            webView.evaluateJavascript("window.setTheme('" + theme + "');", null);
            
            // Save preference
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
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
}