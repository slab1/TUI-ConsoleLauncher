package ohi.andre.consolelauncher.commands.smartlauncher.developer.security;

import android.util.Log;
import android.webkit.URLUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * InputValidator - OWASP compliant input validation utilities
 * 
 * Implements security measures for:
 * - M7: Client Code Quality
 * - Injection Prevention (SQL, Command, XSS)
 * 
 * Features:
 * - Comprehensive input sanitization
 * - URL validation
 * - File path validation
 * - JavaScript injection prevention
 * - SQL injection prevention
 * - Command injection prevention
 */
public class InputValidator {
    private static final String TAG = "InputValidator";
    
    // Maximum allowed lengths
    private static final int MAX_FILE_PATH_LENGTH = 500;
    private static final int MAX_URL_LENGTH = 2048;
    private static final int MAX_INPUT_LENGTH = 10000;
    
    // Allowed patterns
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._\\-]+$"
    );
    
    private static final Pattern SAFE_PATH_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._\\-/]+$"
    );
    
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9]+$"
    );
    
    private static final Pattern SAFE_JSON_VALUE_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_\\-.,;:\\s\\{\\}\\[\\]\"']+$"
    );
    
    // Dangerous patterns to detect
    private static final Set<Pattern> DANGEROUS_PATTERNS = new HashSet<>();
    
    static {
        // Script injection patterns
        DANGEROUS_PATTERNS.add(Pattern.compile(
            "<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        ));
        DANGEROUS_PATTERNS.add(Pattern.compile(
            "javascript\\s*:", Pattern.CASE_INSENSITIVE
        ));
        DANGEROUS_PATTERNS.add(Pattern.compile(
            "on\\w+\\s*=", Pattern.CASE_INSENSITIVE
        ));
        DANGEROUS_PATTERNS.add(Pattern.compile(
            "<iframe[^>]*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        ));
        DANGEROUS_PATTERNS.add(Pattern.compile(
            "<object[^>]*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        ));
        DANGEROUS_PATTERNS.add(Pattern.compile(
            "<embed[^>]*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        ));
        
        // SQL injection patterns
        DANGEROUS_PATTERNS.add(Pattern.compile(
            "('|(\"|%27)|(--|%2D%2D)|(#|%23)|(/\\*|%2F%2A))", 
            Pattern.CASE_INSENSITIVE
        ));
        DANGEROUS_PATTERNS.add(Pattern.compile(
            "(union|select|insert|update|delete|drop|create|alter|exec|execute)",
            Pattern.CASE_INSENSITIVE
        ));
        
        // Command injection patterns
        DANGEROUS_PATTERNS.add(Pattern.compile(
            "(\\|{2}|&{2}|;|`|\\$|\\(|\\)|\\{|\\}|<|>)",
            Pattern.CASE_INSENSITIVE
        ));
        DANGEROUS_PATTERNS.add(Pattern.compile(
            "(rm|cp|mv|cat|ls|ps|kill|wget|curl|nc)",
            Pattern.CASE_INSENSITIVE
        ));
    }
    
    /**
     * Validate and sanitize file path to prevent path traversal attacks
     * 
     * @param filePath The file path to validate
     * @return Sanitized file path, or null if invalid
     */
    public static String validateFilePath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            Log.w(TAG, "Empty file path rejected");
            return null;
        }
        
        // Check length
        if (filePath.length() > MAX_FILE_PATH_LENGTH) {
            Log.w(TAG, "File path too long: " + filePath.length() + " characters");
            return null;
        }
        
        // Remove null bytes
        filePath = filePath.replace("\0", "");
        
        // Block path traversal attempts
        if (filePath.contains("..") || filePath.contains("./") || filePath.contains("/.")) {
            Log.w(TAG, "Path traversal attempt detected: " + sanitizeForLogging(filePath));
            return null;
        }
        
        // Block absolute paths (except for allowed directories)
        if (filePath.startsWith("/")) {
            // Allow only specific absolute paths
            String[] allowedPaths = {
                "/android_asset/",
                "/android_res/",
                "/data/data/",
                "/storage/"
            };
            
            boolean allowed = false;
            for (String allowedPath : allowedPaths) {
                if (filePath.startsWith(allowedPath)) {
                    allowed = true;
                    break;
                }
            }
            
            if (!allowed) {
                Log.w(TAG, "Disallowed absolute path: " + sanitizeForLogging(filePath));
                return null;
            }
        }
        
        // Validate path characters
        if (!SAFE_PATH_PATTERN.matcher(filePath).matches()) {
            Log.w(TAG, "Invalid characters in file path: " + sanitizeForLogging(filePath));
            return null;
        }
        
        return filePath;
    }
    
    /**
     * Validate URL for security
     * 
     * @param url The URL to validate
     * @return true if URL is safe, false otherwise
     */
    public static boolean validateUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        
        // Check length
        if (url.length() > MAX_URL_LENGTH) {
            Log.w(TAG, "URL too long: " + url.length() + " characters");
            return false;
        }
        
        // Remove null bytes
        url = url.replace("\0", "");
        
        // Only allow http and https schemes
        if (!url.toLowerCase().startsWith("http://") && 
            !url.toLowerCase().startsWith("https://")) {
            Log.w(TAG, "Disallowed URL scheme: " + sanitizeForLogging(url));
            return false;
        }
        
        // Validate URL format
        try {
            URL parsedUrl = new URL(url);
            
            // Block localhost and private IPs in production
            String host = parsedUrl.getHost().toLowerCase();
            if (isPrivateIpAddress(host)) {
                Log.w(TAG, "Private IP address in URL: " + host);
                return false;
            }
            
            // Check for suspicious TLDs
            if (isSuspiciousTLD(parsedUrl)) {
                Log.w(TAG, "Suspicious TLD in URL: " + parsedUrl.getHost());
                return false;
            }
            
            // Verify URL is reachable (basic check)
            return URLUtil.isValidUrl(url);
            
        } catch (MalformedURLException e) {
            Log.w(TAG, "Malformed URL: " + sanitizeForLogging(url));
            return false;
        }
    }
    
    /**
     * Check if hostname is a private IP address
     */
    private static boolean isPrivateIpAddress(String host) {
        // Check for literal IP addresses
        String[] octets = host.split("\\.");
        if (octets.length == 4) {
            try {
                int first = Integer.parseInt(octets[0]);
                int second = Integer.parseInt(octets[1]);
                
                // 10.0.0.0/8
                if (first == 10) return true;
                
                // 172.16.0.0/12
                if (first == 172 && second >= 16 && second <= 31) return true;
                
                // 192.168.0.0/16
                if (first == 192 && second == 168) return true;
                
                // 127.0.0.0/8 (localhost)
                if (first == 127) return true;
                
            } catch (NumberFormatException e) {
                // Not an IP address, continue
            }
        }
        
        // Check for localhost
        if (host.equals("localhost") || host.equals("127.0.0.1") || 
            host.equals("::1") || host.equals("0.0.0.0")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Check if TLD is suspicious
     */
    private static boolean isSuspiciousTLD(URL url) {
        String host = url.getHost().toLowerCase();
        
        // Check for commonly abused TLDs
        String[] suspiciousTLDs = {".tk", ".ml", ".ga", ".cf", ".gq", ".xyz", ".top"};
        
        for (String tld : suspiciousTLDs) {
            if (host.endsWith(tld)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if input contains potentially dangerous content
     * 
     * @param input The input to check
     * @return true if dangerous content detected
     */
    public static boolean containsDangerousContent(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        
        for (Pattern pattern : DANGEROUS_PATTERNS) {
            if (pattern.matcher(input).find()) {
                Log.w(TAG, "Dangerous pattern detected: " + pattern.pattern());
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Sanitize input to remove dangerous content
     * 
     * @param input The input to sanitize
     * @return Sanitized input, or empty string if input is null
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        
        // Check length
        if (input.length() > MAX_INPUT_LENGTH) {
            Log.w(TAG, "Input truncated to max length");
            input = input.substring(0, MAX_INPUT_LENGTH);
        }
        
        // Remove null bytes
        input = input.replace("\0", "");
        
        // Remove dangerous HTML/script content
        String sanitized = input
            .replaceAll("<script[^>]*>.*?</script>", "")
            .replaceAll("<[^>]+>", "") // Remove all HTML tags
            .replaceAll("javascript\\s*:", "")
            .replaceAll("on\\w+\\s*=", "");
        
        return sanitized;
    }
    
    /**
     * Sanitize JavaScript string for safe embedding
     * 
     * @param input The input to sanitize
     * @return Sanitized string safe for JavaScript embedding
     */
    public static String sanitizeForJavaScript(String input) {
        if (input == null) {
            return "";
        }
        
        return input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("<", "\\x3C")
            .replace(">", "\\x3E");
    }
    
    /**
     * Sanitize JSON value
     * 
     * @param input The input to sanitize
     * @return Sanitized JSON value
     */
    public static String sanitizeForJson(String input) {
        if (input == null) {
            return "";
        }
        
        // Remove control characters
        String sanitized = input.replaceAll("[\\x00-\\x1F\\x7F]", "");
        
        // Escape special JSON characters
        sanitized = sanitized
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("/", "\\/")
            .replace("\b", "\\b")
            .replace("\f", "\\f")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
        
        return sanitized;
    }
    
    /**
     * Validate file name for security
     * 
     * @param fileName The file name to validate
     * @return true if file name is safe
     */
    public static boolean isValidFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        
        // Check length
        if (fileName.length() > 255) {
            return false;
        }
        
        // Check pattern
        return SAFE_FILENAME_PATTERN.matcher(fileName).matches();
    }
    
    /**
     * Validate language identifier (for syntax highlighting)
     * 
     * @param language The language identifier to validate
     * @return true if language is valid
     */
    public static boolean isValidLanguage(String language) {
        if (language == null || language.isEmpty()) {
            return false;
        }
        
        // Only allow alphanumeric characters
        return ALPHANUMERIC_PATTERN.matcher(language).matches();
    }
    
    /**
     * Validate JSON content
     * 
     * @param json The JSON string to validate
     * @return true if JSON appears valid
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.isEmpty()) {
            return false;
        }
        
        // Check for basic JSON structure
        json = json.trim();
        
        if ((json.startsWith("{") && json.endsWith("}")) ||
            (json.startsWith("[") && json.endsWith("]"))) {
            return true;
        }
        
        Log.w(TAG, "Invalid JSON structure");
        return false;
    }
    
    /**
     * Validate and sanitize LSP (Language Server Protocol) request
     * 
     * @param requestId The request identifier
     * @param method The LSP method name
     * @param params The request parameters
     * @return ValidatedRequest or null if validation fails
     */
    public static ValidatedRequest validateLspRequest(String requestId, String method, String params) {
        // Validate request ID
        if (requestId == null || requestId.isEmpty() || 
            !ALPHANUMERIC_PATTERN.matcher(requestId).matches()) {
            Log.w(TAG, "Invalid LSP request ID");
            return null;
        }
        
        // Validate method name
        if (method == null || method.isEmpty() ||
            !method.matches("^[a-z0-9/]+$")) {
            Log.w(TAG, "Invalid LSP method: " + sanitizeForLogging(method));
            return null;
        }
        
        // Validate params (should be valid JSON)
        if (params != null && !params.isEmpty()) {
            // Params should be valid JSON object or array
            String trimmed = params.trim();
            if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
                Log.w(TAG, "Invalid LSP params format");
                return null;
            }
        }
        
        return new ValidatedRequest(requestId, method, params);
    }
    
    /**
     * Validate debug command
     * 
     * @param command The debug command to validate
     * @param args The command arguments
     * @return ValidatedDebugCommand or null if validation fails
     */
    public static ValidatedDebugCommand validateDebugCommand(String command, String args) {
        // Only allow known safe commands
        String[] allowedCommands = {
            "start", "stop", "continue", "stepOver", "stepInto", "stepOut",
            "toggleBreakpoint", "addWatch", "removeWatch", "evaluate"
        };
        
        boolean isAllowed = false;
        for (String allowed : allowedCommands) {
            if (allowed.equals(command)) {
                isAllowed = true;
                break;
            }
        }
        
        if (!isAllowed) {
            Log.w(TAG, "Disallowed debug command: " + sanitizeForLogging(command));
            return null;
        }
        
        // Validate args format if present
        if (args != null && !args.isEmpty()) {
            if (!isValidJson(args)) {
                Log.w(TAG, "Invalid debug command args");
                return null;
            }
        }
        
        return new ValidatedDebugCommand(command, args);
    }
    
    /**
     * Sanitize string for safe logging
     */
    private static String sanitizeForLogging(String input) {
        if (input == null || input.isEmpty()) {
            return "[empty]";
        }
        
        // Truncate long inputs
        if (input.length() > 100) {
            input = input.substring(0, 100) + "...";
        }
        
        // Remove potentially sensitive patterns
        return input
            .replaceAll("([?&])(token|key|auth|password|secret)=[^&]*", "$1***=REDACTED")
            .replaceAll("[\\x00-\\x1F\\x7F]", "");
    }
    
    /**
     * Get validation statistics for monitoring
     * 
     * @return Validation statistics
     */
    public static ValidationStats getStats() {
        return new ValidationStats(
            DANGEROUS_PATTERNS.size(),
            MAX_FILE_PATH_LENGTH,
            MAX_URL_LENGTH,
            MAX_INPUT_LENGTH
        );
    }
    
    // ======= Inner Classes =======
    
    /**
     * Validated LSP request container
     */
    public static class ValidatedRequest {
        public final String requestId;
        public final String method;
        public final String params;
        
        public ValidatedRequest(String requestId, String method, String params) {
            this.requestId = requestId;
            this.method = method;
            this.params = params;
        }
    }
    
    /**
     * Validated debug command container
     */
    public static class ValidatedDebugCommand {
        public final String command;
        public final String args;
        
        public ValidatedDebugCommand(String command, String args) {
            this.command = command;
            this.args = args;
        }
    }
    
    /**
     * Validation statistics
     */
    public static class ValidationStats {
        public final int dangerousPatternCount;
        public final int maxFilePathLength;
        public final int maxUrlLength;
        public final int maxInputLength;
        
        public ValidationStats(int patternCount, int maxPath, int maxUrl, int maxInput) {
            this.dangerousPatternCount = patternCount;
            this.maxFilePathLength = maxPath;
            this.maxUrlLength = maxUrl;
            this.maxInputLength = maxInput;
        }
    }
}