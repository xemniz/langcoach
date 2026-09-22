package com.xemniz.langcoach.data.repo

import com.xemniz.langcoach.data.db.PracticalGoal
import com.xemniz.langcoach.data.db.PracticalGoalDao
import com.xemniz.langcoach.data.db.PracticalGoalStatus
import kotlinx.coroutines.flow.Flow

interface PracticalGoalStore {
    suspend fun save(goal: PracticalGoal): PracticalGoal
    suspend fun byId(id: Long): PracticalGoal?
    suspend fun bySourceSessionId(sourceSessionId: Long): PracticalGoal?
    suspend fun remove(id: Long, updatedAt: Long)
    suspend fun active(targetLang: String): List<PracticalGoal>
    suspend fun tentative(targetLang: String): PracticalGoal?
    suspend fun confirmed(targetLang: String): PracticalGoal?
    suspend fun deferOtherConfirmed(targetLang: String, exceptId: Long, updatedAt: Long)
    suspend fun updateTentativeStatus(
        id: Long,
        status: PracticalGoalStatus,
        updatedAt: Long,
    ): Boolean
    suspend fun confirm(id: Long, targetLang: String, updatedAt: Long): Boolean
    suspend fun confirmTentative(id: Long, targetLang: String, updatedAt: Long): Boolean
    suspend fun editIfActive(id: Long, description: String, updatedAt: Long): Boolean
    suspend fun all(): List<PracticalGoal>
}

class RoomPracticalGoalStore(
    private val dao: PracticalGoalDao,
) : PracticalGoalStore {
    override suspend fun save(goal: PracticalGoal): PracticalGoal {
        if (goal.id != 0L) {
            dao.update(goal)
            return goal
        }
        return goal.copy(id = dao.insert(goal))
    }

    override suspend fun byId(id: Long): PracticalGoal? = dao.byId(id)
    override suspend fun bySourceSessionId(sourceSessionId: Long): PracticalGoal? =
        dao.bySourceSessionId(sourceSessionId)
    override suspend fun remove(id: Long, updatedAt: Long) = dao.scrub(id, updatedAt)
    override suspend fun active(targetLang: String): List<PracticalGoal> = dao.active(targetLang)
    override suspend fun tentative(targetLang: String): PracticalGoal? = dao.tentative(targetLang)
    override suspend fun confirmed(targetLang: String): PracticalGoal? = dao.confirmed(targetLang)
    override suspend fun deferOtherConfirmed(targetLang: String, exceptId: Long, updatedAt: Long) =
        dao.deferOtherConfirmed(targetLang, exceptId, updatedAt)
    override suspend fun updateTentativeStatus(
        id: Long,
        status: PracticalGoalStatus,
        updatedAt: Long,
    ): Boolean = dao.updateTentativeStatus(id, status, updatedAt) == 1
    override suspend fun confirm(id: Long, targetLang: String, updatedAt: Long): Boolean =
        dao.confirm(id, targetLang, updatedAt) == 1
    override suspend fun confirmTentative(
        id: Long,
        targetLang: String,
        updatedAt: Long,
    ): Boolean = dao.confirmTentative(id, targetLang, updatedAt) == 1
    override suspend fun editIfActive(id: Long, description: String, updatedAt: Long): Boolean =
        dao.editIfActive(id, description, updatedAt) == 1
    override suspend fun all(): List<PracticalGoal> = dao.all()

    fun observeAll(): Flow<List<PracticalGoal>> = dao.observeAll()
}
