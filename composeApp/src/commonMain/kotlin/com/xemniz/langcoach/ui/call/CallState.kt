package com.xemniz.langcoach.ui.call

enum class ChatRole { User, Assistant }

data class ChatMessage(
    val role: ChatRole,
    val text: String,
    val realtimeItemId: String? = null,
)

sealed interface CallState {
    data object Idle : CallState
    data object PermissionRequired : CallState
    data object Connecting : CallState
    data class Live(
        val sessionId: Long,
        val messages: List<ChatMessage>,
        val isMuted: Boolean,
        val isAssistantSpeaking: Boolean,
        val isReconnecting: Boolean,
    ) : CallState
    data class Failed(val message: String) : CallState
    data object ProcessingSummary : CallState
    data class Ended(
        val summary: String? = null,
        val strength: String? = null,
        val nextStep: String? = null,
        val assignment: String? = null,
        val processingError: String? = null,
    ) : CallState
}

sealed interface CallIntent {
    data object Start : CallIntent
    data object PermissionGranted : CallIntent
    data object PermissionDenied : CallIntent
    data object ToggleMute : CallIntent
    data object End : CallIntent
}
