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
    val type: String = "realtime",
    val instructions: String,
    val output_modalities: List<String> = listOf("audio"),
    val audio: WireAudioConfig,
)

@Serializable
internal data class WireAudioConfig(
    val input: WireAudioInput,
    val output: WireAudioOutput,
)

@Serializable
internal data class WireAudioInput(
    val format: WireAudioFormat = WireAudioFormat(),
    val transcription: WireTranscriptionCfg = WireTranscriptionCfg(),
    val noise_reduction: WireNoiseReduction = WireNoiseReduction(),
    val turn_detection: WireTurnDetection = WireTurnDetection(),
)

@Serializable
internal data class WireNoiseReduction(
    val type: String = "near_field",
)

@Serializable
internal data class WireAudioOutput(
    val format: WireAudioFormat = WireAudioFormat(),
    val voice: String,
)

@Serializable
internal data class WireAudioFormat(
    val type: String = "audio/pcm",
    val rate: Int = 24_000,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
internal data class WireTranscriptionCfg(
    // Favor transcript accuracy for a language-learning app; the text is part of the product.
    val model: String = "gpt-4o-transcribe",
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val language: String? = null,
)

@Serializable
internal data class WireTurnDetection(
    val type: String = "server_vad",
    val threshold: Double = 0.65,
    val prefix_padding_ms: Int = 300,
    val silence_duration_ms: Int = 650,
    val create_response: Boolean = true,
    // The Android client deliberately uses half-duplex turns so speaker echo cannot barge in.
    val interrupt_response: Boolean = false,
)

@Serializable
internal data class WireInputAudioAppend(
    val type: String = "input_audio_buffer.append",
    val audio: String,
)

@Serializable
internal data class WireServerEvent(
    val type: String,
    val event_id: String? = null,
    val response_id: String? = null,
    val item_id: String? = null,
    val delta: String? = null,
    val transcript: String? = null,
    val response: WireResponse? = null,
    val error: WireError? = null,
)

@Serializable
internal data class WireResponse(
    val id: String? = null,
    val status: String? = null,
    val status_details: WireStatusDetails? = null,
    val usage: WireUsage? = null,
)

@Serializable
internal data class WireStatusDetails(
    val reason: String? = null,
    val error: WireError? = null,
)

@Serializable
internal data class WireUsage(
    val total_tokens: Int = 0,
    val input_tokens: Int = 0,
    val output_tokens: Int = 0,
)

@Serializable
internal data class WireError(val type: String? = null, val message: String? = null)
