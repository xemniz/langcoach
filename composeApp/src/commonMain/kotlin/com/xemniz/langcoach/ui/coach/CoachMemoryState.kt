package com.xemniz.langcoach.ui.coach

import com.xemniz.langcoach.data.db.PracticalGoal
import com.xemniz.langcoach.data.db.SessionSummary

data class CoachMemoryState(
    val content: String = "",
    val updatedAt: Long? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val justSaved: Boolean = false,
    val goals: List<PracticalGoal> = emptyList(),
    val recentLessons: List<SessionSummary> = emptyList(),
)
