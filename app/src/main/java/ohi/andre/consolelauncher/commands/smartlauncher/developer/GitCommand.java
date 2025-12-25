package ohi.andre.consolelauncher.commands.smartlauncher.developer;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;

/**
 * Git Command - Basic Git operations for T-UI
 * Supports clone, status, commit, push, pull, and log
 */
public class GitCommand implements CommandAbstraction {

    private static final String TAG = "GitCommand";
    private static final String DEFAULT_HELP = "Git version control operations";

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
            case "clone":
                return gitClone(context, parameters);
            case "status":
                return gitStatus(context);
            case "add":
                return gitAdd(context, parameters);
            case "commit":
                return gitCommit(context, parameters);
            case "push":
                return gitPush(context);
            case "pull":
                return gitPull(context);
            case "log":
                return gitLog(context);
            case "diff":
                return gitDiff(context);
            case "branch":
                return gitBranch(context);
            case "checkout":
            case "switch":
                return gitCheckout(context, parameters);
            case "remote":
                return gitRemote(context);
            case "init":
                return gitInit(context);
            case "open":
                return gitOpen(context);
            default:
                return "Unknown git command: " + command + "\n" + getUsage();
        }
    }

    @Override
    public int[] argType() {
        return new int[]{CommandAbstraction.COMMAND};
    }

    @Override
    public int priority() {
        return 5;
    }

    @Override
    public int helpRes() {
        return 0; // We'll return the help text directly in the exec method
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
               "║                     GIT COMMANDS                      ║\n" +
               "╠══════════════════════════════════════════════════════╣\n" +
               "║  git clone <url>              - Clone repository     ║\n" +
               "║  git status                   - Show status          ║\n" +
               "║  git add <file>               - Stage files          ║\n" +
               "║  git add .                    - Stage all            ║\n" +
               "║  git commit -m \"<message>\"    - Commit changes       ║\n" +
               "║  git push                     - Push to remote       ║\n" +
               "║  git pull                     - Pull from remote     ║\n" +
               "║  git log                      - Show commit log      ║\n" +
               "║  git diff                     - Show changes         ║\n" +
               "║  git branch                   - List branches        ║\n" +
               "║  git checkout <branch>        - Switch branch        ║\n" +
               "║  git remote                   - Show remotes         ║\n" +
               "║  git init                     - Initialize repo      ║\n" +
               "║  git open                     - Open repo in browser ║\n" +
               "╚══════════════════════════════════════════════════════╝\n";
    }

    /**
     * Gets current git directory
     */
    private File getGitDir(Context context) {
        File dir = context.getFilesDir();
        while (dir != null) {
            File gitDir = new File(dir, ".git");
            if (gitDir.exists() && gitDir.isDirectory()) {
                return dir;
            }
            dir = dir.getParentFile();
        }
        return null;
    }

    /**
     * Executes a git command and returns output
     */
    private String executeGit(Context context, String... command) {
        try {
            File gitDir = getGitDir(context);
            if (gitDir == null) {
                return "Not in a git repository. Use 'git clone <url>' or 'git init'.";
            }

            Process process = Runtime.getRuntime().exec(
                mergeArray("git", command), 
                null, 
                gitDir
            );

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            StringBuilder output = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            while ((line = errorReader.readLine()) != null) {
                output.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                output.insert(0, "Error (exit code " + exitCode + "):\n");
            }

            return output.toString();
        } catch (Exception e) {
            Log.e(TAG, "Git command failed", e);
            return "Git command failed: " + e.getMessage() + "\nIs Git installed?";
        }
    }

    /**
     * Helper method to merge command array
     */
    private String[] mergeArray(String first, String... rest) {
        String[] result = new String[rest.length + 1];
        result[0] = first;
        System.arraycopy(rest, 0, result, 1, rest.length);
        return result;
    }

    /**
     * Clones a repository
     */
    private String gitClone(Context context, ArrayList<String> args) {
        if (args.isEmpty()) {
            return "Usage: git clone <repository-url> [directory]";
        }

        String url = args.get(0);
        String directory = args.size() > 1 ? args.get(1) : extractRepoName(url);

        File targetDir = new File(context.getFilesDir(), directory);

        try {
            StringBuilder builder = new StringBuilder();
            builder.append("\n");
            builder.append("Cloning repository...\n");
            builder.append("URL: ").append(url).append("\n");
            builder.append("Directory: ").append(directory).append("\n");
            builder.append("─".repeat(50)).append("\n");

            Process process = Runtime.getRuntime().exec(
                mergeArray("git", "clone", url, directory),
                null,
                context.getFilesDir()
            );

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            builder.append("\n");
            builder.append("Exit code: ").append(exitCode).append("\n");

            if (exitCode == 0 && targetDir.exists()) {
                builder.append("✓ Repository cloned successfully!");
            }

            return builder.toString();
        } catch (Exception e) {
            Log.e(TAG, "Clone failed", e);
            return "Clone failed: " + e.getMessage();
        }
    }

    /**
     * Shows repository status
     */
    private String gitStatus(Context context) {
        File repoDir = getGitDir(context);
        if (repoDir == null) {
            return "Not in a git repository.\nUse 'git clone <url>' or 'git init'.";
        }

        String output = executeGit(context, "status", "--short");

        StringBuilder result = new StringBuilder();
        result.append("\n");
        result.append("Repository: ").append(repoDir.getName()).append("\n");
        result.append("─".repeat(50)).append("\n");

        if (output.trim().isEmpty()) {
            result.append("  Working tree clean ✓\n");
        } else {
            result.append(output);
        }

        return result.toString();
    }

    /**
     * Stages files
     */
    private String gitAdd(Context context, ArrayList<String> args) {
        if (args.isEmpty()) {
            return "Usage: git add <file> or git add .";
        }

        String target = ".".equals(args.get(0)) ? "--all" : args.get(0);
        String output = executeGit(context, "add", target);

        return "Files staged.\n" + output;
    }

    /**
     * Commits changes
     */
    private String gitCommit(Context context, ArrayList<String> args) {
        int messageIndex = args.indexOf("-m");
        if (messageIndex == -1 || messageIndex + 1 >= args.size()) {
            return "Usage: git commit -m \"<message>\"";
        }

        String message = args.get(messageIndex + 1);
        if (message.startsWith("\"") && message.endsWith("\"")) {
            message = message.substring(1, message.length() - 1);
        }

        String output = executeGit(context, "commit", "-m", message);

        StringBuilder result = new StringBuilder();
        result.append("Commit created.\n");
        result.append("─".repeat(50)).append("\n");
        result.append(output);

        return result.toString();
    }

    /**
     * Pushes to remote
     */
    private String gitPush(Context context) {
        String output = executeGit(context, "push");

        StringBuilder result = new StringBuilder();
        result.append("Push result:\n");
        result.append("─".repeat(50)).append("\n");
        result.append(output);

        return result.toString();
    }

    /**
     * Pulls from remote
     */
    private String gitPull(Context context) {
        String output = executeGit(context, "pull");

        StringBuilder result = new StringBuilder();
        result.append("Pull result:\n");
        result.append("─".repeat(50)).append("\n");
        result.append(output);

        return result.toString();
    }

    /**
     * Shows commit log
     */
    private String gitLog(Context context) {
        String output = executeGit(context, "log", "--oneline", "-10");

        StringBuilder result = new StringBuilder();
        result.append("\n");
        result.append("Recent Commits (last 10):\n");
        result.append("─".repeat(50)).append("\n");
        
        if (output.trim().isEmpty()) {
            result.append("No commits yet.");
        } else {
            result.append(output);
        }

        return result.toString();
    }

    /**
     * Shows changes
     */
    private String gitDiff(Context context) {
        String output = executeGit(context, "diff", "--stat");

        StringBuilder result = new StringBuilder();
        result.append("Changes:\n");
        result.append("─".repeat(50)).append("\n");
        
        if (output.trim().isEmpty()) {
            result.append("No changes to show.");
        } else {
            result.append(output);
        }

        return result.toString();
    }

    /**
     * Lists branches
     */
    private String gitBranch(Context context) {
        String output = executeGit(context, "branch", "-a");

        StringBuilder result = new StringBuilder();
        result.append("\n");
        result.append("Branches:\n");
        result.append("─".repeat(50)).append("\n");
        
        if (output.trim().isEmpty()) {
            result.append("No branches.");
        } else {
            result.append(output);
        }

        return result.toString();
    }

    /**
     * Switches branch
     */
    private String gitCheckout(Context context, ArrayList<String> args) {
        if (args.isEmpty()) {
            return "Usage: git checkout <branch-name>";
        }

        String branch = args.get(0);
        String output = executeGit(context, "checkout", branch);

        StringBuilder result = new StringBuilder();
        result.append("Switched to branch: ").append(branch).append("\n");
        result.append("─".repeat(50)).append("\n");
        result.append(output);

        return result.toString();
    }

    /**
     * Shows remotes
     */
    private String gitRemote(Context context) {
        String output = executeGit(context, "remote", "-v");

        StringBuilder result = new StringBuilder();
        result.append("\n");
        result.append("Remotes:\n");
        result.append("─".repeat(50)).append("\n");
        
        if (output.trim().isEmpty()) {
            result.append("No remotes configured.");
        } else {
            result.append(output);
        }

        return result.toString();
    }

    /**
     * Initializes new repository
     */
    private String gitInit(Context context) {
        String output = executeGit(context, "init");

        StringBuilder result = new StringBuilder();
        result.append("Repository initialized.\n");
        result.append("─".repeat(50)).append("\n");
        result.append(output);

        return result.toString();
    }

    /**
     * Opens repository URL in browser
     */
    private String gitOpen(Context context) {
        File repoDir = getGitDir(context);
        if (repoDir == null) {
            return "Not in a git repository.";
        }

        // Try to get remote URL
        String output = executeGit(context, "remote", "get-url", "origin");
        String remoteUrl = output.trim();

        if (remoteUrl.isEmpty() || remoteUrl.startsWith("fatal")) {
            return "No remote origin configured.";
        }

        // Convert SSH URL to HTTPS if needed
        String httpsUrl = remoteUrl
                .replace("git@", "")
                .replace(":", "/")
                .replace("github.com/", "github.com/");

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://" + httpsUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return "Opening: " + httpsUrl;
        } catch (Exception e) {
            Log.e(TAG, "Could not open URL", e);
            return "Could not open URL: " + e.getMessage();
        }
    }

    private String extractRepoName(String url) {
        String trimmed = url.endsWith(".git") ? url.substring(0, url.length() - 4) : url;
        int lastSlash = trimmed.lastIndexOf('/');
        return lastSlash >= 0 ? trimmed.substring(lastSlash + 1) : trimmed;
    }
}