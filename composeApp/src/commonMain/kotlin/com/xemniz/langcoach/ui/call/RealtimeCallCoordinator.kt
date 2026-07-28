package com.xemniz.langcoach.ui.call

import com.xemniz.langcoach.core.AppError
import com.xemniz.langcoach.core.AppResult
import com.xemniz.langcoach.llm.realtime.OpenAIRealtimeClient
import com.xemniz.langcoach.llm.realtime.RealtimeEvent
import com.xemniz.langcoach.llm.realtime.RealtimeSession
import com.xemniz.langcoach.voice.AudioCapture
import com.xemniz.langcoach.voice.AudioPlayback
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal enum class RealtimeConnectionState {
    Stopped,
    Connecting,
    Connected,
    Reconnecting,
}

internal data class CallUsage(
    val tokensIn: Int = 0,
    val tokensOut: Int = 0,
)

internal data class RealtimeCallSnapshot(
    val connection: RealtimeConnectionState = RealtimeConnectionState.Stopped,
    val turn: VoiceTurnState = VoiceTurnState.Stopped,
    val isMuted: Boolean = false,
    val usage: CallUsage = CallUsage(),
) {
    val canSendMicrophone: Boolean
        get() = connection == RealtimeConnectionState.Connected &&
            !isMuted &&
            turn.acceptsMicrophone
}

internal sealed interface RealtimeCallEvent {
    data class TranscriptDelta(
        val text: String,
        val isUser: Boolean,
        val itemId: String?,
    ) : RealtimeCallEvent

    data class TranscriptCompleted(
        val text: String,
        val isUser: Boolean,
        val itemId: String?,
    ) : RealtimeCallEvent
}

/**
 * Serializes Realtime and playback control events into one deterministic turn pipeline.
 *
 * Microphone PCM stays on a separate data path and consults the atomic turn snapshot before each
 * send. UI code observes snapshots and transcripts; it never manipulates playback or response IDs.
 */
internal class RealtimeCallCoordinator(
    private val realtimeClient: OpenAIRealtimeClient,
    private val audioCapture: AudioCapture,
    private val audioPlayback: AudioPlayback,
) {
    private sealed interface Command {
        data class ServerEvent(val value: RealtimeEvent) : Command
        data class SetMuted(val muted: Boolean) : Command
        data class PlaybackDrained(val responseId: String?) : Command
        data class PlaybackFailed(val message: String) : Command
        data class PipelineFailed(val message: String) : Command
    }

    private val rootScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(RealtimeCallSnapshot())
    val state: StateFlow<RealtimeCallSnapshot> = _state.asStateFlow()

    private val _events = MutableSharedFlow<RealtimeCallEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<RealtimeCallEvent> = _events.asSharedFlow()

    private var session: RealtimeSession? = null
    private var sessionJob: Job? = null
    private var commands: Channel<Command>? = null
    private val lifecycleMutex = Mutex()

    suspend fun start(
        systemPrompt: String,
        transcriptionLanguage: String?,
    ): AppResult<Unit> = lifecycleMutex.withLock {
        stopUnlocked()
        _state.value = RealtimeCallSnapshot(
            connection = RealtimeConnectionState.Connecting,
            turn = VoiceTurnState.Stopped,
        )
        return when (val result = realtimeClient.connect()) {
            is AppResult.Failure -> {
                _state.value = _state.value.copy(
                    connection = RealtimeConnectionState.Stopped,
                    turn = VoiceTurnState.Failed(result.error.message),
                )
                AppResult.Failure(result.error)
            }
            is AppResult.Success -> {
                runCatching {
                    val activeSession = result.value
                    session = activeSession
                    activeSession.configure(
                        systemPrompt = systemPrompt,
                        transcriptionLanguage = transcriptionLanguage,
                    )
                    audioPlayback.start()
                    startPipeline(activeSession)
                }.fold(
                    onSuccess = { AppResult.Success(Unit) },
                    onFailure = { error ->
                        stopUnlocked()
                        val message = error.message ?: "Could not start voice audio"
                        _state.value = _state.value.copy(
                            turn = VoiceTurnState.Failed(message),
                        )
                        AppResult.Failure(AppError.Unknown(message))
                    },
                )
            }
        }
    }

    fun setMuted(muted: Boolean) {
        commands?.trySend(Command.SetMuted(muted))
    }

    suspend fun stop(): CallUsage = lifecycleMutex.withLock {
        stopUnlocked()
    }

    private suspend fun stopUnlocked(): CallUsage {
        val usage = _state.value.usage
        commands?.close()
        commands = null
        sessionJob?.cancelAndJoin()
        sessionJob = null
        runCatching { audioCapture.stop() }
        runCatching { audioPlayback.stop() }
        val activeSession = session
        session = null
        runCatching { activeSession?.close() }
        _state.value = RealtimeCallSnapshot(
            turn = reduceVoiceTurn(_state.value.turn, VoiceTurnEvent.Stop).state,
        )
        return usage
    }

    private fun startPipeline(activeSession: RealtimeSession) {
        val channel = Channel<Command>(capacity = Channel.UNLIMITED)
        commands = channel
        val pipelineJob = SupervisorJob(rootScope.coroutineContext[Job])
        sessionJob = pipelineJob
        val pipelineScope = CoroutineScope(rootScope.coroutineContext + pipelineJob)

        _state.value = RealtimeCallSnapshot(
            connection = RealtimeConnectionState.Connected,
            turn = reduceVoiceTurn(
                VoiceTurnState.Stopped,
                VoiceTurnEvent.Connected,
            ).state,
        )

        pipelineScope.launch {
            for (command in channel) {
                handleCommand(command, pipelineScope, channel)
            }
        }
        pipelineScope.launch {
            activeSession.events.collect { event ->
                channel.send(Command.ServerEvent(event))
            }
        }
        pipelineScope.launch {
            try {
                audioCapture.start().collect { pcm ->
                    if (_state.value.canSendMicrophone) {
                        activeSession.sendAudio(pcm)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (t: Throwable) {
                channel.send(
                    Command.PipelineFailed(t.message ?: "Microphone capture stopped"),
                )
            }
        }
    }

    private suspend fun handleCommand(
        command: Command,
        pipelineScope: CoroutineScope,
        channel: Channel<Command>,
    ) {
        when (command) {
            is Command.SetMuted -> {
                _state.value = _state.value.copy(isMuted = command.muted)
            }
            is Command.PlaybackDrained -> {
                applyTurnEvent(VoiceTurnEvent.PlaybackDrained(command.responseId), pipelineScope, channel)
            }
            is Command.PlaybackFailed -> {
                applyTurnEvent(VoiceTurnEvent.PlaybackFailed(command.message), pipelineScope, channel)
            }
            is Command.PipelineFailed -> {
                applyTurnEvent(VoiceTurnEvent.SessionFailed(command.message), pipelineScope, channel)
            }
            is Command.ServerEvent -> handleServerEvent(
                command.value,
                pipelineScope,
                channel,
            )
        }
    }

    private suspend fun handleServerEvent(
        event: RealtimeEvent,
        pipelineScope: CoroutineScope,
        channel: Channel<Command>,
    ) {
        when (event) {
            RealtimeEvent.SessionCreated -> Unit
            is RealtimeEvent.ResponseStarted ->
                applyTurnEvent(VoiceTurnEvent.ResponseStarted(event.responseId), pipelineScope, channel)
            is RealtimeEvent.AudioDelta -> {
                applyTurnEvent(VoiceTurnEvent.AudioReceived(event.responseId), pipelineScope, channel)
                audioPlayback.write(event.pcm16Le)
            }
            is RealtimeEvent.TranscriptDelta -> _events.emit(
                RealtimeCallEvent.TranscriptDelta(event.text, event.isUser, event.itemId),
            )
            is RealtimeEvent.TranscriptCompleted -> _events.emit(
                RealtimeCallEvent.TranscriptCompleted(event.text, event.isUser, event.itemId),
            )
            is RealtimeEvent.ResponseDone -> {
                val usage = _state.value.usage
                _state.value = _state.value.copy(
                    usage = CallUsage(
                        tokensIn = usage.tokensIn + event.tokensIn,
                        tokensOut = usage.tokensOut + event.tokensOut,
                    ),
                )
                applyTurnEvent(
                    VoiceTurnEvent.ResponseFinished(
                        responseId = event.responseId,
                        status = event.status,
                        reason = event.reason,
                    ),
                    pipelineScope,
                    channel,
                )
            }
            is RealtimeEvent.ErrorEvent ->
                applyTurnEvent(VoiceTurnEvent.SessionFailed(event.message), pipelineScope, channel)
            is RealtimeEvent.Reconnecting -> {
                _state.value = _state.value.copy(
                    connection = RealtimeConnectionState.Reconnecting,
                )
                applyTurnEvent(VoiceTurnEvent.TransportInterrupted, pipelineScope, channel)
            }
            RealtimeEvent.Reconnected -> {
                _state.value = _state.value.copy(
                    connection = RealtimeConnectionState.Connected,
                )
            }
        }
    }

    private fun applyTurnEvent(
        event: VoiceTurnEvent,
        pipelineScope: CoroutineScope,
        channel: Channel<Command>,
    ) {
        val transition = reduceVoiceTurn(_state.value.turn, event)
        _state.value = _state.value.copy(turn = transition.state)
        transition.effects.forEach { effect ->
            when (effect) {
                is VoiceTurnEffect.DrainPlayback -> pipelineScope.launch {
                    try {
                        audioPlayback.awaitIdle()
                        channel.send(Command.PlaybackDrained(effect.responseId))
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (t: Throwable) {
                        channel.send(
                            Command.PlaybackFailed(t.message ?: "Audio playback did not finish"),
                        )
                    }
                }
            }
        }
    }
}
