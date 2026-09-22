package com.xemniz.langcoach.data.repo

import com.xemniz.langcoach.data.db.UserModelDao
import com.xemniz.langcoach.data.db.UserModelLanguage
import kotlinx.datetime.Clock

class UserModelRepo(private val dao: UserModelDao) {
    suspend fun get(targetLang: String): UserModelLanguage? = dao.getOrMigrateToLanguage(targetLang)

    suspend fun getContent(targetLang: String): String? = get(targetLang)?.content

    suspend fun applyLessonIfUnchanged(
        targetLang: String,
        content: String,
        sessionId: Long,
        expectedUpdatedAt: Long?,
        atMillis: Long,
        previousAppliedSessionIds: String,
        tokensIn: Int,
        tokensOut: Int,
    ): Boolean {
        val usage = parseAppliedUsage(previousAppliedSessionIds).toMutableMap().apply {
            put(sessionId, AppliedLessonUsage(tokensIn, tokensOut))
        }
        val ids = usage.entries.sortedBy { it.key }.joinToString(",") { (id, tokens) ->
            "$id:${tokens.tokensIn}:${tokens.tokensOut}"
        }
        val next = UserModelLanguage(targetLang, content, atMillis, ids)
        return if (expectedUpdatedAt == null) {
            dao.insertLanguageIfAbsent(next) != -1L
        } else {
            dao.updateLanguageIfVersion(
                targetLang = targetLang,
                content = content,
                updatedAt = atMillis,
                appliedSessionIds = ids,
                expectedUpdatedAt = expectedUpdatedAt,
                expectedAppliedSessionIds = previousAppliedSessionIds,
            ) == 1
        }
    }

    suspend fun appliedLessonUsage(targetLang: String, sessionId: Long): AppliedLessonUsage? =
        parseAppliedUsage(get(targetLang)?.appliedSessionIds.orEmpty())[sessionId]

    suspend fun setNow(targetLang: String, content: String): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        val previous = get(targetLang)
        dao.upsertLanguage(
            UserModelLanguage(
                targetLang = targetLang,
                content = content,
                updatedAt = now,
                appliedSessionIds = previous?.appliedSessionIds.orEmpty(),
            ),
        )
        return now
    }

    suspend fun clear(targetLang: String) {
        val previous = get(targetLang)
        dao.upsertLanguage(
            UserModelLanguage(
                targetLang = targetLang,
                content = "",
                updatedAt = Clock.System.now().toEpochMilliseconds(),
                appliedSessionIds = previous?.appliedSessionIds.orEmpty(),
            ),
        )
    }

    private fun parseAppliedUsage(value: String): Map<Long, AppliedLessonUsage> = value
        .split(',')
        .mapNotNull { encoded ->
            val fields = encoded.split(':')
            val id = fields.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
            id to AppliedLessonUsage(
                tokensIn = fields.getOrNull(1)?.toIntOrNull() ?: 0,
                tokensOut = fields.getOrNull(2)?.toIntOrNull() ?: 0,
            )
        }
        .toMap()

    data class AppliedLessonUsage(val tokensIn: Int, val tokensOut: Int)
}
