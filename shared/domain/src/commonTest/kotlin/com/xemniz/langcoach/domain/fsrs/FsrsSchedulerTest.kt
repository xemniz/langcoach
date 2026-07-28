package com.xemniz.langcoach.domain.fsrs

import com.xemniz.langcoach.data.db.VocabItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FsrsSchedulerTest {
    private val scheduler = FsrsScheduler()
    private val now = 1_700_000_000_000L

    @Test
    fun goodFirstReviewSchedulesFourDaysAhead() {
        val reviewed = scheduler.review(newItem(), rating = 3, atMillis = now)

        assertEquals(1, reviewed.reps)
        assertEquals(4.0, reviewed.stability)
        assertEquals(now + 4 * FsrsScheduler.DAY_MILLIS, reviewed.dueAt)
    }

    @Test
    fun forgottenWordIncrementsLapsesAndShortensStability() {
        val known = newItem().copy(reps = 4, stability = 8.0, difficulty = 4.0)
        val reviewed = scheduler.review(known, rating = 1, atMillis = now)

        assertEquals(1, reviewed.lapses)
        assertTrue(reviewed.stability < known.stability)
    }

    @Test
    fun invalidRatingIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            scheduler.review(newItem(), rating = 0, atMillis = now)
        }
    }

    private fun newItem() = VocabItem(
        word = "regatear",
        lemma = "regatear",
        context = "Intenté regatear.",
        targetLang = "es",
        dueAt = now,
        createdAt = now,
    )
}
