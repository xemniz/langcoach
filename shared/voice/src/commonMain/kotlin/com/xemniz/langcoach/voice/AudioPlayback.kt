package com.xemniz.langcoach.voice

expect class AudioPlayback() {
    fun start()
    /** Append PCM16 LE 24 kHz mono bytes for lossless, ordered playback. */
    fun write(pcm16Le: ByteArray)
    /** Suspend until every audio byte accepted before this call has physically played. */
    suspend fun awaitIdle()
    fun stop()
}
