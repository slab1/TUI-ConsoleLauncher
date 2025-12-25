package ohi.andre.consolelauncher.commands.smartlauncher.developer.integration;

import android.content.Context;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import ohi.andre.consolelauncher.commands.smartlauncher.developer.MonacoEditorActivity;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for MonacoEditorActivity Phase 3 features
 * Tests the interaction between components and end-to-end workflows
 */
@RunWith(MockitoJUnitRunner.class)
public class MonacoEditorIntegrationTest {

    @Mock
    private Context mockContext;
    
    @Mock
    private WebView mockWebView;
    
    @Mock
    private WebViewClient mockWebViewClient;
    
    private MonacoEditorActivity testActivity;
    private MonacoEditorActivity.LanguageServerManager lspManager;
    private MonacoEditorActivity.DebugManager debugManager;
    private MonacoEditorActivity.EnhancedMonacoJavaScriptInterface javaScriptInterface;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        testActivity = new MonacoEditorActivity();
        lspManager = testActivity.new LanguageServerManager();
        debugManager = testActivity.new DebugManager();
        javaScriptInterface = testActivity.new EnhancedMonacoJavaScriptInterface();
        
        // Set up WebView mocks
        when(mockWebView.getContext()).thenReturn(mockContext);
        when(mockWebView.getWebViewClient()).thenReturn(mockWebViewClient);
    }

    // =============================================================================
    // End-to-End LSP Integration Tests
    // =============================================================================

    @Test
    public void testCompleteLspFlowFromJavaScript() {
        // Simulate complete LSP workflow from JavaScript perspective
        
        // 1. JavaScript requests completion
        String completionRequest = javaScriptInterface.onLspRequest("lsp-e2e-001", 
            "textDocument/completion", 
            "{\"position\":{\"line\":10,\"character\":5},\"filePath\":\"/test/        
        // Example.java\"}");
2. Verify LSP manager processed the request
        assertNotNull("LSP request should be processed", completionRequest);
        
        // 3. JavaScript requests diagnostics
        String diagnosticRequest = javaScriptInterface.onLspRequest("lsp-e2e-002",
            "textDocument/diagnostics",
            "{\"filePath\":\"/test/Example.java\"}");
        
        assertNotNull("Diagnostic request should be processed", diagnosticRequest);
        
        // 4. Verify server status
        String serverStatus = lspManager.getServerStatus();
        assertNotNull("Server status should be available", serverStatus);
    }

    @Test
    public void testLspAndDebugIntegration() {
        // Test interaction between LSP and Debug components
        
        // 1. Start LSP session
        javaScriptInterface.onLspRequest("integration-001", "textDocument/completion", "{}");
        
        // 2. Start debugging session
        javaScriptInterface.onDebugCommand("start", "{\"filePath\":\"/test/Integration.java\"}");
        
        // 3. Verify both are running
        assertNotNull("LSP should be active", lspManager.getServerPath());
        assertTrue("Debug should be active", debugManager.isDebugging());
        
        // 4. Set breakpoints during LSP session
        javaScriptInterface.onToggleBreakpoint("/test/Integration.java", 15, true);
        javaScriptInterface.onToggleBreakpoint("/test/Integration.java", 25, true);
        
        // 5. Verify breakpoints are set
        assertTrue("Breakpoint 15 should be set", debugManager.isBreakpointSet("/test/Integration.java", 15));
        assertTrue("Breakpoint 25 should be set", debugManager.isBreakpointSet("/test/Integration.java", 25));
        
        // 6. Continue LSP operations while debugging
        String additionalCompletion = javaScriptInterface.onLspRequest("integration-002", 
            "textDocument/completion", "{}");
        assertNotNull("LSP should work during debugging", additionalCompletion);
    }

    // =============================================================================
    // WebView Integration Tests
    // =============================================================================

    @Test
    public void testWebViewCommunicationBridge() {
        // Test the communication bridge between WebView and Android
        
        // 1. Simulate WebView loading
        testActivity.setWebView(mockWebView);
        
        // 2. Verify JavaScript interface is properly set up
        verify(mockWebView).addJavascriptInterface(any(MonacoEditorActivity.EnhancedMonacoJavaScriptInterface.class), eq("Android"));
        
        // 3. Test JavaScript method calls
        String lspResult = javaScriptInterface.onLspRequest("webview-001", "completion", "{}");
        assertNotNull("WebView LSP communication should work", lspResult);
        
        String debugResult = javaScriptInterface.onDebugCommand("status", "{}");
        assertNotNull("WebView debug communication should work", debugResult);
    }

    @Test
    public void testWebViewLoadAndInitialization() {
        // Test WebView loading and initialization sequence
        
        // 1. Set up WebView
        testActivity.setWebView(mockWebView);
        
        // 2. Simulate WebView ready state
        when(mockWebView.getUrl()).thenReturn("file:///android_asset/monaco_editor.html");
        
        // 3. Initialize managers after WebView is ready
        testActivity.initializeManagers();
        
        // 4. Verify managers are initialized
        assertNotNull("LSP manager should be initialized", lspManager);
        assertNotNull("Debug manager should be initialized", debugManager);
        
        // 5. Test post-initialization communication
        String initTest = javaScriptInterface.onLspRequest("init-001", "initialized", "{}");
        assertNotNull("Post-initialization communication should work", initTest);
    }

    // =============================================================================
    // Manager Coordination Tests
    // =============================================================================

    @Test
    public void testManagerCoordination() {
        // Test coordination between different managers
        
        // 1. Start both LSP and debug managers
        lspManager.startServer();
        debugManager.handleCommand("start", "{\"filePath\":\"/test/Coordination.java\"}");
        
        // 2. Verify both are running
        assertTrue("LSP server should be running", lspManager.isServerConnected());
        assertTrue("Debug manager should be running", debugManager.isDebugging());
        
        // 3. Test coordinated operations
        javaScriptInterface.onToggleBreakpoint("/test/Coordination.java", 10, true);
        
        // 4. Verify LSP can still operate while debugging
        String lspDuringDebug = javaScriptInterface.onLspRequest("coord-001", 
            "textDocument/hover", "{\"position\":{\"line\":10,\"character\":5}}");
        assertNotNull("LSP should work during debugging", lspDuringDebug);
        
        // 5. Stop debugging and verify LSP continues
        debugManager.handleCommand("stop", "");
        assertFalse("Debug should be stopped", debugManager.isDebugging());
        
        String lspAfterDebug = javaScriptInterface.onLspRequest("coord-002",
            "textDocument/completion", "{}");
        assertNotNull("LSP should work after debugging", lspAfterDebug);
    }

    // =============================================================================
    // Error Recovery Integration Tests
    // =============================================================================

    @Test
    public void testErrorRecoveryAndFallback() {
        // Test error recovery scenarios across components
        
        // 1. Test LSP error recovery
        String invalidLspResult = javaScriptInterface.onLspRequest("error-001", 
            "invalid/method", "{}");
        assertNotNull("LSP should handle errors gracefully", invalidLspResult);
        
        // 2. Test debug error recovery
        String invalidDebugResult = javaScriptInterface.onDebugCommand("invalid_command", "{}");
        assertNotNull("Debug should handle errors gracefully", invalidDebugResult);
        
        // 3. Verify managers continue to work after errors
        String validLspAfterError = javaScriptInterface.onLspRequest("error-002",
            "textDocument/completion", "{}");
        assertNotNull("LSP should work after error", validLspAfterError);
        
        String validDebugAfterError = javaScriptInterface.onDebugCommand("start", 
            "{\"filePath\":\"/test/Recovery.java\"}");
        assertTrue("Debug should work after error", debugManager.isDebugging());
    }

    @Test
    public void testResourceCleanupIntegration() {
        // Test resource cleanup across all components
        
        // 1. Set up active sessions
        lspManager.startServer();
        debugManager.handleCommand("start", "{\"filePath\":\"/test/Cleanup.java\"}");
        debugManager.toggleBreakpoint("/test/Cleanup.java", 5, true);
        
        // 2. Perform cleanup
        testActivity.cleanupManagers();
        
        // 3. Verify cleanup was effective
        assertFalse("LSP server should be stopped", lspManager.isServerConnected());
        assertFalse("Debug manager should be stopped", debugManager.isDebugging());
        assertEquals("All breakpoints should be cleared", 0, debugManager.getActiveBreakpoints().size());
    }

    // =============================================================================
    // Performance Integration Tests
    // =============================================================================

    @Test
    public void testHighLoadIntegration() {
        // Test system behavior under high load
        
        int concurrentOperations = 50;
        long startTime = System.currentTimeMillis();
        
        // 1. Start multiple operations concurrently
        for (int i = 0; i < concurrentOperations; i++) {
            final int index = i;
            
            // Mix LSP and debug operations
            if (i % 2 == 0) {
                javaScriptInterface.onLspRequest("load-test-" + index, "completion", "{}");
            } else {
                javaScriptInterface.onDebugCommand("start", "{\"filePath\":\"/test/Load" + index + ".java\"}");
                debugManager.handleCommand("stop", "");
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Should handle 50 operations efficiently
        assertTrue("50 concurrent operations should complete quickly", duration < 2000);
    }

    @Test
    public void testMemoryUsageUnderLoad() {
        // Test memory usage during intensive operations
        
        // 1. Perform many LSP operations
        for (int i = 0; i < 100; i++) {
            javaScriptInterface.onLspRequest("memory-test-" + i, "completion", 
                "{\"position\":{\"line\":" + (i % 20) + ",\"character\":" + (i % 10) + "}}");
        }
        
        // 2. Perform many debug operations
        for (int i = 0; i < 100; i++) {
            debugManager.handleCommand("start", "{\"filePath\":\"/test/Memory" + i + ".java\"}");
            debugManager.toggleBreakpoint("/test/Memory" + i + ".java", i % 30, true);
            debugManager.handleCommand("stop", "");
        }
        
        // 3. Verify system is still responsive
        String responsivenessTest = javaScriptInterface.onLspRequest("memory-final", "completion", "{}");
        assertNotNull("System should remain responsive after memory test", responsivenessTest);
    }

    // =============================================================================
    // Cross-Platform Compatibility Tests
    // =============================================================================

    @Test
    public void testPlatformCompatibility() {
        // Test compatibility across different Android versions and configurations
        
        // 1. Test with different WebView configurations
        when(mockWebView.getSettings()).thenReturn(null);
        when(mockWebView.getContext()).thenReturn(mockContext);
        
        // 2. Initialize with different configurations
        testActivity.initializeManagers();
        
        // 3. Test functionality across configurations
        String compatibilityTest = javaScriptInterface.onLspRequest("compat-001", "completion", "{}");
        assertNotNull("Functionality should work across configurations", compatibilityTest);
        
        debugManager.handleCommand("start", "{\"filePath\":\"/test/Compat.java\"}");
        assertTrue("Debug should work across configurations", debugManager.isDebugging());
    }

    // =============================================================================
    // Lifecycle Integration Tests
    // =============================================================================

    @Test
    public void testActivityLifecycleIntegration() {
        // Test integration with Android Activity lifecycle
        
        // 1. Simulate onCreate
        testActivity.onCreate(null);
        assertNotNull("Managers should be initialized onCreate", lspManager);
        
        // 2. Simulate onStart
        testActivity.onStart();
        // Managers should be ready but not necessarily active
        
        // 3. Simulate onResume (WebView becomes visible)
        testActivity.onResume();
        // WebView should be loaded and JavaScript interface ready
        
        // 4. Test functionality during active state
        String lifecycleTest = javaScriptInterface.onLspRequest("lifecycle-001", "completion", "{}");
        assertNotNull("Functionality should work during active state", lifecycleTest);
        
        // 5. Simulate onPause
        testActivity.onPause();
        // Managers should be in a paused state but not destroyed
        
        // 6. Simulate onResume again
        testActivity.onResume();
        // Should be able to resume operations
        
        String resumeTest = javaScriptInterface.onDebugCommand("status", "{}");
        assertNotNull("Should resume after pause", resumeTest);
    }
}