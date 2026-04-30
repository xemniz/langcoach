package com.xemniz.langcoach.voice

import kotlinx.coroutines.flow.Flow

expect class AudioCapture() {
    /**
     * Starts mic capture. Emits 24 kHz PCM16 mono LE frames (~100ms each). Cancel collection to stop.
     * Throws if RECORD_AUDIO permission not granted.
     */
    fun start(): Flow<ByteArray>

    fun stop()
}
