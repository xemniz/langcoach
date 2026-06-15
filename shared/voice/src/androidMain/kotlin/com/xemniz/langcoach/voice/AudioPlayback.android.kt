package com.xemniz.langcoach.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

actual class AudioPlayback actual constructor() {
    private var track: AudioTrack? = null

    actual fun start() {
        if (track != null) return
        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val internalBuf = maxOf(minBuf * 2, SAMPLE_RATE_HZ * BYTES_PER_SAMPLE)
        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
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
        t.play()
        track = t
    }

    actual fun write(pcm16Le: ByteArray) {
        track?.write(pcm16Le, 0, pcm16Le.size, AudioTrack.WRITE_BLOCKING)
    }

    actual fun stop() {
        track?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
        track = null
    }
}
