package com.xemniz.langcoach.di

import com.xemniz.langcoach.ui.call.AndroidCallSessionLifecycle
import com.xemniz.langcoach.ui.call.CallSessionLifecycle
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidUiModule = module {
    single<CallSessionLifecycle> { AndroidCallSessionLifecycle(androidContext()) }
}
