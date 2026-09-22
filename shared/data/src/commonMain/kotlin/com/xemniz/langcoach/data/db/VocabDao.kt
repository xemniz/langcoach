package com.xemniz.langcoach.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabDao {
    @Insert
    suspend fun insert(item: VocabItem): Long

    @Update
    suspend fun update(item: VocabItem)

    @Query("SELECT * FROM vocab_items WHERE dueAt <= :nowMillis ORDER BY dueAt ASC")
    suspend fun getDueItems(nowMillis: Long): List<VocabItem>

    @Query("SELECT * FROM vocab_items WHERE dueAt <= :nowMillis AND targetLang = :targetLang ORDER BY dueAt ASC")
    suspend fun getDueItemsForLanguage(nowMillis: Long, targetLang: String): List<VocabItem>

    @Query("SELECT * FROM vocab_items ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<VocabItem>>

    @Query("SELECT COUNT(*) FROM vocab_items")
    suspend fun count(): Int

    @Query("SELECT word FROM vocab_items WHERE targetLang = :targetLang ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recentWords(targetLang: String, limit: Int): List<String>

    @Query("SELECT * FROM vocab_items WHERE word = :word AND lemma = :lemma AND targetLang = :targetLang LIMIT 1")
    suspend fun findByWord(word: String, lemma: String, targetLang: String): VocabItem?
}
