package com.xemniz.langcoach.voice

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

actual class AudioCapture actual constructor() {
    actual fun start(): Flow<ByteArray> = flow {
        throw NotImplementedError("iOS AudioCapture not implemented in M3 (Android-only milestone).")
    }
    actual fun stop() {}
}
