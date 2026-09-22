package com.xemniz.langcoach.data.repo

import com.xemniz.langcoach.data.db.UsageDao
import com.xemniz.langcoach.data.db.UsageEntry

class UsageRepo(private val dao: UsageDao) {
    suspend fun add(entry: UsageEntry): Long = dao.insert(entry)
    suspend fun replaceForSessionEndpoint(entry: UsageEntry): Long =
        dao.replaceForSessionEndpoint(entry)
    suspend fun totalCostCents(): Int = dao.totalCostCents()
    suspend fun totalTokens(): Int = dao.totalTokens()
    suspend fun recent(limit: Int = 20): List<UsageEntry> = dao.recent(limit)
}
