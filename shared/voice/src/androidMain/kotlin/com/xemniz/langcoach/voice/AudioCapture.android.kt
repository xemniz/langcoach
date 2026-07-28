package com.xemniz.langcoach.voice

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

actual class AudioCapture actual constructor() {
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private data class Capture(
        val recorder: AudioRecord,
        val aec: AcousticEchoCanceler?,
        val ns: NoiseSuppressor?,
    )

    private val current = AtomicReference<Capture?>(null)

    @SuppressLint("MissingPermission")
    actual fun start(): Flow<ByteArray> = callbackFlow {
        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val internalBuf = maxOf(minBuf * 2, SAMPLE_RATE_HZ * BYTES_PER_SAMPLE)
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            internalBuf,
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            close(IllegalStateException("AudioRecord failed to initialize"))
            return@callbackFlow
        }
        recorder.startRecording()

        val sessionId = recorder.audioSessionId
        val aec = if (AcousticEchoCanceler.isAvailable())
            AcousticEchoCanceler.create(sessionId)?.also { it.enabled = true }
        else null
        val ns = if (NoiseSuppressor.isAvailable())
            NoiseSuppressor.create(sessionId)?.also { it.enabled = true }
        else null

        val capture = Capture(recorder, aec, ns)
        current.set(capture)

        val chunkBytes = SAMPLE_RATE_HZ / 10 * BYTES_PER_SAMPLE
        val buffer = ByteArray(chunkBytes)

        val job = launch(Dispatchers.IO) {
            try {
                while (!isClosedForSend) {
                    val read = recorder.read(buffer, 0, chunkBytes, AudioRecord.READ_BLOCKING)
                    if (read > 0) {
                        val out = if (read == chunkBytes) buffer.copyOf() else buffer.copyOfRange(0, read)
                        trySend(out)
                    } else if (read < 0) {
                        close(IllegalStateException("AudioRecord.read error: $read"))
                        return@launch
                    }
                }
            } catch (t: Throwable) {
                close(t)
            }
        }

        awaitClose {
            job.cancel()
            runCatching { aec?.release() }
            runCatching { ns?.release() }
            runCatching { recorder.stop() }
            runCatching { recorder.release() }
            current.compareAndSet(capture, null)
        }
    }.flowOn(Dispatchers.IO)

    actual fun stop() {
        current.getAndSet(null)?.let { capture ->
            cleanupScope.launch {
                runCatching { capture.aec?.release() }
                runCatching { capture.ns?.release() }
                runCatching { capture.recorder.stop() }
                runCatching { capture.recorder.release() }
            }
        }
    }
}
