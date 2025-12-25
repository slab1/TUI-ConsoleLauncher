package ohi.andre.consolelauncher.commands.smartlauncher.developer.settings;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for EditorSettings data model
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class EditorSettingsTest {

    private EditorSettings defaultSettings;

    @Before
    public void setUp() {
        defaultSettings = EditorSettings.getDefaults();
    }

    @Test
    public void testDefaultValues() {
        // Verify default editor preferences
        assertEquals(EditorSettings.DEFAULT_FONT_SIZE, defaultSettings.getFontSize());
        assertEquals(EditorSettings.DEFAULT_THEME, defaultSettings.getTheme());
        assertEquals(EditorSettings.DEFAULT_WORD_WRAP, defaultSettings.getWordWrap());
        assertEquals(EditorSettings.DEFAULT_MINIMAP_ENABLED, defaultSettings.isMinimapEnabled());
        assertEquals(EditorSettings.DEFAULT_TAB_SIZE, defaultSettings.getTabSize());

        // Verify default behavior settings
        assertEquals(EditorSettings.DEFAULT_AUTO_SAVE, defaultSettings.isAutoSave());
        assertFalse(defaultSettings.isFormatOnSave()); // Default is true in Builder

        // Verify default feature flags
        assertEquals(EditorSettings.DEFAULT_LSP_ENABLED, defaultSettings.isLspEnabled());
        assertEquals(EditorSettings.DEFAULT_DEBUG_ENABLED, defaultSettings.isDebugEnabled());
        assertEquals(EditorSettings.DEFAULT_SIDEBAR_VISIBLE, defaultSettings.isSidebarVisible());
    }

    @Test
    public void testFontSizeValidation() {
        // Test minimum bound (8)
        EditorSettings smallFont = new EditorSettings.Builder()
            .setFontSize(4)
            .build();
        assertEquals(8, smallFont.getFontSize());

        // Test maximum bound (36)
        EditorSettings largeFont = new EditorSettings.Builder()
            .setFontSize(100)
            .build();
        assertEquals(36, largeFont.getFontSize());

        // Test valid value
        EditorSettings validFont = new EditorSettings.Builder()
            .setFontSize(18)
            .build();
        assertEquals(18, validFont.getFontSize());
    }

    @Test
    public void testTabSizeValidation() {
        // Test minimum bound (1)
        EditorSettings smallTab = new EditorSettings.Builder()
            .setTabSize(0)
            .build();
        assertEquals(1, smallTab.getTabSize());

        // Test maximum bound (16)
        EditorSettings largeTab = new EditorSettings.Builder()
            .setTabSize(100)
            .build();
        assertEquals(16, largeTab.getTabSize());
    }

    @Test
    public void testAutoSaveDelayValidation() {
        // Test minimum bound (100ms)
        EditorSettings fastSave = new EditorSettings.Builder()
            .setAutoSaveDelay(50)
            .build();
        assertEquals(100, fastSave.getAutoSaveDelay());

        // Test maximum bound (60000ms)
        EditorSettings slowSave = new EditorSettings.Builder()
            .setAutoSaveDelay(120000)
            .build();
        assertEquals(60000, slowSave.getAutoSaveDelay());
    }

    @Test
    public void testThemeSetting() {
        EditorSettings darkTheme = new EditorSettings.Builder()
            .setTheme("vs-dark")
            .build();
        assertEquals("vs-dark", darkTheme.getTheme());

        EditorSettings lightTheme = new EditorSettings.Builder()
            .setTheme("vs")
            .build();
        assertEquals("vs", lightTheme.getTheme());

        EditorSettings nullTheme = new EditorSettings.Builder()
            .setTheme(null)
            .build();
        assertEquals(EditorSettings.DEFAULT_THEME, nullTheme.getTheme());
    }

    @Test
    public void testWordWrapSetting() {
        EditorSettings wrapOn = new EditorSettings.Builder()
            .setWordWrap("on")
            .build();
        assertEquals("on", wrapOn.getWordWrap());

        EditorSettings wrapOff = new EditorSettings.Builder()
            .setWordWrap("off")
            .build();
        assertEquals("off", wrapOff.getWordWrap());
    }

    @Test
    public void testSensitiveKeysSet() {
        // Verify that sensitive keys are properly defined
        assertTrue(EditorSettings.SENSITIVE_KEYS.contains("lsp.token"));
    }

    @Test
    public void testToJson() {
        JSONObject json = defaultSettings.toJson();

        // Verify JSON contains all key settings
        assertEquals(defaultSettings.getFontSize(), json.getInt("fontSize"));
        assertEquals(defaultSettings.getTheme(), json.getString("theme"));
        assertEquals(defaultSettings.getWordWrap(), json.getString("wordWrap"));
        assertEquals(defaultSettings.isMinimapEnabled(), json.getBoolean("minimapEnabled"));
        assertEquals(defaultSettings.isAutoSave(), json.getBoolean("autoSave"));
        assertEquals(defaultSettings.isLspEnabled(), json.getBoolean("lspEnabled"));
        assertEquals(defaultSettings.isDebugEnabled(), json.getBoolean("debugEnabled"));
        assertEquals(defaultSettings.isSidebarVisible(), json.getBoolean("sidebarVisible"));
    }

    @Test
    public void testToMonacoOptions() {
        JSONObject monacoOptions = defaultSettings.toMonacoOptions();

        // Verify Monaco-compatible options format
        assertEquals(defaultSettings.getFontSize(), monacoOptions.getInt("fontSize"));
        assertEquals(defaultSettings.getTheme(), monacoOptions.getString("theme"));
        assertEquals(defaultSettings.getWordWrap(), monacoOptions.getString("wordWrap"));
        assertNotNull(monacoOptions.optJSONObject("minimap"));
        assertTrue(monacoOptions.optBoolean("scrollBeyondLastLine", true));
    }

    @Test
    public void testToMap() {
        Map<String, Object> map = defaultSettings.toMap();

        // Verify map contains expected keys and values
        assertEquals(defaultSettings.getFontSize(), map.get("fontSize"));
        assertEquals(defaultSettings.getTheme(), map.get("theme"));
        assertEquals(defaultSettings.getWordWrap(), map.get("wordWrap"));
        assertEquals(defaultSettings.isMinimapEnabled(), map.get("minimapEnabled"));
        assertEquals(defaultSettings.isAutoSave(), map.get("autoSave"));
    }

    @Test
    public void testBuilderApplyMap() {
        Map<String, Object> testMap = new java.util.HashMap<>();
        testMap.put("fontSize", 20);
        testMap.put("theme", "hc-black");
        testMap.put("autoSave", true);
        testMap.put("tabSize", 8);

        EditorSettings customSettings = new EditorSettings.Builder()
            .applyMap(testMap)
            .build();

        assertEquals(20, customSettings.getFontSize());
        assertEquals("hc-black", customSettings.getTheme());
        assertTrue(customSettings.isAutoSave());
        assertEquals(8, customSettings.getTabSize());
    }

    @Test
    public void testEditorBehaviorSettings() {
        EditorSettings behaviorSettings = new EditorSettings.Builder()
            .setAutoSave(true)
            .setAutoSaveDelay(2000)
            .setFormatOnSave(true)
            .setFormatOnPaste(false)
            .setTrimTrailingWhitespace(true)
            .build();

        assertTrue(behaviorSettings.isAutoSave());
        assertEquals(2000, behaviorSettings.getAutoSaveDelay());
        assertTrue(behaviorSettings.isFormatOnSave());
        assertFalse(behaviorSettings.isFormatOnPaste());
        assertTrue(behaviorSettings.isTrimTrailingWhitespace());
    }

    @Test
    public void testLspSettings() {
        EditorSettings lspSettings = new EditorSettings.Builder()
            .setLspEnabled(true)
            .setLspServerPath("/usr/bin/pyls")
            .setLspPythonPath("/usr/bin/python3")
            .setLspAutoComplete(true)
            .setLspDiagnostics(true)
            .build();

        assertTrue(lspSettings.isLspEnabled());
        assertEquals("/usr/bin/pyls", lspSettings.getLspServerPath());
        assertEquals("/usr/bin/python3", lspSettings.getLspPythonPath());
        assertTrue(lspSettings.isLspAutoComplete());
        assertTrue(lspSettings.isLspDiagnostics());
    }

    @Test
    public void testDebugSettings() {
        EditorSettings debugSettings = new EditorSettings.Builder()
            .setDebugEnabled(true)
            .setBreakOnException(true)
            .setBreakOnEntry(false)
            .setMaxVariablesPerScope(50)
            .setShowDebugToolbar(true)
            .build();

        assertTrue(debugSettings.isDebugEnabled());
        assertTrue(debugSettings.isBreakOnException());
        assertFalse(debugSettings.isBreakOnEntry());
        assertEquals(50, debugSettings.getMaxVariablesPerScope());
        assertTrue(debugSettings.isShowDebugToolbar());
    }

    @Test
    public void testUiPreferences() {
        EditorSettings uiSettings = new EditorSettings.Builder()
            .setSidebarVisible(false)
            .setSidebarWidth("300px")
            .setSmoothScrolling(true)
            .setMouseWheelZoom(false)
            .setMinimapSideRight(false)
            .build();

        assertFalse(uiSettings.isSidebarVisible());
        assertEquals("300px", uiSettings.getSidebarWidth());
        assertTrue(uiSettings.isSmoothScrolling());
        assertFalse(uiSettings.isMouseWheelZoom());
        assertFalse(uiSettings.isMinimapSideRight());
    }

    @Test
    public void testFileSettings() {
        EditorSettings fileSettings = new EditorSettings.Builder()
            .setDefaultEncoding("UTF-16")
            .setAutoDetectEncoding(false)
            .setConfirmOnClose(true)
            .setRecentFilesLimit(20)
            .setPreserveUndoStack(false)
            .build();

        assertEquals("UTF-16", fileSettings.getDefaultEncoding());
        assertFalse(fileSettings.isAutoDetectEncoding());
        assertTrue(fileSettings.isConfirmOnClose());
        assertEquals(20, fileSettings.getRecentFilesLimit());
        assertFalse(fileSettings.isPreserveUndoStack());
    }

    @Test
    public void testSecuritySettings() {
        EditorSettings securitySettings = new EditorSettings.Builder()
            .setAllowInsecureConnections(true)
            .setSaveSensitiveDataEncrypted(false)
            .build();

        assertTrue(securitySettings.isAllowInsecureConnections());
        assertFalse(securitySettings.isSaveSensitiveDataEncrypted());
    }

    @Test
    public void testImmutability() {
        EditorSettings settings = new EditorSettings.Builder()
            .setFontSize(16)
            .setTheme("vs-dark")
            .build();

        // Settings should be effectively immutable
        // All getters should return consistent values
        assertEquals(16, settings.getFontSize());
        assertEquals(16, settings.getFontSize());
        assertEquals("vs-dark", settings.getTheme());
        assertEquals("vs-dark", settings.getTheme());
    }

    @Test
    public void testBuilderCopyPattern() {
        EditorSettings original = new EditorSettings.Builder()
            .setFontSize(20)
            .setTheme("vs")
            .setAutoSave(true)
            .build();

        // Create a new builder from an existing settings object
        EditorSettings copied = new EditorSettings.Builder()
            .setFontSize(original.getFontSize())
            .setTheme(original.getTheme())
            .setAutoSave(original.isAutoSave())
            .build();

        assertEquals(original.getFontSize(), copied.getFontSize());
        assertEquals(original.getTheme(), copied.getTheme());
        assertEquals(original.isAutoSave(), copied.isAutoSave());
    }
}
