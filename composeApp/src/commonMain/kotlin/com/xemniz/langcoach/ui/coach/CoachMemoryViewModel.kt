package com.xemniz.langcoach.ui.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xemniz.langcoach.data.repo.UserModelRepo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CoachMemoryViewModel(
    private val userModelRepo: UserModelRepo,
) : ViewModel() {

    private val _state = MutableStateFlow(CoachMemoryState())
    val state: StateFlow<CoachMemoryState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val record = userModelRepo.get()
            _state.update {
                it.copy(
                    content = record?.content.orEmpty(),
                    updatedAt = record?.updatedAt,
                    loading = false,
                )
            }
        }
    }

    fun onContentChange(s: String) {
        _state.update { it.copy(content = s, justSaved = false) }
    }

    fun save() {
        val current = _state.value
        if (current.saving) return
        _state.update { it.copy(saving = true, justSaved = false) }
        viewModelScope.launch {
            val now = userModelRepo.setNow(current.content)
            _state.update {
                it.copy(
                    saving = false,
                    justSaved = true,
                    updatedAt = now,
                )
            }
            delay(2000)
            _state.update { it.copy(justSaved = false) }
        }
    }

    fun clear() {
        viewModelScope.launch {
            userModelRepo.clear()
            _state.update {
                it.copy(
                    content = "",
                    updatedAt = null,
                    justSaved = false,
                )
            }
        }
    }
}
