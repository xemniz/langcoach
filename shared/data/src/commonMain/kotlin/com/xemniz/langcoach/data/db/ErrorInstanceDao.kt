package com.xemniz.langcoach.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query

@Dao
interface ErrorInstanceDao {
    @Insert
    suspend fun insert(item: ErrorInstance): Long

    @Query("SELECT * FROM error_instances WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    suspend fun forSession(sessionId: Long): List<ErrorInstance>

    @Query("DELETE FROM error_instances WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: Long)

    @Query("""
        SELECT categoryId, COUNT(*) AS recentCount
        FROM error_instances
        WHERE createdAt >= :sinceMillis
        GROUP BY categoryId
        ORDER BY recentCount DESC
        LIMIT :limit
    """)
    suspend fun topCategoriesSince(sinceMillis: Long, limit: Int): List<CategoryCount>

    @Query("""
        SELECT error_instances.categoryId, COUNT(*) AS recentCount
        FROM error_instances
        INNER JOIN session_summaries ON session_summaries.id = error_instances.sessionId
        WHERE error_instances.createdAt >= :sinceMillis
          AND session_summaries.targetLang = :targetLang
        GROUP BY error_instances.categoryId
        ORDER BY recentCount DESC
        LIMIT :limit
    """)
    suspend fun topCategoriesSinceForLanguage(
        sinceMillis: Long,
        targetLang: String,
        limit: Int,
    ): List<CategoryCount>
}

data class CategoryCount(val categoryId: Long, val recentCount: Int)
