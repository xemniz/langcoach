package com.xemniz.langcoach.di

import com.xemniz.langcoach.ui.call.CallViewModel
import com.xemniz.langcoach.ui.call.RealtimeCallCoordinator
import com.xemniz.langcoach.ui.coach.CoachMemoryViewModel
import com.xemniz.langcoach.ui.errors.ErrorsViewModel
import com.xemniz.langcoach.ui.home.HomeViewModel
import com.xemniz.langcoach.ui.settings.SettingsViewModel
import com.xemniz.langcoach.ui.usage.UsageDashboardViewModel
import com.xemniz.langcoach.ui.vocab.VocabListViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val uiModule = module {
    single { AppScope() }
    factoryOf(::RealtimeCallCoordinator)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::CallViewModel)
    viewModelOf(::VocabListViewModel)
    viewModelOf(::ErrorsViewModel)
    viewModelOf(::UsageDashboardViewModel)
    viewModelOf(::CoachMemoryViewModel)
}
