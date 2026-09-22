package com.xemniz.langcoach.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(item: SessionSummary): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTurns(turns: List<SessionTurn>)

    @Query("DELETE FROM session_turns WHERE sessionId = :sessionId")
    suspend fun deleteTurns(sessionId: Long)

    @Query("UPDATE session_summaries SET endedAt = :endedAt, summary = :summary, tokensIn = :tokensIn, tokensOut = :tokensOut, costCents = :costCents WHERE id = :id")
    suspend fun finish(id: Long, endedAt: Long, summary: String, tokensIn: Int, tokensOut: Int, costCents: Int)

    @Query("UPDATE session_summaries SET endedAt = :endedAt, tokensIn = :tokensIn, tokensOut = :tokensOut, costCents = :costCents, processingState = 'Pending', processingError = NULL WHERE id = :id")
    suspend fun queueReflection(id: Long, endedAt: Long, tokensIn: Int, tokensOut: Int, costCents: Int)

    @Transaction
    suspend fun finishAndQueue(
        id: Long,
        endedAt: Long,
        tokensIn: Int,
        tokensOut: Int,
        costCents: Int,
        turns: List<SessionTurn>,
    ) {
        queueReflection(id, endedAt, tokensIn, tokensOut, costCents)
        deleteTurns(id)
        if (turns.isNotEmpty()) insertTurns(turns)
    }

    @Query("SELECT * FROM session_turns WHERE sessionId = :sessionId ORDER BY position ASC")
    suspend fun turns(sessionId: Long): List<SessionTurn>

    @Query("SELECT id FROM session_summaries WHERE processingState IN ('Pending', 'Failed') ORDER BY endedAt ASC")
    suspend fun pendingReflectionIds(): List<Long>

    @Query("UPDATE session_summaries SET processingState = 'Pending', processingError = 'Interrupted while processing; retrying' WHERE processingState = 'Processing' AND (reflectionStartedAt IS NULL OR reflectionStartedAt <= :staleBefore)")
    suspend fun recoverInterruptedReflections(staleBefore: Long)

    @Query("UPDATE session_summaries SET processingState = 'Processing', processingError = NULL, reflectionStartedAt = :startedAt WHERE id = :id AND processingState IN ('Pending', 'Failed')")
    suspend fun markReflectionStarted(id: Long, startedAt: Long): Int

    @Query("UPDATE session_summaries SET processingState = 'Failed', processingError = :error WHERE id = :id")
    suspend fun markReflectionFailed(id: Long, error: String)

    @Query("UPDATE session_summaries SET summary = :summary, strength = :strength, nextStep = :nextStep, assignment = :assignment, tokensIn = :tokensIn, tokensOut = :tokensOut, processingState = 'Completed', processingError = NULL, reflectionCompletedAt = :completedAt WHERE id = :id")
    suspend fun completeReflection(
        id: Long,
        summary: String,
        strength: String,
        nextStep: String,
        assignment: String,
        tokensIn: Int,
        tokensOut: Int,
        completedAt: Long,
    )

    @Transaction
    suspend fun completeReflectionAndForgetTranscript(
        id: Long,
        summary: String,
        strength: String,
        nextStep: String,
        assignment: String,
        tokensIn: Int,
        tokensOut: Int,
        completedAt: Long,
    ) {
        completeReflection(id, summary, strength, nextStep, assignment, tokensIn, tokensOut, completedAt)
        deleteTurns(id)
    }

    @Query("UPDATE session_summaries SET processingState = 'Discarded', processingError = NULL WHERE id = :id AND processingState IN ('Pending', 'Failed')")
    suspend fun markDiscarded(id: Long): Int

    @Transaction
    suspend fun discardPendingTranscript(id: Long): Int {
        val changed = markDiscarded(id)
        if (changed == 1) deleteTurns(id)
        return changed
    }

    @Query(
        """
        UPDATE session_summaries
        SET objectiveOutcome = :outcome,
            objectiveEvidenceTurnId = :evidenceTurnId,
            objectiveEvidenceText = :evidenceText,
            objectiveConfidence = :confidence
        WHERE id = :id
        """,
    )
    suspend fun updateObjectiveEvaluation(
        id: Long,
        outcome: String,
        evidenceTurnId: String?,
        evidenceText: String?,
        confidence: Double,
    )

    @Query("SELECT * FROM session_summaries WHERE endedAt IS NOT NULL ORDER BY endedAt DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<SessionSummary>

    @Query("SELECT * FROM session_summaries WHERE endedAt IS NOT NULL AND targetLang = :targetLang ORDER BY endedAt DESC LIMIT :limit")
    suspend fun recentForLanguage(targetLang: String, limit: Int): List<SessionSummary>

    @Query("SELECT * FROM session_summaries ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<SessionSummary>>

    @Query("SELECT * FROM session_summaries WHERE id = :id")
    suspend fun byId(id: Long): SessionSummary?
}
