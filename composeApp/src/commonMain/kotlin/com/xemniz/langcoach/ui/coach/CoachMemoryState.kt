package com.xemniz.langcoach.ui.coach

import com.xemniz.langcoach.data.db.PracticalGoal

data class CoachMemoryState(
    val content: String = "",
    val updatedAt: Long? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val justSaved: Boolean = false,
    val goals: List<PracticalGoal> = emptyList(),
    val recentLessons: List<RecentLesson> = emptyList(),
    val retryingLessonId: Long? = null,
)

data class RecentLesson(
    val id: Long,
    val startedAt: Long,
    val targetLanguage: String,
    val summary: String?,
    val strength: String?,
    val nextStep: String?,
    val assignment: String?,
    val recapStatus: LessonRecapStatus,
)

enum class LessonRecapStatus {
    Ready,
    Queued,
    Preparing,
    Failed,
    Removed,
    None,
}
