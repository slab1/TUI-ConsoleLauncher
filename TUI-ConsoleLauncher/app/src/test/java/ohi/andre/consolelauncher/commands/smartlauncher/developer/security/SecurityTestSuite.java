package ohi.andre.consolelauncher.commands.smartlauncher.developer.security;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

/**
 * OWASP Security Compliance Test Suite
 * 
 * Tests security components for:
 * - Input validation
 * - URL validation
 * - File path validation
 * - XSS prevention
 * - Injection prevention
 */
@RunWith(MockitoJUnitRunner.class)
public class SecurityTestSuite {

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ======= Input Validation Tests =======

    @Test
    public void testValidFilePath() {
        String validPath = "src/main/java/Test.java";
        String result = InputValidator.validateFilePath(validPath);
        assertNotNull("Valid file path should pass", result);
        assertEquals(validPath, result);
    }

    @Test
    public void testInvalidFilePathTraversal() {
        String maliciousPath = "../../../etc/passwd";
        String result = InputValidator.validateFilePath(maliciousPath);
        assertNull("Path traversal should be blocked", result);
    }

    @Test
    public void testInvalidFilePathNull() {
        assertNull("Null path should be rejected", InputValidator.validateFilePath(null));
        assertNull("Empty path should be rejected", InputValidator.validateFilePath(""));
    }

    @Test
    public void testFilePathWithNullBytes() {
        String pathWithNull = "test\x00file.java";
        String result = InputValidator.validateFilePath(pathWithNull);
        assertNull("Path with null bytes should be rejected", result);
    }

    @Test
    public void testValidUrl() {
        String validUrl = "https://example.com/api";
        assertTrue("Valid HTTPS URL should pass", InputValidator.validateUrl(validUrl));
    }

    @Test
    public void testInvalidUrlScheme() {
        String invalidUrl = "javascript:alert('xss')";
        assertFalse("JavaScript URL should be blocked", InputValidator.validateUrl(invalidUrl));
    }

    @Test
    public void testInvalidUrlLocalhost() {
        String localhostUrl = "http://localhost:8080";
        assertFalse("Localhost URL should be blocked", InputValidator.validateUrl(localhostUrl));
    }

    @Test
    public void testMaliciousScriptInjection() {
        String maliciousInput = "<script>alert('XSS')</script>";
        assertTrue("Should detect script injection", 
            InputValidator.containsDangerousContent(maliciousInput));
    }

    @Test
    public void testMaliciousJavascriptProtocol() {
        String maliciousInput = "javascript:alert('XSS')";
        assertTrue("Should detect JavaScript protocol", 
            InputValidator.containsDangerousContent(maliciousInput));
    }

    @Test
    public void testMaliciousSqlInjection() {
        String maliciousInput = "'; DROP TABLE users; --";
        assertTrue("Should detect SQL injection", 
            InputValidator.containsDangerousContent(maliciousInput));
    }

    @Test
    public void testMaliciousCommandInjection() {
        String maliciousInput = "; cat /etc/passwd";
        assertTrue("Should detect command injection", 
            InputValidator.containsDangerousContent(maliciousInput));
    }

    @Test
    public void testSanitizeInput() {
        String maliciousInput = "<script>alert('XSS')</script>";
        String sanitized = InputValidator.sanitizeInput(maliciousInput);
        assertFalse("Sanitized input should not contain script tags", 
            sanitized.contains("<script"));
        assertFalse("Sanitized input should not contain tags", 
            sanitized.contains("<"));
    }

    @Test
    public void testSanitizeForJavaScript() {
        String input = "test\"value";
        String sanitized = InputValidator.sanitizeForJavaScript(input);
        assertTrue("Should escape quotes", sanitized.contains("\\\""));
    }

    @Test
    public void testSanitizeForJson() {
        String input = "test\"value";
        String sanitized = InputValidator.sanitizeForJson(input);
        assertTrue("Should escape quotes", sanitized.contains("\\\""));
    }

    @Test
    public void testValidFileName() {
        assertTrue("Valid filename should pass", InputValidator.isValidFileName("Test.java"));
        assertTrue("Filename with dash should pass", InputValidator.isValidFileName("test-file.js"));
        assertTrue("Filename with underscore should pass", InputValidator.isValidFileName("test_file.py"));
    }

    @Test
    public void testInvalidFileName() {
        assertFalse("Null filename should fail", InputValidator.isValidFileName(null));
        assertFalse("Empty filename should fail", InputValidator.isValidFileName(""));
        assertFalse("Filename with path should fail", InputValidator.isValidFileName("../test.java"));
        assertFalse("Filename with spaces should fail", InputValidator.isValidFileName("test file.java"));
    }

    @Test
    public void testValidLanguage() {
        assertTrue("Valid language should pass", InputValidator.isValidLanguage("java"));
        assertTrue("Valid language should pass", InputValidator.isValidLanguage("python3"));
    }

    @Test
    public void testInvalidLanguage() {
        assertFalse("Null language should fail", InputValidator.isValidLanguage(null));
        assertFalse("Language with special chars should fail", InputValidator.isValidLanguage("java;"));
    }

    @Test
    public void testValidJson() {
        assertTrue("Valid JSON object should pass", 
            InputValidator.isValidJson("{\"key\": \"value\"}"));
        assertTrue("Valid JSON array should pass", 
            InputValidator.isValidJson("[1, 2, 3]"));
    }

    @Test
    public void testInvalidJson() {
        assertFalse("Invalid JSON should fail", 
            InputValidator.isValidJson("not json"));
        assertFalse("Null should fail", 
            InputValidator.isValidJson(null));
    }

    @Test
    public void testLspRequestValidation() {
        InputValidator.ValidatedRequest request = InputValidator.validateLspRequest(
            "req-001", "textDocument/completion", "{\"position\":{\"line\":1}}"
        );
        assertNotNull("Valid LSP request should pass", request);
        assertEquals("req-001", request.requestId);
        assertEquals("textDocument/completion", request.method);
    }

    @Test
    public void testInvalidLspRequestId() {
        InputValidator.ValidatedRequest request = InputValidator.validateLspRequest(
            "invalid id with spaces", "textDocument/completion", "{}"
        );
        assertNull("Invalid request ID should fail", request);
    }

    @Test
    public void testDebugCommandValidation() {
        InputValidator.ValidatedDebugCommand command = InputValidator.validateDebugCommand(
            "start", "{\"filePath\":\"/test.java\"}"
        );
        assertNotNull("Valid debug command should pass", command);
        assertEquals("start", command.command);
    }

    @Test
    public void testInvalidDebugCommand() {
        InputValidator.ValidatedDebugCommand command = InputValidator.validateDebugCommand(
            "dangerous_command", "{}"
        );
        assertNull("Invalid debug command should fail", command);
    }

    @Test
    public void testValidationStats() {
        InputValidator.ValidationStats stats = InputValidator.getStats();
        assertNotNull("Stats should not be null", stats);
        assertTrue("Should have dangerous patterns", stats.dangerousPatternCount > 0);
        assertTrue("Should have max file path length", stats.maxFilePathLength > 0);
    }

    // ======= Security Logger Tests =======

    @Test
    public void testSecurityEventCreation() {
        SecurityLogger.SecurityEvent event = new SecurityLogger.SecurityEvent(
            SecurityLogger.SecurityEventType.INPUT_VALIDATION,
            SecurityLogger.SeverityLevel.MEDIUM,
            "Test message",
            "Test details",
            "Test stack trace"
        );
        
        assertNotNull("Event should be created", event);
        assertEquals(SecurityLogger.SecurityEventType.INPUT_VALIDATION, event.eventType);
        assertEquals(SecurityLogger.SeverityLevel.MEDIUM, event.severity);
        assertEquals("Test message", event.message);
    }

    @Test
    public void testSecurityStatsCreation() {
        SecurityLogger.SecurityStats stats = new SecurityLogger.SecurityStats(
            5, 1024, 1, 2, 3
        );
        
        assertNotNull("Stats should be created", stats);
        assertEquals(5, stats.totalLogFiles);
        assertEquals(1, stats.criticalEvents);
        assertEquals(2, stats.highEvents);
        assertEquals(3, stats.mediumEvents);
    }

    // ======= Edge Cases =======

    @Test
    public void testLongInputTruncation() {
        StringBuilder longInput = new StringBuilder();
        for (int i = 0; i < 20000; i++) {
            longInput.append("a");
        }
        String result = InputValidator.sanitizeInput(longInput.toString());
        assertTrue("Result should be truncated", result.length() <= InputValidator.MAX_INPUT_LENGTH);
    }

    @Test
    public void testEmptySanitization() {
        assertEquals("", InputValidator.sanitizeInput(null));
        assertEquals("", InputValidator.sanitizeInput(""));
    }

    @Test
    public void testNoDangerousContent() {
        String normalInput = "This is normal text content";
        assertFalse("Normal content should not be flagged", 
            InputValidator.containsDangerousContent(normalInput));
    }
}