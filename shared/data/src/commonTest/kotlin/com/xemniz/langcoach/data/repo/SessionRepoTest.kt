package com.xemniz.langcoach.data.repo

import com.xemniz.langcoach.data.db.SessionDao
import com.xemniz.langcoach.data.db.SessionSummary
import com.xemniz.langcoach.data.db.SessionTurn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionRepoTest {
    @Test
    fun completingReflectionRetainsTranscript() = runBlocking {
        val dao = RecordingSessionDao()
        val repo = SessionRepo(dao)

        repo.completeReflection(
            id = 42,
            summary = "Summary",
            strength = "Strength",
            nextStep = "Next",
            assignment = "Assignment",
            tokensIn = 10,
            tokensOut = 5,
            completedAt = 1234,
        )

        assertEquals(listOf(42L), dao.completedSessionIds)
        assertEquals(emptyList(), dao.deletedSessionIds)
    }
}

private class RecordingSessionDao : SessionDao {
    val completedSessionIds = mutableListOf<Long>()
    val deletedSessionIds = mutableListOf<Long>()

    override suspend fun insert(item: SessionSummary): Long = item.id
    override suspend fun insertTurns(turns: List<SessionTurn>) = Unit
    override suspend fun deleteTurns(sessionId: Long) {
        deletedSessionIds += sessionId
    }
    override suspend fun finish(
        id: Long,
        endedAt: Long,
        summary: String,
        tokensIn: Int,
        tokensOut: Int,
        costCents: Int,
    ) = Unit
    override suspend fun queueReflection(
        id: Long,
        endedAt: Long,
        tokensIn: Int,
        tokensOut: Int,
        costCents: Int,
    ) = Unit
    override suspend fun turns(sessionId: Long): List<SessionTurn> = emptyList()
    override suspend fun pendingReflectionIds(): List<Long> = emptyList()
    override suspend fun recoverInterruptedReflections(staleBefore: Long) = Unit
    override suspend fun markReflectionStarted(id: Long, startedAt: Long): Int = 0
    override suspend fun markReflectionFailed(id: Long, error: String) = Unit
    override suspend fun completeReflection(
        id: Long,
        summary: String,
        strength: String,
        nextStep: String,
        assignment: String,
        tokensIn: Int,
        tokensOut: Int,
        completedAt: Long,
    ) {
        completedSessionIds += id
    }
    override suspend fun markDiscarded(id: Long): Int = 0
    override suspend fun updateObjectiveEvaluation(
        id: Long,
        outcome: String,
        evidenceTurnId: String?,
        evidenceText: String?,
        confidence: Double,
    ) = Unit
    override suspend fun recent(limit: Int): List<SessionSummary> = emptyList()
    override suspend fun recentForLanguage(targetLang: String, limit: Int): List<SessionSummary> =
        emptyList()
    override fun observeAll(): Flow<List<SessionSummary>> = flowOf(emptyList())
    override suspend fun byId(id: Long): SessionSummary? = null
}
