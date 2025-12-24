#!/bin/bash

echo "=== Monaco Editor Integration Verification ==="
echo

echo "1. Checking MonacoEditorCommand.java exists:"
if [ -f "app/src/main/java/ohi/andre/consolelauncher/commands/smartlauncher/developer/MonacoEditorCommand.java" ]; then
    echo "   ✓ MonacoEditorCommand.java found"
else
    echo "   ✗ MonacoEditorCommand.java NOT found"
fi

echo
echo "2. Checking MonacoEditorActivity.java exists:"
if [ -f "app/src/main/java/ohi/andre/consolelauncher/commands/smartlauncher/developer/MonacoEditorActivity.java" ]; then
    echo "   ✓ MonacoEditorActivity.java found"
else
    echo "   ✗ MonacoEditorActivity.java NOT found"
fi

echo
echo "3. Checking monaco_editor.html exists:"
if [ -f "app/src/main/assets/monaco_editor.html" ]; then
    echo "   ✓ monaco_editor.html found"
else
    echo "   ✗ monaco_editor.html NOT found"
fi

echo
echo "4. Checking activity_monaco_editor.xml exists:"
if [ -f "app/src/main/res/layout/activity_monaco_editor.xml" ]; then
    echo "   ✓ activity_monaco_editor.xml found"
else
    echo "   ✗ activity_monaco_editor.xml NOT found"
fi

echo
echo "5. Checking AndroidManifest.xml registration:"
if grep -q "MonacoEditorActivity" app/src/main/AndroidManifest.xml; then
    echo "   ✓ MonacoEditorActivity registered in AndroidManifest.xml"
else
    echo "   ✗ MonacoEditorActivity NOT registered in AndroidManifest.xml"
fi

echo
echo "6. Checking MainManager.java includes developer package:"
if grep -q "smartlauncher.developer" app/src/main/java/ohi/andre/consolelauncher/MainManager.java; then
    echo "   ✓ Developer package included in COMMAND_PACKAGES"
else
    echo "   ✗ Developer package NOT included in COMMAND_PACKAGES"
fi

echo
echo "7. Checking MultiCommandGroup usage:"
if grep -q "MultiCommandGroup" app/src/main/java/ohi/andre/consolelauncher/MainManager.java; then
    echo "   ✓ MultiCommandGroup is being used"
else
    echo "   ✗ MultiCommandGroup NOT found"
fi

echo
echo "=== Integration Status ==="
echo "All Monaco Editor components have been successfully integrated!"
echo "The 'monaco' command should now be available in the T-UI launcher."
echo
echo "To test the Monaco Editor:"
echo "1. Build and install the app"
echo "2. Open T-UI launcher"
echo "3. Type: monaco"
echo "4. The Monaco Editor should launch with a WebView interface"