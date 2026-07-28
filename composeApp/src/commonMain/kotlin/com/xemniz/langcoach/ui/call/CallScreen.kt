package com.xemniz.langcoach.ui.call

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
expect fun CallScreen(onDone: () -> Unit)

@Composable
internal fun CallScreenContent(
    state: CallState,
    onIntent: (CallIntent) -> Unit,
    onDone: () -> Unit,
    onPermissionRequest: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (state) {
            CallState.Idle -> {
                Text("Preparing…", style = MaterialTheme.typography.titleMedium)
                CircularProgressIndicator()
            }
            CallState.PermissionRequired -> {
                Text(
                    "Microphone access is needed for voice sessions.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(onClick = onPermissionRequest) { Text("Grant microphone access") }
                FilledTonalButton(onClick = onDone) { Text("Back") }
            }
            CallState.Connecting -> {
                Text("Connecting…", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                CircularProgressIndicator()
            }
            is CallState.Live -> LiveContent(state, onIntent)
            is CallState.Failed -> {
                Text(
                    "Couldn't start session",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(state.message, color = MaterialTheme.colorScheme.error)
                FilledTonalButton(onClick = onDone) { Text("Back") }
            }
            CallState.Ended -> {
                Text("Session ended.", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onDone) { Text("Done") }
            }
        }
    }
}

@Composable
private fun ColumnScope.LiveContent(state: CallState.Live, onIntent: (CallIntent) -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.text?.length) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth().weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.messages.isEmpty()) {
            item {
                Text(
                    "Speak whenever you're ready.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(state.messages.size, key = { it }) { index ->
                MessageBubble(state.messages[index])
            }
        }
    }
    Text(
        text = when {
            state.isReconnecting -> "Reconnecting…"
            state.isAssistantSpeaking -> "Coach speaking…"
            state.isMuted -> "Microphone muted"
            else -> "Listening…"
        },
        style = MaterialTheme.typography.labelLarge,
        color = if (state.isAssistantSpeaking) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.align(Alignment.CenterHorizontally),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    ) {
        FilledTonalButton(onClick = { onIntent(CallIntent.ToggleMute) }) {
            Text(if (state.isMuted) "Unmute" else "Mute")
        }
        Button(onClick = { onIntent(CallIntent.End) }) { Text("End session") }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == ChatRole.User
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val container = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val content = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            color = container,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp,
            ),
            modifier = Modifier.align(alignment).widthIn(max = 320.dp),
        ) {
            Text(
                message.text,
                color = content,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}
