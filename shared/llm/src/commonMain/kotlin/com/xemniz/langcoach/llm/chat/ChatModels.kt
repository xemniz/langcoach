package com.xemniz.langcoach.llm.chat

import kotlinx.serialization.Serializable

@Serializable
data class ExtractedVocab(
    val word: String,
    val lemma: String,
    val context: String,
    val source: String,
)

@Serializable
data class ExtractedError(
    val categoryCode: String,
    val originalText: String,
    val correctedText: String,
)

@Serializable
data class VocabExtractionResult(val items: List<ExtractedVocab>)

@Serializable
data class ErrorClassificationResult(val items: List<ExtractedError>)

@Serializable
data class SessionSummaryResult(
    val summary: String,
    val strength: String,
    val nextStep: String,
    val assignment: String,
)

@Serializable
data class PracticalGoalObservationResult(
    val hasGoal: Boolean,
    val description: String?,
    val evidenceTurnId: String?,
    val evidenceText: String?,
    val confidence: Double,
    val decision: String,
)

@Serializable
data class UserModelResult(val content: String)

@Serializable
data class ObjectiveEvaluationResult(
    val outcome: String,
    val evidenceTurnId: String?,
    val evidenceText: String?,
    val confidence: Double,
)
