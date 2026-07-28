package com.xemniz.langcoach.ui.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xemniz.langcoach.ui.theme.ForestDark
import com.xemniz.langcoach.ui.theme.Gold

@Composable
fun SessionRecapScreen(recap: DemoRecap, onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("SESSION COMPLETE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text("Nice work—here’s what changed.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RecapMetric(recap.duration, "speaking", Modifier.weight(1f))
            RecapMetric("${recap.turns}", "turns", Modifier.weight(1f))
            RecapMetric(recap.cost, "estimated", Modifier.weight(1f))
        }

        RecapCard("What went well", recap.strength)

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("One useful correction", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(recap.correctionBefore, color = MaterialTheme.colorScheme.error)
                Text(recap.correctionAfter, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = ForestDark, contentColor = MaterialTheme.colorScheme.onPrimary),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Added to your learning loop", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    recap.newWords.forEach { word ->
                        Surface(color = Gold, contentColor = ForestDark, shape = RoundedCornerShape(99.dp)) {
                            Text(word, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text("Next focus: ${recap.nextFocus}", color = MaterialTheme.colorScheme.primaryContainer)
            }
        }

        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Back to home", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RecapMetric(value: String, label: String, modifier: Modifier) {
    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp)).padding(14.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RecapCard(title: String, body: String) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(body)
        }
    }
}
