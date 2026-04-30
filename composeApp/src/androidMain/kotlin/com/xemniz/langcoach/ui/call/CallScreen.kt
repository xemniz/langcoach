package com.xemniz.langcoach.ui.call

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CallScreen(
    onDone: () -> Unit,
    viewModel: CallViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.onIntent(CallIntent.PermissionGranted)
        else viewModel.onIntent(CallIntent.PermissionDenied)
    }

    LaunchedEffect(Unit) {
        if (state == CallState.Idle) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) viewModel.onIntent(CallIntent.Start)
            else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (val s = state) {
            CallState.Idle -> {
                Text("Preparing…", style = MaterialTheme.typography.titleMedium)
                CircularProgressIndicator()
            }
            CallState.PermissionRequired -> {
                Text(
                    "Microphone access is needed for voice sessions.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                    Text("Grant microphone access")
                }
                FilledTonalButton(onClick = onDone) { Text("Back") }
            }
            CallState.Connecting -> {
                Text("Connecting…", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                CircularProgressIndicator()
            }
            is CallState.Live -> LiveContent(s, viewModel)
            is CallState.Failed -> {
                Text(
                    "Couldn't start session",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(s.message, color = MaterialTheme.colorScheme.error)
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
private fun ColumnScope.LiveContent(state: CallState.Live, viewModel: CallViewModel) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.text?.length) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
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
            items(state.messages.size, key = { it }) { i ->
                MessageBubble(state.messages[i])
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    ) {
        FilledTonalButton(onClick = { viewModel.onIntent(CallIntent.ToggleMute) }) {
            Text(if (state.isMuted) "Unmute" else "Mute")
        }
        Button(onClick = { viewModel.onIntent(CallIntent.End) }) {
            Text("End session")
        }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    val isUser = msg.role == ChatRole.User
    val align = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val container = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val onContainer = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            color = container,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp,
            ),
            modifier = Modifier.align(align).widthIn(max = 320.dp),
        ) {
            Text(
                msg.text,
                color = onContainer,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}
