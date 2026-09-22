package com.xemniz.langcoach.data.db

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

@Entity(
    tableName = "session_turns",
    primaryKeys = ["sessionId", "turnId"],
    foreignKeys = [
        ForeignKey(
            entity = SessionSummary::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class SessionTurn(
    val sessionId: Long,
    val turnId: String,
    val position: Int,
    val speaker: String,
    val text: String,
)
