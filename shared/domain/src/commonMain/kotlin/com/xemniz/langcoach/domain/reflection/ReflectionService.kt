package com.xemniz.langcoach.domain.reflection

import com.xemniz.langcoach.core.AppResult

interface ReflectionService {
    suspend fun reflect(
        targetLang: String,
        nativeLang: String,
        level: String,
        recentVocab: List<String>,
        userTranscript: String,
        assistantTranscript: String,
        allowedCategoryCodes: List<String>,
    ): AppResult<ReflectionResult>

    suspend fun updateUserModel(
        targetLang: String,
        nativeLang: String,
        previousModel: String?,
        userTranscript: String,
        assistantTranscript: String,
        sessionSummary: String,
    ): AppResult<UserModelUpdate>
}

data class UserModelUpdate(val content: String, val tokensIn: Int, val tokensOut: Int)
