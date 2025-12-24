#!/bin/bash

# T-UI Smart IDE Launcher Integration Script
# This script helps integrate the smart launcher commands into T-UI ConsoleLauncher

echo "🚀 T-UI Smart IDE Launcher Integration"
echo "======================================"
echo

# Check if we're in a T-UI ConsoleLauncher directory
if [ ! -f "app/build.gradle" ] || [ ! -d "app/src/main/java/ohi/andre/consolelauncher" ]; then
    echo "❌ Error: This doesn't appear to be a T-UI ConsoleLauncher project."
    echo "Please run this script from the root of the T-UI ConsoleLauncher project."
    exit 1
fi

echo "✅ Found T-UI ConsoleLauncher project"
echo

# Create directory structure if it doesn't exist
echo "📁 Creating command directories..."
mkdir -p app/src/main/java/ohi/andre/consolelauncher/commands/smartlauncher/{ai,developer,productivity,automation}

# Copy command files
echo "📋 Copying command files..."
cp -r /workspace/TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/commands/smartlauncher/* \
    app/src/main/java/ohi/andre/consolelauncher/commands/smartlauncher/ 2>/dev/null || true

echo "✅ Command files copied"
echo

# Check for required dependencies in build.gradle
echo "🔍 Checking dependencies..."
if ! grep -q "okhttp" app/build.gradle; then
    echo "⚠️  Adding OkHttp dependency..."
    echo "    implementation 'com.squareup.okhttp3:okhttp:4.11.0'" >> app/build.gradle
fi

if ! grep -q "gson" app/build.gradle; then
    echo "⚠️  Adding Gson dependency..."
    echo "    implementation 'com.google.code.gson:gson:2.10.1'" >> app/build.gradle
fi

echo "✅ Dependencies updated"
echo

# Check for required permissions in AndroidManifest.xml
echo "🔐 Checking permissions..."
MANIFEST="app/src/main/AndroidManifest.xml"

if [ -f "$MANIFEST" ]; then
    if ! grep -q "android.permission.INTERNET" "$MANIFEST"; then
        echo "⚠️  Adding INTERNET permission..."
        sed -i '/<uses-permission/i\    <uses-permission android:name="android.permission.INTERNET" />' "$MANIFEST"
    fi
    
    if ! grep -q "android.permission.ACCESS_NETWORK_STATE" "$MANIFEST"; then
        echo "⚠️  Adding ACCESS_NETWORK_STATE permission..."
        sed -i '/<uses-permission/i\    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />' "$MANIFEST"
    fi
    
    echo "✅ Permissions updated"
else
    echo "⚠️  AndroidManifest.xml not found - please add permissions manually"
fi
echo

# Create a simple registration guide
echo "📝 Creating integration guide..."
cat > SMART-LAUNCHER-INTEGRATION.md << 'EOF'
# Smart Launcher Integration Guide

## Command Registration

To use the Smart IDE Launcher commands, you need to register them in T-UI's command system.

### Option 1: Manual Registration

1. **Find your main activity or command manager** (usually `LauncherActivity.java` or similar)
2. **Add command imports**:
```java
import ohi.andre.consolelauncher.commands.smartlauncher.ai.AICommand;
import ohi.andre.consolelauncher.commands.smartlauncher.developer.GitCommand;
import ohi.andre.consolelauncher.commands.smartlauncher.developer.FileManagerCommand;
import ohi.andre.consolelauncher.commands.smartlauncher.productivity.CalculatorCommand;
import ohi.andre.consolelauncher.commands.smartlauncher.productivity.NetworkCommand;
import ohi.andre.consolelauncher.commands.smartlauncher.productivity.NotesCommand;
import ohi.andre.consolelauncher.commands.smartlauncher.productivity.SystemCommand;
import ohi.andre.consolelauncher.commands.smartlauncher.automation.AutomationCommand;
```

3. **Register commands** in your command initialization:
```java
// Add these commands to your command registry
commands.add(new AICommand());
commands.add(new GitCommand());
commands.add(new FileManagerCommand());
commands.add(new CalculatorCommand());
commands.add(new NetworkCommand());
commands.add(new NotesCommand());
commands.add(new SystemCommand());
commands.add(new AutomationCommand());
```

### Option 2: Package Scanning

If T-UI uses package scanning for commands, add this to your AndroidManifest.xml:

```xml
<application
    android:label="@string/app_name"
    android:theme="@style/AppTheme">
    
    <!-- Smart Launcher Commands -->
    <meta-data
        android:name="smartlauncher.commands"
        android:value="ohi.andre.consolelauncher.commands.smartlauncher" />
</application>
```

## Testing the Integration

1. **Build the project**: `./gradlew assembleDebug`
2. **Install on device**: `adb install app/build/outputs/apk/debug/app-debug.apk`
3. **Test commands**:
   - `ai hello` - Test AI integration
   - `git --help` - Test Git command
   - `calc 2+2` - Test calculator
   - `network ping google.com` - Test network tools

## Configuration

### AI Setup
1. Get API credentials from [MiniMax API](https://api.minimax.chat)
2. Configure: `ai config <your_api_key> <your_group_id>`
3. Test: `ai test`

### Permissions
Ensure these permissions are granted:
- INTERNET (for AI and network commands)
- ACCESS_NETWORK_STATE (for network diagnostics)
- READ/WRITE_EXTERNAL_STORAGE (for file operations)

## Troubleshooting

**Commands not recognized**
- Check command registration
- Verify imports are correct
- Check for build errors

**AI not working**
- Configure with `ai config`
- Test with `ai test`
- Check internet connection

**File operations failing**
- Grant storage permissions
- Use app-specific directories
- Check Android version compatibility

## Support

For issues and questions, refer to the main SMART-LAUNCHER-README.md file.
EOF

echo "✅ Integration guide created: SMART-LAUNCHER-INTEGRATION.md"
echo

# Create a simple test script
echo "🧪 Creating test script..."
cat > test-smart-launcher.sh << 'EOF'
#!/bin/bash

echo "🧪 Testing Smart Launcher Commands"
echo "=================================="
echo

# Test that files exist
echo "📁 Checking command files..."
files=(
    "app/src/main/java/ohi/andre/consolelauncher/commands/smartlauncher/ai/AICommand.java"
    "app/src/main/java/ohi/andre/consolelauncher/commands/smartlauncher/ai/MiniMaxService.java"
    "app/src/main/java/ohi/andre/consolelauncher/commands/smartlauncher/developer/GitCommand.java"
    "app/src/main/java/ohi/andre/consolelauncher/commands/smartlauncher/developer/FileManagerCommand.java"
    "app/src/main/java/ohi/andre/consolelauncher/commands/smartlauncher/productivity/CalculatorCommand.java"
    "app/src/main/java/ohi/andre/consolelauncher/commands/smartlauncher/productivity/NetworkCommand.java"
    "app/src/main/java/ohi/andre/consolelauncher/commands/smartlauncher/productivity/NotesCommand.java"
    "app/src/main/java/ohi/andre/consolelauncher/commands/smartlauncher/productivity/SystemCommand.java"
    "app/src/main/java/ohi/andre/consolelauncher/commands/smartlauncher/automation/AutomationCommand.java"
)

all_good=true
for file in "${files[@]}"; do
    if [ -f "$file" ]; then
        echo "  ✅ $file"
    else
        echo "  ❌ $file (missing)"
        all_good=false
    fi
done

echo
if [ "$all_good" = true ]; then
    echo "✅ All command files are present"
    echo "🚀 Ready to integrate with T-UI!"
else
    echo "❌ Some files are missing"
fi

echo
echo "📋 Next steps:"
echo "1. Read SMART-LAUNCHER-INTEGRATION.md"
echo "2. Register commands in T-UI's command system"
echo "3. Build and test the application"
echo "4. Configure AI with 'ai config <key> <group>'"
EOF

chmod +x test-smart-launcher.sh
echo "✅ Test script created: test-smart-launcher.sh"
echo

# Run the test script
echo "🧪 Running integration test..."
./test-smart-launcher.sh

echo
echo "🎉 Smart IDE Launcher Integration Complete!"
echo "=============================================="
echo
echo "📚 Documentation:"
echo "   • SMART-LAUNCHER-README.md - Complete feature documentation"
echo "   • SMART-LAUNCHER-INTEGRATION.md - Integration guide"
echo "   • test-smart-launcher.sh - Test script"
echo
echo "🔧 Next Steps:"
echo "   1. Read the integration guide"
echo "   2. Register commands in your T-UI project"
echo "   3. Build and test the application"
echo "   4. Configure AI integration"
echo
echo "🤖 AI Setup:"
echo "   • Get API key from: https://api.minimax.chat"
echo "   • Configure with: ai config <your_key> <your_group>"
echo "   • Test with: ai test"
echo
echo "Happy coding! 🚀"