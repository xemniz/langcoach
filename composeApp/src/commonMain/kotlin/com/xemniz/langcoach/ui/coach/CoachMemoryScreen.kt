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
import kotlin.time.Instant
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
            "These notes help your tutor prepare future lessons. You can change or clear them at any time.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = state.content,
            onValueChange = { viewModel.onContentChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 240.dp),
            label = { Text("Tutor notes") },
        )

        state.updatedAt?.let { ts ->
            Text(
                "Updated ${formatTimestamp(ts)}",
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
                Text("Clear notes")
            }
        }

        Text("Your goals", style = MaterialTheme.typography.titleLarge)
        if (state.goals.none { it.status != PracticalGoalStatus.Rejected }) {
            Text(
                "When your tutor notices a real-world goal, you'll be asked before it shapes future lessons.",
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
                        if (goal.status == PracticalGoalStatus.Confirmed) {
                            "${goal.targetLang} · Active goal"
                        } else {
                            "${goal.targetLang} · Suggested goal"
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )
                    OutlinedTextField(
                        value = goal.description,
                        onValueChange = { viewModel.onGoalDescriptionChange(goal.id, it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("What you want to do") },
                    )
                    Text(
                        "Based on: “${goal.evidenceText}”",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (goal.status != PracticalGoalStatus.Confirmed) {
                        Button(
                            onClick = {
                                viewModel.respondToGoal(goal.id, PracticalGoalResponse.Accept)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Use this goal") }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.respondToGoal(goal.id, PracticalGoalResponse.Defer)
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text("Decide later") }
                            OutlinedButton(
                                onClick = {
                                    viewModel.respondToGoal(goal.id, PracticalGoalResponse.Reject)
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text("This isn't my goal") }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.saveGoal(goal.id) }) { Text("Save changes") }
                        OutlinedButton(
                            onClick = { viewModel.removeGoal(goal.id) },
                        ) { Text("Remove") }
                    }
                }
            }
        }

        Text("Recent lessons", style = MaterialTheme.typography.titleLarge)
        if (state.recentLessons.isEmpty()) {
            Text(
                "Your completed lessons will appear here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        state.recentLessons.forEach { lesson ->
            Card {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        listOfNotNull(
                            lesson.targetLanguage.takeIf(String::isNotBlank),
                            formatTimestamp(lesson.startedAt),
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    lesson.summary?.takeIf(String::isNotBlank)?.let { Text(it) }
                    lesson.strength?.takeIf(String::isNotBlank)?.let { Text("What went well: $it") }
                    lesson.nextStep?.takeIf(String::isNotBlank)?.let { Text("Next lesson: $it") }
                    lesson.assignment?.takeIf(String::isNotBlank)?.let { Text("Before then: $it") }
                    when (lesson.recapStatus) {
                        LessonRecapStatus.Queued, LessonRecapStatus.Preparing -> Text(
                            "Preparing your recap…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LessonRecapStatus.Failed -> Text(
                            "The recap isn't ready. You can try again.",
                            color = MaterialTheme.colorScheme.error,
                        )
                        LessonRecapStatus.Removed -> Text(
                            "This unfinished recap was removed.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LessonRecapStatus.Ready, LessonRecapStatus.None -> Unit
                    }
                    if (lesson.recapStatus in setOf(LessonRecapStatus.Queued, LessonRecapStatus.Failed)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.retryLessonRecap(lesson.id) },
                                enabled = state.retryingLessonId == null,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(if (state.retryingLessonId == lesson.id) "Retrying…" else "Retry recap")
                            }
                            OutlinedButton(
                                onClick = { viewModel.discardLessonTranscript(lesson.id) },
                                enabled = state.retryingLessonId == null,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Remove unfinished lesson")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(epochMillis: Long): String {
    return Instant.fromEpochMilliseconds(epochMillis).toString().substringBefore('T')
}
