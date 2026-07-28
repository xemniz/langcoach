package com.xemniz.langcoach.ui.call

import com.xemniz.langcoach.llm.realtime.RealtimeResponseStatus

internal sealed interface VoiceTurnState {
    data object Stopped : VoiceTurnState
    data object Listening : VoiceTurnState
    data class Responding(val responseId: String?) : VoiceTurnState
    data class Draining(val responseId: String?) : VoiceTurnState
    data class Failed(val message: String) : VoiceTurnState
}

internal sealed interface VoiceTurnEvent {
    data object Connected : VoiceTurnEvent
    data class ResponseStarted(val responseId: String?) : VoiceTurnEvent
    data class AudioReceived(val responseId: String?) : VoiceTurnEvent
    data class ResponseFinished(
        val responseId: String?,
        val status: RealtimeResponseStatus,
        val reason: String?,
    ) : VoiceTurnEvent
    data object TransportInterrupted : VoiceTurnEvent
    data class PlaybackDrained(val responseId: String?) : VoiceTurnEvent
    data class PlaybackFailed(val message: String) : VoiceTurnEvent
    data class SessionFailed(val message: String) : VoiceTurnEvent
    data object Stop : VoiceTurnEvent
}

internal sealed interface VoiceTurnEffect {
    data class DrainPlayback(val responseId: String?) : VoiceTurnEffect
}

internal data class VoiceTurnTransition(
    val state: VoiceTurnState,
    val effects: List<VoiceTurnEffect> = emptyList(),
)

internal val VoiceTurnState.acceptsMicrophone: Boolean
    get() = this is VoiceTurnState.Listening

internal val VoiceTurnState.isAssistantSpeaking: Boolean
    get() = this is VoiceTurnState.Responding || this is VoiceTurnState.Draining

internal fun reduceVoiceTurn(
    current: VoiceTurnState,
    event: VoiceTurnEvent,
): VoiceTurnTransition = when (event) {
    VoiceTurnEvent.Connected -> VoiceTurnTransition(VoiceTurnState.Listening)
    VoiceTurnEvent.Stop -> VoiceTurnTransition(VoiceTurnState.Stopped)
    is VoiceTurnEvent.SessionFailed -> VoiceTurnTransition(VoiceTurnState.Failed(event.message))
    is VoiceTurnEvent.PlaybackFailed -> VoiceTurnTransition(VoiceTurnState.Failed(event.message))

    is VoiceTurnEvent.ResponseStarted ->
        VoiceTurnTransition(VoiceTurnState.Responding(event.responseId))

    is VoiceTurnEvent.AudioReceived -> {
        val responding = current as? VoiceTurnState.Responding
        if (responding != null && responseIdsMatch(responding.responseId, event.responseId)) {
            VoiceTurnTransition(current)
        } else {
            VoiceTurnTransition(VoiceTurnState.Responding(event.responseId))
        }
    }

    is VoiceTurnEvent.ResponseFinished -> {
        if (!belongsToCurrentResponse(current, event.responseId)) {
            VoiceTurnTransition(current)
        } else {
            when (event.status) {
                RealtimeResponseStatus.Completed,
                RealtimeResponseStatus.Cancelled,
                RealtimeResponseStatus.Unknown,
                -> {
                    val responseId = event.responseId ?: current.responseIdOrNull()
                    VoiceTurnTransition(
                        state = VoiceTurnState.Draining(responseId),
                        effects = listOf(VoiceTurnEffect.DrainPlayback(responseId)),
                    )
                }
                RealtimeResponseStatus.Failed,
                RealtimeResponseStatus.Incomplete,
                RealtimeResponseStatus.InProgress,
                -> VoiceTurnTransition(
                    VoiceTurnState.Failed(
                        event.reason
                            ?: "The coach response ended with status ${event.status.name.lowercase()}",
                    ),
                )
            }
        }
    }

    VoiceTurnEvent.TransportInterrupted -> when (current) {
        is VoiceTurnState.Responding -> VoiceTurnTransition(
            state = VoiceTurnState.Draining(current.responseId),
            effects = listOf(VoiceTurnEffect.DrainPlayback(current.responseId)),
        )
        else -> VoiceTurnTransition(current)
    }

    is VoiceTurnEvent.PlaybackDrained -> {
        val draining = current as? VoiceTurnState.Draining
        if (draining != null && responseIdsMatch(draining.responseId, event.responseId)) {
            VoiceTurnTransition(VoiceTurnState.Listening)
        } else {
            VoiceTurnTransition(current)
        }
    }
}

private fun belongsToCurrentResponse(state: VoiceTurnState, responseId: String?): Boolean =
    when (state) {
        is VoiceTurnState.Responding -> responseIdsMatch(state.responseId, responseId)
        is VoiceTurnState.Draining -> responseIdsMatch(state.responseId, responseId)
        VoiceTurnState.Listening -> true
        else -> false
    }

private fun VoiceTurnState.responseIdOrNull(): String? = when (this) {
    is VoiceTurnState.Responding -> responseId
    is VoiceTurnState.Draining -> responseId
    else -> null
}

private fun responseIdsMatch(left: String?, right: String?): Boolean =
    left == null || right == null || left == right
