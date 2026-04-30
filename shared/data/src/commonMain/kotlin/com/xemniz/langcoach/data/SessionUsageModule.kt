package com.xemniz.langcoach.data

import com.xemniz.langcoach.data.db.LangCoachDatabase
import com.xemniz.langcoach.data.repo.SessionRepo
import com.xemniz.langcoach.data.repo.UsageRepo
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val sessionUsageDataModule = module {
    single { get<LangCoachDatabase>().sessionDao() }
    single { get<LangCoachDatabase>().usageDao() }
    singleOf(::SessionRepo)
    singleOf(::UsageRepo)
}
