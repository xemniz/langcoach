package com.xemniz.langcoach.domain.usecase

import com.xemniz.langcoach.core.AppResult
import com.xemniz.langcoach.data.prefs.ProfilePrefs
import com.xemniz.langcoach.data.repo.UserModelRepo
import com.xemniz.langcoach.domain.reflection.ReflectionService
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock

class UpdateUserModel(
    private val reflectionService: ReflectionService,
    private val userModelRepo: UserModelRepo,
    private val profilePrefs: ProfilePrefs,
) {
    suspend operator fun invoke(
        userTranscript: String,
        assistantTranscript: String,
        sessionSummary: String,
        atMillis: Long = Clock.System.now().toEpochMilliseconds(),
    ): AppResult<TokenUsage> {
        val targetLang = profilePrefs.targetLang.first()
        val nativeLang = profilePrefs.nativeLang.first()
        val previous = userModelRepo.getContent()
        val result = reflectionService.updateUserModel(
            targetLang, nativeLang, previous,
            userTranscript, assistantTranscript, sessionSummary,
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
