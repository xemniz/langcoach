package com.xemniz.langcoach.data.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "usage_ledger",
    indices = [Index("createdAt"), Index("sessionId")],
)
data class UsageEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val sessionId: Long? = null,
    val endpoint: String,
    val model: String,
    val tokensIn: Int,
    val tokensOut: Int,
    val costCents: Int,
)
