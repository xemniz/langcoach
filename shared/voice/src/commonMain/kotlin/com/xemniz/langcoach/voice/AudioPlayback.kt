package com.xemniz.langcoach.voice

expect class AudioPlayback() {
    fun start()
    /** Append PCM16 LE 24 kHz mono bytes for playback. Non-blocking; buffers internally. */
    fun write(pcm16Le: ByteArray)
    fun stop()
}
