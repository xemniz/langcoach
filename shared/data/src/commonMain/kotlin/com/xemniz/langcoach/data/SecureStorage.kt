package com.xemniz.langcoach.data

expect class SecureStorage {
    suspend fun save(key: String, value: String)
    suspend fun read(key: String): String?
    suspend fun delete(key: String)
}
