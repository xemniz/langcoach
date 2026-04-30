package com.xemniz.langcoach.ui.coach

data class CoachMemoryState(
    val content: String = "",
    val updatedAt: Long? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val justSaved: Boolean = false,
)
