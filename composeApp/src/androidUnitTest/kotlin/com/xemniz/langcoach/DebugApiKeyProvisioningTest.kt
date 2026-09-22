package com.xemniz.langcoach

import kotlin.test.Test
import kotlin.test.assertEquals

class DebugApiKeyProvisioningTest {
    @Test
    fun seedsConfiguredKeyBeforeReturningWhenStorageIsEmpty() {
        var storedKey: String? = null

        provisionDebugApiKeyBeforeUi(
            isDebug = true,
            configuredKey = "debug-key",
            readExisting = { storedKey },
            save = { storedKey = it },
        )

        assertEquals("debug-key", storedKey)
    }
}
