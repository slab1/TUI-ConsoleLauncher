package ohi.andre.consolelauncher.ui.views;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.ai.SpeechRecognizerHelper;
import ohi.andre.consolelauncher.settings.modules.VoiceSettingsModule;

/**
 * Voice input button view for T-UI launcher.
 * This view provides a microphone button for voice command input
 * with visual feedback for listening state.
 */
public class VoiceInputView extends LinearLayout {
    
    private ImageButton microphoneButton;
    private VoiceSettingsModule voiceSettings;
    private SpeechRecognizerHelper speechRecognizer;
    private VoiceInputListener listener;
    
    private boolean isListening = false;
    private int listeningColor;
    private int idleColor;
    private int errorColor;
    
    public VoiceInputView(Context context) {
        super(context);
        init(context, null);
    }
    
    public VoiceInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }
    
    public VoiceInputView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }
    
    private void init(Context context, AttributeSet attrs) {
        setOrientation(VERTICAL);
        
        // Inflate layout
        LayoutInflater.from(context).inflate(R.layout.view_voice_input, this, true);
        
        // Get references
        microphoneButton = findViewById(R.id.microphone_button);
        
        // Load attributes
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.VoiceInputView);
        try {
            idleColor = typedArray.getColor(
                R.styleable.VoiceInputView_viv_idleColor,
                context.getColor(R.color.microphone_idle));
            listeningColor = typedArray.getColor(
                R.styleable.VoiceInputView_viv_listeningColor,
                context.getColor(R.color.microphone_listening));
            errorColor = typedArray.getColor(
                R.styleable.VoiceInputView_viv_errorColor,
                context.getColor(R.color.microphone_error));
        } finally {
            typedArray.recycle();
        }
        
        // Initialize settings
        voiceSettings = new VoiceSettingsModule(context);
        
        // Setup button
        setupButton();
    }
    
    private void setupButton() {
        microphoneButton.setOnClickListener(v -> toggleListening());
        
        // Set initial color
        updateButtonColor(idleColor);
    }
    
    /**
     * Sets the voice input listener.
     * 
     * @param listener The listener to receive voice events
     */
    public void setVoiceInputListener(VoiceInputListener listener) {
        this.listener = listener;
    }
    
    /**
     * Sets the speech recognizer helper.
     * 
     * @param recognizer The SpeechRecognizerHelper instance
     */
    public void setSpeechRecognizer(SpeechRecognizerHelper recognizer) {
        this.speechRecognizer = recognizer;
    }
    
    /**
     * Toggles the listening state.
     */
    private void toggleListening() {
        if (isListening) {
            stopListening();
        } else {
            startListening();
        }
    }
    
    /**
     * Starts listening for voice input.
     */
    private void startListening() {
        if (speechRecognizer == null) {
            showError("Speech recognizer not initialized");
            return;
        }
        
        if (!voiceSettings.isRecognitionEnabled()) {
            showError("Voice recognition is disabled");
            return;
        }
        
        // Initialize recognizer if needed
        if (!speechRecognizer.isAvailable()) {
            showError("Speech recognition not available");
            return;
        }
        
        // Start listening
        boolean started = speechRecognizer.startListening();
        
        if (started) {
            isListening = true;
            updateButtonColor(listeningColor);
            
            if (listener != null) {
                listener.onListeningStarted();
            }
        } else {
            showError("Failed to start listening");
        }
    }
    
    /**
     * Stops listening for voice input.
     */
    private void stopListening() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
        
        isListening = false;
        updateButtonColor(idleColor);
        
        if (listener != null) {
            listener.onListeningStopped();
        }
    }
    
    /**
     * Updates the button color based on state.
     * 
     * @param color The color to apply
     */
    private void updateButtonColor(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            microphoneButton.setBackgroundTintList(ColorStateList.valueOf(color));
        } else {
            // For older versions, use a drawable
            GradientDrawable background = new GradientDrawable();
            background.setShape(GradientDrawable.OVAL);
            background.setColor(color);
            microphoneButton.setBackground(background);
        }
    }
    
    /**
     * Shows an error message.
     * 
     * @param message The error message
     */
    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        
        // Flash error color
        updateButtonColor(errorColor);
        postDelayed(() -> updateButtonColor(idleColor), 500);
        
        if (listener != null) {
            listener.onError(message);
        }
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
     * Handles a successful recognition result.
     * 
     * @param result The recognized text
     */
    public void onRecognitionSuccess(String result) {
        isListening = false;
        updateButtonColor(idleColor);
        
        if (listener != null) {
            listener.onRecognitionResult(result);
        }
    }
    
    /**
     * Handles a recognition error.
     * 
     * @param errorMessage The error message
     */
    public void onRecognitionError(String errorMessage) {
        isListening = false;
        updateButtonColor(idleColor);
        showError(errorMessage);
    }
    
    /**
     * Handles partial recognition results.
     * 
     * @param partialResult The partial recognized text
     */
    public void onPartialResult(String partialResult) {
        if (listener != null) {
            listener.onPartialResult(partialResult);
        }
    }
    
    /**
     * Interface for voice input events.
     */
    public interface VoiceInputListener {
        void onListeningStarted();
        void onListeningStopped();
        void onRecognitionResult(String result);
        void onPartialResult(String partialResult);
        void onError(String errorMessage);
    }
}
