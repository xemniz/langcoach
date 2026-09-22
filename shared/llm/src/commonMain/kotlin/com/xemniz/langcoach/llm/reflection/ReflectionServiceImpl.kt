package com.xemniz.langcoach.llm.reflection

import com.xemniz.langcoach.core.AppResult
import com.xemniz.langcoach.domain.reflection.ReflectedError
import com.xemniz.langcoach.domain.reflection.ReflectedVocab
import com.xemniz.langcoach.domain.reflection.PracticalGoalObservation
import com.xemniz.langcoach.domain.reflection.PracticalGoalDecision
import com.xemniz.langcoach.domain.reflection.PracticalGoalDecisionObservation
import com.xemniz.langcoach.domain.reflection.ReflectionResult
import com.xemniz.langcoach.domain.reflection.ReflectionService
import com.xemniz.langcoach.domain.reflection.SessionTranscript
import com.xemniz.langcoach.domain.reflection.UserModelUpdate
import com.xemniz.langcoach.domain.session.ObjectiveEvaluation
import com.xemniz.langcoach.domain.session.ObjectiveOutcome
import com.xemniz.langcoach.domain.session.SessionPlan
import com.xemniz.langcoach.llm.chat.ChatCompletionsClient
import com.xemniz.langcoach.llm.chat.ErrorClassificationResult
import com.xemniz.langcoach.llm.chat.ObjectiveEvaluationResult
import com.xemniz.langcoach.llm.chat.PracticalGoalObservationResult
import com.xemniz.langcoach.llm.chat.SessionSummaryResult
import com.xemniz.langcoach.llm.chat.UserModelResult
import com.xemniz.langcoach.llm.chat.VocabExtractionResult
import com.xemniz.langcoach.llm.chat.errorClassificationSchema
import com.xemniz.langcoach.llm.chat.objectiveEvaluationSchema
import com.xemniz.langcoach.llm.chat.practicalGoalObservationSchema
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
        transcript: SessionTranscript,
        sessionPlan: SessionPlan?,
        tentativePracticalGoal: String?,
        allowedCategoryCodes: List<String>,
    ): AppResult<ReflectionResult> {
        val renderedTranscript = transcript.render().ifBlank { "(no conversation captured)" }

        val recentVocabList = if (recentVocab.isEmpty()) "(none)" else recentVocab.joinToString(", ")

        val vocabSystemPrompt = "You select vocabulary worth adding to a $targetLang learner's spaced-repetition deck after a tutoring session. The learner's level is $level.\n\nReturn AT MOST 8 items. Pick only items that meet ALL of:\n- They appear in the transcript (either user said it or tutor said it).\n- They are useful at level $level or one level above (just beyond reach is best). Skip items obviously below the learner's level.\n- They are NOT in this list of words the learner already has: $recentVocabList\n- They are NOT proper names, numbers, or pure cognates with $nativeLang.\n- They are content words — verbs, nouns, adjectives, adverbs, useful collocations or short phrases. Skip function words (articles, prepositions, conjunctions) unless part of an idiomatic chunk.\n\nFor each item set source to \"spoken\" if the learner produced it themselves, or \"heard\" if only the tutor used it. Prefer items the learner spoke when they look like edge-of-ability words (paused, half-formed, immediately followed by a correction).\n\nIf nothing meets the bar, return an empty array."

        val vocabResult = chat.structured(
            systemPrompt = vocabSystemPrompt,
            userPrompt = "Native language: $nativeLang. Target language: $targetLang.\n\nTranscript:\n$renderedTranscript",
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
            userPrompt = "Native language: $nativeLang. Target language: $targetLang.\n\nAllowed category codes: ${allowedCategoryCodes.joinToString()}\n\nTranscript:\n$renderedTranscript",
            schemaName = "error_classification",
            schema = errorClassificationSchema(allowedCategoryCodes),
            deserializer = ErrorClassificationResult.serializer(),
        )
        val errors = when (errorResult) {
            is AppResult.Failure -> return errorResult
            is AppResult.Success -> errorResult.value
        }

        val summaryResult = chat.structured(
            systemPrompt = "You create an evidence-based lesson record. Write a 60 to 100 word third-person summary plus: one demonstrated strength, one next teaching step, and one small assignment that can be checked independently in the next lesson. Do not claim unobserved mastery. Plain text only.",
            userPrompt = "Native language: $nativeLang. Target language: $targetLang.\n\nTranscript:\n$renderedTranscript",
            schemaName = "session_summary",
            schema = summarySchema(),
            deserializer = SessionSummaryResult.serializer(),
        )
        val summary = when (summaryResult) {
            is AppResult.Failure -> return summaryResult
            is AppResult.Success -> summaryResult.value
        }

        val goalResult = chat.structured(
            systemPrompt = """
                Identify at most one practical language-learning goal directly supported by a learner turn.
                A practical goal is a real-world situation where the learner wants or needs to use the target language.
                An interest, enjoyable topic, passing event, or tutor suggestion is not by itself a goal.
                Set hasGoal=false unless the learner's own words provide clear evidence. When true, cite the exact
                learner turn ID and a short exact quote. The description should be concise and action-oriented.

                The current tentative goal is: ${tentativePracticalGoal ?: "(none)"}.
                If the learner explicitly accepts, rejects, or defers making that goal a direction for future
                lessons, set decision accordingly and cite that exact learner response. Otherwise use None.
            """.trimIndent(),
            userPrompt = "Target language: $targetLang\n\nTranscript:\n$renderedTranscript",
            schemaName = "practical_goal_observation",
            schema = practicalGoalObservationSchema(),
            deserializer = PracticalGoalObservationResult.serializer(),
        )
        val goal = when (goalResult) {
            is AppResult.Failure -> return goalResult
            is AppResult.Success -> goalResult.value
        }
        val practicalGoalObservation = goal.value.takeIf { it.hasGoal }?.let { value ->
            val description = value.description ?: return@let null
            val evidenceTurnId = value.evidenceTurnId ?: return@let null
            val evidenceText = value.evidenceText ?: return@let null
            PracticalGoalObservation(
                description = description,
                evidenceTurnId = evidenceTurnId,
                evidenceText = evidenceText,
                confidence = value.confidence.coerceIn(0.0, 1.0),
            )
        }
        val practicalGoalDecision = goal.value.decision
            .takeIf { it != "None" }
            ?.let { decision ->
                val evidenceTurnId = goal.value.evidenceTurnId ?: return@let null
                val evidenceText = goal.value.evidenceText ?: return@let null
                val parsed = runCatching { PracticalGoalDecision.valueOf(decision) }.getOrNull()
                    ?: return@let null
                PracticalGoalDecisionObservation(
                    decision = parsed,
                    evidenceTurnId = evidenceTurnId,
                    evidenceText = evidenceText,
                    confidence = goal.value.confidence.coerceIn(0.0, 1.0),
                )
            }

        val objectiveResult = sessionPlan?.let { plan ->
            chat.structured(
                systemPrompt = """
                    You evaluate one language-learning objective from an ordered tutoring transcript.
                    Judge only evidence in learner turns. Never infer mastery from the tutor's words.

                    Outcome rules:
                    - NotObserved: the transcript contains no clear attempt at the planned target, or
                      the relevant audio/transcript is too uncertain to judge.
                    - Attempted: the learner clearly attempts the target but does not demonstrate it
                      accurately.
                    - AchievedWithHelp: the learner succeeds after a direct answer, model, correction,
                      sentence completion, or explicit form in the immediately preceding tutor turn.
                    - AchievedIndependently: the learner accurately produces the target for a genuine
                      communicative purpose without copying a tutor-supplied answer.

                    For NotObserved, evidenceTurnId and evidenceText must be null. Otherwise cite one
                    exact learner turn ID and a short exact quote. Confidence measures confidence in
                    this classification, not learner ability.
                """.trimIndent(),
                userPrompt = """
                    Target language: $targetLang
                    Objective kind: ${plan.kind.name}
                    Objective: ${plan.objective}
                    Target: ${plan.target ?: "(none)"}
                    Success criteria: ${plan.successCriteria}

                    Ordered transcript:
                    $renderedTranscript
                """.trimIndent(),
                schemaName = "objective_evaluation",
                schema = objectiveEvaluationSchema(),
                deserializer = ObjectiveEvaluationResult.serializer(),
            )
        }
        val objectiveEvaluation = when (objectiveResult) {
            is AppResult.Failure -> return objectiveResult
            is AppResult.Success -> {
                val value = objectiveResult.value.value
                ObjectiveOutcome.entries
                    .firstOrNull { it.name == value.outcome }
                    ?.let { outcome ->
                        ObjectiveEvaluation(
                            outcome = outcome,
                            evidenceTurnId = value.evidenceTurnId,
                            evidenceText = value.evidenceText,
                            confidence = value.confidence.coerceIn(0.0, 1.0),
                        )
                    }
            }
            null -> null
        }

        val objectiveTokensIn = (objectiveResult as? AppResult.Success)?.value?.tokensIn ?: 0
        val objectiveTokensOut = (objectiveResult as? AppResult.Success)?.value?.tokensOut ?: 0
        val tokensIn = vocab.tokensIn + errors.tokensIn + summary.tokensIn + goal.tokensIn + objectiveTokensIn
        val tokensOut = vocab.tokensOut + errors.tokensOut + summary.tokensOut + goal.tokensOut + objectiveTokensOut

        return AppResult.Success(
            ReflectionResult(
                newVocab = vocab.value.items.map { ReflectedVocab(it.word, it.lemma, it.context, it.source) },
                errors = errors.value.items.map { ReflectedError(it.categoryCode, it.originalText, it.correctedText) },
                summary = summary.value.summary,
                strength = summary.value.strength,
                nextStep = summary.value.nextStep,
                assignment = summary.value.assignment,
                objectiveEvaluation = objectiveEvaluation,
                practicalGoalObservation = practicalGoalObservation,
                practicalGoalDecision = practicalGoalDecision,
                tokensIn = tokensIn,
                tokensOut = tokensOut,
            )
        )
    }

    override suspend fun updateUserModel(
        targetLang: String,
        nativeLang: String,
        previousModel: String?,
        transcript: SessionTranscript,
        sessionSummary: String,
    ): AppResult<UserModelUpdate> {
        val renderedTranscript = transcript.render().ifBlank { "(no conversation captured)" }

        val systemPrompt = "You maintain a single freeform-text doc representing what a language tutor knows about a learner. Given the previous version of this doc, the most recent session transcript, and the session summary, produce an updated version. Keep what is still true. Update what changed. Add what is genuinely new (interests, personal facts the learner stated about themselves, comfort levels with grammar areas, unfinished conversation threads). Drop anything that seems stale or contradicted. Maximum 500 words. Plain prose, no headers, no markdown, no bullet lists. Third person, written for the tutor as a private memory aid. Be conservative — only state things the learner actually revealed, not inferences."

        val userPrompt = buildString {
            append("Native language: ").append(nativeLang).append(". Target language: ").append(targetLang).append(".\n\n")
            append("Previous user model:\n")
            append(previousModel?.takeIf { it.isNotBlank() } ?: "(no prior memory yet)")
            append("\n\nSession summary:\n")
            append(sessionSummary.ifBlank { "(no summary)" })
            append("\n\nTranscript:\n")
            append(renderedTranscript)
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
