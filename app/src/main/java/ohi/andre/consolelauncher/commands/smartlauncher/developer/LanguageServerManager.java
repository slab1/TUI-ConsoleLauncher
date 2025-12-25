package ohi.andre.consolelauncher.commands.smartlauncher.developer;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * LanguageServerManager - Handles Language Server Protocol (LSP) operations
 * Manages language-specific features like auto-completion, hover, diagnostics
 */
public class LanguageServerManager {
    private static final String TAG = "LanguageServerManager";
    
    private boolean isInitialized = false;
    private final Map<String, DocumentInfo> openDocuments = new HashMap<>();
    private String currentServerPath = "";
    private boolean isConnected = false;
    
    public String handleCompletion(String requestId, JSONObject params) {
        try {
            Log.d(TAG, "Handling completion request: " + requestId);
            
            // Extract parameters
            JSONObject position = params.getJSONObject("position");
            String textDocument = params.getString("textDocument");
            
            int line = position.getInt("line");
            int character = position.getInt("character");
            
            // Generate mock completion items
            JSONArray completions = generateCompletions(textDocument, line, character);
            
            JSONObject response = new JSONObject();
            response.put("id", requestId);
            response.put("result", completions);
            
            return response.toString();
            
        } catch (JSONException e) {
            Log.e(TAG, "Error handling completion request", e);
            return createErrorResponse(requestId, "Failed to process completion request: " + e.getMessage());
        }
    }
    
    public String handleDefinition(String requestId, JSONObject params) {
        try {
            Log.d(TAG, "Handling definition request: " + requestId);
            
            JSONObject position = params.getJSONObject("position");
            String textDocument = params.getString("textDocument");
            
            int line = position.getInt("line");
            int character = position.getInt("character");
            
            // Generate mock definition locations
            JSONArray definitions = generateDefinitions(textDocument, line, character);
            
            JSONObject response = new JSONObject();
            response.put("id", requestId);
            response.put("result", definitions);
            
            return response.toString();
            
        } catch (JSONException e) {
            Log.e(TAG, "Error handling definition request", e);
            return createErrorResponse(requestId, "Failed to process definition request: " + e.getMessage());
        }
    }
    
    public String handleHover(String requestId, JSONObject params) {
        try {
            Log.d(TAG, "Handling hover request: " + requestId);
            
            JSONObject position = params.getJSONObject("position");
            String textDocument = params.getString("textDocument");
            
            int line = position.getInt("line");
            int character = position.getInt("character");
            
            // Generate mock hover information
            JSONObject hover = generateHover(textDocument, line, character);
            
            JSONObject response = new JSONObject();
            response.put("id", requestId);
            response.put("result", hover);
            
            return response.toString();
            
        } catch (JSONException e) {
            Log.e(TAG, "Error handling hover request", e);
            return createErrorResponse(requestId, "Failed to process hover request: " + e.getMessage());
        }
    }
    
    public String handleDiagnostics(String requestId, JSONObject params) {
        try {
            Log.d(TAG, "Handling diagnostics request: " + requestId);
            
            String textDocument = params.getString("textDocument");
            
            // Generate mock diagnostics
            JSONArray diagnostics = generateDiagnostics(textDocument);
            
            JSONObject response = new JSONObject();
            response.put("id", requestId);
            response.put("result", diagnostics);
            
            return response.toString();
            
        } catch (JSONException e) {
            Log.e(TAG, "Error handling diagnostics request", e);
            return createErrorResponse(requestId, "Failed to process diagnostics request: " + e.getMessage());
        }
    }
    
    public String handleInitialize(String requestId, JSONObject params) {
        try {
            Log.d(TAG, "Initializing Language Server Protocol");
            
            isInitialized = true;
            isConnected = true;
            
            JSONObject capabilities = new JSONObject();
            
            // Text document capabilities
            JSONObject textDocument = new JSONObject();
            
            // Completion capabilities
            JSONObject completion = new JSONObject();
            completion.put("dynamicRegistration", true);
            completion.put("completionItem", new JSONObject()
                .put("snippetSupport", true)
                .put("commitCharactersSupport", true)
                .put("documentationFormat", new JSONArray().put("markdown").put("plaintext"))
                .put("deprecatedSupport", true)
                .put("preselectSupport", true));
            textDocument.put("completion", completion);
            
            // Hover capabilities
            JSONObject hover = new JSONObject();
            hover.put("dynamicRegistration", true);
            hover.put("contentFormat", new JSONArray().put("markdown").put("plaintext"));
            textDocument.put("hover", hover);
            
            // Definition capabilities
            JSONObject definition = new JSONObject();
            definition.put("dynamicRegistration", true);
            textDocument.put("definition", definition);
            
            // Diagnostics capabilities
            JSONObject diagnostics = new JSONObject();
            diagnostics.put("dynamicRegistration", true);
            textDocument.put("diagnostics", diagnostics);
            
            capabilities.put("textDocument", textDocument);
            
            JSONObject response = new JSONObject();
            response.put("id", requestId);
            response.put("result", new JSONObject()
                .put("capabilities", capabilities));
            
            return response.toString();
            
        } catch (JSONException e) {
            Log.e(TAG, "Error initializing LSP", e);
            return createErrorResponse(requestId, "Failed to initialize LSP: " + e.getMessage());
        }
    }
    
    public String handleShutdown(String requestId, JSONObject params) {
        try {
            Log.d(TAG, "Shutting down Language Server Protocol");
            
            isInitialized = false;
            isConnected = false;
            openDocuments.clear();
            
            JSONObject response = new JSONObject();
            response.put("id", requestId);
            response.put("result", null);
            
            return response.toString();
            
        } catch (JSONException e) {
            Log.e(TAG, "Error shutting down LSP", e);
            return createErrorResponse(requestId, "Failed to shutdown LSP: " + e.getMessage());
        }
    }
    
    public void documentOpened(String uri, String content) {
        Log.d(TAG, "Document opened: " + uri);
        openDocuments.put(uri, new DocumentInfo(uri, content));
    }
    
    public void documentChanged(String uri, String content) {
        Log.d(TAG, "Document changed: " + uri);
        DocumentInfo docInfo = openDocuments.get(uri);
        if (docInfo != null) {
            docInfo.content = content;
            docInfo.version++;
        }
    }
    
    public void documentClosed(String uri) {
        Log.d(TAG, "Document closed: " + uri);
        openDocuments.remove(uri);
    }
    
    // ======= Mock Data Generation =======
    private JSONArray generateCompletions(String document, int line, int character) throws JSONException {
        JSONArray completions = new JSONArray();
        
        // Common JavaScript completions
        completions.put(createCompletionItem("console.log", "console.log(${1:message});", "Function", "Prints a message to the console"));
        completions.put(createCompletionItem("console.error", "console.error(${1:error});", "Function", "Prints an error message to the console"));
        completions.put(createCompletionItem("console.warn", "console.warn(${1:warning});", "Function", "Prints a warning message to the console"));
        completions.put(createCompletionItem("console.info", "console.info(${1:info});", "Function", "Prints an info message to the console"));
        completions.put(createCompletionItem("console.table", "console.table(${1:data});", "Function", "Displays tabular data as a table"));
        completions.put(createCompletionItem("console.time", "console.time(${1:label});", "Function", "Starts a timer"));
        completions.put(createCompletionItem("console.timeEnd", "console.timeEnd(${1:label});", "Function", "Stops a timer"));
        completions.put(createCompletionItem("console.dir", "console.dir(${1:object});", "Function", "Prints object properties"));
        completions.put(createCompletionItem("console.trace", "console.trace(${1:message});", "Function", "Prints a stack trace"));
        completions.put(createCompletionItem("console.clear", "console.clear();", "Function", "Clears the console"));
        
        return completions;
    }
    
    private JSONArray generateDefinitions(String document, int line, int character) throws JSONException {
        JSONArray definitions = new JSONArray();
        
        JSONObject definition = new JSONObject();
        definition.put("uri", document);
        definition.put("range", createRange(line - 1, character - 1, line - 1, character + 5));
        definitions.put(definition);
        
        return definitions;
    }
    
    private JSONObject generateHover(String document, int line, int character) throws JSONException {
        JSONObject hover = new JSONObject();
        
        // Markdown content
        StringBuilder contents = new StringBuilder();
        contents.append("**Console.log Method**\n\n");
        contents.append("Prints a message to the console.\n\n");
        contents.append("```javascript\n");
        contents.append("console.log(message);\n");
        contents.append("```\n\n");
        contents.append("**Parameters:**\n");
        contents.append("- `message`: The message to log (any type)\n\n");
        contents.append("**Example:**\n");
        contents.append("```javascript\n");
        contents.append("console.log('Hello, world!');\n");
        contents.append("console.log({ name: 'John', age: 30 });\n");
        contents.append("```");
        
        hover.put("contents", contents.toString());
        hover.put("range", createRange(line - 1, character - 1, line - 1, character + 1));
        
        return hover;
    }
    
    private JSONArray generateDiagnostics(String document) throws JSONException {
        JSONArray diagnostics = new JSONArray();
        
        // Simulate some common issues
        JSONObject unusedVar = new JSONObject();
        unusedVar.put("range", createRange(5, 0, 5, 10));
        unusedVar.put("severity", 2); // Warning
        unusedVar.put("message", "Unused variable 'unusedVar'");
        unusedVar.put("source", "JavaScript");
        unusedVar.put("code", "unused-local");
        diagnostics.put(unusedVar);
        
        JSONObject missingSemicolon = new JSONObject();
        missingSemicolon.put("range", createRange(10, 15, 10, 16));
        missingSemicolon.put("severity", 1); // Error
        missingSemicolon.put("message", "Missing semicolon");
        missingSemicolon.put("source", "JavaScript");
        missingSemicolon.put("code", "missing-semicolon");
        diagnostics.put(missingSemicolon);
        
        JSONObject undefinedVariable = new JSONObject();
        undefinedVariable.put("range", createRange(15, 5, 15, 15));
        undefinedVariable.put("severity", 1); // Error
        undefinedVariable.put("message", "Variable 'undefinedVar' is not defined");
        undefinedVariable.put("source", "JavaScript");
        undefinedVariable.put("code", "undefined-variable");
        diagnostics.put(undefinedVariable);
        
        return diagnostics;
    }
    
    private JSONObject createCompletionItem(String label, String insertText, String kind, String detail) throws JSONException {
        JSONObject item = new JSONObject();
        item.put("label", label);
        item.put("kind", getCompletionKind(kind));
        item.put("detail", detail);
        item.put("insertText", insertText);
        item.put("insertTextFormat", 2); // Snippet format
        item.put("sortText", label);
        item.put("filterText", label);
        return item;
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
    
    private int getCompletionKind(String kind) {
        switch (kind.toLowerCase()) {
            case "function": return 3; // Function
            case "method": return 3; // Function
            case "property": return 10; // Property
            case "variable": return 6; // Variable
            case "class": return 4; // Class
            case "interface": return 11; // Interface
            case "keyword": return 14; // Keyword
            case "text": return 1; // Text
            default: return 1; // Text
        }
    }
    
    private String createErrorResponse(String requestId, String errorMessage) {
        try {
            JSONObject response = new JSONObject();
            response.put("id", requestId);
            response.put("error", new JSONObject()
                .put("code", -32602) // Invalid params
                .put("message", errorMessage));
            return response.toString();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create error response", e);
            return "{}";
        }
    }
    
    // ======= Getters =======
    public boolean isServerConnected() {
        return isConnected;
    }
    
    public String getServerPath() {
        return currentServerPath;
    }
    
    public String getServerStatus() {
        if (!isInitialized) {
            return "Not Initialized";
        } else if (isConnected) {
            return "Connected";
        } else {
            return "Disconnected";
        }
    }
    
    public String getLastRequestId() {
        // For testing purposes
        return "last-handled-request";
    }
    
    public void cleanup() {
        isInitialized = false;
        isConnected = false;
        openDocuments.clear();
        Log.d(TAG, "Language Server Protocol cleaned up");
    }
    
    // ======= Internal Classes =======
    private static class DocumentInfo {
        String uri;
        String content;
        int version;
        
        DocumentInfo(String uri, String content) {
            this.uri = uri;
            this.content = content;
            this.version = 1;
        }
    }
}