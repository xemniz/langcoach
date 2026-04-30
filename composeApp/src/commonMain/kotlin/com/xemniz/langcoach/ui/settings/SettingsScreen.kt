package com.xemniz.langcoach.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xemniz.langcoach.core.ProfileLevel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("OpenAI API Key", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = state.apiKey,
            onValueChange = { viewModel.onIntent(SettingsIntent.ApiKeyChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            label = { Text("sk-...") },
        )
        Text(
            "BYO key — never leaves your device",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = { viewModel.onIntent(SettingsIntent.VerifyAndSaveKey) },
            enabled = !state.isVerifying,
        ) {
            if (state.isVerifying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Verify & Save")
            }
        }

        state.verifyMessage?.let { msg ->
            Text(
                text = msg,
                color = if (state.verifySuccess) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        HorizontalDivider()

        Text("Native Language", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = state.nativeLang,
            onValueChange = { viewModel.onIntent(SettingsIntent.NativeLangChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("e.g. English") },
        )

        Text("Target Language", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = state.targetLang,
            onValueChange = { viewModel.onIntent(SettingsIntent.TargetLangChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("e.g. Italian") },
        )

        Text("Level", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProfileLevel.entries.forEach { level ->
                FilterChip(
                    selected = state.level == level,
                    onClick = { viewModel.onIntent(SettingsIntent.LevelChanged(level)) },
                    label = { Text(level.name) },
                )
            }
        }
    }
}
