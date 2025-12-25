package ohi.andre.consolelauncher.commands.smartlauncher.developer.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Set;

/**
 * SecureStorageManager - OWASP compliant encrypted storage implementation
 * 
 * Implements security measures for:
 * - M2: Insecure Data Storage
 * 
 * Features:
 * - AES-256-GCM encryption for files
 * - Encrypted SharedPreferences for settings
 * - Secure key management using Android Keystore
 * - Automatic data integrity verification
 * - Protection against rooted device data extraction
 */
public class SecureStorageManager {
    private static final String TAG = "SecureStorageManager";
    
    // Encryption configuration
    private static final String ENCRYPTED_PREFS_FILE = "monaco_secure_prefs";
    private static final String MASTER_KEY_ALIAS = "MonacoEditorMasterKey";
    private static final String AES_256_GCM = "AES_256_GCM_HKDF_4KB";
    
    // File encryption configuration
    private static final int BUFFER_SIZE = 4096; // 4KB chunks for streaming encryption
    
    private final Context context;
    private final MasterKey masterKey;
    private final SharedPreferences encryptedPrefs;
    
    /**
     * Constructor - initializes encrypted storage
     * 
     * @param context Application context
     */
    public SecureStorageManager(Context context) {
        this.context = context.getApplicationContext();
        
        try {
            // Create master key using Android Keystore
            this.masterKey = createMasterKey();
            
            // Initialize encrypted SharedPreferences
            this.encryptedPrefs = createEncryptedPreferences();
            
            Log.i(TAG, "Secure storage initialized successfully");
        } catch (GeneralSecurityException e) {
            Log.e(TAG, "Failed to initialize secure storage", e);
            throw new SecurityException("Secure storage initialization failed", e);
        } catch (IOException e) {
            Log.e(TAG, "Failed to create encrypted preferences", e);
            throw new SecurityException("Encrypted preferences creation failed", e);
        }
    }
    
    /**
     * Create MasterKey using Android Keystore for secure key storage
     * 
     * The MasterKey is used to encrypt all other keys and data.
     * It leverages hardware-backed storage when available.
     */
    private MasterKey createMasterKey() throws GeneralSecurityException, IOException {
        MasterKey.Builder builder = new MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setUserAuthenticationRequired(false); // Set to true for biometric protection
        
        // In a production environment, consider requiring authentication:
        // .setUserAuthenticationRequired(true)
        // .setUserAuthenticationValidityDurationSeconds(30)
        
        return builder.build();
    }
    
    /**
     * Create encrypted SharedPreferences for secure settings storage
     * 
     * EncryptedSharedPreferences automatically encrypts:
     * - All keys
     * - All values
     * - Any file metadata
     */
    private SharedPreferences createEncryptedPreferences() throws GeneralSecurityException, IOException {
        return EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }
    
    /**
     * Store a string value securely
     * 
     * @param key Unique identifier for the value
     * @param value The value to store
     */
    public void putString(String key, String value) {
        if (key == null || value == null) {
            Log.w(TAG, "Cannot store null key or value");
            return;
        }
        
        // Validate input to prevent injection
        if (!isValidStorageKey(key)) {
            Log.w(TAG, "Invalid storage key: " + key);
            return;
        }
        
        encryptedPrefs.edit()
            .putString(key, value)
            .apply();
        
        Log.d(TAG, "Securely stored string: " + sanitizeKeyForLogging(key));
    }
    
    /**
     * Retrieve a string value securely
     * 
     * @param key Unique identifier for the value
     * @param defaultValue Value to return if key not found
     * @return The stored value or defaultValue
     */
    public String getString(String key, String defaultValue) {
        if (!isValidStorageKey(key)) {
            Log.w(TAG, "Invalid storage key: " + key);
            return defaultValue;
        }
        
        return encryptedPrefs.getString(key, defaultValue);
    }
    
    /**
     * Store a boolean value securely
     * 
     * @param key Unique identifier for the value
     * @param value The value to store
     */
    public void putBoolean(String key, boolean value) {
        if (!isValidStorageKey(key)) {
            Log.w(TAG, "Invalid storage key: " + key);
            return;
        }
        
        encryptedPrefs.edit()
            .putBoolean(key, value)
            .apply();
    }
    
    /**
     * Retrieve a boolean value securely
     * 
     * @param key Unique identifier for the value
     * @param defaultValue Value to return if key not found
     * @return The stored value or defaultValue
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        if (!isValidStorageKey(key)) {
            Log.w(TAG, "Invalid storage key: " + key);
            return defaultValue;
        }
        
        return encryptedPrefs.getBoolean(key, defaultValue);
    }
    
    /**
     * Store an integer value securely
     * 
     * @param key Unique identifier for the value
     * @param value The value to store
     */
    public void putInt(String key, int value) {
        if (!isValidStorageKey(key)) {
            Log.w(TAG, "Invalid storage key: " + key);
            return;
        }
        
        encryptedPrefs.edit()
            .putInt(key, value)
            .apply();
    }
    
    /**
     * Retrieve an integer value securely
     * 
     * @param key Unique identifier for the value
     * @param defaultValue Value to return if key not found
     * @return The stored value or defaultValue
     */
    public int getInt(String key, int defaultValue) {
        if (!isValidStorageKey(key)) {
            Log.w(TAG, "Invalid storage key: " + key);
            return defaultValue;
        }
        
        return encryptedPrefs.getInt(key, defaultValue);
    }
    
    /**
     * Store a Set of strings securely
     * 
     * @param key Unique identifier for the value
     * @param value The Set to store
     */
    public void putStringSet(String key, Set<String> value) {
        if (!isValidStorageKey(key)) {
            Log.w(TAG, "Invalid storage key: " + key);
            return;
        }
        
        encryptedPrefs.edit()
            .putStringSet(key, value)
            .apply();
    }
    
    /**
     * Retrieve a Set of strings securely
     * 
     * @param key Unique identifier for the value
     * @return The stored Set or null
     */
    public Set<String> getStringSet(String key) {
        if (!isValidStorageKey(key)) {
            Log.w(TAG, "Invalid storage key: " + key);
            return null;
        }
        
        return encryptedPrefs.getStringSet(key, null);
    }
    
    /**
     * Write encrypted file for source code storage
     * 
     * This method encrypts source code before writing to disk,
     * protecting against file extraction on rooted devices.
     * 
     * @param file The file to write to
     * @param content The content to encrypt and write
     * @return true if write successful, false otherwise
     */
    public boolean writeEncryptedFile(File file, String content) {
        if (file == null || content == null) {
            Log.w(TAG, "Cannot write null file or content");
            return false;
        }
        
        EncryptedFile encryptedFile = null;
        OutputStream outputStream = null;
        
        try {
            // Create encrypted file with AES-256-GCM encryption
            encryptedFile = new EncryptedFile.Builder(
                context,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build();
            
            // Write encrypted content
            outputStream = encryptedFile.openWriteOutputStream();
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
            outputStream.write(contentBytes);
            outputStream.flush();
            
            Log.d(TAG, "Encrypted file written: " + file.getName());
            return true;
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to write encrypted file: " + file.getName(), e);
            return false;
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                Log.w(TAG, "Error closing output stream", e);
            }
        }
    }
    
    /**
     * Read encrypted file for source code retrieval
     * 
     * @param file The file to read from
     * @return Decrypted content, or null if read fails
     */
    public String readEncryptedFile(File file) {
        if (file == null || !file.exists()) {
            Log.w(TAG, "Cannot read null or non-existent file");
            return null;
        }
        
        EncryptedFile encryptedFile = null;
        InputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;
        
        try {
            // Open encrypted file for reading
            encryptedFile = new EncryptedFile.Builder(
                context,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build();
            
            // Read and decrypt content
            inputStream = encryptedFile.openReadStream();
            outputStream = new ByteArrayOutputStream();
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            String content = outputStream.toString(StandardCharsets.UTF_8.name());
            Log.d(TAG, "Encrypted file read: " + file.getName());
            
            return content;
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to read encrypted file: " + file.getName(), e);
            return null;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                Log.w(TAG, "Error closing streams", e);
            }
        }
    }
    
    /**
     * Delete a value from secure storage
     * 
     * @param key The key to remove
     */
    public void remove(String key) {
        if (!isValidStorageKey(key)) {
            Log.w(TAG, "Invalid storage key: " + key);
            return;
        }
        
        encryptedPrefs.edit()
            .remove(key)
            .apply();
        
        Log.d(TAG, "Secure value removed: " + sanitizeKeyForLogging(key));
    }
    
    /**
     * Clear all stored values
     * 
     * WARNING: This permanently deletes all encrypted data
     */
    public void clearAll() {
        encryptedPrefs.edit().clear().apply();
        Log.w(TAG, "All secure storage cleared");
    }
    
    /**
     * Check if a key exists in storage
     * 
     * @param key The key to check
     * @return true if key exists
     */
    public boolean containsKey(String key) {
        if (!isValidStorageKey(key)) {
            return false;
        }
        
        return encryptedPrefs.contains(key);
    }
    
    /**
     * Get all stored keys
     * 
     * @return Set of all keys in storage
     */
    public Set<String> getAllKeys() {
        return encryptedPrefs.getAll().keySet();
    }
    
    /**
     * Validate storage key to prevent injection attacks
     * 
     * Keys should only contain alphanumeric characters, underscores, and dots.
     */
    private boolean isValidStorageKey(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        
        // Only allow alphanumeric characters, underscores, dots, and hyphens
        return key.matches("[a-zA-Z0-9_.\\-]+");
    }
    
    /**
     * Sanitize key for safe logging (mask potentially sensitive keys)
     */
    private String sanitizeKeyForLogging(String key) {
        if (key == null || key.isEmpty()) {
            return "[empty]";
        }
        
        // Mask keys that might contain sensitive information
        String lowerKey = key.toLowerCase();
        if (lowerKey.contains("token") || lowerKey.contains("key") || 
            lowerKey.contains("secret") || lowerKey.contains("password")) {
            return key.substring(0, Math.min(5, key.length())) + "***REDACTED***";
        }
        
        return key;
    }
    
    /**
     * Generate secure hash for data integrity verification
     * 
     * @param data The data to hash
     * @return SHA-256 hash as hexadecimal string
     */
    public String generateDataHash(String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SHA-256 algorithm not available", e);
            return null;
        }
    }
    
    /**
     * Verify data integrity using stored hash
     * 
     * @param data The data to verify
     * @param expectedHash The expected hash
     * @return true if data integrity is verified
     */
    public boolean verifyDataIntegrity(String data, String expectedHash) {
        if (data == null || expectedHash == null) {
            return false;
        }
        
        String actualHash = generateDataHash(data);
        return expectedHash.equals(actualHash);
    }
    
    /**
     * Get storage statistics for monitoring
     * 
     * @return Map containing storage statistics
     */
    public Map<String, Object> getStorageStats() {
        java.util.HashMap<String, Object> stats = new java.util.HashMap<>();
        
        stats.put("totalKeys", getAllKeys().size());
        stats.put("encryptionScheme", AES_256_GCM);
        stats.put("androidVersion", Build.VERSION.SDK_INT);
        stats.put("masterKeyAvailable", masterKey != null);
        
        return stats;
    }
}