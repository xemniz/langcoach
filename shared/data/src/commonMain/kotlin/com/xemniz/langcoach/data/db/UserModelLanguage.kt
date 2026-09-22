package com.xemniz.langcoach.data.db

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "user_model_languages")
data class UserModelLanguage(
    @PrimaryKey val targetLang: String,
    val content: String,
    val updatedAt: Long,
    @ColumnInfo(defaultValue = "''") val appliedSessionIds: String = "",
)
