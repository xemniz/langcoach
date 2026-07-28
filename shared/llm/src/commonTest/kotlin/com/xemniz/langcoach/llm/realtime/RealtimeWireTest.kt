package com.xemniz.langcoach.llm.realtime

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RealtimeWireTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun responseDonePreservesFinalStatusAndFailureReason() {
        val event = json.decodeFromString(
            WireServerEvent.serializer(),
            """
                {
                  "type": "response.done",
                  "response": {
                    "id": "resp_test",
                    "status": "incomplete",
                    "status_details": { "reason": "max_output_tokens" },
                    "usage": { "input_tokens": 12, "output_tokens": 34 }
                  }
                }
            """.trimIndent(),
        )

        assertEquals(RealtimeResponseStatus.Incomplete, RealtimeResponseStatus.fromWire(event.response?.status))
        assertEquals("resp_test", event.response?.id)
        assertEquals("max_output_tokens", event.response?.status_details?.reason)
        assertEquals(12, event.response?.usage?.input_tokens)
        assertEquals(34, event.response?.usage?.output_tokens)
    }

    @Test
    fun serverVadCannotInterruptAssistantPlayback() {
        assertFalse(WireTurnDetection().interrupt_response)
    }

    @Test
    fun languageLearningUsesHighAccuracyTranscription() {
        assertEquals("gpt-4o-transcribe", WireTranscriptionCfg().model)
    }

    @Test
    fun audioDeltaPreservesItsResponseId() {
        val event = json.decodeFromString(
            WireServerEvent.serializer(),
            """
                {
                  "type": "response.output_audio.delta",
                  "response_id": "resp_audio",
                  "delta": "AA=="
                }
            """.trimIndent(),
        )

        assertEquals("resp_audio", event.response_id)
    }
}
