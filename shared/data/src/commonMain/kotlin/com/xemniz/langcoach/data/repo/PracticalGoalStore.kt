package com.xemniz.langcoach.data.repo

import com.xemniz.langcoach.data.db.PracticalGoal
import com.xemniz.langcoach.data.db.PracticalGoalDao
import com.xemniz.langcoach.data.db.PracticalGoalStatus
import kotlinx.coroutines.flow.Flow

interface PracticalGoalStore {
    suspend fun save(goal: PracticalGoal): PracticalGoal
    suspend fun byId(id: Long): PracticalGoal?
    suspend fun bySourceSessionId(sourceSessionId: Long): PracticalGoal?
    suspend fun delete(id: Long)
    suspend fun active(targetLang: String): List<PracticalGoal>
    suspend fun tentative(targetLang: String): PracticalGoal?
    suspend fun confirmed(targetLang: String): PracticalGoal?
    suspend fun deferOtherConfirmed(targetLang: String, exceptId: Long, updatedAt: Long)
    suspend fun updateTentativeStatus(
        id: Long,
        status: PracticalGoalStatus,
        updatedAt: Long,
    ): Boolean
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
    override suspend fun delete(id: Long) = dao.delete(id)
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
    override suspend fun all(): List<PracticalGoal> = dao.all()

    fun observeAll(): Flow<List<PracticalGoal>> = dao.observeAll()
}
