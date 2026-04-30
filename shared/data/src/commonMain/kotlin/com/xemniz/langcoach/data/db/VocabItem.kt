package com.xemniz.langcoach.data.db

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "vocab_items")
data class VocabItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,
    val lemma: String,
    val context: String,
    val targetLang: String,
    val stability: Double = 0.0,
    val difficulty: Double = 0.0,
    val reps: Int = 0,
    val lapses: Int = 0,
    val dueAt: Long, // epoch millis
    val lastReviewAt: Long? = null,
    val createdAt: Long, // epoch millis
    @ColumnInfo(defaultValue = "heard") val source: String = "heard",
)
