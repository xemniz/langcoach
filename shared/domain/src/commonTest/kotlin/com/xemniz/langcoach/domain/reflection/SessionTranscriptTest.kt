package com.xemniz.langcoach.domain.reflection

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class SessionTranscriptTest {
    @Test
    fun renderPreservesTurnOrderAndEvidenceIds() {
        val transcript = SessionTranscript(
            listOf(
                TranscriptTurn("turn-1", TranscriptSpeaker.Tutor, "Ayer fui al mercado."),
                TranscriptTurn("turn-2", TranscriptSpeaker.Learner, "Ayer fui al mercado."),
                TranscriptTurn("turn-3", TranscriptSpeaker.Tutor, "¿Qué compraste?"),
            ),
        )

        val rendered = transcript.render()

        assertContains(rendered, "turn-1 TUTOR")
        assertContains(rendered, "turn-2 LEARNER")
        assertEquals(
            listOf("Ayer fui al mercado."),
            transcript.turns
                .filter { it.speaker == TranscriptSpeaker.Learner }
                .map { it.text },
        )
    }
}
