package com.xemniz.langcoach.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import com.xemniz.langcoach.ui.call.CallScreen
import com.xemniz.langcoach.ui.coach.CoachMemoryScreen
import com.xemniz.langcoach.ui.errors.ErrorsScreen
import com.xemniz.langcoach.ui.demo.DemoCallScreen
import com.xemniz.langcoach.ui.demo.DemoRecap
import com.xemniz.langcoach.ui.demo.SessionRecapScreen
import com.xemniz.langcoach.ui.home.HomeScreen
import com.xemniz.langcoach.ui.settings.SettingsScreen
import com.xemniz.langcoach.ui.usage.UsageDashboardScreen
import com.xemniz.langcoach.ui.vocab.VocabListScreen

private sealed interface Route {
    data object Home : Route
    data object Settings : Route
    data object Call : Route
    data object Vocab : Route
    data object Errors : Route
    data object Usage : Route
    data object CoachMemory : Route
    data object Demo : Route
    data class Recap(val value: DemoRecap) : Route
}

@Composable
fun AppNav() {
    val backStack: SnapshotStateList<Route> = remember { mutableStateListOf(Route.Home) }
    val pop = {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }
    BackHandler(enabled = backStack.size > 1) { pop() }
    App {
        when (backStack.last()) {
            Route.Home -> BareScaffold {
                HomeScreen(
                    onSettingsClick = { backStack.add(Route.Settings) },
                    onStartSession = { backStack.add(Route.Call) },
                    onDemoClick = { backStack.add(Route.Demo) },
                    onVocabClick = { backStack.add(Route.Vocab) },
                    onProgressClick = { backStack.add(Route.Errors) },
                    onCoachMemoryClick = { backStack.add(Route.CoachMemory) },
                )
            }
            Route.Settings -> WithBar("Settings", pop) { SettingsScreen() }
            Route.Vocab -> WithBar("Vocabulary", pop) { VocabListScreen() }
            Route.Errors -> WithBar("Error patterns", pop) { ErrorsScreen() }
            Route.Usage -> WithBar("Usage", pop) { UsageDashboardScreen() }
            Route.CoachMemory -> WithBar("Coach memory", pop) { CoachMemoryScreen() }
            Route.Call -> WithBar("Session", pop) { CallScreen(onDone = pop) }
            Route.Demo -> BareScaffold {
                DemoCallScreen(
                    onFinish = { recap -> backStack.add(Route.Recap(recap)) },
                    onExit = pop,
                )
            }
            is Route.Recap -> BareScaffold {
                SessionRecapScreen(
                    recap = (backStack.last() as Route.Recap).value,
                    onDone = { backStack.clear(); backStack.add(Route.Home) },
                )
            }
        }
    }
}

@Composable
private fun BareScaffold(content: @Composable () -> Unit) {
    Scaffold { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) { content() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WithBar(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("‹", style = MaterialTheme.typography.headlineMedium)
                    }
                },
            )
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) { content() }
    }
}
