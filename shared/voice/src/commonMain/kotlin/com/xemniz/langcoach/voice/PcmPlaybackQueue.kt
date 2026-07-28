package com.xemniz.langcoach.voice

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

internal sealed interface PcmPlaybackCommand {
    data class Chunk(val pcm16Le: ByteArray) : PcmPlaybackCommand
    data class Drain(val completion: CompletableDeferred<Unit>) : PcmPlaybackCommand
}

/**
 * Lossless FIFO between bursty network audio and real-time hardware playback.
 *
 * Realtime audio commonly arrives faster than wall-clock playback. A small dropping queue creates
 * holes in words and sentences, so this queue is intentionally unlimited for the bounded lifetime
 * of one voice call. The drain command is ordered after every preceding audio chunk.
 */
internal class PcmPlaybackQueue {
    private val channel = Channel<PcmPlaybackCommand>(capacity = Channel.UNLIMITED)

    val commands: ReceiveChannel<PcmPlaybackCommand> = channel

    fun offer(pcm16Le: ByteArray): Boolean =
        channel.trySend(PcmPlaybackCommand.Chunk(pcm16Le)).isSuccess

    suspend fun awaitDrain() {
        val completion = CompletableDeferred<Unit>()
        channel.send(PcmPlaybackCommand.Drain(completion))
        completion.await()
    }

    fun close() {
        channel.close()
    }
}
