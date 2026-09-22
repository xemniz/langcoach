package com.xemniz.langcoach.ui.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xemniz.langcoach.core.AppResult
import com.xemniz.langcoach.di.AppScope
import com.xemniz.langcoach.data.repo.SessionRepo
import com.xemniz.langcoach.domain.reflection.SessionTranscript
import com.xemniz.langcoach.domain.reflection.TranscriptSpeaker
import com.xemniz.langcoach.domain.reflection.TranscriptTurn
import com.xemniz.langcoach.domain.session.SessionOrchestrator
import com.xemniz.langcoach.domain.usecase.FinishSession
import com.xemniz.langcoach.domain.usecase.ReflectSession
import com.xemniz.langcoach.domain.usecase.StartSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CallViewModel internal constructor(
    private val orchestrator: SessionOrchestrator,
    private val coordinator: RealtimeCallCoordinator,
    private val startSession: StartSession,
    private val finishSession: FinishSession,
    private val reflectSession: ReflectSession,
    private val appScope: AppScope,
    private val callSessionLifecycle: CallSessionLifecycle,
    private val sessions: SessionRepo,
) : ViewModel() {
    private val _state = MutableStateFlow<CallState>(CallState.Idle)
    val state: StateFlow<CallState> = _state.asStateFlow()

    private var sessionId: Long? = null
    private var ending = false

    init {
        viewModelScope.launch {
            coordinator.events.collect(::handleCoordinatorEvent)
        }
        viewModelScope.launch {
            coordinator.state.collect(::handleCoordinatorState)
        }
    }

    fun onIntent(intent: CallIntent) {
        when (intent) {
            CallIntent.Start -> connect()
            CallIntent.PermissionGranted -> connect()
            CallIntent.PermissionDenied -> _state.value = CallState.PermissionRequired
            CallIntent.ToggleMute -> toggleMute()
            CallIntent.End -> end()
        }
    }

    private fun connect() {
        if (_state.value is CallState.Live || _state.value is CallState.Connecting) return
        sessionId = null
        ending = false
        _state.value = CallState.Connecting
        callSessionLifecycle.start()
        viewModelScope.launch {
            val preparedSession = orchestrator.prepareSession()
            val result = coordinator.start(
                systemPrompt = preparedSession.systemPrompt,
                transcriptionLanguage = preparedSession.transcriptionLanguage,
            )
            when (result) {
                is AppResult.Failure -> failCall(result.error.message)
                is AppResult.Success -> {
                    val newId = runCatching {
                        startSession(preparedSession)
                    }.getOrElse { error ->
                        coordinator.stop()
                        failCall(error.message ?: "Could not create the practice session")
                        return@launch
                    }
                    sessionId = newId
                    val snapshot = coordinator.state.value
                    _state.value = CallState.Live(
                        sessionId = newId,
                        messages = emptyList(),
                        isMuted = snapshot.isMuted,
                        isAssistantSpeaking = snapshot.turn.isAssistantSpeaking,
                        isReconnecting = snapshot.connection ==
                            RealtimeConnectionState.Reconnecting,
                    )
                }
            }
        }
    }

    private fun handleCoordinatorEvent(event: RealtimeCallEvent) {
        when (event) {
            is RealtimeCallEvent.TranscriptDelta -> appendFragment(
                role = if (event.isUser) ChatRole.User else ChatRole.Assistant,
                fragment = event.text,
                realtimeItemId = event.itemId,
            )
            is RealtimeCallEvent.TranscriptCompleted -> replaceLastFragment(
                role = if (event.isUser) ChatRole.User else ChatRole.Assistant,
                fullText = event.text,
                realtimeItemId = event.itemId,
            )
        }
    }

    private fun handleCoordinatorState(snapshot: RealtimeCallSnapshot) {
        val failure = snapshot.turn as? VoiceTurnState.Failed
        if (failure != null) {
            failCall(failure.message)
            return
        }
        _state.update { current ->
            if (current !is CallState.Live) return@update current
            current.copy(
                isMuted = snapshot.isMuted,
                isAssistantSpeaking = snapshot.turn.isAssistantSpeaking,
                isReconnecting = snapshot.connection ==
                    RealtimeConnectionState.Reconnecting,
            )
        }
    }

    private fun appendFragment(role: ChatRole, fragment: String, realtimeItemId: String?) {
        _state.update { current ->
            if (current !is CallState.Live) return@update current
            current.copy(
                messages = appendTranscript(current.messages, role, fragment, realtimeItemId),
            )
        }
    }

    private fun replaceLastFragment(role: ChatRole, fullText: String, realtimeItemId: String?) {
        _state.update { current ->
            if (current !is CallState.Live) return@update current
            current.copy(
                messages = completeTranscript(current.messages, role, fullText, realtimeItemId),
            )
        }
    }

    private fun toggleMute() {
        val current = _state.value as? CallState.Live ?: return
        coordinator.setMuted(!current.isMuted)
    }

    private fun failCall(message: String) {
        if (_state.value is CallState.Failed || _state.value is CallState.Ended) return
        val interrupted = _state.value as? CallState.Live
        val interruptedSessionId = sessionId
        _state.value = CallState.Failed(message)
        viewModelScope.launch {
            val usage = runCatching { coordinator.stop() }.getOrNull()
            runCatching { callSessionLifecycle.stop() }
            if (interrupted != null && interruptedSessionId != null && usage != null) {
                val transcript = renderTranscript(interrupted.messages)
                runCatching {
                    finishSession(
                        sessionId = interruptedSessionId,
                        tokensIn = usage.tokensIn,
                        tokensOut = usage.tokensOut,
                        transcript = transcript,
                    )
                    appScope.scope.launch { reflectSession(interruptedSessionId) }
                }
            }
        }
    }

    private fun end() {
        if (ending) return
        ending = true
        viewModelScope.launch {
            val usage = coordinator.stop()
            callSessionLifecycle.stop()
            val current = _state.value
            val transcript = if (current is CallState.Live) {
                renderTranscript(current.messages)
            } else {
                SessionTranscript(emptyList())
            }
            val id = sessionId
            if (id != null) {
                finishSession(
                    sessionId = id,
                    tokensIn = usage.tokensIn,
                    tokensOut = usage.tokensOut,
                    transcript = transcript,
                )
                _state.value = CallState.ProcessingSummary
                appScope.scope.launch {
                    val result = reflectSession(sessionId = id)
                    val lesson = sessions.byId(id)
                    _state.value = CallState.Ended(
                        summary = lesson?.summary,
                        strength = lesson?.strength,
                        nextStep = lesson?.nextStep,
                        assignment = lesson?.assignment,
                        processingError = (result as? AppResult.Failure)?.error?.message
                            ?: lesson?.processingError,
                    )
                }
            } else {
                _state.value = CallState.Ended()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        appScope.scope.launch {
            runCatching { coordinator.stop() }
        }
        runCatching { callSessionLifecycle.stop() }
    }

    private fun renderTranscript(messages: List<ChatMessage>) = SessionTranscript(
        turns = messages
            .filter { it.text.isNotBlank() }
            .mapIndexed { index, message ->
                TranscriptTurn(
                    id = "turn-${index + 1}",
                    speaker = when (message.role) {
                        ChatRole.User -> TranscriptSpeaker.Learner
                        ChatRole.Assistant -> TranscriptSpeaker.Tutor
                    },
                    text = message.text.trim(),
                )
            },
    )
}
