package com.xemniz.langcoach.domain.usecase

import com.xemniz.langcoach.core.AppError
import com.xemniz.langcoach.core.AppResult
import com.xemniz.langcoach.data.SecureStorage
import com.xemniz.langcoach.domain.OPENAI_API_KEY
import com.xemniz.langcoach.domain.OpenAIClient

class VerifyApiKey(
    private val client: OpenAIClient,
    private val storage: SecureStorage,
) {
    suspend operator fun invoke(key: String): AppResult<Unit> {
        if (key.isBlank()) return AppResult.Failure(AppError.Api("Empty key"))
        val trimmed = key.trim()
        return when (val r = client.verifyKey(trimmed)) {
            is AppResult.Success -> {
                storage.save(OPENAI_API_KEY, trimmed)
                AppResult.Success(Unit)
            }
            is AppResult.Failure -> r
        }
    }
}
