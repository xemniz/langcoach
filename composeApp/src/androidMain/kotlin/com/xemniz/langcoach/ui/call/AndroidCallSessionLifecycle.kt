package com.xemniz.langcoach.ui.call

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AndroidCallSessionLifecycle(private val context: Context) : CallSessionLifecycle {

    override fun start() {
        val intent = Intent().apply {
            component = ComponentName(context.packageName, SERVICE_CLASS)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    override fun stop() {
        val intent = Intent().apply {
            component = ComponentName(context.packageName, SERVICE_CLASS)
        }
        context.stopService(intent)
    }

    companion object {
        private const val SERVICE_CLASS = "com.xemniz.langcoach.CallSessionService"
    }
}
