package com.xemniz.langcoach.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
interface ErrorCategoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<ErrorCategory>): List<Long>

    @Query("SELECT * FROM error_categories ORDER BY name ASC")
    suspend fun all(): List<ErrorCategory>

    @Query("SELECT COUNT(*) FROM error_categories")
    suspend fun count(): Int

    @Query("SELECT * FROM error_categories WHERE code = :code LIMIT 1")
    suspend fun byCode(code: String): ErrorCategory?
}
