package com.xemniz.langcoach.ui.coach

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.Instant
import org.koin.compose.viewmodel.koinViewModel
import com.xemniz.langcoach.data.db.PracticalGoalStatus
import com.xemniz.langcoach.domain.usecase.PracticalGoalResponse

@Composable
fun CoachMemoryScreen(
    viewModel: CoachMemoryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text("Loading…", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "This is what the coach remembers about you. Edit or delete anything you want changed.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = state.content,
            onValueChange = { viewModel.onContentChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 240.dp),
            label = { Text("What the coach knows") },
        )

        state.updatedAt?.let { ts ->
            Text(
                "Last updated: ${formatTimestamp(ts)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { viewModel.save() },
                enabled = !state.saving,
            ) {
                Text(
                    when {
                        state.saving -> "Saving…"
                        state.justSaved -> "Saved"
                        else -> "Save"
                    }
                )
            }
            OutlinedButton(
                onClick = { viewModel.clear() },
                enabled = !state.saving,
            ) {
                Text("Clear")
            }
        }

        Text("Practical goals", style = MaterialTheme.typography.titleLarge)
        if (state.goals.none { it.status != PracticalGoalStatus.Rejected }) {
            Text(
                "The coach will infer possible real-world goals from your conversations and ask before changing your course.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.goals.filter { it.status != PracticalGoalStatus.Rejected }.forEach { goal ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "${goal.targetLang} · ${goal.status.name}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    OutlinedTextField(
                        value = goal.description,
                        onValueChange = { viewModel.onGoalDescriptionChange(goal.id, it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("What you want to do with the language") },
                    )
                    Text(
                        "From your words: “${goal.evidenceText}”",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.saveGoal(goal.id) }) { Text("Save") }
                        if (goal.status != PracticalGoalStatus.Confirmed) {
                            Button(
                                onClick = {
                                    viewModel.respondToGoal(goal.id, PracticalGoalResponse.Accept)
                                },
                            ) { Text("Use for my course") }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (goal.status != PracticalGoalStatus.Confirmed) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.respondToGoal(goal.id, PracticalGoalResponse.Defer)
                                },
                            ) { Text("Later") }
                            OutlinedButton(
                                onClick = {
                                    viewModel.respondToGoal(goal.id, PracticalGoalResponse.Reject)
                                },
                            ) { Text("Not my goal") }
                        }
                        OutlinedButton(
                            onClick = { viewModel.removeGoal(goal.id) },
                        ) { Text("Remove") }
                    }
                }
            }
        }

        Text("Recent lesson records", style = MaterialTheme.typography.titleLarge)
        state.recentLessons.forEach { lesson ->
            Card {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        "${lesson.targetLang.ifBlank { "Language not recorded" }} · ${lesson.processingState}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    lesson.strength?.takeIf(String::isNotBlank)?.let { Text("Demonstrated: $it") }
                    lesson.nextStep?.takeIf(String::isNotBlank)?.let { Text("Next step: $it") }
                    lesson.assignment?.takeIf(String::isNotBlank)?.let { Text("Practice: $it") }
                    lesson.processingError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                    if (lesson.processingState in setOf("Pending", "Failed")) {
                        OutlinedButton(
                            onClick = { viewModel.discardLessonTranscript(lesson.id) },
                        ) {
                            Text("Discard raw transcript")
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(epochMillis: Long): String {
    return Instant.fromEpochMilliseconds(epochMillis).toString()
}
