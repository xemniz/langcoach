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
data class SessionSummaryResult(val summary: String)

@Serializable
data class UserModelResult(val content: String)
