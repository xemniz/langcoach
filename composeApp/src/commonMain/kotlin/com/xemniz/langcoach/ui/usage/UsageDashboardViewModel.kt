package com.xemniz.langcoach.ui.usage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xemniz.langcoach.data.db.UsageEntry
import com.xemniz.langcoach.data.repo.UsageRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UsageState(
    val totalCostCents: Int = 0,
    val totalTokens: Int = 0,
    val recent: List<UsageEntry> = emptyList(),
    val isLoading: Boolean = true,
)

class UsageDashboardViewModel(private val repo: UsageRepo) : ViewModel() {
    private val _state = MutableStateFlow(UsageState())
    val state: StateFlow<UsageState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val totalCost = repo.totalCostCents()
            val totalTokens = repo.totalTokens()
            val recent = repo.recent(limit = 20)
            _state.value = UsageState(totalCost, totalTokens, recent, isLoading = false)
        }
    }
}
