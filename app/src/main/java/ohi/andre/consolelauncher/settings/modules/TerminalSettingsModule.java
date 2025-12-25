package ohi.andre.consolelauncher.settings.modules;

import android.content.Context;
import android.content.SharedPreferences;

import ohi.andre.consolelauncher.settings.BaseSettingsModule;
import ohi.andre.consolelauncher.settings.ISettingsModule;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings module for terminal emulation configuration.
 * This module manages terminal appearance and behavior settings.
 */
public class TerminalSettingsModule extends BaseSettingsModule {
    
    private static final String TAG = "TerminalSettings";
    public static final String MODULE_ID = "terminal_settings";
    
    // Preference keys
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_FONT_NAME = "font_name";
    private static final String KEY_TERMINAL_TYPE = "terminal_type";
    private static final String KEY_CURSOR_BLINK = "cursor_blink";
    private static final String KEY_SCROLL_BAR = "scroll_bar";
    private static final String KEY_BELL_ENABLED = "bell_enabled";
    private static final String KEY_BACKSPACE_SEQUENCE = "backspace_sequence";
    private static final String KEY_DELETE_SEQUENCE = "delete_sequence";
    private static final String KEY_HOME_SEQUENCE = "home_sequence";
    private static final String KEY_END_SEQUENCE = "end_sequence";
    private static final String KEY_TAB_SEQUENCE = "tab_sequence";
    private static final String KEY_ALT_SENDS_ESC = "alt_sends_esc";
    private static final String KEY_UTF8_ENABLED = "utf8_enabled";
    private static final String KEY_ROWS = "rows";
    private static final String KEY_COLUMNS = "columns";
    
    // Default values
    private static final int DEFAULT_FONT_SIZE = 12;
    private static final String DEFAULT_FONT_NAME = "monospace";
    private static final String DEFAULT_TERMINAL_TYPE = "xterm-color";
    private static final boolean DEFAULT_CURSOR_BLINK = true;
    private static final boolean DEFAULT_SCROLL_BAR = true;
    private static final boolean DEFAULT_BELL_ENABLED = true;
    private static final String DEFAULT_BACKSPACE_SEQUENCE = "\u007F";
    private static final String DEFAULT_DELETE_SEQUENCE = "\u001B[3~";
    private static final String DEFAULT_HOME_SEQUENCE = "\u001B[H";
    private static final String DEFAULT_END_SEQUENCE = "\u001B[F";
    private static final String DEFAULT_TAB_SEQUENCE = "\t";
    private static final boolean DEFAULT_ALT_SENDS_ESC = true;
    private static final boolean DEFAULT_UTF8_ENABLED = true;
    private static final int DEFAULT_ROWS = 24;
    private static final int DEFAULT_COLUMNS = 80;
    
    public TerminalSettingsModule(Context context) {
        super(context, MODULE_ID);
    }
    
    @Override
    public void loadSettings() {
        Log.d(TAG, "Terminal settings loaded");
    }
    
    @Override
    public void saveSettings() {
        clearChangedFlag();
        Log.d(TAG, "Terminal settings saved");
    }
    
    @Override
    public void resetToDefaults() {
        clearAll();
        markAsChanged();
        notifyReset();
        Log.d(TAG, "Terminal settings reset to defaults");
    }
    
    // ==================== Font Settings ====================
    
    public int getFontSize() {
        return getInt(KEY_FONT_SIZE, DEFAULT_FONT_SIZE);
    }
    
    public void setFontSize(int size) {
        int clamped = Math.max(8, Math.min(32, size));
        setInt(KEY_FONT_SIZE, clamped);
    }
    
    public String getFontName() {
        return getString(KEY_FONT_NAME, DEFAULT_FONT_NAME);
    }
    
    public void setFontName(String fontName) {
        setString(KEY_FONT_NAME, fontName != null ? fontName : DEFAULT_FONT_NAME);
    }
    
    // ==================== Terminal Type ====================
    
    public String getTerminalType() {
        return getString(KEY_TERMINAL_TYPE, DEFAULT_TERMINAL_TYPE);
    }
    
    public void setTerminalType(String type) {
        setString(KEY_TERMINAL_TYPE, type != null ? type : DEFAULT_TERMINAL_TYPE);
    }
    
    /**
     * Gets available terminal types.
     * 
     * @return Array of supported terminal types
     */
    public String[] getAvailableTerminalTypes() {
        return new String[]{
            "xterm",
            "xterm-color",
            "xterm-256color",
            "vt100",
            "vt102",
            "linux",
            "screen"
        };
    }
    
    // ==================== Cursor Settings ====================
    
    public boolean isCursorBlinkEnabled() {
        return getBoolean(KEY_CURSOR_BLINK, DEFAULT_CURSOR_BLINK);
    }
    
    public void setCursorBlinkEnabled(boolean enabled) {
        setBoolean(KEY_CURSOR_BLINK, enabled);
    }
    
    // ==================== Scroll Bar ====================
    
    public boolean isScrollBarEnabled() {
        return getBoolean(KEY_SCROLL_BAR, DEFAULT_SCROLL_BAR);
    }
    
    public void setScrollBarEnabled(boolean enabled) {
        setBoolean(KEY_SCROLL_BAR, enabled);
    }
    
    // ==================== Bell ====================
    
    public boolean isBellEnabled() {
        return getBoolean(KEY_BELL_ENABLED, DEFAULT_BELL_ENABLED);
    }
    
    public void setBellEnabled(boolean enabled) {
        setBoolean(KEY_BELL_ENABLED, enabled);
    }
    
    // ==================== Control Sequences ====================
    
    public String getBackspaceSequence() {
        return getString(KEY_BACKSPACE_SEQUENCE, DEFAULT_BACKSPACE_SEQUENCE);
    }
    
    public void setBackspaceSequence(String sequence) {
        setString(KEY_BACKSPACE_SEQUENCE, sequence != null ? sequence : DEFAULT_BACKSPACE_SEQUENCE);
    }
    
    public String getDeleteSequence() {
        return getString(KEY_DELETE_SEQUENCE, DEFAULT_DELETE_SEQUENCE);
    }
    
    public void setDeleteSequence(String sequence) {
        setString(KEY_DELETE_SEQUENCE, sequence != null ? sequence : DEFAULT_DELETE_SEQUENCE);
    }
    
    public String getHomeSequence() {
        return getString(KEY_HOME_SEQUENCE, DEFAULT_HOME_SEQUENCE);
    }
    
    public void setHomeSequence(String sequence) {
        setString(KEY_HOME_SEQUENCE, sequence != null ? sequence : DEFAULT_HOME_SEQUENCE);
    }
    
    public String getEndSequence() {
        return getString(KEY_END_SEQUENCE, DEFAULT_END_SEQUENCE);
    }
    
    public void setEndSequence(String sequence) {
        setString(KEY_END_SEQUENCE, sequence != null ? sequence : DEFAULT_END_SEQUENCE);
    }
    
    public String getTabSequence() {
        return getString(KEY_TAB_SEQUENCE, DEFAULT_TAB_SEQUENCE);
    }
    
    public void setTabSequence(String sequence) {
        setString(KEY_TAB_SEQUENCE, sequence != null ? sequence : DEFAULT_TAB_SEQUENCE);
    }
    
    // ==================== Alt Key ====================
    
    public boolean isAltSendsEscEnabled() {
        return getBoolean(KEY_ALT_SENDS_ESC, DEFAULT_ALT_SENDS_ESC);
    }
    
    public void setAltSendsEscEnabled(boolean enabled) {
        setBoolean(KEY_ALT_SENDS_ESC, enabled);
    }
    
    // ==================== Encoding ====================
    
    public boolean isUtf8Enabled() {
        return getBoolean(KEY_UTF8_ENABLED, DEFAULT_UTF8_ENABLED);
    }
    
    public void setUtf8Enabled(boolean enabled) {
        setBoolean(KEY_UTF8_ENABLED, enabled);
    }
    
    // ==================== Dimensions ====================
    
    public int getRows() {
        return getInt(KEY_ROWS, DEFAULT_ROWS);
    }
    
    public void setRows(int rows) {
        int clamped = Math.max(1, Math.min(100, rows));
        setInt(KEY_ROWS, clamped);
    }
    
    public int getColumns() {
        return getInt(KEY_COLUMNS, DEFAULT_COLUMNS);
    }
    
    public void setColumns(int columns) {
        int clamped = Math.max(1, Math.min(200, columns));
        setInt(KEY_COLUMNS, clamped);
    }
    
    // ==================== Export/Import ====================
    
    @Override
    public List<SettingEntry> exportSettings() {
        List<SettingEntry> entries = new ArrayList<>();
        
        entries.add(new SettingEntry(KEY_FONT_SIZE, getFontSize(), SettingType.INTEGER));
        entries.add(new SettingEntry(KEY_FONT_NAME, getFontName(), SettingType.STRING));
        entries.add(new SettingEntry(KEY_TERMINAL_TYPE, getTerminalType(), SettingType.STRING));
        entries.add(new SettingEntry(KEY_CURSOR_BLINK, isCursorBlinkEnabled(), SettingType.BOOLEAN));
        entries.add(new SettingEntry(KEY_SCROLL_BAR, isScrollBarEnabled(), SettingType.BOOLEAN));
        entries.add(new SettingEntry(KEY_BELL_ENABLED, isBellEnabled(), SettingType.BOOLEAN));
        entries.add(new SettingEntry(KEY_BACKSPACE_SEQUENCE, getBackspaceSequence(), SettingType.STRING));
        entries.add(new SettingEntry(KEY_DELETE_SEQUENCE, getDeleteSequence(), SettingType.STRING));
        entries.add(new SettingEntry(KEY_HOME_SEQUENCE, getHomeSequence(), SettingType.STRING));
        entries.add(new SettingEntry(KEY_END_SEQUENCE, getEndSequence(), SettingType.STRING));
        entries.add(new SettingEntry(KEY_TAB_SEQUENCE, getTabSequence(), SettingType.STRING));
        entries.add(new SettingEntry(KEY_ALT_SENDS_ESC, isAltSendsEscEnabled(), SettingType.BOOLEAN));
        entries.add(new SettingEntry(KEY_UTF8_ENABLED, isUtf8Enabled(), SettingType.BOOLEAN));
        entries.add(new SettingEntry(KEY_ROWS, getRows(), SettingType.INTEGER));
        entries.add(new SettingEntry(KEY_COLUMNS, getColumns(), SettingType.INTEGER));
        
        return entries;
    }
    
    @Override
    public boolean importSettings(List<SettingEntry> entries) {
        for (SettingEntry entry : entries) {
            switch (entry.getKey()) {
                case KEY_FONT_SIZE:
                    setFontSize(entry.getIntValue());
                    break;
                case KEY_FONT_NAME:
                    setFontName(entry.getStringValue());
                    break;
                case KEY_TERMINAL_TYPE:
                    setTerminalType(entry.getStringValue());
                    break;
                case KEY_CURSOR_BLINK:
                    setBoolean(KEY_CURSOR_BLINK, entry.getBooleanValue());
                    break;
                case KEY_SCROLL_BAR:
                    setBoolean(KEY_SCROLL_BAR, entry.getBooleanValue());
                    break;
                case KEY_BELL_ENABLED:
                    setBoolean(KEY_BELL_ENABLED, entry.getBooleanValue());
                    break;
                case KEY_BACKSPACE_SEQUENCE:
                    setBackspaceSequence(entry.getStringValue());
                    break;
                case KEY_DELETE_SEQUENCE:
                    setDeleteSequence(entry.getStringValue());
                    break;
                case KEY_HOME_SEQUENCE:
                    setHomeSequence(entry.getStringValue());
                    break;
                case KEY_END_SEQUENCE:
                    setEndSequence(entry.getStringValue());
                    break;
                case KEY_TAB_SEQUENCE:
                    setTabSequence(entry.getStringValue());
                    break;
                case KEY_ALT_SENDS_ESC:
                    setBoolean(KEY_ALT_SENDS_ESC, entry.getBooleanValue());
                    break;
                case KEY_UTF8_ENABLED:
                    setBoolean(KEY_UTF8_ENABLED, entry.getBooleanValue());
                    break;
                case KEY_ROWS:
                    setRows(entry.getIntValue());
                    break;
                case KEY_COLUMNS:
                    setColumns(entry.getIntValue());
                    break;
            }
        }
        return true;
    }
    
    @Override
    public boolean validateSettings() {
        // Validate font size
        int fontSize = getFontSize();
        if (fontSize < 8 || fontSize > 32) {
            return false;
        }
        
        // Validate dimensions
        int rows = getRows();
        int columns = getColumns();
        if (rows < 1 || rows > 100 || columns < 1 || columns > 200) {
            return false;
        }
        
        return true;
    }
}
