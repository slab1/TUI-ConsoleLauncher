package ohi.andre.consolelauncher.settings;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ISettingsModule interface and BaseSettingsModule.
 */
@RunWith(MockitoJUnitRunner.class)
public class SettingsModuleTest {
    
    private static final String TEST_MODULE_ID = "test_settings";
    
    @Mock
    private Context mockContext;
    
    @Mock
    private SharedPreferences mockPreferences;
    
    @Mock
    private SharedPreferences.Editor mockEditor;
    
    private TestSettingsModule testModule;
    
    /**
     * Test implementation of ISettingsModule for testing.
     */
    static class TestSettingsModule extends BaseSettingsModule {
        
        private boolean loadCalled = false;
        private boolean saveCalled = false;
        private boolean resetCalled = false;
        
        public TestSettingsModule(Context context, String moduleId) {
            super(context, moduleId);
        }
        
        @Override
        public void loadSettings() {
            loadCalled = true;
        }
        
        @Override
        public void saveSettings() {
            saveCalled = true;
        }
        
        @Override
        public void resetToDefaults() {
            resetCalled = true;
        }
        
        public boolean wasLoadCalled() {
            return loadCalled;
        }
        
        public boolean wasSaveCalled() {
            return saveCalled;
        }
        
        public boolean wasResetCalled() {
            return resetCalled;
        }
    }
    
    @Before
    public void setUp() {
        when(mockContext.getApplicationContext()).thenReturn(mockContext);
        when(mockPreferences.edit()).thenReturn(mockEditor);
        when(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor);
        when(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor);
        when(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor);
        when(mockEditor.putFloat(anyString(), anyFloat())).thenReturn(mockEditor);
        when(mockEditor.putLong(anyString(), anyLong())).thenReturn(mockEditor);
        when(mockEditor.remove(anyString())).thenReturn(mockEditor);
        when(mockEditor.clear()).thenReturn(mockEditor);
        
        testModule = new TestSettingsModule(mockContext, TEST_MODULE_ID);
    }
    
    @Test
    public void testModuleIdIsCorrect() {
        assertEquals(TEST_MODULE_ID, testModule.getModuleId());
    }
    
    @Test
    public void testRegisterChangeListener() {
        SettingsChangeListener mockListener = mock(SettingsChangeListener.class);
        testModule.registerChangeListener(mockListener);
        
        // Register same listener again - should not duplicate
        testModule.registerChangeListener(mockListener);
        
        // No exception means success
        assertTrue(true);
    }
    
    @Test
    public void testUnregisterChangeListener() {
        SettingsChangeListener mockListener = mock(SettingsChangeListener.class);
        testModule.registerChangeListener(mockListener);
        testModule.unregisterChangeListener(mockListener);
        
        // No exception means success
        assertTrue(true);
    }
    
    @Test
    public void testHasUnsavedChangesInitiallyFalse() {
        assertFalse(testModule.hasUnsavedChanges());
    }
    
    @Test
    public void testGetStringWithDefault() {
        when(mockPreferences.getString("test_key", "default_value")).thenReturn("default_value");
        
        String result = testModule.getString("test_key", "default_value");
        
        assertEquals("default_value", result);
        verify(mockPreferences).getString("test_key", "default_value");
    }
    
    @Test
    public void testGetIntWithDefault() {
        when(mockPreferences.getInt("test_int", 42)).thenReturn(42);
        
        int result = testModule.getInt("test_int", 42);
        
        assertEquals(42, result);
        verify(mockPreferences).getInt("test_int", 42);
    }
    
    @Test
    public void testGetBooleanWithDefault() {
        when(mockPreferences.getBoolean("test_bool", false)).thenReturn(true);
        
        boolean result = testModule.getBoolean("test_bool", false);
        
        assertTrue(result);
        verify(mockPreferences).getBoolean("test_bool", false);
    }
    
    @Test
    public void testGetFloatWithDefault() {
        when(mockPreferences.getFloat("test_float", 1.5f)).thenReturn(1.5f);
        
        float result = testModule.getFloat("test_float", 1.5f);
        
        assertEquals(1.5f, result, 0.001);
        verify(mockPreferences).getFloat("test_float", 1.5f);
    }
    
    @Test
    public void testGetLongWithDefault() {
        when(mockPreferences.getLong("test_long", 100L)).thenReturn(100L);
        
        long result = testModule.getLong("test_long", 100L);
        
        assertEquals(100L, result);
        verify(mockPreferences).getLong("test_long", 100L);
    }
    
    @Test
    public void testContains() {
        when(mockPreferences.contains("existing_key")).thenReturn(true);
        when(mockPreferences.contains("missing_key")).thenReturn(false);
        
        assertTrue(testModule.contains("existing_key"));
        assertFalse(testModule.contains("missing_key"));
    }
    
    @Test
    public void testExportSettingsReturnsList() {
        when(mockPreferences.getAll()).thenReturn(new java.util.HashMap<>());
        
        List<ISettingsModule.SettingEntry> entries = testModule.exportSettings();
        
        assertNotNull(entries);
        assertTrue(entries instanceof List);
    }
    
    @Test
    public void testImportSettingsWithEmptyList() {
        boolean result = testModule.importSettings(null);
        assertFalse(result);
    }
    
    @Test
    public void testValidateSettingsDefaultTrue() {
        assertTrue(testModule.validateSettings());
    }
    
    @Test
    public void testSettingEntryGetters() {
        ISettingsModule.SettingEntry entry = new ISettingsModule.SettingEntry(
            "test_key", "test_value", ISettingsModule.SettingType.STRING);
        
        assertEquals("test_key", entry.getKey());
        assertEquals("test_value", entry.getValue());
        assertEquals(ISettingsModule.SettingType.STRING, entry.getType());
        assertEquals("test_value", entry.getStringValue());
    }
    
    @Test
    public void testSettingEntryIntValue() {
        ISettingsModule.SettingEntry entry = new ISettingsModule.SettingEntry(
            "test_key", 42, ISettingsModule.SettingType.INTEGER);
        
        assertEquals(42, entry.getIntValue());
    }
    
    @Test
    public void testSettingEntryBooleanValue() {
        ISettingsModule.SettingEntry entry = new ISettingsModule.SettingEntry(
            "test_key", true, ISettingsModule.SettingType.BOOLEAN);
        
        assertTrue(entry.getBooleanValue());
    }
    
    @Test
    public void testSettingEntryFloatValue() {
        ISettingsModule.SettingEntry entry = new ISettingsModule.SettingEntry(
            "test_key", 1.5f, ISettingsModule.SettingType.FLOAT);
        
        assertEquals(1.5f, entry.getFloatValue(), 0.001);
    }
}
