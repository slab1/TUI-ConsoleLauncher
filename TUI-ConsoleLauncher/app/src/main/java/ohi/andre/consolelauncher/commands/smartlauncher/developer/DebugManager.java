package ohi.andre.consolelauncher.commands.smartlauncher.developer;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DebugManager - Handles debugging operations
 * Manages breakpoints, debug sessions, and variable inspection
 */
public class DebugManager {
    private static final String TAG = "DebugManager";
    
    private boolean isDebugging = false;
    private String currentStatus = "stopped";
    private String currentFilePath = "";
    private int currentLine = 0;
    private final Map<String, List<Integer>> breakpoints = new HashMap<>();
    private final Map<String, VariableInfo> watchedVariables = new HashMap<>();
    private final List<StackFrame> callStack = new ArrayList<>();
    
    public void toggleBreakpoint(String filePath, int lineNumber, boolean enabled) {
        Log.d(TAG, "Toggling breakpoint at " + filePath + ":" + lineNumber + " (enabled: " + enabled + ")");
        
        if (!breakpoints.containsKey(filePath)) {
            breakpoints.put(filePath, new ArrayList<>());
        }
        
        List<Integer> fileBreakpoints = breakpoints.get(filePath);
        if (enabled) {
            if (!fileBreakpoints.contains(lineNumber)) {
                fileBreakpoints.add(lineNumber);
                Log.i(TAG, "Breakpoint set at " + filePath + ":" + lineNumber);
            }
        } else {
            fileBreakpoints.remove(Integer.valueOf(lineNumber));
            Log.i(TAG, "Breakpoint removed from " + filePath + ":" + lineNumber);
        }
    }
    
    public boolean isBreakpointSet(String filePath, int lineNumber) {
        List<Integer> fileBreakpoints = breakpoints.get(filePath);
        return fileBreakpoints != null && fileBreakpoints.contains(lineNumber);
    }
    
    public void start() {
        Log.d(TAG, "Starting debug session");
        isDebugging = true;
        currentStatus = "running";
        
        // Initialize call stack
        callStack.clear();
        callStack.add(new StackFrame("main", 1, "global scope"));
        
        Log.i(TAG, "Debug session started");
    }
    
    public void continueExecution() {
        Log.d(TAG, "Continuing debug execution");
        currentStatus = "running";
        
        // Simulate moving to next breakpoint
        moveToNextBreakpoint();
        
        Log.i(TAG, "Debug execution continued");
    }
    
    public void stepOver() {
        Log.d(TAG, "Stepping over in debug");
        currentStatus = "stepping";
        currentLine++;
        
        // Simulate stepping over
        updateCallStack();
        
        Log.i(TAG, "Stepped over, now at line " + currentLine);
    }
    
    public void stepInto() {
        Log.d(TAG, "Stepping into debug");
        currentStatus = "stepping";
        currentLine++;
        
        // Simulate stepping into function
        if (currentLine % 5 == 0) {
            callStack.add(new StackFrame("function_" + currentLine, currentLine, "function scope"));
        }
        
        updateCallStack();
        
        Log.i(TAG, "Stepped into, now at line " + currentLine);
    }
    
    public void stepOut() {
        Log.d(TAG, "Stepping out debug");
        currentStatus = "stepping";
        
        // Simulate stepping out of function
        if (!callStack.isEmpty() && callStack.size() > 1) {
            callStack.remove(callStack.size() - 1);
        }
        currentLine++;
        
        updateCallStack();
        
        Log.i(TAG, "Stepped out, now at line " + currentLine);
    }
    
    public void stop() {
        Log.d(TAG, "Stopping debug session");
        isDebugging = false;
        currentStatus = "stopped";
        currentFilePath = "";
        currentLine = 0;
        callStack.clear();
        
        Log.i(TAG, "Debug session stopped");
    }
    
    public void addWatch(String variableName, String expression) {
        Log.d(TAG, "Adding watch for: " + variableName);
        
        VariableInfo varInfo = new VariableInfo(variableName, expression);
        varInfo.value = evaluateExpression(expression);
        varInfo.type = getVariableType(expression);
        
        watchedVariables.put(variableName, varInfo);
        
        Log.i(TAG, "Watch added for: " + variableName);
    }
    
    public void removeWatch(String variableName) {
        Log.d(TAG, "Removing watch for: " + variableName);
        watchedVariables.remove(variableName);
        Log.i(TAG, "Watch removed for: " + variableName);
    }
    
    public List<Integer> getActiveBreakpoints() {
        List<Integer> allBreakpoints = new ArrayList<>();
        for (List<Integer> fileBreakpoints : breakpoints.values()) {
            allBreakpoints.addAll(fileBreakpoints);
        }
        return allBreakpoints;
    }
    
    public Map<String, VariableInfo> getWatchedVariables() {
        return new HashMap<>(watchedVariables);
    }
    
    public List<StackFrame> getCallStack() {
        return new ArrayList<>(callStack);
    }
    
    public String getCurrentStatus() {
        return currentStatus;
    }
    
    public boolean isDebugging() {
        return isDebugging;
    }
    
    public void cleanup() {
        isDebugging = false;
        currentStatus = "stopped";
        breakpoints.clear();
        watchedVariables.clear();
        callStack.clear();
        Log.d(TAG, "Debug manager cleaned up");
    }
    
    // ======= Internal Helper Methods =======
    private void moveToNextBreakpoint() {
        // Simulate hitting the next breakpoint
        List<Integer> allBreakpoints = getActiveBreakpoints();
        if (!allBreakpoints.isEmpty()) {
            int nextBreakpoint = allBreakpoints.get(0);
            if (currentLine < nextBreakpoint) {
                currentLine = nextBreakpoint;
                Log.i(TAG, "Hit breakpoint at line " + currentLine);
            }
        }
    }
    
    private void updateCallStack() {
        // Update the current stack frame
        if (!callStack.isEmpty()) {
            StackFrame currentFrame = callStack.get(callStack.size() - 1);
            currentFrame.line = currentLine;
        }
    }
    
    private String evaluateExpression(String expression) {
        // Mock evaluation - in a real debugger, this would evaluate the actual expression
        if (expression.contains("x") || expression.contains("variable")) {
            return "42";
        } else if (expression.contains("name") || expression.contains("string")) {
            return "\"Hello World\"";
        } else if (expression.contains("count") || expression.contains("number")) {
            return "123";
        } else if (expression.contains("obj") || expression.contains("object")) {
            return "{x: 10, y: 20}";
        } else if (expression.contains("arr") || expression.contains("array")) {
            return "[1, 2, 3, 4, 5]";
        } else if (expression.contains("bool") || expression.contains("flag")) {
            return "true";
        } else {
            return "undefined";
        }
    }
    
    private String getVariableType(String expression) {
        if (expression.contains("x") || expression.contains("count") || expression.contains("number")) {
            return "number";
        } else if (expression.contains("name") || expression.contains("string")) {
            return "string";
        } else if (expression.contains("obj") || expression.contains("object")) {
            return "object";
        } else if (expression.contains("arr") || expression.contains("array")) {
            return "array";
        } else if (expression.contains("bool") || expression.contains("flag")) {
            return "boolean";
        } else {
            return "undefined";
        }
    }
    
    public void handleCommand(String command, String params) {
        Log.d(TAG, "Handling debug command: " + command);
        
        try {
            JSONObject jsonParams = new JSONObject(params);
            
            switch (command) {
                case "start":
                    start();
                    break;
                case "continue":
                    continueExecution();
                    break;
                case "stepOver":
                    stepOver();
                    break;
                case "stepInto":
                    stepInto();
                    break;
                case "stepOut":
                    stepOut();
                    break;
                case "stop":
                    stop();
                    break;
                case "addWatch":
                    String varName = jsonParams.optString("variableName", "");
                    String expression = jsonParams.optString("expression", varName);
                    if (!varName.isEmpty()) {
                        addWatch(varName, expression);
                    }
                    break;
                case "removeWatch":
                    String watchVarName = jsonParams.optString("variableName", "");
                    if (!watchVarName.isEmpty()) {
                        removeWatch(watchVarName);
                    }
                    break;
                default:
                    Log.w(TAG, "Unknown debug command: " + command);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error handling debug command: " + command, e);
        }
    }
    
    // ======= Data Classes =======
    public static class VariableInfo {
        public String name;
        public String expression;
        public String value;
        public String type;
        public boolean watchPoint = false;
        
        public VariableInfo(String name, String expression) {
            this.name = name;
            this.expression = expression;
        }
        
        public JSONObject toJSON() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("name", name);
            json.put("expression", expression);
            json.put("value", value);
            json.put("type", type);
            json.put("watchPoint", watchPoint);
            return json;
        }
        
        public static VariableInfo fromJSON(JSONObject json) throws JSONException {
            VariableInfo info = new VariableInfo(json.getString("name"), json.getString("expression"));
            info.value = json.getString("value");
            info.type = json.getString("type");
            info.watchPoint = json.getBoolean("watchPoint");
            return info;
        }
    }
    
    public static class StackFrame {
        public String functionName;
        public int line;
        public String scope;
        
        public StackFrame(String functionName, int line, String scope) {
            this.functionName = functionName;
            this.line = line;
            this.scope = scope;
        }
        
        public JSONObject toJSON() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("functionName", functionName);
            json.put("line", line);
            json.put("scope", scope);
            return json;
        }
        
        public static StackFrame fromJSON(JSONObject json) throws JSONException {
            return new StackFrame(
                json.getString("functionName"),
                json.getInt("line"),
                json.getString("scope")
            );
        }
    }
    
    // ======= Getters for Testing =======
    public String getServerStatus() {
        return currentStatus;
    }
    
    public String getLastRequestId() {
        return "last-debug-request";
    }
}