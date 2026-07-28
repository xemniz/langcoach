package com.xemniz.langcoach.data.repo

import com.xemniz.langcoach.data.db.SessionDao
import com.xemniz.langcoach.data.db.SessionSummary
import kotlinx.coroutines.flow.Flow

class SessionRepo(private val dao: SessionDao) {
    suspend fun create(
        startedAt: Long,
        planVersion: String? = null,
        objectiveId: String? = null,
        objectiveKind: String? = null,
        objectiveDescription: String? = null,
        objectiveTarget: String? = null,
        objectiveSuccessCriteria: String? = null,
    ): Long = dao.insert(
        SessionSummary(
            startedAt = startedAt,
            planVersion = planVersion,
            objectiveId = objectiveId,
            objectiveKind = objectiveKind,
            objectiveDescription = objectiveDescription,
            objectiveTarget = objectiveTarget,
            objectiveSuccessCriteria = objectiveSuccessCriteria,
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
