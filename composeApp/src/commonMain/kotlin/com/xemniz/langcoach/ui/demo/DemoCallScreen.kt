package com.xemniz.langcoach.ui.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xemniz.langcoach.ui.call.ChatMessage
import com.xemniz.langcoach.ui.call.ChatRole
import com.xemniz.langcoach.ui.theme.ForestDark
import com.xemniz.langcoach.ui.theme.Gold
import kotlinx.coroutines.delay

private val demoMessages = listOf(
    ChatMessage(ChatRole.Assistant, "Hoy vamos a practicar el pasado. ¿Qué hiciste el fin de semana?"),
    ChatMessage(ChatRole.User, "Ayer voy al mercado con mi amiga y compramos fruta."),
    ChatMessage(ChatRole.Assistant, "Muy bien. Pequeña corrección: ayer fui al mercado. ¿Había muchos puestos?"),
    ChatMessage(ChatRole.User, "Sí, y intenté regatear por primera vez."),
    ChatMessage(ChatRole.Assistant, "¡Perfecto! Has usado regatear de forma muy natural. ¿Conseguiste un buen precio?"),
    ChatMessage(ChatRole.User, "Sí, después dimos una vuelta por el barrio."),
)

@Composable
fun DemoCallScreen(
    onFinish: (DemoRecap) -> Unit,
    onExit: () -> Unit,
) {
    var visibleCount by remember { mutableIntStateOf(1) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        while (visibleCount < demoMessages.size) {
            delay(1_050)
            visibleCount++
        }
    }
    LaunchedEffect(visibleCount) {
        listState.animateScrollToItem(visibleCount - 1)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("SPANISH · B2", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text("Storytelling practice", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(99.dp)) {
                Text("DEMO", modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), fontWeight = FontWeight.Bold)
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = ForestDark,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(24.dp),
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.background(Gold, CircleShape).padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("LC", color = ForestDark, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text(if (visibleCount < demoMessages.size) "Coach is listening" else "Session complete", fontWeight = FontWeight.Bold)
                    Text("Live transcript · private memory on", color = MaterialTheme.colorScheme.primaryContainer)
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(demoMessages.take(visibleCount)) { message ->
                DemoBubble(message)
            }
        }

        if (visibleCount == demoMessages.size) {
            Button(
                onClick = { onFinish(portfolioDemoRecap) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("See session recap", fontWeight = FontWeight.Bold)
            }
        } else {
            FilledTonalButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
                Text("Exit demo")
            }
        }
    }
}

@Composable
private fun DemoBubble(message: ChatMessage) {
    val user = message.role == ChatRole.User
    Box(Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.align(if (user) Alignment.CenterEnd else Alignment.CenterStart).widthIn(max = 330.dp),
            color = if (user) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (user) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(message.text, modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp))
        }
    }
}
