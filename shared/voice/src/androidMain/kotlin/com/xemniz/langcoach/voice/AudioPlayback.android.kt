package com.xemniz.langcoach.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

actual class AudioPlayback actual constructor() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var playbackJob: Job? = null
    private var audioQueue: PcmPlaybackQueue? = null

    actual fun start() {
        if (playbackJob != null) return
        val queue = PcmPlaybackQueue()
        audioQueue = queue
        playbackJob = scope.launch {
            val minBuf = AudioTrack.getMinBufferSize(
                SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            val internalBuf = maxOf(minBuf * 2, SAMPLE_RATE_HZ * BYTES_PER_SAMPLE)
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE_HZ)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(internalBuf)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            var writtenFrames = 0L
            try {
                track.play()
                for (command in queue.commands) {
                    when (command) {
                        is PcmPlaybackCommand.Chunk -> {
                            val writtenBytes = writeFully(track, command.pcm16Le)
                            writtenFrames += writtenBytes / BYTES_PER_SAMPLE
                        }
                        is PcmPlaybackCommand.Drain -> {
                            runCatching {
                                awaitPlaybackHead(track, writtenFrames)
                            }.fold(
                                onSuccess = {
                                    Log.d(TAG, "Playback drained at $writtenFrames frames")
                                    command.completion.complete(Unit)
                                },
                                onFailure = { command.completion.completeExceptionally(it) },
                            )
                        }
                    }
                }
            } finally {
                runCatching { track.stop() }
                runCatching { track.release() }
            }
        }
    }

    actual fun write(pcm16Le: ByteArray) {
        if (pcm16Le.isEmpty()) return
        if (audioQueue?.offer(pcm16Le) == false) {
            Log.w(TAG, "Ignored audio because playback is not active")
        }
    }

    actual suspend fun awaitIdle() {
        val queue = audioQueue ?: return
        withTimeout(DRAIN_TIMEOUT_MS) {
            queue.awaitDrain()
        }
    }

    actual fun stop() {
        audioQueue?.close()
        audioQueue = null
        playbackJob?.cancel()
        playbackJob = null
    }

    private fun writeFully(track: AudioTrack, pcm: ByteArray): Int {
        var offset = 0
        while (offset < pcm.size) {
            val written = track.write(
                pcm,
                offset,
                pcm.size - offset,
                AudioTrack.WRITE_BLOCKING,
            )
            check(written > 0) { "AudioTrack.write failed: $written" }
            offset += written
        }
        return offset
    }

    private suspend fun awaitPlaybackHead(track: AudioTrack, targetFrames: Long) {
        val startedAt = SystemClock.elapsedRealtime()
        while (unsignedPlaybackHead(track) < targetFrames) {
            check(track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                "AudioTrack stopped before buffered audio drained"
            }
            check(SystemClock.elapsedRealtime() - startedAt < DRAIN_TIMEOUT_MS) {
                "Timed out waiting for buffered audio to drain"
            }
            delay(PLAYBACK_POLL_MS)
        }
    }

    private fun unsignedPlaybackHead(track: AudioTrack): Long =
        track.playbackHeadPosition.toLong() and 0xffff_ffffL

    private companion object {
        const val TAG = "LangCoachAudio"
        const val PLAYBACK_POLL_MS = 10L
        const val DRAIN_TIMEOUT_MS = 30_000L
    }
}
