package ohi.andre.consolelauncher.ai;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Helper class for text-to-speech (TTS) functionality.
 * This class wraps Android's TextToSpeech engine to provide easy integration
 * for speaking AI responses and feedback within the T-UI console.
 */
public class TTSHelper {
    
    private static final String TAG = "TTSHelper";
    
    private final Context context;
    private TextToSpeech textToSpeech;
    private boolean isInitialized;
    private boolean isSpeaking;
    private float pitch;
    private float speed;
    private float volume;
    private Locale preferredLocale;
    private final List<OnTTSListener> listeners;
    private final Set<String> pendingUtterances;
    private final ScheduledExecutorService scheduler;
    
    public TTSHelper(Context context) {
        this.context = context.getApplicationContext();
        this.listeners = new CopyOnWriteArrayList<>();
        this.pendingUtterances = new HashSet<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.isInitialized = false;
        this.isSpeaking = false;
        this.pitch = 1.0f;
        this.speed = 1.0f;
        this.volume = 1.0f;
        this.preferredLocale = Locale.getDefault();
    }
    
    /**
     * Initializes the TTS engine.
     * Should be called when the activity starts.
     * 
     * @param listener Listener for initialization result
     */
    public void initialize(TextToSpeech.OnInitListener listener) {
        if (textToSpeech != null) {
            // Already initialized
            if (listener != null) {
                listener.onInit(TextToSpeech.SUCCESS);
            }
            return;
        }
        
        try {
            textToSpeech = new TextToSpeech(context, status -> {
                Log.d(TAG, "TTS init status: " + status);
                
                if (status == TextToSpeech.SUCCESS) {
                    isInitialized = true;
                    configureEngine();
                    setupProgressListener();
                    Log.d(TAG, "TTS initialized successfully");
                } else {
                    isInitialized = false;
                    Log.e(TAG, "TTS initialization failed with status: " + status);
                }
                
                if (listener != null) {
                    listener.onInit(status);
                }
                
                notifyInitialized(status == TextToSpeech.SUCCESS);
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to create TTS engine", e);
            isInitialized = false;
            if (listener != null) {
                listener.onInit(TextToSpeech.ERROR);
            }
        }
    }
    
    /**
     * Initializes the TTS engine with default listener.
     */
    public void initialize() {
        initialize(status -> {
            if (status != TextToSpeech.SUCCESS) {
                Log.w(TAG, "TTS initialization failed");
            }
        });
    }
    
    /**
     * Configures the TTS engine with current settings.
     */
    private void configureEngine() {
        if (textToSpeech == null) return;
        
        try {
            // Set pitch
            textToSpeech.setPitch(pitch);
            
            // Set speech rate
            textToSpeech.setSpeechRate(speed);
            
            // Set language
            setLanguage(preferredLocale);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to configure TTS engine", e);
        }
    }
    
    /**
     * Sets up the utterance progress listener.
     */
    private void setupProgressListener() {
        if (textToSpeech == null) return;
        
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                isSpeaking = true;
                pendingUtterances.add(utteranceId);
                Log.d(TAG, "Started speaking: " + utteranceId);
                notifySpeechStarted(utteranceId);
            }
            
            @Override
            public void onDone(String utteranceId) {
                isSpeaking = pendingUtterances.size() > 1;
                pendingUtterances.remove(utteranceId);
                Log.d(TAG, "Finished speaking: " + utteranceId);
                notifySpeechCompleted(utteranceId);
                
                // Cleanup if no more pending utterances
                if (pendingUtterances.isEmpty()) {
                    isSpeaking = false;
                }
            }
            
            @Override
            public void onError(String utteranceId) {
                isSpeaking = !pendingUtterances.isEmpty();
                pendingUtterances.remove(utteranceId);
                Log.e(TAG, "Error speaking: " + utteranceId);
                notifySpeechError(utteranceId, -1);
            }
            
            @Override
            public void onError(String utteranceId, int errorCode) {
                isSpeaking = !pendingUtterances.isEmpty();
                pendingUtterances.remove(utteranceId);
                Log.e(TAG, "Error speaking " + utteranceId + ": " + errorCode);
                notifySpeechError(utteranceId, errorCode);
            }
        });
    }
    
    /**
     * Speaks the given text.
     * 
     * @param text Text to speak
     * @return true if speaking started successfully
     */
    public boolean speak(String text) {
        return speak(text, null);
    }
    
    /**
     * Speaks the given text with a custom utterance ID.
     * 
     * @param text Text to speak
     * @param utteranceId Custom utterance ID (auto-generated if null)
     * @return true if speaking started successfully
     */
    public boolean speak(String text, String utteranceId) {
        if (!isInitialized || textToSpeech == null) {
            Log.w(TAG, "Cannot speak - TTS not initialized");
            return false;
        }
        
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        // Generate utterance ID if not provided
        String id = utteranceId != null ? utteranceId : UUID.randomUUID().toString();
        
        try {
            // Apply current settings
            textToSpeech.setPitch(pitch);
            textToSpeech.setSpeechRate(speed);
            
            int result = textToSpeech.speak(
                text,
                TextToSpeech.QUEUE_FLUSH, // Flush previous utterances
                null,
                id
            );
            
            boolean success = result == TextToSpeech.SUCCESS;
            if (!success) {
                Log.e(TAG, "TTS speak failed with result: " + result);
            }
            
            return success;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to speak text", e);
            return false;
        }
    }
    
    /**
     * Queues text to be spoken after current utterances.
     * 
     * @param text Text to queue
     * @return true if queued successfully
     */
    public boolean speakQueued(String text) {
        return speakQueued(text, null);
    }
    
    /**
     * Queues text with custom utterance ID.
     * 
     * @param text Text to queue
     * @param utteranceId Custom utterance ID
     * @return true if queued successfully
     */
    public boolean speakQueued(String text, String utteranceId) {
        if (!isInitialized || textToSpeech == null) {
            return false;
        }
        
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        String id = utteranceId != null ? utteranceId : UUID.randomUUID().toString();
        
        try {
            int result = textToSpeech.speak(
                text,
                TextToSpeech.QUEUE_ADD, // Add to queue
                null,
                id
            );
            
            return result == TextToSpeech.SUCCESS;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to queue text", e);
            return false;
        }
    }
    
    /**
     * Speaks multiple texts sequentially.
     * 
     * @param texts List of texts to speak
     * @return true if all texts queued successfully
     */
    public boolean speakAll(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return false;
        }
        
        if (!isInitialized || textToSpeech == null) {
            return false;
        }
        
        try {
            textToSpeech.setPitch(pitch);
            textToSpeech.setSpeechRate(speed);
            
            String firstId = UUID.randomUUID().toString();
            StringBuilder allText = new StringBuilder();
            
            for (int i = 0; i < texts.size(); i++) {
                String text = texts.get(i);
                String id = i == 0 ? firstId : UUID.randomUUID().toString();
                
                int queueMode = i == 0 ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD;
                int result = textToSpeech.speak(text, queueMode, null, id);
                
                if (result != TextToSpeech.SUCCESS) {
                    return false;
                }
                
                allText.append(text).append(" ");
            }
            
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to speak multiple texts", e);
            return false;
        }
    }
    
    /**
     * Stops all current and queued speech.
     */
    public void stop() {
        if (textToSpeech != null) {
            try {
                textToSpeech.stop();
                pendingUtterances.clear();
                isSpeaking = false;
                Log.d(TAG, "TTS stopped");
                notifyStopped();
            } catch (Exception e) {
                Log.e(TAG, "Failed to stop TTS", e);
            }
        }
    }
    
    /**
     * Checks if TTS is currently speaking.
     * 
     * @return true if speaking
     */
    public boolean isSpeaking() {
        if (textToSpeech == null) {
            return false;
        }
        
        try {
            return textToSpeech.isSpeaking();
        } catch (Exception e) {
            Log.e(TAG, "Failed to check speaking status", e);
            return false;
        }
    }
    
    /**
     * Sets the speech pitch.
     * 
     * @param pitch Pitch multiplier (0.5 to 2.0)
     */
    public void setPitch(float pitch) {
        this.pitch = Math.max(0.5f, Math.min(2.0f, pitch));
        if (textToSpeech != null) {
            textToSpeech.setPitch(this.pitch);
        }
    }
    
    /**
     * Sets the speech rate.
     * 
     * @param speed Speed multiplier (0.5 to 2.0)
     */
    public void setSpeed(float speed) {
        this.speed = Math.max(0.5f, Math.min(2.0f, speed));
        if (textToSpeech != null) {
            textToSpeech.setSpeechRate(this.speed);
        }
    }
    
    /**
     * Sets the speech volume.
     * 
     * @param volume Volume (0.0 to 1.0)
     */
    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
    }
    
    /**
     * Sets the language for TTS.
     * 
     * @param locale Language locale
     * @return true if language is available
     */
    public boolean setLanguage(Locale locale) {
        if (textToSpeech == null) {
            preferredLocale = locale;
            return false;
        }
        
        try {
            int result = textToSpeech.setLanguage(locale);
            boolean available = result >= TextToSpeech.LANG_AVAILABLE;
            
            if (available) {
                preferredLocale = locale;
                Log.d(TAG, "TTS language set to: " + locale.toString());
            } else {
                Log.w(TAG, "TTS language not available: " + locale.toString());
            }
            
            return available;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to set TTS language", e);
            return false;
        }
    }
    
    /**
     * Gets the current language.
     * 
     * @return Current language locale
     */
    public Locale getLanguage() {
        if (textToSpeech == null) {
            return preferredLocale;
        }
        
        try {
            return textToSpeech.getLanguage();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get TTS language", e);
            return preferredLocale;
        }
    }
    
    /**
     * Gets available TTS languages.
     * 
     * @return Set of available locales
     */
    public Set<Locale> getAvailableLanguages() {
        if (textToSpeech == null) {
            return new HashSet<>();
        }
        
        try {
            return textToSpeech.getAvailableLanguages();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get available languages", e);
            return new HashSet<>();
        }
    }
    
    /**
     * Checks if TTS data is available for a language.
     * 
     * @param locale Language locale
     * @return true if data is available
     */
    public boolean isLanguageAvailable(Locale locale) {
        if (textToSpeech == null) {
            return false;
        }
        
        try {
            int availability = textToSpeech.isLanguageAvailable(locale);
            return availability >= TextToSpeech.LANG_AVAILABLE;
        } catch (Exception e) {
            Log.e(TAG, "Failed to check language availability", e);
            return false;
        }
    }
    
    /**
     * Sets the TTS engine by package name.
     * 
     * @param enginePackageName Engine package name
     * @return true if engine was set
     */
    public boolean setEngine(String enginePackageName) {
        if (textToSpeech == null) {
            return false;
        }
        
        try {
            int result = textToSpeech.setEngineByPackageName(enginePackageName);
            return result == TextToSpeech.SUCCESS;
        } catch (Exception e) {
            Log.e(TAG, "Failed to set TTS engine", e);
            return false;
        }
    }
    
    /**
     * Gets the current pitch value.
     * 
     * @return Current pitch
     */
    public float getPitch() {
        return pitch;
    }
    
    /**
     * Gets the current speed value.
     * 
     * @return Current speed
     */
    public float getSpeed() {
        return speed;
    }
    
    /**
     * Gets the current volume.
     * 
     * @return Current volume
     */
    public float getVolume() {
        return volume;
    }
    
    /**
     * Checks if TTS is initialized.
     * 
     * @return true if initialized
     */
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * Registers a TTS listener.
     * 
     * @param listener Listener to register
     */
    public void registerListener(OnTTSListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * Unregisters a TTS listener.
     * 
     * @param listener Listener to unregister
     */
    public void unregisterListener(OnTTSListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyInitialized(boolean success) {
        for (OnTTSListener listener : listeners) {
            listener.onInitialized(success);
        }
    }
    
    private void notifySpeechStarted(String utteranceId) {
        for (OnTTSListener listener : listeners) {
            listener.onSpeechStarted(utteranceId);
        }
    }
    
    private void notifySpeechCompleted(String utteranceId) {
        for (OnTTSListener listener : listeners) {
            listener.onSpeechCompleted(utteranceId);
        }
    }
    
    private void notifySpeechError(String utteranceId, int errorCode) {
        for (OnTTSListener listener : listeners) {
            listener.onSpeechError(utteranceId, errorCode);
        }
    }
    
    private void notifyStopped() {
        for (OnTTSListener listener : listeners) {
            listener.onStopped();
        }
    }
    
    /**
     * Shuts down the TTS engine.
     * Should be called when the activity is destroyed.
     */
    public void shutdown() {
        stop();
        
        if (textToSpeech != null) {
            try {
                textToSpeech.shutdown();
                textToSpeech = null;
                isInitialized = false;
                Log.d(TAG, "TTS shutdown complete");
            } catch (Exception e) {
                Log.e(TAG, "Failed to shutdown TTS", e);
            }
        }
        
        // Shutdown scheduler
        scheduler.shutdown();
    }
    
    /**
     * Interface for TTS callbacks.
     */
    public interface OnTTSListener {
        void onInitialized(boolean success);
        void onSpeechStarted(String utteranceId);
        void onSpeechCompleted(String utteranceId);
        void onSpeechError(String utteranceId, int errorCode);
        void onStopped();
    }
}
