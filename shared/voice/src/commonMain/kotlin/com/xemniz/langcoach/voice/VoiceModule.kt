package com.xemniz.langcoach.voice

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val voiceModule = module {
    singleOf(::AudioCapture)
    singleOf(::AudioPlayback)
}
