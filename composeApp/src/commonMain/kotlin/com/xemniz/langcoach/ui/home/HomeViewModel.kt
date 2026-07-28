package com.xemniz.langcoach.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xemniz.langcoach.data.SecureStorage
import com.xemniz.langcoach.data.repo.UsageRepo
import com.xemniz.langcoach.data.repo.VocabRepo
import com.xemniz.langcoach.domain.OPENAI_API_KEY
import com.xemniz.langcoach.domain.usecase.GetDueVocab
import com.xemniz.langcoach.domain.usecase.GetWeakCategories
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeState(
    val vocabCount: Int = 0,
    val dueCount: Int = 0,
    val weakCount: Int = 0,
    val totalCostCents: Int = 0,
    val hasApiKey: Boolean = false,
)

class HomeViewModel(
    private val vocabRepo: VocabRepo,
    private val usageRepo: UsageRepo,
    private val secureStorage: SecureStorage,
    private val getDueVocab: GetDueVocab,
    private val getWeakCategories: GetWeakCategories,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val vocab = vocabRepo.count()
            val due = getDueVocab().size
            val weak = getWeakCategories(windowDays = 14, limit = 10).size
            val cost = usageRepo.totalCostCents()
            val hasApiKey = !secureStorage.read(OPENAI_API_KEY).isNullOrBlank()
            _state.update { HomeState(vocab, due, weak, cost, hasApiKey) }
        }
    }
}
