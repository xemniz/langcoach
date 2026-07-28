package com.xemniz.langcoach.ui.call

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
actual fun CallScreen(onDone: () -> Unit) {
    val viewModel = koinViewModel<CallViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onIntent(if (granted) CallIntent.PermissionGranted else CallIntent.PermissionDenied)
    }
    val requestPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }

    LaunchedEffect(Unit) {
        if (state is CallState.Idle || state is CallState.Ended || state is CallState.Failed) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) viewModel.onIntent(CallIntent.Start) else requestPermission()
        }
    }

    CallScreenContent(
        state = state,
        onIntent = viewModel::onIntent,
        onDone = onDone,
        onPermissionRequest = requestPermission,
    )
}
