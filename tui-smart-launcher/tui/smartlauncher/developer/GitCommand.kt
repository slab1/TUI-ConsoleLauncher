package tui.smartlauncher.developer

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import tui.smartlauncher.core.CommandHandler
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter

/**
 * Git Command - Basic Git operations for T-UI
 * Supports clone, status, commit, push, pull, and log
 */
class GitCommand : CommandHandler {

    companion object {
        private const val TAG = "GitCommand"
    }

    private var context: Context? = null

    override fun getName(): String = "git"

    override fun getAliases(): List<String> = listOf("git", "g")

    override fun getDescription(): String = "Git version control operations"

    override fun getUsage(): String = """
        ╔══════════════════════════════════════════════════════╗
        ║                     GIT COMMANDS                      ║
        ╠══════════════════════════════════════════════════════╣
        ║  git clone <url>              - Clone repository     ║
        ║  git status                   - Show status          ║
        ║  git add <file>               - Stage files          ║
        ║  git add .                    - Stage all            ║
        ║  git commit -m "<message>"    - Commit changes       ║
        ║  git push                     - Push to remote       ║
        ║  git pull                     - Pull from remote     ║
        ║  git log                      - Show commit log      ║
        ║  git diff                     - Show changes         ║
        ║  git branch                   - List branches        ║
        ║  git checkout <branch>        - Switch branch        ║
        ║  git remote                   - Show remotes         ║
        ║  git init                     - Initialize repo      ║
        ║  git open                     - Open repo in browser ║
        ╚══════════════════════════════════════════════════════╝
    """.trimIndent()

    override fun execute(context: Context, args: List<String>): String {
        this.context = context

        if (args.isEmpty() || args[0] == "--help" || args[0] == "-h") {
            return getUsage()
        }

        val command = args[0].lowercase()
        val parameters = args.drop(1)

        return when (command) {
            "clone" -> gitClone(parameters)
            "status" -> gitStatus()
            "add" -> gitAdd(parameters)
            "commit" -> gitCommit(parameters)
            "push" -> gitPush()
            "pull" -> gitPull()
            "log" -> gitLog()
            "diff" -> gitDiff()
            "branch" -> gitBranch()
            "checkout", "switch" -> gitCheckout(parameters)
            "remote" -> gitRemote()
            "init" -> gitInit()
            "open" -> gitOpen()
            else -> "Unknown git command: $command\n${getUsage()}"
        }
    }

    /**
     * Gets current git directory
     */
    private fun getGitDir(): File? {
        var dir = context?.filesDir
        while (dir != null) {
            val gitDir = File(dir, ".git")
            if (gitDir.exists() && gitDir.isDirectory) {
                return dir
            }
            dir = dir.parentFile
        }
        return null
    }

    /**
     * Executes a git command and returns output
     */
    private fun executeGit(vararg command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("git") + command, null, getGitDir())
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

            val output = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }

            while (errorReader.readLine().also { line = it } != null) {
                output.appendLine(line)
            }

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                output.insert(0, "Error (exit code $exitCode):\n")
            }

            output.toString()
        } catch (e: Exception) {
            "Git command failed: ${e.message}\nIs Git installed?"
        }
    }

    /**
     * Clones a repository
     */
    private fun gitClone(args: List<String>): String {
        if (args.isEmpty()) {
            return "Usage: git clone <repository-url> [directory]"
        }

        val url = args[0]
        val directory = args.getOrNull(1) ?: extractRepoName(url)

        val targetDir = File(context?.filesDir, directory)

        return try {
            val builder = StringBuilder()
            builder.appendLine()
            builder.appendLine("Cloning repository...")
            builder.appendLine("URL: $url")
            builder.appendLine("Directory: $directory")
            builder.appendLine("─".repeat(50))

            val process = Runtime.getRuntime().exec(
                arrayOf("git", "clone", url, directory),
                null,
                context?.filesDir
            )

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                builder.appendLine(line)
            }

            val exitCode = process.waitFor()
            builder.appendLine()
            builder.appendLine("Exit code: $exitCode")

            if (exitCode == 0 && targetDir.exists()) {
                builder.appendLine("✓ Repository cloned successfully!")
            }

            builder.toString()
        } catch (e: Exception) {
            "Clone failed: ${e.message}"
        }
    }

    /**
     * Shows repository status
     */
    private fun gitStatus(): String {
        val repoDir = getGitDir()
        if (repoDir == null) {
            return "Not in a git repository.\nUse 'git clone <url>' or 'git init'."
        }

        val output = executeGit("status", "--short")

        return buildString {
            appendLine()
            appendLine("Repository: ${repoDir.name}")
            appendLine("─".repeat(50))

            if (output.isBlank()) {
                appendLine("  Working tree clean ✓")
            } else {
                appendLine(output)
            }
        }
    }

    /**
     * Stages files
     */
    private fun gitAdd(args: List<String>): String {
        if (args.isEmpty()) {
            return "Usage: git add <file> or git add ."
        }

        val target = if (args[0] == ".") "--all" else args[0]
        val output = executeGit("add", target)

        return "Files staged.\n$output"
    }

    /**
     * Commits changes
     */
    private fun gitCommit(args: List<String>): String {
        val messageIndex = args.indexOf("-m")
        if (messageIndex == -1 || messageIndex + 1 >= args.size) {
            return "Usage: git commit -m \"<message>\""
        }

        val message = args[messageIndex + 1].removeSurrounding("\"")

        val output = executeGit("commit", "-m", message)

        return buildString {
            appendLine("Commit created.")
            appendLine("─".repeat(50))
            appendLine(output)
        }
    }

    /**
     * Pushes to remote
     */
    private fun gitPush(): String {
        val output = executeGit("push")

        return buildString {
            appendLine("Push result:")
            appendLine("─".repeat(50))
            appendLine(output)
        }
    }

    /**
     * Pulls from remote
     */
    private fun gitPull(): String {
        val output = executeGit("pull")

        return buildString {
            appendLine("Pull result:")
            appendLine("─".repeat(50))
            appendLine(output)
        }
    }

    /**
     * Shows commit log
     */
    private fun gitLog(): String {
        val output = executeGit("log", "--oneline", "-10")

        return buildString {
            appendLine()
            appendLine("Recent Commits (last 10):")
            appendLine("─".repeat(50))
            appendLine(output.ifBlank { "No commits yet." })
        }
    }

    /**
     * Shows changes
     */
    private fun gitDiff(): String {
        val output = executeGit("diff", "--stat")

        return buildString {
            appendLine("Changes:")
            appendLine("─".repeat(50))
            appendLine(output.ifBlank { "No changes to show." })
        }
    }

    /**
     * Lists branches
     */
    private fun gitBranch(): String {
        val output = executeGit("branch", "-a")

        return buildString {
            appendLine()
            appendLine("Branches:")
            appendLine("─".repeat(50))
            appendLine(output.ifBlank { "No branches." })
        }
    }

    /**
     * Switches branch
     */
    private fun gitCheckout(args: List<String>): String {
        if (args.isEmpty()) {
            return "Usage: git checkout <branch-name>"
        }

        val branch = args[0]
        val output = executeGit("checkout", branch)

        return buildString {
            appendLine("Switched to branch: $branch")
            appendLine("─".repeat(50))
            appendLine(output)
        }
    }

    /**
     * Shows remotes
     */
    private fun gitRemote(): String {
        val output = executeGit("remote", "-v")

        return buildString {
            appendLine()
            appendLine("Remotes:")
            appendLine("─".repeat(50))
            appendLine(output.ifBlank { "No remotes configured." })
        }
    }

    /**
     * Initializes new repository
     */
    private fun gitInit(): String {
        val output = executeGit("init")

        return buildString {
            appendLine("Repository initialized.")
            appendLine("─".repeat(50))
            appendLine(output)
        }
    }

    /**
     * Opens repository URL in browser
     */
    private fun gitOpen(): String {
        val repoDir = getGitDir()
        if (repoDir == null) {
            return "Not in a git repository."
        }

        // Try to get remote URL
        val output = executeGit("remote", "get-url", "origin")
        val remoteUrl = output.trim()

        if (remoteUrl.isBlank() || remoteUrl.startsWith("fatal")) {
            return "No remote origin configured."
        }

        // Convert SSH URL to HTTPS if needed
        val httpsUrl = remoteUrl
            .replace("git@", "")
            .replace(":", "/")
            .replace("github.com/", "github.com/")

        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://$httpsUrl"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context?.startActivity(intent)
            "Opening: $httpsUrl"
        } catch (e: Exception) {
            "Could not open URL: ${e.message}"
        }
    }

    private fun extractRepoName(url: String): String {
        return url
            .removeSuffix(".git")
            .substringAfterLast("/")
    }
}
