package com.xemniz.langcoach.domain.usecase

import com.xemniz.langcoach.data.db.VocabItem
import com.xemniz.langcoach.data.repo.VocabRepo
import kotlinx.datetime.Clock

class GetDueVocab(private val repo: VocabRepo) {
    suspend operator fun invoke(nowMillis: Long = Clock.System.now().toEpochMilliseconds()): List<VocabItem> =
        repo.getDue(nowMillis)
}
