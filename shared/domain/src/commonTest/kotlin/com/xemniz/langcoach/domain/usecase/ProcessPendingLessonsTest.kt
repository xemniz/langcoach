package com.xemniz.langcoach.domain.usecase

import com.xemniz.langcoach.core.AppError
import com.xemniz.langcoach.core.AppResult
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class ProcessPendingLessonsTest {
    @Test
    fun retriesEveryPendingLessonEvenWhenOneFails() = runBlocking {
        val processed = mutableListOf<Long>()
        val useCase = ProcessPendingLessons(
            pendingLessons = object : PendingLessons {
                override suspend fun ids(): List<Long> = listOf(4, 7, 9)
            },
            processor = object : LessonReflectionProcessor {
                override suspend fun process(sessionId: Long): AppResult<Int> {
                    processed += sessionId
                    return if (sessionId == 7L) {
                        AppResult.Failure(AppError.Storage("temporary failure"))
                    } else {
                        AppResult.Success(1)
                    }
                }
            },
        )

        val result = useCase()

        assertEquals(listOf(4L, 7L, 9L), processed)
        assertEquals(2, result.completed)
        assertEquals(listOf(7L), result.failedSessionIds)
    }
}
