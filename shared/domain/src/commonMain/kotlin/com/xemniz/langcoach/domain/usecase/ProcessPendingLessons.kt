package com.xemniz.langcoach.domain.usecase

import com.xemniz.langcoach.core.AppResult
import com.xemniz.langcoach.data.repo.SessionRepo

interface PendingLessons {
    suspend fun ids(): List<Long>
}

interface LessonReflectionProcessor {
    suspend fun process(sessionId: Long): AppResult<Int>
}

class SessionPendingLessons(
    private val sessions: SessionRepo,
) : PendingLessons {
    override suspend fun ids(): List<Long> {
        sessions.recoverInterruptedReflections()
        return sessions.pendingReflectionIds()
    }
}

class ProcessPendingLessons(
    private val pendingLessons: PendingLessons,
    private val processor: LessonReflectionProcessor,
) {
    suspend operator fun invoke(): PendingLessonResult {
        var completed = 0
        val failed = mutableListOf<Long>()
        pendingLessons.ids().forEach { sessionId ->
            when (processor.process(sessionId)) {
                is AppResult.Success -> completed += 1
                is AppResult.Failure -> failed += sessionId
            }
        }
        return PendingLessonResult(completed, failed)
    }
}

data class PendingLessonResult(
    val completed: Int,
    val failedSessionIds: List<Long>,
)
