package com.xemniz.langcoach.llm.reflection

import com.xemniz.langcoach.core.AppResult
import com.xemniz.langcoach.domain.reflection.ReflectedError
import com.xemniz.langcoach.domain.reflection.ReflectedVocab
import com.xemniz.langcoach.domain.reflection.ReflectionResult
import com.xemniz.langcoach.domain.reflection.ReflectionService
import com.xemniz.langcoach.domain.reflection.UserModelUpdate
import com.xemniz.langcoach.llm.chat.ChatCompletionsClient
import com.xemniz.langcoach.llm.chat.ErrorClassificationResult
import com.xemniz.langcoach.llm.chat.SessionSummaryResult
import com.xemniz.langcoach.llm.chat.UserModelResult
import com.xemniz.langcoach.llm.chat.VocabExtractionResult
import com.xemniz.langcoach.llm.chat.errorClassificationSchema
import com.xemniz.langcoach.llm.chat.summarySchema
import com.xemniz.langcoach.llm.chat.userModelSchema
import com.xemniz.langcoach.llm.chat.vocabExtractionSchema

class ReflectionServiceImpl(
    private val chat: ChatCompletionsClient,
) : ReflectionService {

    override suspend fun reflect(
        targetLang: String,
        nativeLang: String,
        level: String,
        recentVocab: List<String>,
        userTranscript: String,
        assistantTranscript: String,
        allowedCategoryCodes: List<String>,
    ): AppResult<ReflectionResult> {
        val transcript = buildString {
            append("USER (in ").append(targetLang).append("):\n")
            append(userTranscript.ifBlank { "(no speech detected)" })
            append("\n\nTUTOR:\n")
            append(assistantTranscript.ifBlank { "(no response)" })
        }

        val recentVocabList = if (recentVocab.isEmpty()) "(none)" else recentVocab.joinToString(", ")

        val vocabSystemPrompt = "You select vocabulary worth adding to a $targetLang learner's spaced-repetition deck after a tutoring session. The learner's level is $level.\n\nReturn AT MOST 8 items. Pick only items that meet ALL of:\n- They appear in the transcript (either user said it or tutor said it).\n- They are useful at level $level or one level above (just beyond reach is best). Skip items obviously below the learner's level.\n- They are NOT in this list of words the learner already has: $recentVocabList\n- They are NOT proper names, numbers, or pure cognates with $nativeLang.\n- They are content words — verbs, nouns, adjectives, adverbs, useful collocations or short phrases. Skip function words (articles, prepositions, conjunctions) unless part of an idiomatic chunk.\n\nFor each item set source to \"spoken\" if the learner produced it themselves, or \"heard\" if only the tutor used it. Prefer items the learner spoke when they look like edge-of-ability words (paused, half-formed, immediately followed by a correction).\n\nIf nothing meets the bar, return an empty array."

        val vocabResult = chat.structured(
            systemPrompt = vocabSystemPrompt,
            userPrompt = "Native language: $nativeLang. Target language: $targetLang.\n\nTranscript:\n$transcript",
            schemaName = "vocab_extraction",
            schema = vocabExtractionSchema(),
            deserializer = VocabExtractionResult.serializer(),
        )
        val vocab = when (vocabResult) {
            is AppResult.Failure -> return vocabResult
            is AppResult.Success -> vocabResult.value
        }

        val errorResult = chat.structured(
            systemPrompt = "You classify mistakes the user made in their $targetLang. For each, pick the closest category from the provided list. Only include genuine errors, not stylistic variation. Output an empty array if none.",
            userPrompt = "Native language: $nativeLang. Target language: $targetLang.\n\nAllowed category codes: ${allowedCategoryCodes.joinToString()}\n\nTranscript:\n$transcript",
            schemaName = "error_classification",
            schema = errorClassificationSchema(allowedCategoryCodes),
            deserializer = ErrorClassificationResult.serializer(),
        )
        val errors = when (errorResult) {
            is AppResult.Failure -> return errorResult
            is AppResult.Success -> errorResult.value
        }

        val summaryResult = chat.structured(
            systemPrompt = "You write short, plain-language session summaries for a language learner. About 60 to 100 words, third person, no markdown, no quotes. Mention topic, level of fluency observed, and one or two things to focus on next time.",
            userPrompt = "Native language: $nativeLang. Target language: $targetLang.\n\nTranscript:\n$transcript",
            schemaName = "session_summary",
            schema = summarySchema(),
            deserializer = SessionSummaryResult.serializer(),
        )
        val summary = when (summaryResult) {
            is AppResult.Failure -> return summaryResult
            is AppResult.Success -> summaryResult.value
        }

        val tokensIn = vocab.tokensIn + errors.tokensIn + summary.tokensIn
        val tokensOut = vocab.tokensOut + errors.tokensOut + summary.tokensOut

        return AppResult.Success(
            ReflectionResult(
                newVocab = vocab.value.items.map { ReflectedVocab(it.word, it.lemma, it.context, it.source) },
                errors = errors.value.items.map { ReflectedError(it.categoryCode, it.originalText, it.correctedText) },
                summary = summary.value.summary,
                tokensIn = tokensIn,
                tokensOut = tokensOut,
            )
        )
    }

    override suspend fun updateUserModel(
        targetLang: String,
        nativeLang: String,
        previousModel: String?,
        userTranscript: String,
        assistantTranscript: String,
        sessionSummary: String,
    ): AppResult<UserModelUpdate> {
        val transcript = buildString {
            append("USER (in ").append(targetLang).append("):\n")
            append(userTranscript.ifBlank { "(no speech detected)" })
            append("\n\nTUTOR:\n")
            append(assistantTranscript.ifBlank { "(no response)" })
        }

        val systemPrompt = "You maintain a single freeform-text doc representing what a language tutor knows about a learner. Given the previous version of this doc, the most recent session transcript, and the session summary, produce an updated version. Keep what is still true. Update what changed. Add what is genuinely new (interests, personal facts the learner stated about themselves, comfort levels with grammar areas, unfinished conversation threads). Drop anything that seems stale or contradicted. Maximum 500 words. Plain prose, no headers, no markdown, no bullet lists. Third person, written for the tutor as a private memory aid. Be conservative — only state things the learner actually revealed, not inferences."

        val userPrompt = buildString {
            append("Native language: ").append(nativeLang).append(". Target language: ").append(targetLang).append(".\n\n")
            append("Previous user model:\n")
            append(previousModel?.takeIf { it.isNotBlank() } ?: "(no prior memory yet)")
            append("\n\nSession summary:\n")
            append(sessionSummary.ifBlank { "(no summary)" })
            append("\n\nTranscript:\n")
            append(transcript)
        }

        val result = chat.structured(
            systemPrompt = systemPrompt,
            userPrompt = userPrompt,
            schemaName = "user_model",
            schema = userModelSchema(),
            deserializer = UserModelResult.serializer(),
        )
        return when (result) {
            is AppResult.Failure -> result
            is AppResult.Success -> AppResult.Success(
                UserModelUpdate(
                    content = result.value.value.content,
                    tokensIn = result.value.tokensIn,
                    tokensOut = result.value.tokensOut,
                )
            )
        }
    }
}
