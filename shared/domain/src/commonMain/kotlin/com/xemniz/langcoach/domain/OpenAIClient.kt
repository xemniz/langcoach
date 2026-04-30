package com.xemniz.langcoach.domain

import com.xemniz.langcoach.core.AppResult

interface OpenAIClient {
    suspend fun verifyKey(key: String): AppResult<Unit>
}
