package tui.smartlauncher.ai

/**
 * Events emitted by [MiniMaxService.smartQuery] during streaming AI response processing.
 *
 * Collect these events from the returned [kotlinx.coroutines.flow.Flow] to render
 * live streaming output, display errors, show command suggestions, and surface
 * contextual metadata.
 */
sealed class AIEvent {

    /** The AI model has started processing — use to show a "Thinking..." indicator. */
    data object Thinking : AIEvent()

    /** A single chunk of streaming text from the incremental response. */
    data class Chunk(val text: String) : AIEvent()

    /** The full response has been assembled and is ready for display. */
    data class Complete(val fullResponse: String) : AIEvent()

    /** A non-recoverable error occurred while processing the query. */
    data class Error(val message: String) : AIEvent()

    /** The AI suggested a shell/CLI command the user might want to execute. */
    data class CommandSuggestion(val command: String, val explanation: String) : AIEvent()

    /** Contextual metadata (e.g. model name, query type, token usage). */
    data class ContextInfo(val info: String) : AIEvent()
}
