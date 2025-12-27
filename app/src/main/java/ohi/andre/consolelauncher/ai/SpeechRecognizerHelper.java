package ohi.andre.consolelauncher.ai;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Helper class for speech recognition functionality.
 * This class wraps Android's SpeechRecognizer to provide easy integration
 * with the T-UI console for voice command input.
 */
public class SpeechRecognizerHelper {
    
    private static final String TAG = "SpeechRecognizer";
    
    private final Context context;
    private SpeechRecognizer speechRecognizer;
    private boolean isListening;
    private boolean isAvailable;
    private final List<RecognitionListenerImpl> listeners;
    
    public SpeechRecognizerHelper(Context context) {
        this.context = context.getApplicationContext();
        this.listeners = new CopyOnWriteArrayList<>();
        this.isListening = false;
        this.isAvailable = checkAvailability();
    }
    
    /**
     * Checks if speech recognition is available on this device.
     * 
     * @return true if speech recognition is available
     */
    public boolean checkAvailability() {
        try {
            boolean available = SpeechRecognizer.isRecognitionAvailable(context);
            Log.d(TAG, "Speech recognition available: " + available);
            return available;
        } catch (Exception e) {
            Log.e(TAG, "Failed to check recognition availability", e);
            return false;
        }
    }
    
    /**
     * Initializes the speech recognizer.
     * Should be called when the activity starts.
     * 
     * @return true if initialization successful
     */
    public boolean initialize() {
        if (speechRecognizer != null) {
            return true; // Already initialized
        }
        
        if (!isAvailable) {
            Log.w(TAG, "Cannot initialize - speech recognition not available");
            return false;
        }
        
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
            speechRecognizer.setRecognitionListener(new RecognitionListenerImpl());
            Log.d(TAG, "Speech recognizer initialized");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize speech recognizer", e);
            return false;
        }
    }
    
    /**
     * Starts listening for speech input.
     * 
     * @param language Language for recognition (null for system default)
     * @param partialResults Whether to receive partial results
     * @return true if listening started successfully
     */
    public boolean startListening(Locale language, boolean partialResults) {
        if (speechRecognizer == null || isListening) {
            return false;
        }
        
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        
        // Use system language if not specified
        Locale locale = language != null ? language : Locale.getDefault();
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toString());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, 
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        
        // Configure recognition
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, partialResults);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        
        try {
            speechRecognizer.startListening(intent);
            isListening = true;
            Log.d(TAG, "Started listening in " + locale.toString());
            notifyListeningStarted();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to start listening", e);
            isListening = false;
            return false;
        }
    }
    
    /**
     * Starts listening with default settings.
     * 
     * @return true if listening started successfully
     */
    public boolean startListening() {
        return startListening(null, true);
    }
    
    /**
     * Stops listening for speech.
     */
    public void stopListening() {
        if (speechRecognizer != null && isListening) {
            try {
                speechRecognizer.stopListening();
            } catch (Exception e) {
                Log.e(TAG, "Failed to stop listening", e);
            }
            isListening = false;
            notifyListeningStopped();
        }
    }
    
    /**
     * Cancels the current recognition session.
     */
    public void cancel() {
        if (speechRecognizer != null) {
            try {
                speechRecognizer.cancel();
                isListening = false;
            } catch (Exception e) {
                Log.e(TAG, "Failed to cancel recognition", e);
            }
        }
    }
    
    /**
     * Gets the command hints for improved recognition.
     * 
     * @return List of command phrases to hint
     */
    private List<String> getCommandHints() {
        List<String> hints = new ArrayList<>();
        hints.add("ai");
        hints.add("run");
        hints.add("open");
        hints.add("calculate");
        hints.add("search");
        hints.add("help");
        hints.add("status");
        return hints;
    }
    
    /**
     * Gets the current listening state.
     * 
     * @return true if currently listening
     */
    public boolean isListening() {
        return isListening;
    }
    
    /**
     * Checks if speech recognition is available.
     * 
     * @return true if available
     */
    public boolean isAvailable() {
        return isAvailable;
    }
    
    /**
     * Registers a recognition listener.
     * 
     * @param listener Listener to register
     */
    public void registerListener(OnRecognitionListener listener) {
        if (listener != null) {
            listeners.add(new RecognitionListenerImpl(listener));
        }
    }
    
    /**
     * Unregisters a recognition listener.
     * 
     * @param listener Listener to unregister
     */
    public void unregisterListener(OnRecognitionListener listener) {
        listeners.removeIf(impl -> impl.userListener == listener);
    }
    
    /**
     * Shuts down the speech recognizer.
     * Should be called when the activity is destroyed.
     */
    public void shutdown() {
        cancel();
        if (speechRecognizer != null) {
            try {
                speechRecognizer.destroy();
                speechRecognizer = null;
                Log.d(TAG, "Speech recognizer destroyed");
            } catch (Exception e) {
                Log.e(TAG, "Failed to destroy speech recognizer", e);
            }
        }
    }
    
    private void notifyRecognitionResult(String result, boolean isPartial) {
        for (RecognitionListenerImpl listener : listeners) {
            listener.onRecognitionResult(result, isPartial);
        }
    }
    
    private void notifyPartialResult(String partialResult) {
        for (RecognitionListenerImpl listener : listeners) {
            listener.onPartialResult(partialResult);
        }
    }
    
    private void notifyError(int errorCode, String errorMessage) {
        for (RecognitionListenerImpl listener : listeners) {
            listener.onError(errorCode, errorMessage);
        }
    }
    
    private void notifyListeningStarted() {
        for (RecognitionListenerImpl listener : listeners) {
            listener.onListeningStarted();
        }
    }
    
    private void notifyListeningStopped() {
        for (RecognitionListenerImpl listener : listeners) {
            listener.onListeningStopped();
        }
    }
    
    /**
     * Internal recognition listener implementation.
     */
    private class RecognitionListenerImpl implements RecognitionListener {
        private final OnRecognitionListener userListener;
        
        RecognitionListenerImpl() {
            this.userListener = null;
        }
        
        RecognitionListenerImpl(OnRecognitionListener listener) {
            this.userListener = listener;
        }
        
        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.d(TAG, "Ready for speech");
        }
        
        @Override
        public void onBeginningOfSpeech() {
            Log.d(TAG, "Beginning of speech detected");
        }
        
        @Override
        public void onRmsChanged(float rmsdB) {
            // Can be used for visualization
        }
        
        @Override
        public void onBufferReceived(byte[] buffer) {
            // Audio buffer received
        }
        
        @Override
        public void onEndOfSpeech() {
            Log.d(TAG, "End of speech detected");
            isListening = false;
            notifyListeningStopped();
        }
        
        @Override
        public void onError(int error) {
            isListening = false;
            notifyListeningStopped();
            
            String errorMessage = getErrorMessage(error);
            Log.e(TAG, "Recognition error: " + error + " - " + errorMessage);
            notifyError(error, errorMessage);
        }
        
        @Override
        public void onResults(Bundle results) {
            isListening = false;
            notifyListeningStopped();
            
            ArrayList<String> matches = results.getStringArrayList(
                SpeechRecognizer.RESULTS_RECOGNITION);
            
            if (matches != null && !matches.isEmpty()) {
                String result = matches.get(0);
                Log.d(TAG, "Recognition result: " + result);
                notifyRecognitionResult(result, false);
            }
        }
        
        @Override
        public void onPartialResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(
                SpeechRecognizer.RESULTS_RECOGNITION);
            
            if (matches != null && !matches.isEmpty()) {
                String partialResult = matches.get(0);
                Log.d(TAG, "Partial result: " + partialResult);
                notifyPartialResult(partialResult);
            }
        }
        
        @Override
        public void onEvent(int eventType, Bundle params) {
            // Reserved for future events
        }
        
        private void onRecognitionResult(String result, boolean isPartial) {
            if (userListener != null) {
                userListener.onRecognitionResult(result, isPartial);
            }
        }
        
        private void onPartialResult(String partialResult) {
            if (userListener != null) {
                userListener.onPartialResult(partialResult);
            }
        }
        
        private void onError(int errorCode, String errorMessage) {
            if (userListener != null) {
                userListener.onError(errorCode, errorMessage);
            }
        }
        
        private void onListeningStarted() {
            if (userListener != null) {
                userListener.onListeningStarted();
            }
        }
        
        private void onListeningStopped() {
            if (userListener != null) {
                userListener.onListeningStopped();
            }
        }
    }
    
    /**
     * Gets a human-readable error message for the error code.
     * 
     * @param errorCode Error code from SpeechRecognizer
     * @return Error message string
     */
    public static String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error";
            case SpeechRecognizer.ERROR_CLIENT:
                return "Client side error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Microphone permission not granted";
            case SpeechRecognizer.ERROR_NETWORK:
                return "Network error - check your connection";
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Network timeout";
            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No speech match found";
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Recognition service busy";
            case SpeechRecognizer.ERROR_SERVER:
                return "Server error";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "Speech timeout - no speech detected";
            default:
                return "Unknown error: " + errorCode;
        }
    }
    
    /**
     * Interface for receiving recognition callbacks.
     */
    public interface OnRecognitionListener {
        void onRecognitionResult(String result, boolean isFinal);
        void onPartialResult(String partialResult);
        void onError(int errorCode, String errorMessage);
        void onListeningStarted();
        void onListeningStopped();
    }
}
