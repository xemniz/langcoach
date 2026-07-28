package com.xemniz.langcoach.ui.call

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import platform.AVFAudio.AVAudioSession

@Composable
actual fun CallScreen(onDone: () -> Unit) {
    val viewModel = koinViewModel<CallViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val requestPermission = {
        AVAudioSession.sharedInstance().requestRecordPermission { granted ->
            viewModel.onIntent(
                if (granted) CallIntent.PermissionGranted else CallIntent.PermissionDenied,
            )
        }
    }

    LaunchedEffect(Unit) {
        if (state is CallState.Idle || state is CallState.Ended || state is CallState.Failed) {
            requestPermission()
        }
    }

    CallScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
        onDone = onDone,
        onPermissionRequest = requestPermission,
    )
}
