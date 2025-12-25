package ohi.andre.consolelauncher.commands.smartlauncher.developer;

import android.content.Context;
import android.webkit.WebView;

import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test utilities for MonacoEditorActivity Phase 3 testing
 * 
 * Provides common test utilities, mock builders, and helper methods
 * for consistent and efficient testing across all test categories.
 */
public class MonacoEditorTestUtils {

    // =============================================================================
    // Mock Builder Utilities
    // =============================================================================

    /**
     * Create a mock MonacoEditorActivity with all managers initialized
     */
    public static MonacoEditorActivity createMockActivity() {
        MonacoEditorActivity activity = Mockito.mock(MonacoEditorActivity.class);
        
        // Create real manager instances (not mocks) for testing
        MonacoEditorActivity.LanguageServerManager lspManager = activity.new LanguageServerManager();
        MonacoEditorActivity.DebugManager debugManager = activity.new DebugManager();
        MonacoEditorActivity.EnhancedMonacoJavaScriptInterface jsInterface = activity.new EnhancedMonacoJavaScriptInterface();
        
        // Use Mockito to spy on real instances for verification
        MonacoEditorActivity.LanguageServerManager spiedLspManager = Mockito.spy(lspManager);
        MonacoEditorActivity.DebugManager spiedDebugManager = Mockito.spy(debugManager);
        MonacoEditorActivity.EnhancedMonacoJavaScriptInterface spiedJsInterface = Mockito.spy(jsInterface);
        
        // Return mocked activity that returns the spied instances
        Mockito.when(activity.new LanguageServerManager()).thenReturn(spiedLspManager);
        Mockito.when(activity.new DebugManager()).thenReturn(spiedDebugManager);
        Mockito.when(activity.new EnhancedMonacoJavaScriptInterface()).thenReturn(spiedJsInterface);
        
        return activity;
    }

    /**
     * Create a mock WebView with common configurations
     */
    public static WebView createMockWebView(Context context) {
        WebView mockWebView = Mockito.mock(WebView.class);
        Mockito.when(mockWebView.getContext()).thenReturn(context);
        return mockWebView;
    }

    // =============================================================================
    // Test Data Generators
    // =============================================================================

    /**
     * Generate test LSP requests with various parameters
     */
    public static class LspTestDataGenerator {
        
        public static Map<String, String> generateCompletionRequests(int count) {
            Map<String, String> requests = new HashMap<>();
            for (int i = 0; i < count; i++) {
                String requestId = "completion-test-" + i;
                String params = String.format(
                    "{\"position\":{\"line\":%d,\"character\":%d},\"filePath\":\"/test/File%d.java\"}",
                    i % 20, i % 10, i % 5
                );
                requests.put(requestId, params);
            }
            return requests;
        }
        
        public static Map<String, String> generateDiagnosticsRequests(int count) {
            Map<String, String> requests = new HashMap<>();
            for (int i = 0; i < count; i++) {
                String requestId = "diagnostics-test-" + i;
                String params = String.format("{\"filePath\":\"/test/DiagnosticFile%d.java\"}", i % 10);
                requests.put(requestId, params);
            }
            return requests;
        }
        
        public static Map<String, String> generateHoverRequests(int count) {
            Map<String, String> requests = new HashMap<>();
            for (int i = 0; i < count; i++) {
                String requestId = "hover-test-" + i;
                String params = String.format(
                    "{\"position\":{\"line\":%d,\"character\":%d},\"filePath\":\"/test/HoverFile%d.java\"}",
                    i % 15, i % 8, i % 7
                );
                requests.put(requestId, params);
            }
            return requests;
        }
    }

    /**
     * Generate test debug scenarios
     */
    public static class DebugTestDataGenerator {
        
        public static List<String> generateDebugCommands(int count) {
            List<String> commands = new ArrayList<>();
            String[] commandTypes = {"start", "stop", "continue", "stepOver", "stepInto", "stepOut"};
            
            for (int i = 0; i < count; i++) {
                String command = commandTypes[i % commandTypes.length];
                String params = String.format("{\"filePath\":\"/test/DebugFile%d.java\",\"line\":%d}", i % 10, i % 25);
                commands.add(command + ":" + params);
            }
            return commands;
        }
        
        public static List<String> generateBreakpointToggles(int count) {
            List<String> toggles = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String filePath = "/test/BreakpointFile" + (i % 8) + ".java";
                int lineNumber = i % 30;
                boolean enabled = i % 2 == 0;
                toggles.add(filePath + ":" + lineNumber + ":" + enabled);
            }
            return toggles;
        }
    }

    // =============================================================================
    // Performance Testing Utilities
    // =============================================================================

    /**
     * Measure execution time for test operations
     */
    public static class PerformanceTimer {
        private long startTime;
        private long endTime;
        
        public void start() {
            startTime = System.currentTimeMillis();
        }
        
        public void stop() {
            endTime = System.currentTimeMillis();
        }
        
        public long getDuration() {
            return endTime - startTime;
        }
        
        public boolean isWithinLimit(long maxDuration) {
            return getDuration() <= maxDuration;
        }
        
        @Override
        public String toString() {
            return "PerformanceTimer{duration=" + getDuration() + "ms}";
        }
    }

    /**
     * Wait for asynchronous operations to complete
     */
    public static boolean waitForOperation(CountDownLatch latch, long timeoutMs) {
        try {
            return latch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    // =============================================================================
    // Validation Utilities
    // =============================================================================

    /**
     * Validate LSP request format
     */
    public static boolean isValidLspRequest(String requestId, String method, String params) {
        return requestId != null && !requestId.isEmpty() &&
               method != null && !method.isEmpty() &&
               params != null;
    }

    /**
     * Validate debug command format
     */
    public static boolean isValidDebugCommand(String command, String params) {
        return command != null && !command.isEmpty() &&
               params != null;
    }

    /**
     * Validate breakpoint specification
     */
    public static boolean isValidBreakpoint(String filePath, int lineNumber) {
        return filePath != null && !filePath.isEmpty() &&
               lineNumber >= 0;
    }

    // =============================================================================
    // Test Assertion Helpers
    // =============================================================================

    /**
     * Assert that an operation completes within a time limit
     */
    public static void assertCompletesWithin(Runnable operation, long maxTimeMs, String message) {
        PerformanceTimer timer = new PerformanceTimer();
        timer.start();
        
        operation.run();
        
        timer.stop();
        assertTrue(message + " (actual time: " + timer.getDuration() + "ms)", 
                   timer.isWithinLimit(maxTimeMs));
    }

    /**
     * Assert that multiple operations complete within a time limit
     */
    public static void assertMultipleOperationsComplete(List<Runnable> operations, 
                                                        long maxTimeMs, 
                                                        String message) {
        PerformanceTimer timer = new PerformanceTimer();
        timer.start();
        
        for (Runnable operation : operations) {
            operation.run();
        }
        
        timer.stop();
        assertTrue(message + " (actual time: " + timer.getDuration() + "ms)", 
                   timer.isWithinLimit(maxTimeMs));
    }

    // =============================================================================
    // Error Simulation Utilities
    // =============================================================================

    /**
     * Simulate various error conditions for testing error handling
     */
    public static class ErrorSimulator {
        
        public static String simulateInvalidLspMethod() {
            return "invalid/lsp/method";
        }
        
        public static String simulateInvalidDebugCommand() {
            return "invalid_debug_command";
        }
        
        public static String simulateEmptyParams() {
            return "";
        }
        
        public static String simulateMalformedJson() {
            return "{invalid json";
        }
        
        public static int simulateInvalidLineNumber() {
            return -1;
        }
        
        public static String simulateInvalidFilePath() {
            return "";
        }
    }

    // =============================================================================
    // Resource Cleanup Utilities
    // =============================================================================

    /**
     * Clean up test resources
     */
    public static void cleanupTestResources(MonacoEditorActivity activity) {
        if (activity != null) {
            // Clean up managers
            try {
                MonacoEditorActivity.LanguageServerManager lspManager = activity.new LanguageServerManager();
                MonacoEditorActivity.DebugManager debugManager = activity.new DebugManager();
                
                if (lspManager != null) {
                    lspManager.cleanup();
                }
                
                if (debugManager != null) {
                    debugManager.cleanup();
                }
            } catch (Exception e) {
                // Ignore cleanup errors in tests
            }
        }
    }

    /**
     * Reset all test state
     */
    public static void resetAllTestState() {
        // Clear any static test data
        // Reset counters
        // Clean up temporary files
        System.gc(); // Suggest garbage collection
    }

    // =============================================================================
    // Test Configuration
    // =============================================================================

    /**
     * Test configuration constants
     */
    public static class TestConfig {
        // Performance thresholds (in milliseconds)
        public static final long LSP_REQUEST_TIMEOUT = 1000;
        public static final long DEBUG_COMMAND_TIMEOUT = 500;
        public static final long UI_OPERATION_TIMEOUT = 200;
        public static final long INTEGRATION_TEST_TIMEOUT = 5000;
        
        // Test data limits
        public static final int MAX_CONCURRENT_OPERATIONS = 100;
        public static final int MAX_BREAKPOINTS = 50;
        public static final int MAX_FILE_PATHS = 20;
        
        // Memory thresholds
        public static final long MAX_MEMORY_USAGE_MB = 50;
        
        // UI test constants
        public static final int TOUCH_COORDINATE_X = 100;
        public static final int TOUCH_COORDINATE_Y = 200;
        public static final String DEFAULT_SEARCH_QUERY = "test";
    }

    // =============================================================================
    // Logging Utilities
    // =============================================================================

    /**
     * Log test execution details
     */
    public static void logTestExecution(String testName, long executionTime, boolean success) {
        String status = success ? "PASS" : "FAIL";
        System.out.println(String.format("[%s] %s - %dms", status, testName, executionTime));
    }

    /**
     * Log performance metrics
     */
    public static void logPerformanceMetrics(String operation, long minTime, long maxTime, long avgTime) {
        System.out.println(String.format("Performance Metrics for %s:", operation));
        System.out.println(String.format("  Min: %dms, Max: %dms, Avg: %dms", minTime, maxTime, avgTime));
    }
}