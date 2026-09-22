package com.xemniz.langcoach.ui.errors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xemniz.langcoach.data.repo.WeakCategory
import com.xemniz.langcoach.domain.usecase.GetWeakCategories
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ErrorsScreenState(
    val weak: List<WeakCategory> = emptyList(),
    val isLoading: Boolean = true,
)

class ErrorsViewModel(
    private val getWeakCategories: GetWeakCategories,
) : ViewModel() {
    private val _state = MutableStateFlow(ErrorsScreenState())
    val state: StateFlow<ErrorsScreenState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val weak = getWeakCategories(windowDays = 14, limit = 10)
            _state.value = ErrorsScreenState(weak = weak, isLoading = false)
        }
    }
}
