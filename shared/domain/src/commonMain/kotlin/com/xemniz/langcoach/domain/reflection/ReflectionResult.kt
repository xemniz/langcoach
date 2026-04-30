package com.xemniz.langcoach.domain.reflection

data class ReflectionResult(
    val newVocab: List<ReflectedVocab>,
    val errors: List<ReflectedError>,
    val summary: String,
    val tokensIn: Int,
    val tokensOut: Int,
)

data class ReflectedVocab(val word: String, val lemma: String, val context: String, val source: String)

data class ReflectedError(val categoryCode: String, val originalText: String, val correctedText: String)
