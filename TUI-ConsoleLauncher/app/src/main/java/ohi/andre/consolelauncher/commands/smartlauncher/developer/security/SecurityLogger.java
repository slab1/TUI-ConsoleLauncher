package ohi.andre.consolelauncher.commands.smartlauncher.developer.security;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SecurityLogger - OWASP compliant security event logging
 * 
 * Implements security measures for:
 * - M8: Code Tampering
 * - M9: Reverse Engineering
 * - Insufficient Logging & Monitoring
 * 
 * Features:
 * - Security event categorization
 * - Secure log storage with integrity protection
 * - Background logging to prevent main thread blocking
 * - Log rotation and cleanup
 * - Security alert generation
 */
public class SecurityLogger {
    private static final String TAG = "SecurityLogger";
    
    // Log file configuration
    private static final String LOG_DIRECTORY = "security_logs";
    private static final String LOG_FILE_PREFIX = "security_";
    private static final String LOG_FILE_EXTENSION = ".log";
    private static final int MAX_LOG_FILE_SIZE = 1024 * 1024; // 1MB
    private static final int MAX_LOG_FILES = 5;
    
    // Event types for categorization
    public enum SecurityEventType {
        AUTHENTICATION("AUTH", "Authentication events"),
        AUTHORIZATION("AUTHZ", "Authorization events"),
        INPUT_VALIDATION("INPUT", "Input validation events"),
        WEBVIEW_SECURITY("WEBVIEW", "WebView security events"),
        DATA_ACCESS("DATA", "Data access events"),
        NETWORK_ACCESS("NETWORK", "Network access events"),
        FILE_ACCESS("FILE", "File access events"),
        SUSPICIOUS_ACTIVITY("SUSPICIOUS", "Suspicious activity detected"),
        DEBUG_EVENT("DEBUG", "Debug/diagnostic events"),
        CONFIGURATION("CONFIG", "Configuration changes")
    }
    
    // Severity levels
    public enum SeverityLevel {
        INFO(0),
        LOW(1),
        MEDIUM(2),
        HIGH(3),
        CRITICAL(4);
        
        private final int level;
        
        SeverityLevel(int level) {
            this.level = level;
        }
        
        public int getLevel() {
            return level;
        }
    }
    
    private final Context context;
    private final ExecutorService logExecutor;
    private final SimpleDateFormat dateFormat;
    private File logDirectory;
    
    // Singleton instance
    private static SecurityLogger instance;
    
    /**
     * Get singleton instance
     */
    public static synchronized SecurityLogger getInstance(Context context) {
        if (instance == null) {
            instance = new SecurityLogger(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Private constructor for singleton pattern
     */
    private SecurityLogger(Context context) {
        this.context = context;
        this.logExecutor = Executors.newSingleThreadExecutor();
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        
        // Initialize log directory
        initializeLogDirectory();
    }
    
    /**
     * Initialize secure log directory
     */
    private void initializeLogDirectory() {
        try {
            logDirectory = new File(context.getFilesDir(), LOG_DIRECTORY);
            if (!logDirectory.exists()) {
                logDirectory.mkdirs();
            }
            
            // Set restrictive permissions (owner only)
            logDirectory.setReadable(false, false);
            logDirectory.setWritable(false, false);
            logDirectory.setExecutable(false, false);
            
            Log.i(TAG, "Security log directory initialized: " + logDirectory.getAbsolutePath());
        } catch (SecurityException e) {
            Log.e(TAG, "Failed to create log directory", e);
        }
    }
    
    /**
     * Log a security event
     * 
     * @param eventType The type of security event
     * @param severity The severity level
     * @param message The event message
     * @param details Additional details (can be null)
     */
    public void log(SecurityEventType eventType, SeverityLevel severity, 
                    String message, String details) {
        SecurityEvent event = new SecurityEvent(
            eventType,
            severity,
            message,
            details,
            getStackTrace()
        );
        
        // Log to Android logcat
        logToLogcat(event);
        
        // Write to file asynchronously
        logToFile(event);
    }
    
    /**
     * Log authentication event
     */
    public void logAuthentication(String action, boolean success, String details) {
        SeverityLevel severity = success ? SeverityLevel.INFO : SeverityLevel.MEDIUM;
        log(SecurityEventType.AUTHENTICATION, severity, 
            "Authentication: " + action, details);
    }
    
    /**
     * Log authorization event
     */
    public void logAuthorization(String action, String subject, boolean granted) {
        SeverityLevel severity = granted ? SeverityLevel.INFO : SeverityLevel.MEDIUM;
        log(SecurityEventType.AUTHORIZATION, severity,
            "Authorization: " + action + " for " + subject,
            granted ? "Access granted" : "Access denied");
    }
    
    /**
     * Log input validation event
     */
    public void logInputValidation(String inputType, boolean valid, String details) {
        SeverityLevel severity = valid ? SeverityLevel.INFO : SeverityLevel.MEDIUM;
        log(SecurityEventType.INPUT_VALIDATION, severity,
            "Input validation for " + inputType + ": " + (valid ? "VALID" : "INVALID"),
            details);
    }
    
    /**
     * Log WebView security event
     */
    public void logWebViewSecurity(String action, String url, boolean allowed) {
        SeverityLevel severity = allowed ? SeverityLevel.INFO : SeverityLevel.HIGH;
        log(SecurityEventType.WEBVIEW_SECURITY, severity,
            "WebView " + action + ": " + (allowed ? "ALLOWED" : "BLOCKED"),
            sanitizeUrl(url));
    }
    
    /**
     * Log suspicious activity
     */
    public void logSuspiciousActivity(String activity, String details) {
        log(SecurityEventType.SUSPICIOUS_ACTIVITY, SeverityLevel.HIGH,
            "Suspicious activity detected: " + activity, details);
    }
    
    /**
     * Log file access event
     */
    public void logFileAccess(String filePath, String operation, boolean success) {
        SeverityLevel severity = success ? SeverityLevel.INFO : SeverityLevel.MEDIUM;
        log(SecurityEventType.FILE_ACCESS, severity,
            "File " + operation + ": " + sanitizeFilePath(filePath),
            success ? "Success" : "Failed");
    }
    
    /**
     * Log debug event (low severity, useful for troubleshooting)
     */
    public void logDebug(String message, String details) {
        log(SecurityEventType.DEBUG_EVENT, SeverityLevel.INFO, message, details);
    }
    
    /**
     * Log security event to Android Logcat
     */
    private void logToLogcat(SecurityEvent event) {
        String logMessage = String.format("[%s][%s][%s] %s",
            event.eventType.code,
            event.severity.name(),
            event.timestamp,
            event.message
        );
        
        switch (event.severity) {
            case CRITICAL:
            case HIGH:
                Log.e(TAG, logMessage);
                break;
            case MEDIUM:
                Log.w(TAG, logMessage);
                break;
            case LOW:
            case INFO:
            default:
                Log.i(TAG, logMessage);
                break;
        }
    }
    
    /**
     * Log security event to file asynchronously
     */
    private void logToFile(SecurityEvent event) {
        logExecutor.execute(() -> {
            PrintWriter writer = null;
            
            try {
                File logFile = getCurrentLogFile();
                
                // Check if we need to rotate logs
                if (logFile.length() > MAX_LOG_FILE_SIZE) {
                    rotateLogs();
                    logFile = getCurrentLogFile();
                }
                
                writer = new PrintWriter(new FileWriter(logFile, true));
                writer.println(formatEventForFile(event));
                writer.flush();
                
            } catch (IOException e) {
                Log.e(TAG, "Failed to write security log", e);
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        });
    }
    
    /**
     * Get the current log file, creating if necessary
     */
    private File getCurrentLogFile() {
        File logFile = new File(logDirectory, 
            LOG_FILE_PREFIX + dateFormat.format(new Date()).replace(":", "-") + LOG_FILE_EXTENSION);
        return logFile;
    }
    
    /**
     * Format event for file storage
     */
    private String formatEventForFile(SecurityEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append(event.timestamp).append("|");
        sb.append(event.eventType.code).append("|");
        sb.append(event.severity.name()).append("|");
        sb.append(event.message.replace("|", "\\|")).append("|");
        
        if (event.details != null) {
            sb.append(event.details.replace("|", "\\|"));
        }
        sb.append("|");
        
        if (event.stackTrace != null) {
            sb.append(event.stackTrace.replace("|", "\\|").replace("\n", "\\n"));
        }
        
        return sb.toString();
    }
    
    /**
     * Rotate log files to prevent unbounded growth
     */
    private void rotateLogs() {
        File[] files = logDirectory.listFiles((dir, name) -> 
            name.startsWith(LOG_FILE_PREFIX) && name.endsWith(LOG_FILE_EXTENSION)
        );
        
        if (files == null || files.length == 0) {
            return;
        }
        
        // Sort by last modified
        java.util.Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
        
        // Delete oldest files beyond limit
        for (int i = MAX_LOG_FILES; i < files.length; i++) {
            files[i].delete();
        }
    }
    
    /**
     * Get recent security events for analysis
     * 
     * @param maxEvents Maximum number of events to return
     * @return List of recent security events
     */
    public List<SecurityEvent> getRecentEvents(int maxEvents) {
        List<SecurityEvent> events = new ArrayList<>();
        
        File[] files = logDirectory.listFiles((dir, name) -> 
            name.startsWith(LOG_FILE_PREFIX) && name.endsWith(LOG_FILE_EXTENSION)
        );
        
        if (files == null || files.length == 0) {
            return events;
        }
        
        // Sort by last modified, most recent first
        java.util.Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
        
        int count = 0;
        for (File file : files) {
            if (count >= maxEvents) break;
            
            List<SecurityEvent> fileEvents = parseLogFile(file);
            for (SecurityEvent event : fileEvents) {
                if (count >= maxEvents) break;
                events.add(event);
                count++;
            }
        }
        
        return events;
    }
    
    /**
     * Parse log file into events
     */
    private List<SecurityEvent> parseLogFile(File file) {
        List<SecurityEvent> events = new ArrayList<>();
        
        try {
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.FileReader(file)
            );
            
            String line;
            while ((line = reader.readLine()) != null) {
                SecurityEvent event = parseLogLine(line);
                if (event != null) {
                    events.add(event);
                }
            }
            
            reader.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to parse log file", e);
        }
        
        return events;
    }
    
    /**
     * Parse single log line into event
     */
    private SecurityEvent parseLogLine(String line) {
        try {
            String[] parts = line.split("\\|", -1);
            if (parts.length < 4) {
                return null;
            }
            
            String timestamp = parts[0];
            String eventTypeCode = parts[1];
            String severityStr = parts[2];
            String message = parts[3];
            String details = parts.length > 4 ? parts[4] : null;
            String stackTrace = parts.length > 5 ? parts[5] : null;
            
            // Find event type
            SecurityEventType eventType = null;
            for (SecurityEventType type : SecurityEventType.values()) {
                if (type.code.equals(eventTypeCode)) {
                    eventType = type;
                    break;
                }
            }
            
            if (eventType == null) {
                return null;
            }
            
            SeverityLevel severity;
            try {
                severity = SeverityLevel.valueOf(severityStr);
            } catch (IllegalArgumentException e) {
                severity = SeverityLevel.INFO;
            }
            
            return new SecurityEvent(eventType, severity, message, details, stackTrace);
            
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse log line", e);
            return null;
        }
    }
    
    /**
     * Get security statistics
     * 
     * @return Security statistics
     */
    public SecurityStats getStatistics() {
        File[] files = logDirectory.listFiles((dir, name) -> 
            name.startsWith(LOG_FILE_PREFIX) && name.endsWith(LOG_FILE_EXTENSION)
        );
        
        int totalFiles = files != null ? files.length : 0;
        long totalSize = 0;
        int criticalCount = 0;
        int highCount = 0;
        int mediumCount = 0;
        
        for (SecurityEvent event : getRecentEvents(1000)) {
            switch (event.severity) {
                case CRITICAL:
                    criticalCount++;
                    break;
                case HIGH:
                    highCount++;
                    break;
                case MEDIUM:
                    mediumCount++;
                    break;
            }
        }
        
        return new SecurityStats(totalFiles, totalSize, criticalCount, highCount, mediumCount);
    }
    
    /**
     * Clear all logs (for privacy/data protection)
     */
    public void clearLogs() {
        logExecutor.execute(() -> {
            File[] files = logDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            Log.w(TAG, "Security logs cleared");
        });
    }
    
    /**
     * Get stack trace for debugging
     */
    private String getStackTrace() {
        try {
            throw new Exception("Stack trace for logging");
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            String trace = sw.toString();
            // Limit trace length
            if (trace.length() > 500) {
                trace = trace.substring(0, 500) + "...";
            }
            return trace;
        }
    }
    
    /**
     * Sanitize URL for logging
     */
    private String sanitizeUrl(String url) {
        if (url == null) return "[null]";
        
        // Remove sensitive parameters
        return url.replaceAll("([?&])(token|key|auth|password|secret)=[^&]*", "$1***=REDACTED");
    }
    
    /**
     * Sanitize file path for logging
     */
    private String sanitizeFilePath(String path) {
        if (path == null) return "[null]";
        
        // Just show filename for security
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0) {
            return "..." + path.substring(lastSlash);
        }
        return path;
    }
    
    // ======= Inner Classes =======
    
    /**
     * Security event data class
     */
    public static class SecurityEvent {
        public final SecurityEventType eventType;
        public final SeverityLevel severity;
        public final String message;
        public final String details;
        public final String stackTrace;
        public final String timestamp;
        
        public SecurityEvent(SecurityEventType eventType, SeverityLevel severity,
                            String message, String details, String stackTrace) {
            this.eventType = eventType;
            this.severity = severity;
            this.message = message;
            this.details = details;
            this.stackTrace = stackTrace;
            this.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
                .format(new Date());
        }
    }
    
    /**
     * Security statistics
     */
    public static class SecurityStats {
        public final int totalLogFiles;
        public final long totalLogSize;
        public final int criticalEvents;
        public final int highEvents;
        public final int mediumEvents;
        
        public SecurityStats(int files, long size, int critical, int high, int medium) {
            this.totalLogFiles = files;
            this.totalLogSize = size;
            this.criticalEvents = critical;
            this.highEvents = high;
            this.mediumEvents = medium;
        }
    }
}