package com.xemniz.langcoach.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.xemniz.langcoach.ui.theme.LangCoachTheme

@Composable
fun App(content: @Composable () -> Unit) {
    LangCoachTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}
