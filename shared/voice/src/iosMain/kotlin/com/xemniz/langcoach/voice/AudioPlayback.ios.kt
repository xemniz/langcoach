package com.xemniz.langcoach.voice

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.set
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPCMFormatInt16
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionAllowBluetooth
import platform.AVFAudio.AVAudioSessionCategoryOptionDefaultToSpeaker
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVAudioSessionModeVoiceChat
import kotlin.math.max

@OptIn(ExperimentalForeignApi::class)
actual class AudioPlayback actual constructor() {
    private var engine: AVAudioEngine? = null
    private var player: AVAudioPlayerNode? = null
    private var format: AVAudioFormat? = null
    private var pendingBuffers = 0

    actual fun start() {
        if (engine != null) return
        val session = AVAudioSession.sharedInstance()
        session.setCategory(
            AVAudioSessionCategoryPlayAndRecord,
            mode = AVAudioSessionModeVoiceChat,
            options = AVAudioSessionCategoryOptionDefaultToSpeaker or
                AVAudioSessionCategoryOptionAllowBluetooth,
            error = null,
        )
        val playbackFormat = AVAudioFormat(
            commonFormat = AVAudioPCMFormatInt16,
            sampleRate = SAMPLE_RATE_HZ.toDouble(),
            channels = 1u,
            interleaved = true,
        )
        val audioEngine = AVAudioEngine()
        val playerNode = AVAudioPlayerNode()
        audioEngine.attachNode(playerNode)
        audioEngine.connect(playerNode, to = audioEngine.mainMixerNode, format = playbackFormat)
        audioEngine.prepare()
        check(audioEngine.startAndReturnError(null)) { "Unable to start iOS audio playback" }
        playerNode.play()

        format = playbackFormat
        player = playerNode
        engine = audioEngine
    }

    actual fun write(pcm16Le: ByteArray) {
        if (pcm16Le.isEmpty()) return
        val playerNode = player ?: return
        val playbackFormat = format ?: return
        val frameCount = pcm16Le.size / BYTES_PER_SAMPLE
        if (frameCount == 0) return
        val buffer = AVAudioPCMBuffer(
            pCMFormat = playbackFormat,
            frameCapacity = frameCount.toUInt(),
        )
        buffer.frameLength = frameCount.toUInt()
        val channel = buffer.int16ChannelData?.get(0) ?: error("Missing iOS PCM channel")
        repeat(frameCount) { index ->
            val lo = pcm16Le[index * 2].toInt() and 0xff
            val hi = pcm16Le[index * 2 + 1].toInt()
            channel[index] = ((hi shl 8) or lo).toShort()
        }
        pendingBuffers++
        playerNode.scheduleBuffer(buffer) {
            pendingBuffers = max(0, pendingBuffers - 1)
        }
        if (!playerNode.isPlaying()) playerNode.play()
    }

    actual suspend fun awaitIdle() {
        withTimeout(DRAIN_TIMEOUT_MS) {
            while (pendingBuffers > 0) delay(PLAYBACK_POLL_MS)
        }
    }

    actual fun stop() {
        player?.stop()
        engine?.stop()
        player = null
        engine = null
        format = null
        pendingBuffers = 0
    }

    private companion object {
        const val PLAYBACK_POLL_MS = 10L
        const val DRAIN_TIMEOUT_MS = 30_000L
    }
}
