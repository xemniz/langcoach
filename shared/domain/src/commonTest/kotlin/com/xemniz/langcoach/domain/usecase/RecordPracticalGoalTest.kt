package com.xemniz.langcoach.domain.usecase

import com.xemniz.langcoach.data.db.PracticalGoal
import com.xemniz.langcoach.data.db.PracticalGoalStatus
import com.xemniz.langcoach.data.repo.PracticalGoalStore
import com.xemniz.langcoach.domain.reflection.PracticalGoalObservation
import com.xemniz.langcoach.domain.reflection.SessionTranscript
import com.xemniz.langcoach.domain.reflection.TranscriptSpeaker
import com.xemniz.langcoach.domain.reflection.TranscriptTurn
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class RecordPracticalGoalTest {
    private val store = FakePracticalGoalStore()
    private val goals = RecordPracticalGoal(store)

    @Test
    fun inferredGoalPersonalisesExamplesWithoutReplacingConfirmedCourseDirection() = runBlocking {
        store.save(
            PracticalGoal(
                id = 1,
                targetLang = "es",
                description = "Handle conversations with neighbours",
                status = PracticalGoalStatus.Confirmed,
                evidenceTurnId = "turn-old",
                evidenceText = "I want to speak with my neighbours",
                sourceSessionId = 4,
                createdAt = 10,
                updatedAt = 10,
            ),
        )

        val inferred = goals.infer(
            targetLang = "es",
            sourceSessionId = 9,
            description = "Prepare for a job interview",
            evidenceTurnId = "turn-3",
            evidenceText = "I have an interview next month",
            atMillis = 20,
        )

        assertEquals(PracticalGoalStatus.Tentative, inferred.status)
        assertEquals("Prepare for a job interview", store.tentative("es")?.description)
        assertEquals("Handle conversations with neighbours", store.confirmed("es")?.description)
    }

    @Test
    fun acceptingTentativeGoalMakesItTheOnlyConfirmedCourseDirection() = runBlocking {
        store.save(confirmedGoal(id = 1, description = "Speak with neighbours"))
        val tentative = goals.infer(
            targetLang = "es",
            sourceSessionId = 9,
            description = "Prepare for a job interview",
            evidenceTurnId = "turn-3",
            evidenceText = "I have an interview next month",
            atMillis = 20,
        )

        goals.respond(tentative.id, PracticalGoalResponse.Accept, atMillis = 30)

        assertEquals("Prepare for a job interview", store.confirmed("es")?.description)
        assertEquals(
            1,
            store.active("es").count { it.status == PracticalGoalStatus.Confirmed },
        )
    }

    @Test
    fun unsupportedInferenceIsDiscarded() = runBlocking {
        val result = goals.inferFromObservation(
            targetLang = "es",
            sourceSessionId = 9,
            transcript = SessionTranscript(
                listOf(TranscriptTurn("turn-3", TranscriptSpeaker.Learner, "I enjoy football")),
            ),
            observation = PracticalGoalObservation(
                description = "Negotiate a football contract",
                evidenceTurnId = "turn-3",
                evidenceText = "I have a contract negotiation",
                confidence = 0.98,
            ),
            atMillis = 20,
        )

        assertEquals(null, result)
        assertEquals(null, store.tentative("es"))
    }

    @Test
    fun explicitLearnerAcceptanceConfirmsTentativeGoal() = runBlocking {
        val tentative = goals.infer(
            targetLang = "es",
            sourceSessionId = 9,
            description = "Prepare for a job interview",
            evidenceTurnId = "turn-1",
            evidenceText = "I have an interview next month",
            atMillis = 20,
        )

        goals.respondFromObservation(
            goalId = tentative.id,
            response = PracticalGoalResponse.Accept,
            transcript = SessionTranscript(
                listOf(TranscriptTurn("turn-4", TranscriptSpeaker.Learner, "Yes, let's work on that.")),
            ),
            evidenceTurnId = "turn-4",
            evidenceText = "Yes, let's work on that.",
            confidence = 0.92,
            atMillis = 30,
        )

        assertEquals(PracticalGoalStatus.Confirmed, store.byId(tentative.id)?.status)
    }

    @Test
    fun retryingTheSameLessonDoesNotDuplicateItsInferredGoal() = runBlocking {
        repeat(2) {
            goals.infer(
                targetLang = "es",
                sourceSessionId = 9,
                description = "Prepare for a job interview",
                evidenceTurnId = "turn-3",
                evidenceText = "I have an interview next month",
                atMillis = 20L + it,
            )
        }

        assertEquals(1, store.all().size)
    }

    @Test
    fun staleProcessingCannotPromoteAGoalTheLearnerRejected() = runBlocking {
        val tentative = goals.infer(
            targetLang = "es",
            sourceSessionId = 9,
            description = "Prepare for a job interview",
            evidenceTurnId = "turn-1",
            evidenceText = "I have an interview next month",
            atMillis = 20,
        )
        goals.respond(tentative.id, PracticalGoalResponse.Reject, atMillis = 21)

        val result = goals.respondFromObservation(
            goalId = tentative.id,
            response = PracticalGoalResponse.Accept,
            transcript = SessionTranscript(
                listOf(TranscriptTurn("turn-4", TranscriptSpeaker.Learner, "Yes, let's do that.")),
            ),
            evidenceTurnId = "turn-4",
            evidenceText = "Yes, let's do that.",
            confidence = 0.95,
            atMillis = 22,
        )

        assertEquals(null, result)
        assertEquals(PracticalGoalStatus.Rejected, store.byId(tentative.id)?.status)
    }

    @Test
    fun learnerCanPermanentlyRemoveAGoal() = runBlocking {
        val tentative = goals.infer(
            targetLang = "es",
            sourceSessionId = 9,
            description = "Prepare for a job interview",
            evidenceTurnId = "turn-1",
            evidenceText = "I have an interview next month",
            atMillis = 20,
        )

        goals.remove(tentative.id)

        assertEquals("", store.byId(tentative.id)?.description)
        assertEquals(PracticalGoalStatus.Rejected, store.byId(tentative.id)?.status)
        assertEquals(0, store.active("es").size)
    }

    private fun confirmedGoal(id: Long, description: String) = PracticalGoal(
        id = id,
        targetLang = "es",
        description = description,
        status = PracticalGoalStatus.Confirmed,
        evidenceTurnId = "turn-old",
        evidenceText = description,
        sourceSessionId = 4,
        createdAt = 10,
        updatedAt = 10,
    )
}

private class FakePracticalGoalStore : PracticalGoalStore {
    private val records = mutableListOf<PracticalGoal>()
    private var nextId = 100L

    override suspend fun save(goal: PracticalGoal): PracticalGoal {
        val saved = if (goal.id == 0L) goal.copy(id = nextId++) else goal
        records.removeAll { it.id == saved.id }
        records += saved
        return saved
    }

    override suspend fun byId(id: Long): PracticalGoal? = records.firstOrNull { it.id == id }

    override suspend fun bySourceSessionId(sourceSessionId: Long): PracticalGoal? =
        records.firstOrNull { it.sourceSessionId == sourceSessionId }

    override suspend fun remove(id: Long, updatedAt: Long) {
        val index = records.indexOfFirst { it.id == id }
        if (index >= 0) {
            records[index] = records[index].copy(
                description = "",
                evidenceTurnId = "",
                evidenceText = "",
                status = PracticalGoalStatus.Rejected,
                updatedAt = updatedAt,
            )
        }
    }

    override suspend fun active(targetLang: String): List<PracticalGoal> = records.filter {
        it.targetLang == targetLang && it.status != PracticalGoalStatus.Rejected
    }

    override suspend fun tentative(targetLang: String): PracticalGoal? = records.lastOrNull {
        it.targetLang == targetLang && it.status == PracticalGoalStatus.Tentative
    }

    override suspend fun confirmed(targetLang: String): PracticalGoal? = records.lastOrNull {
        it.targetLang == targetLang && it.status == PracticalGoalStatus.Confirmed
    }

    override suspend fun deferOtherConfirmed(targetLang: String, exceptId: Long, updatedAt: Long) {
        records.indices.forEach { index ->
            val goal = records[index]
            if (
                goal.targetLang == targetLang &&
                goal.status == PracticalGoalStatus.Confirmed &&
                goal.id != exceptId
            ) {
                records[index] = goal.copy(
                    status = PracticalGoalStatus.Deferred,
                    updatedAt = updatedAt,
                )
            }
        }
    }

    override suspend fun updateTentativeStatus(
        id: Long,
        status: PracticalGoalStatus,
        updatedAt: Long,
    ): Boolean {
        val index = records.indexOfFirst { it.id == id && it.status == PracticalGoalStatus.Tentative }
        if (index < 0) return false
        records[index] = records[index].copy(status = status, updatedAt = updatedAt)
        return true
    }

    override suspend fun confirm(id: Long, targetLang: String, updatedAt: Long): Boolean {
        deferOtherConfirmed(targetLang, id, updatedAt)
        val index = records.indexOfFirst { it.id == id }
        if (index < 0) return false
        records[index] = records[index].copy(
            status = PracticalGoalStatus.Confirmed,
            updatedAt = updatedAt,
        )
        return true
    }

    override suspend fun confirmTentative(
        id: Long,
        targetLang: String,
        updatedAt: Long,
    ): Boolean {
        val changed = updateTentativeStatus(id, PracticalGoalStatus.Confirmed, updatedAt)
        if (changed) deferOtherConfirmed(targetLang, id, updatedAt)
        return changed
    }

    override suspend fun editIfActive(
        id: Long,
        description: String,
        updatedAt: Long,
    ): Boolean {
        val index = records.indexOfFirst {
            it.id == id && it.status != PracticalGoalStatus.Rejected
        }
        if (index < 0) return false
        records[index] = records[index].copy(description = description, updatedAt = updatedAt)
        return true
    }

    override suspend fun all(): List<PracticalGoal> = records.toList()
}
