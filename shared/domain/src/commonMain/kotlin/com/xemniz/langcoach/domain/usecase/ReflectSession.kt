package com.xemniz.langcoach.domain.usecase

import com.xemniz.langcoach.core.AppResult
import com.xemniz.langcoach.data.db.VocabItem
import com.xemniz.langcoach.data.prefs.ProfilePrefs
import com.xemniz.langcoach.data.repo.ErrorRepo
import com.xemniz.langcoach.data.repo.SessionRepo
import com.xemniz.langcoach.data.repo.VocabRepo
import com.xemniz.langcoach.domain.reflection.ReflectionService
import com.xemniz.langcoach.domain.reflection.SessionTranscript
import com.xemniz.langcoach.domain.session.ObjectiveEvaluationPolicy
import com.xemniz.langcoach.domain.session.SessionObjectiveKind
import com.xemniz.langcoach.domain.session.SessionPlan
import com.xemniz.langcoach.domain.session.vocabularyReviewRating
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock

class ReflectSession(
    private val reflectionService: ReflectionService,
    private val profilePrefs: ProfilePrefs,
    private val vocabRepo: VocabRepo,
    private val errorRepo: ErrorRepo,
    private val sessionRepo: SessionRepo,
    private val updateUserModel: UpdateUserModel,
    private val objectiveEvaluationPolicy: ObjectiveEvaluationPolicy,
    private val scheduleVocabReview: ScheduleVocabReview,
) {
    suspend operator fun invoke(
        sessionId: Long,
        transcript: SessionTranscript,
        atMillis: Long = Clock.System.now().toEpochMilliseconds(),
    ): AppResult<Int> {
        val targetLang = profilePrefs.targetLang.first()
        val nativeLang = profilePrefs.nativeLang.first()
        val level = profilePrefs.level.first().name
        val recentVocab = vocabRepo.recentWords(targetLang, limit = 100)
        val dueVocabulary = vocabRepo.getDue(atMillis)
        val categories = errorRepo.listCategories()
        val allowedCodes = categories.map { it.code }
        val codeToId = categories.associate { it.code to it.id }
        val existing = sessionRepo.byId(sessionId)
        val sessionPlan = existing?.let { session ->
            val objectiveId = session.objectiveId ?: return@let null
            val kind = session.objectiveKind
                ?.let { runCatching { SessionObjectiveKind.valueOf(it) }.getOrNull() }
                ?: return@let null
            val objective = session.objectiveDescription ?: return@let null
            val successCriteria = session.objectiveSuccessCriteria ?: return@let null
            SessionPlan(
                version = session.planVersion ?: SessionPlan.VERSION,
                objectiveId = objectiveId,
                kind = kind,
                objective = objective,
                target = session.objectiveTarget,
                openingHint = "",
                successCriteria = successCriteria,
            )
        }

        val result = reflectionService.reflect(
            targetLang = targetLang,
            nativeLang = nativeLang,
            level = level,
            recentVocab = recentVocab,
            transcript = transcript,
            sessionPlan = sessionPlan,
            allowedCategoryCodes = allowedCodes,
        )
        return when (result) {
            is AppResult.Failure -> result
            is AppResult.Success -> {
                val r = result.value
                r.newVocab.forEach { v ->
                    vocabRepo.insertIfNew(
                        VocabItem(
                            word = v.word,
                            lemma = v.lemma,
                            context = v.context,
                            targetLang = targetLang,
                            dueAt = atMillis,
                            createdAt = atMillis,
                            source = v.source,
                        ),
                    )
                }
                r.errors.forEach { e ->
                    val catId = codeToId[e.categoryCode] ?: return@forEach
                    errorRepo.logInstance(
                        categoryId = catId,
                        sessionId = sessionId,
                        originalText = e.originalText,
                        correctedText = e.correctedText,
                        atMillis = atMillis,
                    )
                }
                if (existing != null) {
                    sessionRepo.finish(
                        id = sessionId,
                        endedAt = existing.endedAt ?: atMillis,
                        summary = r.summary,
                        tokensIn = existing.tokensIn + r.tokensIn,
                        tokensOut = existing.tokensOut + r.tokensOut,
                        costCents = existing.costCents,
                    )
                }
                r.objectiveEvaluation?.let { proposedEvaluation ->
                    val evaluation = sessionPlan?.let {
                        objectiveEvaluationPolicy.validate(it, transcript, proposedEvaluation)
                    } ?: proposedEvaluation
                    sessionRepo.updateObjectiveEvaluation(
                        id = sessionId,
                        outcome = evaluation.outcome.name,
                        evidenceTurnId = evaluation.evidenceTurnId,
                        evidenceText = evaluation.evidenceText,
                        confidence = evaluation.confidence,
                    )
                    if (
                        sessionPlan?.kind == SessionObjectiveKind.VocabularyRetrieval &&
                        evaluation.confidence >= OBJECTIVE_ACTION_CONFIDENCE
                    ) {
                        val rating = evaluation.outcome.vocabularyReviewRating()
                        val target = sessionPlan.target
                        val item = dueVocabulary.firstOrNull {
                            it.targetLang == targetLang &&
                                it.word.equals(target, ignoreCase = true)
                        }
                        if (item != null && rating != null) {
                            scheduleVocabReview(item, rating, atMillis)
                        }
                    }
                }
                updateUserModel(
                    transcript = transcript,
                    sessionSummary = r.summary,
                    atMillis = atMillis,
                )
                AppResult.Success(r.newVocab.size)
            }
        }
    }

    private companion object {
        const val OBJECTIVE_ACTION_CONFIDENCE = 0.8
    }
}
