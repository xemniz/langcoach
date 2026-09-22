package com.xemniz.langcoach.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xemniz.langcoach.core.ProfileLevel
import com.xemniz.langcoach.data.SecureStorage
import com.xemniz.langcoach.data.prefs.ProfilePrefs
import com.xemniz.langcoach.data.repo.SessionRepo
import com.xemniz.langcoach.domain.OPENAI_API_KEY
import com.xemniz.langcoach.domain.usecase.GetDueVocab
import com.xemniz.langcoach.domain.usecase.GetWeakCategories
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeState(
    val dueCount: Int = 0,
    val weakCount: Int = 0,
    val hasApiKey: Boolean = false,
    val nativeLanguage: String = "",
    val targetLanguage: String = "",
    val level: ProfileLevel = ProfileLevel.Unknown,
    val nextStep: String? = null,
) {
    val profileConfigured: Boolean
        get() = nativeLanguage.isNotBlank() && targetLanguage.isNotBlank() && level != ProfileLevel.Unknown
}

class HomeViewModel(
    private val secureStorage: SecureStorage,
    private val getDueVocab: GetDueVocab,
    private val getWeakCategories: GetWeakCategories,
    private val profilePrefs: ProfilePrefs,
    private val sessions: SessionRepo,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val due = getDueVocab().size
            val weak = getWeakCategories(windowDays = 14, limit = 10).size
            val hasApiKey = !secureStorage.read(OPENAI_API_KEY).isNullOrBlank()
            val nativeLanguage = profilePrefs.nativeLang.first()
            val targetLanguage = profilePrefs.targetLang.first()
            val level = profilePrefs.level.first()
            val nextStep = sessions.recentForLanguage(targetLanguage, limit = 1)
                .firstOrNull()
                ?.nextStep
                ?.takeIf(String::isNotBlank)
            _state.update {
                HomeState(
                    dueCount = due,
                    weakCount = weak,
                    hasApiKey = hasApiKey,
                    nativeLanguage = nativeLanguage,
                    targetLanguage = targetLanguage,
                    level = level,
                    nextStep = nextStep,
                )
            }
        }
    }
}
