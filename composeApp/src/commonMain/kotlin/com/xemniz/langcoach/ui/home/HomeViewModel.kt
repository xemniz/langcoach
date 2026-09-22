package com.xemniz.langcoach.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xemniz.langcoach.data.SecureStorage
import com.xemniz.langcoach.data.prefs.ProfilePrefs
import com.xemniz.langcoach.data.repo.SessionRepo
import com.xemniz.langcoach.data.repo.UsageRepo
import com.xemniz.langcoach.data.repo.VocabRepo
import com.xemniz.langcoach.domain.OPENAI_API_KEY
import com.xemniz.langcoach.domain.usecase.GetDueVocab
import com.xemniz.langcoach.domain.usecase.GetWeakCategories
import com.xemniz.langcoach.domain.usecase.ProcessPendingLessons
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeState(
    val vocabCount: Int = 0,
    val dueCount: Int = 0,
    val weakCount: Int = 0,
    val totalCostCents: Int = 0,
    val totalTokens: Int = 0,
    val hasApiKey: Boolean = false,
    val lessonsProcessedOnRefresh: Int = 0,
    val lessonsNeedingAttention: Int = 0,
    val targetLanguage: String = "",
    val level: String = "",
    val nextStep: String? = null,
)

class HomeViewModel(
    private val vocabRepo: VocabRepo,
    private val usageRepo: UsageRepo,
    private val secureStorage: SecureStorage,
    private val getDueVocab: GetDueVocab,
    private val getWeakCategories: GetWeakCategories,
    private val processPendingLessons: ProcessPendingLessons,
    private val profilePrefs: ProfilePrefs,
    private val sessions: SessionRepo,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val processing = processPendingLessons()
            val vocab = vocabRepo.count()
            val due = getDueVocab().size
            val weak = getWeakCategories(windowDays = 14, limit = 10).size
            val cost = usageRepo.totalCostCents()
            val tokens = usageRepo.totalTokens()
            val hasApiKey = !secureStorage.read(OPENAI_API_KEY).isNullOrBlank()
            val targetLanguage = profilePrefs.targetLang.first()
            val level = profilePrefs.level.first().name
            val nextStep = sessions.recentForLanguage(targetLanguage, limit = 1)
                .firstOrNull()
                ?.nextStep
                ?.takeIf(String::isNotBlank)
            _state.update {
                HomeState(
                    vocabCount = vocab,
                    dueCount = due,
                    weakCount = weak,
                    totalCostCents = cost,
                    totalTokens = tokens,
                    hasApiKey = hasApiKey,
                    lessonsProcessedOnRefresh = processing.completed,
                    lessonsNeedingAttention = processing.failedSessionIds.size,
                    targetLanguage = targetLanguage,
                    level = level,
                    nextStep = nextStep,
                )
            }
        }
    }
}
