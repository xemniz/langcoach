package com.xemniz.langcoach.domain.usecase

import com.xemniz.langcoach.data.repo.ErrorRepo
import com.xemniz.langcoach.data.repo.WeakCategory
import kotlinx.datetime.Clock

class GetWeakCategories(private val repo: ErrorRepo) {
    suspend operator fun invoke(
        windowDays: Int = 14,
        limit: Int = 5,
        nowMillis: Long = Clock.System.now().toEpochMilliseconds(),
    ): List<WeakCategory> {
        val sinceMillis = nowMillis - windowDays * 86_400_000L
        return repo.weakCategories(sinceMillis, limit)
    }
}
