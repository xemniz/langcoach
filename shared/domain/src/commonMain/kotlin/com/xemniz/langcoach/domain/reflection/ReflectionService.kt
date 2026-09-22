package com.xemniz.langcoach.domain.reflection

import com.xemniz.langcoach.core.AppResult
import com.xemniz.langcoach.domain.session.SessionPlan

interface ReflectionService {
    suspend fun reflect(
        targetLang: String,
        nativeLang: String,
        level: String,
        recentVocab: List<String>,
        transcript: SessionTranscript,
        sessionPlan: SessionPlan?,
        tentativePracticalGoal: String?,
        allowedCategoryCodes: List<String>,
    ): AppResult<ReflectionResult>

    suspend fun updateUserModel(
        targetLang: String,
        nativeLang: String,
        previousModel: String?,
        transcript: SessionTranscript,
        sessionSummary: String,
    ): AppResult<UserModelUpdate>
}

data class UserModelUpdate(val content: String, val tokensIn: Int, val tokensOut: Int)
