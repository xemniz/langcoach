package com.xemniz.langcoach

import androidx.compose.ui.window.ComposeUIViewController
import com.xemniz.langcoach.data.dataModule
import com.xemniz.langcoach.data.iosDataModule
import com.xemniz.langcoach.data.repo.ErrorRepo
import com.xemniz.langcoach.data.sessionUsageDataModule
import com.xemniz.langcoach.data.vocabErrorsDataModule
import com.xemniz.langcoach.di.iosUiModule
import com.xemniz.langcoach.di.uiModule
import com.xemniz.langcoach.domain.domainModule
import com.xemniz.langcoach.llm.llmModule
import com.xemniz.langcoach.ui.AppNav
import com.xemniz.langcoach.voice.voiceModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatformTools
import platform.UIKit.UIViewController

private val iosAppScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

fun MainViewController(): UIViewController {
    if (KoinPlatformTools.defaultContext().getOrNull() == null) {
        val koin = startKoin {
            modules(
                dataModule,
                iosDataModule,
                vocabErrorsDataModule,
                sessionUsageDataModule,
                domainModule,
                llmModule,
                voiceModule,
                uiModule,
                iosUiModule,
            )
        }.koin
        iosAppScope.launch {
            koin.get<ErrorRepo>().seedTaxonomyIfEmpty()
        }
    }
    return ComposeUIViewController { AppNav() }
}
