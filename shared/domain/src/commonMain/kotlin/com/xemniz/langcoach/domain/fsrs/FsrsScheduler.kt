package com.xemniz.langcoach.domain.fsrs

import com.xemniz.langcoach.data.db.VocabItem
import kotlin.math.max
import kotlin.math.min

class FsrsScheduler {
    fun review(prev: VocabItem, rating: Int, atMillis: Long): VocabItem {
        require(rating in 1..4) { "rating must be 1..4" }
        return if (prev.reps == 0) reviewNew(prev, rating, atMillis)
        else reviewExisting(prev, rating, atMillis)
    }

    private fun reviewNew(prev: VocabItem, rating: Int, atMillis: Long): VocabItem {
        val stability = when (rating) { 1 -> 0.5; 2 -> 1.5; 3 -> 4.0; 4 -> 9.0; else -> 4.0 }
        val difficulty = when (rating) { 1 -> 7.0; 2 -> 5.5; 3 -> 4.0; 4 -> 2.5; else -> 4.0 }
        return prev.copy(
            stability = stability,
            difficulty = difficulty,
            reps = 1,
            lapses = if (rating == 1) 1 else 0,
            lastReviewAt = atMillis,
            dueAt = atMillis + (stability * DAY_MILLIS).toLong(),
        )
    }

    private fun reviewExisting(prev: VocabItem, rating: Int, atMillis: Long): VocabItem {
        val newStability = when (rating) {
            1 -> max(1.0, prev.stability * 0.5)
            2 -> prev.stability * 1.2
            3 -> prev.stability * (2.0 - prev.difficulty * 0.1).coerceIn(1.1, 3.0)
            4 -> prev.stability * 3.0
            else -> prev.stability
        }
        val newDifficulty = when (rating) {
            1 -> min(10.0, prev.difficulty + 1.0)
            2 -> min(10.0, prev.difficulty + 0.1)
            3 -> prev.difficulty
            4 -> max(1.0, prev.difficulty - 0.1)
            else -> prev.difficulty
        }
        return prev.copy(
            stability = newStability,
            difficulty = newDifficulty,
            reps = prev.reps + 1,
            lapses = if (rating == 1) prev.lapses + 1 else prev.lapses,
            lastReviewAt = atMillis,
            dueAt = atMillis + (newStability * DAY_MILLIS).toLong(),
        )
    }

    companion object {
        const val DAY_MILLIS: Long = 86_400_000L
    }
}
