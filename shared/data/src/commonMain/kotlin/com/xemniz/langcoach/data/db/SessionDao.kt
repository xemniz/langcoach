package com.xemniz.langcoach.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(item: SessionSummary): Long

    @Query("UPDATE session_summaries SET endedAt = :endedAt, summary = :summary, tokensIn = :tokensIn, tokensOut = :tokensOut, costCents = :costCents WHERE id = :id")
    suspend fun finish(id: Long, endedAt: Long, summary: String, tokensIn: Int, tokensOut: Int, costCents: Int)

    @Query("SELECT * FROM session_summaries WHERE endedAt IS NOT NULL ORDER BY endedAt DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<SessionSummary>

    @Query("SELECT * FROM session_summaries ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<SessionSummary>>

    @Query("SELECT * FROM session_summaries WHERE id = :id")
    suspend fun byId(id: Long): SessionSummary?
}
