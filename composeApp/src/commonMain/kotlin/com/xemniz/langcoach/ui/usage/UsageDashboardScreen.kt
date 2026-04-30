package com.xemniz.langcoach.ui.usage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xemniz.langcoach.data.db.UsageEntry
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun UsageDashboardScreen(viewModel: UsageDashboardViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatCard(
                    title = "Estimated cost",
                    value = formatDollars(state.totalCostCents),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = "Total tokens",
                    value = formatThousands(state.totalTokens),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Text(
                "Recent calls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
            )
        }
        if (state.recent.isEmpty()) {
            item {
                Text(
                    if (state.isLoading) "Loading…" else "No calls logged yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(state.recent, key = { it.id }) { entry ->
                UsageRow(entry)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun UsageRow(entry: UsageEntry) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("${entry.endpoint} · ${entry.model}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "${entry.tokensIn} in / ${entry.tokensOut} out",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(formatDollars(entry.costCents), style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatDollars(cents: Int): String {
    val sign = if (cents < 0) "-" else ""
    val abs = kotlin.math.abs(cents)
    val dollars = abs / 100
    val rem = abs % 100
    val pad = if (rem < 10) "0$rem" else "$rem"
    return "$sign$$dollars.$pad"
}

private fun formatThousands(n: Int): String {
    if (n < 1000) return "$n"
    val s = n.toString()
    val out = StringBuilder()
    var count = 0
    for (i in s.length - 1 downTo 0) {
        out.append(s[i])
        count++
        if (count % 3 == 0 && i != 0) out.append(',')
    }
    return out.reverse().toString()
}
