package com.xemniz.langcoach.domain.usecase

import com.xemniz.langcoach.core.AppResult
import com.xemniz.langcoach.core.AppError
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
        sessionId: Long,
        targetLang: String,
        nativeLang: String,
        atMillis: Long = Clock.System.now().toEpochMilliseconds(),
    ): AppResult<TokenUsage> {
        val previous = userModelRepo.get(targetLang)
        userModelRepo.appliedLessonUsage(targetLang, sessionId)?.let { applied ->
            return AppResult.Success(TokenUsage(applied.tokensIn, applied.tokensOut))
        }
        val result = reflectionService.updateUserModel(
            targetLang, nativeLang, previous?.content,
            transcript, sessionSummary,
        )
        return when (result) {
            is AppResult.Failure -> result
            is AppResult.Success -> {
                val applied = userModelRepo.applyLessonIfUnchanged(
                    targetLang = targetLang,
                    content = result.value.content,
                    sessionId = sessionId,
                    expectedUpdatedAt = previous?.updatedAt,
                    atMillis = atMillis,
                    previousAppliedSessionIds = previous?.appliedSessionIds.orEmpty(),
                    tokensIn = result.value.tokensIn,
                    tokensOut = result.value.tokensOut,
                )
                if (!applied) {
                    val latest = userModelRepo.get(targetLang)
                    val previousIds = previous?.appliedSessionIds.orEmpty()
                    if (latest?.appliedSessionIds != previousIds) {
                        return AppResult.Failure(
                            AppError.Storage("Coach memory changed while this lesson was processing; retrying"),
                        )
                    }
                }
                AppResult.Success(TokenUsage(result.value.tokensIn, result.value.tokensOut))
            }
        }
    }

    data class TokenUsage(val tokensIn: Int, val tokensOut: Int)
}
