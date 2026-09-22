package com.xemniz.langcoach

import kotlinx.coroutines.runBlocking

internal fun provisionDebugApiKeyBeforeUi(
    isDebug: Boolean,
    configuredKey: String,
    readExisting: suspend () -> String?,
    save: suspend (String) -> Unit,
) {
    val key = configuredKey.trim()
    if (!isDebug || key.isEmpty()) return

    runBlocking {
        if (readExisting().isNullOrBlank()) save(key)
    }
}
