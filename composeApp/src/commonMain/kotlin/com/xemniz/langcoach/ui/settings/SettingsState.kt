package com.xemniz.langcoach.ui.settings

import com.xemniz.langcoach.core.ProfileLevel

data class SettingsState(
    val apiKey: String = "",
    val isVerifying: Boolean = false,
    val verifyMessage: String? = null,
    val verifySuccess: Boolean = false,
    val nativeLang: String = "",
    val targetLang: String = "",
    val level: ProfileLevel = ProfileLevel.A1,
)

sealed interface SettingsIntent {
    data class ApiKeyChanged(val value: String) : SettingsIntent
    data object VerifyAndSaveKey : SettingsIntent
    data class NativeLangChanged(val value: String) : SettingsIntent
    data class TargetLangChanged(val value: String) : SettingsIntent
    data class LevelChanged(val value: ProfileLevel) : SettingsIntent
}
