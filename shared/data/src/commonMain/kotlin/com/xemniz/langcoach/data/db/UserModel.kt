package com.xemniz.langcoach.data.db

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "user_model")
data class UserModel(
    @PrimaryKey val id: Int = 1,
    val content: String,
    val updatedAt: Long,
)
