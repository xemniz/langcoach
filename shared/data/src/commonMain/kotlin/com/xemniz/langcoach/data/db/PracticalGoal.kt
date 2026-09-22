package com.xemniz.langcoach.data.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "practical_goals",
    indices = [Index("targetLang"), Index("status"), Index(value = ["sourceSessionId"], unique = true)],
)
data class PracticalGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetLang: String,
    val description: String,
    val status: PracticalGoalStatus,
    val evidenceTurnId: String,
    val evidenceText: String,
    val sourceSessionId: Long,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class PracticalGoalStatus {
    Tentative,
    Confirmed,
    Deferred,
    Rejected,
}
