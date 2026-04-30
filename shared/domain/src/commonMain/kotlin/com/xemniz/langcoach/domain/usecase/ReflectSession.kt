package com.xemniz.langcoach.domain.usecase

import com.xemniz.langcoach.core.AppResult
import com.xemniz.langcoach.data.db.VocabItem
import com.xemniz.langcoach.data.prefs.ProfilePrefs
import com.xemniz.langcoach.data.repo.ErrorRepo
import com.xemniz.langcoach.data.repo.SessionRepo
import com.xemniz.langcoach.data.repo.VocabRepo
import com.xemniz.langcoach.domain.reflection.ReflectionService
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock

class ReflectSession(
    private val reflectionService: ReflectionService,
    private val profilePrefs: ProfilePrefs,
    private val vocabRepo: VocabRepo,
    private val errorRepo: ErrorRepo,
    private val sessionRepo: SessionRepo,
    private val updateUserModel: UpdateUserModel,
) {
    suspend operator fun invoke(
        sessionId: Long,
        userTranscript: String,
        assistantTranscript: String,
        atMillis: Long = Clock.System.now().toEpochMilliseconds(),
    ): AppResult<Int> {
        val targetLang = profilePrefs.targetLang.first()
        val nativeLang = profilePrefs.nativeLang.first()
        val level = profilePrefs.level.first().name
        val recentVocab = vocabRepo.recentWords(targetLang, limit = 100)
        val categories = errorRepo.listCategories()
        val allowedCodes = categories.map { it.code }
        val codeToId = categories.associate { it.code to it.id }

        println("REFLECT: start sessionId=$sessionId userChars=${userTranscript.length} assistantChars=${assistantTranscript.length} categoryCodes=${allowedCodes.size}")

        val result = reflectionService.reflect(
            targetLang = targetLang,
            nativeLang = nativeLang,
            level = level,
            recentVocab = recentVocab,
            userTranscript = userTranscript,
            assistantTranscript = assistantTranscript,
            allowedCategoryCodes = allowedCodes,
        )
        return when (result) {
            is AppResult.Failure -> {
                println("REFLECT: reflect FAILURE error=${result.error}")
                result
            }
            is AppResult.Success -> {
                val r = result.value
                println("REFLECT: reflect OK vocab=${r.newVocab.size} errors=${r.errors.size} summaryChars=${r.summary.length}")
                var added = 0
                var skipped = 0
                r.newVocab.forEach { v ->
                    val inserted = vocabRepo.insertIfNew(
                        VocabItem(
                            word = v.word,
                            lemma = v.lemma,
                            context = v.context,
                            targetLang = targetLang,
                            dueAt = atMillis,
                            createdAt = atMillis,
                            source = v.source,
                        )
                    )
                    if (inserted) added++ else skipped++
                }
                println("REFLECT: dedup added=$added skipped=$skipped")
                r.errors.forEach { e ->
                    val catId = codeToId[e.categoryCode] ?: return@forEach
                    errorRepo.logInstance(
                        categoryId = catId,
                        sessionId = sessionId,
                        originalText = e.originalText,
                        correctedText = e.correctedText,
                        atMillis = atMillis,
                    )
                }
                val existing = sessionRepo.byId(sessionId)
                if (existing != null) {
                    sessionRepo.finish(
                        id = sessionId,
                        endedAt = existing.endedAt ?: atMillis,
                        summary = r.summary,
                        tokensIn = existing.tokensIn + r.tokensIn,
                        tokensOut = existing.tokensOut + r.tokensOut,
                        costCents = existing.costCents,
                    )
                }
                updateUserModel(
                    userTranscript = userTranscript,
                    assistantTranscript = assistantTranscript,
                    sessionSummary = r.summary,
                    atMillis = atMillis,
                )
                AppResult.Success(r.newVocab.size)
            }
        }
    }
}
