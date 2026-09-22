package com.xemniz.langcoach.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xemniz.langcoach.core.AppResult
import com.xemniz.langcoach.core.ProfileLevel
import com.xemniz.langcoach.data.SecureStorage
import com.xemniz.langcoach.data.prefs.ProfilePrefs
import com.xemniz.langcoach.domain.usecase.VerifyApiKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val verifyApiKey: VerifyApiKey,
    private val secureStorage: SecureStorage,
    private val profilePrefs: ProfilePrefs,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            profilePrefs.nativeLang.collect { value ->
                _state.update { it.copy(nativeLang = value) }
            }
        }
        viewModelScope.launch {
            profilePrefs.targetLang.collect { value ->
                _state.update { it.copy(targetLang = value) }
            }
        }
        viewModelScope.launch {
            profilePrefs.level.collect { value ->
                _state.update { it.copy(level = value) }
            }
        }
        viewModelScope.launch {
            val existing = secureStorage.read(OPENAI_API_KEY)
            _state.update {
                it.copy(
                    hasApiKey = !existing.isNullOrBlank(),
                )
            }
        }
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.ApiKeyChanged -> _state.update { it.copy(apiKey = intent.value, verifyMessage = null) }
            SettingsIntent.VerifyAndSaveKey -> verifyAndSave()
            SettingsIntent.RemoveApiKey -> removeApiKey()
            is SettingsIntent.NativeLangChanged -> {
                _state.update { it.copy(nativeLang = intent.value) }
                viewModelScope.launch { profilePrefs.setNativeLang(intent.value) }
            }
            is SettingsIntent.TargetLangChanged -> {
                _state.update { it.copy(targetLang = intent.value) }
                viewModelScope.launch { profilePrefs.setTargetLang(intent.value) }
            }
            is SettingsIntent.LevelChanged -> {
                _state.update { it.copy(level = intent.value) }
                viewModelScope.launch { profilePrefs.setLevel(intent.value) }
            }
        }
    }

    private fun verifyAndSave() {
        val key = _state.value.apiKey
        if (key.isBlank()) return
        _state.update { it.copy(isVerifying = true, verifyMessage = null) }
        viewModelScope.launch {
            val result = verifyApiKey(key)
            _state.update {
                when (result) {
                    is AppResult.Success -> it.copy(
                        isVerifying = false,
                        verifyMessage = "Verified",
                        verifySuccess = true,
                        apiKey = "",
                        hasApiKey = true,
                    )
                    is AppResult.Failure -> {
                        val error = result.error
                        it.copy(
                            isVerifying = false,
                            verifyMessage = if (error is com.xemniz.langcoach.core.AppError.Api && error.code == 401) {
                                "That API key wasn't accepted."
                            } else {
                                "Couldn't verify the key. Check your connection and try again."
                            },
                            verifySuccess = false,
                        )
                    }
                }
            }
        }
    }

    private fun removeApiKey() {
        viewModelScope.launch {
            secureStorage.delete(OPENAI_API_KEY)
            _state.update {
                it.copy(
                    apiKey = "",
                    hasApiKey = false,
                    verifyMessage = "API key removed",
                    verifySuccess = true,
                )
            }
        }
    }

    companion object {
        const val OPENAI_API_KEY = "openai_api_key"
    }
}
