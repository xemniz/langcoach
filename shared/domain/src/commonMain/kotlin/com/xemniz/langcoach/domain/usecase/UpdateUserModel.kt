package com.xemniz.langcoach.domain.usecase

import com.xemniz.langcoach.core.AppResult
import com.xemniz.langcoach.data.repo.UserModelRepo
import com.xemniz.langcoach.domain.reflection.ReflectionService
import com.xemniz.langcoach.domain.reflection.SessionTranscript
import kotlinx.datetime.Clock

class UpdateUserModel(
    private val reflectionService: ReflectionService,
    private val userModelRepo: UserModelRepo,
) {
    suspend operator fun invoke(
        transcript: SessionTranscript,
        sessionSummary: String,
        targetLang: String,
        nativeLang: String,
        atMillis: Long = Clock.System.now().toEpochMilliseconds(),
    ): AppResult<TokenUsage> {
        val previous = userModelRepo.getContent()
        val result = reflectionService.updateUserModel(
            targetLang, nativeLang, previous,
            transcript, sessionSummary,
        )
        return when (result) {
            is AppResult.Failure -> result
            is AppResult.Success -> {
                userModelRepo.set(result.value.content, atMillis)
                AppResult.Success(TokenUsage(result.value.tokensIn, result.value.tokensOut))
            }
        }
    }

    data class TokenUsage(val tokensIn: Int, val tokensOut: Int)
}
