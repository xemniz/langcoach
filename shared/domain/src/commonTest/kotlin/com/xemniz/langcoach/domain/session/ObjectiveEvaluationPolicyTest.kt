package com.xemniz.langcoach.domain.session

import com.xemniz.langcoach.domain.reflection.SessionTranscript
import com.xemniz.langcoach.domain.reflection.TranscriptSpeaker
import com.xemniz.langcoach.domain.reflection.TranscriptTurn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObjectiveEvaluationPolicyTest {
    private val policy = ObjectiveEvaluationPolicy()
    private val plan = SessionPlan(
        objectiveId = "vocabulary:regatear",
        kind = SessionObjectiveKind.VocabularyRetrieval,
        objective = "Retrieve regatear.",
        target = "regatear",
        openingHint = "",
        successCriteria = "Use it independently.",
    )

    @Test
    fun suppliedTargetCannotCountAsIndependentSuccess() {
        val transcript = transcript(
            tutor("turn-1", "Puedes usar el verbo regatear."),
            learner("turn-2", "Me gusta regatear en el mercado."),
        )

        val validated = policy.validate(
            plan,
            transcript,
            evaluation(ObjectiveOutcome.AchievedIndependently),
        )

        assertEquals(ObjectiveOutcome.AchievedWithHelp, validated.outcome)
    }

    @Test
    fun independentEvidenceRemainsIndependent() {
        val transcript = transcript(
            tutor("turn-1", "¿Cómo conseguiste un precio mejor?"),
            learner("turn-2", "Tuve que regatear con el vendedor."),
        )

        val validated = policy.validate(
            plan,
            transcript,
            evaluation(ObjectiveOutcome.AchievedIndependently),
        )

        assertEquals(ObjectiveOutcome.AchievedIndependently, validated.outcome)
    }

    @Test
    fun invalidEvidenceIsRejected() {
        val transcript = transcript(
            learner("turn-2", "Tuve que regatear con el vendedor."),
        )

        val validated = policy.validate(
            plan,
            transcript,
            evaluation(ObjectiveOutcome.AchievedIndependently).copy(
                evidenceText = "A sentence the learner never said.",
            ),
        )

        assertEquals(ObjectiveOutcome.NotObserved, validated.outcome)
        assertNull(validated.evidenceTurnId)
        assertEquals(0.0, validated.confidence)
    }

    @Test
    fun unobservedObjectiveDoesNotChangeVocabularySchedule() {
        assertNull(ObjectiveOutcome.NotObserved.vocabularyReviewRating())
        assertEquals(1, ObjectiveOutcome.Attempted.vocabularyReviewRating())
        assertEquals(2, ObjectiveOutcome.AchievedWithHelp.vocabularyReviewRating())
        assertEquals(3, ObjectiveOutcome.AchievedIndependently.vocabularyReviewRating())
    }

    private fun evaluation(outcome: ObjectiveOutcome) = ObjectiveEvaluation(
        outcome = outcome,
        evidenceTurnId = "turn-2",
        evidenceText = "regatear",
        confidence = 0.9,
    )

    private fun transcript(vararg turns: TranscriptTurn) = SessionTranscript(turns.toList())

    private fun tutor(id: String, text: String) =
        TranscriptTurn(id, TranscriptSpeaker.Tutor, text)

    private fun learner(id: String, text: String) =
        TranscriptTurn(id, TranscriptSpeaker.Learner, text)
}
