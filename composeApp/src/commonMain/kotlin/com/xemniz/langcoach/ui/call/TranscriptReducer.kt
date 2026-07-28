package com.xemniz.langcoach.ui.call

internal fun appendTranscript(
    messages: List<ChatMessage>,
    role: ChatRole,
    fragment: String,
    realtimeItemId: String? = null,
): List<ChatMessage> {
    val matchingIndex = realtimeItemId?.let { id ->
        messages.indexOfLast { it.role == role && it.realtimeItemId == id }
    } ?: -1
    if (matchingIndex >= 0) {
        return messages.mapIndexed { index, message ->
            if (index == matchingIndex) message.copy(text = message.text + fragment) else message
        }
    }

    val last = messages.lastOrNull()
    return if (realtimeItemId == null && last != null && last.role == role) {
        messages.dropLast(1) + last.copy(text = last.text + fragment)
    } else {
        messages + ChatMessage(role, fragment, realtimeItemId)
    }
}

internal fun completeTranscript(
    messages: List<ChatMessage>,
    role: ChatRole,
    fullText: String,
    realtimeItemId: String? = null,
): List<ChatMessage> {
    val matchingIndex = realtimeItemId?.let { id ->
        messages.indexOfLast { it.role == role && it.realtimeItemId == id }
    } ?: -1
    if (matchingIndex >= 0) {
        return messages.mapIndexed { index, message ->
            if (index == matchingIndex) message.copy(text = fullText) else message
        }
    }

    val last = messages.lastOrNull()
    return if (realtimeItemId == null && last != null && last.role == role) {
        messages.dropLast(1) + last.copy(text = fullText)
    } else {
        messages + ChatMessage(role, fullText, realtimeItemId)
    }
}
