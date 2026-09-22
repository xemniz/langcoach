package com.xemniz.langcoach.domain.reflection

import com.xemniz.langcoach.domain.session.ObjectiveEvaluation

data class ReflectionResult(
    val newVocab: List<ReflectedVocab>,
    val errors: List<ReflectedError>,
    val summary: String,
    val strength: String,
    val nextStep: String,
    val assignment: String,
    val objectiveEvaluation: ObjectiveEvaluation?,
    val practicalGoalObservation: PracticalGoalObservation? = null,
    val practicalGoalDecision: PracticalGoalDecisionObservation? = null,
    val tokensIn: Int,
    val tokensOut: Int,
)

data class ReflectedVocab(val word: String, val lemma: String, val context: String, val source: String)

data class ReflectedError(val categoryCode: String, val originalText: String, val correctedText: String)

data class PracticalGoalObservation(
    val description: String,
    val evidenceTurnId: String,
    val evidenceText: String,
    val confidence: Double,
)

data class PracticalGoalDecisionObservation(
    val decision: PracticalGoalDecision,
    val evidenceTurnId: String,
    val evidenceText: String,
    val confidence: Double,
)

enum class PracticalGoalDecision {
    Accept,
    Defer,
    Reject,
}
