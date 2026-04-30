package com.xemniz.langcoach.data.db

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "error_categories",
    indices = [Index(value = ["code"], unique = true)],
)
data class ErrorCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val description: String,
)
