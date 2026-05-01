package com.xemniz.langcoach.llm.realtime

sealed interface RealtimeEvent {
    object SessionCreated : RealtimeEvent

    /** PCM16 LE 24 kHz mono audio chunk from the assistant. */
    data class AudioDelta(val pcm16Le: ByteArray) : RealtimeEvent

    /** Live text transcript fragment. isUser=true for user STT, false for assistant. */
    data class TranscriptDelta(val text: String, val isUser: Boolean) : RealtimeEvent

    /** Final, full transcript for a turn. Replaces any accumulated deltas for that role. */
    data class TranscriptCompleted(val text: String, val isUser: Boolean) : RealtimeEvent

    data class ResponseDone(val tokensIn: Int, val tokensOut: Int) : RealtimeEvent

    data class ErrorEvent(val message: String) : RealtimeEvent

    data class Reconnecting(val attempt: Int) : RealtimeEvent

    object Reconnected : RealtimeEvent
}
