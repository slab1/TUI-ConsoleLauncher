# 🚀 T-UI Smart IDE Launcher - Quick Start Guide

## Welcome to Your Enhanced T-UI! 🎉

Your T-UI ConsoleLauncher is now a **Smart IDE Launcher** with AI assistance, developer tools, and automation capabilities.

---

## ⚡ Quick Start (5 Minutes)

### 1. **Build & Install**
```bash
# In your T-UI ConsoleLauncher directory:
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. **Test Basic Commands**
Open T-UI and try these:
```bash
calc 2+2                    # Calculator
network ping google.com     # Network test
file ls /sdcard/           # File browser
note create "Hello" "World" # Note taking
```

### 3. **Configure AI (Optional but Recommended)**
1. Get API key from [MiniMax API](https://api.minimax.chat)
2. Configure in T-UI:
   ```
   ai config YOUR_API_KEY YOUR_GROUP_ID
   ai test
   ```

---

## 🧪 Command Categories

### 🤖 **AI Assistant** - Get intelligent help
```bash
ai "How do I fix a Git merge conflict?"
ai "What's the fastest way to ping a server?"
ai "Help me organize my project notes"
```

### 💻 **Developer Tools** - Professional development workflow
```bash
git clone https://github.com/user/repo.git
git status
git commit -m "Add new feature"
file cat README.md
file find "*.java"
```

### 🛠️ **Productivity** - Daily task automation
```bash
calc "15% of 240"
network scan localhost 80,443,8080
note create "Meeting Notes" "Discussed Q4 goals #meeting"
sys info
```

### 🔧 **Automation** - System integration
```bash
auto tasker              # List Tasker tasks
auto termux "ls -la"     # Run Termux command
auto apps                # Check automation apps
```

---

## 🎯 Common Workflows

### **Development Session**
```bash
# Start development
git clone <project-url>
file cd <project-folder>
ai "What should my first commit message be?"
note create "Today's Tasks" "#development #important"

# Work on code
git add .
git commit -m "Initial commit"
ai "How do I push to remote?"

# Check status
git status
network ping github.com
note list
```

### **System Troubleshooting**
```bash
# Check system health
sys info
sys battery
network status

# Diagnose network
network ping 8.8.8.8
network dns google.com
network scan localhost 80,443,8080

# Get help
ai "My internet is slow, what should I check?"
```

### **Note & Project Management**
```bash
# Create project notes
note create "Mobile App Project" "#mobile #android #react-native"

# Search and organize
note search android
note list

# Export important notes
note export mobile-app-project
```

---

## 🔍 Command Reference

### **AI Commands**
| Command | Description | Example |
|---------|-------------|---------|
| `ai <message>` | Chat with AI | `ai "Help with Git"` |
| `ai config <key> <group>` | Setup AI | `ai config sk-abc def123` |
| `ai test` | Test connection | `ai test` |
| `ai status` | Show status | `ai status` |
| `ai models` | List models | `ai models` |

### **Developer Commands**
| Command | Description | Example |
|---------|-------------|---------|
| `git clone <url>` | Clone repo | `git clone https://github.com/user/repo.git` |
| `git status` | Show status | `git status` |
| `git add <file>` | Stage files | `git add .` |
| `git commit -m "<msg>"` | Commit | `git commit -m "Add feature"` |
| `file ls` | List files | `file ls /sdcard/` |
| `file cat <file>` | View file | `file cat README.md` |
| `file find <pattern>` | Search | `file find "*.java"` |

### **Productivity Commands**
| Command | Description | Example |
|---------|-------------|---------|
| `calc <expression>` | Calculate | `calc "2 * (3 + 4)"` |
| `network ping <host>` | Ping host | `network ping google.com` |
| `network scan <host> <ports>` | Port scan | `network scan localhost 80,443` |
| `note create <title>` | Create note | `note create "Ideas" "New feature"` |
| `note list` | List notes | `note list` |
| `note search <query>` | Search notes | `note search android` |
| `sys info` | System info | `sys info` |

### **Automation Commands**
| Command | Description | Example |
|---------|-------------|---------|
| `auto tasker` | List tasks | `auto tasker` |
| `auto task <name>` | Run task | `auto task "Morning"` |
| `auto termux <command>` | Run command | `auto termux "pkg update"` |
| `auto apps` | List apps | `auto apps` |

---

## ⚡ Pro Tips

### **Speed Up Your Workflow**
1. **Use aliases**: Create shortcuts for frequent commands
2. **Combine commands**: Use `&&` to chain commands
3. **Use AI context**: Ask follow-up questions
4. **Leverage search**: Use `file find` and `note search`

### **AI-Powered Productivity**
```bash
# Get explanations
ai "Explain what 'git status' shows"
ai "What does network ping measure?"

# Learn features
ai "How do I use the file manager?"
ai "What automation apps work with T-UI?"

# Problem solving
ai "I have merge conflicts, help me resolve them"
ai "My network is slow, what's wrong?"
```

### **Development Best Practices**
```bash
# Check before committing
git status
ai "Review my git changes"

# Organize work
note create "Daily Log" "#work #important"
note search today

# Monitor system
sys info
network status
```

---

## 🚨 Troubleshooting

### **Commands Not Working**
1. Check spelling: `help` for command list
2. Verify permissions for file operations
3. Test basic commands: `calc 2+2`

### **AI Not Responding**
1. Check: `ai status`
2. Test: `ai test`
3. Reconfigure: `ai config <key> <group>`

### **Network Issues**
1. Check: `network status`
2. Test: `network ping google.com`
3. Verify internet connection

### **File Operations**
1. Grant storage permissions
2. Use app directories: `/sdcard/Android/data/`
3. Check: `file ls /sdcard/`

---

## 📚 Resources

### **Documentation**
- **Main Guide**: `SMART-LAUNCHER-README.md`
- **AI Setup**: `AI-SETUP-GUIDE.md`
- **Integration**: `INTEGRATION-SUCCESS-REPORT.md`

### **Getting Help**
- Use `ai help` for AI assistance
- Check command help: `<command> --help`
- Run tests: Use SmartLauncherTestActivity

### **Community**
- Share workflows and tips
- Report issues or suggestions
- Contribute new features

---

## 🎯 What's Next?

### **Immediate Goals**
1. ✅ **Configure AI** for intelligent assistance
2. ✅ **Try developer tools** for your projects
3. ✅ **Set up automation** with Tasker/Termux
4. ✅ **Create note workflows** for organization

### **Advanced Features**
1. **Monaco Editor**: Code editing in T-UI
2. **Git GUI**: Visual Git operations
3. **Plugin System**: Custom commands
4. **Voice Commands**: Voice-activated operations

---

## 🎉 Success Indicators

You'll know you're successful when:

- ✅ **AI responds** to your questions intelligently
- ✅ **Git commands** work smoothly for version control
- ✅ **File operations** feel natural and fast
- ✅ **Network tools** help diagnose issues quickly
- ✅ **Notes system** keeps you organized
- ✅ **Automation** saves time on repetitive tasks

---

## 🚀 Ready to Launch!

Your T-UI Smart IDE Launcher is now ready for professional development and productivity work. 

**Start with**: `ai hello` and explore from there! 🎯

---

**Happy Computing!** 💻✨