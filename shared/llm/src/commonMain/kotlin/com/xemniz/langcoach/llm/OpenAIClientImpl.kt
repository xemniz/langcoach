package com.xemniz.langcoach.llm

import com.xemniz.langcoach.core.AppError
import com.xemniz.langcoach.core.AppResult
import com.xemniz.langcoach.domain.OpenAIClient
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlin.coroutines.cancellation.CancellationException

class OpenAIClientImpl(
    private val http: HttpClient,
) : OpenAIClient {
    override suspend fun verifyKey(key: String): AppResult<Unit> {
        return try {
            val response: HttpResponse = http.get("https://api.openai.com/v1/models") {
                headers { append(HttpHeaders.Authorization, "Bearer $key") }
            }
            when (val status = response.status) {
                HttpStatusCode.OK -> AppResult.Success(Unit)
                HttpStatusCode.Unauthorized -> AppResult.Failure(AppError.Api("Invalid API key", 401))
                else -> AppResult.Failure(AppError.Api("OpenAI API error: ${status.value}", status.value))
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            AppResult.Failure(AppError.Network(t.message ?: "Network error"))
        }
    }
}
