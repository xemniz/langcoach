package com.xemniz.langcoach.data.db

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "session_summaries")
data class SessionSummary(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long? = null,
    val summary: String? = null,
    val tokensIn: Int = 0,
    val tokensOut: Int = 0,
    val costCents: Int = 0,
    val planVersion: String? = null,
    val objectiveId: String? = null,
    val objectiveKind: String? = null,
    val objectiveDescription: String? = null,
    val objectiveTarget: String? = null,
    val objectiveSuccessCriteria: String? = null,
    val objectiveOutcome: String? = null,
    val objectiveEvidenceTurnId: String? = null,
    val objectiveEvidenceText: String? = null,
    val objectiveConfidence: Double? = null,
)
