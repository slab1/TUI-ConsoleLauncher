package ohi.andre.consolelauncher.commands.smartlauncher.productivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;

/**
 * Notes Command - Quick note taking and retrieval
 * Supports creating, listing, searching, and managing notes
 */
public class NotesCommand implements CommandAbstraction {

    private static final String TAG = "NotesCommand";
    private static final String PREFS_NAME = "tui_notes_prefs";
    private static final String KEY_NOTES = "saved_notes";
    private static final int MAX_NOTE_LENGTH = 10000;

    /**
     * Note data class equivalent
     */
    static class Note {
        String id;
        String title;
        String content;
        long createdAt;
        long updatedAt;
        ArrayList<String> tags;

        Note(String id, String title, String content, long createdAt, long updatedAt, ArrayList<String> tags) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.tags = tags;
        }
    }

    @Override
    public String exec(ExecutePack pack) throws Exception {
        Context context = pack.context;
        
        ArrayList<String> argsList = new ArrayList<>();
        for (Object arg : pack.args) {
            if (arg != null) {
                argsList.add(arg.toString());
            }
        }

        if (argsList.isEmpty()) {
            return listNotes(context);
        }

        String command = argsList.get(0).toLowerCase();
        ArrayList<String> parameters = new ArrayList<>(argsList.subList(1, argsList.size()));

        switch (command) {
            case "create":
            case "new":
            case "add":
                return createNote(context, parameters);
            case "list":
            case "ls":
                return listNotes(context);
            case "show":
            case "view":
            case "get":
                return showNote(context, parameters);
            case "edit":
            case "update":
            case "modify":
                return editNote(context, parameters);
            case "delete":
            case "del":
            case "remove":
                return deleteNote(context, parameters);
            case "search":
            case "find":
            case "grep":
                return searchNotes(context, parameters);
            case "tag":
            case "tags":
                return tagNote(context, parameters);
            case "export":
                return exportNote(context, parameters);
            case "clear":
                return clearAllNotes(context);
            default:
                // Try to find note by title or ID
                return showNote(context, argsList);
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
               "║                   NOTES COMMANDS                      ║\n" +
               "╠══════════════════════════════════════════════════════╣\n" +
               "║  note create <title>         - Create new note       ║\n" +
               "║  note create <title> <text>  - Create with content   ║\n" +
               "║  note list                   - List all notes        ║\n" +
               "║  note show <id>              - Show note content     ║\n" +
               "║  note show <title>           - Show by title         ║\n" +
               "║  note edit <id> <text>       - Edit note content     ║\n" +
               "║  note delete <id>            - Delete a note         ║\n" +
               "║  note search <query>         - Search notes          ║\n" +
               "║  note tag <id> <tag>         - Add tag to note       ║\n" +
               "║  note export <id>            - Export note as text   ║\n" +
               "╚══════════════════════════════════════════════════════╝\n";
    }

    /**
     * Creates a new note
     */
    private String createNote(Context context, ArrayList<String> args) {
        if (args.isEmpty()) {
            return "Usage: note create <title> [content]\nUse 'note create \"My Title\" This is content' for inline creation";
        }

        String title = args.get(0);
        String content = "";
        if (args.size() > 1) {
            StringBuilder contentBuilder = new StringBuilder();
            for (int i = 1; i < args.size(); i++) {
                if (i > 1) contentBuilder.append(" ");
                contentBuilder.append(args.get(i));
            }
            content = contentBuilder.toString();
        }

        try {
            long now = System.currentTimeMillis();
            Note note = new Note(
                generateNoteId(),
                title,
                content,
                now,
                now,
                extractTags(content)
            );

            saveNote(context, note);
            
            String preview = content.length() > 50 ? content.substring(0, 50) + "..." : content;
            String contentInfo = !content.trim().isEmpty() ? 
                "Content: " + preview : "(Empty note - use 'note edit " + note.id + "' to add content)";
            
            return "Note created: [" + note.id + "] " + note.title + "\n" + contentInfo;
        } catch (Exception e) {
            Log.e(TAG, "Error creating note", e);
            return "Error creating note: " + e.getMessage();
        }
    }

    /**
     * Lists all notes
     */
    private String listNotes(Context context) {
        ArrayList<Note> notes = getAllNotes(context);
        if (notes.isEmpty()) {
            return "\nNo notes yet.\n\nCreate your first note:\n  note create \"My Title\" This is the content";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("\n");
        builder.append("═══════════════════════════════════════════════════════════════\n");
        builder.append("                         NOTES (").append(notes.size()).append(")                            \n");
        builder.append("═══════════════════════════════════════════════════════════════\n");

        // Sort by updated date (newest first)
        notes.sort((n1, n2) -> Long.compare(n2.updatedAt, n1.updatedAt));

        for (int index = 0; index < notes.size(); index++) {
            Note note = notes.get(index);
            String preview = note.content.replace("\n", " ");
            if (preview.length() > 40) {
                preview = preview.substring(0, 40) + "...";
            }
            String date = formatDate(note.updatedAt);
            
            builder.append("\n");
            builder.append("  [").append(note.id).append("] ").append(note.title).append("\n");
            builder.append("       ").append(preview).append("\n");
            builder.append("       Updated: ").append(date).append("\n");
            if (!note.tags.isEmpty()) {
                builder.append("       Tags: ").append(listToString(note.tags)).append("\n");
            }
        }

        return builder.toString();
    }

    /**
     * Shows a specific note
     */
    private String showNote(Context context, ArrayList<String> args) {
        if (args.isEmpty()) {
            return "Usage: note show <id or title>";
        }

        String query = listToString(args);
        Note note = findNote(context, query);

        if (note == null) {
            return "Note not found: " + query + "\nUse 'note list' to see all notes.";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("\n");
        builder.append("═══════════════════════════════════════════════════════════════\n");
        builder.append("                    NOTE: ").append(note.title).append("                       \n");
        builder.append("═══════════════════════════════════════════════════════════════\n");
        builder.append("\n");
        builder.append("ID: ").append(note.id).append("\n");
        builder.append("Created: ").append(formatDate(note.createdAt)).append("\n");
        builder.append("Updated: ").append(formatDate(note.updatedAt)).append("\n");
        if (!note.tags.isEmpty()) {
            builder.append("Tags: ").append(listToString(note.tags)).append("\n");
        }
        builder.append("\n");
        builder.append("─".repeat(60)).append("\n");
        builder.append("\n");
        builder.append(note.content).append("\n");
        builder.append("\n");
        builder.append("─".repeat(60)).append("\n");
        builder.append("\n");
        builder.append("Use 'note edit ").append(note.id).append("' to modify or 'note delete ").append(note.id).append("' to remove");

        return builder.toString();
    }

    /**
     * Edits a note's content
     */
    private String editNote(Context context, ArrayList<String> args) {
        if (args.size() < 2) {
            return "Usage: note edit <id> <new content>";
        }

        String id = args.get(0);
        StringBuilder contentBuilder = new StringBuilder();
        for (int i = 1; i < args.size(); i++) {
            if (i > 1) contentBuilder.append(" ");
            contentBuilder.append(args.get(i));
        }
        String newContent = contentBuilder.toString();

        ArrayList<Note> notes = getAllNotes(context);
        int index = -1;
        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);
            if (note.id.equals(id) || note.title.toLowerCase().equals(id.toLowerCase())) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            return "Note not found: " + id;
        }

        Note oldNote = notes.get(index);
        ArrayList<String> newTags = extractTags(newContent);
        Note updatedNote = new Note(
            oldNote.id,
            oldNote.title,
            newContent,
            oldNote.createdAt,
            System.currentTimeMillis(),
            newTags
        );
        notes.set(index, updatedNote);

        saveAllNotes(context, notes);
        return "Note updated: [" + updatedNote.id + "] " + updatedNote.title;
    }

    /**
     * Deletes a note
     */
    private String deleteNote(Context context, ArrayList<String> args) {
        if (args.isEmpty()) {
            return "Usage: note delete <id>";
        }

        String id = args.get(0);
        ArrayList<Note> notes = getAllNotes(context);
        int index = -1;
        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);
            if (note.id.equals(id) || note.title.toLowerCase().equals(id.toLowerCase())) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            return "Note not found: " + id;
        }

        Note removed = notes.remove(index);
        saveAllNotes(context, notes);
        return "Note deleted: " + removed.title;
    }

    /**
     * Searches notes
     */
    private String searchNotes(Context context, ArrayList<String> args) {
        if (args.isEmpty()) {
            return "Usage: note search <query>";
        }

        String query = listToString(args).toLowerCase();
        ArrayList<Note> allNotes = getAllNotes(context);
        ArrayList<Note> results = new ArrayList<>();

        for (Note note : allNotes) {
            if (note.title.toLowerCase().contains(query) ||
                note.content.toLowerCase().contains(query) ||
                containsTag(note.tags, query)) {
                results.add(note);
            }
        }

        if (results.isEmpty()) {
            return "No notes found matching: " + query;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("\n");
        builder.append("Search results for \"").append(query).append("\" (").append(results.size()).append(" found):\n");
        builder.append("─".repeat(60)).append("\n");

        for (Note note : results) {
            String preview = note.content.replace("\n", " ");
            if (preview.length() > 60) {
                preview = preview.substring(0, 60);
            }
            builder.append("  [").append(note.id).append("] ").append(note.title).append("\n");
            builder.append("       ").append(preview).append("\n");
        }

        return builder.toString();
    }

    private boolean containsTag(ArrayList<String> tags, String query) {
        for (String tag : tags) {
            if (tag.toLowerCase().contains(query)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a tag to a note
     */
    private String tagNote(Context context, ArrayList<String> args) {
        if (args.size() < 2) {
            return "Usage: note tag <id> <tag>";
        }

        String id = args.get(0);
        String tag = args.get(1).toLowerCase();

        ArrayList<Note> notes = getAllNotes(context);
        int index = -1;
        for (int i = 0; i < notes.size(); i++) {
            Note note = notes.get(i);
            if (note.id.equals(id) || note.title.toLowerCase().equals(id.toLowerCase())) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            return "Note not found: " + id;
        }

        Note note = notes.get(index);
        if (note.tags.contains(tag)) {
            return "Tag already exists: " + tag;
        }

        note.tags.add(tag);
        saveAllNotes(context, notes);
        return "Tag added: " + tag + " to note [" + note.id + "]";
    }

    /**
     * Exports a note
     */
    private String exportNote(Context context, ArrayList<String> args) {
        if (args.isEmpty()) {
            return "Usage: note export <id>";
        }

        String id = args.get(0);
        Note note = findNote(context, id);

        if (note == null) {
            return "Note not found: " + id;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Title: ").append(note.title).append("\n");
        builder.append("Created: ").append(formatDate(note.createdAt)).append("\n");
        builder.append("Updated: ").append(formatDate(note.updatedAt)).append("\n");
        if (!note.tags.isEmpty()) {
            builder.append("Tags: ").append(listToString(note.tags)).append("\n");
        }
        builder.append("\n");
        builder.append(note.content).append("\n");

        return builder.toString();
    }

    /**
     * Clears all notes
     */
    private String clearAllNotes(Context context) {
        saveAllNotes(context, new ArrayList<>());
        return "All notes cleared.";
    }

    private ArrayList<Note> getAllNotes(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_NOTES, "[]");
        return parseNotesJson(json != null ? json : "[]");
    }

    private Note findNote(Context context, String query) {
        ArrayList<Note> notes = getAllNotes(context);
        for (Note note : notes) {
            if (note.id.equals(query) || note.title.toLowerCase().equals(query.toLowerCase())) {
                return note;
            }
        }
        return null;
    }

    private void saveNote(Context context, Note note) {
        ArrayList<Note> notes = getAllNotes(context);
        notes.add(note);
        saveAllNotes(context, notes);
    }

    private void saveAllNotes(Context context, ArrayList<Note> notes) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = notesToJson(notes);
        prefs.edit().putString(KEY_NOTES, json).apply();
    }

    private String generateNoteId() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder id = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int index = (int) (Math.random() * chars.length());
            id.append(chars.charAt(index));
        }
        return id.toString();
    }

    private ArrayList<String> extractTags(String content) {
        ArrayList<String> tags = new ArrayList<>();
        Pattern pattern = Pattern.compile("#(\\w+)");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            tags.add(matcher.group(1));
        }
        return tags;
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
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

    private ArrayList<Note> parseNotesJson(String json) {
        if (json.equals("[]") || json.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            ArrayList<Note> notes = new ArrayList<>();
            String cleanJson = json.trim();
            if (cleanJson.startsWith("[")) cleanJson = cleanJson.substring(1);
            if (cleanJson.endsWith("]")) cleanJson = cleanJson.substring(0, cleanJson.length() - 1);
            
            if (cleanJson.trim().isEmpty()) return notes;

            String[] segments = cleanJson.split("\\},\\{");
            for (String segment : segments) {
                String clean = segment.trim();
                if (clean.startsWith("{")) clean = clean.substring(1);
                if (clean.endsWith("}")) clean = clean.substring(0, clean.length() - 1);
                
                String[] parts = clean.split(",(?=\")");
                
                String id = "";
                String title = "";
                String content = "";
                long created = 0L;
                long updated = 0L;
                ArrayList<String> tags = new ArrayList<>();

                for (String part : parts) {
                    String trimmed = part.trim();
                    if (trimmed.startsWith("\"id\":")) {
                        id = trimmed.substring(4).trim().replace("\"", "");
                    } else if (trimmed.startsWith("\"title\":")) {
                        title = trimmed.substring(7).trim().replace("\"", "");
                    } else if (trimmed.startsWith("\"content\":")) {
                        content = trimmed.substring(9).trim().replace("\"", "");
                    } else if (trimmed.startsWith("\"created\":")) {
                        try {
                            created = Long.parseLong(trimmed.substring(9).trim());
                        } catch (NumberFormatException e) {
                            // Use default
                        }
                    } else if (trimmed.startsWith("\"updated\":")) {
                        try {
                            updated = Long.parseLong(trimmed.substring(9).trim());
                        } catch (NumberFormatException e) {
                            // Use default
                        }
                    } else if (trimmed.startsWith("\"tags\":")) {
                        String tagsStr = trimmed.substring(7).trim();
                        if (tagsStr.startsWith("[")) tagsStr = tagsStr.substring(1);
                        if (tagsStr.endsWith("]")) tagsStr = tagsStr.substring(0, tagsStr.length() - 1);
                        if (!tagsStr.trim().isEmpty()) {
                            String[] tagArray = tagsStr.split(",");
                            for (String tag : tagArray) {
                                tags.add(tag.trim().replace("\"", ""));
                            }
                        }
                    }
                }

                notes.add(new Note(id, title, content, created, updated, tags));
            }

            return notes;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing notes: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private String notesToJson(ArrayList<Note> notes) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < notes.size(); i++) {
            if (i > 0) json.append(",");
            Note note = notes.get(i);
            json.append("{");
            json.append("\"id\":\"").append(note.id).append("\",");
            json.append("\"title\":\"").append(escape(note.title)).append("\",");
            json.append("\"content\":\"").append(escape(note.content)).append("\",");
            json.append("\"created\":").append(note.createdAt).append(",");
            json.append("\"updated\":").append(note.updatedAt).append(",");
            json.append("\"tags\":[");
            for (int j = 0; j < note.tags.size(); j++) {
                if (j > 0) json.append(",");
                json.append("\"").append(note.tags.get(j)).append("\"");
            }
            json.append("]");
            json.append("}");
        }
        json.append("]");
        return json.toString();
    }

    private String escape(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }
}