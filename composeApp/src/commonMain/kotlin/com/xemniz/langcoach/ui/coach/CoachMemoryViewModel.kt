package com.xemniz.langcoach.ui.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xemniz.langcoach.data.repo.UserModelRepo
import com.xemniz.langcoach.data.repo.PracticalGoalStore
import com.xemniz.langcoach.data.repo.SessionRepo
import com.xemniz.langcoach.domain.usecase.PracticalGoalResponse
import com.xemniz.langcoach.domain.usecase.RecordPracticalGoal
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CoachMemoryViewModel(
    private val userModelRepo: UserModelRepo,
    private val practicalGoals: PracticalGoalStore,
    private val recordPracticalGoal: RecordPracticalGoal,
    private val sessions: SessionRepo,
) : ViewModel() {

    private val _state = MutableStateFlow(CoachMemoryState())
    val state: StateFlow<CoachMemoryState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val record = userModelRepo.get()
            val goals = practicalGoals.all()
            val recentLessons = sessions.recent(limit = 5)
            _state.update {
                it.copy(
                    content = record?.content.orEmpty(),
                    updatedAt = record?.updatedAt,
                    loading = false,
                    goals = goals,
                    recentLessons = recentLessons,
                )
            }
        }
    }

    fun onGoalDescriptionChange(goalId: Long, description: String) {
        _state.update { current ->
            current.copy(goals = current.goals.map { goal ->
                if (goal.id == goalId) goal.copy(description = description) else goal
            })
        }
    }

    fun saveGoal(goalId: Long) {
        val description = _state.value.goals.firstOrNull { it.id == goalId }?.description ?: return
        viewModelScope.launch {
            recordPracticalGoal.edit(goalId, description)
            load()
        }
    }

    fun respondToGoal(goalId: Long, response: PracticalGoalResponse) {
        viewModelScope.launch {
            recordPracticalGoal.respond(goalId, response)
            load()
        }
    }

    fun removeGoal(goalId: Long) {
        viewModelScope.launch {
            recordPracticalGoal.remove(goalId)
            load()
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
