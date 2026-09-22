package com.xemniz.langcoach.data.repo

import com.xemniz.langcoach.data.db.UsageDao
import com.xemniz.langcoach.data.db.UsageEntry

class UsageRepo(private val dao: UsageDao) {
    suspend fun replaceForSessionEndpoint(entry: UsageEntry): Long =
        dao.replaceForSessionEndpoint(entry)
}
