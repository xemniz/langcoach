package com.xemniz.langcoach.domain.usecase

import com.xemniz.langcoach.data.db.PracticalGoal
import com.xemniz.langcoach.data.db.PracticalGoalStatus
import com.xemniz.langcoach.data.repo.PracticalGoalStore
import com.xemniz.langcoach.domain.reflection.PracticalGoalObservation
import com.xemniz.langcoach.domain.reflection.SessionTranscript
import com.xemniz.langcoach.domain.reflection.TranscriptSpeaker
import kotlinx.datetime.Clock

class RecordPracticalGoal(
    private val store: PracticalGoalStore,
) {
    suspend fun inferFromObservation(
        targetLang: String,
        sourceSessionId: Long,
        transcript: SessionTranscript,
        observation: PracticalGoalObservation,
        atMillis: Long = Clock.System.now().toEpochMilliseconds(),
    ): PracticalGoal? {
        if (observation.confidence < MINIMUM_CONFIDENCE) return null
        if (observation.description.isBlank() || observation.evidenceText.isBlank()) return null
        val evidenceTurn = transcript.turns.firstOrNull {
            it.id == observation.evidenceTurnId && it.speaker == TranscriptSpeaker.Learner
        } ?: return null
        if (!evidenceTurn.text.contains(observation.evidenceText, ignoreCase = true)) return null
        return infer(
            targetLang = targetLang,
            sourceSessionId = sourceSessionId,
            description = observation.description,
            evidenceTurnId = observation.evidenceTurnId,
            evidenceText = observation.evidenceText,
            atMillis = atMillis,
        )
    }

    suspend fun infer(
        targetLang: String,
        sourceSessionId: Long,
        description: String,
        evidenceTurnId: String,
        evidenceText: String,
        atMillis: Long = Clock.System.now().toEpochMilliseconds(),
    ): PracticalGoal {
        require(description.isNotBlank()) { "A practical goal needs a description" }
        require(evidenceText.isNotBlank()) { "A practical goal needs learner evidence" }
        store.bySourceSessionId(sourceSessionId)?.let { return it }
        return store.save(
            PracticalGoal(
                targetLang = targetLang,
                description = description.trim(),
                status = PracticalGoalStatus.Tentative,
                evidenceTurnId = evidenceTurnId,
                evidenceText = evidenceText,
                sourceSessionId = sourceSessionId,
                createdAt = atMillis,
                updatedAt = atMillis,
            ),
        )
    }

    suspend fun respond(
        goalId: Long,
        response: PracticalGoalResponse,
        description: String? = null,
        atMillis: Long = Clock.System.now().toEpochMilliseconds(),
    ): PracticalGoal? {
        val existing = store.byId(goalId) ?: return null
        val status = when (response) {
            PracticalGoalResponse.Accept -> PracticalGoalStatus.Confirmed
            PracticalGoalResponse.Defer -> PracticalGoalStatus.Deferred
            PracticalGoalResponse.Reject -> PracticalGoalStatus.Rejected
        }
        if (status == PracticalGoalStatus.Confirmed) {
            store.deferOtherConfirmed(existing.targetLang, existing.id, atMillis)
        }
        return store.save(
            existing.copy(
                description = description?.trim()?.takeIf(String::isNotEmpty) ?: existing.description,
                status = status,
                updatedAt = atMillis,
            ),
        )
    }

    suspend fun respondFromObservation(
        goalId: Long,
        response: PracticalGoalResponse,
        transcript: SessionTranscript,
        evidenceTurnId: String,
        evidenceText: String,
        confidence: Double,
        atMillis: Long = Clock.System.now().toEpochMilliseconds(),
    ): PracticalGoal? {
        if (confidence < MINIMUM_CONFIDENCE) return null
        if (evidenceText.isBlank()) return null
        val evidenceTurn = transcript.turns.firstOrNull {
            it.id == evidenceTurnId && it.speaker == TranscriptSpeaker.Learner
        } ?: return null
        if (!evidenceTurn.text.contains(evidenceText, ignoreCase = true)) return null
        val status = when (response) {
            PracticalGoalResponse.Accept -> PracticalGoalStatus.Confirmed
            PracticalGoalResponse.Defer -> PracticalGoalStatus.Deferred
            PracticalGoalResponse.Reject -> PracticalGoalStatus.Rejected
        }
        if (!store.updateTentativeStatus(goalId, status, atMillis)) return null
        val updated = store.byId(goalId) ?: return null
        if (status == PracticalGoalStatus.Confirmed) {
            store.deferOtherConfirmed(updated.targetLang, updated.id, atMillis)
        }
        return updated
    }

    suspend fun edit(
        goalId: Long,
        description: String,
        atMillis: Long = Clock.System.now().toEpochMilliseconds(),
    ): PracticalGoal? {
        val existing = store.byId(goalId) ?: return null
        val clean = description.trim()
        if (clean.isEmpty()) return null
        return store.save(existing.copy(description = clean, updatedAt = atMillis))
    }

    suspend fun remove(goalId: Long) = store.delete(goalId)

    private companion object {
        const val MINIMUM_CONFIDENCE = 0.8
    }
}

enum class PracticalGoalResponse {
    Accept,
    Defer,
    Reject,
}
