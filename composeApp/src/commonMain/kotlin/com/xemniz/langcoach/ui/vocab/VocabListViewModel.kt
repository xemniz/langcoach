package com.xemniz.langcoach.ui.vocab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xemniz.langcoach.data.db.VocabItem
import com.xemniz.langcoach.data.repo.VocabRepo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class VocabListViewModel(repo: VocabRepo) : ViewModel() {
    val items: StateFlow<List<VocabItem>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())
}
