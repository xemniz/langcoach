package com.xemniz.langcoach.domain.session

import com.xemniz.langcoach.domain.reflection.SessionTranscript
import com.xemniz.langcoach.domain.reflection.TranscriptSpeaker

/**
 * Validates model-proposed objective outcomes against transcript invariants before persistence.
 */
class ObjectiveEvaluationPolicy {
    fun validate(
        plan: SessionPlan,
        transcript: SessionTranscript,
        proposed: ObjectiveEvaluation,
    ): ObjectiveEvaluation {
        if (proposed.outcome == ObjectiveOutcome.NotObserved) {
            return proposed.copy(evidenceTurnId = null, evidenceText = null)
        }

        val evidenceIndex = transcript.turns.indexOfFirst { it.id == proposed.evidenceTurnId }
        val evidenceTurn = transcript.turns.getOrNull(evidenceIndex)
        val evidenceText = proposed.evidenceText
        if (
            evidenceTurn == null ||
            evidenceTurn.speaker != TranscriptSpeaker.Learner ||
            evidenceText.isNullOrBlank() ||
            !evidenceTurn.text.contains(evidenceText, ignoreCase = true)
        ) {
            return ObjectiveEvaluation(
                outcome = ObjectiveOutcome.NotObserved,
                evidenceTurnId = null,
                evidenceText = null,
                confidence = 0.0,
            )
        }

        if (proposed.outcome != ObjectiveOutcome.AchievedIndependently) return proposed
        val target = plan.target?.takeIf(String::isNotBlank) ?: return proposed
        val precedingTutorTurn = transcript.turns
            .subList(0, evidenceIndex)
            .lastOrNull { it.speaker == TranscriptSpeaker.Tutor }
        return if (precedingTutorTurn?.text?.contains(target, ignoreCase = true) == true) {
            proposed.copy(outcome = ObjectiveOutcome.AchievedWithHelp)
        } else {
            proposed
        }
    }
}
