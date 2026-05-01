package com.xemniz.langcoach.llm.realtime

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

@Serializable
internal data class WireSessionUpdate(
    val type: String = "session.update",
    val session: WireSessionConfig,
)

@Serializable
internal data class WireSessionConfig(
    val modalities: List<String> = listOf("audio", "text"),
    val instructions: String,
    val voice: String,
    val input_audio_format: String = "pcm16",
    val output_audio_format: String = "pcm16",
    val input_audio_transcription: WireTranscriptionCfg = WireTranscriptionCfg(),
    val turn_detection: WireTurnDetection = WireTurnDetection(),
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class WireTranscriptionCfg(
    val model: String = "whisper-1",
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val language: String? = null,
)

@Serializable
internal data class WireTurnDetection(
    val type: String = "semantic_vad",
    val eagerness: String = "low",
)

@Serializable
internal data class WireInputAudioAppend(
    val type: String = "input_audio_buffer.append",
    val audio: String,
)

@Serializable
internal data class WireServerEvent(
    val type: String,
    val delta: String? = null,
    val transcript: String? = null,
    val response: WireResponse? = null,
    val error: WireError? = null,
)

@Serializable
internal data class WireResponse(
    val usage: WireUsage? = null,
)

@Serializable
internal data class WireUsage(
    val total_tokens: Int = 0,
    val input_tokens: Int = 0,
    val output_tokens: Int = 0,
)

@Serializable
internal data class WireError(val type: String? = null, val message: String? = null)
