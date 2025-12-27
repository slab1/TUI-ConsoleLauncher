# 🤖 AI Setup & Configuration Guide

## Overview
This guide will help you configure the MiniMax AI integration in your T-UI Smart IDE Launcher.

---

## 📋 Prerequisites

### 1. **Get MiniMax API Credentials**
1. Visit [MiniMax API Portal](https://api.minimax.chat)
2. Sign up for a free account
3. Navigate to API Keys section
4. Create a new API key
5. Note down your **API Key** and **Group ID**

### 2. **Verify Internet Access**
Ensure your device has internet connectivity for AI features to work.

---

## 🔧 Configuration Steps

### **Method 1: Via T-UI Commands (Recommended)**

1. **Open T-UI** on your device
2. **Configure AI** with your credentials:
   ```
   ai config YOUR_API_KEY YOUR_GROUP_ID
   ```
   Example:
   ```
   ai config sk-abc123def456ghi789jkl012mno345pqr678stu901vwx234yz def123ghi456jkl789mno012pqr345stu678vwx901yz234
   ```

3. **Test the connection**:
   ```
   ai test
   ```
   Expected output:
   ```
   ✅ Connection successful!
   Response: Hello! I'm your AI assistant...
   ```

4. **Check status**:
   ```
   ai status
   ```
   Should show:
   ```
   🤖 AI Service Status
   Status: ✅ Enabled
   Model: abab6.5-chat
   ```

### **Method 2: Manual Configuration (Advanced)**

If you prefer manual setup:

1. **Locate T-UI's data directory**:
   - Android: `/data/data/ohi.andre.consolelauncher/shared_prefs/`
   - Or use T-UI: `file ls /data/data/ohi.andre.consolelauncher/shared_prefs/`

2. **Create configuration file**:
   ```
   file create minimax_config.xml
   ```

3. **Add configuration**:
   ```xml
   <?xml version='1.0' encoding='utf-8' standalone='yes' ?>
   <map>
       <string name="api_key">YOUR_API_KEY</string>
       <string name="group_id">YOUR_GROUP_ID</string>
       <string name="model">abab6.5-chat</string>
       <boolean name="enabled" value="true" />
   </map>
   ```

---

## 🧪 Testing AI Features

### **Basic AI Chat**
```bash
# Direct chat
ai Hello, can you help me with Android development?

# Get coding help
ai How do I create a Git commit with multiple files?

# Ask about features
ai What network commands are available?
```

### **AI with Context**
```bash
# AI remembers context within conversation
ai I'm working on a React project
ai Can you help me set up Git?
ai What should my first commit message be?
```

### **Troubleshooting AI Issues**

**Connection Failed:**
- Check internet connection
- Verify API key and Group ID
- Try: `ai test` for detailed error

**No Response:**
- Try simpler queries first
- Check: `ai status`
- Restart T-UI app

**Rate Limiting:**
- Wait a few seconds between requests
- AI has usage limits (check your plan)

---

## ⚙️ Advanced Configuration

### **Available Models**
```bash
# List all models
ai models

# Available models:
• abab6.5-chat (default) - Best overall performance
• abab6.5s-chat (fast) - Faster responses
• abab5.5-chat (legacy) - Backward compatibility
```

### **Custom Model Usage**
```bash
# Use specific model
ai config YOUR_API_KEY YOUR_GROUP_ID abab6.5s-chat
```

### **Clear Configuration**
```bash
# Reset AI configuration
ai clear
# Then reconfigure with new credentials
```

---

## 🚀 AI-Powered Workflow Examples

### **Development Assistance**
```bash
# Get help with commands
ai What does 'git status' show?

# Learn new tools
ai How do I use network ping command?

# Code debugging
ai I'm getting a merge conflict, how do I resolve it?
```

### **Productivity Enhancement**
```bash
# Quick calculations with AI explanation
ai Calculate 15% of 240 and explain the math

# Network troubleshooting
ai My internet is slow, what should I check first?

# Note organization
ai Help me organize my project notes with tags
```

### **Automation Help**
```bash
# Learn automation
ai How do I use Tasker with T-UI?

# System management
ai What system information should I monitor?
```

---

## 📊 Monitoring & Management

### **Usage Statistics**
```bash
# Check AI service status
ai status

# Shows:
# • API Key status (masked)
# • Group ID (masked) 
# • Model in use
# • Connection status
```

### **Performance Tips**
1. **Keep queries concise** for faster responses
2. **Use specific commands** for technical questions
3. **Ask follow-up questions** to get more details
4. **Use context** to build on previous conversations

---

## 🔒 Security & Privacy

### **API Key Protection**
- ✅ Keys are stored securely in Android SharedPreferences
- ✅ Keys are masked in status displays
- ✅ No keys are logged or transmitted unnecessarily

### **Data Privacy**
- ✅ Conversations are sent to MiniMax servers
- ✅ No conversation history stored locally
- ✅ Check MiniMax privacy policy for details

### **Best Practices**
- 🔒 Never share your API key publicly
- 🔒 Use environment variables for automation
- 🔒 Regularly rotate API keys
- 🔒 Monitor usage in MiniMax dashboard

---

## ❗ Troubleshooting Guide

### **Common Issues**

**"AI service not configured"**
```
Solution: Run ai config with valid credentials
```

**"API request failed: 401"**
```
Solution: Check your API key and Group ID
```

**"Network error"**
```
Solution: Check internet connection
```

**"Connection timeout"**
```
Solution: Check network speed, try again
```

**"Invalid response format"**
```
Solution: Try ai test to diagnose
```

### **Debug Commands**
```bash
# Test basic connectivity
ai test

# Check configuration
ai status

# Clear and reconfigure
ai clear
ai config NEW_KEY NEW_GROUP
```

---

## 🎯 Success Checklist

- [ ] API key and Group ID obtained from MiniMax
- [ ] Internet connection verified
- [ ] AI configured: `ai config <key> <group>`
- [ ] Connection tested: `ai test`
- [ ] Status checked: `ai status`
- [ ] First AI conversation successful
- [ ] Basic commands working (calc, git, etc.)

---

## 🚀 Next Steps

Once AI is configured:

1. **Explore AI Commands**: Try various AI interactions
2. **Test Integration**: Use AI with other Smart Launcher features
3. **Build Workflows**: Combine AI with git, network, notes commands
4. **Custom Prompts**: Develop your own AI-assisted workflows

---

**🎉 Congratulations!** Your T-UI Smart IDE Launcher now has AI-powered assistance!

**Need Help?** Check the main README.md or run `ai help` for command reference.