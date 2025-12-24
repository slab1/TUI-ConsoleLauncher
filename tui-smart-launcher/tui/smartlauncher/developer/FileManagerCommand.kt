package tui.smartlauncher.developer

import android.content.Context
import android.os.Environment
import android.util.Log
import tui.smartlauncher.core.CommandHandler
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * File Manager Command - Comprehensive file operations
 * Supports navigation, reading, writing, and basic file management
 */
class FileManagerCommand : CommandHandler {

    companion object {
        private const val TAG = "FileManager"
    }

    private var currentDirectory: File = Environment.getExternalStorageDirectory()
    private val history = mutableListOf<File>()
    private var historyIndex = -1

    override fun getName(): String = "file"

    override fun getAliases(): List<String> = listOf("files", "fm", "ls", "cd", "cat", "mkdir", "rm", "cp", "mv")

    override fun getDescription(): String = "File management and navigation"

    override fun getUsage(): String = """
        ╔══════════════════════════════════════════════════════╗
        ║                  FILE COMMANDS                        ║
        ╠══════════════════════════════════════════════════════╣
        ║  file ls              - List directory contents      ║
        ║  file cd <dir>        - Change directory             ║
        ║  file pwd             - Show current directory       ║
        ║  file cat <file>      - Display file contents        ║
        ║  file mkdir <name>    - Create directory             ║
        ║  file touch <name>    - Create empty file            ║
        ║  file rm <path>       - Delete file/directory        ║
        ║  file cp <src> <dst>  - Copy file                    ║
        ║  file mv <src> <dst>  - Move/rename file             ║
        ║  file wc <file>       - Count lines/words/chars      ║
        ║  file find <pattern>  - Search for files             ║
        ║  file info <path>     - Show file information        ║
        ╚══════════════════════════════════════════════════════╝
    """.trimIndent()

    override fun execute(context: Context, args: List<String>): String {
        if (args.isEmpty()) {
            return listDirectory(currentDirectory)
        }

        val command = args[0].lowercase()
        val parameters = args.drop(1)

        return when (command) {
            "ls", "list" -> listDirectory(currentDirectory, parameters)
            "cd", "chdir" -> changeDirectory(parameters)
            "pwd" -> showCurrentDirectory()
            "cat", "type", "view" -> readFile(parameters)
            "mkdir" -> makeDirectory(parameters)
            "touch" -> createFile(parameters)
            "rm", "del", "delete" -> deleteFile(parameters)
            "cp", "copy" -> copyFile(parameters)
            "mv", "move", "ren", "rename" -> moveFile(parameters)
            "wc" -> wordCount(parameters)
            "find", "search" -> findFiles(parameters)
            "info", "stat" -> fileInfo(parameters)
            "tree" -> showTree(parameters)
            else -> "Unknown file command: $command\n${getUsage()}"
        }
    }

    /**
     * Lists directory contents
     */
    private fun listDirectory(dir: File, args: List<String> = emptyList()): String {
        if (!dir.exists() || !dir.isDirectory) {
            return "Directory not found: ${dir.absolutePath}"
        }

        val showAll = args.contains("-a") || args.contains("--all")
        val showLong = args.contains("-l") || args.contains("--long")
        val sortBySize = args.contains("-S")
        val sortByTime = args.contains("-t")

        var files = dir.listFiles()?.toMutableList() ?: return "Cannot read directory"

        if (!showAll) {
            files = files.filter { !it.name.startsWith(".") }.toMutableList()
        }

        // Sorting
        files = when {
            sortBySize -> files.sortedByDescending { it.length }.toMutableList()
            sortByTime -> files.sortedByDescending { it.lastModified() }.toMutableList()
            else -> files.sortedBy { it.name.lowercase() }.toMutableList()
        }

        val builder = StringBuilder()
        builder.appendLine()
        builder.appendLine("Directory: ${dir.absolutePath}")
        builder.appendLine("─".repeat(60))

        if (showLong) {
            builder.appendLine(String.format("%-10s %-10s %-15s %s", "SIZE", "TYPE", "DATE", "NAME"))
            builder.appendLine("─".repeat(60))
        }

        files.forEach { file ->
            if (showLong) {
                val type = if (file.isDirectory) "DIR" else "FILE"
                val date = formatDate(file.lastModified())
                val size = formatSize(file.length())
                builder.appendLine(String.format("%-10s %-10s %-15s %s", size, type, date, file.name))
            } else {
                val suffix = if (file.isDirectory) "/" else ""
                builder.appendLine("  ${file.name}$suffix")
            }
        }

        builder.appendLine("─".repeat(60))
        builder.appendLine("${files.count()} items")

        return builder.toString()
    }

    /**
     * Changes current directory
     */
    private fun changeDirectory(args: List<String>): String {
        if (args.isEmpty()) {
            return "Usage: file cd <directory>\nCurrent: ${currentDirectory.absolutePath}"
        }

        val targetPath = args[0]
        val target = when {
            targetPath == ".." -> currentDirectory.parentFile
            targetPath == "../" -> currentDirectory.parentFile
            targetPath.startsWith("/") -> File(targetPath)
            targetPath == "~" -> Environment.getExternalStorageDirectory()
            else -> File(currentDirectory, targetPath)
        }

        if (target == null) {
            return "Cannot navigate to: $targetPath"
        }

        if (!target.exists()) {
            return "Directory does not exist: $targetPath"
        }

        if (!target.isDirectory) {
            return "Not a directory: $targetPath"
        }

        // Add to history
        history.add(currentDirectory)
        historyIndex = history.size - 1
        currentDirectory = target

        return "Changed to: ${target.absolutePath}"
    }

    /**
     * Shows current directory
     */
    private fun showCurrentDirectory(): String {
        return currentDirectory.absolutePath
    }

    /**
     * Reads file contents
     */
    private fun readFile(args: List<String>): String {
        if (args.isEmpty()) {
            return "Usage: file cat <filename>"
        }

        val fileName = args[0]
        val file = if (fileName.startsWith("/")) {
            File(fileName)
        } else {
            File(currentDirectory, fileName)
        }

        if (!file.exists()) {
            return "File not found: $fileName"
        }

        if (file.isDirectory) {
            return "Use 'ls' to view directory contents"
        }

        return try {
            val lines = file.readLines()
            val builder = StringBuilder()
            builder.appendLine()
            builder.appendLine("File: ${file.name} (${file.length()} bytes)")
            builder.appendLine("─".repeat(60))

            // Limit output for large files
            if (lines.size > 500) {
                builder.appendLine("(Showing first 500 of ${lines.size} lines)")
                builder.appendLine("─".repeat(60))
                lines.take(500).forEach { builder.appendLine(it) }
            } else {
                lines.forEach { builder.appendLine(it) }
            }

            builder.appendLine("─".repeat(60))
            builder.appendLine("${lines.size} lines")
            builder.toString()
        } catch (e: Exception) {
            "Error reading file: ${e.message}"
        }
    }

    /**
     * Creates a directory
     */
    private fun makeDirectory(args: List<String>): String {
        if (args.isEmpty()) {
            return "Usage: file mkdir <directory_name>"
        }

        val dirName = args[0]
        val parent = if (args.size > 1) {
            if (args[1].startsWith("/")) File(args[1]) else File(currentDirectory, args[1])
        } else {
            currentDirectory
        }

        val newDir = File(parent, dirName)

        return try {
            if (newDir.mkdirs()) {
                "Created directory: ${newDir.absolutePath}"
            } else {
                "Failed to create directory: ${e.message}"
            }
        } catch (e: Exception) {
            "Error creating directory: ${e.message}"
        }
    }

    /**
     * Creates an empty file
     */
    private fun createFile(args: List<String>): String {
        if (args.isEmpty()) {
            return "Usage: file touch <filename>"
        }

        val fileName = args[0]
        val file = File(currentDirectory, fileName)

        return try {
            if (file.createNewFile()) {
                "Created file: ${file.absolutePath}"
            } else {
                "File already exists: ${file.name}"
            }
        } catch (e: Exception) {
            "Error creating file: ${e.message}"
        }
    }

    /**
     * Deletes a file or directory
     */
    private fun deleteFile(args: List<String>): String {
        if (args.isEmpty()) {
            return "Usage: file rm <path>"
        }

        val fileName = args[0]
        val recursive = args.contains("-r") || args.contains("--recursive")
        val file = File(currentDirectory, fileName)

        if (!file.exists()) {
            return "Path not found: $fileName"
        }

        return try {
            if (file.isDirectory && !recursive) {
                "Use 'rm -r' to delete directory"
            } else {
                val deleted = if (recursive) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }

                if (deleted) {
                    "Deleted: $fileName"
                } else {
                    "Failed to delete: $fileName"
                }
            }
        } catch (e: Exception) {
            "Error deleting: ${e.message}"
        }
    }

    /**
     * Copies a file
     */
    private fun copyFile(args: List<String>): String {
        if (args.size < 2) {
            return "Usage: file cp <source> <destination>"
        }

        val sourceName = args[0]
        val destName = args[1]

        val source = if (sourceName.startsWith("/")) File(sourceName) else File(currentDirectory, sourceName)
        val dest = if (destName.startsWith("/")) File(destName) else File(currentDirectory, destName)

        if (!source.exists()) {
            return "Source not found: $sourceName"
        }

        if (source.isDirectory) {
            return "Use 'cp -r' for directories"
        }

        return try {
            source.copyTo(dest, overwrite = true)
            "Copied: $sourceName -> $destName"
        } catch (e: Exception) {
            "Error copying: ${e.message}"
        }
    }

    /**
     * Moves or renames a file
     */
    private fun moveFile(args: List<String>): String {
        if (args.size < 2) {
            return "Usage: file mv <source> <destination>"
        }

        val sourceName = args[0]
        val destName = args[1]

        val source = File(currentDirectory, sourceName)
        val dest = File(currentDirectory, destName)

        if (!source.exists()) {
            return "Source not found: $sourceName"
        }

        return try {
            source.renameTo(dest)
            "Moved: $sourceName -> $destName"
        } catch (e: Exception) {
            "Error moving: ${e.message}"
        }
    }

    /**
     * Word, line, and character count
     */
    private fun wordCount(args: List<String>): String {
        if (args.isEmpty()) {
            return "Usage: file wc <filename>"
        }

        val fileName = args[0]
        val file = File(currentDirectory, fileName)

        if (!file.exists() || file.isDirectory) {
            return "File not found: $fileName"
        }

        return try {
            val content = file.readText()
            val lines = content.lines()
            val words = content.split(Regex("\\s+")).filter { it.isNotBlank() }

            String.format("%5d %5d %5d %s", lines.size, words.size, content.length, fileName)
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    /**
     * Finds files matching a pattern
     */
    private fun findFiles(args: List<String>): String {
        if (args.isEmpty()) {
            return "Usage: file find <pattern>"
        }

        val pattern = args[0].lowercase()
        val maxDepth = args.find { it == "-maxdepth" }?.let {
            args.indexOf(it) + 1
        }?.let { args.getOrNull(it)?.toIntOrNull() } ?: 3

        val results = mutableListOf<File>()
        searchFiles(currentDirectory, pattern, maxDepth, 0, results)

        val builder = StringBuilder()
        builder.appendLine()
        builder.appendLine("Search results for \"$pattern\" (max depth: $maxDepth):")
        builder.appendLine("─".repeat(60))

        if (results.isEmpty()) {
            builder.appendLine("No matches found")
        } else {
            results.take(50).forEach { file ->
                builder.appendLine("  ${file.relativeTo(currentDirectory)}")
            }
            if (results.size > 50) {
                builder.appendLine("  ... and ${results.size - 50} more")
            }
        }

        return builder.toString()
    }

    private fun searchFiles(dir: File, pattern: String, maxDepth: Int, currentDepth: Int, results: MutableList<File>) {
        if (currentDepth > maxDepth || dir.listFiles() == null) return

        dir.listFiles()?.forEach { file ->
            if (file.name.lowercase().contains(pattern)) {
                results.add(file)
            }
            if (file.isDirectory && currentDepth < maxDepth) {
                searchFiles(file, pattern, maxDepth, currentDepth + 1, results)
            }
        }
    }

    /**
     * Shows file information
     */
    private fun fileInfo(args: List<String>): String {
        if (args.isEmpty()) {
            return "Usage: file info <filename>"
        }

        val fileName = args[0]
        val file = File(currentDirectory, fileName)

        if (!file.exists()) {
            return "File not found: $fileName"
        }

        val builder = StringBuilder()
        builder.appendLine()
        builder.appendLine("File Information: ${file.name}")
        builder.appendLine("═".repeat(60))
        builder.appendLine("  Path: ${file.absolutePath}")
        builder.appendLine("  Size: ${formatSize(file.length())}")
        builder.appendLine("  Type: ${if (file.isDirectory) "Directory" else "File"}")
        builder.appendLine("  Permissions: ${getPermissions(file)}")
        builder.appendLine("  Created: ${formatDate(file.lastModified())}")
        builder.appendLine("  Modified: ${formatDate(file.lastModified())}")
        builder.appendLine("  Readable: ${file.canRead()}")
        builder.appendLine("  Writable: ${file.canWrite()}")
        builder.appendLine("  Executable: ${file.canExecute()}")

        return builder.toString()
    }

    /**
     * Shows directory tree
     */
    private fun showTree(args: List<String>): String {
        val maxDepth = args.find { it.startsWith("-") }?.let {
            it.substringAfter("depth=")?.toIntOrNull() ?: it.substringAfter("-")?.toIntOrNull()
        } ?: 2

        val builder = StringBuilder()
        builder.appendLine()
        builder.appendLine("Directory Tree: ${currentDirectory.name}")
        builder.appendLine("─".repeat(60))

        buildTree(currentDirectory, "", true, maxDepth, 0, builder)

        return builder.toString()
    }

    private fun buildTree(
        dir: File,
        prefix: String,
        isLast: Boolean,
        maxDepth: Int,
        currentDepth: Int,
        builder: StringBuilder
    ) {
        if (currentDepth > maxDepth) return

        val branch = if (isLast) "└── " else "├── "
        builder.appendLine("$prefix$branch${dir.name}/")

        val newPrefix = if (isLast) "$prefix    " else "$prefix│   "

        dir.listFiles()?.sortedBy { it.name.lowercase() }?.forEachIndexed { index, file ->
            val isLastItem = index == (dir.listFiles()?.size ?: 0) - 1
            if (file.isDirectory) {
                buildTree(file, newPrefix, isLastItem, maxDepth, currentDepth + 1, builder)
            } else {
                builder.appendLine("$newPrefix${if (isLastItem) "└── " else "├── "}${file.name}")
            }
        }
    }

    private fun getPermissions(file: File): String {
        val perms = StringBuilder()
        perms.append(if (file.canRead()) "r" else "-")
        perms.append(if (file.canWrite()) "w" else "-")
        perms.append(if (file.canExecute()) "x" else "-")
        return perms.toString()
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
}
