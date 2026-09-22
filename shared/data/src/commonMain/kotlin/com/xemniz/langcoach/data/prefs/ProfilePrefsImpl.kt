package com.xemniz.langcoach.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.xemniz.langcoach.core.ProfileLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfilePrefsImpl(private val dataStore: DataStore<Preferences>) : ProfilePrefs {
    private val nativeLangKey = stringPreferencesKey("native_lang")
    private val targetLangKey = stringPreferencesKey("target_lang")
    private val levelKey = stringPreferencesKey("level")

    override val nativeLang: Flow<String> = dataStore.data.map { it[nativeLangKey].orEmpty() }
    override val targetLang: Flow<String> = dataStore.data.map { it[targetLangKey].orEmpty() }
    override val level: Flow<ProfileLevel> = dataStore.data.map {
        runCatching { ProfileLevel.valueOf(it[levelKey] ?: "Unknown") }
            .getOrDefault(ProfileLevel.Unknown)
    }

    override suspend fun setNativeLang(value: String) { dataStore.edit { it[nativeLangKey] = value } }
    override suspend fun setTargetLang(value: String) { dataStore.edit { it[targetLangKey] = value } }
    override suspend fun setLevel(value: ProfileLevel) { dataStore.edit { it[levelKey] = value.name } }
}
