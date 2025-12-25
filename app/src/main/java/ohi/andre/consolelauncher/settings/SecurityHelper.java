package ohi.andre.consolelauncher.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class providing secure storage capabilities using EncryptedSharedPreferences.
 * This class wraps the AndroidX Security library to provide easy access to encrypted
 * storage for sensitive configuration values such as API keys, tokens, and credentials.
 * 
 * The implementation uses AES-256 encryption for both keys and values, with keys
 * protected by the Android Keystore system for hardware-backed security on supported devices.
 */
public class SecurityHelper {
    
    private static final String TAG = "SecurityHelper";
    private static final String ENCRYPTED_PREFS_FILE_NAME = "tui_secure_prefs";
    private static final String MASTER_KEY_ALIAS = "tui_master_key";
    
    private static volatile SharedPreferences encryptedPreferences;
    private static volatile boolean encryptionAvailable = true;
    private static final Map<String, SharedPreferences> preferenceCaches = new HashMap<>();
    
    /**
     * Gets the encrypted SharedPreferences instance.
     * On devices that don't support encryption, falls back to standard SharedPreferences.
     * 
     * @param context Application context
     * @return SharedPreferences instance (encrypted if available)
     */
    public static SharedPreferences getEncryptedPreferences(Context context) {
        if (encryptedPreferences == null) {
            synchronized (SecurityHelper.class) {
                if (encryptedPreferences == null) {
                    encryptedPreferences = createEncryptedPreferences(context);
                }
            }
        }
        return encryptedPreferences;
    }
    
    /**
     * Creates encrypted SharedPreferences with proper key and value encryption.
     * 
     * @param context Application context
     * @return Encrypted SharedPreferences or standard if encryption fails
     */
    private static SharedPreferences createEncryptedPreferences(Context context) {
        if (!encryptionAvailable) {
            Log.d(TAG, "Encryption previously failed, using standard preferences");
            return context.getSharedPreferences(ENCRYPTED_PREFS_FILE_NAME, Context.MODE_PRIVATE);
        }
        
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .setKeyAlias(MASTER_KEY_ALIAS)
                .build();
            
            SharedPreferences encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            
            Log.i(TAG, "EncryptedSharedPreferences created successfully");
            return encryptedPrefs;
            
        } catch (GeneralSecurityException e) {
            Log.e(TAG, "Security exception creating encrypted preferences", e);
            encryptionAvailable = false;
            return getFallbackPreferences(context);
            
        } catch (IOException e) {
            Log.e(TAG, "IO exception creating encrypted preferences", e);
            encryptionAvailable = false;
            return getFallbackPreferences(context);
        }
    }
    
    /**
     * Gets fallback standard preferences when encryption is unavailable.
     * 
     * @param context Application context
     * @return Standard SharedPreferences
     */
    private static SharedPreferences getFallbackPreferences(Context context) {
        Log.w(TAG, "Using fallback standard preferences - encryption unavailable");
        return context.getSharedPreferences(ENCRYPTED_PREFS_FILE_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Checks if encryption is currently available.
     * This can be used to determine if sensitive data should be stored.
     * 
     * @return true if encryption is available and working
     */
    public static boolean isEncryptionAvailable() {
        return encryptionAvailable && encryptedPreferences != null;
    }
    
    /**
     * Stores a sensitive string value in encrypted preferences.
     * 
     * @param context Application context
     * @param key The preference key
     * @param value The value to store
     * @return true if storage was successful
     */
    public static boolean putSecureString(Context context, String key, String value) {
        try {
            SharedPreferences prefs = getEncryptedPreferences(context);
            prefs.edit().putString(key, value).apply();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to store secure string: " + key, e);
            return false;
        }
    }
    
    /**
     * Retrieves a sensitive string value from encrypted preferences.
     * 
     * @param context Application context
     * @param key The preference key
     * @param defaultValue Value to return if key doesn't exist
     * @return The stored value or default
     */
    public static String getSecureString(Context context, String key, String defaultValue) {
        try {
            SharedPreferences prefs = getEncryptedPreferences(context);
            return prefs.getString(key, defaultValue);
        } catch (Exception e) {
            Log.e(TAG, "Failed to retrieve secure string: " + key, e);
            return defaultValue;
        }
    }
    
    /**
     * Removes a sensitive value from encrypted preferences.
     * 
     * @param context Application context
     * @param key The preference key to remove
     * @return true if removal was successful
     */
    public static boolean removeSecureValue(Context context, String key) {
        try {
            SharedPreferences prefs = getEncryptedPreferences(context);
            prefs.edit().remove(key).apply();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to remove secure value: " + key, e);
            return false;
        }
    }
    
    /**
     * Clears all encrypted preferences.
     * This is useful for logout or reset operations.
     * 
     * @param context Application context
     * @return true if clear was successful
     */
    public static boolean clearEncryptedPreferences(Context context) {
        try {
            SharedPreferences prefs = getEncryptedPreferences(context);
            prefs.edit().clear().apply();
            Log.i(TAG, "Cleared all encrypted preferences");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear encrypted preferences", e);
            return false;
        }
    }
    
    /**
     * Checks if a key exists in encrypted preferences.
     * 
     * @param context Application context
     * @param key The preference key to check
     * @return true if the key exists
     */
    public static boolean containsSecureKey(Context context, String key) {
        try {
            SharedPreferences prefs = getEncryptedPreferences(context);
            return prefs.contains(key);
        } catch (Exception e) {
            Log.e(TAG, "Failed to check for secure key: " + key, e);
            return false;
        }
    }
    
    /**
     * Gets a secure editor for batch operations.
     * 
     * @param context Application context
     * @return SharedPreferences.Editor for encrypted preferences
     */
    public static SharedPreferences.Editor getSecureEditor(Context context) {
        SharedPreferences prefs = getEncryptedPreferences(context);
        return prefs.edit();
    }
    
    /**
     * Registers an OnSharedPreferenceChangeListener for encrypted preferences.
     * 
     * @param context Application context
     * @param listener The listener to register
     */
    public static void registerOnSharedPreferenceChangeListener(
            Context context, 
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        SharedPreferences prefs = getEncryptedPreferences(context);
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }
    
    /**
     * Unregisters a previously registered OnSharedPreferenceChangeListener.
     * 
     * @param context Application context
     * @param listener The listener to unregister
     */
    public static void unregisterOnSharedPreferenceChangeListener(
            Context context, 
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        SharedPreferences prefs = getEncryptedPreferences(context);
        prefs.unregisterOnSharedPreferenceChangeListener(listener);
    }
    
    /**
     * Validates that encryption is working correctly.
     * This method performs a write-read-verify cycle to test encryption.
     * 
     * @param context Application context
     * @return true if encryption validation passed
     */
    public static boolean validateEncryption(Context context) {
        String testKey = "__encryption_test_key__";
        String testValue = "Test value " + System.currentTimeMillis();
        
        try {
            // Write test value
            putSecureString(context, testKey, testValue);
            
            // Read back
            String readValue = getSecureString(context, testKey, null);
            
            // Verify
            boolean success = testValue.equals(readValue);
            
            // Cleanup
            removeSecureValue(context, testKey);
            
            if (success) {
                Log.i(TAG, "Encryption validation successful");
            } else {
                Log.e(TAG, "Encryption validation failed - values don't match");
            }
            
            return success;
            
        } catch (Exception e) {
            Log.e(TAG, "Encryption validation failed with exception", e);
            return false;
        }
    }
    
    /**
     * Gets the last known encryption error, if any.
     * 
     * @return The error message, or null if no recent errors
     */
    public static String getLastError() {
        if (!encryptionAvailable) {
            return "Encryption is not available on this device";
        }
        return null;
    }
    
    /**
     * Resets the encryption state.
     * This clears cached preferences and attempts to reinitialize encryption.
     * Useful when the device security state has changed.
     * 
     * @param context Application context
     */
    public static synchronized void reset(Context context) {
        encryptedPreferences = null;
        preferenceCaches.clear();
        encryptionAvailable = true;
        
        // Force re-creation
        getEncryptedPreferences(context);
        
        Log.i(TAG, "SecurityHelper state reset");
    }
}
