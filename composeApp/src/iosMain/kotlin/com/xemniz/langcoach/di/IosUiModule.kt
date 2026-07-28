package com.xemniz.langcoach.di

import com.xemniz.langcoach.ui.call.CallSessionLifecycle
import com.xemniz.langcoach.ui.call.IosCallSessionLifecycle
import org.koin.dsl.module

val iosUiModule = module {
    single<CallSessionLifecycle> { IosCallSessionLifecycle() }
}
