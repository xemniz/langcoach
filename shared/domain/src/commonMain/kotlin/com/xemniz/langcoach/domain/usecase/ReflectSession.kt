package com.xemniz.langcoach.domain.usecase

import com.xemniz.langcoach.core.AppError
import com.xemniz.langcoach.core.AppResult
import com.xemniz.langcoach.data.db.SessionSummary
import com.xemniz.langcoach.data.db.UsageEntry
import com.xemniz.langcoach.data.db.VocabItem
import com.xemniz.langcoach.data.repo.ErrorRepo
import com.xemniz.langcoach.data.repo.SessionRepo
import com.xemniz.langcoach.data.repo.VocabRepo
import com.xemniz.langcoach.data.repo.UsageRepo
import com.xemniz.langcoach.data.repo.PracticalGoalStore
import com.xemniz.langcoach.domain.reflection.ReflectionService
import com.xemniz.langcoach.domain.reflection.SessionTranscript
import com.xemniz.langcoach.domain.reflection.TranscriptSpeaker
import com.xemniz.langcoach.domain.reflection.TranscriptTurn
import com.xemniz.langcoach.domain.session.ObjectiveEvaluationPolicy
import com.xemniz.langcoach.domain.session.ObjectiveOutcome
import com.xemniz.langcoach.domain.session.SessionObjectiveKind
import com.xemniz.langcoach.domain.session.SessionPlan
import com.xemniz.langcoach.domain.session.vocabularyReviewRating
import com.xemniz.langcoach.domain.reflection.PracticalGoalDecision
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.Clock

class ReflectSession(
    private val reflectionService: ReflectionService,
    private val vocabRepo: VocabRepo,
    private val errorRepo: ErrorRepo,
    private val sessionRepo: SessionRepo,
    private val updateUserModel: UpdateUserModel,
    private val objectiveEvaluationPolicy: ObjectiveEvaluationPolicy,
    private val scheduleVocabReview: ScheduleVocabReview,
    private val recordPracticalGoal: RecordPracticalGoal,
    private val practicalGoals: PracticalGoalStore,
    private val usageRepo: UsageRepo,
) : LessonReflectionProcessor {
    override suspend fun process(sessionId: Long): AppResult<Int> = invoke(sessionId)

    suspend operator fun invoke(
        sessionId: Long,
        atMillis: Long = Clock.System.now().toEpochMilliseconds(),
    ): AppResult<Int> {
        val existing = sessionRepo.byId(sessionId)
            ?: return AppResult.Failure(AppError.Storage("Lesson $sessionId was not found"))
        if (existing.processingState == "Completed") return AppResult.Success(0)
        if (!sessionRepo.markReflectionStarted(sessionId, atMillis)) {
            return AppResult.Failure(AppError.Storage("Lesson $sessionId could not be claimed for processing"))
        }
        return try {
            processClaimed(existing, atMillis)
        } catch (failure: Throwable) {
            if (failure is CancellationException) throw failure
            val message = failure.message ?: "Lesson processing failed"
            sessionRepo.markReflectionFailed(sessionId, message)
            AppResult.Failure(AppError.Storage(message))
        }
    }

    private suspend fun processClaimed(
        existing: SessionSummary,
        atMillis: Long,
    ): AppResult<Int> {
        val sessionId = existing.id
        val transcript = SessionTranscript(
            sessionRepo.transcriptTurns(sessionId).map { turn ->
                TranscriptTurn(
                    id = turn.turnId,
                    speaker = TranscriptSpeaker.valueOf(turn.speaker),
                    text = turn.text,
                )
            },
        )
        val targetLang = existing.targetLang
        val nativeLang = existing.nativeLang
        val level = existing.level
        if (targetLang.isBlank() || nativeLang.isBlank() || level.isBlank()) {
            val error = "Lesson is missing its original language or level context"
            sessionRepo.markReflectionFailed(sessionId, error)
            return AppResult.Failure(AppError.Storage(error))
        }

        val recentVocab = vocabRepo.recentWords(targetLang, limit = 100)
        val dueVocabulary = vocabRepo.getDue(atMillis, targetLang)
        val categories = errorRepo.listCategories()
        val allowedCodes = categories.map { it.code }
        val codeToId = categories.associate { it.code to it.id }
        val sessionPlan = existing.toSessionPlan()
        val tentativeGoal = existing.tentativePracticalGoalId
            ?.let { practicalGoals.byId(it) }
            ?.takeIf { it.status == com.xemniz.langcoach.data.db.PracticalGoalStatus.Tentative }
        val result = reflectionService.reflect(
            targetLang = targetLang,
            nativeLang = nativeLang,
            level = level,
            recentVocab = recentVocab,
            transcript = transcript,
            sessionPlan = sessionPlan,
            tentativePracticalGoal = tentativeGoal?.description,
            allowedCategoryCodes = allowedCodes,
        )
        if (result is AppResult.Failure) {
            sessionRepo.markReflectionFailed(sessionId, result.error.message)
            return result
        }
        val reflected = (result as AppResult.Success).value

        reflected.newVocab.forEach { vocabulary ->
            vocabRepo.insertIfNew(
                VocabItem(
                    word = vocabulary.word,
                    lemma = vocabulary.lemma,
                    context = vocabulary.context,
                    targetLang = targetLang,
                    dueAt = atMillis,
                    createdAt = atMillis,
                    source = vocabulary.source,
                ),
            )
        }
        errorRepo.deleteForSession(sessionId)
        reflected.errors.forEach { error ->
            val categoryId = codeToId[error.categoryCode] ?: return@forEach
            errorRepo.logInstance(
                categoryId = categoryId,
                sessionId = sessionId,
                originalText = error.originalText,
                correctedText = error.correctedText,
                atMillis = atMillis,
            )
        }
        reflected.objectiveEvaluation?.let { proposed ->
            val validated = sessionPlan?.let {
                objectiveEvaluationPolicy.validate(it, transcript, proposed)
            } ?: proposed
            val previouslyHelped = sessionPlan?.let { plan ->
                sessionRepo.recentForLanguage(targetLang, limit = 10).any { previous ->
                    previous.id != sessionId &&
                        previous.objectiveId == plan.objectiveId &&
                        previous.objectiveOutcome == ObjectiveOutcome.AchievedWithHelp.name
                }
            } ?: false
            val evaluation = if (
                validated.outcome == ObjectiveOutcome.AchievedIndependently && previouslyHelped
            ) {
                validated.copy(outcome = ObjectiveOutcome.RetainedLater)
            } else {
                validated
            }
            sessionRepo.updateObjectiveEvaluation(
                id = sessionId,
                outcome = evaluation.outcome.name,
                evidenceTurnId = evaluation.evidenceTurnId,
                evidenceText = evaluation.evidenceText,
                confidence = evaluation.confidence,
            )
            if (
                existing.objectiveOutcome == null &&
                sessionPlan?.kind == SessionObjectiveKind.VocabularyRetrieval &&
                evaluation.confidence >= OBJECTIVE_ACTION_CONFIDENCE
            ) {
                val rating = evaluation.outcome.vocabularyReviewRating()
                val item = dueVocabulary.firstOrNull {
                    it.word.equals(sessionPlan.target, ignoreCase = true)
                }
                if (item != null && rating != null) scheduleVocabReview(item, rating, atMillis)
            }
        }
        reflected.practicalGoalObservation?.let { observation ->
            recordPracticalGoal.inferFromObservation(
                targetLang = targetLang,
                sourceSessionId = sessionId,
                transcript = transcript,
                observation = observation,
                atMillis = atMillis,
            )
        }
        if (tentativeGoal != null) {
            reflected.practicalGoalDecision?.let { observation ->
                val response = when (observation.decision) {
                    PracticalGoalDecision.Accept -> PracticalGoalResponse.Accept
                    PracticalGoalDecision.Defer -> PracticalGoalResponse.Defer
                    PracticalGoalDecision.Reject -> PracticalGoalResponse.Reject
                }
                recordPracticalGoal.respondFromObservation(
                    goalId = tentativeGoal.id,
                    response = response,
                    transcript = transcript,
                    evidenceTurnId = observation.evidenceTurnId,
                    evidenceText = observation.evidenceText,
                    confidence = observation.confidence,
                    atMillis = atMillis,
                )
            }
        }
        val memoryUsage = updateUserModel(
            transcript = transcript,
            sessionSummary = reflected.summary,
            sessionId = sessionId,
            targetLang = targetLang,
            nativeLang = nativeLang,
            atMillis = atMillis,
        )
        if (memoryUsage is AppResult.Failure) {
            sessionRepo.markReflectionFailed(sessionId, memoryUsage.error.message)
            return memoryUsage
        }
        val appliedMemoryUsage = (memoryUsage as AppResult.Success).value
        val extraTokensIn = appliedMemoryUsage.tokensIn
        val extraTokensOut = appliedMemoryUsage.tokensOut
        usageRepo.replaceForSessionEndpoint(
            UsageEntry(
                createdAt = atMillis,
                sessionId = sessionId,
                endpoint = "lesson-processing",
                model = "gpt-4o-2024-11-20",
                tokensIn = reflected.tokensIn + extraTokensIn,
                tokensOut = reflected.tokensOut + extraTokensOut,
                costCents = 0,
            ),
        )
        sessionRepo.completeReflection(
            id = sessionId,
            summary = reflected.summary,
            strength = reflected.strength,
            nextStep = reflected.nextStep,
            assignment = reflected.assignment,
            tokensIn = existing.tokensIn + reflected.tokensIn + extraTokensIn,
            tokensOut = existing.tokensOut + reflected.tokensOut + extraTokensOut,
            completedAt = atMillis,
        )
        return AppResult.Success(reflected.newVocab.size)
    }

    private fun SessionSummary.toSessionPlan(): SessionPlan? {
        val id = objectiveId ?: return null
        val kind = objectiveKind?.let { runCatching { SessionObjectiveKind.valueOf(it) }.getOrNull() }
            ?: return null
        val description = objectiveDescription ?: return null
        val criteria = objectiveSuccessCriteria ?: return null
        return SessionPlan(
            version = planVersion ?: SessionPlan.VERSION,
            objectiveId = id,
            kind = kind,
            objective = description,
            target = objectiveTarget,
            openingHint = "",
            successCriteria = criteria,
        )
    }

    private companion object {
        const val OBJECTIVE_ACTION_CONFIDENCE = 0.8
    }
}
