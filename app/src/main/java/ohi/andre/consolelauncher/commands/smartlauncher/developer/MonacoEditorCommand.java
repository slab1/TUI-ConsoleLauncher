package ohi.andre.consolelauncher.commands.smartlauncher.developer;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;

/**
 * MonacoEditorCommand - Professional code editor integration
 * Provides full-featured code editing with Monaco Editor
 */
public class MonacoEditorCommand implements CommandAbstraction {

    private static final String TAG = "MonacoEditorCommand";
    private static final int MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @Override
    public String exec(ExecutePack pack) throws Exception {
        Context context = pack.context;
        
        ArrayList<String> argsList = new ArrayList<>();
        for (Object arg : pack.args) {
            if (arg != null) {
                argsList.add(arg.toString());
            }
        }

        if (argsList.isEmpty() || "--help".equals(argsList.get(0)) || "-h".equals(argsList.get(0))) {
            return getUsage();
        }

        String command = argsList.get(0).toLowerCase();
        ArrayList<String> parameters = new ArrayList<>(argsList.subList(1, argsList.size()));

        switch (command) {
            case "edit":
            case "code":
            case "monaco":
                return openEditor(context, parameters);
            case "new":
            case "create":
                return createNewFile(context, parameters);
            case "list":
            case "recent":
                return listRecentFiles(context);
            default:
                // Try to open as file
                return openEditor(context, argsList);
        }
    }

    @Override
    public int[] argType() {
        return new int[]{CommandAbstraction.COMMAND};
    }

    @Override
    public int priority() {
        return 6; // Higher priority for editor
    }

    @Override
    public int helpRes() {
        return 0;
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int indexNotFound) {
        return "Missing argument at position " + indexNotFound + "\n" + getUsage();
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        return "Not enough arguments (" + nArgs + " required)\n" + getUsage();
    }

    private String getUsage() {
        return "\n╔══════════════════════════════════════════════════════╗\n" +
               "║              MONACO EDITOR COMMANDS                   ║\n" +
               "╠══════════════════════════════════════════════════════╣\n" +
               "║  code <file>               - Open file in editor     ║\n" +
               "║  edit <file>               - Edit file               ║\n" +
               "║  monaco <file>             - Monaco editor           ║\n" +
               "║  code new <name>           - Create new file         ║\n" +
               "║  code create <path>        - Create and open         ║\n" +
               "║  code recent               - List recent files       ║\n" +
               "║                                                      ║\n" +
               "║  Auto-detects language from file extension          ║\n" +
               "║  Supports: Java, Kotlin, Python, JS, HTML, CSS      ║\n" +
               "║  Features: Syntax highlighting, autocomplete        ║\n" +
               "║  Integration: Git, AI assistance, Notes            ║\n" +
               "╚══════════════════════════════════════════════════════╝\n";
    }

    /**
     * Open file in Monaco Editor
     */
    private String openEditor(Context context, ArrayList<String> args) {
        if (args.isEmpty()) {
            return "Usage: code <file_path>\n" +
                   "Or: code new <filename.extension> to create new file";
        }

        String filePath = listToString(args);
        File file = new File(filePath);

        // Handle directory opening
        if (file.isDirectory()) {
            return openDirectoryInEditor(context, file);
        }

        // Check if file exists
        if (!file.exists()) {
            return "File not found: " + filePath + "\n" +
                   "Use 'code new " + filePath + "' to create it, or check the path.";
        }

        // Check file size
        if (file.length() > MAX_FILE_SIZE) {
            return "File too large (" + (file.length() / 1024 / 1024) + "MB)\n" +
                   "Maximum supported size is " + (MAX_FILE_SIZE / 1024 / 1024) + "MB";
        }

        // Launch Monaco Editor
        try {
            Intent intent = new Intent(context, MonacoEditorActivity.class);
            intent.putExtra(MonacoEditorActivity.EXTRA_FILE_PATH, file.getAbsolutePath());
            intent.putExtra(MonacoEditorActivity.EXTRA_IS_NEW_FILE, false);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);

            return "\n🚀 Opening " + file.getName() + " in Monaco Editor...\n" +
                   "📁 Path: " + file.getAbsolutePath() + "\n" +
                   "🔧 Features: Syntax highlighting, autocomplete, Git integration\n" +
                   "💾 Press Ctrl+S to save changes";
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch Monaco Editor", e);
            return "Failed to open editor: " + e.getMessage();
        }
    }

    /**
     * Create new file and open in editor
     */
    private String createNewFile(Context context, ArrayList<String> args) {
        if (args.isEmpty()) {
            return "Usage: code new <filename.extension>\n" +
                   "Example: code new MainActivity.java";
        }

        String fileName = args.get(0);
        
        // Determine default directory
        String defaultDir = "/sdcard/Documents/TUI-Editor/";
        File directory = new File(defaultDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File file = new File(directory, fileName);

        // Check if file already exists
        if (file.exists()) {
            return "File already exists: " + file.getAbsolutePath() + "\n" +
                   "Use 'code " + file.getAbsolutePath() + "' to edit it.";
        }

        try {
            // Create file with basic template
            String template = getFileTemplate(fileName);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();
            
            // Write template
            android.util.Log.i(TAG, "Creating file: " + file.getAbsolutePath());
            
            Intent intent = new Intent(context, MonacoEditorActivity.class);
            intent.putExtra(MonacoEditorActivity.EXTRA_FILE_PATH, file.getAbsolutePath());
            intent.putExtra(MonacoEditorActivity.EXTRA_IS_NEW_FILE, true);
            intent.putExtra(MonacoEditorActivity.EXTRA_INITIAL_CONTENT, template);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);

            return "\n✨ Creating new file: " + fileName + "\n" +
                   "📁 Location: " + file.getAbsolutePath() + "\n" +
                   "🎨 Template: " + getLanguageFromExtension(fileName) + "\n" +
                   "🚀 Opening in Monaco Editor...";
        } catch (Exception e) {
            Log.e(TAG, "Failed to create file", e);
            return "Failed to create file: " + e.getMessage();
        }
    }

    /**
     * List recent files
     */
    private String listRecentFiles(Context context) {
        // This would integrate with a file history system
        // For now, return available templates and recent locations
        return "\n📋 Recent Files & Templates\n" +
               "─".repeat(50) + "\n" +
               "Templates available:\n" +
               "• MainActivity.java (Android)\n" +
               "• main.py (Python)\n" +
               "• index.html (Web)\n" +
               "• style.css (CSS)\n" +
               "• script.js (JavaScript)\n" +
               "• README.md (Markdown)\n\n" +
               "Use: code new <template_name>\n\n" +
               "Recent locations:\n" +
               "• /sdcard/Documents/TUI-Editor/\n" +
               "• Current working directory\n\n" +
               "💡 Tip: Use 'file find *.java' to find Java files";
    }

    /**
     * Open directory in file explorer within editor
     */
    private String openDirectoryInEditor(Context context, File directory) {
        try {
            Intent intent = new Intent(context, MonacoEditorActivity.class);
            intent.putExtra(MonacoEditorActivity.EXTRA_DIRECTORY_PATH, directory.getAbsolutePath());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);

            return "\n📁 Opening directory: " + directory.getName() + "\n" +
                   "📍 Path: " + directory.getAbsolutePath() + "\n" +
                   "📝 Select a file to edit in Monaco Editor";
        } catch (Exception e) {
            Log.e(TAG, "Failed to open directory", e);
            return "Failed to open directory: " + e.getMessage();
        }
    }

    /**
     * Get file template based on extension
     */
    private String getFileTemplate(String fileName) {
        String lowerName = fileName.toLowerCase();
        
        if (lowerName.endsWith(".java")) {
            return "public class " + fileName.replace(".java", "") + " {\n" +
                   "    public static void main(String[] args) {\n" +
                   "        System.out.println(\"Hello, World!\");\n" +
                   "    }\n" +
                   "}\n";
        } else if (lowerName.endsWith(".py")) {
            return "# " + fileName.replace(".py", "") + "\n" +
                   "\n" +
                   "def main():\n" +
                   "    print(\"Hello, World!\")\n" +
                   "\n" +
                   "if __name__ == \"__main__\":\n" +
                   "    main()\n";
        } else if (lowerName.endsWith(".html")) {
            return "<!DOCTYPE html>\n" +
                   "<html lang=\"en\">\n" +
                   "<head>\n" +
                   "    <meta charset=\"UTF-8\">\n" +
                   "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                   "    <title>" + fileName.replace(".html", "") + "</title>\n" +
                   "</head>\n" +
                   "<body>\n" +
                   "    <h1>Hello, World!</h1>\n" +
                   "    <p>Welcome to Monaco Editor</p>\n" +
                   "</body>\n" +
                   "</html>\n";
        } else if (lowerName.endsWith(".css")) {
            return "/* " + fileName.replace(".css", "") + " */\n" +
                   "\n" +
                   "body {\n" +
                   "    font-family: Arial, sans-serif;\n" +
                   "    margin: 0;\n" +
                   "    padding: 20px;\n" +
                   "    background-color: #f5f5f5;\n" +
                   "}\n" +
                   "\n" +
                   "h1 {\n" +
                   "    color: #333;\n" +
                   "    text-align: center;\n" +
                   "}\n";
        } else if (lowerName.endsWith(".js")) {
            return "// " + fileName.replace(".js", "") + "\n" +
                   "\n" +
                   "function main() {\n" +
                   "    console.log('Hello, World!');\n" +
                   "    \n" +
                   "    // Add your code here\n" +
                   "    \n" +
                   "}\n" +
                   "\n" +
                   "// Execute when page loads\n" +
                   "document.addEventListener('DOMContentLoaded', main);\n";
        } else if (lowerName.endsWith(".md")) {
            return "# " + fileName.replace(".md", "") + "\n\n" +
                   "Welcome to Monaco Editor!\n\n" +
                   "## Features\n\n" +
                   "- Syntax highlighting\n" +
                   "- Auto-completion\n" +
                   "- Git integration\n" +
                   "- AI assistance\n\n" +
                   "## Getting Started\n\n" +
                   "Start editing your markdown file here.\n";
        } else {
            return "// " + fileName + "\n" +
                   "// Created with T-UI Smart IDE Launcher\n" +
                   "// Monaco Editor is ready for editing\n\n";
        }
    }

    /**
     * Get language from file extension
     */
    private String getLanguageFromExtension(String fileName) {
        String lowerName = fileName.toLowerCase();
        
        if (lowerName.endsWith(".java") || lowerName.endsWith(".kt")) {
            return "Java/Kotlin";
        } else if (lowerName.endsWith(".py")) {
            return "Python";
        } else if (lowerName.endsWith(".js") || lowerName.endsWith(".ts")) {
            return "JavaScript/TypeScript";
        } else if (lowerName.endsWith(".html")) {
            return "HTML";
        } else if (lowerName.endsWith(".css")) {
            return "CSS";
        } else if (lowerName.endsWith(".json")) {
            return "JSON";
        } else if (lowerName.endsWith(".xml")) {
            return "XML";
        } else if (lowerName.endsWith(".md")) {
            return "Markdown";
        } else {
            return "Text";
        }
    }

    private String listToString(ArrayList<String> list) {
        if (list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(" ");
            sb.append(list.get(i));
        }
        return sb.toString();
    }
}