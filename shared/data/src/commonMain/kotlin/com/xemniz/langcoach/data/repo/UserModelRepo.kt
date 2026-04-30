package com.xemniz.langcoach.data.repo

import com.xemniz.langcoach.data.db.UserModel
import com.xemniz.langcoach.data.db.UserModelDao
import kotlinx.datetime.Clock

class UserModelRepo(private val dao: UserModelDao) {
    suspend fun get(): UserModel? = dao.get()

    suspend fun getContent(): String? = dao.get()?.content

    suspend fun set(content: String, atMillis: Long) {
        dao.upsert(UserModel(id = 1, content = content, updatedAt = atMillis))
    }

    suspend fun setNow(content: String): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        dao.upsert(UserModel(id = 1, content = content, updatedAt = now))
        return now
    }

    suspend fun clear() = dao.clear()
}
