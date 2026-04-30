package com.xemniz.langcoach.llm.realtime

import com.xemniz.langcoach.core.AppError
import com.xemniz.langcoach.core.AppResult
import com.xemniz.langcoach.data.SecureStorage
import com.xemniz.langcoach.domain.OPENAI_API_KEY
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.headers
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json

class OpenAIRealtimeClient(
    private val httpClient: HttpClient,
    private val storage: SecureStorage,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun connect(model: String = DEFAULT_MODEL): AppResult<RealtimeSession> {
        val key = storage.read(OPENAI_API_KEY)
            ?: return AppResult.Failure(AppError.Storage("API key not set"))
        val openWs: suspend () -> DefaultClientWebSocketSession = {
            httpClient.webSocketSession {
                url("wss://api.openai.com/v1/realtime?model=$model")
                headers {
                    append(HttpHeaders.Authorization, "Bearer $key")
                    append("OpenAI-Beta", "realtime=v1")
                }
            }
        }
        return runCatching {
            val ws = openWs()
            val session = RealtimeSession(ws, json, openWs)
            session.startReading()
            session
        }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { AppResult.Failure(AppError.Network(it.message ?: "WebSocket connect failed")) },
        )
    }

    companion object {
        const val DEFAULT_MODEL = "gpt-4o-realtime-preview-2024-12-17"
    }
}
