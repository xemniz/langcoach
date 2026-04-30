package com.xemniz.langcoach.data.db

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert

@Dao
interface UserModelDao {
    @Query("SELECT * FROM user_model WHERE id = 1")
    suspend fun get(): UserModel?

    @Upsert
    suspend fun upsert(model: UserModel)

    @Query("DELETE FROM user_model")
    suspend fun clear()
}
