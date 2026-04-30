package com.xemniz.langcoach.data

import com.xemniz.langcoach.data.db.LangCoachDatabase
import com.xemniz.langcoach.data.repo.ErrorRepo
import com.xemniz.langcoach.data.repo.VocabRepo
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val vocabErrorsDataModule = module {
    single { get<LangCoachDatabase>().errorCategoryDao() }
    single { get<LangCoachDatabase>().errorInstanceDao() }
    singleOf(::VocabRepo)
    singleOf(::ErrorRepo)
}
