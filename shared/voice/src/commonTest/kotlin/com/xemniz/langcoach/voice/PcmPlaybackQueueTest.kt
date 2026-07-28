package com.xemniz.langcoach.voice

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PcmPlaybackQueueTest {
    @Test
    fun burstyAudioIsBufferedWithoutDropsAndDrainsInOrder() = runBlocking {
        val queue = PcmPlaybackQueue()
        val expected = List(200) { index -> byteArrayOf(index.toByte(), (index + 1).toByte()) }
        val received = mutableListOf<ByteArray>()

        expected.forEach { chunk ->
            assertTrue(queue.offer(chunk))
        }

        val consumer = launch {
            for (command in queue.commands) {
                when (command) {
                    is PcmPlaybackCommand.Chunk -> received += command.pcm16Le
                    is PcmPlaybackCommand.Drain -> command.completion.complete(Unit)
                }
            }
        }

        queue.awaitDrain()

        assertEquals(expected.size, received.size)
        expected.zip(received).forEach { (wanted, actual) ->
            assertContentEquals(wanted, actual)
        }

        queue.close()
        consumer.cancelAndJoin()
    }
}
