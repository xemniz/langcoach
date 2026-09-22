package com.xemniz.langcoach.data.repo

import com.xemniz.langcoach.data.db.SessionDao
import com.xemniz.langcoach.data.db.SessionSummary
import kotlinx.coroutines.flow.Flow
import com.xemniz.langcoach.data.db.SessionTurn

class SessionRepo(private val dao: SessionDao) {
    suspend fun create(
        startedAt: Long,
        planVersion: String? = null,
        objectiveId: String? = null,
        objectiveKind: String? = null,
        objectiveDescription: String? = null,
        objectiveTarget: String? = null,
        objectiveSuccessCriteria: String? = null,
        nativeLang: String = "",
        targetLang: String = "",
        level: String = "",
    ): Long = dao.insert(
        SessionSummary(
            startedAt = startedAt,
            planVersion = planVersion,
            objectiveId = objectiveId,
            objectiveKind = objectiveKind,
            objectiveDescription = objectiveDescription,
            objectiveTarget = objectiveTarget,
            objectiveSuccessCriteria = objectiveSuccessCriteria,
            nativeLang = nativeLang,
            targetLang = targetLang,
            level = level,
        ),
    )

    suspend fun finish(
        id: Long,
        endedAt: Long,
        summary: String,
        tokensIn: Int,
        tokensOut: Int,
        costCents: Int,
    ) = dao.finish(id, endedAt, summary, tokensIn, tokensOut, costCents)

    suspend fun recent(limit: Int = 3): List<SessionSummary> = dao.recent(limit)

    suspend fun recentForLanguage(targetLang: String, limit: Int = 3): List<SessionSummary> =
        dao.recentForLanguage(targetLang, limit)

    suspend fun finishAndQueue(
        id: Long,
        endedAt: Long,
        tokensIn: Int,
        tokensOut: Int,
        costCents: Int,
        turns: List<SessionTurn>,
    ) = dao.finishAndQueue(
        id = id,
        endedAt = endedAt,
        tokensIn = tokensIn,
        tokensOut = tokensOut,
        costCents = costCents,
        turns = turns,
    )

    suspend fun transcriptTurns(id: Long): List<SessionTurn> = dao.turns(id)

    suspend fun pendingReflectionIds(): List<Long> = dao.pendingReflectionIds()
    suspend fun recoverInterruptedReflections() = dao.recoverInterruptedReflections()
    suspend fun markReflectionStarted(id: Long, startedAt: Long): Boolean =
        dao.markReflectionStarted(id, startedAt) == 1
    suspend fun markReflectionFailed(id: Long, error: String) = dao.markReflectionFailed(id, error)
    suspend fun completeReflection(
        id: Long,
        summary: String,
        strength: String,
        nextStep: String,
        assignment: String,
        tokensIn: Int,
        tokensOut: Int,
        completedAt: Long,
    ) = dao.completeReflection(
        id, summary, strength, nextStep, assignment, tokensIn, tokensOut, completedAt,
    )

    fun observeAll(): Flow<List<SessionSummary>> = dao.observeAll()

    suspend fun byId(id: Long): SessionSummary? = dao.byId(id)

    suspend fun updateObjectiveEvaluation(
        id: Long,
        outcome: String,
        evidenceTurnId: String?,
        evidenceText: String?,
        confidence: Double,
    ) = dao.updateObjectiveEvaluation(
        id = id,
        outcome = outcome,
        evidenceTurnId = evidenceTurnId,
        evidenceText = evidenceText,
        confidence = confidence,
    )
}
