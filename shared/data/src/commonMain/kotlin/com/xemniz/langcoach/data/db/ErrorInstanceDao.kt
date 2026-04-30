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

    @Query("""
        SELECT categoryId, COUNT(*) AS recentCount
        FROM error_instances
        WHERE createdAt >= :sinceMillis
        GROUP BY categoryId
        ORDER BY recentCount DESC
        LIMIT :limit
    """)
    suspend fun topCategoriesSince(sinceMillis: Long, limit: Int): List<CategoryCount>
}

data class CategoryCount(val categoryId: Long, val recentCount: Int)
