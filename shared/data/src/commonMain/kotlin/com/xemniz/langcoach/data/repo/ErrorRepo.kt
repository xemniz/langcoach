package com.xemniz.langcoach.data.repo

import com.xemniz.langcoach.data.db.ErrorCategory
import com.xemniz.langcoach.data.db.ErrorCategoryDao
import com.xemniz.langcoach.data.db.ErrorInstance
import com.xemniz.langcoach.data.db.ErrorInstanceDao
import com.xemniz.langcoach.data.taxonomy.DEFAULT_ERROR_TAXONOMY

data class WeakCategory(val category: ErrorCategory, val recentCount: Int)

class ErrorRepo(
    private val categoryDao: ErrorCategoryDao,
    private val instanceDao: ErrorInstanceDao,
) {
    suspend fun seedTaxonomyIfEmpty() {
        if (categoryDao.count() == 0) {
            categoryDao.insertAll(DEFAULT_ERROR_TAXONOMY)
        }
    }

    suspend fun listCategories(): List<ErrorCategory> = categoryDao.all()

    suspend fun categoryByCode(code: String): ErrorCategory? = categoryDao.byCode(code)

    suspend fun logInstance(
        categoryId: Long,
        sessionId: Long?,
        originalText: String,
        correctedText: String,
        atMillis: Long,
    ): Long = instanceDao.insert(
        ErrorInstance(
            categoryId = categoryId,
            sessionId = sessionId,
            originalText = originalText,
            correctedText = correctedText,
            createdAt = atMillis,
        )
    )

    suspend fun forSession(sessionId: Long): List<ErrorInstance> = instanceDao.forSession(sessionId)

    suspend fun weakCategories(sinceMillis: Long, limit: Int = 5): List<WeakCategory> {
        val counts = instanceDao.topCategoriesSince(sinceMillis, limit)
        if (counts.isEmpty()) return emptyList()
        val byId = categoryDao.all().associateBy { it.id }
        return counts.mapNotNull { c -> byId[c.categoryId]?.let { WeakCategory(it, c.recentCount) } }
    }
}
