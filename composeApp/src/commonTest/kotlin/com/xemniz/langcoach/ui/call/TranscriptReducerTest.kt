package com.xemniz.langcoach.ui.call

import kotlin.test.Test
import kotlin.test.assertEquals

class TranscriptReducerTest {
    @Test
    fun consecutiveDeltasForSameSpeakerAreMerged() {
        val messages = appendTranscript(emptyList(), ChatRole.Assistant, "Buenos")
        val updated = appendTranscript(messages, ChatRole.Assistant, " días")

        assertEquals(listOf(ChatMessage(ChatRole.Assistant, "Buenos días")), updated)
    }

    @Test
    fun speakerChangeCreatesNewMessage() {
        val messages = listOf(ChatMessage(ChatRole.Assistant, "¿Qué hiciste?"))
        val updated = appendTranscript(messages, ChatRole.User, "Fui al mercado")

        assertEquals(2, updated.size)
        assertEquals(ChatRole.User, updated.last().role)
    }

    @Test
    fun completedTranscriptReplacesStreamingDraft() {
        val draft = listOf(ChatMessage(ChatRole.User, "Ayer voy"))
        val updated = completeTranscript(draft, ChatRole.User, "Ayer fui al mercado")

        assertEquals("Ayer fui al mercado", updated.single().text)
    }

    @Test
    fun completedTranscriptUpdatesItsItemAfterAssistantEventsInterleave() {
        val userItemId = "item-user-1"
        val assistantItemId = "item-assistant-1"
        val draft = appendTranscript(
            messages = emptyList(),
            role = ChatRole.User,
            fragment = "Ayer fui",
            realtimeItemId = userItemId,
        )
        val interleaved = appendTranscript(
            messages = draft,
            role = ChatRole.Assistant,
            fragment = "Muy bien",
            realtimeItemId = assistantItemId,
        )

        val completed = completeTranscript(
            messages = interleaved,
            role = ChatRole.User,
            fullText = "Ayer fui al mercado",
            realtimeItemId = userItemId,
        )

        assertEquals(2, completed.size)
        assertEquals("Ayer fui al mercado", completed.first().text)
        assertEquals("Muy bien", completed.last().text)
    }

    @Test
    fun duplicateCompletionForSameItemDoesNotCreateAnotherMessage() {
        val itemId = "item-user-1"
        val first = completeTranscript(
            messages = emptyList(),
            role = ChatRole.User,
            fullText = "Hola",
            realtimeItemId = itemId,
        )
        val duplicate = completeTranscript(
            messages = first,
            role = ChatRole.User,
            fullText = "Hola",
            realtimeItemId = itemId,
        )

        assertEquals(1, duplicate.size)
    }
}
