package ohi.andre.consolelauncher.commands.smartlauncher.developer;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Smart Launcher File Manager Command
 * Provides basic file operations within the app's sandbox
 */
public class FileManagerCommand implements CommandAbstraction {

    private static final int MAX_FILE_SIZE = 1024 * 1024; // 1MB limit
    private File currentDir = null;

    @Override
    public String exec(ExecutePack pack) throws Exception {
        Context context = pack.context;
        
        if (pack.mArgs == null || pack.mArgs.length == 0) {
            return listFiles(context);
        }
        
        String operation = pack.mArgs[0].toString().toLowerCase();
        
        switch (operation) {
            case "ls":
            case "list":
                return listFiles(context);
            case "cd":
                if (pack.mArgs.length < 2) {
                    return "Usage: file cd <directory>";
                }
                return changeDirectory(context, pack.mArgs[1].toString());
            case "pwd":
                return printWorkingDirectory();
            case "cat":
                if (pack.mArgs.length < 2) {
                    return "Usage: file cat <filename>";
                }
                return catFile(context, pack.mArgs[1].toString());
            case "mkdir":
                if (pack.mArgs.length < 2) {
                    return "Usage: file mkdir <directory_name>";
                }
                return makeDirectory(context, pack.mArgs[1].toString());
            case "touch":
                if (pack.mArgs.length < 2) {
                    return "Usage: file touch <filename>";
                }
                return touchFile(context, pack.mArgs[1].toString());
            case "rm":
                if (pack.mArgs.length < 2) {
                    return "Usage: file rm <path>";
                }
                return removeFile(context, pack.mArgs[1].toString());
            case "info":
                if (pack.mArgs.length < 2) {
                    return "Usage: file info <path>";
                }
                return fileInfo(context, pack.mArgs[1].toString());
            default:
                return "Unknown operation: " + operation + "\nAvailable: ls, cd, pwd, cat, mkdir, touch, rm, info";
        }
    }

    @Override
    public int[] argType() {
        return new int[]{CommandAbstraction.PLAIN_TEXT};
    }

    @Override
    public int priority() {
        return 3;
    }

    @Override
    public int helpRes() {
        return 0;
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int indexNotFound) {
        return "File command requires more arguments";
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        return "File command requires arguments\nAvailable: ls, cd, pwd, cat, mkdir, touch, rm, info";
    }

    private String listFiles(Context context) throws Exception {
        File dir = currentDir != null ? currentDir : context.getFilesDir();
        
        if (!dir.exists() || !dir.isDirectory()) {
            return "Directory not found: " + (currentDir != null ? currentDir.getAbsolutePath() : "app files");
        }
        
        File[] files = dir.listFiles();
        if (files == null) {
            return "Cannot read directory";
        }
        
        StringBuilder output = new StringBuilder();
        output.append("\nDirectory: ").append(dir.getAbsolutePath()).append("\n");
        output.append("─".repeat(60)).append("\n");
        
        for (File file : files) {
            String name = file.getName();
            String type = file.isDirectory() ? "DIR" : "FILE";
            String size = file.isDirectory() ? "" : formatBytes(file.length());
            output.append(String.format("%-20s %-8s %-12s\n", name, type, size));
        }
        
        output.append("─".repeat(60)).append("\n");
        output.append(files.length).append(" items\n");
        
        return output.toString();
    }

    private String changeDirectory(Context context, String path) throws Exception {
        File newDir;
        
        if (path.equals("..")) {
            if (currentDir == null || currentDir.equals(context.getFilesDir())) {
                return "Already at root";
            }
            newDir = currentDir.getParentFile();
        } else if (path.equals("~") || path.equals("/")) {
            newDir = context.getFilesDir();
        } else if (path.startsWith("/")) {
            newDir = new File(path);
        } else {
            File baseDir = currentDir != null ? currentDir : context.getFilesDir();
            newDir = new File(baseDir, path);
        }
        
        if (!newDir.exists() || !newDir.isDirectory()) {
            return "Directory not found: " + path;
        }
        
        currentDir = newDir;
        return "Changed to: " + newDir.getAbsolutePath();
    }

    private String printWorkingDirectory() {
        return currentDir != null ? currentDir.getAbsolutePath() : "app files directory";
    }

    private String catFile(Context context, String filename) throws Exception {
        File file = resolveFile(context, filename);
        
        if (!file.exists()) {
            return "File not found: " + filename;
        }
        
        if (file.isDirectory()) {
            return "Use 'file ls' to view directory contents";
        }
        
        if (file.length() > MAX_FILE_SIZE) {
            return "File too large to display (>1MB)";
        }
        
        StringBuilder content = new StringBuilder();
        try (FileReader reader = new FileReader(file)) {
            int c;
            while ((c = reader.read()) != -1) {
                content.append((char) c);
            }
        }
        
        StringBuilder output = new StringBuilder();
        output.append("\nFile: ").append(file.getName()).append(" (").append(formatBytes(file.length())).append(")\n");
        output.append("─".repeat(60)).append("\n");
        output.append(content.toString());
        output.append("\n").append("─".repeat(60)).append("\n");
        
        return output.toString();
    }

    private String makeDirectory(Context context, String dirname) throws Exception {
        File dir = resolveFile(context, dirname);
        
        if (dir.exists()) {
            return "Directory already exists: " + dirname;
        }
        
        if (dir.mkdirs()) {
            return "Directory created: " + dir.getAbsolutePath();
        } else {
            return "Failed to create directory: " + dirname;
        }
    }

    private String touchFile(Context context, String filename) throws Exception {
        File file = resolveFile(context, filename);
        
        if (file.exists()) {
            return "File already exists: " + filename;
        }
        
        if (file.createNewFile()) {
            return "File created: " + file.getAbsolutePath();
        } else {
            return "Failed to create file: " + filename;
        }
    }

    private String removeFile(Context context, String path) throws Exception {
        File file = resolveFile(context, path);
        
        if (!file.exists()) {
            return "Path not found: " + path;
        }
        
        boolean deleted = file.delete();
        if (deleted) {
            return "Deleted: " + path;
        } else {
            return "Failed to delete: " + path;
        }
    }

    private String fileInfo(Context context, String path) throws Exception {
        File file = resolveFile(context, path);
        
        if (!file.exists()) {
            return "Path not found: " + path;
        }
        
        StringBuilder output = new StringBuilder();
        output.append("\nFile Information: ").append(file.getName()).append("\n");
        output.append("═".repeat(60)).append("\n");
        output.append("  Path: ").append(file.getAbsolutePath()).append("\n");
        output.append("  Size: ").append(formatBytes(file.length())).append("\n");
        output.append("  Type: ").append(file.isDirectory() ? "Directory" : "File").append("\n");
        output.append("  Created: ").append(formatDate(file.lastModified())).append("\n");
        output.append("  Modified: ").append(formatDate(file.lastModified())).append("\n");
        output.append("  Readable: ").append(file.canRead() ? "Yes" : "No").append("\n");
        output.append("  Writable: ").append(file.canWrite() ? "Yes" : "No").append("\n");
        
        return output.toString();
    }

    private File resolveFile(Context context, String path) {
        if (path.startsWith("/")) {
            return new File(path);
        } else {
            File baseDir = currentDir != null ? currentDir : context.getFilesDir();
            return new File(baseDir, path);
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return (bytes / 1024) + " KB";
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        }
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
