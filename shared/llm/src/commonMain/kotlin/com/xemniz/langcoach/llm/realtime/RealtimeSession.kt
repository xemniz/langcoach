package com.xemniz.langcoach.llm.realtime

import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class RealtimeSession internal constructor(
    initialWs: DefaultClientWebSocketSession,
    private val json: Json,
    private val openWs: suspend () -> DefaultClientWebSocketSession,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _events = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 64)
    val events: Flow<RealtimeEvent> = _events.asSharedFlow()
    private var readJob: Job? = null

    private var currentWs: DefaultClientWebSocketSession = initialWs
    private var lastSystemPrompt: String? = null
    private var lastVoice: String? = null
    private var lastTranscriptionLanguage: String? = null
    private var closed = false

    internal fun startReading() {
        readJob = scope.launch {
            runReadLoop()
        }
    }

    private suspend fun runReadLoop() {
        while (!closed) {
            val ws = currentWs
            try {
                for (frame in ws.incoming) {
                    if (frame is Frame.Text) handleText(frame.readText())
                }
                if (closed) return
                if (!reconnectLoop()) return
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                if (closed) return
                if (!reconnectLoop()) return
            }
        }
    }

    private suspend fun reconnectLoop(): Boolean {
        val delaysMs = longArrayOf(1000, 2000, 4000, 8000, 16000)
        for ((index, wait) in delaysMs.withIndex()) {
            val attempt = index + 1
            _events.emit(RealtimeEvent.Reconnecting(attempt))
            delay(wait)
            if (closed) return false
            val newWs = runCatching { openWs() }.getOrNull()
            if (newWs != null) {
                currentWs = newWs
                val prompt = lastSystemPrompt
                if (prompt != null) {
                    runCatching {
                        val msg = WireSessionUpdate(
                            session = WireSessionConfig(
                                instructions = prompt,
                                audio = WireAudioConfig(
                                    input = WireAudioInput(
                                        transcription = WireTranscriptionCfg(
                                            language = lastTranscriptionLanguage,
                                        ),
                                    ),
                                    output = WireAudioOutput(voice = lastVoice ?: "alloy"),
                                ),
                            ),
                        )
                        newWs.send(json.encodeToString(WireSessionUpdate.serializer(), msg))
                    }
                }
                _events.emit(RealtimeEvent.Reconnected)
                return true
            }
        }
        _events.emit(RealtimeEvent.ErrorEvent("Reconnect failed after ${delaysMs.size} attempts"))
        return false
    }

    private suspend fun handleText(text: String) {
        val ev = runCatching { json.decodeFromString(WireServerEvent.serializer(), text) }.getOrNull() ?: return
        when (ev.type) {
            "session.created" -> _events.emit(RealtimeEvent.SessionCreated)
            "response.created" -> {
                _events.emit(RealtimeEvent.ResponseStarted(ev.response?.id))
            }
            "response.output_audio.delta", "response.audio.delta" -> ev.delta?.let { b64 ->
                runCatching { Base64.decode(b64) }.getOrNull()?.let {
                    _events.emit(RealtimeEvent.AudioDelta(it, responseId = ev.response_id))
                }
            }
            "response.output_audio_transcript.delta", "response.audio_transcript.delta" -> ev.delta?.let {
                _events.emit(RealtimeEvent.TranscriptDelta(it, isUser = false, itemId = ev.item_id))
            }
            "response.output_audio_transcript.done", "response.audio_transcript.done" -> ev.transcript?.let {
                _events.emit(RealtimeEvent.TranscriptCompleted(it, isUser = false, itemId = ev.item_id))
            }
            "conversation.item.input_audio_transcription.delta" -> ev.delta?.let {
                _events.emit(RealtimeEvent.TranscriptDelta(it, isUser = true, itemId = ev.item_id))
            }
            "conversation.item.input_audio_transcription.completed" -> ev.transcript?.let {
                _events.emit(RealtimeEvent.TranscriptCompleted(it, isUser = true, itemId = ev.item_id))
            }
            "response.done" -> {
                val u = ev.response?.usage
                val details = ev.response?.status_details
                _events.emit(
                    RealtimeEvent.ResponseDone(
                        tokensIn = u?.input_tokens ?: 0,
                        tokensOut = u?.output_tokens ?: 0,
                        status = RealtimeResponseStatus.fromWire(ev.response?.status),
                        reason = details?.error?.message ?: details?.reason,
                        responseId = ev.response?.id,
                    ),
                )
            }
            "error" -> _events.emit(RealtimeEvent.ErrorEvent(ev.error?.message ?: "Unknown error"))
            else -> Unit
        }
    }

    suspend fun configure(
        systemPrompt: String,
        voice: String = "alloy",
        transcriptionLanguage: String? = null,
    ) {
        lastSystemPrompt = systemPrompt
        lastVoice = voice
        lastTranscriptionLanguage = transcriptionLanguage
        val msg = WireSessionUpdate(
            session = WireSessionConfig(
                instructions = systemPrompt,
                audio = WireAudioConfig(
                    input = WireAudioInput(
                        transcription = WireTranscriptionCfg(language = transcriptionLanguage),
                    ),
                    output = WireAudioOutput(voice = voice),
                ),
            ),
        )
        runCatching { currentWs.send(json.encodeToString(WireSessionUpdate.serializer(), msg)) }
    }

    suspend fun sendAudio(pcm16Le: ByteArray) {
        val msg = WireInputAudioAppend(audio = Base64.encode(pcm16Le))
        runCatching { currentWs.send(json.encodeToString(WireInputAudioAppend.serializer(), msg)) }
    }

    suspend fun close() {
        closed = true
        readJob?.cancel()
        runCatching { currentWs.close() }
    }
}
