package com.xemniz.langcoach.domain.usecase

import com.xemniz.langcoach.data.db.SessionSummary
import com.xemniz.langcoach.data.repo.SessionRepo

class GetRecentSessions(private val repo: SessionRepo) {
    suspend operator fun invoke(limit: Int = 3): List<SessionSummary> = repo.recent(limit)
}
