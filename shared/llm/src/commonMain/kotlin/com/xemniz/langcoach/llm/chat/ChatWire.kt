package com.xemniz.langcoach.llm.chat

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class WireChatRequest(
    val model: String,
    val messages: List<WireChatMessage>,
    val response_format: WireResponseFormat? = null,
    val temperature: Double = 0.2,
)

@Serializable
internal data class WireChatMessage(
    val role: String,
    val content: String,
)

@Serializable
internal data class WireResponseFormat(
    val type: String = "json_schema",
    val json_schema: WireJsonSchema,
)

@Serializable
internal data class WireJsonSchema(
    val name: String,
    val strict: Boolean = true,
    val schema: JsonElement,
)

@Serializable
internal data class WireChatResponse(
    val id: String? = null,
    val choices: List<WireChoice> = emptyList(),
    val usage: WireUsage? = null,
)

@Serializable
internal data class WireChoice(val message: WireResponseMessage)

@Serializable
internal data class WireResponseMessage(val content: String? = null)

@Serializable
internal data class WireUsage(
    val prompt_tokens: Int = 0,
    val completion_tokens: Int = 0,
    val total_tokens: Int = 0,
)
