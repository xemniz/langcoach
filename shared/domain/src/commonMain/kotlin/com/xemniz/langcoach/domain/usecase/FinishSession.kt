package com.xemniz.langcoach.domain.usecase

import com.xemniz.langcoach.data.db.UsageEntry
import com.xemniz.langcoach.data.repo.SessionRepo
import com.xemniz.langcoach.data.repo.UsageRepo
import kotlinx.datetime.Clock

class FinishSession(
    private val sessionRepo: SessionRepo,
    private val usageRepo: UsageRepo,
) {
    suspend operator fun invoke(
        sessionId: Long,
        tokensIn: Int,
        tokensOut: Int,
        atMillis: Long = Clock.System.now().toEpochMilliseconds(),
        summary: String = "",
        costCents: Int = 0,
    ) {
        sessionRepo.finish(
            id = sessionId,
            endedAt = atMillis,
            summary = summary,
            tokensIn = tokensIn,
            tokensOut = tokensOut,
            costCents = costCents,
        )
        usageRepo.add(
            UsageEntry(
                createdAt = atMillis,
                sessionId = sessionId,
                endpoint = "realtime",
                model = "gpt-4o-realtime-preview-2024-12-17",
                tokensIn = tokensIn,
                tokensOut = tokensOut,
                costCents = costCents,
            )
        )
    }
}
