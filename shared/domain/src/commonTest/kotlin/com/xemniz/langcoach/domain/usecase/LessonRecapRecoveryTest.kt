package com.xemniz.langcoach.domain.usecase

import com.xemniz.langcoach.core.AppResult
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class LessonRecapRecoveryTest {
    @Test
    fun retriesOnlyTheSelectedLesson() = runBlocking {
        val processed = mutableListOf<Long>()
        val useCase = RetryLessonRecap(
            processor = object : LessonReflectionProcessor {
                override suspend fun process(sessionId: Long): AppResult<Int> {
                    processed += sessionId
                    return AppResult.Success(1)
                }
            },
        )

        val result = useCase(7)

        assertEquals(AppResult.Success(1), result)
        assertEquals(listOf(7L), processed)
    }
}
