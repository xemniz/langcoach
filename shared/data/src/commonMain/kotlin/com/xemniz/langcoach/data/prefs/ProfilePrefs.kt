package com.xemniz.langcoach.data.prefs

import com.xemniz.langcoach.core.ProfileLevel
import kotlinx.coroutines.flow.Flow

interface ProfilePrefs {
    val nativeLang: Flow<String>
    val targetLang: Flow<String>
    val level: Flow<ProfileLevel>
    suspend fun setNativeLang(value: String)
    suspend fun setTargetLang(value: String)
    suspend fun setLevel(value: ProfileLevel)
}
