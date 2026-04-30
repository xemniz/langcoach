package com.xemniz.langcoach.llm

import com.xemniz.langcoach.domain.OpenAIClient
import com.xemniz.langcoach.domain.reflection.ReflectionService
import com.xemniz.langcoach.llm.chat.ChatCompletionsClient
import com.xemniz.langcoach.llm.realtime.OpenAIRealtimeClient
import com.xemniz.langcoach.llm.reflection.ReflectionServiceImpl
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val llmModule = module {
    single { buildHttpClient() }
    single<OpenAIClient> { OpenAIClientImpl(get()) }
    singleOf(::OpenAIRealtimeClient)
    singleOf(::ChatCompletionsClient)
    single<ReflectionService> { ReflectionServiceImpl(get()) }
}
