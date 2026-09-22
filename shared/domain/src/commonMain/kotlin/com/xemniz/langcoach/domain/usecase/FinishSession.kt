package com.xemniz.langcoach.domain.usecase

import com.xemniz.langcoach.data.db.UsageEntry
import com.xemniz.langcoach.data.db.SessionTurn
import com.xemniz.langcoach.data.repo.SessionRepo
import com.xemniz.langcoach.data.repo.UsageRepo
import com.xemniz.langcoach.domain.reflection.SessionTranscript
import kotlinx.datetime.Clock

class FinishSession(
    private val sessionRepo: SessionRepo,
    private val usageRepo: UsageRepo,
) {
    suspend operator fun invoke(
        sessionId: Long,
        tokensIn: Int,
        tokensOut: Int,
        atMillis: Long = Clock.System.now().toEpochMilliseconds(),
        costCents: Int = 0,
        transcript: SessionTranscript = SessionTranscript(emptyList()),
    ) {
        sessionRepo.finishAndQueue(
            id = sessionId,
            endedAt = atMillis,
            tokensIn = tokensIn,
            tokensOut = tokensOut,
            costCents = costCents,
            turns = transcript.turns.mapIndexed { index, turn ->
                SessionTurn(sessionId, turn.id, index, turn.speaker.name, turn.text)
            },
        )
        usageRepo.replaceForSessionEndpoint(
            UsageEntry(
                createdAt = atMillis,
                sessionId = sessionId,
                endpoint = "realtime",
                model = "gpt-realtime-2.1",
                tokensIn = tokensIn,
                tokensOut = tokensOut,
                costCents = costCents,
            )
        )
    }
}
