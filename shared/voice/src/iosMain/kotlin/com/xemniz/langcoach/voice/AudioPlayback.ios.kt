package com.xemniz.langcoach.voice

actual class AudioPlayback actual constructor() {
    actual fun start() {}
    actual fun write(pcm16Le: ByteArray) {}
    actual fun stop() {}
}
