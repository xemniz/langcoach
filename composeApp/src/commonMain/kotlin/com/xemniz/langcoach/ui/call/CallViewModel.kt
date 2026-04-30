package com.xemniz.langcoach.ui.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xemniz.langcoach.core.AppResult
import com.xemniz.langcoach.di.AppScope
import com.xemniz.langcoach.domain.session.SessionOrchestrator
import com.xemniz.langcoach.domain.usecase.FinishSession
import com.xemniz.langcoach.domain.usecase.ReflectSession
import com.xemniz.langcoach.domain.usecase.StartSession
import com.xemniz.langcoach.llm.realtime.OpenAIRealtimeClient
import com.xemniz.langcoach.llm.realtime.RealtimeEvent
import com.xemniz.langcoach.llm.realtime.RealtimeSession
import com.xemniz.langcoach.voice.AudioCapture
import com.xemniz.langcoach.voice.AudioPlayback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CallViewModel(
    private val orchestrator: SessionOrchestrator,
    private val realtimeClient: OpenAIRealtimeClient,
    private val startSession: StartSession,
    private val finishSession: FinishSession,
    private val reflectSession: ReflectSession,
    private val audioCapture: AudioCapture,
    private val audioPlayback: AudioPlayback,
    private val appScope: AppScope,
    private val callSessionLifecycle: CallSessionLifecycle,
) : ViewModel() {
    private val _state = MutableStateFlow<CallState>(CallState.Idle)
    val state: StateFlow<CallState> = _state.asStateFlow()

    private var session: RealtimeSession? = null
    private var sessionId: Long? = null
    private var captureJob: Job? = null
    private var eventsJob: Job? = null
    private var tokensIn: Int = 0
    private var tokensOut: Int = 0

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
        _state.value = CallState.Connecting
        callSessionLifecycle.start()
        viewModelScope.launch {
            val prompt = orchestrator.buildSystemPrompt()
            val newId = startSession()
            sessionId = newId
            when (val r = realtimeClient.connect()) {
                is AppResult.Failure -> {
                    _state.value = CallState.Failed(r.error.message)
                }
                is AppResult.Success -> {
                    session = r.value
                    r.value.configure(systemPrompt = prompt)
                    audioPlayback.start()
                    _state.value = CallState.Live(
                        sessionId = newId,
                        messages = emptyList(),
                        isMuted = false,
                    )
                    startEventPump(r.value)
                    startCapturePump(r.value)
                }
            }
        }
    }

    private fun startEventPump(s: RealtimeSession) {
        eventsJob = viewModelScope.launch(Dispatchers.IO) {
            s.events.collect { ev ->
                when (ev) {
                    is RealtimeEvent.AudioDelta -> audioPlayback.write(ev.pcm16Le)
                    is RealtimeEvent.TranscriptDelta -> appendFragment(
                        if (ev.isUser) ChatRole.User else ChatRole.Assistant,
                        ev.text,
                    )
                    is RealtimeEvent.ResponseDone -> {
                        tokensIn += ev.tokensIn
                        tokensOut += ev.tokensOut
                    }
                    is RealtimeEvent.ErrorEvent -> _state.value = CallState.Failed(ev.message)
                    RealtimeEvent.SessionCreated -> Unit
                    is RealtimeEvent.Reconnecting -> Unit
                    RealtimeEvent.Reconnected -> Unit
                }
            }
        }
    }

    private fun startCapturePump(s: RealtimeSession) {
        captureJob = viewModelScope.launch {
            audioCapture.start().collect { chunk ->
                val current = _state.value
                if (current is CallState.Live && !current.isMuted) {
                    s.sendAudio(chunk)
                }
            }
        }
    }

    private fun appendFragment(role: ChatRole, fragment: String) {
        _state.update { current ->
            if (current !is CallState.Live) return@update current
            val msgs = current.messages
            val last = msgs.lastOrNull()
            val updated = if (last != null && last.role == role) {
                msgs.dropLast(1) + last.copy(text = last.text + fragment)
            } else {
                msgs + ChatMessage(role, fragment)
            }
            current.copy(messages = updated)
        }
    }

    private fun toggleMute() {
        _state.update { current ->
            if (current is CallState.Live) current.copy(isMuted = !current.isMuted) else current
        }
    }

    private fun end() {
        val current = _state.value
        val transcripts = if (current is CallState.Live) renderTranscripts(current.messages) else "" to ""
        viewModelScope.launch {
            captureJob?.cancel()
            eventsJob?.cancel()
            runCatching { audioCapture.stop() }
            runCatching { audioPlayback.stop() }
            session?.close()
            session = null
            callSessionLifecycle.stop()
            val id = sessionId
            if (id != null) {
                finishSession(sessionId = id, tokensIn = tokensIn, tokensOut = tokensOut)
                _state.value = CallState.Ended
                appScope.scope.launch {
                    val r = runCatching {
                        reflectSession(
                            sessionId = id,
                            userTranscript = transcripts.first,
                            assistantTranscript = transcripts.second,
                        )
                    }
                    r.onFailure { println("REFLECT_END: threw ${it::class.simpleName}: ${it.message}") }
                    r.onSuccess { ar ->
                        when (ar) {
                            is AppResult.Failure -> println("REFLECT_END: returned failure ${ar.error}")
                            is AppResult.Success -> println("REFLECT_END: ok addedVocab=${ar.value}")
                        }
                    }
                }
            } else {
                _state.value = CallState.Ended
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        val s = session
        session = null
        if (s != null) {
            appScope.scope.launch { runCatching { s.close() } }
        }
        runCatching { callSessionLifecycle.stop() }
        runCatching { audioCapture.stop() }
        runCatching { audioPlayback.stop() }
    }

    private fun renderTranscripts(messages: List<ChatMessage>): Pair<String, String> {
        val user = messages.filter { it.role == ChatRole.User }.joinToString("\n") { it.text }
        val assistant = messages.filter { it.role == ChatRole.Assistant }.joinToString("\n") { it.text }
        return user to assistant
    }
}
