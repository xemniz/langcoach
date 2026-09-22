package com.xemniz.langcoach.domain.usecase

import com.xemniz.langcoach.data.db.VocabItem
import com.xemniz.langcoach.data.repo.VocabRepo
import com.xemniz.langcoach.data.prefs.ProfilePrefs
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock

class GetDueVocab(private val repo: VocabRepo, private val profilePrefs: ProfilePrefs) {
    suspend operator fun invoke(nowMillis: Long = Clock.System.now().toEpochMilliseconds()): List<VocabItem> =
        repo.getDue(nowMillis, profilePrefs.targetLang.first())
}
