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
    putJsonArray("required") {
        add("summary"); add("strength"); add("nextStep"); add("assignment")
    }
    putJsonObject("properties") {
        putJsonObject("summary") { put("type", "string") }
        putJsonObject("strength") { put("type", "string") }
        putJsonObject("nextStep") { put("type", "string") }
        putJsonObject("assignment") { put("type", "string") }
    }
}

internal fun practicalGoalObservationSchema(): JsonElement = buildJsonObject {
    put("type", "object")
    put("additionalProperties", false)
    putJsonArray("required") {
        add("hasGoal"); add("description"); add("evidenceTurnId"); add("evidenceText"); add("confidence"); add("decision")
    }
    putJsonObject("properties") {
        putJsonObject("hasGoal") { put("type", "boolean") }
        listOf("description", "evidenceTurnId", "evidenceText").forEach { field ->
            putJsonObject(field) {
                putJsonArray("type") { add("string"); add("null") }
            }
        }
        putJsonObject("confidence") {
            put("type", "number")
            put("minimum", 0.0)
            put("maximum", 1.0)
        }
        putJsonObject("decision") {
            put("type", "string")
            putJsonArray("enum") { add("None"); add("Accept"); add("Defer"); add("Reject") }
        }
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

internal fun objectiveEvaluationSchema(): JsonElement = buildJsonObject {
    put("type", "object")
    put("additionalProperties", false)
    putJsonArray("required") {
        add("outcome")
        add("evidenceTurnId")
        add("evidenceText")
        add("confidence")
    }
    putJsonObject("properties") {
        putJsonObject("outcome") {
            put("type", "string")
            putJsonArray("enum") {
                add("NotObserved")
                add("Attempted")
                add("AchievedWithHelp")
                add("AchievedIndependently")
            }
        }
        putJsonObject("evidenceTurnId") {
            putJsonArray("type") {
                add("string")
                add("null")
            }
        }
        putJsonObject("evidenceText") {
            putJsonArray("type") {
                add("string")
                add("null")
            }
        }
        putJsonObject("confidence") {
            put("type", "number")
            put("minimum", 0.0)
            put("maximum", 1.0)
        }
    }
}
