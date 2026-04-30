package com.xemniz.langcoach.domain.usecase

import com.xemniz.langcoach.data.db.VocabItem
import com.xemniz.langcoach.data.repo.VocabRepo
import com.xemniz.langcoach.domain.fsrs.FsrsScheduler
import kotlinx.datetime.Clock

class ScheduleVocabReview(
    private val repo: VocabRepo,
    private val fsrs: FsrsScheduler,
) {
    suspend operator fun invoke(
        item: VocabItem,
        rating: Int,
        atMillis: Long = Clock.System.now().toEpochMilliseconds(),
    ): VocabItem {
        val updated = fsrs.review(item, rating, atMillis)
        repo.update(updated)
        return updated
    }
}
