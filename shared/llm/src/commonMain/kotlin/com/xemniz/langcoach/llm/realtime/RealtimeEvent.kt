package com.xemniz.langcoach.llm.realtime

enum class RealtimeResponseStatus {
    Completed,
    Cancelled,
    Failed,
    Incomplete,
    InProgress,
    Unknown;

    internal companion object {
        fun fromWire(value: String?): RealtimeResponseStatus = when (value) {
            "completed" -> Completed
            "cancelled" -> Cancelled
            "failed" -> Failed
            "incomplete" -> Incomplete
            "in_progress" -> InProgress
            else -> Unknown
        }
    }
}

sealed interface RealtimeEvent {
    object SessionCreated : RealtimeEvent

    /** A response has started, so microphone audio must not interrupt local playback. */
    data class ResponseStarted(val responseId: String?) : RealtimeEvent

    /** PCM16 LE 24 kHz mono audio chunk from the assistant. */
    data class AudioDelta(
        val pcm16Le: ByteArray,
        val responseId: String?,
    ) : RealtimeEvent

    /** Live text transcript fragment. isUser=true for user STT, false for assistant. */
    data class TranscriptDelta(
        val text: String,
        val isUser: Boolean,
        val itemId: String? = null,
    ) : RealtimeEvent

    /** Final, full transcript for a turn. Replaces deltas belonging to the same Realtime item. */
    data class TranscriptCompleted(
        val text: String,
        val isUser: Boolean,
        val itemId: String? = null,
    ) : RealtimeEvent

    data class ResponseDone(
        val tokensIn: Int,
        val tokensOut: Int,
        val status: RealtimeResponseStatus,
        val reason: String? = null,
        val responseId: String? = null,
    ) : RealtimeEvent

    data class ErrorEvent(val message: String) : RealtimeEvent

    data class Reconnecting(val attempt: Int) : RealtimeEvent

    object Reconnected : RealtimeEvent
}
