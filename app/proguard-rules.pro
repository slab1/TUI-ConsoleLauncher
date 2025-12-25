-keep public class ohi.andre.consolelauncher.commands.main.raw.* { public *; }
-keep public abstract class ohi.andre.consolelauncher.commands.main.generals.* { public *; }
-keep public class ohi.andre.consolelauncher.commands.tuixt.raw.* { public *; }
-keep public class ohi.andre.consolelauncher.managers.notifications.NotificationService
-keep public class ohi.andre.consolelauncher.managers.notifications.KeeperService
-keep public class ohi.andre.consolelauncher.managers.options.**
-keep class ohi.andre.consolelauncher.tuils.libsuperuser.**
-keep class ohi.andre.consolelauncher.managers.suggestions.HideSuggestionViewValues
-keep public class it.andreuzzi.comparestring2.**

-dontwarn ohi.andre.consolelauncher.commands.main.raw.**

-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn sun.misc.Unsafe

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

-dontwarn org.htmlcleaner.**
-dontwarn com.jayway.jsonpath.**
-dontwarn org.slf4j.**

-dontwarn org.jdom2.**

# ============================================
# OWASP SECURITY COMPLIANCE RULES
# ============================================

# M8: Code Tampering & M9: Reverse Engineering Protection

# ============================================
# SECURITY CLASSES (Keep with full names)
# ============================================

# Keep security-related classes
-keep class ohi.andre.consolelauncher.commands.smartlauncher.developer.security.** { *; }

# Keep InputValidator patterns
-keep class ohi.andre.consolelauncher.commands.smartlauncher.developer.security.InputValidator$ValidatedRequest { *; }
-keep class ohi.andre.consolelauncher.commands.smartlauncher.developer.security.InputValidator$ValidatedDebugCommand { *; }
-keep class ohi.andre.consolelauncher.commands.smartlauncher.developer.security.InputValidator$ValidationStats { *; }

# Keep SecurityLogger classes
-keep class ohi.andre.consolelauncher.commands.smartlauncher.developer.security.SecurityLogger$SecurityEvent { *; }
-keep class ohi.andre.consolelauncher.commands.smartlauncher.developer.security.SecurityLogger$SecurityStats { *; }

# Keep SecurityLogger enum
-keepclassmembers class ohi.andre.consolelauncher.commands.smartlauncher.developer.security.SecurityLogger$SecurityEventType { *; }
-keepclassmembers class ohi.andre.consolelauncher.commands.smartlauncher.developer.security.SecurityLogger$SeverityLevel { *; }

# ============================================
# WEBVIEW SECURITY
# ============================================

# Keep WebView callback methods
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep WebViewClient subclasses
-keep class * extends android.webkit.WebViewClient { *; }

# Keep WebChromeClient subclasses  
-keep class * extends android.webkit.WebChromeClient { *; }

# ============================================
# ENCRYPTION & SECURITY
# ============================================

# AndroidX Security Crypto
-keep class androidx.security.crypto.** { *; }

# Keep encryption-related classes
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }

# MasterKey and related classes
-keep class androidx.security.crypto.MasterKey { *; }
-keep class androidx.security.crypto.EncryptedFile { *; }
-keep class androidx.security.crypto.EncryptedSharedPreferences { *; }

# ============================================
# OBFUSCATION RULES
# ============================================

# Apply to all classes
-optimizationpasses 5
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# Package-level obfuscation
-repackageclasses ''

# Flatten packages
-flattenpackagehierarchy '.'

# ============================================
# ANNOTATION PRESERVATION
# ============================================

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Preserve enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ============================================
# REFLECTION PRESERVATION
# ============================================

# Security checks often use reflection
-keep class java.lang.reflect.** { *; }
-keepclassmembers class * {
    @java.lang.reflect.* <methods>;
}

# ============================================
# SERIALIZATION
# ============================================

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============================================
# LOGGING
# ============================================

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# But keep security logs
-keep class ohi.andre.consolelauncher.commands.smartlauncher.developer.security.SecurityLogger { *; }

# ============================================
# MONACO EDITOR ASSETS
# ============================================

# Don't obfuscate Monaco Editor assets
-keep class assets.** { *; }
-keepclassmembers class ** {
    @android.webkit.JavascriptInterface <methods>;
}

# ============================================
# JSON PROCESSING
# ============================================

# Keep JSON libraries
-keep class org.json.** { *; }

# Keep GSON/JSON serialization classes
-keepattributes Signature
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ============================================
# NATIVE METHODS
# ============================================

# Keep native method names
-keepclasseswithmembernames class * {
    native <methods>;
}

# ============================================
# SECURITY-SPECIFIC RULES
# ============================================

# Prevent modification of security classes
-keep class ohi.andre.consolelauncher.commands.smartlauncher.developer.security.** { *; }
-keepclassmembers class ohi.andre.consolelauncher.commands.smartlauncher.developer.security.** { *; }

# Preserve method signatures used by reflection
-keepclassmembernames class * {
    java.lang.Class class$(java.lang.String);
    java.lang.Class class$(java.lang.String, boolean);
}

# Prevent access to security constants
-keepclassmembers class ** {
    public static ** SECRET_KEY;
    public static ** API_KEY;
    public static ** PASSWORD;
}

# ============================================
# DEBUGGING & TESTING
# ============================================

# Remove debug information
-keepattributes SourceFile,LineNumberTable

# Add debugging help in debug builds
-renamesourcefileattribute SourceFile

# ============================================
# OPTIMIZATIONS
# ============================================

# Enable optimizations
-optimizationpasses 5
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# Remove unused classes
-mergecommonsaggressively

# Remove debug blocks
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

# ============================================
# CRITICAL: PREVENT CLASS REMOVAL
# ============================================

# Keep all security-related classes
-keep class com.security.** { *; }
-keep class ohi.andre.consolelauncher.commands.smartlauncher.developer.security.** { *; }

# Keep WebView interfaces
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep View.OnClickListener
-keepclassmembers class * implements android.view.View.OnClickListener {
    public abstract void onClick(android.view.View);
}

# ============================================
# RESOURCE SHRINKING
# ============================================

# Enable resource shrinking
-reusecolors false
-reusewidths false
-reuseheights false