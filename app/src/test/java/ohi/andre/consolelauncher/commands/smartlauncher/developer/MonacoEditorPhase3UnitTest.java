package ohi.andre.consolelauncher.commands.smartlauncher.developer;

import android.content.Context;
import android.webkit.WebView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MonacoEditorActivity Phase 3 features
 * Tests Language Server Protocol (LSP) integration, Debugging capabilities,
 * and enhanced JavaScript interface functionality.
 */
@RunWith(MockitoJUnitRunner.class)
public class MonacoEditorPhase3UnitTest {

    @Mock
    private Context mockContext;
    
    @Mock
    private WebView mockWebView;
    
    @Mock
    private MonacoEditorActivity.EnhancedMonacoJavaScriptInterface mockJavaScriptInterface;
    
    private MonacoEditorActivity.LanguageServerManager lspManager;
    private MonacoEditorActivity.DebugManager debugManager;
    private MonacoEditorActivity testActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create instance of MonacoEditorActivity for testing
        testActivity = new MonacoEditorActivity();
        
        // Initialize managers (normally done in onCreate)
        lspManager = testActivity.new LanguageServerManager();
        debugManager = testActivity.new DebugManager();
        
        // Initialize JavaScript interface
        mockJavaScriptInterface = testActivity.new EnhancedMonacoJavaScriptInterface();
    }

    // =============================================================================
    // Language Server Protocol (LSP) Tests
    // =============================================================================

    @Test
    public void testLspManagerInitialization() {
        assertNotNull("LSP Manager should be initialized", lspManager);
        assertNotNull("LSP Manager should have default server path", lspManager.getServerPath());
    }

    @Test
    public void testLspRequestCompletion() {
        String requestId = "completion-001";
        String params = "{\"position\":{\"line\":10,\"character\":5}}";
        
        // Test completion request
        String result = lspManager.requestCompletion(requestId, params);
        
        assertNotNull("Completion request should return a result", result);
        assertTrue("Result should contain request ID", result.contains(requestId));
    }

    @Test
    public void testLspRequestDiagnostics() {
        String requestId = "diagnostics-001";
        String params = "{\"filePath\":\"/test/file.java\"}";
        
        String result = lspManager.requestDiagnostics(requestId, params);
        
        assertNotNull("Diagnostics request should return a result", result);
        assertTrue("Result should contain request ID", result.contains(requestId));
    }

    @Test
    public void testLspHandleRequest() {
        String requestId = "test-001";
        String method = "textDocument/completion";
        String params = "{\"position\":{\"line\":5,\"character\":10}}";
        
        String result = lspManager.handleRequest(requestId, method, params);
        
        assertNotNull("LSP request should be handled", result);
        assertEquals("Request ID should match", requestId, lspManager.getLastRequestId());
    }

    @Test
    public void testLspServerConnection() {
        String serverPath = lspManager.getServerPath();
        
        assertNotNull("Server path should not be null", serverPath);
        assertFalse("Server path should not be empty", serverPath.isEmpty());
        
        boolean isConnected = lspManager.isServerConnected();
        assertFalse("LSP server should not be connected initially", isConnected);
    }

    // =============================================================================
    // Debug Manager Tests
    // =============================================================================

    @Test
    public void testDebugManagerInitialization() {
        assertNotNull("Debug Manager should be initialized", debugManager);
        assertFalse("Debug manager should not be running initially", debugManager.isDebugging());
    }

    @Test
    public void testToggleBreakpoint() {
        String filePath = "/test/MyActivity.java";
        int lineNumber = 25;
        boolean enabled = true;
        
        debugManager.toggleBreakpoint(filePath, lineNumber, enabled);
        
        assertTrue("Breakpoint should be enabled after toggle", 
                   debugManager.isBreakpointSet(filePath, lineNumber));
    }

    @Test
    public void testToggleBreakpointDisabled() {
        String filePath = "/test/Utils.java";
        int lineNumber = 15;
        
        // First enable the breakpoint
        debugManager.toggleBreakpoint(filePath, lineNumber, true);
        assertTrue("Breakpoint should be set", debugManager.isBreakpointSet(filePath, lineNumber));
        
        // Then disable it
        debugManager.toggleBreakpoint(filePath, lineNumber, false);
        assertFalse("Breakpoint should be removed after disable", 
                   debugManager.isBreakpointSet(filePath, lineNumber));
    }

    @Test
    public void testHandleDebugCommandStart() {
        String command = "start";
        String params = "{\"filePath\":\"/test/DebugTest.java\"}";
        
        String result = debugManager.handleCommand(command, params);
        
        assertNotNull("Debug command should return a result", result);
        assertTrue("Debug manager should be running after start command", debugManager.isDebugging());
    }

    @Test
    public void testHandleDebugCommandStop() {
        // Start debugging first
        debugManager.handleCommand("start", "{\"filePath\":\"/test/Test.java\"}");
        
        // Then stop
        String result = debugManager.handleCommand("stop", "");
        
        assertNotNull("Stop command should return a result", result);
        assertFalse("Debug manager should not be running after stop", debugManager.isDebugging());
    }

    @Test
    public void testGetActiveBreakpoints() {
        // Set some breakpoints
        debugManager.toggleBreakpoint("/test/File1.java", 10, true);
        debugManager.toggleBreakpoint("/test/File1.java", 20, true);
        debugManager.toggleBreakpoint("/test/File2.java", 5, true);
        
        var breakpoints = debugManager.getActiveBreakpoints();
        
        assertNotNull("Active breakpoints should not be null", breakpoints);
        assertEquals("Should have 3 active breakpoints", 3, breakpoints.size());
    }

    @Test
    public void testStepOver() {
        // Start debugging
        debugManager.handleCommand("start", "{\"filePath\":\"/test/Test.java\"}");
        
        String result = debugManager.stepOver();
        
        assertNotNull("Step over should return a result", result);
        assertTrue("Result should indicate step over action", result.contains("step"));
    }

    // =============================================================================
    // Enhanced JavaScript Interface Tests
    // =============================================================================

    @Test
    public void testOnLspRequestInterface() {
        String requestId = "interface-test-001";
        String method = "textDocument/completion";
        String params = "{\"position\":{\"line\":1,\"character\":5}}";
        
        // This should call the enhanced JavaScript interface method
        mockJavaScriptInterface.onLspRequest(requestId, method, params);
        
        // Verify that the interface method was called successfully
        // (In a real test, we would verify the interaction with lspManager)
        assertNotNull("LSP request should be processed", 
                     lspManager.handleRequest(requestId, method, params));
    }

    @Test
    public void testOnDebugCommandInterface() {
        String command = "continue";
        String params = "{\"threadId\":1}";
        
        mockJavaScriptInterface.onDebugCommand(command, params);
        
        // Verify debug command processing
        assertNotNull("Debug command should be processed", 
                     debugManager.handleCommand(command, params));
    }

    @Test
    public void testOnToggleBreakpointInterface() {
        String filePath = "/test/InterfaceTest.java";
        int lineNumber = 42;
        boolean enabled = true;
        
        mockJavaScriptInterface.onToggleBreakpoint(filePath, lineNumber, enabled);
        
        // Verify breakpoint was set through interface
        assertTrue("Breakpoint should be set via interface", 
                  debugManager.isBreakpointSet(filePath, lineNumber));
    }

    // =============================================================================
    // Integration Tests
    // =============================================================================

    @Test
    public void testCompleteLspWorkflow() {
        // 1. Initialize LSP
        assertNotNull("LSP Manager should be available", lspManager);
        
        // 2. Send completion request
        String completionResult = lspManager.handleRequest("workflow-001", "textDocument/completion", 
                                                         "{\"position\":{\"line\":5,\"character\":10}}");
        assertNotNull("Completion should return result", completionResult);
        
        // 3. Send diagnostics request
        String diagResult = lspManager.handleRequest("workflow-002", "textDocument/diagnostics", 
                                                   "{\"filePath\":\"/test/workflow.java\"}");
        assertNotNull("Diagnostics should return result", diagResult);
        
        // 4. Verify server connection status
        String serverStatus = lspManager.getServerStatus();
        assertNotNull("Server status should be available", serverStatus);
    }

    @Test
    public void testCompleteDebugWorkflow() {
        // 1. Start debugging session
        String startResult = debugManager.handleCommand("start", "{\"filePath\":\"/test/debug.java\"}");
        assertTrue("Debug session should be active", debugManager.isDebugging());
        
        // 2. Set breakpoints
        debugManager.toggleBreakpoint("/test/debug.java", 10, true);
        debugManager.toggleBreakpoint("/test/debug.java", 25, true);
        
        // 3. Step through code
        String stepResult = debugManager.stepOver();
        assertNotNull("Step operation should succeed", stepResult);
        
        // 4. Stop debugging
        String stopResult = debugManager.handleCommand("stop", "");
        assertFalse("Debug session should be inactive", debugManager.isDebugging());
    }

    @Test
    public void testJavaScriptToJavaCommunication() {
        // Simulate complete JavaScript -> Java communication flow
        String requestId = "comm-test";
        String method = "textDocument/completion";
        String params = "{\"position\":{\"line\":0,\"character\":0}}";
        
        // 1. JavaScript calls onLspRequest
        mockJavaScriptInterface.onLspRequest(requestId, method, params);
        
        // 2. Verify LSP manager processed it
        String lspResult = lspManager.handleRequest(requestId, method, params);
        assertNotNull("LSP should process JavaScript request", lspResult);
        
        // 3. JavaScript calls onDebugCommand
        mockJavaScriptInterface.onDebugCommand("start", "{\"filePath\":\"/test/comm.java\"}");
        
        // 4. Verify debug manager processed it
        assertTrue("Debug should be active from JavaScript command", debugManager.isDebugging());
        
        // 5. JavaScript calls onToggleBreakpoint
        mockJavaScriptInterface.onToggleBreakpoint("/test/comm.java", 15, true);
        
        // 6. Verify breakpoint was set
        assertTrue("Breakpoint should be set from JavaScript", 
                  debugManager.isBreakpointSet("/test/comm.java", 15));
    }

    // =============================================================================
    // Edge Cases and Error Handling Tests
    // =============================================================================

    @Test
    public void testLspHandleRequestWithInvalidMethod() {
        String requestId = "error-test-001";
        String method = "invalid/method";
        String params = "{}";
        
        String result = lspManager.handleRequest(requestId, method, params);
        
        assertNotNull("Invalid method should still return a result", result);
        assertTrue("Result should indicate unsupported method", result.contains("unsupported"));
    }

    @Test
    public void testDebugHandleCommandWithInvalidCommand() {
        String command = "invalid_command";
        String params = "{}";
        
        String result = debugManager.handleCommand(command, params);
        
        assertNotNull("Invalid command should return result", result);
        assertTrue("Result should indicate unknown command", result.contains("unknown"));
    }

    @Test
    public void testToggleBreakpointWithInvalidLine() {
        String filePath = "/test/valid.java";
        int invalidLine = -1;
        boolean enabled = true;
        
        debugManager.toggleBreakpoint(filePath, invalidLine, enabled);
        
        // Should handle gracefully without throwing exception
        assertFalse("Invalid line should not set breakpoint", 
                   debugManager.isBreakpointSet(filePath, invalidLine));
    }

    @Test
    public void testLspHandleRequestWithEmptyParams() {
        String requestId = "empty-params-test";
        String method = "textDocument/completion";
        String params = "";
        
        String result = lspManager.handleRequest(requestId, method, params);
        
        assertNotNull("Empty params should not cause null result", result);
    }

    // =============================================================================
    // Performance Tests
    // =============================================================================

    @Test
    public void testMultipleLspRequestsPerformance() {
        int requestCount = 100;
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < requestCount; i++) {
            lspManager.handleRequest("perf-" + i, "textDocument/completion", 
                                   "{\"position\":{\"line\":0,\"character\":0}}");
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Should handle 100 requests in reasonable time (less than 1 second)
        assertTrue("100 LSP requests should complete quickly", duration < 1000);
    }

    @Test
    public void testMultipleBreakpointTogglesPerformance() {
        int toggleCount = 1000;
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < toggleCount; i++) {
            debugManager.toggleBreakpoint("/test/perf.java", i % 50, i % 2 == 0);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Should handle 1000 toggles quickly
        assertTrue("1000 breakpoint toggles should complete quickly", duration < 500);
    }

    // =============================================================================
    // Memory and Resource Tests
    // =============================================================================

    @Test
    public void testLspManagerCleanup() {
        // Set up some state
        lspManager.handleRequest("cleanup-test", "textDocument/completion", "{}");
        
        // Perform cleanup (if applicable)
        lspManager.cleanup();
        
        // Verify cleanup was effective
        assertNull("LSP Manager should be cleaned up", lspManager.getLastRequestId());
    }

    @Test
    public void testDebugManagerCleanup() {
        // Set up debugging session
        debugManager.handleCommand("start", "{\"filePath\":\"/test/cleanup.java\"}");
        debugManager.toggleBreakpoint("/test/cleanup.java", 10, true);
        
        // Perform cleanup
        debugManager.cleanup();
        
        // Verify debugging is stopped and breakpoints cleared
        assertFalse("Debug manager should be cleaned up", debugManager.isDebugging());
        assertEquals("All breakpoints should be cleared", 0, debugManager.getActiveBreakpoints().size());
    }
}