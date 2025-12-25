package ohi.andre.consolelauncher.commands.smartlauncher.developer;

import android.app.Activity;
import android.util.Log;
import android.webkit.JavascriptInterface;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;

/**
 * Fixed JavaScript bridge to prevent memory leaks
 * Uses WeakReference to prevent Activity memory leak
 */
public class MonacoJavaScriptBridge {
    private static final String TAG = "MonacoJSBridge";
    
    private final WeakReference<Activity> activityRef;
    private final WeakReference<MonacoEditorController> controllerRef;
    private final ExecutorService executor;
    
    public MonacoJavaScriptBridge(Activity activity, MonacoEditorController controller, ExecutorService executor) {
        this.activityRef = new WeakReference<>(activity);
        this.controllerRef = new WeakReference<>(controller);
        this.executor = executor;
    }
    
    // ======= File Operations =======
    @JavascriptInterface
    public void onContentChanged(String content) {
        MonacoEditorController controller = controllerRef.get();
        if (controller != null) {
            controller.handleContentChanged(content);
        } else {
            Log.w(TAG, "Controller is null, content change ignored");
        }
    }
    
    @JavascriptInterface
    public void onSaveRequested() {
        MonacoEditorController controller = controllerRef.get();
        if (controller != null) {
            controller.handleSaveRequested();
        } else {
            Log.w(TAG, "Controller is null, save request ignored");
        }
    }
    
    @JavascriptInterface
    public void onOpenFile(String filePath) {
        MonacoEditorController controller = controllerRef.get();
        if (controller != null) {
            controller.handleOpenFile(filePath);
        } else {
            Log.w(TAG, "Controller is null, open file request ignored");
        }
    }
    
    // ======= LSP (Language Server Protocol) Integration =======
    @JavascriptInterface
    public void onLspRequest(String requestId, String method, String params) {
        MonacoEditorController controller = controllerRef.get();
        if (controller != null) {
            controller.handleLspRequest(requestId, method, params);
        } else {
            Log.w(TAG, "Controller is null, LSP request ignored: " + method);
        }
    }
    
    @JavascriptInterface
    public void onCompletionRequest(String documentUri, int line, int column, String triggerCharacter) {
        MonacoEditorController controller = controllerRef.get();
        if (controller != null) {
            controller.handleCompletionRequest(documentUri, line, column, triggerCharacter);
        } else {
            Log.w(TAG, "Controller is null, completion request ignored");
        }
    }
    
    @JavascriptInterface
    public void onDefinitionRequest(String documentUri, int line, int column) {
        MonacoEditorController controller = controllerRef.get();
        if (controller != null) {
            controller.handleDefinitionRequest(documentUri, line, column);
        } else {
            Log.w(TAG, "Controller is null, definition request ignored");
        }
    }
    
    @JavascriptInterface
    public void onHoverRequest(String documentUri, int line, int column) {
        MonacoEditorController controller = controllerRef.get();
        if (controller != null) {
            controller.handleHoverRequest(documentUri, line, column);
        } else {
            Log.w(TAG, "Controller is null, hover request ignored");
        }
    }
    
    @JavascriptInterface
    public void onDiagnosticsRequest(String documentUri) {
        MonacoEditorController controller = controllerRef.get();
        if (controller != null) {
            controller.handleDiagnosticsRequest(documentUri);
        } else {
            Log.w(TAG, "Controller is null, diagnostics request ignored");
        }
    }
    
    // ======= Debug Integration =======
    @JavascriptInterface
    public void onDebugCommand(String command, String params) {
        MonacoEditorController controller = controllerRef.get();
        if (controller != null) {
            controller.handleDebugCommand(command, params);
        } else {
            Log.w(TAG, "Controller is null, debug command ignored: " + command);
        }
    }
    
    @JavascriptInterface
    public void onToggleBreakpoint(String filePath, int lineNumber, boolean enabled) {
        MonacoEditorController controller = controllerRef.get();
        if (controller != null) {
            controller.handleBreakpointToggle(filePath, lineNumber, enabled);
        } else {
            Log.w(TAG, "Controller is null, breakpoint toggle ignored");
        }
    }
    
    @JavascriptInterface
    public void onVariableWatch(String variableName, String expression) {
        MonacoEditorController controller = controllerRef.get();
        if (controller != null) {
            controller.handleVariableWatch(variableName, expression);
        } else {
            Log.w(TAG, "Controller is null, variable watch ignored");
        }
    }
    
    // ======= Utility Methods =======
    @JavascriptInterface
    public void onError(String errorMessage, String source) {
        Log.e(TAG, "JavaScript Error [" + source + "]: " + errorMessage);
        
        // Could send to crash reporting service here
        Activity activity = activityRef.get();
        if (activity != null) {
            Log.d(TAG, "Activity still alive, error can be handled");
        } else {
            Log.d(TAG, "Activity already destroyed, error handling skipped");
        }
    }
    
    @JavascriptInterface
    public void onLog(String message, String level) {
        switch (level.toLowerCase()) {
            case "error":
                Log.e(TAG, "JS: " + message);
                break;
            case "warn":
                Log.w(TAG, "JS: " + message);
                break;
            case "info":
                Log.i(TAG, "JS: " + message);
                break;
            default:
                Log.d(TAG, "JS: " + message);
        }
    }
}