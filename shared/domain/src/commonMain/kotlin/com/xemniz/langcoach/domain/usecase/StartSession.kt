package com.xemniz.langcoach.domain.usecase

import com.xemniz.langcoach.data.repo.SessionRepo
import kotlinx.datetime.Clock

class StartSession(private val sessionRepo: SessionRepo) {
    suspend operator fun invoke(atMillis: Long = Clock.System.now().toEpochMilliseconds()): Long =
        sessionRepo.create(atMillis)
}
