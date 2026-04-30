package com.xemniz.langcoach

import android.app.Application
import com.xemniz.langcoach.compose.BuildConfig
import com.xemniz.langcoach.data.SecureStorage
import com.xemniz.langcoach.data.androidDataModule
import com.xemniz.langcoach.data.dataModule
import com.xemniz.langcoach.data.repo.ErrorRepo
import com.xemniz.langcoach.data.sessionUsageDataModule
import com.xemniz.langcoach.data.vocabErrorsDataModule
import com.xemniz.langcoach.di.androidUiModule
import com.xemniz.langcoach.di.uiModule
import com.xemniz.langcoach.domain.OPENAI_API_KEY
import com.xemniz.langcoach.domain.domainModule
import com.xemniz.langcoach.llm.llmModule
import com.xemniz.langcoach.voice.voiceModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class LangCoachApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@LangCoachApp)
            modules(
                dataModule,
                androidDataModule,
                vocabErrorsDataModule,
                sessionUsageDataModule,
                domainModule,
                llmModule,
                voiceModule,
                uiModule,
                androidUiModule,
            )
        }
        val errorRepo: ErrorRepo by inject()
        val secureStorage: SecureStorage by inject()
        appScope.launch {
            errorRepo.seedTaxonomyIfEmpty()
            if (BuildConfig.DEBUG && BuildConfig.DEV_OPENAI_API_KEY.isNotBlank() &&
                secureStorage.read(OPENAI_API_KEY) == null
            ) {
                secureStorage.save(OPENAI_API_KEY, BuildConfig.DEV_OPENAI_API_KEY)
            }
        }
    }
}
