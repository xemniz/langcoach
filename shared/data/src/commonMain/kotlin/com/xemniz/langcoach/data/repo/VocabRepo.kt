package com.xemniz.langcoach.data.repo

import com.xemniz.langcoach.data.db.VocabDao
import com.xemniz.langcoach.data.db.VocabItem
import kotlinx.coroutines.flow.Flow

class VocabRepo(private val dao: VocabDao) {
    suspend fun count(): Int = dao.count()
    suspend fun getDue(nowMillis: Long): List<VocabItem> = dao.getDueItems(nowMillis)
    fun observeAll(): Flow<List<VocabItem>> = dao.observeAll()
    suspend fun insert(item: VocabItem): Long = dao.insert(item)
    suspend fun update(item: VocabItem) = dao.update(item)
    suspend fun recentWords(targetLang: String, limit: Int = 100): List<String> =
        dao.recentWords(targetLang, limit)

    suspend fun insertIfNew(item: VocabItem): Boolean {
        val existing = dao.findByWord(item.word, item.lemma, item.targetLang)
        return if (existing == null) {
            dao.insert(item)
            true
        } else false
    }
}
