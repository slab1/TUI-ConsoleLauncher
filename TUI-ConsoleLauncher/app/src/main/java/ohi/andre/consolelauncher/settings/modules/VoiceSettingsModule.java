package ohi.andre.consolelauncher.settings.modules;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import ohi.andre.consolelauncher.settings.BaseSettingsModule;
import ohi.andre.consolelauncher.settings.ISettingsModule;
import ohi.andre.consolelauncher.settings.SettingsChangeListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Settings module for voice recognition and text-to-speech configuration.
 * This module manages voice input settings, TTS preferences, and language options
 * for both speech recognition and speech synthesis.
 */
public class VoiceSettingsModule extends BaseSettingsModule {
    
    private static final String TAG = "VoiceSettingsModule";
    public static final String MODULE_ID = "voice_settings";
    
    // Speech Recognition Keys
    private static final String KEY_RECOGNITION_ENABLED = "recognition_enabled";
    private static final String KEY_RECOGNITION_LANGUAGE = "recognition_language";
    private static final String KEY_RECOGNITION_AUTO_submit = "recognition_auto_submit";
    private static final String KEY_RECOGNITION_DOT_TIMEOUT = "recognition_dot_timeout";
    private static final String KEY_RECOGNITION_HOTWORD = "recognition_hotword";
    private static final String KEY_RECOGNITION_HOTWORD_PHRASE = "recognition_hotword_phrase";
    
    // TTS Keys
    private static final String KEY_TTS_ENABLED = "tts_enabled";
    private static final String KEY_TTS_LANGUAGE = "tts_language";
    private static final String KEY_TTS_ENGINE = "tts_engine";
    private static final String KEY_TTS_PITCH = "tts_pitch";
    private static final String KEY_TTS_SPEED = "tts_speed";
    private static final String KEY_TTS_VOLUME = "tts_volume";
    private static final String KEY_TTS_SPEAK_ERRORS = "tts_speak_errors";
    private static final String KEY_TTS_SPEAK_SUCCESS = "tts_speak_success";
    
    // General Keys
    private static final String KEY_VOICE_FEEDBACK_ENABLED = "voice_feedback_enabled";
    private static final String KEY_SILENCE_TIMEOUT = "silence_timeout";
    
    // Default Values
    private static final boolean DEFAULT_RECOGNITION_ENABLED = true;
    private static final boolean DEFAULT_RECOGNITION_AUTO_submit = false;
    private static final int DEFAULT_RECOGNITION_DOT_TIMEOUT = 2000;
    private static final boolean DEFAULT_RECOGNITION_HOTWORD = false;
    private static final String DEFAULT_RECOGNITION_HOTWORD_PHRASE = "hey tui";
    
    private static final boolean DEFAULT_TTS_ENABLED = false;
    private static final float DEFAULT_TTS_PITCH = 1.0f;
    private static final float DEFAULT_TTS_SPEED = 1.0f;
    private static final float DEFAULT_TTS_VOLUME = 1.0f;
    private static final boolean DEFAULT_TTS_SPEAK_ERRORS = true;
    private static final boolean DEFAULT_TTS_SPEAK_SUCCESS = false;
    
    private static final boolean DEFAULT_VOICE_FEEDBACK_ENABLED = true;
    private static final int DEFAULT_SILENCE_TIMEOUT = 3000;
    
    private TextToSpeech textToSpeech;
    private boolean ttsInitialized;
    private Set<OnTTSUpdateListener> ttsListeners;
    
    public VoiceSettingsModule(Context context) {
        super(context, MODULE_ID);
        this.ttsInitialized = false;
        this.ttsListeners = new HashSet<>();
    }
    
    @Override
    public void loadSettings() {
        Log.d(TAG, "Loading voice settings");
        // Settings are loaded automatically through SharedPreferences
    }
    
    @Override
    public void saveSettings() {
        clearChangedFlag();
        Log.d(TAG, "Voice settings saved");
    }
    
    @Override
    public void resetToDefaults() {
        clearAll();
        markAsChanged();
        notifyReset();
        Log.d(TAG, "Voice settings reset to defaults");
    }
    
    // ==================== Speech Recognition Settings ====================
    
    /**
     * Checks if speech recognition is enabled.
     * 
     * @return true if recognition is enabled
     */
    public boolean isRecognitionEnabled() {
        return getBoolean(KEY_RECOGNITION_ENABLED, DEFAULT_RECOGNITION_ENABLED);
    }
    
    /**
     * Sets whether speech recognition is enabled.
     * 
     * @param enabled True to enable recognition
     */
    public void setRecognitionEnabled(boolean enabled) {
        setBoolean(KEY_RECOGNITION_ENABLED, enabled);
    }
    
    /**
     * Gets the recognition language code.
     * 
     * @return Language code (e.g., "en-US"), or empty for system default
     */
    public String getRecognitionLanguage() {
        return getString(KEY_RECOGNITION_LANGUAGE, "");
    }
    
    /**
     * Sets the recognition language.
     * 
     * @param languageCode Language code (e.g., "en-US", "zh-CN")
     */
    public void setRecognitionLanguage(String languageCode) {
        setString(KEY_RECOGNITION_LANGUAGE, languageCode);
    }
    
    /**
     * Checks if auto-submit is enabled for voice input.
     * When enabled, recognized text is automatically submitted as command.
     * 
     * @return true if auto-submit is enabled
     */
    public boolean isRecognitionAutoSubmitEnabled() {
        return getBoolean(KEY_RECOGNITION_AUTO_submit, DEFAULT_RECOGNITION_AUTO_submit);
    }
    
    /**
     * Sets whether auto-submit is enabled for voice input.
     * 
     * @param enabled True to enable auto-submit
     */
    public void setRecognitionAutoSubmitEnabled(boolean enabled) {
        setBoolean(KEY_RECOGNITION_AUTO_submit, enabled);
    }
    
    /**
     * Gets the dot timeout for recognition.
     * Time in ms after which recognition ends if no speech is detected.
     * 
     * @return Timeout in milliseconds
     */
    public int getRecognitionDotTimeout() {
        return getInt(KEY_RECOGNITION_DOT_TIMEOUT, DEFAULT_RECOGNITION_DOT_TIMEOUT);
    }
    
    /**
     * Sets the dot timeout for recognition.
     * 
     * @param timeout Timeout in milliseconds
     */
    public void setRecognitionDotTimeout(int timeout) {
        setInt(KEY_RECOGNITION_DOT_TIMEOUT, timeout);
    }
    
    /**
     * Checks if hotword detection is enabled.
     * 
     * @return true if hotword detection is enabled
     */
    public boolean isHotwordEnabled() {
        return getBoolean(KEY_RECOGNITION_HOTWORD, DEFAULT_RECOGNITION_HOTWORD);
    }
    
    /**
     * Sets whether hotword detection is enabled.
     * 
     * @param enabled True to enable hotword detection
     */
    public void setHotwordEnabled(boolean enabled) {
        setBoolean(KEY_RECOGNITION_HOTWORD, enabled);
    }
    
    /**
     * Gets the hotword phrase for activation.
     * 
     * @return Hotword phrase
     */
    public String getHotwordPhrase() {
        return getString(KEY_RECOGNITION_HOTWORD_PHRASE, DEFAULT_RECOGNITION_HOTWORD_PHRASE);
    }
    
    /**
     * Sets the hotword phrase for activation.
     * 
     * @param phrase Hotword phrase
     */
    public void setHotwordPhrase(String phrase) {
        setString(KEY_RECOGNITION_HOTWORD_PHRASE, phrase);
    }
    
    // ==================== TTS Settings ====================
    
    /**
     * Checks if TTS is enabled.
     * 
     * @return true if TTS is enabled
     */
    public boolean isTtsEnabled() {
        return getBoolean(KEY_TTS_ENABLED, DEFAULT_TTS_ENABLED);
    }
    
    /**
     * Sets whether TTS is enabled.
     * 
     * @param enabled True to enable TTS
     */
    public void setTtsEnabled(boolean enabled) {
        setBoolean(KEY_TTS_ENABLED, enabled);
        if (!enabled) {
            stopTTS();
        }
    }
    
    /**
     * Gets the TTS language code.
     * 
     * @return Language code, or empty for system default
     */
    public String getTtsLanguage() {
        return getString(KEY_TTS_LANGUAGE, "");
    }
    
    /**
     * Sets the TTS language.
     * 
     * @param languageCode Language code
     */
    public void setTtsLanguage(String languageCode) {
        setString(KEY_TTS_LANGUAGE, languageCode);
    }
    
    /**
     * Gets the TTS engine package name.
     * 
     * @return Engine package name, or empty for default
     */
    public String getTtsEngine() {
        return getString(KEY_TTS_ENGINE, "");
    }
    
    /**
     * Sets the TTS engine.
     * 
     * @param enginePackageName Engine package name
     */
    public void setTtsEngine(String enginePackageName) {
        setString(KEY_TTS_ENGINE, enginePackageName);
    }
    
    /**
     * Gets the TTS pitch.
     * 
     * @return Pitch multiplier (0.5 to 2.0)
     */
    public float getTtsPitch() {
        return getFloat(KEY_TTS_PITCH, DEFAULT_TTS_PITCH);
    }
    
    /**
     * Sets the TTS pitch.
     * 
     * @param pitch Pitch multiplier (0.5 to 2.0)
     */
    public void setTtsPitch(float pitch) {
        float clamped = Math.max(0.5f, Math.min(2.0f, pitch));
        setFloat(KEY_TTS_PITCH, clamped);
    }
    
    /**
     * Gets the TTS speech rate.
     * 
     * @return Speed multiplier (0.5 to 2.0)
     */
    public float getTtsSpeed() {
        return getFloat(KEY_TTS_SPEED, DEFAULT_TTS_SPEED);
    }
    
    /**
     * Sets the TTS speech rate.
     * 
     * @param speed Speed multiplier (0.5 to 2.0)
     */
    public void setTtsSpeed(float speed) {
        float clamped = Math.max(0.5f, Math.min(2.0f, speed));
        setFloat(KEY_TTS_SPEED, clamped);
    }
    
    /**
     * Gets the TTS volume.
     * 
     * @return Volume (0.0 to 1.0)
     */
    public float getTtsVolume() {
        return getFloat(KEY_TTS_VOLUME, DEFAULT_TTS_VOLUME);
    }
    
    /**
     * Sets the TTS volume.
     * 
     * @param volume Volume (0.0 to 1.0)
     */
    public void setTtsVolume(float volume) {
        float clamped = Math.max(0.0f, Math.min(1.0f, volume));
        setFloat(KEY_TTS_VOLUME, clamped);
    }
    
    /**
     * Checks if errors should be spoken.
     * 
     * @return true if errors should be spoken
     */
    public boolean isTtsSpeakErrorsEnabled() {
        return getBoolean(KEY_TTS_SPEAK_ERRORS, DEFAULT_TTS_SPEAK_ERRORS);
    }
    
    /**
     * Sets whether errors should be spoken.
     * 
     * @param enabled True to speak errors
     */
    public void setTtsSpeakErrorsEnabled(boolean enabled) {
        setBoolean(KEY_TTS_SPEAK_ERRORS, enabled);
    }
    
    /**
     * Checks if success messages should be spoken.
     * 
     * @return true if success messages should be spoken
     */
    public boolean isTtsSpeakSuccessEnabled() {
        return getBoolean(KEY_TTS_SPEAK_SUCCESS, DEFAULT_TTS_SPEAK_SUCCESS);
    }
    
    /**
     * Sets whether success messages should be spoken.
     * 
     * @param enabled True to speak success messages
     */
    public void setTtsSpeakSuccessEnabled(boolean enabled) {
        setBoolean(KEY_TTS_SPEAK_SUCCESS, enabled);
    }
    
    // ==================== General Voice Settings ====================
    
    /**
     * Checks if voice feedback is enabled.
     * Voice feedback provides audio confirmation for actions.
     * 
     * @return true if voice feedback is enabled
     */
    public boolean isVoiceFeedbackEnabled() {
        return getBoolean(KEY_VOICE_FEEDBACK_ENABLED, DEFAULT_VOICE_FEEDBACK_ENABLED);
    }
    
    /**
     * Sets whether voice feedback is enabled.
     * 
     * @param enabled True to enable voice feedback
     */
    public void setVoiceFeedbackEnabled(boolean enabled) {
        setBoolean(KEY_VOICE_FEEDBACK_ENABLED, enabled);
    }
    
    /**
     * Gets the silence timeout.
     * Time in ms after which voice input ends due to silence.
     * 
     * @return Timeout in milliseconds
     */
    public int getSilenceTimeout() {
        return getInt(KEY_SILENCE_TIMEOUT, DEFAULT_SILENCE_TIMEOUT);
    }
    
    /**
     * Sets the silence timeout.
     * 
     * @param timeout Timeout in milliseconds
     */
    public void setSilenceTimeout(int timeout) {
        setInt(KEY_SILENCE_TIMEOUT, timeout);
    }
    
    // ==================== TTS Management ====================
    
    /**
     * Initializes the TTS engine.
     * Should be called when the activity starts.
     * 
     * @param listener Listener for initialization result
     */
    public void initializeTTS(TextToSpeech.OnInitListener listener) {
        if (textToSpeech != null) {
            return; // Already initialized
        }
        
        try {
            textToSpeech = new TextToSpeech(context, listener);
            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    notifyTtsStarted(utteranceId);
                }
                
                @Override
                public void onDone(String utteranceId) {
                    notifyTtsCompleted(utteranceId);
                }
                
                @Override
                public void onError(String utteranceId) {
                    notifyTtsError(utteranceId);
                }
                
                @Override
                public void onError(String utteranceId, int errorCode) {
                    notifyTtsError(utteranceId, errorCode);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize TTS", e);
        }
    }
    
    /**
     * Speaks the given text using TTS.
     * 
     * @param text Text to speak
     * @param utteranceId Unique identifier for this utterance
     * @return true if speaking started successfully
     */
    public boolean speak(String text, String utteranceId) {
        if (!isTtsEnabled() || textToSpeech == null || !ttsInitialized) {
            return false;
        }
        
        try {
            float pitch = getTtsPitch();
            float speed = getTtsSpeed();
            float volume = getTtsVolume();
            
            textToSpeech.setPitch(pitch);
            textToSpeech.setSpeechRate(speed);
            
            int result = textToSpeech.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                utteranceId
            );
            
            return result == TextToSpeech.SUCCESS;
            
        } catch (Exception e) {
            Log.e(TAG, "TTS speak failed", e);
            return false;
        }
    }
    
    /**
     * Speaks the given text using TTS with queuing.
     * 
     * @param text Text to speak
     * @param utteranceId Unique identifier for this utterance
     * @return true if queued successfully
     */
    public boolean speakQueued(String text, String utteranceId) {
        if (!isTtsEnabled() || textToSpeech == null || !ttsInitialized) {
            return false;
        }
        
        try {
            int result = textToSpeech.speak(
                text,
                TextToSpeech.QUEUE_ADD,
                null,
                utteranceId
            );
            
            return result == TextToSpeech.SUCCESS;
            
        } catch (Exception e) {
            Log.e(TAG, "TTS queued speak failed", e);
            return false;
        }
    }
    
    /**
     * Stops TTS playback.
     */
    public void stopTTS() {
        if (textToSpeech != null) {
            try {
                textToSpeech.stop();
            } catch (Exception e) {
                Log.e(TAG, "Failed to stop TTS", e);
            }
        }
    }
    
    /**
     * Checks if TTS is currently speaking.
     * 
     * @return true if TTS is speaking
     */
    public boolean isSpeaking() {
        if (textToSpeech == null) {
            return false;
        }
        
        try {
            return textToSpeech.isSpeaking();
        } catch (Exception e) {
            Log.e(TAG, "Failed to check TTS status", e);
            return false;
        }
    }
    
    /**
     * Gets available TTS languages.
     * 
     * @return Set of available Locale objects
     */
    public Set<Locale> getAvailableTtsLanguages() {
        if (textToSpeech == null) {
            return new HashSet<>();
        }
        
        try {
            return textToSpeech.getAvailableLanguages();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get TTS languages", e);
            return new HashSet<>();
        }
    }
    
    /**
     * Sets the TTS language.
     * 
     * @param locale The locale to use
     * @return TextToSpeech.LANG_* result code
     */
    public int setTtsLocale(Locale locale) {
        if (textToSpeech == null) {
            return TextToSpeech.LANG_MISSING_DATA;
        }
        
        try {
            return textToSpeech.setLanguage(locale);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set TTS locale", e);
            return TextToSpeech.LANG_MISSING_DATA;
        }
    }
    
    /**
     * Gets the current TTS language.
     * 
     * @return Current language locale, or null if not set
     */
    public Locale getTtsLocale() {
        if (textToSpeech == null) {
            return null;
        }
        
        try {
            return textToSpeech.getLanguage();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get TTS locale", e);
            return null;
        }
    }
    
    /**
     * Checks if TTS data is available for the current language.
     * 
     * @return true if data is available
     */
    public boolean isTtsDataAvailable() {
        if (textToSpeech == null) {
            return false;
        }
        
        Locale currentLocale = getTtsLocale();
        if (currentLocale == null) {
            return true; // Using system default
        }
        
        try {
            int availability = textToSpeech.isLanguageAvailable(currentLocale);
            return availability >= TextToSpeech.LANG_AVAILABLE;
        } catch (Exception e) {
            Log.e(TAG, "Failed to check TTS availability", e);
            return false;
        }
    }
    
    /**
     * Shuts down TTS resources.
     * Should be called when the activity is destroyed.
     */
    public void shutdownTTS() {
        if (textToSpeech != null) {
            try {
                textToSpeech.stop();
                textToSpeech.shutdown();
                textToSpeech = null;
                ttsInitialized = false;
                Log.d(TAG, "TTS shutdown complete");
            } catch (Exception e) {
                Log.e(TAG, "Failed to shutdown TTS", e);
            }
        }
    }
    
    /**
     * Sets the TTS initialization status.
     * 
     * @param initialized true if TTS is initialized
     */
    public void setTtsInitialized(boolean initialized) {
        this.ttsInitialized = initialized;
    }
    
    /**
     * Checks if TTS is initialized.
     * 
     * @return true if TTS is initialized
     */
    public boolean isTtsInitialized() {
        return ttsInitialized;
    }
    
    /**
     * Registers a TTS update listener.
     * 
     * @param listener Listener to register
     */
    public void registerTtsListener(OnTTSUpdateListener listener) {
        if (listener != null) {
            ttsListeners.add(listener);
        }
    }
    
    /**
     * Unregisters a TTS update listener.
     * 
     * @param listener Listener to unregister
     */
    public void unregisterTtsListener(OnTTSUpdateListener listener) {
        ttsListeners.remove(listener);
    }
    
    private void notifyTtsStarted(String utteranceId) {
        for (OnTTSUpdateListener listener : ttsListeners) {
            listener.onTtsStarted(utteranceId);
        }
    }
    
    private void notifyTtsCompleted(String utteranceId) {
        for (OnTTSUpdateListener listener : ttsListeners) {
            listener.onTtsCompleted(utteranceId);
        }
    }
    
    private void notifyTtsError(String utteranceId) {
        for (OnTTSUpdateListener listener : ttsListeners) {
            listener.onTtsError(utteranceId, -1);
        }
    }
    
    private void notifyTtsError(String utteranceId, int errorCode) {
        for (OnTTSUpdateListener listener : ttsListeners) {
            listener.onTtsError(utteranceId, errorCode);
        }
    }
    
    @Override
    public List<SettingEntry> exportSettings() {
        List<SettingEntry> entries = new ArrayList<>();
        
        entries.add(new SettingEntry(KEY_RECOGNITION_ENABLED, 
            isRecognitionEnabled(), SettingType.BOOLEAN));
        entries.add(new SettingEntry(KEY_RECOGNITION_LANGUAGE, 
            getRecognitionLanguage(), SettingType.STRING));
        entries.add(new SettingEntry(KEY_RECOGNITION_AUTO_submit, 
            isRecognitionAutoSubmitEnabled(), SettingType.BOOLEAN));
        entries.add(new SettingEntry(KEY_RECOGNITION_DOT_TIMEOUT, 
            getRecognitionDotTimeout(), SettingType.INTEGER));
        entries.add(new SettingEntry(KEY_RECOGNITION_HOTWORD, 
            isHotwordEnabled(), SettingType.BOOLEAN));
        entries.add(new SettingEntry(KEY_RECOGNITION_HOTWORD_PHRASE, 
            getHotwordPhrase(), SettingType.STRING));
        
        entries.add(new SettingEntry(KEY_TTS_ENABLED, 
            isTtsEnabled(), SettingType.BOOLEAN));
        entries.add(new SettingEntry(KEY_TTS_LANGUAGE, 
            getTtsLanguage(), SettingType.STRING));
        entries.add(new SettingEntry(KEY_TTS_ENGINE, 
            getTtsEngine(), SettingType.STRING));
        entries.add(new SettingEntry(KEY_TTS_PITCH, 
            getTtsPitch(), SettingType.FLOAT));
        entries.add(new SettingEntry(KEY_TTS_SPEED, 
            getTtsSpeed(), SettingType.FLOAT));
        entries.add(new SettingEntry(KEY_TTS_VOLUME, 
            getTtsVolume(), SettingType.FLOAT));
        entries.add(new SettingEntry(KEY_TTS_SPEAK_ERRORS, 
            isTtsSpeakErrorsEnabled(), SettingType.BOOLEAN));
        entries.add(new SettingEntry(KEY_TTS_SPEAK_SUCCESS, 
            isTtsSpeakSuccessEnabled(), SettingType.BOOLEAN));
        
        entries.add(new SettingEntry(KEY_VOICE_FEEDBACK_ENABLED, 
            isVoiceFeedbackEnabled(), SettingType.BOOLEAN));
        entries.add(new SettingEntry(KEY_SILENCE_TIMEOUT, 
            getSilenceTimeout(), SettingType.INTEGER));
        
        return entries;
    }
    
    @Override
    public boolean importSettings(List<SettingEntry> entries) {
        for (SettingEntry entry : entries) {
            switch (entry.getKey()) {
                case KEY_RECOGNITION_ENABLED:
                    setBoolean(KEY_RECOGNITION_ENABLED, entry.getBooleanValue());
                    break;
                case KEY_RECOGNITION_LANGUAGE:
                    setString(KEY_RECOGNITION_LANGUAGE, entry.getStringValue());
                    break;
                case KEY_RECOGNITION_AUTO_submit:
                    setBoolean(KEY_RECOGNITION_AUTO_submit, entry.getBooleanValue());
                    break;
                case KEY_RECOGNITION_DOT_TIMEOUT:
                    setInt(KEY_RECOGNITION_DOT_TIMEOUT, entry.getIntValue());
                    break;
                case KEY_RECOGNITION_HOTWORD:
                    setBoolean(KEY_RECOGNITION_HOTWORD, entry.getBooleanValue());
                    break;
                case KEY_RECOGNITION_HOTWORD_PHRASE:
                    setString(KEY_RECOGNITION_HOTWORD_PHRASE, entry.getStringValue());
                    break;
                case KEY_TTS_ENABLED:
                    setBoolean(KEY_TTS_ENABLED, entry.getBooleanValue());
                    break;
                case KEY_TTS_LANGUAGE:
                    setString(KEY_TTS_LANGUAGE, entry.getStringValue());
                    break;
                case KEY_TTS_ENGINE:
                    setString(KEY_TTS_ENGINE, entry.getStringValue());
                    break;
                case KEY_TTS_PITCH:
                    setFloat(KEY_TTS_PITCH, entry.getFloatValue());
                    break;
                case KEY_TTS_SPEED:
                    setFloat(KEY_TTS_SPEED, entry.getFloatValue());
                    break;
                case KEY_TTS_VOLUME:
                    setFloat(KEY_TTS_VOLUME, entry.getFloatValue());
                    break;
                case KEY_TTS_SPEAK_ERRORS:
                    setBoolean(KEY_TTS_SPEAK_ERRORS, entry.getBooleanValue());
                    break;
                case KEY_TTS_SPEAK_SUCCESS:
                    setBoolean(KEY_TTS_SPEAK_SUCCESS, entry.getBooleanValue());
                    break;
                case KEY_VOICE_FEEDBACK_ENABLED:
                    setBoolean(KEY_VOICE_FEEDBACK_ENABLED, entry.getBooleanValue());
                    break;
                case KEY_SILENCE_TIMEOUT:
                    setInt(KEY_SILENCE_TIMEOUT, entry.getIntValue());
                    break;
            }
        }
        return true;
    }
    
    /**
     * Interface for TTS update callbacks.
     */
    public interface OnTTSUpdateListener {
        void onTtsStarted(String utteranceId);
        void onTtsCompleted(String utteranceId);
        void onTtsError(String utteranceId, int errorCode);
    }
}
