package com.xemniz.langcoach.llm.chat

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

internal fun vocabExtractionSchema(): JsonElement = buildJsonObject {
    put("type", "object")
    put("additionalProperties", false)
    putJsonArray("required") { add("items") }
    putJsonObject("properties") {
        putJsonObject("items") {
            put("type", "array")
            putJsonObject("items") {
                put("type", "object")
                put("additionalProperties", false)
                putJsonArray("required") {
                    add("word"); add("lemma"); add("context"); add("source")
                }
                putJsonObject("properties") {
                    putJsonObject("word") { put("type", "string") }
                    putJsonObject("lemma") { put("type", "string") }
                    putJsonObject("context") { put("type", "string") }
                    putJsonObject("source") {
                        put("type", "string")
                        putJsonArray("enum") { add("spoken"); add("heard") }
                    }
                }
            }
        }
    }
}

internal fun errorClassificationSchema(allowedCategoryCodes: List<String>): JsonElement = buildJsonObject {
    put("type", "object")
    put("additionalProperties", false)
    putJsonArray("required") { add("items") }
    putJsonObject("properties") {
        putJsonObject("items") {
            put("type", "array")
            putJsonObject("items") {
                put("type", "object")
                put("additionalProperties", false)
                putJsonArray("required") {
                    add("categoryCode"); add("originalText"); add("correctedText")
                }
                putJsonObject("properties") {
                    putJsonObject("categoryCode") {
                        put("type", "string")
                        putJsonArray("enum") { allowedCategoryCodes.forEach { add(it) } }
                    }
                    putJsonObject("originalText") { put("type", "string") }
                    putJsonObject("correctedText") { put("type", "string") }
                }
            }
        }
    }
}

internal fun summarySchema(): JsonElement = buildJsonObject {
    put("type", "object")
    put("additionalProperties", false)
    putJsonArray("required") { add("summary") }
    putJsonObject("properties") {
        putJsonObject("summary") { put("type", "string") }
    }
}

internal fun userModelSchema(): JsonElement = buildJsonObject {
    put("type", "object")
    put("additionalProperties", false)
    putJsonArray("required") { add("content") }
    putJsonObject("properties") {
        putJsonObject("content") { put("type", "string") }
    }
}
