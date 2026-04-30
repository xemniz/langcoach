package com.xemniz.langcoach.data

import com.xemniz.langcoach.data.db.LangCoachDatabase
import com.xemniz.langcoach.data.prefs.ProfilePrefs
import com.xemniz.langcoach.data.prefs.ProfilePrefsImpl
import com.xemniz.langcoach.data.repo.UserModelRepo
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dataModule = module {
    single<ProfilePrefs> { ProfilePrefsImpl(get()) }
    single { get<LangCoachDatabase>().vocabDao() }
    single { get<LangCoachDatabase>().userModelDao() }
    singleOf(::UserModelRepo)
}
