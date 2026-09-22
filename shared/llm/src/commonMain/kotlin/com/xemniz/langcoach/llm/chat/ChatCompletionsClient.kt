package com.xemniz.langcoach.llm.chat

import com.xemniz.langcoach.core.AppError
import com.xemniz.langcoach.core.AppResult
import com.xemniz.langcoach.data.SecureStorage
import com.xemniz.langcoach.domain.OPENAI_API_KEY
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json

class ChatCompletionsClient(
    private val httpClient: HttpClient,
    private val storage: SecureStorage,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun <T : Any> structured(
        model: String = DEFAULT_MODEL,
        systemPrompt: String,
        userPrompt: String,
        schemaName: String,
        schema: kotlinx.serialization.json.JsonElement,
        deserializer: kotlinx.serialization.DeserializationStrategy<T>,
    ): AppResult<StructuredOutput<T>> {
        val key = storage.read(OPENAI_API_KEY)
            ?: return AppResult.Failure(AppError.Storage("API key not set"))
        val request = WireChatRequest(
            model = model,
            messages = listOf(
                WireChatMessage("system", systemPrompt),
                WireChatMessage("user", userPrompt),
            ),
            response_format = WireResponseFormat(
                json_schema = WireJsonSchema(name = schemaName, schema = schema),
            ),
        )
        return runCatching {
            val resp: HttpResponse = httpClient.post("https://api.openai.com/v1/chat/completions") {
                contentType(ContentType.Application.Json)
                headers { append(HttpHeaders.Authorization, "Bearer $key") }
                setBody(request)
            }
            if (resp.status != HttpStatusCode.OK) {
                return AppResult.Failure(AppError.Api("OpenAI request failed", resp.status.value))
            }
            val parsed: WireChatResponse = resp.body()
            val content = parsed.choices.firstOrNull()?.message?.content
                ?: return AppResult.Failure(AppError.Api("Empty response"))
            val value = json.decodeFromString(deserializer, content)
            val usage = parsed.usage
            AppResult.Success(StructuredOutput(value, usage?.prompt_tokens ?: 0, usage?.completion_tokens ?: 0))
        }.getOrElse { t ->
            if (t is kotlin.coroutines.cancellation.CancellationException) throw t
            AppResult.Failure(AppError.Network("Network request failed"))
        }
    }

    companion object {
        const val DEFAULT_MODEL = "gpt-4o-2024-11-20"
    }
}

data class StructuredOutput<T>(val value: T, val tokensIn: Int, val tokensOut: Int)
