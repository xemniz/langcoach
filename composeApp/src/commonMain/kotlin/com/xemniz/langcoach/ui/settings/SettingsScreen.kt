package com.xemniz.langcoach.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
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
        Text("Learning preferences", style = MaterialTheme.typography.titleLarge)
        Text(
            "Tell your tutor what you speak and what you want to practise.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = state.nativeLang,
            onValueChange = { viewModel.onIntent(SettingsIntent.NativeLangChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Language you already speak") },
        )

        OutlinedTextField(
            value = state.targetLang,
            onValueChange = { viewModel.onIntent(SettingsIntent.TargetLangChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Language you want to practise") },
        )

        Text("Current level", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProfileLevel.entries.filter { it != ProfileLevel.Unknown }.forEach { level ->
                FilterChip(
                    selected = state.level == level,
                    onClick = { viewModel.onIntent(SettingsIntent.LevelChanged(level)) },
                    label = { Text(level.name) },
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text("Voice service", style = MaterialTheme.typography.titleLarge)
        Text(
            if (state.hasApiKey) "OpenAI is connected." else "Connect OpenAI to start voice lessons.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = state.apiKey,
            onValueChange = { viewModel.onIntent(SettingsIntent.ApiKeyChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            label = { Text(if (state.hasApiKey) "Replace API key" else "OpenAI API key") },
        )
        Text(
            "Stored securely on this device and sent only to OpenAI.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.onIntent(SettingsIntent.VerifyAndSaveKey) },
                enabled = !state.isVerifying && state.apiKey.isNotBlank(),
            ) {
                if (state.isVerifying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Verify and save")
                }
            }
            if (state.hasApiKey) {
                OutlinedButton(onClick = { viewModel.onIntent(SettingsIntent.RemoveApiKey) }) {
                    Text("Remove key")
                }
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

    }
}
