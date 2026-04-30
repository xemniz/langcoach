package com.xemniz.langcoach.data.db

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "error_instances",
    foreignKeys = [
        ForeignKey(
            entity = ErrorCategory::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("categoryId"), Index("sessionId"), Index("createdAt")],
)
data class ErrorInstance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val sessionId: Long?,
    val originalText: String,
    val correctedText: String,
    val createdAt: Long,
)
