package com.xemniz.langcoach

import android.media.AudioManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.xemniz.langcoach.ui.AppNav

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC
        setContent {
            AppNav()
        }
    }
}
