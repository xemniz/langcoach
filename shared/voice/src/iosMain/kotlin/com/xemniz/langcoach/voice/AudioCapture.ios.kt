package com.xemniz.langcoach.voice

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.AVFAudio.AVAudioCommonFormat
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMFormatInt16
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionAllowBluetooth
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionModeVoiceChat

@OptIn(ExperimentalForeignApi::class)
actual class AudioCapture actual constructor() {
    private var engine: AVAudioEngine? = null

    actual fun start(): Flow<ByteArray> = callbackFlow {
        check(engine == null) { "Audio capture is already active" }
        val session = AVAudioSession.sharedInstance()
        session.setCategory(
            AVAudioSessionCategoryPlayAndRecord,
            mode = AVAudioSessionModeVoiceChat,
            options = AVAudioSessionCategoryOptionDefaultToSpeaker or
                AVAudioSessionCategoryOptionAllowBluetooth,
            error = null,
        )
        val audioEngine = AVAudioEngine()
        val input = audioEngine.inputNode
        val format = AVAudioFormat(
            commonFormat = AVAudioPCMFormatInt16,
            sampleRate = SAMPLE_RATE_HZ.toDouble(),
            channels = 1u,
            interleaved = true,
        )
        input.installTapOnBus(
            bus = 0u,
            bufferSize = (SAMPLE_RATE_HZ / 10).toUInt(),
            format = format,
        ) { buffer, _ ->
            val pcm = buffer?.int16ChannelData?.get(0) ?: return@installTapOnBus
            val frameCount = buffer.frameLength.toInt()
            val bytes = ByteArray(frameCount * BYTES_PER_SAMPLE)
            repeat(frameCount) { index ->
                val sample = pcm[index].toInt()
                bytes[index * 2] = (sample and 0xff).toByte()
                bytes[index * 2 + 1] = ((sample ushr 8) and 0xff).toByte()
            }
            trySend(bytes)
        }
        audioEngine.prepare()
        check(audioEngine.startAndReturnError(null)) { "Unable to start iOS audio capture" }
        engine = audioEngine

        awaitClose {
            input.removeTapOnBus(0u)
            audioEngine.stop()
            if (engine === audioEngine) engine = null
        }
    }

    actual fun stop() {
        engine?.inputNode?.removeTapOnBus(0u)
        engine?.stop()
        engine = null
    }
}
