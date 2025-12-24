package tui.smartlauncher.productivity

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Notes Command - Quick note taking and retrieval
 * Supports creating, listing, searching, and managing notes
 */
class NotesCommand : CommandHandler {

    companion object {
        private const val TAG = "NotesCommand"
        private const val PREFS_NAME = "tui_notes_prefs"
        private const val KEY_NOTES = "saved_notes"
        private const val MAX_NOTE_LENGTH = 10000
    }

    data class Note(
        val id: String,
        val title: String,
        val content: String,
        val createdAt: Long,
        val updatedAt: Long,
        val tags: List<String>
    )

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getName(): String = "note"

    override fun getAliases(): List<String> = listOf("notes", "notepad", "memo", "jot")

    override fun getDescription(): String = "Quick note taking and management"

    override fun getUsage(): String = """
        ╔══════════════════════════════════════════════════════╗
        ║                   NOTES COMMANDS                      ║
        ╠══════════════════════════════════════════════════════╣
        ║  note create <title>         - Create new note       ║
        ║  note create <title> <text>  - Create with content   ║
        ║  note list                   - List all notes        ║
        ║  note show <id>              - Show note content     ║
        ║  note show <title>           - Show by title         ║
        ║  note edit <id> <text>       - Edit note content     ║
        ║  note delete <id>            - Delete a note         ║
        ║  note search <query>         - Search notes          ║
        ║  note tag <id> <tag>         - Add tag to note       ║
        ║  note export <id>            - Export note as text   ║
        ╚══════════════════════════════════════════════════════╝
    """.trimIndent()

    override fun execute(context: Context, args: List<String>): String {
        if (args.isEmpty()) {
            return listNotes()
        }

        val command = args[0].lowercase()
        val parameters = args.drop(1)

        return when (command) {
            "create", "new", "add" -> createNote(parameters)
            "list", "ls", "ls" -> listNotes()
            "show", "view", "get" -> showNote(parameters)
            "edit", "update", "modify" -> editNote(parameters)
            "delete", "del", "remove" -> deleteNote(parameters)
            "search", "find", "grep" -> searchNotes(parameters)
            "tag", "tags" -> tagNote(parameters)
            "export" -> exportNote(parameters)
            "clear" -> clearAllNotes()
            else -> {
                // Try to find note by title or ID
                showNote(listOf(command))
            }
        }
    }

    /**
     * Creates a new note
     */
    private fun createNote(args: List<String>): String {
        if (args.isEmpty()) {
            return "Usage: note create <title> [content]\nUse 'note create \"My Title\" This is content' for inline creation."
        }

        val title = args[0]
        val content = if (args.size > 1) args.drop(1).joinToString(" ") else ""

        return try {
            val note = Note(
                id = generateNoteId(),
                title = title,
                content = content,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                tags = extractTags(content)
            )

            saveNote(note)
            "Note created: [${note.id}] ${note.title}\n${if (content.isNotBlank()) "Content: ${content.take(50)}${if (content.length > 50) "..." else ""}" else "(Empty note - use 'note edit ${note.id}' to add content)"}"
        } catch (e: Exception) {
            "Error creating note: ${e.message}"
        }
    }

    /**
     * Lists all notes
     */
    private fun listNotes(): String {
        val notes = getAllNotes()
        if (notes.isEmpty()) {
            return buildString {
                appendLine()
                appendLine("No notes yet.")
                appendLine()
                appendLine("Create your first note:")
                appendLine("  note create \"My Title\" This is the content")
            }
        }

        val builder = StringBuilder()
        builder.appendLine()
        builder.appendLine("═══════════════════════════════════════════════════════════════")
        builder.appendLine("                         NOTES (${notes.size})                            ")
        builder.appendLine("═══════════════════════════════════════════════════════════════")

        notes.sortedByDescending { it.updatedAt }.forEachIndexed { index, note ->
            val preview = note.content.take(40).replace("\n", " ")
            val date = formatDate(note.updatedAt)
            builder.appendLine()
            builder.appendLine("  [${note.id}] ${note.title}")
            builder.appendLine("       $preview${if (note.content.length > 40) "..." else ""}")
            builder.appendLine("       Updated: $date")
            if (note.tags.isNotEmpty()) {
                builder.appendLine("       Tags: ${note.tags.joinToString(", ")}")
            }
        }

        return builder.toString()
    }

    /**
     * Shows a specific note
     */
    private fun showNote(args: List<String>): String {
        if (args.isEmpty()) {
            return "Usage: note show <id or title>"
        }

        val query = args.joinToString(" ")
        val note = findNote(query)

        if (note == null) {
            return "Note not found: $query\nUse 'note list' to see all notes."
        }

        return buildString {
            appendLine()
            appendLine("═══════════════════════════════════════════════════════════════")
            appendLine("                    NOTE: ${note.title}                       ")
            appendLine("═══════════════════════════════════════════════════════════════")
            appendLine()
            appendLine("ID: ${note.id}")
            appendLine("Created: ${formatDate(note.createdAt)}")
            appendLine("Updated: ${formatDate(note.updatedAt)}")
            if (note.tags.isNotEmpty()) {
                appendLine("Tags: ${note.tags.joinToString(", ")}")
            }
            appendLine()
            appendLine("─".repeat(60))
            appendLine()
            appendLine(note.content)
            appendLine()
            appendLine("─".repeat(60))
            appendLine()
            appendLine("Use 'note edit ${note.id}' to modify or 'note delete ${note.id}' to remove")
        }
    }

    /**
     * Edits a note's content
     */
    private fun editNote(args: List<String>): String {
        if (args.size < 2) {
            return "Usage: note edit <id> <new content>"
        }

        val id = args[0]
        val newContent = args.drop(1).joinToString(" ")

        val notes = getAllNotes().toMutableList()
        val index = notes.indexOfFirst { it.id == id || it.title.lowercase() == id.lowercase() }

        if (index == -1) {
            return "Note not found: $id"
        }

        val oldNote = notes[index]
        val updatedNote = oldNote.copy(
            content = newContent,
            updatedAt = System.currentTimeMillis(),
            tags = extractTags(newContent)
        )
        notes[index] = updatedNote

        saveAllNotes(notes)
        return "Note updated: [${updatedNote.id}] ${updatedNote.title}"
    }

    /**
     * Deletes a note
     */
    private fun deleteNote(args: List<String>): String {
        if (args.isEmpty()) {
            return "Usage: note delete <id>"
        }

        val id = args[0]
        val notes = getAllNotes().toMutableList()
        val index = notes.indexOfFirst { it.id == id || it.title.lowercase() == id.lowercase() }

        if (index == -1) {
            return "Note not found: $id"
        }

        val removed = notes.removeAt(index)
        saveAllNotes(notes)
        return "Note deleted: ${removed.title}"
    }

    /**
     * Searches notes
     */
    private fun searchNotes(args: List<String>): String {
        if (args.isEmpty()) {
            return "Usage: note search <query>"
        }

        val query = args.joinToString(" ").lowercase()
        val results = getAllNotes().filter {
            it.title.lowercase().contains(query) ||
            it.content.lowercase().contains(query) ||
            it.tags.any { tag -> tag.lowercase().contains(query) }
        }

        if (results.isEmpty()) {
            return "No notes found matching: $query"
        }

        val builder = StringBuilder()
        builder.appendLine()
        builder.appendLine("Search results for \"$query\" (${results.size} found):")
        builder.appendLine("─".repeat(60))

        results.forEach { note ->
            builder.appendLine("  [${note.id}] ${note.title}")
            builder.appendLine("       ${note.content.take(60).replace("\n", " ")}")
        }

        return builder.toString()
    }

    /**
     * Adds a tag to a note
     */
    private fun tagNote(args: List<String>): String {
        if (args.size < 2) {
            return "Usage: note tag <id> <tag>"
        }

        val id = args[0]
        val tag = args[1].lowercase()

        val notes = getAllNotes().toMutableList()
        val index = notes.indexOfFirst { it.id == id || it.title.lowercase() == id.lowercase() }

        if (index == -1) {
            return "Note not found: $id"
        }

        val note = notes[index]
        if (note.tags.contains(tag)) {
            return "Tag already exists: $tag"
        }

        notes[index] = note.copy(tags = note.tags + tag)
        saveAllNotes(notes)
        return "Tag added: $tag to note [${note.id}]"
    }

    /**
     * Exports a note
     */
    private fun exportNote(args: List<String>): String {
        if (args.isEmpty()) {
            return "Usage: note export <id>"
        }

        val id = args[0]
        val note = findNote(id)

        if (note == null) {
            return "Note not found: $id"
        }

        return buildString {
            appendLine("Title: ${note.title}")
            appendLine("Created: ${formatDate(note.createdAt)}")
            appendLine("Updated: ${formatDate(note.updatedAt)}")
            if (note.tags.isNotEmpty()) {
                appendLine("Tags: ${note.tags.joinToString(", ")}")
            }
            appendLine()
            appendLine(note.content)
        }
    }

    /**
     * Clears all notes
     */
    private fun clearAllNotes(): String {
        saveAllNotes(emptyList())
        return "All notes cleared."
    }

    private fun getAllNotes(): List<Note> {
        val json = prefs.getString(KEY_NOTES, "[]") ?: "[]"
        return parseNotesJson(json)
    }

    private fun findNote(query: String): Note? {
        return getAllNotes().find {
            it.id == query || it.title.lowercase() == query.lowercase()
        }
    }

    private fun saveNote(note: Note) {
        val notes = getAllNotes().toMutableList()
        notes.add(note)
        saveAllNotes(notes)
    }

    private fun saveAllNotes(notes: List<Note>) {
        val json = notesToJson(notes)
        prefs.edit().putString(KEY_NOTES, json).apply()
    }

    private fun generateNoteId(): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    private fun extractTags(content: String): List<String> {
        val tagPattern = Regex("#(\\w+)")
        return tagPattern.findAll(content).map { it.groupValues[1] }.toList()
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun parseNotesJson(json: String): List<Note> {
        if (json == "[]" || json.isBlank()) return emptyList()

        return try {
            val cleanJson = json.trim().removeSurrounding("[", "]")
            if (cleanJson.isBlank()) return emptyList()

            cleanJson.split("},{").map { segment ->
                val clean = segment.removeSurrounding("{", "}")
                val parts = clean.split(",(?=\")".toRegex())

                val id = parts.find { it.contains("id") }?.substringAfter(":")?.trim()?.removeSurrounding("\"") ?: ""
                val title = parts.find { it.contains("title") }?.substringAfter(":")?.trim()?.removeSurrounding("\"") ?: ""
                val content = parts.find { it.contains("content") }?.substringAfter(":")?.trim()?.removeSurrounding("\"") ?: ""
                val created = parts.find { it.contains("created") }?.substringAfter(":")?.trim()?.toLongOrNull() ?: 0L
                val updated = parts.find { it.contains("updated") }?.substringAfter(":")?.trim()?.toLongOrNull() ?: 0L

                Note(id, title, content, created, updated, emptyList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing notes: ${e.message}")
            emptyList()
        }
    }

    private fun notesToJson(notes: List<Note>): String {
        return notes.joinToString(",", "[", "]") { note ->
            buildString {
                append("{")
                append("\"id\":\"${note.id}\",")
                append("\"title\":\"${escape(note.title)}\",")
                append("\"content\":\"${escape(note.content)}\",")
                append("\"created\":${note.createdAt},")
                append("\"updated\":${note.updatedAt},")
                append("\"tags\":[${note.tags.map { "\"$it\"" }.joinToString(",")}]")
                append("}")
            }
        }
    }

    private fun escape(input: String): String {
        return input.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
