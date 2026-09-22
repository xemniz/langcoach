package com.xemniz.langcoach.domain.usecase

import com.xemniz.langcoach.core.AppResult
import com.xemniz.langcoach.data.repo.SessionRepo
import kotlinx.datetime.Clock

interface LessonReflectionProcessor {
    suspend fun process(sessionId: Long): AppResult<Int>
}

class RecoverInterruptedLessonRecaps(
    private val sessions: SessionRepo,
) {
    suspend operator fun invoke() {
        sessions.recoverInterruptedReflections(
            staleBefore = Clock.System.now().toEpochMilliseconds() - PROCESSING_LEASE_MILLIS,
        )
    }

    private companion object {
        const val PROCESSING_LEASE_MILLIS = 15 * 60 * 1_000L
    }
}

class RetryLessonRecap(
    private val processor: LessonReflectionProcessor,
) {
    suspend operator fun invoke(sessionId: Long): AppResult<Int> = processor.process(sessionId)
}
