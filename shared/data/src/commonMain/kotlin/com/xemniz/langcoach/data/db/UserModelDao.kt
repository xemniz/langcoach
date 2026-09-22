package com.xemniz.langcoach.data.db

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Transaction

@Dao
interface UserModelDao {
    @Query("SELECT * FROM user_model WHERE id = 1")
    suspend fun get(): UserModel?

    @Upsert
    suspend fun upsert(model: UserModel)

    @Query("DELETE FROM user_model")
    suspend fun clear()

    @Query("SELECT * FROM user_model_languages WHERE targetLang = :targetLang")
    suspend fun getForLanguage(targetLang: String): UserModelLanguage?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLanguageIfAbsent(model: UserModelLanguage): Long

    @Query("UPDATE user_model_languages SET content = :content, updatedAt = :updatedAt, appliedSessionIds = :appliedSessionIds WHERE targetLang = :targetLang AND updatedAt = :expectedUpdatedAt AND appliedSessionIds = :expectedAppliedSessionIds")
    suspend fun updateLanguageIfVersion(
        targetLang: String,
        content: String,
        updatedAt: Long,
        appliedSessionIds: String,
        expectedUpdatedAt: Long,
        expectedAppliedSessionIds: String,
    ): Int

    @Upsert
    suspend fun upsertLanguage(model: UserModelLanguage)

    @Transaction
    suspend fun getOrMigrateToLanguage(targetLang: String): UserModelLanguage? {
        getForLanguage(targetLang)?.let { return it }
        val legacy = get() ?: return null
        insertLanguageIfAbsent(
            UserModelLanguage(
                targetLang = targetLang,
                content = legacy.content,
                updatedAt = legacy.updatedAt,
            ),
        )
        clear()
        return getForLanguage(targetLang)
    }
}
