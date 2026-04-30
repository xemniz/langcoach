package com.xemniz.langcoach.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query

@Dao
interface UsageDao {
    @Insert
    suspend fun insert(item: UsageEntry): Long

    @Query("SELECT COALESCE(SUM(costCents), 0) FROM usage_ledger")
    suspend fun totalCostCents(): Int

    @Query("SELECT COALESCE(SUM(tokensIn + tokensOut), 0) FROM usage_ledger")
    suspend fun totalTokens(): Int

    @Query("SELECT * FROM usage_ledger ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<UsageEntry>
}
