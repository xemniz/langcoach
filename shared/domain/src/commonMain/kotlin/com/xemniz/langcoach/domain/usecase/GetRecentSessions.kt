package com.xemniz.langcoach.domain.usecase

import com.xemniz.langcoach.data.db.SessionSummary
import com.xemniz.langcoach.data.repo.SessionRepo
import com.xemniz.langcoach.data.prefs.ProfilePrefs
import kotlinx.coroutines.flow.first

class GetRecentSessions(private val repo: SessionRepo, private val profilePrefs: ProfilePrefs) {
    suspend operator fun invoke(limit: Int = 3): List<SessionSummary> =
        repo.recentForLanguage(profilePrefs.targetLang.first(), limit)
}
