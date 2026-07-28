package com.xemniz.langcoach.domain

import com.xemniz.langcoach.domain.fsrs.FsrsScheduler
import com.xemniz.langcoach.domain.session.SessionOrchestrator
import com.xemniz.langcoach.domain.session.ObjectiveEvaluationPolicy
import com.xemniz.langcoach.domain.session.SessionPlanner
import com.xemniz.langcoach.domain.usecase.FinishSession
import com.xemniz.langcoach.domain.usecase.GetDueVocab
import com.xemniz.langcoach.domain.usecase.GetRecentSessions
import com.xemniz.langcoach.domain.usecase.GetWeakCategories
import com.xemniz.langcoach.domain.usecase.ReflectSession
import com.xemniz.langcoach.domain.usecase.ScheduleVocabReview
import com.xemniz.langcoach.domain.usecase.StartSession
import com.xemniz.langcoach.domain.usecase.UpdateUserModel
import com.xemniz.langcoach.domain.usecase.VerifyApiKey
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val domainModule = module {
    singleOf(::FsrsScheduler)
    singleOf(::ObjectiveEvaluationPolicy)
    singleOf(::SessionPlanner)
    singleOf(::SessionOrchestrator)
    factoryOf(::VerifyApiKey)
    factoryOf(::GetDueVocab)
    factoryOf(::GetWeakCategories)
    factoryOf(::GetRecentSessions)
    factoryOf(::ScheduleVocabReview)
    factoryOf(::StartSession)
    factoryOf(::FinishSession)
    factoryOf(::ReflectSession)
    factoryOf(::UpdateUserModel)
}
